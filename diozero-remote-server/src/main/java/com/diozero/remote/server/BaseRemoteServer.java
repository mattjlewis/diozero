package com.diozero.remote.server;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Remote Server
 * Filename:     BaseRemoteServer.java
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.DigitalInputEvent;
import com.diozero.api.I2CConstants;
import com.diozero.api.I2CDeviceInterface;
import com.diozero.api.PinInfo;
import com.diozero.api.RuntimeIOException;
import com.diozero.api.SerialDeviceInterface;
import com.diozero.api.SpiDeviceInterface;
import com.diozero.api.function.DeviceEventConsumer;
import com.diozero.internal.spi.AnalogInputDeviceInterface;
import com.diozero.internal.spi.AnalogOutputDeviceInterface;
import com.diozero.internal.spi.GpioDigitalDeviceInterface;
import com.diozero.internal.spi.GpioDigitalInputDeviceInterface;
import com.diozero.internal.spi.GpioDigitalInputOutputDeviceInterface;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;
import com.diozero.internal.spi.InternalDeviceInterface;
import com.diozero.internal.spi.NativeDeviceFactoryInterface;
import com.diozero.internal.spi.PwmOutputDeviceInterface;
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
import com.diozero.sbc.BoardInfo;
import com.diozero.sbc.DeviceFactoryHelper;

