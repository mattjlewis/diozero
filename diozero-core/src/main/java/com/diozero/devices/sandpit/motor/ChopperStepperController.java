package com.diozero.devices.sandpit.motor;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     ChopperStepperController.java
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

import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.PwmOutputDevice;
import com.diozero.api.RuntimeIOException;
import com.diozero.devices.sandpit.motor.StepperMotorInterface.Direction;

/**
 * A basic device for a stepper driven by a "chopper" driver: uses 3 GPIO pins (enable, direction, frequency) to
 * control a motor.
 * <p>
 * The direction pin starts "low" for <b>clockwise</b> operations.
 *
 * @author E. A. Graham Jr.
 */
public interface ChopperStepperController extends StepperMotorInterface.StepperMotorController {
    /**
     * Status of the "enable" pin.
     *
     * @return {@code true} if the driver is enabled
     */
    boolean isEnabled();

    /**
     * Set enabled or not.
     *
     * @param enabled turns on the driver if {@code true}
     */
    void setEnabled(boolean enabled);

    /**
     * Gets the current direction of rotation. This is in relation to the <i>face</i> of the motor.
     *
     * @return which way
     */
    Direction getDirection();

    /**
     * Set the current direction of rotation. This is in relation to the <i>face</i> of the motor.
     *
     * @param direction which way
     */
    void setDirection(Direction direction);

    /**
     * Get the current set frequency in Hz of the speed-control device.
     *
     * @return current PWM frequency
     */
    int getFrequency();

    /**
     * Set the current set frequency in Hz of the speed-control device.
     *
     * @param frequency the PWM frequency
     */
    void setFrequency(int frequency);

    /**
     * Whether the stepper is supposedly running or not.
     *
     * @return {@code true} if the PWM device is on
     */
    boolean isRunning();

    /**
     * Enable the PWM device.
     */
    void run();

    void stop();

    /**
     * Base implementation.
     */
    class BasicChopperController implements ChopperStepperController {
        private final DigitalOutputDevice enableDevice;
        private final DigitalOutputDevice directionSet;
        private final PwmOutputDevice stepControl;

        /**
         * Basic constructor for this type of driver.
         *
         * @param enablePin    enable the driver on/off
         * @param directionPin set the direction (typically <i>low</i> is <b>clockwise</b>)
         * @param stepPin      controls the <i>frequency</i> of the motor and whether stepping or not
         */
        public BasicChopperController(int enablePin, int directionPin, int stepPin) {
            enableDevice = new DigitalOutputDevice(enablePin, false, false);
            directionSet = new DigitalOutputDevice(directionPin, true, false);
            stepControl = new PwmOutputDevice(stepPin);
        }

        @Override
        public boolean isEnabled() {
            return enableDevice.isOn();
        }

        @Override
        public void setEnabled(boolean enabled) {
            enableDevice.setOn(enabled);
        }

        @Override
        public Direction getDirection() {
            return directionSet.isOn() ? Direction.COUNTERCLOCKWISE : Direction.CW;
        }

        @Override
        public void setDirection(Direction direction) {
            directionSet.setOn(direction.getDirection() == Direction.COUNTERCLOCKWISE.getDirection());
        }

        @Override
        public int getFrequency() {
            return stepControl.getPwmFrequency();
        }

        @Override
        public void setFrequency(int frequency) {
            stepControl.setPwmFrequency(frequency);
        }

        @Override
        public boolean isRunning() {
            return stepControl.isOn();
        }

        @Override
        public void run() {
            stepControl.setValue(.5f);
        }

        @Override
        public void stop() {
            stepControl.setValue(0f);
        }

        @Override
        public void release() {
            stop();
            setEnabled(false);
        }

        @Override
        public void close() throws RuntimeIOException {
            enableDevice.close();
            directionSet.close();
            stepControl.close();
        }
    }

    /**
     * Some drivers can "multiply" the number of steps per rotation by micro-stepping. This is usually a
     * <b>hardware</b> setup or controlled via on-board logic. It is <b>not</b> settable via this driver class.
     * <p>
     * This is typically used for {@link SilentStepStick} steppers.
     */
    class FrequencyMultiplierChopperController extends BasicChopperController {

        /**
         * Human-readable multiplier values.
         */
        public enum Resolution {
            FULL(1),
            HALF(2),
            QUARTER(4),
            EIGHTH(8),
            SIXTEENTH(16),
            THIRTY_SECOND(32),
            SIXTY_FOURTH(64),
            ONE_THIRTY_TWO(132);

            private final int multiplier;

            Resolution(int resolution) {
                this.multiplier = resolution;
            }

            public int multiplier() {
                return multiplier;
            }
        }

        private Resolution resolution = Resolution.FULL;

        public FrequencyMultiplierChopperController(int enablePin, int directionPin, int stepPin) {
            super(enablePin, directionPin, stepPin);
        }

        public Resolution getResolution() {
            return resolution;
        }

        public void setResolution(Resolution resolution) {
            this.resolution = resolution;
        }
    }
}
