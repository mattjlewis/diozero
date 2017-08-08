package com.diozero.remote;

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

import java.io.Closeable;

import org.pmw.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.DigitalInputEvent;
import com.diozero.api.InputEventListener;
import com.diozero.api.PinInfo;
import com.diozero.internal.provider.DeviceInterface;
import com.diozero.internal.provider.GpioDigitalDeviceInterface;
import com.diozero.internal.provider.GpioDigitalInputDeviceInterface;
import com.diozero.internal.provider.GpioDigitalInputOutputDeviceInterface;
import com.diozero.internal.provider.GpioDigitalOutputDeviceInterface;
import com.diozero.internal.provider.NativeDeviceFactoryInterface;
import com.diozero.internal.provider.SpiDeviceInterface;
import com.diozero.remote.message.GpioClose;
import com.diozero.remote.message.GpioDigitalRead;
import com.diozero.remote.message.GpioDigitalReadResponse;
import com.diozero.remote.message.GpioDigitalWrite;
import com.diozero.remote.message.GpioEvents;
import com.diozero.remote.message.ProvisionDigitalInputDevice;
import com.diozero.remote.message.ProvisionDigitalInputOutputDevice;
import com.diozero.remote.message.ProvisionDigitalOutputDevice;
import com.diozero.remote.message.ProvisionSpiDevice;
import com.diozero.remote.message.Response;
import com.diozero.remote.message.SpiClose;
import com.diozero.remote.message.SpiResponse;
import com.diozero.remote.message.SpiWrite;
import com.diozero.remote.message.SpiWriteAndRead;
import com.diozero.util.DeviceFactoryHelper;
import com.diozero.util.RuntimeIOException;

@SuppressWarnings("resource")
public abstract class BaseRemoteServer implements InputEventListener<DigitalInputEvent>, Closeable {
	private NativeDeviceFactoryInterface deviceFactory;

	public BaseRemoteServer() {
		this(DeviceFactoryHelper.getNativeDeviceFactory());
	}

	public BaseRemoteServer(NativeDeviceFactoryInterface deviceFactory) {
		this.deviceFactory = deviceFactory;
	}

