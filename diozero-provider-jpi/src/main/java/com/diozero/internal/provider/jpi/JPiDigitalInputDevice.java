package com.diozero.internal.provider.jpi;

/*
 * #%L
 * Device I/O Zero - Java Native provider for the Raspberry Pi
 * %%
 * Copyright (C) 2016 mattjlewis
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

import com.diozero.api.*;
import com.diozero.internal.spi.AbstractInputDevice;
import com.diozero.internal.spi.GpioDeviceInterface;
import com.diozero.internal.spi.GpioDigitalInputDeviceInterface;
import com.diozero.util.RuntimeIOException;

public class JPiDigitalInputDevice extends AbstractInputDevice<DigitalInputEvent>
implements GpioDigitalInputDeviceInterface, InputEventListener<DigitalInputEvent> {
	private JPiDeviceFactory jpiDeviceFactory;
	private int gpio;
	private GpioDigitalInputDeviceInterface sysFsDigitialInput;

	JPiDigitalInputDevice(JPiDeviceFactory deviceFactory, String key,
			int gpio, GpioPullUpDown pud, GpioEventTrigger trigger) {
		super(key, deviceFactory);
		
		this.jpiDeviceFactory = deviceFactory;
		this.gpio = gpio;
		
		deviceFactory.getMmapGpio().setMode(gpio, GpioDeviceInterface.Mode.DIGITAL_INPUT);
		deviceFactory.getMmapGpio().setPullUpDown(gpio, pud);

		sysFsDigitialInput = jpiDeviceFactory.getSysFsDeviceFactory().provisionDigitalInputPin(
				gpio, pud, trigger);
	}

	@Override
	public int getGpio() {
		return gpio;
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		return jpiDeviceFactory.getMmapGpio().gpioRead(gpio);
	}

	@Override
	public void setDebounceTimeMillis(int debounceTime) {
		throw new UnsupportedOperationException("Debounce not supported");
	}
	
	@Override
	protected void enableListener() {
		sysFsDigitialInput.setListener(this);
	}
	
	@Override
	protected void disableListener() {
		sysFsDigitialInput.removeListener();
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.debug("closeDevice()");
		disableListener();
		if (sysFsDigitialInput != null) {
			sysFsDigitialInput.close();
			sysFsDigitialInput = null;
		}
		// FIXME No GPIO close method?
		// TODO Revert to default input mode?
		// What do wiringPi / pigpio do?
	}
}
