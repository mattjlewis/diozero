package com.diozero.internal.provider.remote.grpc;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Remote Provider
 * Filename:     GrpcClientDeviceFactory.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2024 diozero
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.tinylog.Logger;

import com.diozero.api.AnalogInputEvent;
import com.diozero.api.DeviceMode;
import com.diozero.api.DigitalInputEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.I2CConstants;
import com.diozero.api.PinInfo;
import com.diozero.api.RuntimeIOException;
import com.diozero.api.SerialDevice;
import com.diozero.api.SpiClockMode;
import com.diozero.internal.spi.AnalogInputDeviceInterface;
import com.diozero.internal.spi.AnalogOutputDeviceInterface;
import com.diozero.internal.spi.BaseNativeDeviceFactory;
import com.diozero.internal.spi.GpioDigitalInputDeviceInterface;
import com.diozero.internal.spi.GpioDigitalInputOutputDeviceInterface;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;
import com.diozero.internal.spi.InternalI2CDeviceInterface;
import com.diozero.internal.spi.InternalPwmOutputDeviceInterface;
import com.diozero.internal.spi.InternalSerialDeviceInterface;
import com.diozero.internal.spi.InternalServoDeviceInterface;
import com.diozero.internal.spi.InternalSpiDeviceInterface;
import com.diozero.remote.DiozeroProtosConverter;
import com.diozero.remote.grpc.GrpcConstants;
import com.diozero.remote.message.protobuf.Board;
import com.diozero.remote.message.protobuf.BoardServiceGrpc;
import com.diozero.remote.message.protobuf.BoardServiceGrpc.BoardServiceBlockingStub;
import com.diozero.remote.message.protobuf.BooleanResponse;
import com.diozero.remote.message.protobuf.FloatResponse;
import com.diozero.remote.message.protobuf.Gpio;
import com.diozero.remote.message.protobuf.GpioServiceGrpc;
import com.diozero.remote.message.protobuf.GpioServiceGrpc.GpioServiceBlockingStub;
import com.diozero.remote.message.protobuf.I2CServiceGrpc;
import com.diozero.remote.message.protobuf.I2CServiceGrpc.I2CServiceBlockingStub;
import com.diozero.remote.message.protobuf.IntegerArrayResponse;
import com.diozero.remote.message.protobuf.IntegerMessage;
import com.diozero.remote.message.protobuf.IntegerResponse;
import com.diozero.remote.message.protobuf.Response;
import com.diozero.remote.message.protobuf.SPIServiceGrpc;
import com.diozero.remote.message.protobuf.SPIServiceGrpc.SPIServiceBlockingStub;
import com.diozero.remote.message.protobuf.SerialServiceGrpc;
import com.diozero.remote.message.protobuf.SerialServiceGrpc.SerialServiceBlockingStub;
import com.diozero.remote.message.protobuf.Status;
import com.diozero.sbc.BoardInfo;
import com.diozero.util.DiozeroScheduler;
import com.diozero.util.PropertyUtil;
import com.google.protobuf.Empty;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

public class GrpcClientDeviceFactory extends BaseNativeDeviceFactory {
	public static final String NAME = "gRPC-Remote";

	private String serverHostname;
	private ManagedChannel channel;
	private BoardServiceBlockingStub boardBlockingStub;
	private GpioServiceBlockingStub gpioBlockingStub;
	// private GpioServiceStub gpioAsyncStub;
	private I2CServiceBlockingStub i2cBlockingStub;
	private SPIServiceBlockingStub spiBlockingStub;
	private SerialServiceBlockingStub serialBlockingStub;
	private int boardPwmFrequency;
	private int boardServoFrequency;
	private int spiBufferSize;
	private Map<Integer, Future<?>> subscriptions;

	public GrpcClientDeviceFactory() {
		this(PropertyUtil.getProperty(GrpcConstants.HOST_PROPERTY_NAME).orElseThrow(
				() -> new IllegalArgumentException("Error, " + GrpcConstants.HOST_PROPERTY_NAME + " not set")));
	}

	public GrpcClientDeviceFactory(String hostname) {
		this(hostname, PropertyUtil.getIntProperty(GrpcConstants.PORT_PROPERTY_NAME, GrpcConstants.DEFAULT_PORT));
	}

