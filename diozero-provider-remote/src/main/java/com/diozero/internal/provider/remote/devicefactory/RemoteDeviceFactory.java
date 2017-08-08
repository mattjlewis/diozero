package com.diozero.internal.provider.remote.devicefactory;

/*-
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Remote Provider
 * Filename:     RemoteDeviceFactory.java  
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

import org.pmw.tinylog.Logger;

import com.diozero.api.DeviceMode;
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
import com.diozero.internal.provider.remote.http.JsonHttpProtocolHandler;
import com.diozero.remote.message.GpioClose;
import com.diozero.remote.message.GpioDigitalRead;
import com.diozero.remote.message.GpioDigitalReadResponse;
import com.diozero.remote.message.GpioDigitalWrite;
import com.diozero.remote.message.GpioEvents;
import com.diozero.remote.message.Response;
import com.diozero.util.RuntimeIOException;

public class RemoteDeviceFactory extends BaseNativeDeviceFactory {
	public static final String DEVICE_NAME = "Remote";
	
	private ProtocolHandlerInterface protocolHandler;

	public RemoteDeviceFactory() {
		//protocolHandler = new MqttProtocolHandler(this);
		protocolHandler = new JsonHttpProtocolHandler(this);
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AnalogInputDeviceInterface createAnalogInputDevice(String key, PinInfo pinInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public AnalogOutputDeviceInterface createAnalogOutputDevice(String key, PinInfo pinInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected SpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect, int frequency,
			SpiClockMode spiClockMode, boolean lsbFirst) throws RuntimeIOException {
		return new RemoteSpiDevice(this, key, controller, chipSelect, frequency, spiClockMode, lsbFirst);
	}

	@Override
	protected I2CDeviceInterface createI2CDevice(String key, int controller, int address, int addressSize,
			int clockFrequency) throws RuntimeIOException {
		// TODO Auto-generated method stub
		return null;
	}

	boolean digitalRead(int gpio) {
		GpioDigitalRead request = new GpioDigitalRead(gpio);

		GpioDigitalReadResponse response = protocolHandler.sendRequest(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error reading GPIO: " + response.getDetail());
		}

		return response.getValue();
	}

	void digitalWrite(int gpio, boolean value) {
		GpioDigitalWrite request = new GpioDigitalWrite(gpio, value);

		Response response = protocolHandler.sendRequest(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error reading GPIO: " + response.getDetail());
		}
	}

	void enableEvents(int gpio, boolean b) {
		GpioEvents request = new GpioEvents(gpio, true);

		Response response = protocolHandler.sendRequest(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error reading GPIO: " + response.getDetail());
		}
	}

	void closeGpio(int gpio) {
		GpioClose request = new GpioClose(gpio);
		
		Response response = protocolHandler.sendRequest(request);
		if (response.getStatus() != Response.Status.OK) {
			Logger.error("Error closing device: " + response.getDetail());
		}
	}

	ProtocolHandlerInterface getProtocolHandler() {
		return protocolHandler;
	}
}
