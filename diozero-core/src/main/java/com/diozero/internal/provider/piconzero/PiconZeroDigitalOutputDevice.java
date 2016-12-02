package com.diozero.internal.provider.piconzero;

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


import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;
import com.diozero.sandpit.PiconZero;

public class PiconZeroDigitalOutputDevice extends AbstractDevice implements GpioDigitalOutputDeviceInterface {
	private PiconZero piconZero;
	private int channel;
	private boolean value;

	public PiconZeroDigitalOutputDevice(PiconZero piconZero, String key, int channel, boolean initialValue) {
		super(key, piconZero);
		
		this.piconZero = piconZero;
		this.channel = channel;
	}

	@Override
	public void closeDevice() {
		piconZero.closeChannel(channel);
	}
	
	@Override
	public int getPin() {
		return channel;
	}
	
	@Override
	public boolean getValue() {
		return value;
	}
	
	@Override
	public void setValue(boolean value) {
		piconZero.setOutput(channel, value ? 1 : 0);
		this.value = value;
	}
}
