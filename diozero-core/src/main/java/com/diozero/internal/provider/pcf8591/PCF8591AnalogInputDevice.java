package com.diozero.internal.provider.pcf8591;

/*
 * #%L
 * Device I/O Zero - Core
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

import com.diozero.api.AnalogInputEvent;
import com.diozero.internal.spi.AbstractInputDevice;
import com.diozero.internal.spi.GpioAnalogInputDeviceInterface;
import com.diozero.sandpit.PCF8591;
import com.diozero.util.RuntimeIOException;

public class PCF8591AnalogInputDevice extends AbstractInputDevice<AnalogInputEvent> implements GpioAnalogInputDeviceInterface {
	private PCF8591 pcf8591;
	private int gpio;

	public PCF8591AnalogInputDevice(PCF8591 pcf8591, String key, int gpio) {
		super(key, pcf8591);
		
		this.pcf8591 = pcf8591;
		this.gpio = gpio;
	}

	@Override
	public void closeDevice() {
		Logger.debug("closeDevice()");
		// TODO Nothing to do?
	}

	@Override
	public float getValue() throws RuntimeIOException {
		return pcf8591.getValue(gpio);
	}

	@Override
	public int getGpio() {
		return gpio;
	}
}
