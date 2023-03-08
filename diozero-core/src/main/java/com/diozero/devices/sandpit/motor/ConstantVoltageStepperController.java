package com.diozero.devices.sandpit.motor;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     ConstantVoltageStepperController.java
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

import java.util.Objects;

import com.diozero.api.AnalogOutputDevice;
import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.RuntimeIOException;
import com.diozero.util.SleepUtil;

import static com.diozero.util.SleepUtil.NS_IN_MS;

/**
 * The controller for a "constant voltage" stepper controller. This controller basically "fires" the magnets inside
 * the motor directly in a sequence to induce torque.
 *
 * @author E. A. Graham Jr.
 */
public interface ConstantVoltageStepperController extends StepperMotorInterface.StepperMotorController {
    /**
     * Execute a single "step" in a clockwise direction.  If the timing is set to the manufacturer's recommendation,
     * this should be at "full torque".
     * <p>
     * TODO see note below about "double-steps" - this supposedly increases torque
     *
     * @param nanos interval in <b>nanoseconds</b> to wait between pin triggers: this effectively controls the
     *              "speed" of the rotation
     */
    void stepForward(long nanos);

    /**
     * For controllers that allow it, only activates the <b>next</b> coil. Use of this method can result in loss
     * of torque. Default is "no op".
     */
    default void microStepForward() {
        // no-op
    }

    /**
     * Execute a single "step" in a counter-clockwise direction. If the timing is set to the manufacturer's
     * recommendation, this should be at "full torque".
     * <p>
     * TODO see note below about "double-steps" - this supposedly increases torque
     *
     * @param nanos interval in <b>nanoseconds</b> to wait between pin triggers: this effectively controls the
     *              "speed" of the rotation
     */
    void stepBackward(long nanos);

    /**
     * For controllers that allow it, only activates the <b>previous</b> coil. Use of this method can result in loss
     * of torque. Default is "no op".
     */
    default void microStepBackward() {
        // no-op
    }

    /**
     * Stop the rotation immediately.
     */
    void stop();

    interface StepperPin {
        void setValue(boolean onOff);

        void close();
    }

    /**
     * Basic CV controller.
     * <p>
     * TODO Add "double step", PWM micro-stepping - see the Adafruit source for details;
     */
    abstract class AbstractConstantVoltageController implements ConstantVoltageStepperController {
        // basically "fire these pins" in order to rotate
        // N.B. Adafruit: "single step"
        private static final int[] FULL_STEPS = new int[] { 0b1000, 0b0100, 0b0010, 0b0001 };
        // N.B. Adafruit: "interleave step"
        private static final int[] HALF_STEPS = new int[] {
                0b1000, 0b1100, 0b0100, 0b0110, 0b0010, 0b0011, 0b0001, 0b1001
        };

        private final int[] pinMap;

        private int lastStepCompleted = -1;

        protected final StepperPin[] pins;

        protected final boolean useHalfSteps;

        protected AbstractConstantVoltageController(StepperPin[] pins) {
            this(pins, false);
        }

        /**
         * Constructor.
         *
         * @param pins         the pins to "fire"
         * @param useHalfSteps whether to use half-steps or not
         */
        protected AbstractConstantVoltageController(StepperPin[] pins, boolean useHalfSteps) {
            this(pins, useHalfSteps ? HALF_STEPS : FULL_STEPS);
        }

        /**
         * Constructor.
         *
         * @param pins           the pins to "fire"
         * @param pinFiringOrder determines which pin is fired in which order as a bit map
         */
        protected AbstractConstantVoltageController(StepperPin[] pins, int[] pinFiringOrder) {
            if (pins.length != 4) throw new IllegalArgumentException("This controller requires 4 pins to operate.");
            this.pins = pins.clone();
            useHalfSteps = pinFiringOrder.length > 4;
            pinMap = pinFiringOrder.clone();
        }

        /**
         * Whether this controller is configured to use half-steps or not.
         *
         * @return {@code true} if so configured
         */
        public boolean usesHalfSteps() {
            return useHalfSteps;
        }

        @Override
        public void microStepForward() {
            lastStepCompleted++;
            if (lastStepCompleted >= pinMap.length) lastStepCompleted = 0;
            firePins(pinMap[lastStepCompleted]);
        }

        @Override
        public void stepForward(long nanos) {
            long pause = nanos / 4; // phases
            if (useHalfSteps) pause /= 2;
            for (int i = 0; i < pinMap.length; i++) {
                microStepForward();
                SleepUtil.busySleep(pause);
            }
        }

        @Override
        public void microStepBackward() {
            lastStepCompleted--;
            if (lastStepCompleted < 0) lastStepCompleted = pinMap.length - 1;
            firePins(pinMap[lastStepCompleted]);
        }

        @Override
        public void stepBackward(long nanos) {
            long pause = nanos / 4; // 4 phases
            if (useHalfSteps) pause /= 2;
            for (int i = 0; i < pinMap.length; i++) {
                microStepBackward();
                SleepUtil.busySleep(pause);
            }
        }

        @Override
        public void stop() {
            firePins(0);
            SleepUtil.busySleep(NS_IN_MS * pins.length);
        }

        @Override
        public void close() throws RuntimeIOException {
            for (StepperPin pin : pins) {
                pin.close();
            }
        }

        /**
         * Fire the pins according to the bitmap for a single cycle.
         * <p>
         * <b>WARNING!</b> This method is synchronized to prevent potential "over-writes" when running.
         *
         * @param bitMap the map of pins to fire
         */
        protected synchronized void firePins(int bitMap) {
            for (int i = 0; i < pins.length; i++) {
                int bitSelect = 1 << i;
                boolean isOn = (bitMap & bitSelect) != 0;
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
     * TODO enhance this to add PWM micro-stepping
     */
    class PwmStepperPin implements StepperPin {
        private final AnalogOutputDevice aod;

        public PwmStepperPin(AnalogOutputDevice aod) {
            this.aod = aod;
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
    class UnipolarCVController extends AbstractConstantVoltageController {
        public UnipolarCVController(StepperPin[] pins) {
            this(pins, false);
        }

        public UnipolarCVController(StepperPin[] pins, boolean useHalfSteps) {
            super(pins, useHalfSteps);
        }
    }

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
    class BipolarCVController extends AbstractConstantVoltageController {
        public BipolarCVController(BiPolarTerminal terminalA, BiPolarTerminal terminalB) {
            this(terminalA, terminalB, false);
        }

        public BipolarCVController(BiPolarTerminal terminalA, BiPolarTerminal terminalB, boolean useHalfSteps) {
            super(createPins(terminalA, terminalB), useHalfSteps);
        }

        private static StepperPin[] createPins(BiPolarTerminal terminalA, BiPolarTerminal terminalB) {
            terminalA.validate();
            terminalB.validate();

            return new StepperPin[] { terminalA.plus, terminalB.minus, terminalA.minus, terminalB.plus };
        }
    }
}
