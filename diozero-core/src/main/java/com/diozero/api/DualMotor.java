package com.diozero.api;

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


import java.io.Closeable;
import java.io.IOException;

import org.pmw.tinylog.Logger;

import com.diozero.util.RuntimeIOException;

/**
 * Generic dual bi-directional motor driver
 */
public class DualMotor implements Closeable {
	private MotorInterface leftMotor;
	private MotorInterface rightMotor;
	
	public DualMotor(MotorInterface leftMotor, MotorInterface rightMotor) {
		this.leftMotor = leftMotor;
		this.rightMotor = rightMotor;
	}

	@Override
	public void close() {
		Logger.debug("close()");
		if (leftMotor != null) { try { leftMotor.close(); } catch (IOException e) { } }
		if (rightMotor != null) { try { rightMotor.close(); } catch (IOException e) { } }
	}
	
	public float[] getValues() throws RuntimeIOException {
		return new float[] { leftMotor.getValue(), rightMotor.getValue() };
	}
	
	/**
	 * Set the speed and direction for both motors (clockwise / counter-clockwise)
	 * @param speed Range -1 .. 1. Positive numbers for clockwise, Negative numbers for counter clockwise
	 * @throws RuntimeIOException
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
		rightMotor.forward(speed);
		leftMotor.backward(speed);
	}
	
	public void rotateRight(float speed) throws RuntimeIOException {
		leftMotor.forward(speed);
		rightMotor.backward(speed);
	}
	
	public void reverse() throws RuntimeIOException {
		leftMotor.reverse();
		rightMotor.reverse();
	}
	
	public void stop() throws RuntimeIOException {
		leftMotor.stop();
		rightMotor.stop();
	}
}
