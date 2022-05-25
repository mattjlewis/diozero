package com.diozero.internal.provider.remote.grpc;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Remote Provider
 * Filename:     GrpcClientDigitalInputDevice.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2022 diozero
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

import org.tinylog.Logger;

import com.diozero.api.DigitalInputEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.PinInfo;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.AbstractInputDevice;
import com.diozero.internal.spi.GpioDigitalInputDeviceInterface;

public class GrpcClientDigitalInputDevice extends AbstractInputDevice<DigitalInputEvent>
		implements GpioDigitalInputDeviceInterface {
	private GrpcClientDeviceFactory deviceFactory;
	private int gpio;

	public GrpcClientDigitalInputDevice(GrpcClientDeviceFactory deviceFactory, String key, PinInfo pinInfo,
			GpioPullUpDown pud, GpioEventTrigger trigger) {
		super(key, deviceFactory);

		this.deviceFactory = deviceFactory;
		gpio = pinInfo.getDeviceNumber();

		deviceFactory.provisionDigitalInputDevice(gpio, pud, trigger);
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		return deviceFactory.digitalRead(gpio);
	}

	@Override
	public int getGpio() {
		return gpio;
	}

	@Override
	public void setDebounceTimeMillis(int debounceTime) {
		Logger.warn("Debounce not implemented");
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		deviceFactory.closeGpio(gpio);
	}

	@Override
	protected void enableListener() {
		Logger.trace("Enabling listener");
		deviceFactory.subscribe(gpio);
	}

	@Override
	protected void disableListener() {
		Logger.trace("Disabling listener");
		deviceFactory.unsubscribe(gpio);
	}
}
