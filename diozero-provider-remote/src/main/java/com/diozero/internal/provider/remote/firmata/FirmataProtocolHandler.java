package com.diozero.internal.provider.remote.firmata;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Remote Provider
 * Filename:     FirmataProtocolHandler.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2021 diozero
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.tinylog.Logger;

import com.diozero.api.AnalogInputEvent;
import com.diozero.api.DeviceMode;
import com.diozero.api.DigitalInputEvent;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.RuntimeIOException;
import com.diozero.api.SerialConstants;
import com.diozero.firmata.FirmataAdapter;
import com.diozero.firmata.FirmataAdapter.I2CResponse;
import com.diozero.firmata.FirmataEventListener;
import com.diozero.firmata.FirmataProtocol.PinCapability;
import com.diozero.firmata.FirmataProtocol.PinMode;
import com.diozero.firmata.SerialFirmataAdapter;
import com.diozero.firmata.SocketFirmataAdapter;
import com.diozero.internal.provider.remote.devicefactory.RemoteDeviceFactory;
import com.diozero.remote.message.GetBoardInfoRequest;
import com.diozero.remote.message.GetBoardInfoResponse;
import com.diozero.remote.message.GpioAnalogRead;
import com.diozero.remote.message.GpioAnalogReadResponse;
import com.diozero.remote.message.GpioAnalogWrite;
import com.diozero.remote.message.GpioClose;
import com.diozero.remote.message.GpioDigitalRead;
import com.diozero.remote.message.GpioDigitalReadResponse;
import com.diozero.remote.message.GpioDigitalWrite;
import com.diozero.remote.message.GpioEvents;
import com.diozero.remote.message.GpioGetPwmFrequency;
import com.diozero.remote.message.GpioGetPwmFrequencyResponse;
import com.diozero.remote.message.GpioInfo;
import com.diozero.remote.message.GpioPwmRead;
import com.diozero.remote.message.GpioPwmReadResponse;
import com.diozero.remote.message.GpioPwmWrite;
import com.diozero.remote.message.GpioSetPwmFrequency;
import com.diozero.remote.message.I2CBlockProcessCall;
import com.diozero.remote.message.I2CBooleanResponse;
import com.diozero.remote.message.I2CByteResponse;
import com.diozero.remote.message.I2CBytesResponse;
import com.diozero.remote.message.I2CClose;
import com.diozero.remote.message.I2COpen;
import com.diozero.remote.message.I2CProbe;
import com.diozero.remote.message.I2CProcessCall;
import com.diozero.remote.message.I2CReadBlockData;
import com.diozero.remote.message.I2CReadBlockDataResponse;
import com.diozero.remote.message.I2CReadByte;
import com.diozero.remote.message.I2CReadByteData;
import com.diozero.remote.message.I2CReadBytes;
import com.diozero.remote.message.I2CReadI2CBlockData;
import com.diozero.remote.message.I2CReadWordData;
import com.diozero.remote.message.I2CWordResponse;
import com.diozero.remote.message.I2CWriteBlockData;
import com.diozero.remote.message.I2CWriteByte;
import com.diozero.remote.message.I2CWriteByteData;
import com.diozero.remote.message.I2CWriteBytes;
import com.diozero.remote.message.I2CWriteI2CBlockData;
import com.diozero.remote.message.I2CWriteQuick;
import com.diozero.remote.message.I2CWriteWordData;
import com.diozero.remote.message.ProvisionAnalogInputDevice;
import com.diozero.remote.message.ProvisionAnalogOutputDevice;
import com.diozero.remote.message.ProvisionDigitalInputDevice;
import com.diozero.remote.message.ProvisionDigitalInputOutputDevice;
import com.diozero.remote.message.ProvisionDigitalOutputDevice;
import com.diozero.remote.message.ProvisionPwmOutputDevice;
import com.diozero.remote.message.RemoteProtocolInterface;
import com.diozero.remote.message.Response;
import com.diozero.remote.message.SerialBytesAvailable;
import com.diozero.remote.message.SerialBytesAvailableResponse;
import com.diozero.remote.message.SerialClose;
import com.diozero.remote.message.SerialOpen;
import com.diozero.remote.message.SerialRead;
import com.diozero.remote.message.SerialReadByte;
import com.diozero.remote.message.SerialReadByteResponse;
import com.diozero.remote.message.SerialReadBytes;
import com.diozero.remote.message.SerialReadBytesResponse;
import com.diozero.remote.message.SerialReadResponse;
import com.diozero.remote.message.SerialWriteByte;
import com.diozero.remote.message.SerialWriteBytes;
import com.diozero.remote.message.SpiClose;
import com.diozero.remote.message.SpiOpen;
import com.diozero.remote.message.SpiResponse;
import com.diozero.remote.message.SpiWrite;
import com.diozero.remote.message.SpiWriteAndRead;
import com.diozero.util.PropertyUtil;
import com.diozero.util.RangeUtil;