	protected Response processRequest(ProvisionDigitalInputDevice request) {
		Logger.debug("GPIO input request");

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumber(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		DeviceInterface device = deviceFactory.getDevice(key);

		Response response;
		try {
			if (device == null) {
				deviceFactory.provisionDigitalInputDevice(request.getGpio(), request.getPud(), request.getTrigger());
				response = Response.OK;
			} else if (device instanceof GpioDigitalInputOutputDeviceInterface) {
				GpioDigitalInputOutputDeviceInterface inout = (GpioDigitalInputOutputDeviceInterface) device;
				inout.setMode(DeviceMode.DIGITAL_INPUT);

				response = Response.OK;
			} else if (device instanceof GpioDigitalInputDeviceInterface) {
				response = Response.OK;
			} else {
				response = new Response(Response.Status.ERROR, "GPIO already provisioned");
			}
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new Response(Response.Status.ERROR, "Runtime Error: " + e);
		}

		return response;
	}

	protected Response processRequest(ProvisionDigitalOutputDevice request) {
		Logger.debug("GPIO output request");

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumber(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		DeviceInterface device = deviceFactory.getDevice(key);

		Response response;
		try {
			if (device == null) {
				deviceFactory.provisionDigitalOutputDevice(request.getGpio(), request.getInitialValue());

				response = Response.OK;
			} else if (device instanceof GpioDigitalInputOutputDeviceInterface) {
				GpioDigitalInputOutputDeviceInterface inout = (GpioDigitalInputOutputDeviceInterface) device;
				inout.setMode(DeviceMode.DIGITAL_OUTPUT);
				inout.setValue(request.getInitialValue());

				response = Response.OK;
			} else if (device instanceof GpioDigitalOutputDeviceInterface) {
				GpioDigitalOutputDeviceInterface output = (GpioDigitalOutputDeviceInterface) device;
				output.setValue(request.getInitialValue());

				response = Response.OK;
			} else {
				response = new Response(Response.Status.ERROR, "GPIO already provisioned");
			}
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new Response(Response.Status.ERROR, "Runtime Error: " + e);
		}

		return response;
	}

	protected Response processRequest(ProvisionDigitalInputOutputDevice request) {
		Logger.debug("GPIO input/output request");

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumber(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		DeviceInterface device = deviceFactory.getDevice(key);

		Response response;
		try {
			if (device == null) {
				deviceFactory.provisionDigitalInputOutputDevice(request.getGpio(),
						request.getOutput() ? DeviceMode.DIGITAL_OUTPUT : DeviceMode.DIGITAL_INPUT);

				response = Response.OK;
			} else if (device instanceof GpioDigitalInputOutputDeviceInterface) {
				GpioDigitalInputOutputDeviceInterface inout = (GpioDigitalInputOutputDeviceInterface) device;
				inout.setMode(request.getOutput() ? DeviceMode.DIGITAL_OUTPUT : DeviceMode.DIGITAL_INPUT);

				response = Response.OK;
			} else {
				response = new Response(Response.Status.ERROR, "GPIO already provisioned");
			}
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new Response(Response.Status.ERROR, "Runtime Error: " + e);
		}

		return response;
	}

	protected GpioDigitalReadResponse processRequest(GpioDigitalRead request) {
		Logger.debug("GPIO read request");

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumber(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		DeviceInterface device = deviceFactory.getDevice(key);

		GpioDigitalReadResponse response;
		if (device == null) {
			return new GpioDigitalReadResponse("GPIO not provisioned");
		}

		try {
			response = new GpioDigitalReadResponse(((GpioDigitalDeviceInterface) device).getValue());
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new GpioDigitalReadResponse("Runtime Error: " + e);
		}

		return response;
	}

	protected Response processRequest(GpioDigitalWrite request) {
		Logger.debug("GPIO write request");

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumber(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		DeviceInterface device = deviceFactory.getDevice(key);

		if (device == null) {
			return new Response(Response.Status.ERROR, "GPIO not provisioned");
		}

		Response response;
		// Also covers digital input / output device
		if (device instanceof GpioDigitalOutputDeviceInterface) {
			try {
				((GpioDigitalOutputDeviceInterface) device).setValue(request.getValue());
				response = Response.OK;
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
				response = new Response(Response.Status.ERROR, "Runtime Error: " + e);
			}
		} else {
			response = new Response(Response.Status.ERROR,
					"Invalid mode, device class: " + device.getClass().getName());
		}

		return response;
	}

	protected Response processRequest(GpioEvents request) {
		Logger.debug("GPIO events request");

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumber(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		DeviceInterface device = deviceFactory.getDevice(key);

		if (device == null) {
			return new Response(Response.Status.ERROR, "GPIO not provisioned");
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

				response = Response.OK;
			} else if (device instanceof GpioDigitalInputOutputDeviceInterface) {
				GpioDigitalInputOutputDeviceInterface inout = (GpioDigitalInputOutputDeviceInterface) device;
				if (request.getEnabled()) {
					inout.setListener(this);
				} else {
					inout.removeListener();
				}

				response = Response.OK;
			} else {
				response = new Response(Response.Status.ERROR,
						"Invalid mode, device class: " + device.getClass().getName());
			}
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new Response(Response.Status.ERROR, "Runtime Error: " + e);
		}

		return response;
	}

	protected Response processRequest(GpioClose request) {
		Logger.debug("GPIO close request");

		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumber(request.getGpio());
		String key = deviceFactory.createPinKey(pin_info);
		DeviceInterface device = deviceFactory.getDevice(key);

		if (device == null) {
			return new Response(Response.Status.ERROR, "GPIO not provisioned");
		}

		Response response;
		try {
			device.close();

			response = Response.OK;
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new Response(Response.Status.ERROR, "Runtime Error: " + e);
		}

		return response;
	}

	protected Response processRequest(ProvisionSpiDevice request) {
		Logger.debug("SPI open request {}", request);

		int controller = request.getController();
		int chip_select = request.getChipSelect();
		String key = deviceFactory.createSpiKey(controller, chip_select);

		DeviceInterface device = deviceFactory.getDevice(key);
		if (device != null) {
			return new Response(Response.Status.ERROR, "SPI device already provisioned");
		}

		Response response;
		try {
			deviceFactory.provisionSpiDevice(controller, chip_select, request.getFrequency(), request.getClockMode(),
					request.getLsbFirst());

			response = Response.OK;
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
			response = new Response(Response.Status.ERROR, "Runtime Error: " + e);
		}

		return response;
	}

	protected Response processRequest(SpiWrite request) {
		Logger.debug("SPI write request");

		int controller = request.getController();
		int chip_select = request.getChipSelect();
		String key = deviceFactory.createSpiKey(controller, chip_select);

		DeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			return new Response(Response.Status.ERROR, "SPI device not provisioned");
		}

		Response response;
		if (device instanceof SpiDeviceInterface) {
			try {
				byte[] data = request.getTxData();
				long start = System.currentTimeMillis();
				((SpiDeviceInterface) device).write(data);
				long duration = System.currentTimeMillis() - start;
				System.out.println("Inner time: " + duration + " ms for " + data.length + " bytes, class: " + device.getClass().getName());

				response = Response.OK;
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
				response = new Response(Response.Status.ERROR, "Runtime Error: " + e);
			}
		} else {
			response = new Response(Response.Status.ERROR,
					"Invalid mode, device class: " + device.getClass().getName());
		}

		return response;
	}
	
	protected SpiResponse processRequest(SpiWriteAndRead request) {
		Logger.debug("SPI write and read request");

		int controller = request.getController();
		int chip_select = request.getChipSelect();
		String key = deviceFactory.createSpiKey(controller, chip_select);

		DeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			return new SpiResponse("SPI device not provisioned");
		}

		SpiResponse response;
		if (device instanceof SpiDeviceInterface) {
			try {
				byte[] rx_data = ((SpiDeviceInterface) device).writeAndRead(request.getTxData());

				response = new SpiResponse(rx_data);
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
				response = new SpiResponse("Runtime Error: " + e);
			}
		} else {
			response = new SpiResponse("Invalid mode, device class: " + device.getClass().getName());
		}

		return response;
	}
	
	protected Response processRequest(SpiClose request) {
		Logger.debug("SPI close request");

		int controller = request.getController();
		int chip_select = request.getChipSelect();
		String key = deviceFactory.createSpiKey(controller, chip_select);

		DeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			return new SpiResponse("SPI device not provisioned");
		}

		Response response;
		if (device instanceof SpiDeviceInterface) {
			try {
				device.close();

				response = Response.OK;
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
				response = new Response(Response.Status.ERROR, "Runtime Error: " + e);
			}
		} else {
			response = new Response(Response.Status.ERROR, "Invalid mode, device class: " + device.getClass().getName());
		}

		return response;
	}

	@Override
	public void close() {
		deviceFactory.close();
	}
}