	public GrpcClientDeviceFactory(String hostname, int port) {
		this.serverHostname = hostname;

		channel = ManagedChannelBuilder.forAddress(serverHostname, port).usePlaintext().build();

		boardBlockingStub = BoardServiceGrpc.newBlockingStub(channel);
		gpioBlockingStub = GpioServiceGrpc.newBlockingStub(channel);
		// gpioAsyncStub = GpioServiceGrpc.newStub(channel);
		i2cBlockingStub = I2CServiceGrpc.newBlockingStub(channel);
		spiBlockingStub = SPIServiceGrpc.newBlockingStub(channel);
		serialBlockingStub = SerialServiceGrpc.newBlockingStub(channel);

		subscriptions = new ConcurrentHashMap<>();
	}

	public String getServerHostname() {
		return serverHostname;
	}

	@Override
	public void shutdown() {
		Logger.trace("shutdown()");
		channel.shutdown();
	}

	@Override
	public String getName() {
		return NAME;
	}

	I2CServiceBlockingStub getI2CServiceStub() {
		return i2cBlockingStub;
	}

	SPIServiceBlockingStub getSpiServiceStub() {
		return spiBlockingStub;
	}

	SerialServiceBlockingStub getSerialServiceStub() {
		return serialBlockingStub;
	}

	@Override
	protected BoardInfo lookupBoardInfo() {
		try {
			Board.BoardInfoResponse response = boardBlockingStub.getBoardInfo(Empty.newBuilder().build());
			if (response.getStatus() == Status.ERROR) {
				throw new RuntimeIOException("Error in remote gRPC invocation: " + response.getDetail());
			}

			boardPwmFrequency = response.getBoardPwmFrequency();
			boardServoFrequency = response.getBoardServoFrequency();
			spiBufferSize = response.getSpiBufferSize();

			return new RemoteBoardInfo(response);
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in get board info: " + e);
		}
	}

	@Override
	public int getBoardPwmFrequency() {
		return boardPwmFrequency;
	}

	@Override
	public void setBoardPwmFrequency(int frequency) {
		try {
			Response response = boardBlockingStub
					.setBoardPwmFrequency(IntegerMessage.newBuilder().setValue(frequency).build());
			if (response.getStatus() == Status.ERROR) {
				throw new RuntimeIOException("Error in remote gRPC invocation: " + response.getDetail());
			}
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in set board PWM frequency: " + e);
		}
	}

	@Override
	public int getBoardServoFrequency() {
		return boardServoFrequency;
	}

	@Override
	public void setBoardServoFrequency(int frequency) {
		try {
			Response response = boardBlockingStub
					.setBoardServoFrequency(IntegerMessage.newBuilder().setValue(frequency).build());
			if (response.getStatus() == Status.ERROR) {
				throw new RuntimeIOException("Error in remote gRPC invocation: " + response.getDetail());
			}
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in set board PWM frequency: " + e);
		}
	}

	@Override
	public int getSpiBufferSize() {
		return spiBufferSize;
	}

