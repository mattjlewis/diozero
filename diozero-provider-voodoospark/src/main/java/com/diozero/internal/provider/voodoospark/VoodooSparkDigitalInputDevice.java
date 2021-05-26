package com.diozero.internal.provider.voodoospark;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Voodoo Spark provider for Particle Photon
 * Filename:     VoodooSparkDigitalInputDevice.java
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

import com.diozero.api.*;
import com.diozero.internal.provider.voodoospark.VoodooSparkDeviceFactory.PinMode;
import com.diozero.internal.spi.AbstractInputDevice;
import com.diozero.internal.spi.GpioDigitalInputDeviceInterface;

public class VoodooSparkDigitalInputDevice extends AbstractInputDevice<DigitalInputEvent>
implements GpioDigitalInputDeviceInterface {
	private VoodooSparkDeviceFactory deviceFactory;
	private int gpio;

	VoodooSparkDigitalInputDevice(VoodooSparkDeviceFactory deviceFactory, String key,
			PinInfo pinInfo, GpioPullUpDown pud, GpioEventTrigger trigger) {
		super(key, deviceFactory);
		
		this.deviceFactory = deviceFactory;
		gpio = pinInfo.getDeviceNumber();
		
		deviceFactory.setPinMode(gpio, PinMode.DIGITAL_INPUT);
	}

	@Override
	public int getGpio() {
		return gpio;
	}
	
	@Override
	public boolean getValue() throws RuntimeIOException {
		return deviceFactory.getValue(gpio);
	}

	@Override
	public void setDebounceTimeMillis(int debounceTime) {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		// Nothing to do
	}
}
