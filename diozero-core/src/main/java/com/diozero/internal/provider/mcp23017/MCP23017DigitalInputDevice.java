package com.diozero.internal.provider.mcp23017;

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


import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.MCP23017;
import com.diozero.api.DigitalPinEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.GpioDigitalInputDeviceInterface;
import com.diozero.internal.spi.InternalPinListener;

// TODO Implement interrupt support for detecting value changes
public class MCP23017DigitalInputDevice extends AbstractDevice implements GpioDigitalInputDeviceInterface {
	private static final Logger logger = LogManager.getLogger(MCP23017DigitalInputDevice.class);

	private MCP23017 mcp23017;
	private int pinNumber;
	private InternalPinListener listener;
	private GpioEventTrigger trigger;

	public MCP23017DigitalInputDevice(MCP23017 mcp23017, String key, int pinNumber, GpioEventTrigger trigger) {
		super(key, mcp23017);

		this.mcp23017 = mcp23017;
		this.pinNumber = pinNumber;
		this.trigger = trigger;
	}

	@Override
	public void closeDevice() throws IOException {
		logger.debug("closeDevice()");
		removeListener();
		mcp23017.closePin(pinNumber);
	}

	@Override
	public boolean getValue() throws IOException {
		return mcp23017.getValue(pinNumber);
	}

	@Override
	public int getPin() {
		return pinNumber;
	}

	@Override
	public void setDebounceTimeMillis(int debounceTime) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setListener(InternalPinListener listener) {
		this.listener = listener;
	}
	
	public void valueChanged(DigitalPinEvent event) {
		if (listener != null) {
			listener.valueChanged(event);
		}
	}

	@Override
	public void removeListener() {
		listener = null;
	}
}
