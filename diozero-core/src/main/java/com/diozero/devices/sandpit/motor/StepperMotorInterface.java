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

import com.diozero.api.DeviceInterface;

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
         * Should allow the spindle to move freely.
         */
        void release();
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
     * Move a single step in the given direction at the default speed. No events are fired for this action. Not all
     * implementations support this.
     *
     * @param direction which way to rotate
     */
    default void step(Direction direction) {
        throw new UnsupportedOperationException("Single-stepping is not implemented.");
    }

}
