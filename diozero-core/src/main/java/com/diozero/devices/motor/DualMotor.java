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

import org.tinylog.Logger;

import com.diozero.api.DeviceInterface;
import com.diozero.api.RuntimeIOException;

/**
 * Generic dual bi-directional motor driver
 */
public class DualMotor implements DeviceInterface {
	private MotorInterface leftMotor;
	private MotorInterface rightMotor;
	
	public DualMotor(MotorInterface leftMotor, MotorInterface rightMotor) {
		this.leftMotor = leftMotor;
		this.rightMotor = rightMotor;
	}

	@Override
	public void close() {
		Logger.trace("close()");
		if (leftMotor != null) {
			leftMotor.close();
		}
		if (rightMotor != null) {
			rightMotor.close();
		}
	}
	
	public float[] getValues() throws RuntimeIOException {
		return new float[] { leftMotor.getValue(), rightMotor.getValue() };
	}
	
	/**
	 * Set the speed and direction for both motors (clockwise / counter-clockwise)
	 * @param leftValue Range -1 .. 1. Positive numbers for clockwise, Negative numbers for counter clockwise
	 * @param rightValue Range -1 .. 1. Positive numbers for clockwise, Negative numbers for counter clockwise
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void setValues(float leftValue, float rightValue) throws RuntimeIOException {
		leftMotor.setValue(leftValue);
		rightMotor.setValue(rightValue);
	}
	
	public void forward(float speed) throws RuntimeIOException {
		leftMotor.forward(speed);
		rightMotor.forward(speed);
	}
	
	public void backward(float speed) throws RuntimeIOException {
		leftMotor.backward(speed);
		rightMotor.backward(speed);
	}
	
	public void rotateLeft(float speed) throws RuntimeIOException {
		leftMotor.backward(speed);
		rightMotor.forward(speed);
	}
	
	public void rotateRight(float speed) throws RuntimeIOException {
		leftMotor.forward(speed);
		rightMotor.backward(speed);
	}
	
	public void forwardLeft(float speed) throws RuntimeIOException {
		leftMotor.stop();
		rightMotor.forward(speed);
	}
	
	public void forwardRight(float speed) throws RuntimeIOException {
		leftMotor.forward(speed);
		rightMotor.stop();
	}
	
	public void backwardLeft(float speed) throws RuntimeIOException {
		leftMotor.stop();
		rightMotor.backward(speed);
	}
	
	public void backwardRight(float speed) throws RuntimeIOException {
		leftMotor.backward(speed);
		rightMotor.stop();
	}
	
	public void reverse() throws RuntimeIOException {
		leftMotor.reverse();
		rightMotor.reverse();
	}
	
	public void circleLeft(float speed, float turnRate) {
		setValues(speed, speed-turnRate);
	}
	
	public void circleRight(float speed, float turnRate) {
		setValues(speed-turnRate, speed);
	}
	
	public void stop() throws RuntimeIOException {
		leftMotor.stop();
		rightMotor.stop();
	}
	
	public MotorInterface getLeftMotor() {
		return leftMotor;
	}
	
	public MotorInterface getRightMotor() {
		return leftMotor;
	}
}
