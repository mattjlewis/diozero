package com.diozero.devices.motor;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     MotorBase.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2021 diozero
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


import java.util.ArrayList;
import java.util.List;

import com.diozero.api.RuntimeIOException;
import com.diozero.api.function.Action;
import com.diozero.api.function.FloatConsumer;

public abstract class MotorBase implements MotorInterface, FloatConsumer {
	private Action forwardAction;
	private Action backwardAction;
	private Action stopAction;
	private List<MotorEventListener> listeners;
	
	public MotorBase() {
		listeners = new ArrayList<>();
	}

	/**
	 * Reverse direction of the motors
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	@Override
	public void reverse() throws RuntimeIOException {
		setValue(-getValue());
	}
	
	/**
	 * Set the speed of the motor as a floating point value between -1 (full
	 * speed backward) and 1 (full speed forward)
	 * @param value Range -1 .. 1. Positive numbers for forward, Negative numbers for backward
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	@Override
	public void setValue(float value) throws RuntimeIOException {
		if (value < -1 || value > 1) {
			throw new IllegalArgumentException("Motor value must be between -1 and 1");
		}
		if (value > 0) {
			forward(value);
		} else if (value < 0) {
			backward(-value);
		} else {
			stop();
		}
	}

	@Override
	public void whenForward(Action action) {
		forwardAction = action;
	}

	@Override
	public void whenBackward(Action action) {
		backwardAction = action;
	}

	@Override
	public void whenStop(Action action) {
		stopAction = action;
	}
	
	@Override
	public void addListener(MotorEventListener listener) {
		listeners.add(listener);
	}
	
	@Override
	public void removeListener(MotorEventListener listener) {
		listeners.remove(listener);
	}
	
	@Override
	public void accept(float value) {
		MotorEvent event = new MotorEvent(System.currentTimeMillis(), System.nanoTime(), value);
		listeners.forEach((listener) -> listener.accept(event));
		
		if (value > 0) {
			if (forwardAction != null) {
				forwardAction.action();
			}
		} else if (value < 0) {
			if (backwardAction != null) {
				backwardAction.action();
			}
		} else {
			if (stopAction != null) {
				stopAction.action();
			}
		}
	}
}