public class FirmataProtocolHandler implements RemoteProtocolInterface, FirmataEventListener {
	private static final String TCP_HOST_PROP = "FIRMATA_TCP_HOST";
	private static final String TCP_PORT_PROP = "FIRMATA_TCP_PORT";
	private static final String SERIAL_PORT_PROP = "FIRMATA_SERIAL_PORT";

	private static final int DEFAULT_TCP_PORT = 3030;

	private RemoteDeviceFactory deviceFactory;
	private FirmataAdapter adapter;

	public FirmataProtocolHandler(RemoteDeviceFactory deviceFactory) {
		this.deviceFactory = deviceFactory;

		String hostname = PropertyUtil.getProperty(TCP_HOST_PROP, null);
		if (hostname != null) {
			int port = PropertyUtil.getIntProperty(TCP_PORT_PROP, DEFAULT_TCP_PORT);
			try {
				adapter = new SocketFirmataAdapter(this, hostname, port);
			} catch (IOException e) {
				throw new RuntimeIOException(e);
			}
		} else {
			String serial_port = PropertyUtil.getProperty(SERIAL_PORT_PROP, null);
			if (serial_port != null) {
				//adapter = new SerialFirmataAdapter(this, serial_port, SerialConstants.BAUD_57600,
				//		SerialConstants.DataBits.CS8, SerialConstants.StopBits.ONE_STOP_BIT,
				//		SerialConstants.Parity.NO_PARITY, false, 1, 100);
				adapter = new SerialFirmataAdapter(this, serial_port, SerialConstants.BAUD_57600,
						SerialConstants.DataBits.CS8, SerialConstants.StopBits.ONE_STOP_BIT,
						SerialConstants.Parity.NO_PARITY, true, 1, 0);
			}
		}
		if (adapter == null) {
			Logger.error("Please set either {} or {} property", TCP_HOST_PROP, SERIAL_PORT_PROP);
			throw new IllegalArgumentException("Either " + TCP_HOST_PROP + " or " + SERIAL_PORT_PROP + " must be set");
		}
	}

	@Override
	public void start() {
		adapter.start();
	}

	@Override
	public GetBoardInfoResponse request(GetBoardInfoRequest request) {
		List<List<PinCapability>> board_capabilities = adapter.getBoardCapabilities();
		List<GpioInfo> board_gpio_info = new ArrayList<>();

		int gpio = 0;
		List<DeviceMode> modes;
		for (List<PinCapability> pin_capabilities : board_capabilities) {
			modes = new ArrayList<>();
			for (PinCapability pin_capability : pin_capabilities) {
				modes.add(convert(pin_capability.getMode()));
			}
			if (!modes.isEmpty()) {
				board_gpio_info.add(new GpioInfo(gpio, modes));
			}

			gpio++;
		}

		FirmataAdapter.FirmwareDetails firmware = adapter.getFirmware();

		return new GetBoardInfoResponse(firmware.getName(), "v" + firmware.getMajor() + "." + firmware.getMinor(), -1,
				board_gpio_info, request.getCorrelationId());
	}

	private static DeviceMode convert(PinMode mode) {
		switch (mode) {
		case DIGITAL_INPUT:
		case INPUT_PULLUP:
			return DeviceMode.DIGITAL_INPUT;
		case DIGITAL_OUTPUT:
			return DeviceMode.DIGITAL_OUTPUT;
		case ANALOG_INPUT:
			return DeviceMode.ANALOG_INPUT;
		case PWM:
			return DeviceMode.PWM_OUTPUT;
		default:
			return DeviceMode.UNKNOWN;
		}
	}

