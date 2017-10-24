package com.diozero.remote.server;

/*-
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Remote Server
 * Filename:     BaseRemoteServer.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2017 mattjlewis
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

import org.pmw.tinylog.Logger;

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
import com.diozero.internal.provider.SpiDeviceInterface;
import com.diozero.remote.message.GetBoardGpioInfo;
import com.diozero.remote.message.GetBoardGpioInfoResponse;
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
import com.diozero.remote.message.I2CReadByteResponse;
import com.diozero.remote.message.I2CReadI2CBlockData;
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
import com.diozero.remote.message.SpiClose;
import com.diozero.remote.message.SpiOpen;
import com.diozero.remote.message.SpiResponse;
import com.diozero.remote.message.SpiWrite;
import com.diozero.remote.message.SpiWriteAndRead;
import com.diozero.util.BoardPinInfo;
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
	public GetBoardGpioInfoResponse request(GetBoardGpioInfo request) {
		BoardPinInfo pin_info = deviceFactory.getBoardPinInfo();
		
		// TODO Implementation
		BoardPinInfo board_pin_info = deviceFactory.getBoardPinInfo();
		Collection<PinInfo> gpio_pins = board_pin_info.getGpioPins();
		Collection<PinInfo> adc_pins = board_pin_info.getAdcPins();
		Collection<PinInfo> dac_pins = board_pin_info.getDacPins();
		List<GpioInfo> gpios = new ArrayList<>();
		
		GetBoardGpioInfoResponse response = new GetBoardGpioInfoResponse(gpios, request.getCorrelationId());
		
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
				deviceFactory.provisionDigitalInputDevice(request.getGpio(), request.getPud(), request.getTrigger());
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
				deviceFactory.provisionDigitalOutputDevice(request.getGpio(), request.getInitialValue());

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
				deviceFactory.provisionDigitalInputOutputDevice(request.getGpio(),
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
				AnalogOutputDeviceInterface output = deviceFactory.provisionAnalogOutputDevice(request.getGpio());
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
			response = new GpioDigitalReadResponse(((GpioDigitalDeviceInterface) device).getValue(), request.getCorrelationId());
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
			response = new Response(Response.Status.ERROR,
					"Invalid mode, device class: " + device.getClass().getName(), request.getCorrelationId());
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
			response = new GpioPwmReadResponse(((PwmOutputDeviceInterface) device).getValue(), request.getCorrelationId());
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
			response = new Response(Response.Status.ERROR,
					"Invalid mode, device class: " + device.getClass().getName(), request.getCorrelationId());
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
			response = new GpioAnalogReadResponse(((AnalogOutputDeviceInterface) device).getValue(), request.getCorrelationId());
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
			response = new Response(Response.Status.ERROR,
					"Invalid mode, device class: " + device.getClass().getName(), request.getCorrelationId());
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
			deviceFactory.provisionI2CDevice(controller, address, request.getAddressSize(), request.getClockFrequency());

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
			return new I2CReadByteResponse("I2C device not provisioned", request.getCorrelationId());
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
		return null;
	}
	
	@Override
	public I2CReadByteResponse request(I2CReadByteData request) {
		return null;
	}
	
	@Override
	public Response request(I2CWriteByteData request) {
		return null;
	}
	
	@Override
	public I2CReadResponse request(I2CReadI2CBlockData request) {
		return null;
	}
	
	@Override
	public Response request(I2CWriteI2CBlockData request) {
		return null;
	}
	
	@Override
	public Response request(I2CClose request) {
		return null;
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
			System.out.println("Inner time: " + duration + " ms for " + data.length + " bytes, class: " + device.getClass().getName());

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
	public void close() {
		deviceFactory.close();
	}
}
