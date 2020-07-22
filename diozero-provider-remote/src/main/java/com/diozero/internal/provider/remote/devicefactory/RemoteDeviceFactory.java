package com.diozero.internal.provider.remote.devicefactory;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Remote Provider
 * Filename:     RemoteDeviceFactory.java  
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

import java.util.UUID;

import org.pmw.tinylog.Logger;

import com.diozero.api.AnalogInputEvent;
import com.diozero.api.DeviceMode;
import com.diozero.api.DigitalInputEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.PinInfo;
import com.diozero.api.SpiClockMode;
import com.diozero.internal.provider.AnalogInputDeviceInterface;
import com.diozero.internal.provider.AnalogOutputDeviceInterface;
import com.diozero.internal.provider.BaseNativeDeviceFactory;
import com.diozero.internal.provider.GpioDigitalInputDeviceInterface;
import com.diozero.internal.provider.GpioDigitalInputOutputDeviceInterface;
import com.diozero.internal.provider.GpioDigitalOutputDeviceInterface;
import com.diozero.internal.provider.I2CDeviceInterface;
import com.diozero.internal.provider.PwmOutputDeviceInterface;
import com.diozero.internal.provider.SpiDeviceFactoryInterface;
import com.diozero.internal.provider.SpiDeviceInterface;
import com.diozero.internal.provider.remote.firmata.FirmataProtocolHandler;
import com.diozero.remote.message.GetBoardInfo;
import com.diozero.remote.message.GetBoardInfoResponse;
import com.diozero.remote.message.GpioAnalogRead;
import com.diozero.remote.message.GpioAnalogReadResponse;
import com.diozero.remote.message.GpioClose;
import com.diozero.remote.message.GpioDigitalRead;
import com.diozero.remote.message.GpioDigitalReadResponse;
import com.diozero.remote.message.GpioDigitalWrite;
import com.diozero.remote.message.GpioEvents;
import com.diozero.remote.message.GpioInfo;
import com.diozero.remote.message.GpioPwmRead;
import com.diozero.remote.message.GpioPwmReadResponse;
import com.diozero.remote.message.GpioPwmWrite;
import com.diozero.remote.message.RemoteProtocolInterface;
import com.diozero.remote.message.Response;
import com.diozero.util.BoardInfo;
import com.diozero.util.RuntimeIOException;

public class RemoteDeviceFactory extends BaseNativeDeviceFactory {
	public static final String DEVICE_NAME = "Remote";

	private RemoteProtocolInterface protocolHandler;

	public RemoteDeviceFactory() {
		//protocolHandler = new MqttProtocolHandler(this);
		//protocolHandler = new JsonHttpProtocolHandler(this);
		protocolHandler = new FirmataProtocolHandler(this);
	}

	@Override
	public void close() {
		Logger.info("close()");
		protocolHandler.close();
		super.close();
	}

	@Override
	public String getName() {
		return DEVICE_NAME;
	}
	
	@Override
	protected BoardInfo initialiseBoardInfo() {
		return new RemoteBoardInfo(protocolHandler.request(new GetBoardInfo(UUID.randomUUID().toString())));
	}

	@Override
	public int getBoardPwmFrequency() {
		Logger.warn("Not implemented");
		return -1;
	}

	@Override
	public void setBoardPwmFrequency(int frequency) {
		// Ignore
		Logger.warn("Not implemented");
	}

	@Override
	public int getSpiBufferSize() {
		// FIXME Add to protocol?
		return SpiDeviceFactoryInterface.DEFAULT_SPI_BUFFER_SIZE;
	}

	@Override
	public GpioDigitalInputDeviceInterface createDigitalInputDevice(String key, PinInfo pinInfo, GpioPullUpDown pud,
			GpioEventTrigger trigger) {
		return new RemoteDigitalInputDevice(this, key, pinInfo, pud, trigger);
	}

	@Override
	public GpioDigitalOutputDeviceInterface createDigitalOutputDevice(String key, PinInfo pinInfo,
			boolean initialValue) {
		return new RemoteDigitalOutputDevice(this, key, pinInfo, initialValue);
	}

	@Override
	public GpioDigitalInputOutputDeviceInterface createDigitalInputOutputDevice(String key, PinInfo pinInfo,
			DeviceMode mode) {
		return new RemoteDigitalInputOutputDevice(this, key, pinInfo, mode);
	}

	@Override
	public PwmOutputDeviceInterface createPwmOutputDevice(String key, PinInfo pinInfo, int pwmFrequency,
			float initialValue) {
		return new RemotePwmOutputDevice(this, key, pinInfo, pwmFrequency, initialValue);
	}

	@Override
	public AnalogInputDeviceInterface createAnalogInputDevice(String key, PinInfo pinInfo) {
		return new RemoteAnalogInputDevice(this, key, pinInfo);
	}

