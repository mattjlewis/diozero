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


import java.io.IOException;

import org.pmw.tinylog.Logger;

import com.diozero.api.DeviceFactoryHelper;
import com.diozero.api.MotorInterface;
import com.diozero.api.PwmOutputDevice;
import com.diozero.internal.spi.PwmOutputDeviceFactoryInterface;

/**
 * Generic bi-directional motor controlled by separate forward / backward PWM output pins
 */
public class Motor implements MotorInterface {
	private PwmOutputDevice forward;
	private PwmOutputDevice backward;

	public Motor(int forwardPin, int backwardPin) throws IOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), forwardPin, backwardPin);
	}

	public Motor(PwmOutputDeviceFactoryInterface deviceFactory, int forwardPin, int backwardPin) throws IOException {
		forward = new PwmOutputDevice(deviceFactory, forwardPin, 0);
		backward = new PwmOutputDevice(deviceFactory, forwardPin, 0);
	}

	@Override
	public void close() {
		Logger.debug("close()");
		forward.close();
		backward.close();
	}

	/**
	 * Forward at full speed
	 * @throws IOException
	 */
	public void forward() throws IOException {
		forward(1);
	}

	/**
	 * Backward at full speed
	 * @throws IOException
	 */
	public void backward() throws IOException {
		backward(1);
	}

	/**
	 * @param speed
	 *            Range 0..1
	 * @throws IOException
	 */
	@Override
	public void forward(float speed) throws IOException {
		backward.off();
		forward.setValue(speed);
	}

	/**
	 * @param speed
	 *            Range 0..1
	 * @throws IOException
	 */
	@Override
	public void backward(float speed) throws IOException {
		forward.off();
		backward.setValue(speed);
	}

	@Override
	public void stop() throws IOException {
		forward.off();
		backward.off();
	}

	/**
	 * Reverse direction of the motors
	 * @throws IOException
	 */
	@Override
	public void reverse() throws IOException {
		setValue(-getValue());
	}

	/**
	 * Represents the speed of the motor as a floating point value between -1
	 * (full speed backward) and 1 (full speed forward)
	 */
	@Override
	public float getValue() throws IOException {
		return forward.getValue() - backward.getValue();
	}
	
	/**
	 * Set the speed of the motor as a floating point value between -1 (full
	 * speed backward) and 1 (full speed forward)
	 * @param value Range -1 .. 1. Positive numbers for forward, Negative numbers for backward
	 * @throws IOException
	 */
	@Override
	public void setValue(float value) throws IOException {
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
	public boolean isActive() throws IOException {
		return forward.isOn() || backward.isOn();
	}
}
