package com.diozero.internal.provider.mcp23xxx;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     MCP23xxxDigitalInputOutputDevice.java  
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

import org.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.DigitalInputEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.internal.provider.AbstractInputDevice;
import com.diozero.internal.provider.GpioDigitalInputOutputDeviceInterface;
import com.diozero.util.RuntimeIOException;

public class MCP23xxxDigitalInputOutputDevice extends AbstractInputDevice<DigitalInputEvent> implements GpioDigitalInputOutputDeviceInterface {
	private MCP23xxx mcp23xxx;
	private int gpio;
	private DeviceMode mode;

	public MCP23xxxDigitalInputOutputDevice(MCP23xxx mcp23xxx, String key, int gpio, DeviceMode mode) {
		super(key, mcp23xxx);
		
		this.mcp23xxx = mcp23xxx;
		this.gpio = gpio;
		
		setMode(mode);
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		return mcp23xxx.getValue(gpio);
	}

	@Override
	public void setValue(boolean value) throws RuntimeIOException {
		if (mode != DeviceMode.DIGITAL_OUTPUT) {
			throw new IllegalStateException("Can only set output value for digital output pins");
		}
		mcp23xxx.setValue(gpio, value);
	}

	@Override
	public int getGpio() {
		return gpio;
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.trace("closeDevice()");
		mcp23xxx.closeGpio(gpio);
	}

	@Override
	public DeviceMode getMode() {
		return mode;
	}

	@Override
	public void setMode(DeviceMode mode) {
		if (mode == DeviceMode.DIGITAL_INPUT) {
			mcp23xxx.setInputMode(gpio, GpioPullUpDown.NONE, GpioEventTrigger.BOTH);
		} else {
			mcp23xxx.setOutputMode(gpio);
		}
		this.mode = mode;
	}
}
