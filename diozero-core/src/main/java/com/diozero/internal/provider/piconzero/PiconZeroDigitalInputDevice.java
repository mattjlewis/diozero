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


import com.diozero.api.*;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.GpioDigitalInputDeviceInterface;
import com.diozero.sandpit.PiconZero;
import com.diozero.util.RuntimeIOException;

public class PiconZeroDigitalInputDevice extends AbstractDevice implements GpioDigitalInputDeviceInterface {
	private PiconZero piconZero;
	private int channel;

	public PiconZeroDigitalInputDevice(PiconZero piconZero, String key, int channel, GpioPullUpDown pud,
			GpioEventTrigger trigger) {
		super(key, piconZero);
		
		this.piconZero = piconZero;
		this.channel = channel;
	}
	
	@Override
	public void closeDevice() {
		piconZero.closeChannel(channel);
	}
	
	@Override
	public int getGpio() {
		return channel;
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		return piconZero.readInput(channel) != 0;
	}

	@Override
	public void setDebounceTimeMillis(int debounceTime) {
		throw new UnsupportedOperationException("Not supported");
	}

	@Override
	public void setListener(InputEventListener<DigitalInputEvent> listener) {
		// TODO Need to implement a polling mechanism
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public void removeListener() {
	}
}
