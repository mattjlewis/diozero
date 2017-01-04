package com.diozero.internal.provider.mcp23xxx;

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

import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.GpioDeviceInterface;
import com.diozero.internal.spi.GpioDigitalInputOutputDeviceInterface;
import com.diozero.util.RuntimeIOException;

public class MCP23xxxDigitalInputOutputDevice extends AbstractDevice implements GpioDigitalInputOutputDeviceInterface {
	private MCP23xxx mcp23xxx;
	private int gpio;
	private GpioDeviceInterface.Mode mode;

	public MCP23xxxDigitalInputOutputDevice(MCP23xxx mcp23xxx, String key, int gpio, GpioDeviceInterface.Mode mode) {
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
		if (mode != GpioDeviceInterface.Mode.DIGITAL_OUTPUT) {
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
		Logger.debug("closeDevice()");
		mcp23xxx.closePin(gpio);
	}
	
	private static void checkMode(GpioDeviceInterface.Mode mode) {
		if (mode != GpioDeviceInterface.Mode.DIGITAL_INPUT && mode != GpioDeviceInterface.Mode.DIGITAL_OUTPUT) {
			throw new IllegalArgumentException("Invalid mode, must be DIGITAL_INPUT or DIGITAL_OUTPUT");
		}
	}

	@Override
	public Mode getMode() {
		return mode;
	}

	@Override
	public void setMode(Mode mode) {
		checkMode(mode);
		
		if (mode == GpioDeviceInterface.Mode.DIGITAL_INPUT) {
			mcp23xxx.setInputMode(gpio, GpioPullUpDown.NONE, GpioEventTrigger.BOTH);
		} else {
			mcp23xxx.setOutputMode(gpio);
		}
		this.mode = mode;
	}
}
