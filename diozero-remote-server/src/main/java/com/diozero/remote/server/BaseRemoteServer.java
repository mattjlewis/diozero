package com.diozero.remote.server;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Remote Server
 * Filename:     BaseRemoteServer.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.DigitalInputEvent;
import com.diozero.api.InputEventListener;
import com.diozero.api.PinInfo;
import com.diozero.internal.provider.AnalogInputDeviceInterface;
import com.diozero.internal.provider.AnalogOutputDeviceInterface;
import com.diozero.internal.provider.DeviceInterface;
import com.diozero.internal.provider.GpioDigitalDeviceInterface;
import com.diozero.internal.provider.GpioDigitalInputDeviceInterface;
import com.diozero.internal.provider.GpioDigitalInputOutputDeviceInterface;
import com.diozero.internal.provider.GpioDigitalOutputDeviceInterface;
import com.diozero.internal.provider.I2CDeviceInterface;
import com.diozero.internal.provider.NativeDeviceFactoryInterface;
import com.diozero.internal.provider.PwmOutputDeviceInterface;
import com.diozero.internal.provider.SerialDeviceInterface;
import com.diozero.internal.provider.SpiDeviceInterface;
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
import com.diozero.remote.message.GpioInfo;
import com.diozero.remote.message.GpioPwmRead;
import com.diozero.remote.message.GpioPwmReadResponse;
import com.diozero.remote.message.GpioPwmWrite;
import com.diozero.remote.message.I2CClose;
import com.diozero.remote.message.I2COpen;
import com.diozero.remote.message.I2CRead;
import com.diozero.remote.message.I2CReadByte;
import com.diozero.remote.message.I2CReadByteData;
import com.diozero.remote.message.I2CReadByteDataResponse;
import com.diozero.remote.message.I2CReadByteResponse;
import com.diozero.remote.message.I2CReadI2CBlockData;
import com.diozero.remote.message.I2CReadI2CBlockDataResponse;
import com.diozero.remote.message.I2CReadResponse;
import com.diozero.remote.message.I2CWrite;
import com.diozero.remote.message.I2CWriteByte;
import com.diozero.remote.message.I2CWriteByteData;
import com.diozero.remote.message.I2CWriteI2CBlockData;
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
import com.diozero.util.BoardInfo;
import com.diozero.util.DeviceFactoryHelper;
import com.diozero.util.RuntimeIOException;

@SuppressWarnings("resource")
public abstract class BaseRemoteServer implements InputEventListener<DigitalInputEvent>, RemoteProtocolInterface {
	private NativeDeviceFactoryInterface deviceFactory;

	public BaseRemoteServer() {
		this(DeviceFactoryHelper.getNativeDeviceFactory());
	}

	public BaseRemoteServer(NativeDeviceFactoryInterface deviceFactory) {
		this.deviceFactory = deviceFactory;
	}

	@Override
	public GetBoardInfoResponse request(GetBoardInfoRequest request) {
		BoardInfo board_info = deviceFactory.getBoardInfo();

		Collection<PinInfo> gpio_pins = board_info.getGpioPins();
		Collection<PinInfo> adc_pins = board_info.getAdcPins();
		Collection<PinInfo> dac_pins = board_info.getDacPins();

		// TODO Added board pin info to the API
		List<GpioInfo> gpios = new ArrayList<>();

		GetBoardInfoResponse response = new GetBoardInfoResponse(board_info.getMake(), board_info.getModel(),
				board_info.getMemory(), gpios, request.getCorrelationId());

		return response;
	}

