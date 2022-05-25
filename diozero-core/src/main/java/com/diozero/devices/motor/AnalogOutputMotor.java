package com.diozero.devices.motor;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     AnalogOutputMotor.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2022 diozero
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

import com.diozero.api.AnalogOutputDevice;
import com.diozero.api.RuntimeIOException;

public class AnalogOutputMotor extends MotorBase {
	private AnalogOutputDevice aout;

	public AnalogOutputMotor(AnalogOutputDevice aout) {
		this.aout = aout;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void forward(float speed) throws RuntimeIOException {
		aout.setValue(Math.abs(speed));
		valueChanged(speed);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void backward(float speed) throws RuntimeIOException {
		aout.setValue(Math.abs(speed) * -1);
		valueChanged(Math.abs(speed) * -1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void stop() throws RuntimeIOException {
		aout.setValue(0);
		valueChanged(0);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public float getValue() throws RuntimeIOException {
		return aout.getValue();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isActive() throws RuntimeIOException {
		return aout.getValue() != 0;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() {
		stop();
	}
}
