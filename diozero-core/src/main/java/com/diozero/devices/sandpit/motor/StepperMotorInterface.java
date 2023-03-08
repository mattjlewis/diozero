package com.diozero.devices.sandpit.motor;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     StepperMotorInterface.java
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

import java.time.Duration;

import com.diozero.api.DeviceInterface;
import com.diozero.api.function.Action;

/**
 * Common interface for stepper motors. The rotational speed is determined by the time allowed between steps and the
 * number of steps to complete a rotation.
 * <p>
 * Note that the {@link #getStrideAngle()} and {@link #getStepsPerRotation()} represent the same physical
 * characteristics of the motor.
 * <p>
 * <b>IMPORTANT NOTE!</b> Implementation/usage should not exceed the maximum rotational speed of the device, otherwise
 * bad things can happen.
 *
 * @author E. A. Graham Jr.
 * @see <a href="https://learn.adafruit.com/all-about-stepper-motors?view=all">What is a Stepper Motor?</a>
 */
public interface StepperMotorInterface extends DeviceInterface {
    /**
     * Which way to rotate: this is relative to observing the <i>face</i> of the motor - exactly like a clock.
     */
    enum Direction {
        CLOCKWISE(1),
        COUNTERCLOCKWISE(-1),
        CW(1),
        CCW(-1),
        FORWARD(1),
        BACKWARD(-1);

        private final int direction;

        Direction(int d) {
            direction = d;
        }

        public int getDirection() {
            return direction;
        }
    }

    interface StepperMotorController extends DeviceInterface {
        /**
         * Stops the motor immediately.
         */
        void stop();
    }

    StepperMotorController getController();

    /**
     * Get the stride-angle (degrees per step) for this motor. This <b>MUST</b> take into account any gearboxes.
     *
     * @return the angle
     */
    float getStrideAngle();

    /**
     * Get the number of full steps in a full rotation of the motor shaft. This <b>MUST</b> take into account any
     * gearboxes.
     *
     * @return the steps
     */
    long getStepsPerRotation();

    /**
     * Get the default speed of the motor in RPM.
     *
     * @return the speed
     */
    default float getSpeed() {
        return frequencyToRpm(getDefaultFrequency());
    }

    /**
     * Set the default speed of the motor in RPM.
     * <p>
     * <b>WARNING!!!!</b> Setting the speed (frequency) to other than the manufacturer default may result in loss of
     * torque.
     *
     * @param speedRPM the speed
     */
    default void setSpeed(float speedRPM) {
        setDefaultFrequency(rpmToFrequency(speedRPM));
    }

    /**
     * Get the default frequency of the stepper: this is expressed in Hz (or pulse per second) and is used as the
     * default speed of the motor.
     *
     * @return the frequency in Hz
     */
    int getDefaultFrequency();

    /**
     * Set the default frequency of the stepper: this is expressed in Hz (or pulse per second) and is used as the
     * default speed of the motor.
     * <p>
     * <b>WARNING!!!!</b> Setting the frequency to other than the manufacturer default may result in loss of torque.
     *
     * @param frequencyInHz the frequency in Hz
     */
    void setDefaultFrequency(int frequencyInHz);

    /**
     * Move a single step in the given direction at the default speed. No events are fired for this action. Not all
     * implementations support this.
     *
     * @param direction which way to rotate
     */
    default void step(Direction direction) {
        throw new UnsupportedOperationException("Single-stepping is not implemented.");
    }

    /**
     * Causes the motor to stop <b>immediately</b>. Implementations <b>must</b> ensure that any actions are
     * interrupted when this method is called.
     */
    default void stop() {
        getController().stop();
    }

    /**
     * Rotate a relative angle, positive is clockwise. Uses the default (maximum) speed.
     * <p>
     * <b>NOTE:</b>> this can be interrupted by calling {@link #stop()}, but should be implemented as a blocking
     * operation.
     *
     * @param angle the angle to rotate through - precision is determined by the device
     */
    default void rotate(float angle) {
        rotate(angle, getSpeed());
    }

    /**
     * Rotate a relative angle at a constant speed, positive is clockwise. Not all implementations support this.
     * <p>
     * <b>NOTE:</b>> this can be interrupted by calling {@link #stop()}, but should be implemented as a blocking
     * operation.
     *
     * @param angle the angle to rotate through - precision is determined by the device
     * @param speed axle speed in RPM
     */
    default void rotate(float angle, float speed) {
        throw new UnsupportedOperationException("Rotating to a specific angle is not implemented.");
    }

    /**
     * Rotate a relative angle at a constant speed, positive is clockwise. Not all implementations support this. The
     * graph is roughly:
     * <pre>
     *       _______
     *      /       \
     *     /         \
     * ___/           \____
     * </pre>
     * <p>
     * <b>NOTE:</b>> this can be interrupted by calling {@link #stop()}, but should be implemented as a blocking
     * operation.
     *
     * @param angle            the angle to rotate through - precision is determined by the device
     * @param maxSpeed         <b>maximum</b> axle speed in RPM
     * @param accelerationTime the time to accelerate/decelerate
     */
    default void rotate(float angle, float maxSpeed, Duration accelerationTime) {
        throw new UnsupportedOperationException("Accelerated rotation is not implemented.");
    }

    /**
     * Starts rotating in the direction noted, using the default/current speed.
     * <p>
     * This method <b>must</b> be implemented non-blocking. The motor can be stopped with the {@link #stop()} method.
     *
     * @param direction the direction
     */
    default void start(Direction direction) {
        start(direction, getSpeed());
    }

    /**
     * Starts rotating in the direction noted.
     * <p>
     * This method <b>must</b> be implemented non-blocking. The motor can be stopped with the {@link #stop()} method.
     *
     * @param direction the direction
     * @param speed     axle speed in RPM (this <b>may</b> change the default speed setting.\)
     */
    void start(Direction direction, float speed);

    /**
     * Add a listener for events. Listeners should only be notified on {@link #rotate(float)} and {@link #stop()} methods.
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
     * Perform the action when the motor starts moving. This should only be fired on {@link #rotate(float)} methods.
     *
     * @param action the action
     */
    void onMove(Action action);

    /**
     * Perform the action when the motor stops moving.  This should only be fired on {@link #rotate(float)} and
     * {@link #stop()} methods.
     *
     * @param action the action
     */
    void onStop(Action action);

    /**
     * Translates RPM  to frequency (in Hz) for this motor.
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