	@Override
	public DeviceMode getGpioMode(int gpio) {
		try {
			Board.GpioModeResponse response = boardBlockingStub
					.getGpioMode(Gpio.Identifier.newBuilder().setGpio(gpio).build());

			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in Board getGpioMode: " + response.getDetail());
			}

			return DiozeroProtosConverter.convert(response.getMode());
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in Board getGpioMode: " + e);
		}
	}

	@Override
	public int getGpioValue(int gpio) {
		try {
			IntegerResponse response = boardBlockingStub
					.getGpioValue(Gpio.Identifier.newBuilder().setGpio(gpio).build());

			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in Board getGpioValue: " + response.getDetail());
			}

			return response.getData();
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in Board getGpioValue: " + e);
		}
	}

	@Override
	public float getCpuTemperature() {
		try {
			FloatResponse response = boardBlockingStub.getCpuTemperature(Empty.newBuilder().build());

			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in Board getCpuTemperature: " + response.getDetail());
			}

			return response.getData();
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in Board getCpuTemperature: " + e);
		}
	}

	@Override
	public List<Integer> getI2CBusNumbers() {
		try {
			IntegerArrayResponse response = boardBlockingStub.getI2CBusNumbers(Empty.newBuilder().build());

			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in Board getI2CBusNumbers: " + response.getDetail());
			}

			return response.getDataList();
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in Board getI2CBusNumbers: " + e);
		}
	}

	@Override
	public int getI2CFunctionalities(int controller) {
		try {
			IntegerResponse response = boardBlockingStub
					.getI2CFunctionalities(IntegerMessage.newBuilder().setValue(controller).build());

			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in Board getI2CFunctionalities: " + response.getDetail());
			}

			return response.getData();
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in Board getI2CFunctionalities: " + e);
		}
	}

	@Override
	public GpioDigitalInputDeviceInterface createDigitalInputDevice(String key, PinInfo pinInfo, GpioPullUpDown pud,
			GpioEventTrigger trigger) {
		return new GrpcClientDigitalInputDevice(this, key, pinInfo, pud, trigger);
	}

	@Override
	public GpioDigitalOutputDeviceInterface createDigitalOutputDevice(String key, PinInfo pinInfo,
			boolean initialValue) {
		return new GrpcClientDigitalOutputDevice(this, key, pinInfo, initialValue);
	}

	@Override
	public GpioDigitalInputOutputDeviceInterface createDigitalInputOutputDevice(String key, PinInfo pinInfo,
			DeviceMode mode) {
		return new GrpcClientDigitalInputOutputDevice(this, key, pinInfo, mode);
	}

	@Override
	public InternalPwmOutputDeviceInterface createPwmOutputDevice(String key, PinInfo pinInfo, int pwmFrequency,
			float initialValue) {
		return new GrpcClientPwmOutputDevice(this, key, pinInfo, pwmFrequency, initialValue);
	}

	@Override
	public InternalServoDeviceInterface createServoDevice(String key, PinInfo pinInfo, int pwmFrequency,
			int minPulseWidthUs, int maxPulseWidthUs, int initialPulseWidthUs) {
		return new GrpcClientServoDevice(this, key, pinInfo, pwmFrequency, minPulseWidthUs, maxPulseWidthUs,
				initialPulseWidthUs);
	}

	@Override
	public AnalogInputDeviceInterface createAnalogInputDevice(String key, PinInfo pinInfo) {
		return new GrpcClientAnalogInputDevice(this, key, pinInfo);
	}

	@Override
	public AnalogOutputDeviceInterface createAnalogOutputDevice(String key, PinInfo pinInfo, float initialValue) {
		return new GrpcClientAnalogOutputDevice(this, key, pinInfo, initialValue);
	}

	@Override
	public InternalI2CDeviceInterface createI2CDevice(String key, int controller, int address,
			I2CConstants.AddressSize addressSize) throws RuntimeIOException {
		return new GrpcClientI2CDevice(this, key, controller, address, addressSize);
	}

	@Override
	public InternalSpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect, int frequency,
			SpiClockMode spiClockMode, boolean lsbFirst) throws RuntimeIOException {
		return new GrpcClientSpiDevice(this, key, controller, chipSelect, frequency, spiClockMode, lsbFirst);
	}

	@Override
	public InternalSerialDeviceInterface createSerialDevice(String key, String deviceFile, int baud,
			SerialDevice.DataBits dataBits, SerialDevice.StopBits stopBits, SerialDevice.Parity parity,
			boolean readBlocking, int minReadChars, int readTimeoutMillis) throws RuntimeIOException {
		return new GrpcClientSerialDevice(this, key, deviceFile, baud, dataBits, stopBits, parity, readBlocking,
				minReadChars, readTimeoutMillis);
	}

	//
	// GPIO shared operations
	//
	void provisionDigitalInputDevice(int gpio, GpioPullUpDown pud, GpioEventTrigger trigger) {
		try {
			Response response = gpioBlockingStub.provisionDigitalInputDevice(Gpio.ProvisionDigitalInputDeviceRequest
					.newBuilder().setGpio(gpio).setPud(DiozeroProtosConverter.convert(pud))
					.setTrigger(DiozeroProtosConverter.convert(trigger)).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in GPIO provision digital input device: " + response.getDetail());
			}
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in GPIO provision digital input device: " + e);
		}
	}

	void provisionDigitalOutputDevice(int gpio, boolean initialValue) {
		try {
			Response response = gpioBlockingStub.provisionDigitalOutputDevice(Gpio.ProvisionDigitalOutputDeviceRequest
					.newBuilder().setGpio(gpio).setInitialValue(initialValue).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in GPIO provision digital output device: " + response.getDetail());
			}
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in GPIO provision digital output device: " + e);
		}
	}

	void provisionDigitalInputOutputDevice(int gpio, boolean output) {
		try {
			Response response = gpioBlockingStub.provisionDigitalInputOutputDevice(
					Gpio.ProvisionDigitalInputOutputDeviceRequest.newBuilder().setGpio(gpio).setOutput(output).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException(
						"Error in GPIO provision digital input output device: " + response.getDetail());
			}
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in GPIO provision digital input output device: " + e);
		}
	}

	void provisionPwmOutputDevice(int gpio, int frequency, float initialValue) {
		try {
			Response response = gpioBlockingStub.provisionPwmOutputDevice(Gpio.ProvisionPwmOutputDeviceRequest
					.newBuilder().setGpio(gpio).setFrequency(frequency).setInitialValue(initialValue).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in GPIO provision digital PWM device: " + response.getDetail());
			}
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in GPIO provision digital PWM device: " + e);
		}
	}

	void provisionServoDevice(int gpio, int frequency, int minPulseWidthUs, int maxPulseWidthUs,
			int initialPulseWidthUs) {
		try {
			Response response = gpioBlockingStub.provisionServoDevice(Gpio.ProvisionServoDeviceRequest.newBuilder()
					.setGpio(gpio).setFrequency(frequency).setMinPulseWidthUs(minPulseWidthUs)
					.setMaxPulseWidthUs(maxPulseWidthUs).setInitialPulseWidthUs(initialPulseWidthUs).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in GPIO provision servo device: " + response.getDetail());
			}
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in GPIO provision servo device: " + e);
		}
	}

	void provisionAnalogInputDevice(int gpio) {
		try {
			Response response = gpioBlockingStub.provisionAnalogInputDevice(
					Gpio.ProvisionAnalogInputDeviceRequest.newBuilder().setGpio(gpio).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in GPIO provision analog input device: " + response.getDetail());
			}
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in GPIO provision analog input device: " + e);
		}
	}

	void provisionAnalogOutputDevice(int gpio, float initialValue) {
		try {
			Response response = gpioBlockingStub.provisionAnalogOutputDevice(Gpio.ProvisionAnalogOutputDeviceRequest
					.newBuilder().setGpio(gpio).setInitialValue(initialValue).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in GPIO provision analog output device: " + response.getDetail());
			}
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in GPIO provision analog output device: " + e);
		}
	}

	boolean digitalRead(int gpio) {
		try {
			BooleanResponse response = gpioBlockingStub.digitalRead(Gpio.Identifier.newBuilder().setGpio(gpio).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in GPIO digital read: " + response.getDetail());
			}

			return response.getData();
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in GPIO digital read: " + e);
		}
	}

	void digitalWrite(int gpio, boolean value) {
		try {
			Response response = gpioBlockingStub
					.digitalWrite(Gpio.BooleanMessage.newBuilder().setGpio(gpio).setValue(value).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in GPIO digital write: " + response.getDetail());
			}
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in GPIO digital write: " + e);
		}
	}

	float pwmRead(int gpio) {
		try {
			FloatResponse response = gpioBlockingStub.pwmRead(Gpio.Identifier.newBuilder().setGpio(gpio).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in GPIO PWM read: " + response.getDetail());
			}

			return response.getData();
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in GPIO PWM read: " + e);
		}
	}

	void pwmWrite(int gpio, float value) {
		try {
			Response response = gpioBlockingStub
					.pwmWrite(Gpio.FloatMessage.newBuilder().setGpio(gpio).setValue(value).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in GPIO PWM write: " + response.getDetail());
			}
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in GPIO PWM write: " + e);
		}
	}

	int servoRead(int gpio) {
		try {
			IntegerResponse response = gpioBlockingStub.servoRead(Gpio.Identifier.newBuilder().setGpio(gpio).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in GPIO Servo read: " + response.getDetail());
			}

			return response.getData();
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in GPIO Servo read: " + e);
		}
	}

	void servoWrite(int gpio, int value) {
		try {
			Response response = gpioBlockingStub
					.servoWrite(Gpio.IntegerMessage.newBuilder().setGpio(gpio).setValue(value).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in GPIO Servo write: " + response.getDetail());
			}
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in GPIO Servo write: " + e);
		}
	}

	int getPwmFrequency(int gpio) {
		try {
			IntegerResponse response = gpioBlockingStub
					.getPwmFrequency(Gpio.Identifier.newBuilder().setGpio(gpio).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in GPIO get PWM frequency: " + response.getDetail());
			}

			return response.getData();
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in GPIO get PWM frequency: " + e);
		}
	}

	void setPwmFrequency(int gpio, int frequency) {
		try {
			Response response = gpioBlockingStub
					.setPwmFrequency(Gpio.IntegerMessage.newBuilder().setGpio(gpio).setValue(frequency).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in GPIO set PWM frequency: " + response.getDetail());
			}
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in GPIO set PWM frequency: " + e);
		}
	}

	int getServoFrequency(int gpio) {
		try {
			IntegerResponse response = gpioBlockingStub
					.getServoFrequency(Gpio.Identifier.newBuilder().setGpio(gpio).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in GPIO get Servo frequency: " + response.getDetail());
			}

			return response.getData();
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in GPIO get Servo frequency: " + e);
		}
	}

	void setServoFrequency(int gpio, int frequency) {
		try {
			Response response = gpioBlockingStub
					.setServoFrequency(Gpio.IntegerMessage.newBuilder().setGpio(gpio).setValue(frequency).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in GPIO set Servo frequency: " + response.getDetail());
			}
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in GPIO set Servo frequency: " + e);
		}
	}

	float analogRead(int gpio) {
		try {
			FloatResponse response = gpioBlockingStub.analogRead(Gpio.Identifier.newBuilder().setGpio(gpio).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in GPIO analog read: " + response.getDetail());
			}

			return response.getData();
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in GPIO analog read: " + e);
		}
	}

	void analogWrite(int gpio, float value) {
		try {
			Response response = gpioBlockingStub
					.analogWrite(Gpio.FloatMessage.newBuilder().setGpio(gpio).setValue(value).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in GPIO analog write: " + response.getDetail());
			}
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in GPIO analog write: " + e);
		}
	}

	void setOutput(int gpio, boolean output) {
		try {
			Response response = gpioBlockingStub
					.setOutput(Gpio.BooleanMessage.newBuilder().setGpio(gpio).setValue(output).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in GPIO set outpute: " + response.getDetail());
			}
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in GPIO set output: " + e);
		}
	}

	void subscribe(int gpio) {
		Logger.trace("subscribe({})", Integer.valueOf(gpio));

		if (subscriptions.containsKey(Integer.valueOf(gpio))) {
			Logger.warn("Already subscribed to GPIO {}", Integer.valueOf(gpio));
			return;
		}

		Future<?> future = DiozeroScheduler.getNonDaemonInstance().submit(() -> {
			try {
				Iterator<Gpio.Event> it = gpioBlockingStub
						.subscribe(Gpio.Identifier.newBuilder().setGpio(gpio).build());
				do {
					Gpio.Event event = it.next();
					if (event.getGpio() == -1) {
						Logger.debug("Exiting subscription loop");
						break;
					}
					accept(new DigitalInputEvent(event.getGpio(), event.getEpochTime(), event.getNanoTime(),
							event.getValue()));
				} while (it.hasNext());
			} catch (StatusRuntimeException e) {
				Logger.error(e, "Subscribe request failed: {}", e);
				subscriptions.remove(Integer.valueOf(gpio));
			}
		});

		subscriptions.put(Integer.valueOf(gpio), future);
	}

	void unsubscribe(int gpio) {
		Logger.trace("unsubscribe({})", Integer.valueOf(gpio));

		Future<?> future = subscriptions.get(Integer.valueOf(gpio));
		if (future != null) {
			if (!future.isCancelled() && !future.isDone()) {
				try {
					Response response = gpioBlockingStub
							.unsubscribe(Gpio.Identifier.newBuilder().setGpio(gpio).build());
					if (response.getStatus() != Status.OK) {
						Logger.warn("Unsubscribe request failed: {}", response.getDetail());
					}
				} catch (StatusRuntimeException e) {
					Logger.error(e, "Unsubscribe request failed: {}", e);
				}
			}
			subscriptions.remove(Integer.valueOf(gpio));
		}
	}

	void closeGpio(int gpio) {
		try {
			Response response = gpioBlockingStub.close(Gpio.Identifier.newBuilder().setGpio(gpio).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in GPIO close: " + response.getDetail());
			}
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in GPIO close: " + e);
		}
	}

	public void accept(DigitalInputEvent event) {
		PinInfo pin_info = getBoardPinInfo().getByGpioNumberOrThrow(event.getGpio());
		GrpcClientDigitalInputDevice device = getDevice(createPinKey(pin_info));
		if (device != null) {
			device.accept(event);
		}
	}

	public void accept(AnalogInputEvent event) {
		PinInfo pin_info = getBoardPinInfo().getByGpioNumberOrThrow(event.getGpio());
		GrpcClientAnalogInputDevice device = getDevice(createPinKey(pin_info));
		if (device != null) {
			device.accept(event);
		}
	}

	static class RemoteBoardInfo extends BoardInfo {
		private Board.BoardInfoResponse boardInfoResponse;

		public RemoteBoardInfo(Board.BoardInfoResponse boardInfoResponse) {
			super(boardInfoResponse.getMake(), boardInfoResponse.getModel(), boardInfoResponse.getMemory(),
					boardInfoResponse.getOsId(), boardInfoResponse.getOsVersion());

			this.boardInfoResponse = boardInfoResponse;

			populateBoardPinInfo();
		}

		@Override
		public void populateBoardPinInfo() {
			for (Board.HeaderInfo header : boardInfoResponse.getHeaderList()) {
				for (Board.GpioInfo gpio_info : header.getGpioList()) {
					if (gpio_info.getModeList().contains(Board.GpioMode.PWM_OUTPUT)) {
						addPwmPinInfo(gpio_info.getHeader(), gpio_info.getGpioNumber(), gpio_info.getName(),
								gpio_info.getPhysicalPin(), gpio_info.getPwmChip(), gpio_info.getPwmNum(),
								DiozeroProtosConverter.convert(gpio_info.getModeList()), gpio_info.getChip(),
								gpio_info.getLineOffset());
					} else if (gpio_info.getModeList().contains(Board.GpioMode.DIGITAL_INPUT)
							|| gpio_info.getModeList().contains(Board.GpioMode.DIGITAL_OUTPUT)) {
						addGpioPinInfo(gpio_info.getHeader(), gpio_info.getGpioNumber(), gpio_info.getName(),
								gpio_info.getPhysicalPin(), DiozeroProtosConverter.convert(gpio_info.getModeList()),
								gpio_info.getChip(), gpio_info.getLineOffset());
					} else if (gpio_info.getModeList().contains(Board.GpioMode.ANALOG_INPUT)) {
						addAdcPinInfo(gpio_info.getHeader(), gpio_info.getGpioNumber(), gpio_info.getName(),
								gpio_info.getPhysicalPin(), gpio_info.getAdcVRef());
					} else if (gpio_info.getModeList().contains(Board.GpioMode.ANALOG_OUTPUT)) {
						addDacPinInfo(gpio_info.getHeader(), gpio_info.getGpioNumber(), gpio_info.getName(),
								gpio_info.getPhysicalPin());
					} else {
						addGeneralPinInfo(gpio_info.getHeader(), gpio_info.getPhysicalPin(), gpio_info.getName(),
								gpio_info.getChip(), gpio_info.getLineOffset());
					}
				}
			}
		}
	}
}
