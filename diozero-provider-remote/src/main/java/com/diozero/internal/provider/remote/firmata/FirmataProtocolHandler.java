package com.diozero.internal.provider.remote.firmata;

/*-
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Remote Provider
 * Filename:     FirmataProtocolHandler.java  
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.jetty.io.RuntimeIOException;
import org.pmw.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.GpioPullUpDown;
import com.diozero.internal.provider.remote.devicefactory.RemoteDeviceFactory;
import com.diozero.internal.provider.remote.firmata.FirmataAdapter.I2CResponse;
import com.diozero.internal.provider.remote.firmata.FirmataProtocol.PinCapability;
import com.diozero.internal.provider.remote.firmata.FirmataProtocol.PinMode;
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
import com.diozero.util.PropertyUtil;
import com.diozero.util.RangeUtil;

public class FirmataProtocolHandler implements RemoteProtocolInterface {
	private static final String TCP_HOST_PROP = "FIRMATA_TCP_HOST";
	private static final String TCP_PORT_PROP = "FIRMATA_TCP_PORT";
	private static final String SERIAL_PORT_PROP = "FIRMATA_SERIAL_PORT";

	private static final int DEFAULT_TCP_PORT = 3030;
	private static final int DEFAULT_PWM_MAX = (int) Math.pow(2, 10) - 1;
	private static final int DEFAULT_ANALOG_MAX = (int) Math.pow(2, 14) - 1;

	private FirmataAdapter adapter;

	public FirmataProtocolHandler(RemoteDeviceFactory deviceFactory) {
		String hostname = PropertyUtil.getProperty(TCP_HOST_PROP, null);
		if (hostname != null) {
			int port = PropertyUtil.getIntProperty(TCP_PORT_PROP, DEFAULT_TCP_PORT);
			try {
				adapter = new SocketFirmataAdapter(hostname, port);
			} catch (IOException e) {
				throw new RuntimeIOException(e);
			}
			// } else {
			// String serial_port = PropertyUtil.getProperty(SERIAL_PORT_PROP, null);
			// if (serial_port != null) {
			// adapter = new SerialFirmataAdapter(serial_port)
			// }
		}
		if (adapter == null) {
			Logger.error("Please set either {} or {} property", TCP_HOST_PROP, SERIAL_PORT_PROP);
			throw new IllegalArgumentException("Either " + TCP_HOST_PROP + " or " + SERIAL_PORT_PROP + " must be set");
		}
	}
	
	@Override
	public GetBoardGpioInfoResponse request(GetBoardGpioInfo request) {
		try {
			List<List<PinCapability>> board_capabilities = adapter.getBoardCapabilities();
			List<GpioInfo> board_gpio_info = new ArrayList<>();
			
			int gpio = 0;
			List<DeviceMode> modes;
			for (List<PinCapability> pin_capabilities : board_capabilities) {
				modes = new ArrayList<>();
				for (PinCapability pin_capability : pin_capabilities) {
					modes.add(convert(pin_capability.getMode()));
				}
				if (! modes.isEmpty()) {
					board_gpio_info.add(new GpioInfo(gpio, modes));
				}
				
				gpio++;
			}
			
			return new GetBoardGpioInfoResponse(board_gpio_info, request.getCorrelationId());
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
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
		try {
			adapter.setPinMode(request.getGpio(),
					request.getPud() == GpioPullUpDown.PULL_UP ? PinMode.INPUT_PULLUP : PinMode.DIGITAL_INPUT);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public Response request(ProvisionDigitalOutputDevice request) {
		try {
			adapter.setPinMode(request.getGpio(), PinMode.DIGITAL_OUTPUT);
			adapter.setDigitalValue(request.getGpio(), request.getInitialValue());
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public Response request(ProvisionDigitalInputOutputDevice request) {
		try {
			adapter.setPinMode(request.getGpio(), request.getOutput() ? PinMode.DIGITAL_OUTPUT : PinMode.DIGITAL_INPUT);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public Response request(ProvisionPwmOutputDevice request) {
		try {
			adapter.setPinMode(request.getGpio(), PinMode.PWM);
			// FIXME DEFAULT_PWM_MAX Should really come from the pin capability
			adapter.setValue(request.getGpio(), RangeUtil.map(request.getInitialValue(), 0f, 1f, 0, DEFAULT_PWM_MAX, true));
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public Response request(ProvisionAnalogInputDevice request) {
		try {
			adapter.setPinMode(request.getGpio(), PinMode.ANALOG_INPUT);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
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
		try {
			adapter.setDigitalValue(request.getGpio(), request.getValue());
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public GpioPwmReadResponse request(GpioPwmRead request) {
		// FIXME DEFAULT_PWM_MAX Should really come from the pin capability
		float value = RangeUtil.map(adapter.getValue(request.getGpio()), 0, DEFAULT_PWM_MAX, 0f, 1f, true);
		return new GpioPwmReadResponse(value, UUID.randomUUID().toString());
	}

	@Override
	public Response request(GpioPwmWrite request) {
		try {
			// FIXME DEFAULT_PWM_MAX Should really come from the pin capability
			adapter.setValue(request.getGpio(), RangeUtil.map(request.getValue(), 0f, 1f, 0, DEFAULT_PWM_MAX, true));
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public GpioAnalogReadResponse request(GpioAnalogRead request) {
		// FIXME DEFAULT_ANALOG_MAX Should really come from the pin capability
		float value = RangeUtil.map(adapter.getValue(request.getGpio()), 0, DEFAULT_ANALOG_MAX, 0f, 1f, true);
		return new GpioAnalogReadResponse(value, UUID.randomUUID().toString());
	}

	@Override
	public Response request(GpioAnalogWrite request) {
		try {
			// FIXME DEFAULT_ANALOG_MAX Should really come from the pin capability
			adapter.setValue(request.getGpio(), RangeUtil.map(request.getValue(), 0f, 1f, 0, DEFAULT_ANALOG_MAX, true));
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public Response request(GpioEvents request) {
		try {
			adapter.enableDigitalReporting(request.getGpio(), request.getEnabled());
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public Response request(GpioClose request) {
		try {
			adapter.setPinMode(request.getGpio(), PinMode.DIGITAL_INPUT);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public Response request(I2COpen request) {
		// Nothing to do?
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public I2CReadByteResponse request(I2CReadByte request) {
		try {
			I2CResponse response = adapter.i2cRead(request.getAddress(), false, false, 1);
			byte[] data = response.getData();
			if (data.length != 1) {
				throw new RuntimeIOException("I2C Error: Expected to read 1 byte, got " + data.length);
			}
			return new I2CReadByteResponse(data[0], request.getCorrelationId());
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public Response request(I2CWriteByte request) {
		try {
			adapter.i2cWrite(request.getAddress(), false, false, new byte[] { request.getData() });
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public I2CReadResponse request(I2CRead request) {
		try {
			I2CResponse response = adapter.i2cRead(request.getAddress(), false, false, request.getLength());
			byte[] data = response.getData();
			if (data.length != request.getLength()) {
				throw new RuntimeIOException(
						"I2C Error: Expected to read " + request.getLength() + " bytes, got " + data.length);
			}
			return new I2CReadResponse(data, request.getCorrelationId());
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public Response request(I2CWrite request) {
		try {
			adapter.i2cWrite(request.getAddress(), false, false, request.getData());
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public I2CReadByteResponse request(I2CReadByteData request) {
		try {
			I2CResponse response = adapter.i2cReadData(request.getAddress(), false, false, request.getRegister(), 1);
			byte[] data = response.getData();
			if (data.length != 1) {
				throw new RuntimeIOException("I2C Error: Expected to read 1 byte, got " + data.length);
			}
			return new I2CReadByteResponse(data[0], request.getCorrelationId());
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public Response request(I2CWriteByteData request) {
		try {
			adapter.i2cWriteData(request.getAddress(), false, false, request.getRegister(),
					new byte[] { request.getData() });
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public I2CReadResponse request(I2CReadI2CBlockData request) {
		try {
			I2CResponse response = adapter.i2cReadData(request.getAddress(), false, false, request.getRegister(),
					request.getLength());
			byte[] data = response.getData();
			if (data.length != request.getLength()) {
				throw new RuntimeIOException(
						"I2C Error: Expected to read " + request.getLength() + " bytes, got " + data.length);
			}
			return new I2CReadResponse(data, request.getCorrelationId());
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public Response request(I2CWriteI2CBlockData request) {
		try {
			adapter.i2cWriteData(request.getAddress(), false, false, request.getRegister(), request.getData());
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
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
	public void close() {
		adapter.close();
	}
}
