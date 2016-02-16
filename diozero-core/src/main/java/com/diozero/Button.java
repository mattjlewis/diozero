package com.diozero;

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

import com.diozero.api.*;
import com.diozero.internal.spi.GpioDeviceFactoryInterface;
import com.diozero.util.RuntimeIOException;

/**
 * 
 */
public class Button extends DigitalInputDevice {
	private Action pressedAction;
	private Action releasedAction;

	public Button(int pinNumber) throws RuntimeIOException {
		super(pinNumber, GpioPullUpDown.NONE, GpioEventTrigger.BOTH);
	}

	public Button(int pinNumber, GpioPullUpDown pud) throws RuntimeIOException {
		super(pinNumber, pud, GpioEventTrigger.BOTH);
	}

	public Button(GpioDeviceFactoryInterface deviceFactory, int pinNumber, GpioPullUpDown pud) throws RuntimeIOException {
		super(deviceFactory, pinNumber, pud, GpioEventTrigger.BOTH);
	}
	
	public boolean isPressed() {
		return activeHigh == getValue();
	}
	
	public boolean isReleased() {
		return activeHigh != getValue();
	}
	
	public void whenPressed(Action action) {
		if (action != null) {
			enableListener();
		}
		pressedAction = action;
	}
	
	public void whenReleased(Action action) {
		if (action != null) {
			enableListener();
		}
		releasedAction = action;
	}
	
	@Override
	public void valueChanged(DigitalPinEvent event) {
		Logger.debug("valuechanged(" + event + ")");
		
		if (pressedAction != null && activeHigh == event.getValue()) {
			pressedAction.action();
		}
		if (releasedAction != null && activeHigh != event.getValue()) {
			releasedAction.action();
		}
		super.valueChanged(event);
	}
	
	@FunctionalInterface
	public interface Action {
		void action();
	}
}
