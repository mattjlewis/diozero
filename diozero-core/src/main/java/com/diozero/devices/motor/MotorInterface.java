package com.diozero.devices.motor;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     MotorInterface.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2024 diozero
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

import com.diozero.api.DeviceInterface;
import com.diozero.api.RuntimeIOException;
import com.diozero.api.function.Action;

public interface MotorInterface extends DeviceInterface {
	/**
	 * Set the motor direction to forward at the given speed.
	 *
	 * @param speed normalised speed value, range 0..1
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	void forward(float speed) throws RuntimeIOException;

	/**
	 * Set the motor direction to backward at the given speed.
	 *
	 * @param speed normalised speed value, range 0..1
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	void backward(float speed) throws RuntimeIOException;

	/**
	 * Forward at full speed
	 *
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	default void forward() throws RuntimeIOException {
		forward(1);
	}

	/**
	 * Backward at full speed
	 *
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	default void backward() throws RuntimeIOException {
		backward(1);
	}

	/**
	 * Stop the motor
	 *
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	void stop() throws RuntimeIOException;

	/**
	 * Reverse direction of the motors
	 *
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	default void reverseDirection() throws RuntimeIOException {
		setValue(-getValue());
	}

	/**
	 * Get the relative output speed of the motor in the range -1..1
	 *
	 * @return -1 for full reverse, 1 for full forward, 0 for stop
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	float getValue() throws RuntimeIOException;

	/**
	 * Set the relative speed of the motor as a floating point value between -1
	 * (full speed backward) and 1 (full speed forward)
	 *
	 * @param value Range -1..1. Positive numbers = forward, negative numbers =
	 *              backward
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	default void setValue(float value) throws RuntimeIOException {
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

	/**
	 * Is this motor currently active?
	 *
	 * @return true if speed != 0
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	boolean isActive() throws RuntimeIOException;

	/**
	 * Perform the given action when the direction of the motor is changed to
	 * forwards
	 *
	 * @param action action to perform
	 */
	void whenForward(Action action);

	/**
	 * Perform the given action when the direction of the motor is changed to
	 * backwards
	 *
	 * @param action action to perform
	 */
	void whenBackward(Action action);

	/**
	 * Perform the given action when the motor is stopped
	 *
	 * @param action action to perform
	 */
	void whenStop(Action action);

	void addListener(MotorEventListener listener);

	void removeListener(MotorEventListener listener);
}
