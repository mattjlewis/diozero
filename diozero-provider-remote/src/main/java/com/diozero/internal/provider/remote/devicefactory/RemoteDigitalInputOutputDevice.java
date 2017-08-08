package com.diozero.internal.provider.remote.devicefactory;

/*-
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Remote Provider
 * Filename:     RemoteDigitalInputOutputDevice.java  
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

import com.diozero.api.DeviceMode;
import com.diozero.api.DigitalInputEvent;
import com.diozero.api.PinInfo;
import com.diozero.internal.provider.AbstractInputDevice;
import com.diozero.internal.provider.GpioDigitalInputOutputDeviceInterface;
import com.diozero.remote.message.ProvisionDigitalInputOutputDevice;
import com.diozero.remote.message.Response;
import com.diozero.util.RuntimeIOException;

public class RemoteDigitalInputOutputDevice extends AbstractInputDevice<DigitalInputEvent>
		implements GpioDigitalInputOutputDeviceInterface {
	private RemoteDeviceFactory deviceFactory;
	private int gpio;
	private DeviceMode mode;

	public RemoteDigitalInputOutputDevice(RemoteDeviceFactory deviceFactory, String key, PinInfo pinInfo, DeviceMode mode) {
		super(key, deviceFactory);

		this.deviceFactory = deviceFactory;
		gpio = pinInfo.getDeviceNumber();

		ProvisionDigitalInputOutputDevice request = new ProvisionDigitalInputOutputDevice(gpio, mode == DeviceMode.DIGITAL_OUTPUT);
		
		Response response = deviceFactory.getProtocolHandler().sendRequest(request);
		if (response.getStatus() != Response.Status.OK) {
			throw new RuntimeIOException("Error: " + response.getDetail());
		}
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		return deviceFactory.digitalRead(gpio);
	}

	@Override
	public void setValue(boolean value) throws RuntimeIOException {
		deviceFactory.digitalWrite(gpio, value);
	}

	@Override
	public int getGpio() {
		return gpio;
	}
	
	@Override
	public DeviceMode getMode() {
		return mode;
	}
	
	@Override
	public void setMode(DeviceMode mode) {
		// TODO Implement setMode
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		deviceFactory.closeGpio(gpio);
	}

	@Override
	protected void enableListener() {
		// TODO Enable notification events for this GPIO
	}

	@Override
	protected void disableListener() {
		// TODO Disable notification events for this GPIO
	}
}