	@Override
	public Response request(ProvisionDigitalInputDevice request) {
		Logger.debug("GPIO input request");

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumber(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		DeviceInterface device = deviceFactory.getDevice(key);

		Response response;
		try {
			if (device == null) {
				deviceFactory.provisionDigitalInputDevice(pin_info, request.getPud(), request.getTrigger());
				response = new Response(Response.Status.OK, null, request.getCorrelationId());
			} else if (device instanceof GpioDigitalInputOutputDeviceInterface) {
				GpioDigitalInputOutputDeviceInterface inout = (GpioDigitalInputOutputDeviceInterface) device;
				inout.setMode(DeviceMode.DIGITAL_INPUT);

				response = new Response(Response.Status.OK, null, request.getCorrelationId());
			} else if (device instanceof GpioDigitalInputDeviceInterface) {
				// TODO Update the pud / trigger?
				response = new Response(Response.Status.OK, null, request.getCorrelationId());
			} else {
				response = new Response(Response.Status.ERROR, "GPIO already provisioned", request.getCorrelationId());
			}
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new Response(Response.Status.ERROR, "Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public Response request(ProvisionDigitalOutputDevice request) {
		Logger.debug("GPIO output request");

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumber(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		DeviceInterface device = deviceFactory.getDevice(key);

		Response response;
		try {
			if (device == null) {
				deviceFactory.provisionDigitalOutputDevice(pin_info, request.getInitialValue());

				response = new Response(Response.Status.OK, null, request.getCorrelationId());
			} else if (device instanceof GpioDigitalInputOutputDeviceInterface) {
				GpioDigitalInputOutputDeviceInterface inout = (GpioDigitalInputOutputDeviceInterface) device;
				inout.setMode(DeviceMode.DIGITAL_OUTPUT);
				inout.setValue(request.getInitialValue());

				response = new Response(Response.Status.OK, null, request.getCorrelationId());
			} else if (device instanceof GpioDigitalOutputDeviceInterface) {
				((GpioDigitalOutputDeviceInterface) device).setValue(request.getInitialValue());

				response = new Response(Response.Status.OK, null, request.getCorrelationId());
			} else {
				response = new Response(Response.Status.ERROR, "GPIO already provisioned", request.getCorrelationId());
			}
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new Response(Response.Status.ERROR, "Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public Response request(ProvisionDigitalInputOutputDevice request) {
		Logger.debug("GPIO input/output request");

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumber(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		DeviceInterface device = deviceFactory.getDevice(key);

		Response response;
		try {
			if (device == null) {
				deviceFactory.provisionDigitalInputOutputDevice(pin_info,
						request.getOutput() ? DeviceMode.DIGITAL_OUTPUT : DeviceMode.DIGITAL_INPUT);

				response = new Response(Response.Status.OK, null, request.getCorrelationId());
			} else if (device instanceof GpioDigitalInputOutputDeviceInterface) {
				GpioDigitalInputOutputDeviceInterface inout = (GpioDigitalInputOutputDeviceInterface) device;
				inout.setMode(request.getOutput() ? DeviceMode.DIGITAL_OUTPUT : DeviceMode.DIGITAL_INPUT);

				response = new Response(Response.Status.OK, null, request.getCorrelationId());
			} else {
				response = new Response(Response.Status.ERROR, "GPIO already provisioned", request.getCorrelationId());
			}
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new Response(Response.Status.ERROR, "Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public Response request(ProvisionPwmOutputDevice request) {
		Logger.debug("PWM output request");

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumber(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		DeviceInterface device = deviceFactory.getDevice(key);

		Response response;
		try {
			if (device == null) {
				deviceFactory.provisionPwmOutputDevice(request.getGpio(), request.getFrequency(),
						request.getInitialValue());

				response = new Response(Response.Status.OK, null, request.getCorrelationId());
			} else if (device instanceof PwmOutputDeviceInterface) {
				((PwmOutputDeviceInterface) device).setValue(request.getInitialValue());
				response = new Response(Response.Status.OK, null, request.getCorrelationId());
			} else {
				response = new Response(Response.Status.ERROR, "GPIO already provisioned", request.getCorrelationId());
			}
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new Response(Response.Status.ERROR, "Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public Response request(ProvisionAnalogInputDevice request) {
		Logger.debug("Analog input request");

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumber(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		DeviceInterface device = deviceFactory.getDevice(key);

		Response response;
		try {
			if (device == null) {
				deviceFactory.provisionAnalogInputDevice(request.getGpio());
				response = new Response(Response.Status.OK, null, request.getCorrelationId());
			} else if (device instanceof AnalogInputDeviceInterface) {
				response = new Response(Response.Status.OK, null, request.getCorrelationId());
			} else {
				response = new Response(Response.Status.ERROR, "GPIO already provisioned", request.getCorrelationId());
			}
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new Response(Response.Status.ERROR, "Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public Response request(ProvisionAnalogOutputDevice request) {
		Logger.debug("Analog output request");

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumber(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		DeviceInterface device = deviceFactory.getDevice(key);

		Response response;
		try {
			if (device == null) {
				AnalogOutputDeviceInterface output = deviceFactory.provisionAnalogOutputDevice(request.getGpio(),
						request.getInitialValue());
				output.setValue(request.getInitialValue());

				response = new Response(Response.Status.OK, null, request.getCorrelationId());
			} else if (device instanceof AnalogOutputDeviceInterface) {
				((AnalogOutputDeviceInterface) device).setValue(request.getInitialValue());

				response = new Response(Response.Status.OK, null, request.getCorrelationId());
			} else {
				response = new Response(Response.Status.ERROR, "GPIO already provisioned", request.getCorrelationId());
			}
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new Response(Response.Status.ERROR, "Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public GpioDigitalReadResponse request(GpioDigitalRead request) {
		Logger.debug("GPIO digital read request");

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumber(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		DeviceInterface device = deviceFactory.getDevice(key);

		GpioDigitalReadResponse response;
		if (device == null) {
			return new GpioDigitalReadResponse("GPIO not provisioned", request.getCorrelationId());
		}

		try {
			response = new GpioDigitalReadResponse(((GpioDigitalDeviceInterface) device).getValue(),
					request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new GpioDigitalReadResponse("Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public Response request(GpioDigitalWrite request) {
		Logger.debug("GPIO digital write request");

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumber(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		DeviceInterface device = deviceFactory.getDevice(key);

		if (device == null) {
			return new Response(Response.Status.ERROR, "GPIO not provisioned", request.getCorrelationId());
		}

		Response response;
		// Also covers digital input / output device
		if (device instanceof GpioDigitalOutputDeviceInterface) {
			try {
				((GpioDigitalOutputDeviceInterface) device).setValue(request.getValue());
				response = new Response(Response.Status.OK, null, request.getCorrelationId());
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
				response = new Response(Response.Status.ERROR, "Runtime Error: " + e, request.getCorrelationId());
			}
		} else {
			response = new Response(Response.Status.ERROR, "Invalid mode, device class: " + device.getClass().getName(),
					request.getCorrelationId());
		}

		return response;
	}

	@Override
	public GpioPwmReadResponse request(GpioPwmRead request) {
		Logger.debug("GPIO PWM read request");

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumber(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		DeviceInterface device = deviceFactory.getDevice(key);

		GpioPwmReadResponse response;
		if (device == null) {
			return new GpioPwmReadResponse("GPIO not provisioned", request.getCorrelationId());
		}

		try {
			response = new GpioPwmReadResponse(((PwmOutputDeviceInterface) device).getValue(),
					request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new GpioPwmReadResponse("Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public Response request(GpioPwmWrite request) {
		Logger.debug("GPIO PWM write request");

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumber(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		DeviceInterface device = deviceFactory.getDevice(key);

		if (device == null) {
			return new Response(Response.Status.ERROR, "GPIO not provisioned", request.getCorrelationId());
		}

		Response response;
		if (device instanceof PwmOutputDeviceInterface) {
			try {
				((PwmOutputDeviceInterface) device).setValue(request.getValue());
				response = new Response(Response.Status.OK, null, request.getCorrelationId());
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
				response = new Response(Response.Status.ERROR, "Runtime Error: " + e, request.getCorrelationId());
			}
		} else {
			response = new Response(Response.Status.ERROR, "Invalid mode, device class: " + device.getClass().getName(),
					request.getCorrelationId());
		}

		return response;
	}

	@Override
	public GpioAnalogReadResponse request(GpioAnalogRead request) {
		Logger.debug("GPIO analog read request");

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumber(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		DeviceInterface device = deviceFactory.getDevice(key);

		GpioAnalogReadResponse response;
		if (device == null) {
			return new GpioAnalogReadResponse("GPIO not provisioned", request.getCorrelationId());
		}

		try {
			response = new GpioAnalogReadResponse(((AnalogOutputDeviceInterface) device).getValue(),
					request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new GpioAnalogReadResponse("Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public Response request(GpioAnalogWrite request) {
		Logger.debug("GPIO analog write request");

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumber(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		DeviceInterface device = deviceFactory.getDevice(key);

		if (device == null) {
			return new Response(Response.Status.ERROR, "GPIO not provisioned", request.getCorrelationId());
		}

		Response response;
		if (device instanceof AnalogOutputDeviceInterface) {
			try {
				((AnalogOutputDeviceInterface) device).setValue(request.getValue());
				response = new Response(Response.Status.OK, null, request.getCorrelationId());
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
				response = new Response(Response.Status.ERROR, "Runtime Error: " + e, request.getCorrelationId());
			}
		} else {
			response = new Response(Response.Status.ERROR, "Invalid mode, device class: " + device.getClass().getName(),
					request.getCorrelationId());
		}

		return response;
	}

	@Override
	public Response request(GpioEvents request) {
		Logger.debug("GPIO events request");

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumber(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		DeviceInterface device = deviceFactory.getDevice(key);

		if (device == null) {
			return new Response(Response.Status.ERROR, "GPIO not provisioned", request.getCorrelationId());
		}

		Response response;
		try {
			if (device instanceof GpioDigitalInputDeviceInterface) {
				GpioDigitalInputDeviceInterface input = (GpioDigitalInputDeviceInterface) device;
				if (request.getEnabled()) {
					input.setListener(this);
				} else {
					input.removeListener();
				}

				response = new Response(Response.Status.OK, null, request.getCorrelationId());
			} else if (device instanceof GpioDigitalInputOutputDeviceInterface) {
				GpioDigitalInputOutputDeviceInterface inout = (GpioDigitalInputOutputDeviceInterface) device;
				if (request.getEnabled()) {
					inout.setListener(this);
				} else {
					inout.removeListener();
				}

				response = new Response(Response.Status.OK, null, request.getCorrelationId());
			} else {
				response = new Response(Response.Status.ERROR,
						"Invalid mode, device class: " + device.getClass().getName(), request.getCorrelationId());
			}
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new Response(Response.Status.ERROR, "Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public Response request(GpioClose request) {
		Logger.debug("GPIO close request");

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumber(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		DeviceInterface device = deviceFactory.getDevice(key);

		if (device == null) {
			return new Response(Response.Status.ERROR, "GPIO not provisioned", request.getCorrelationId());
		}

		Response response;
		try {
			device.close();

			response = new Response(Response.Status.OK, null, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new Response(Response.Status.ERROR, "Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public Response request(I2COpen request) {
		Logger.debug("I2C open request {}", request);

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		DeviceInterface device = deviceFactory.getDevice(key);
		if (device != null) {
			return new Response(Response.Status.ERROR, "I2C device already provisioned", request.getCorrelationId());
		}

		Response response;
		try {
			deviceFactory.provisionI2CDevice(controller, address, request.getAddressSize());

			response = new Response(Response.Status.OK, null, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new Response(Response.Status.ERROR, "Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public I2CReadByteResponse request(I2CReadByte request) {
		Logger.debug("I2C read byte request");

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		I2CDeviceInterface device = deviceFactory.getDevice(key, I2CDeviceInterface.class);
		if (device == null) {
			return new I2CReadByteResponse("I2C device not provisioned", request.getCorrelationId());
		}

		I2CReadByteResponse response;
		try {
			byte data = device.readByte();

			response = new I2CReadByteResponse(data, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new I2CReadByteResponse("Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public Response request(I2CWriteByte request) {
		Logger.debug("I2C write byte request");

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		I2CDeviceInterface device = deviceFactory.getDevice(key, I2CDeviceInterface.class);
		if (device == null) {
			return new Response(Response.Status.ERROR, "I2C device not provisioned", request.getCorrelationId());
		}

		Response response;
		try {
			device.writeByte(request.getData());

			response = new Response(Response.Status.OK, null, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new Response(Response.Status.ERROR, "Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public I2CReadResponse request(I2CRead request) {
		Logger.debug("I2C read request");

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		I2CDeviceInterface device = deviceFactory.getDevice(key, I2CDeviceInterface.class);
		if (device == null) {
			return new I2CReadResponse("I2C device not provisioned", request.getCorrelationId());
		}

		I2CReadResponse response;
		try {
			ByteBuffer buffer = ByteBuffer.allocate(request.getLength());
			device.read(buffer);

			byte[] data = new byte[buffer.remaining()];
			buffer.get(data);
			response = new I2CReadResponse(data, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new I2CReadResponse("Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public Response request(I2CWrite request) {
		Logger.debug("I2C write request");

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		I2CDeviceInterface device = deviceFactory.getDevice(key, I2CDeviceInterface.class);
		if (device == null) {
			return new Response(Response.Status.ERROR, "I2C device not provisioned", request.getCorrelationId());
		}

		Response response;
		try {
			byte[] data = request.getData();
			device.write(ByteBuffer.wrap(data));

			response = new Response(Response.Status.OK, null, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new Response(Response.Status.ERROR, "Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public I2CReadByteDataResponse request(I2CReadByteData request) {
		Logger.debug("I2C read byte data request");

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		I2CDeviceInterface device = deviceFactory.getDevice(key, I2CDeviceInterface.class);
		if (device == null) {
			return new I2CReadByteDataResponse("I2C device not provisioned", request.getCorrelationId());
		}

		I2CReadByteDataResponse response;
		try {
			byte data = device.readByteData(request.getRegister());

			response = new I2CReadByteDataResponse(data, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new I2CReadByteDataResponse("Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public Response request(I2CWriteByteData request) {
		Logger.debug("I2C write byte data request");

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		I2CDeviceInterface device = deviceFactory.getDevice(key, I2CDeviceInterface.class);
		if (device == null) {
			return new Response(Response.Status.ERROR, "I2C device not provisioned", request.getCorrelationId());
		}

		Response response;
		try {
			device.writeByteData(request.getRegister(), request.getData());

			response = new Response(Response.Status.OK, null, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new Response(Response.Status.ERROR, "Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public I2CReadI2CBlockDataResponse request(I2CReadI2CBlockData request) {
		Logger.debug("I2C read I2C block data request");

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		I2CDeviceInterface device = deviceFactory.getDevice(key, I2CDeviceInterface.class);
		if (device == null) {
			return new I2CReadI2CBlockDataResponse("I2C device not provisioned", request.getCorrelationId());
		}

		I2CReadI2CBlockDataResponse response;
		try {
			ByteBuffer buffer = ByteBuffer.allocate(request.getLength());
			device.readI2CBlockData(request.getRegister(), request.getSubAddressSize(), buffer);

			byte[] data = new byte[buffer.remaining()];
			buffer.get(data);
			response = new I2CReadI2CBlockDataResponse(data, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new I2CReadI2CBlockDataResponse("Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public Response request(I2CWriteI2CBlockData request) {
		Logger.debug("I2C write I2C block data request");

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		I2CDeviceInterface device = deviceFactory.getDevice(key, I2CDeviceInterface.class);
		if (device == null) {
			return new Response(Response.Status.ERROR, "I2C device not provisioned", request.getCorrelationId());
		}

		Response response;
		try {
			byte[] data = request.getData();
			device.writeI2CBlockData(request.getRegister(), request.getSubAddressSize(), ByteBuffer.wrap(data));

			response = new Response(Response.Status.OK, null, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new Response(Response.Status.ERROR, "Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public Response request(I2CClose request) {
		Logger.debug("I2C close request");

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		I2CDeviceInterface device = deviceFactory.getDevice(key, I2CDeviceInterface.class);
		if (device == null) {
			return new SpiResponse("I2C device not provisioned", request.getCorrelationId());
		}

		Response response;
		try {
			device.close();

			response = new Response(Response.Status.OK, null, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new Response(Response.Status.ERROR, "Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public Response request(SpiOpen request) {
		Logger.debug("SPI open request {}", request);

		int controller = request.getController();
		int chip_select = request.getChipSelect();
		String key = deviceFactory.createSpiKey(controller, chip_select);

		DeviceInterface device = deviceFactory.getDevice(key);
		if (device != null) {
			return new Response(Response.Status.ERROR, "SPI device already provisioned", request.getCorrelationId());
		}

		Response response;
		try {
			deviceFactory.provisionSpiDevice(controller, chip_select, request.getFrequency(), request.getClockMode(),
					request.getLsbFirst());

			response = new Response(Response.Status.OK, null, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new Response(Response.Status.ERROR, "Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public Response request(SpiWrite request) {
		Logger.debug("SPI write request");

		int controller = request.getController();
		int chip_select = request.getChipSelect();
		String key = deviceFactory.createSpiKey(controller, chip_select);

		SpiDeviceInterface device = deviceFactory.getDevice(key, SpiDeviceInterface.class);
		if (device == null) {
			return new Response(Response.Status.ERROR, "SPI device not provisioned", request.getCorrelationId());
		}

		Response response;
		try {
			byte[] data = request.getTxData();
			long start = System.currentTimeMillis();
			device.write(data);
			long duration = System.currentTimeMillis() - start;
			System.out.println("Inner time: " + duration + " ms for " + data.length + " bytes, class: "
					+ device.getClass().getName());

			response = new Response(Response.Status.OK, null, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new Response(Response.Status.ERROR, "Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public SpiResponse request(SpiWriteAndRead request) {
		Logger.debug("SPI write and read request");

		int controller = request.getController();
		int chip_select = request.getChipSelect();
		String key = deviceFactory.createSpiKey(controller, chip_select);

		SpiDeviceInterface device = deviceFactory.getDevice(key, SpiDeviceInterface.class);
		if (device == null) {
			return new SpiResponse("SPI device not provisioned", request.getCorrelationId());
		}

		SpiResponse response;
		try {
			byte[] rx_data = device.writeAndRead(request.getTxData());

			response = new SpiResponse(rx_data, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new SpiResponse("Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public Response request(SpiClose request) {
		Logger.debug("SPI close request");

		int controller = request.getController();
		int chip_select = request.getChipSelect();
		String key = deviceFactory.createSpiKey(controller, chip_select);

		SpiDeviceInterface device = deviceFactory.getDevice(key, SpiDeviceInterface.class);
		if (device == null) {
			return new SpiResponse("SPI device not provisioned", request.getCorrelationId());
		}

		Response response;
		try {
			device.close();

			response = new Response(Response.Status.OK, null, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new Response(Response.Status.ERROR, "Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public Response request(SerialOpen request) {
		Logger.debug("Serial open request {}", request);

		String device_file = request.getDeviceFile();
		String key = deviceFactory.createSerialKey(device_file);

		DeviceInterface device = deviceFactory.getDevice(key);
		if (device != null) {
			return new Response(Response.Status.ERROR, "Serial device already provisioned", request.getCorrelationId());
		}

		Response response;
		try {
			deviceFactory.provisionSerialDevice(device_file, request.getBaud(), request.getDataBits(),
					request.getStopBits(), request.getParity(), request.isReadBlocking(), request.getMinReadChars(),
					request.getReadTimeoutMillis());

			response = new Response(Response.Status.OK, null, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new Response(Response.Status.ERROR, "Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public SerialReadResponse request(SerialRead request) {
		Logger.debug("Serial read request");

		String device_file = request.getDeviceFile();
		String key = deviceFactory.createSerialKey(device_file);

		SerialDeviceInterface device = deviceFactory.getDevice(key, SerialDeviceInterface.class);
		if (device == null) {
			return new SerialReadResponse("Serial device not provisioned", request.getCorrelationId());
		}

		SerialReadResponse response;
		try {
			int data = device.read();

			response = new SerialReadResponse(data, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new SerialReadResponse("Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public SerialReadByteResponse request(SerialReadByte request) {
		Logger.debug("Serial read byte request");

		String device_file = request.getDeviceFile();
		String key = deviceFactory.createSerialKey(device_file);

		SerialDeviceInterface device = deviceFactory.getDevice(key, SerialDeviceInterface.class);
		if (device == null) {
			return new SerialReadByteResponse("Serial device not provisioned", request.getCorrelationId());
		}

		SerialReadByteResponse response;
		try {
			byte data = device.readByte();

			response = new SerialReadByteResponse(data, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new SerialReadByteResponse("Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public Response request(SerialWriteByte request) {
		Logger.debug("Serial write byte request");

		String device_file = request.getDeviceFile();
		String key = deviceFactory.createSerialKey(device_file);

		SerialDeviceInterface device = deviceFactory.getDevice(key, SerialDeviceInterface.class);
		if (device == null) {
			return new Response(Response.Status.ERROR, "Serial device not provisioned", request.getCorrelationId());
		}

		Response response;
		try {
			device.writeByte(request.getData());

			response = new Response(Response.Status.OK, null, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new Response(Response.Status.ERROR, "Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public SerialReadBytesResponse request(SerialReadBytes request) {
		Logger.debug("Serial read bytes request");

		String device_file = request.getDeviceFile();
		String key = deviceFactory.createSerialKey(device_file);

		SerialDeviceInterface device = deviceFactory.getDevice(key, SerialDeviceInterface.class);
		if (device == null) {
			return new SerialReadBytesResponse("Serial device not provisioned", request.getCorrelationId());
		}

		SerialReadBytesResponse response;
		try {
			byte[] data = new byte[request.getLength()];
			device.read(data);

			response = new SerialReadBytesResponse(data, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new SerialReadBytesResponse("Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public Response request(SerialWriteBytes request) {
		Logger.debug("Serial write bytes request");

		String device_file = request.getDeviceFile();
		String key = deviceFactory.createSerialKey(device_file);

		SerialDeviceInterface device = deviceFactory.getDevice(key, SerialDeviceInterface.class);
		if (device == null) {
			return new Response(Response.Status.ERROR, "Serial device not provisioned", request.getCorrelationId());
		}

		Response response;
		try {
			device.write(request.getData());

			response = new Response(Response.Status.OK, null, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new Response(Response.Status.ERROR, "Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public SerialBytesAvailableResponse request(SerialBytesAvailable request) {
		Logger.debug("Serial bytes available request");

		String device_file = request.getDeviceFile();
		String key = deviceFactory.createSerialKey(device_file);

		SerialDeviceInterface device = deviceFactory.getDevice(key, SerialDeviceInterface.class);
		if (device == null) {
			return new SerialBytesAvailableResponse("Serial device not provisioned", request.getCorrelationId());
		}

		SerialBytesAvailableResponse response;
		try {
			int bytes_available = device.bytesAvailable();

			response = new SerialBytesAvailableResponse(bytes_available, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new SerialBytesAvailableResponse("Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public Response request(SerialClose request) {
		Logger.debug("Serial close request");

		String device_file = request.getDeviceFile();
		String key = deviceFactory.createSerialKey(device_file);

		SerialDeviceInterface device = deviceFactory.getDevice(key, SerialDeviceInterface.class);
		if (device == null) {
			return new SpiResponse("Serial device not provisioned", request.getCorrelationId());
		}

		Response response;
		try {
			device.close();

			response = new Response(Response.Status.OK, null, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new Response(Response.Status.ERROR, "Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public void close() {
		deviceFactory.close();
	}
}
