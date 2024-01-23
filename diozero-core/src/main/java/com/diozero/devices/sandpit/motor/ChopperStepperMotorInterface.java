package com.diozero.devices.sandpit.motor;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     ChopperStepperMotorInterface.java
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

import java.time.Duration;

import com.diozero.api.function.Action;

/**
 * A "chopper" stepper uses direction, speed, and "enable" to drive a
 * controller. These are typically used with bipolar steppers for a
 * higher-degree of accuracy than basic 4-pin controllers. Note that these
 * drivers need "stop/start" signals (typically provided via limit switches),
 * otherwise they can over-drive the mechanisms attached to them.
 *
 * @author E. A. Graham Jr.
 */
public interface ChopperStepperMotorInterface extends StepperMotorInterface {
	/**
	 * Get the default speed of the motor in RPM.
	 *
	 * @return the speed
	 */
	default float getSpeed() {
		return frequencyToRpm(getDefaultFrequency());
	}

	/**
	 * Set the default speed of the motor in RPM. <strong>WARNING!!!!</strong>
	 * Setting the speed (frequency) to other than the manufacturer default may
	 * result in loss of torque.
	 *
	 * @param speedRPM the speed
	 */
	default void setSpeed(float speedRPM) {
		setDefaultFrequency(rpmToFrequency(speedRPM));
	}

	/**
	 * Get the default frequency of the stepper: this is expressed in Hz (or pulse
	 * per second) and is used as the default speed of the motor.
	 *
	 * @return the frequency in Hz
	 */
	int getDefaultFrequency();

	/**
	 * Set the default frequency of the stepper: this is expressed in Hz (or pulse
	 * per second) and is used as the default speed of the motor.
	 * <strong>WARNING!!!!</strong> Setting the frequency to other than the
	 * manufacturer default may result in loss of torque.
	 *
	 * @param frequencyInHz the frequency in Hz
	 */
	void setDefaultFrequency(int frequencyInHz);

	/**
	 * Causes the motor to stop <strong>immediately</strong>. Implementations
	 * <strong>must</strong> ensure that any actions are interrupted when this
	 * method is called.
	 */
	default void stop() {
		((ChopperStepperController) getController()).stop();
	}

	/**
	 * Rotate a relative angle, positive is clockwise. Uses the default (maximum)
	 * speed. <strong>NOTE:</strong> this can be interrupted by calling
	 * {@link #stop()}, but should be implemented as a blocking operation.
	 *
	 * @param angle the angle to rotate through - precision is determined by the
	 *              device
	 */
	default void rotate(float angle) {
		rotate(angle, getSpeed());
	}

	/**
	 * Rotate a relative angle at a constant speed, positive is clockwise. Not all
	 * implementations support this. <strong>NOTE:</strong> this can be interrupted
	 * by calling {@link #stop()}, but should be implemented as a blocking
	 * operation.
	 *
	 * @param angle the angle to rotate through - precision is determined by the
	 *              device
	 * @param speed axle speed in RPM
	 */
	default void rotate(float angle, float speed) {
		throw new UnsupportedOperationException("Rotating to a specific angle is not implemented.");
	}

	/**
	 * Rotate a relative angle at a constant speed, positive is clockwise. Not all
	 * implementations support this. The graph is roughly:
	 * 
	 * <pre>
	 *       _______
	 *      /       \
	 *     /         \
	 * ___/           \____
	 * </pre>
	 * 
	 * <strong>NOTE:</strong> this can be interrupted by calling {@link #stop()},
	 * but should be implemented as a blocking operation.
	 *
	 * @param angle            the angle to rotate through - precision is determined
	 *                         by the device
	 * @param maxSpeed         <strong>maximum</strong> axle speed in RPM
	 * @param accelerationTime the time to accelerate/decelerate
	 */
	default void rotate(float angle, float maxSpeed, Duration accelerationTime) {
		throw new UnsupportedOperationException("Accelerated rotation is not implemented.");
	}

	/**
	 * Starts rotating in the direction noted, using the default/current speed. This
	 * method <strong>must</strong> be implemented non-blocking. The motor can be
	 * stopped with the {@link #stop()} method.
	 *
	 * @param direction the direction
	 */
	default void start(StepperMotorInterface.Direction direction) {
		start(direction, getSpeed());
	}

	/**
	 * Starts rotating in the direction noted. This method <strong>must</strong> be
	 * implemented non-blocking. The motor can be stopped with the {@link #stop()}
	 * method.
	 *
	 * @param direction the direction
	 * @param speed     axle speed in RPM (this <strong>may</strong> change the
	 *                  default speed setting.\)
	 */
	void start(StepperMotorInterface.Direction direction, float speed);

	/**
	 * Add a listener for events. Listeners should only be notified on
	 * {@link #rotate(float)} and {@link #stop()} methods.
	 *
	 * @param listener the listener to add
	 */
	void addEventListener(StepperMotorEventListener listener);

	/**
	 * Remove a listener for events.
	 *
	 * @param listener the listener to remove
	 */
	void removeEventListener(StepperMotorEventListener listener);

	/**
	 * Perform the action when the motor starts moving. This should only be fired on
	 * {@link #rotate(float)} methods.
	 *
	 * @param action the action
	 */
	void onMove(Action action);

	/**
	 * Perform the action when the motor stops moving. This should only be fired on
	 * {@link #rotate(float)} and {@link #stop()} methods.
	 *
	 * @param action the action
	 */
	void onStop(Action action);

	/**
	 * Translates RPM to frequency (in Hz) for this motor.
	 *
	 * @param rpm the rpm
	 * @return the frequency in Hz
	 */
	default int rpmToFrequency(float rpm) {
		return Math.round(rpm / ((getStrideAngle() / 360f) * 60f));
	}

	/**
	 * Translates frequency (in Hz) to RPM for this motor.
	 *
	 * @param frequency the frequency in Hz
	 * @return the rpm
	 */
	default float frequencyToRpm(int frequency) {
		return (getStrideAngle() / 360f) * frequency * 60f;
	}
}