	@Override
	public AnalogOutputDeviceInterface createAnalogOutputDevice(String key, PinInfo pinInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public I2CDeviceInterface createI2CDevice(String key, int controller, int address, int addressSize,
			int clockFrequency) throws RuntimeIOException {
		return new RemoteI2CDevice(this, key, controller, address, addressSize, clockFrequency);
	}

	@Override
	public SpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect, int frequency,
			SpiClockMode spiClockMode, boolean lsbFirst) throws RuntimeIOException {
		return new RemoteSpiDevice(this, key, controller, chipSelect, frequency, spiClockMode, lsbFirst);
	}

	boolean digitalRead(int gpio) {
		GpioDigitalRead request = new GpioDigitalRead(gpio, UUID.randomUUID().toString());

		GpioDigitalReadResponse response = protocolHandler.request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in GPIO digital read: " + response.getDetail());
		}

		return response.getValue();
	}

	void digitalWrite(int gpio, boolean value) {
		GpioDigitalWrite request = new GpioDigitalWrite(gpio, value, UUID.randomUUID().toString());

		Response response = protocolHandler.request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in GPIO digital write: " + response.getDetail());
		}
	}

	float pwmRead(int gpio) {
		GpioPwmRead request = new GpioPwmRead(gpio, UUID.randomUUID().toString());

		GpioPwmReadResponse response = protocolHandler.request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in GPIO PWM read: " + response.getDetail());
		}

		return response.getValue();
	}

	void pwmWrite(int gpio, float value) {
		GpioPwmWrite request = new GpioPwmWrite(gpio, value, UUID.randomUUID().toString());

		Response response = protocolHandler.request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in GPIO PWM write: " + response.getDetail());
		}
	}

	float analogRead(int gpio) {
		GpioAnalogRead request = new GpioAnalogRead(gpio, UUID.randomUUID().toString());

		GpioAnalogReadResponse response = protocolHandler.request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error in GPIO analog read: " + response.getDetail());
		}

		return response.getValue();
	}

	void enableEvents(int gpio, boolean b) {
		GpioEvents request = new GpioEvents(gpio, true, UUID.randomUUID().toString());

		Response response = protocolHandler.request(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error reading GPIO: " + response.getDetail());
		}
	}
	
	public void valueChanged(DigitalInputEvent event) {
		PinInfo pin_info = getBoardPinInfo().getByGpioNumber(event.getGpio());
		@SuppressWarnings("resource")
		RemoteDigitalInputDevice device = getDevice(createPinKey(pin_info), RemoteDigitalInputDevice.class);
		if (device != null) {
			device.valueChanged(event);
		}
	}
	
	public void valueChanged(AnalogInputEvent event) {
		PinInfo pin_info = getBoardPinInfo().getByGpioNumber(event.getGpio());
		@SuppressWarnings("resource")
		RemoteAnalogInputDevice device = getDevice(createPinKey(pin_info), RemoteAnalogInputDevice.class);
		if (device != null) {
			device.valueChanged(event);
		}
	}

	void closeGpio(int gpio) {
		GpioClose request = new GpioClose(gpio, UUID.randomUUID().toString());

		Response response = protocolHandler.request(request);
		if (response.getStatus() != Response.Status.OK) {
			Logger.error("Error closing device: " + response.getDetail());
		}
	}

	RemoteProtocolInterface getProtocolHandler() {
		return protocolHandler;
	}
	
	static class RemoteBoardInfo extends BoardInfo {
		private GetBoardInfoResponse boardInfo;
		
		public RemoteBoardInfo(GetBoardInfoResponse boardInfo) {
			super(boardInfo.getMake(), boardInfo.getModel(), -1, "remote", 3.3f);
			
			this.boardInfo = boardInfo;
			
			initialisePins();
		}
		
		@Override
		public void initialisePins() {
			for (GpioInfo gpio_info : boardInfo.getGpios()) {
				if (gpio_info.getModes().contains(DeviceMode.DIGITAL_OUTPUT)
						|| gpio_info.getModes().contains(DeviceMode.DIGITAL_INPUT)) {
					// FIXME GPIO number is not equal to pin number
					addGpioPinInfo(gpio_info.getGpio(), gpio_info.getGpio(), gpio_info.getModes());
				}
				if (gpio_info.getModes().contains(DeviceMode.PWM_OUTPUT)) {
					// FIXME GPIO number is not equal to PWM number
					addPwmPinInfo(gpio_info.getGpio(), gpio_info.getGpio(), gpio_info.getGpio(), gpio_info.getModes());
				}
				if (gpio_info.getModes().contains(DeviceMode.ANALOG_INPUT)) {
					// FIXME GPIO number is not equal to ADC number
					addAdcPinInfo(gpio_info.getGpio(), gpio_info.getGpio());
				}
				if (gpio_info.getModes().contains(DeviceMode.ANALOG_OUTPUT)) {
					// FIXME GPIO number is not equal to DAC number
					addDacPinInfo(gpio_info.getGpio(), gpio_info.getGpio());
				}
			}
		}
	}
}
