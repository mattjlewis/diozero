package com.diozero.api.motor;

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

import com.diozero.api.PwmOutputDevice;
import com.diozero.internal.DeviceFactoryHelper;
import com.diozero.internal.spi.PwmOutputDeviceFactoryInterface;
import com.diozero.util.RuntimeIOException;

/**
 * Generic bi-directional motor controlled by separate forward / backward PWM output pins
 */
public class PwmMotor extends MotorBase {
	private PwmOutputDevice forward;
	private PwmOutputDevice backward;

	public PwmMotor(int forwardPwmPin, int backwardPwmPin) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), forwardPwmPin, backwardPwmPin);
	}

	public PwmMotor(PwmOutputDeviceFactoryInterface deviceFactory, int forwardPwmPin, int backwardPwmPin) throws RuntimeIOException {
		forward = new PwmOutputDevice(deviceFactory, forwardPwmPin, 0);
		backward = new PwmOutputDevice(deviceFactory, backwardPwmPin, 0);
	}

	@Override
	public void close() {
		Logger.debug("close()");
		forward.close();
		backward.close();
	}

	/**
	 * Forward at full speed
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void forward() throws RuntimeIOException {
		forward(1);
	}

	/**
	 * @param speed
	 *            Range 0..1
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	@Override
	public void forward(float speed) throws RuntimeIOException {
		backward.off();
		forward.setValue(speed);
		valueChanged(speed);
	}

	/**
	 * Backward at full speed
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void backward() throws RuntimeIOException {
		backward(1);
	}

	/**
	 * @param speed
	 *            Range 0..1
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	@Override
	public void backward(float speed) throws RuntimeIOException {
		forward.off();
		backward.setValue(speed);
		valueChanged(-speed);
	}

	@Override
	public void stop() throws RuntimeIOException {
		forward.off();
		backward.off();
		valueChanged(0);
	}

	/**
	 * Represents the speed of the motor as a floating point value between -1
	 * (full speed backward) and 1 (full speed forward)
	 * @return current relative motor speed
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	@Override
	public float getValue() throws RuntimeIOException {
		return forward.getValue() - backward.getValue();
	}

	@Override
	public boolean isActive() throws RuntimeIOException {
		return forward.isOn() || backward.isOn();
	}
}