	@Override
	public Response request(ProvisionDigitalInputDevice request) {
		adapter.setPinMode(request.getGpio(),
				request.getPud() == GpioPullUpDown.PULL_UP ? PinMode.INPUT_PULLUP : PinMode.DIGITAL_INPUT);
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public Response request(ProvisionDigitalOutputDevice request) {
		adapter.setPinMode(request.getGpio(), PinMode.DIGITAL_OUTPUT);
		adapter.setDigitalValue(request.getGpio(), request.getInitialValue());
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public Response request(ProvisionDigitalInputOutputDevice request) {
		adapter.setPinMode(request.getGpio(), request.getOutput() ? PinMode.DIGITAL_OUTPUT : PinMode.DIGITAL_INPUT);
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public Response request(ProvisionPwmOutputDevice request) {
		adapter.setPinMode(request.getGpio(), PinMode.PWM);
		adapter.setValue(request.getGpio(), RangeUtil.map(request.getInitialValue(), 0f, 1f, 0,
				adapter.getMax(request.getGpio(), PinMode.PWM), true));
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public Response request(ProvisionAnalogInputDevice request) {
		adapter.setPinMode(request.getGpio(), PinMode.ANALOG_INPUT);
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public Response request(ProvisionAnalogOutputDevice request) {
		throw new UnsupportedOperationException("Analog output isn't supported, use PWM instead");
	}

	@Override
	public GpioDigitalReadResponse request(GpioDigitalRead request) {
		return new GpioDigitalReadResponse(adapter.getDigitalValue(request.getGpio()), UUID.randomUUID().toString());
	}

	@Override
	public Response request(GpioDigitalWrite request) {
		adapter.setDigitalValue(request.getGpio(), request.getValue());
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public GpioPwmReadResponse request(GpioPwmRead request) {
		float value = RangeUtil.map(adapter.getValue(request.getGpio()), 0,
				adapter.getMax(request.getGpio(), PinMode.PWM), 0f, 1f, true);
		return new GpioPwmReadResponse(value, UUID.randomUUID().toString());
	}

	@Override
	public Response request(GpioPwmWrite request) {
		adapter.setValue(request.getGpio(),
				RangeUtil.map(request.getValue(), 0f, 1f, 0, adapter.getMax(request.getGpio(), PinMode.PWM), true));
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public GpioGetPwmFrequencyResponse request(GpioGetPwmFrequency request) {
		throw new UnsupportedOperationException("Get PWM Frequency not supported in Firmata");
	}

	@Override
	public Response request(GpioSetPwmFrequency request) {
		throw new UnsupportedOperationException("Set PWM Frequency not supported in Firmata");
	}

	@Override
	public GpioAnalogReadResponse request(GpioAnalogRead request) {
		float value = RangeUtil.map(adapter.getValue(request.getGpio()), 0,
				adapter.getMax(request.getGpio(), PinMode.ANALOG_INPUT), 0f, 1f, true);
		return new GpioAnalogReadResponse(value, UUID.randomUUID().toString());
	}

	@Override
	public Response request(GpioAnalogWrite request) {
		// Firmata doesn't support analog output, use PWM instead
		adapter.setValue(request.getGpio(),
				RangeUtil.map(request.getValue(), 0f, 1f, 0, adapter.getMax(request.getGpio(), PinMode.PWM), true));
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public Response request(GpioEvents request) {
		adapter.enableDigitalReporting(request.getGpio(), request.getEnabled());
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public Response request(GpioClose request) {
		adapter.setPinMode(request.getGpio(), PinMode.DIGITAL_INPUT);
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public Response request(I2COpen request) {
		// Nothing to do?
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public I2CBooleanResponse request(I2CProbe request) {
		throw new UnsupportedOperationException("I2C probe not supported in Firmata");
	}

	@Override
	public I2CBooleanResponse request(I2CWriteQuick request) {
		throw new UnsupportedOperationException("I2C writeQuick not supported in Firmata");
	}

	@Override
	public I2CByteResponse request(I2CReadByte request) {
		FirmataAdapter.I2CResponse response = adapter.i2cRead(request.getAddress(), false, false, 1);
		byte[] data = response.getData();
		if (data.length != 1) {
			throw new RuntimeIOException("I2C Error: Expected to read 1 byte, got " + data.length);
		}
		return new I2CByteResponse(data[0], request.getCorrelationId());
	}

	@Override
	public Response request(I2CWriteByte request) {
		adapter.i2cWrite(request.getAddress(), false, false, new byte[] { request.getData() });
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public I2CBytesResponse request(I2CReadBytes request) {
		I2CResponse response = adapter.i2cRead(request.getAddress(), false, false, request.getLength());
		byte[] data = response.getData();
		if (data.length != request.getLength()) {
			throw new RuntimeIOException(
					"I2C Error: Expected to read " + request.getLength() + " bytes, got " + data.length);
		}
		return new I2CBytesResponse(data, request.getCorrelationId());
	}

	@Override
	public Response request(I2CWriteBytes request) {
		adapter.i2cWrite(request.getAddress(), false, false, request.getData());
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public I2CByteResponse request(I2CReadByteData request) {
		I2CResponse response = adapter.i2cReadData(request.getAddress(), false, false, request.getRegister(), 1);
		byte[] data = response.getData();
		if (data.length != 1) {
			throw new RuntimeIOException("I2C Error: Expected to read 1 byte, got " + data.length);
		}
		return new I2CByteResponse(data[0], request.getCorrelationId());
	}

	@Override
	public Response request(I2CWriteByteData request) {
		adapter.i2cWriteData(request.getAddress(), false, false, request.getRegister(),
				new byte[] { request.getData() });
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public I2CWordResponse request(I2CReadWordData request) {
		I2CResponse response = adapter.i2cReadData(request.getAddress(), false, false, request.getRegister(), 1);
		byte[] data = response.getData();
		if (data.length != 2) {
			throw new RuntimeIOException("I2C Error: Expected to read 2 byte2, got " + data.length);
		}
		return new I2CWordResponse((data[0] & 0xff) & ((data[1] & 0xff) >> 8), request.getCorrelationId());
	}

	@Override
	public Response request(I2CWriteWordData request) {
		adapter.i2cWriteData(request.getAddress(), false, false, request.getRegister(),
				new byte[] { (byte) (request.getData() & 0xff), (byte) ((request.getData() >> 8) & 0xff) });
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public I2CWordResponse request(I2CProcessCall request) {
		throw new UnsupportedOperationException("I2C processCall not supported in Firmata");
	}

	@Override
	public I2CReadBlockDataResponse request(I2CReadBlockData request) {
		throw new UnsupportedOperationException("I2C readBlockData not supported in Firmata");
	}

	@Override
	public Response request(I2CWriteBlockData request) {
		throw new UnsupportedOperationException("I2C writeBlockData not supported in Firmata");
	}

	@Override
	public I2CBytesResponse request(I2CBlockProcessCall request) {
		throw new UnsupportedOperationException("I2C blockProcessCall not supported in Firmata");
	}

	@Override
	public I2CBytesResponse request(I2CReadI2CBlockData request) {
		I2CResponse response = adapter.i2cReadData(request.getAddress(), false, false, request.getRegister(),
				request.getLength());
		byte[] data = response.getData();
		if (data.length != request.getLength()) {
			throw new RuntimeIOException(
					"I2C Error: Expected to read " + request.getLength() + " bytes, got " + data.length);
		}
		return new I2CBytesResponse(data, request.getCorrelationId());
	}

	@Override
	public Response request(I2CWriteI2CBlockData request) {
		adapter.i2cWriteData(request.getAddress(), false, false, request.getRegister(), request.getData());
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public Response request(I2CClose request) {
		// Nothing to do?
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public Response request(SpiOpen request) {
		throw new UnsupportedOperationException("SPI support not yet included in StandardFirmata");
	}

	@Override
	public Response request(SpiWrite request) {
		throw new UnsupportedOperationException("SPI support not yet included in StandardFirmata");
	}

	@Override
	public SpiResponse request(SpiWriteAndRead request) {
		throw new UnsupportedOperationException("SPI support not yet included in StandardFirmata");
	}

	@Override
	public Response request(SpiClose request) {
		throw new UnsupportedOperationException("SPI support not yet included in StandardFirmata");
	}

	@Override
	public Response request(SerialOpen request) {
		throw new UnsupportedOperationException("Serial support not yet included in StandardFirmata");
	}

	@Override
	public SerialReadResponse request(SerialRead request) {
		throw new UnsupportedOperationException("Serial support not yet included in StandardFirmata");
	}

	@Override
	public SerialReadByteResponse request(SerialReadByte request) {
		throw new UnsupportedOperationException("Serial support not yet included in StandardFirmata");
	}

	@Override
	public Response request(SerialWriteByte request) {
		throw new UnsupportedOperationException("Serial support not yet included in StandardFirmata");
	}

	@Override
	public SerialReadBytesResponse request(SerialReadBytes request) {
		throw new UnsupportedOperationException("Serial support not yet included in StandardFirmata");
	}

	@Override
	public Response request(SerialWriteBytes request) {
		throw new UnsupportedOperationException("Serial support not yet included in StandardFirmata");
	}

	@Override
	public SerialBytesAvailableResponse request(SerialBytesAvailable request) {
		throw new UnsupportedOperationException("Serial support not yet included in StandardFirmata");
	}

	@Override
	public Response request(SerialClose request) {
		throw new UnsupportedOperationException("Serial support not yet included in StandardFirmata");
	}

	@Override
	public void close() {
		adapter.close();
	}

	@Override
	public void event(FirmataEventListener.EventType eventType, int gpio, int value, long epochTime, long nanoTime) {
		switch (eventType) {
		case DIGITAL:
			deviceFactory.accept(new DigitalInputEvent(gpio, epochTime, nanoTime, value != 0));
			break;
		case ANALOG:
			float f = RangeUtil.map(value, 0, adapter.getMax(gpio, PinMode.ANALOG_INPUT), 0f, 1f, true);
			deviceFactory.accept(new AnalogInputEvent(gpio, epochTime, nanoTime, f));
			break;
		default:
		}
	}
}
