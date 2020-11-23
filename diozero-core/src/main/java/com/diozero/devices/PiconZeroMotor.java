package com.diozero.devices;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     PiconZeroMotor.java  
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

import java.io.IOException;

import com.diozero.api.RuntimeIOException;
import com.diozero.devices.motor.MotorBase;

public class PiconZeroMotor extends MotorBase {
	private static final int MAX_FORWARD_SPEED = 127;
	private static final int MAX_BACKWARD_SPEED = -128;
	private PiconZero piconZero;
	private int motor;
	
	public PiconZeroMotor(PiconZero piconZero, int motor) {
		this.piconZero = piconZero;
		this.motor = motor;
	}

	@Override
	public void forward(float speed) throws RuntimeIOException {
		piconZero.setMotor(motor, Math.round(Math.abs(speed) * MAX_FORWARD_SPEED));
	}

	@Override
	public void backward(float speed) throws RuntimeIOException {
		piconZero.setMotor(motor, Math.round(Math.abs(speed) * MAX_BACKWARD_SPEED));
	}

	@Override
	public void stop() throws RuntimeIOException {
		piconZero.setMotor(motor, 0);
	}

	/**
	 * Get the relative output value for the motor
	 * @return -1 for full reverse, 1 for full forward, 0 for stop
	 */
	@Override
	public float getValue() throws RuntimeIOException {
		return piconZero.getMotor(motor) / Math.abs(MAX_BACKWARD_SPEED);
	}

	@Override
	public boolean isActive() throws RuntimeIOException {
		return piconZero.getMotor(motor) != 0;
	}

	@Override
	public void close() throws IOException {
		stop();
	}
}
