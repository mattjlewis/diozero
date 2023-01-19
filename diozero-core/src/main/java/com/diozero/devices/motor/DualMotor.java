package com.diozero.devices.motor;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     DualMotor.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2023 diozero
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

import org.tinylog.Logger;

import com.diozero.api.DeviceInterface;
import com.diozero.api.RuntimeIOException;

/**
 * Generic dual bi-directional motor driver. Assumes that the motors are
 * arranged in a left / right orientation.
 */
public class DualMotor implements DeviceInterface {
	private MotorInterface motorA;
	private MotorInterface motorB;

	public DualMotor(MotorInterface motorA, MotorInterface motorB) {
		this.motorA = motorA;
		this.motorB = motorB;
	}

	@Override
	public void close() {
		Logger.trace("close()");
		if (motorA != null) {
			motorA.close();
		}
		if (motorB != null) {
			motorB.close();
		}
	}

	public float[] getValues() throws RuntimeIOException {
		return new float[] { motorA.getValue(), motorB.getValue() };
	}

	/**
	 * Set the speed and direction for both motors (clockwise / counter-clockwise)
	 *
	 * @param leftValue  Range -1 .. 1. Positive numbers for clockwise, Negative
	 *                   numbers for counter clockwise
	 * @param rightValue Range -1 .. 1. Positive numbers for clockwise, Negative
	 *                   numbers for counter clockwise
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void setValues(float leftValue, float rightValue) throws RuntimeIOException {
		motorA.setValue(leftValue);
		motorB.setValue(rightValue);
	}

	public void forward(float speed) throws RuntimeIOException {
		motorA.forward(speed);
		motorB.forward(speed);
	}

	public void backward(float speed) throws RuntimeIOException {
		motorA.backward(speed);
		motorB.backward(speed);
	}

	public void rotateLeft(float speed) throws RuntimeIOException {
		motorA.backward(speed);
		motorB.forward(speed);
	}

	public void rotateRight(float speed) throws RuntimeIOException {
		motorA.forward(speed);
		motorB.backward(speed);
	}

	public void forwardLeft(float speed) throws RuntimeIOException {
		motorA.stop();
		motorB.forward(speed);
	}

	public void forwardRight(float speed) throws RuntimeIOException {
		motorA.forward(speed);
		motorB.stop();
	}

	public void backwardLeft(float speed) throws RuntimeIOException {
		motorA.stop();
		motorB.backward(speed);
	}

	public void backwardRight(float speed) throws RuntimeIOException {
		motorA.backward(speed);
		motorB.stop();
	}

	public void reverseDirection() throws RuntimeIOException {
		motorA.reverseDirection();
		motorB.reverseDirection();
	}

	public void circleLeft(float speed, float turnRate) {
		setValues(speed, speed - turnRate);
	}

	public void circleRight(float speed, float turnRate) {
		setValues(speed - turnRate, speed);
	}

	public void stop() throws RuntimeIOException {
		motorA.stop();
		motorB.stop();
	}

	public MotorInterface getMotorA() {
		return motorA;
	}

	public MotorInterface getMotorB() {
		return motorA;
	}
}
