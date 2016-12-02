package com.diozero.internal.provider.mcp230xx;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 diozero
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

import com.diozero.api.DigitalInputEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.internal.spi.AbstractInputDevice;
import com.diozero.internal.spi.GpioDigitalInputDeviceInterface;
import com.diozero.util.RuntimeIOException;

public class MCP230xxDigitalInputDevice extends AbstractInputDevice<DigitalInputEvent> implements GpioDigitalInputDeviceInterface {
	private MCP230xx mcp230xx;
	private int pinNumber;
	private GpioEventTrigger trigger;

	public MCP230xxDigitalInputDevice(MCP230xx mcp230xx, String key, int pinNumber, GpioEventTrigger trigger) {
		super(key, mcp230xx);

		this.mcp230xx = mcp230xx;
		this.pinNumber = pinNumber;
		this.trigger = trigger;
	}

	@Override
	public void closeDevice() throws RuntimeIOException {
		Logger.debug("closeDevice()");
		removeListener();
		mcp230xx.closePin(pinNumber);
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		return mcp230xx.getValue(pinNumber);
	}

	@Override
	public int getPin() {
		return pinNumber;
	}

	@Override
	public void setDebounceTimeMillis(int debounceTime) {
		// TODO Auto-generated method stub
	}
}
