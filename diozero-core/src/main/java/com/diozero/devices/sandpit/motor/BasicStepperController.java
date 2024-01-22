package com.diozero.devices.sandpit.motor;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     BasicStepperController.java
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

import java.util.Objects;

import com.diozero.api.DeviceInterface;
import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.InternalPwmOutputDeviceInterface;

/**
 * The basic stepper controller. This controller basically "fires" the magnets inside the motor directly in a
 * sequence to cause the rotor to turn.
 *
 * @author E. A. Graham Jr.
 */
public interface BasicStepperController extends StepperMotorInterface.StepperMotorController {
    enum StepStyle {
        SINGLE, DOUBLE, INTERLEAVE, MICROSTEP
    }
    /**
     * Execute a single "step" in a clockwise direction in the given style of rotation. Note that not all styles may
     * be supported.
     *
     * @param style how to fire the pins
     */
    void stepForward(StepStyle style);

    /**
     * Execute a single "step" in a counter-clockwise direction in the given style of rotation. Note that not all
     * styles may be supported.
     *
     * @param style how to fire the pins
     */
    void stepBackward(StepStyle style);

    /**
     * Should remove all voltage from the stepper.
     */
    void release();

    interface StepperPin extends DeviceInterface {
        void setValue(boolean onOff);
    }

    /**
     * Basic controller.
     */
    abstract class AbstractBasicController implements BasicStepperController {
        // basically "fire these pins" in order to rotate
        private static final int[] FULL_STEPS = new int[] { 0b1000, 0b0100, 0b0010, 0b0001 };
        // supposedly more torque
        private static final int[] DOUBLE_STEPS = new int[] { 0b1001, 0b1010, 0b0110, 0b0101 };
        // slow - kind of half-steps
        private static final int[] INTERLEAVE_STEPS = new int[] {
                0b1000, 0b1100, 0b0100, 0b0110, 0b0010, 0b0011, 0b0001, 0b1001
        };

        private int lastStepCompleted = -1;
        protected final StepperPin[] pins;

        /**
         * Constructor.
         *
         * @param pins the pins to "fire"
         */
        protected AbstractBasicController(StepperPin[] pins) {
            if (pins.length != 4) throw new IllegalArgumentException("This controller requires 4 pins to operate.");
            this.pins = pins.clone();
        }

        @Override
        public void stepForward(StepStyle style) {
            lastStepCompleted++;
            var pinMap = getPinMapForStyle(style);

            if (lastStepCompleted >= pinMap.length) lastStepCompleted = 0;
            firePins(pinMap[lastStepCompleted]);
        }

        @Override
        public void stepBackward(StepStyle style) {
            lastStepCompleted--;
            var pinMap = getPinMapForStyle(style);

            if (lastStepCompleted < 0) lastStepCompleted = pinMap.length - 1;
            firePins(pinMap[lastStepCompleted]);
        }

        private static int[] getPinMapForStyle(StepStyle style) {
            var pinMap = FULL_STEPS;
            if (style == StepStyle.DOUBLE) pinMap = DOUBLE_STEPS;
            else if (style == StepStyle.INTERLEAVE) pinMap = INTERLEAVE_STEPS;
            return pinMap;
        }

        @Override
        public void release() {
            firePins(0);
        }

        @Override
        public void close() throws RuntimeIOException {
            for (StepperPin pin : pins) {
                pin.close();
            }
        }

        /**
         * Fire the pins according to the bitmap for a single step.
         * <p>
         * <b>WARNING!</b> This method is synchronized to prevent potential "over-writes" when running.
         *
         * @param bitMap the map of pins to fire
         */
        protected synchronized void firePins(int bitMap) {
            for (int i = 0; i < pins.length; i++) {
                var bitSelect = 1 << i;
                var isOn = (bitMap & bitSelect) != 0;
                pins[i].setValue(isOn);
            }
        }
    }

    /**
     * Decorator for a GPIO pin.
     */
    class GpioStepperPin implements StepperPin {
        private final DigitalOutputDevice dod;

        public GpioStepperPin(DigitalOutputDevice dod) {
            this.dod = dod;
        }

        @Override
        public void setValue(boolean onOff) {
            dod.setValue(onOff);
        }

        @Override
        public void close() {
            dod.close();
        }
    }

    /**
     * Decorator for a PWM pin.
     * <p>
     * <b>NOTE:</b> even though "micro-stepping" with PWM is available, it's not implemented as it produces
     * less-than-desirable results for this class of controllers.
     */
    class PwmStepperPin implements StepperPin {
        // TODO this is the "2000" setting from the Adafruit python library
        // TODO for the "base" PWM frequency in the stepper class (very confused)
        public final int DEFAULT_FREQ = 2000;
        private final InternalPwmOutputDeviceInterface aod;

        public PwmStepperPin(InternalPwmOutputDeviceInterface aod) {
            this.aod = aod;
            aod.setChild(true);
            aod.setPwmFrequency(DEFAULT_FREQ);
        }

        @Override
        public void setValue(boolean onOff) {
            aod.setValue(onOff ? 1f : 0f);
        }

        @Override
        public void close() {
            aod.close();
        }
    }

    /**
     * Unipolar controller: pins are in A, B, C, D order
     */
    class UnipolarBasicController extends AbstractBasicController {
        public UnipolarBasicController(StepperPin[] pins) {
            super(pins);
        }
    }

    /**
     * Identifies a "terminal block" connection for bipolar motors.
     */
    class BiPolarTerminal {
        public StepperPin plus;
        public StepperPin minus;

        public BiPolarTerminal(StepperPin plus, StepperPin minus) {
            this.plus = plus;
            this.minus = minus;
        }

        public void validate() {
            Objects.requireNonNull(plus, "'plus' terminal must be supplied");
            Objects.requireNonNull(minus, "'minus' terminal must be supplied");
        }
    }

    /**
     * Bipolar controller: pins are in A+, A-, B+, B- order
     * <p>
     * This effectively re-orders the pairs into the proper "magnet" order.
     */
    class BipolarBasicController extends AbstractBasicController {
        public BipolarBasicController(BiPolarTerminal terminalA, BiPolarTerminal terminalB) {
            super(createPins(terminalA, terminalB));
        }

        private static StepperPin[] createPins(BiPolarTerminal terminalA, BiPolarTerminal terminalB) {
            terminalA.validate();
            terminalB.validate();

            return new StepperPin[] { terminalA.plus, terminalB.plus, terminalA.minus, terminalB.minus };
        }
    }
}
