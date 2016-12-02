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


import org.pmw.tinylog.Logger;

import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.PwmOutputDeviceInterface;
import com.diozero.sandpit.PiconZero;
import com.diozero.util.RuntimeIOException;

public class PiconZeroPwmOutputDevice extends AbstractDevice implements PwmOutputDeviceInterface {

	private PiconZero piconZero;
	private int channel;
	private float value;

	public PiconZeroPwmOutputDevice(PiconZero piconZero, String key, int channel, float initialValue) {
		super(key, piconZero);
		
		this.piconZero = piconZero;
		this.channel = channel;
	}

	@Override
	public int getPin() {
		return channel;
	}

	@Override
	public float getValue() throws RuntimeIOException {
		return value;
	}

	@Override
	public void setValue(float value) throws RuntimeIOException {
		piconZero.setValue(channel, value);
		this.value = value;
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.debug("closeDevice()");
		piconZero.closeChannel(channel);
	}
}