@SuppressWarnings("resource")
public abstract class BaseRemoteServer implements DeviceEventConsumer<DigitalInputEvent>, RemoteProtocolInterface {
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
				board_info.getMemoryKb(), gpios, request.getCorrelationId());

		return response;
	}

	@Override
	public Response request(ProvisionDigitalInputDevice request) {
		Logger.debug("GPIO input request");

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

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

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

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

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

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

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByPwmOrGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

		Response response;
		try {
			if (device == null) {
				deviceFactory.provisionPwmOutputDevice(pin_info, request.getFrequency(), request.getInitialValue());

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

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

		Response response;
		try {
			if (device == null) {
				deviceFactory.provisionAnalogInputDevice(pin_info);
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

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

		Response response;
		try {
			if (device == null) {
				AnalogOutputDeviceInterface output = deviceFactory.provisionAnalogOutputDevice(pin_info,
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

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

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

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

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

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

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

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

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
	public GpioGetPwmFrequencyResponse request(GpioGetPwmFrequency request) {
		Logger.debug("GPIO get PWM frequency request");

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

		GpioGetPwmFrequencyResponse response;
		if (device == null) {
			return new GpioGetPwmFrequencyResponse("GPIO not provisioned", request.getCorrelationId());
		}

		try {
			response = new GpioGetPwmFrequencyResponse(((PwmOutputDeviceInterface) device).getPwmFrequency(),
					request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new GpioGetPwmFrequencyResponse("Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public Response request(GpioSetPwmFrequency request) {
		Logger.debug("GPIO set PWM frequency request");

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

		if (device == null) {
			return new Response(Response.Status.ERROR, "GPIO not provisioned", request.getCorrelationId());
		}

		Response response;
		if (device instanceof PwmOutputDeviceInterface) {
			try {
				((PwmOutputDeviceInterface) device).setPwmFrequency(request.getFrequency());
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

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		AnalogOutputDeviceInterface device = deviceFactory.getDevice(key);

		GpioAnalogReadResponse response;
		if (device == null) {
			return new GpioAnalogReadResponse("GPIO not provisioned", request.getCorrelationId());
		}

		try {
			response = new GpioAnalogReadResponse(device.getValue(), request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new GpioAnalogReadResponse("Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public Response request(GpioAnalogWrite request) {
		Logger.debug("GPIO analog write request");

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

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

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

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

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		InternalDeviceInterface device = deviceFactory.getDevice(key);

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

		InternalDeviceInterface device = deviceFactory.getDevice(key);
		if (device != null) {
			return new Response(Response.Status.ERROR, "I2C device already provisioned", request.getCorrelationId());
		}

		Response response;
		try {
			deviceFactory.provisionI2CDevice(controller, address,
					I2CConstants.AddressSize.valueOf(request.getAddressSize()));

			response = new Response(Response.Status.OK, null, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new Response(Response.Status.ERROR, "Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public I2CBooleanResponse request(I2CProbe request) {
		Logger.debug("I2C probe request");

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		I2CDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			return new I2CBooleanResponse("I2C device not provisioned", request.getCorrelationId());
		}

		I2CBooleanResponse response;
		try {
			boolean result = device.probe(request.getProbeMode());

			response = new I2CBooleanResponse(result, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new I2CBooleanResponse("Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public Response request(I2CWriteQuick request) {
		Logger.debug("I2C write quick request");

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		I2CDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			return new Response(Response.Status.ERROR, "I2C device not provisioned", request.getCorrelationId());
		}

		Response response;
		try {
			device.writeQuick((byte) request.getBit());

			response = new Response(Response.Status.OK, null, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new Response(Response.Status.ERROR, "Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public I2CByteResponse request(I2CReadByte request) {
		Logger.debug("I2C read byte request");

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		I2CDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			return new I2CByteResponse("I2C device not provisioned", request.getCorrelationId());
		}

		I2CByteResponse response;
		try {
			byte data = device.readByte();

			response = new I2CByteResponse(data, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new I2CByteResponse("Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public Response request(I2CWriteByte request) {
		Logger.debug("I2C write byte request");

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		I2CDeviceInterface device = deviceFactory.getDevice(key);
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
	public I2CBytesResponse request(I2CReadBytes request) {
		Logger.debug("I2C read request");

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		I2CDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			return new I2CBytesResponse("I2C device not provisioned", request.getCorrelationId());
		}

		I2CBytesResponse response;
		try {
			byte[] buffer = new byte[request.getLength()];
			device.readBytes(buffer);

			response = new I2CBytesResponse(buffer, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new I2CBytesResponse("Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public Response request(I2CWriteBytes request) {
		Logger.debug("I2C write request");

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		I2CDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			return new Response(Response.Status.ERROR, "I2C device not provisioned", request.getCorrelationId());
		}

		Response response;
		try {
			byte[] data = request.getData();
			device.writeBytes(data);

			response = new Response(Response.Status.OK, null, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new Response(Response.Status.ERROR, "Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public I2CByteResponse request(I2CReadByteData request) {
		Logger.debug("I2C read byte data request");

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		I2CDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			return new I2CByteResponse("I2C device not provisioned", request.getCorrelationId());
		}

		I2CByteResponse response;
		try {
			byte data = device.readByteData(request.getRegister());

			response = new I2CByteResponse(data, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new I2CByteResponse("Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public Response request(I2CWriteByteData request) {
		Logger.debug("I2C write byte data request");

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		I2CDeviceInterface device = deviceFactory.getDevice(key);
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
	public I2CWordResponse request(I2CReadWordData request) {
		Logger.debug("I2C read word request");

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		I2CDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			return new I2CWordResponse("I2C device not provisioned", request.getCorrelationId());
		}

		I2CWordResponse response;
		try {
			short data = device.readWordData(request.getRegister());

			response = new I2CWordResponse(data, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new I2CWordResponse("Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public Response request(I2CWriteWordData request) {
		Logger.debug("I2C write word data request");

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		I2CDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			return new Response(Response.Status.ERROR, "I2C device not provisioned", request.getCorrelationId());
		}

		Response response;
		try {
			device.writeWordData(request.getRegister(), (short) request.getData());

			response = new Response(Response.Status.OK, null, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new Response(Response.Status.ERROR, "Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public I2CReadBlockDataResponse request(I2CReadBlockData request) {
		Logger.debug("I2C read block data request");

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		I2CDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			return new I2CReadBlockDataResponse("I2C device not provisioned", request.getCorrelationId());
		}

		I2CReadBlockDataResponse response;
		try {
			byte[] buffer = device.readBlockData(request.getRegister());

			response = new I2CReadBlockDataResponse(buffer.length, buffer, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new I2CReadBlockDataResponse("Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public Response request(I2CWriteBlockData request) {
		Logger.debug("I2C write block data request");

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		I2CDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			return new Response(Response.Status.ERROR, "I2C device not provisioned", request.getCorrelationId());
		}

		Response response;
		try {
			device.writeBlockData(request.getRegister(), request.getData());

			response = new Response(Response.Status.OK, null, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new Response(Response.Status.ERROR, "Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public I2CWordResponse request(I2CProcessCall request) {
		Logger.debug("I2C process call request");

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		I2CDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			return new I2CWordResponse("I2C device not provisioned", request.getCorrelationId());
		}

		I2CWordResponse response;
		try {
			short result = device.processCall(request.getRegister(), (short) request.getData());

			response = new I2CWordResponse(result, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new I2CWordResponse("Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public I2CBytesResponse request(I2CBlockProcessCall request) {
		Logger.debug("I2C block process call request");

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		I2CDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			return new I2CBytesResponse("I2C device not provisioned", request.getCorrelationId());
		}

		I2CBytesResponse response;
		try {
			byte[] result = device.blockProcessCall(request.getRegister(), request.getData());

			response = new I2CBytesResponse(result, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new I2CBytesResponse("Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public I2CBytesResponse request(I2CReadI2CBlockData request) {
		Logger.debug("I2C read I2C block data request");

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		I2CDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			return new I2CBytesResponse("I2C device not provisioned", request.getCorrelationId());
		}

		I2CBytesResponse response;
		try {
			byte[] buffer = new byte[request.getLength()];
			device.readI2CBlockData(request.getRegister(), buffer);

			response = new I2CBytesResponse(buffer, request.getCorrelationId());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new I2CBytesResponse("Runtime Error: " + e, request.getCorrelationId());
		}

		return response;
	}

	@Override
	public Response request(I2CWriteI2CBlockData request) {
		Logger.debug("I2C write I2C block data request");

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		I2CDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			return new Response(Response.Status.ERROR, "I2C device not provisioned", request.getCorrelationId());
		}

		Response response;
		try {
			device.writeI2CBlockData(request.getRegister(), request.getData());

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

		I2CDeviceInterface device = deviceFactory.getDevice(key);
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

		InternalDeviceInterface device = deviceFactory.getDevice(key);
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

		SpiDeviceInterface device = deviceFactory.getDevice(key);
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

		SpiDeviceInterface device = deviceFactory.getDevice(key);
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

		SpiDeviceInterface device = deviceFactory.getDevice(key);
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

		InternalDeviceInterface device = deviceFactory.getDevice(key);
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

		SerialDeviceInterface device = deviceFactory.getDevice(key);
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

		SerialDeviceInterface device = deviceFactory.getDevice(key);
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

		SerialDeviceInterface device = deviceFactory.getDevice(key);
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

		SerialDeviceInterface device = deviceFactory.getDevice(key);
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

		SerialDeviceInterface device = deviceFactory.getDevice(key);
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

		SerialDeviceInterface device = deviceFactory.getDevice(key);
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

		SerialDeviceInterface device = deviceFactory.getDevice(key);
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
