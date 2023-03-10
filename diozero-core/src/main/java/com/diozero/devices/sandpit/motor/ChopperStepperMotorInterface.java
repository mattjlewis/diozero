/*
 * Copyright (c) 2023 by the author(s).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diozero.devices.sandpit.motor;

import java.time.Duration;

import com.diozero.api.function.Action;

/**
 * TODO fill this in
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
     * Causes the motor to stop <b>immediately</b>. Implementations <b>must</b> ensure that any actions are
     * interrupted when this method is called.
     */
    default void stop() {
        ((ChopperStepperController)getController()).stop();
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
    default void start(StepperMotorInterface.Direction direction) {
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
    void start(StepperMotorInterface.Direction direction, float speed);

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
