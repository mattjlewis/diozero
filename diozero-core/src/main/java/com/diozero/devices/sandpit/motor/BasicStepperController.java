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

import java.util.Arrays;

import com.diozero.api.DeviceInterface;
import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.RuntimeIOException;
import com.diozero.util.SleepUtil;

import static com.diozero.util.SleepUtil.NS_IN_MS;

/**
 * The controller for a stepper motor.
 */
public interface BasicStepperController extends DeviceInterface {
    /**
     * Execute a single "step" (the smallest movable increment) in a clockwise direction. For example, a 4-wire
     * half-stepper, this would be a half-step.
     *
     * @param nanos interval in <b>nanoseconds</b> to wait between pin triggers
     */
    void stepForward(long nanos);

    /**
     * Execute a single "step" (the smallest movable increment) in a counter-clockwise direction. For example, a 4-wire
     * half-stepper, this would be a half-step.
     *
     * @param nanos interval in <b>nanoseconds</b> to wait between pin triggers
     */
    void stepBackward(long nanos);

    /**
     * Stop the rotation immediately.
     */
    void stop();

    interface PinOut {
        void setValue(boolean onOff);

        void close();
    }

    /**
     * Unipolar controller that uses discrete pins/wires on the motor for each phase.
     */
    abstract class FiveWireUnipolarController implements BasicStepperController {
        // basically "fire these pins" in order to rotate
        private static final int[] FULL_STEPS = new int[] { 0b1000, 0b0100, 0b0010, 0b0001 };
        private static final int[] HALF_STEPS = new int[] {
                0b1000, 0b1100, 0b0100, 0b0110, 0b0010, 0b0011, 0b0001, 0b1001
        };

        private final int[] pinMap;

        private int lastStepCompleted = -1;

        protected final PinOut[] pins;

        protected final boolean useHalfSteps;

        protected FiveWireUnipolarController(PinOut[] pins) {
            this(pins, false);
        }

        protected FiveWireUnipolarController(PinOut[] pins, boolean useHalfSteps) {
            this.pins = pins.clone();
            this.useHalfSteps = useHalfSteps;
            if (useHalfSteps) {
                System.out.println("Doing half-steps");
                pinMap = HALF_STEPS;
            }
            else {
                System.out.println("Doing full-steps");
                pinMap = FULL_STEPS;
            }
        }

        /**
         * Whether this controller is configured to use half-steps or not.
         *
         * @return {@code true} if so configured
         */
        public boolean usesHalfSteps() {
            return useHalfSteps;
        }

        public void stepForward(long nanos) {
            long pause = nanos / 4; // phases
            if (useHalfSteps) pause /= 2;
            for (int i = 0; i < pinMap.length; i++) {
                lastStepCompleted++;
                if (lastStepCompleted >= pinMap.length) lastStepCompleted = 0;
                firePins(pinMap[lastStepCompleted], pause);
            }
        }

        public void stepBackward(long nanos) {
            long pause = nanos / 4; // 4 phases
            if (useHalfSteps) pause /= 2;
            for (int i = 0; i < pinMap.length; i++) {
                lastStepCompleted--;
                if (lastStepCompleted < 0) lastStepCompleted = pinMap.length - 1;
                firePins(pinMap[lastStepCompleted], pause);
            }
        }

        @Override
        public void stop() {
            firePins(0, NS_IN_MS * pins.length);
        }

        @Override
        public void close() throws RuntimeIOException {
            for (PinOut pin : pins) {
                pin.close();
            }
        }

        /**
         * Fire the pins according to the bitmap for a single cycle.
         * <p>
         * <b>WARNING!</b> This method is synchronized to prevent potential "over-writes" when running.
         *
         * @param bitMap the map of pins to fire
         * @param nanos  pause time in nanoseconds
         */
        protected synchronized void firePins(int bitMap, long nanos) {
            for (int i = 0; i < pins.length; i++) {
                int bitSelect = 1 << i;
                boolean isOn = (bitMap & bitSelect) != 0;
                pins[i].setValue(isOn);
            }
            SleepUtil.busySleep(nanos);
        }
    }

    /**
     * Decorator for a GPIO pin.
     */
    class GpioPinOut implements PinOut {
        private final DigitalOutputDevice dod;

        public GpioPinOut(DigitalOutputDevice dod) {
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
     * Set up a GPIO-based controller.
     */
    class GpioFiveWireUnipolarController extends FiveWireUnipolarController {
        public GpioFiveWireUnipolarController(DigitalOutputDevice pinA,
                                              DigitalOutputDevice pinB,
                                              DigitalOutputDevice pinC,
                                              DigitalOutputDevice pinD) {
            this(pinA, pinB, pinC, pinD, false);
        }

        public GpioFiveWireUnipolarController(DigitalOutputDevice pinA,
                                              DigitalOutputDevice pinB,
                                              DigitalOutputDevice pinC,
                                              DigitalOutputDevice pinD,
                                              boolean useHalfSteps) {
            super(new GpioPinOut[] {
                    new GpioPinOut(pinA),
                    new GpioPinOut(pinB),
                    new GpioPinOut(pinC),
                    new GpioPinOut(pinD)
            }, useHalfSteps);
        }

        public GpioFiveWireUnipolarController(DigitalOutputDevice[] pins) {
            this(pins, false);
        }

        public GpioFiveWireUnipolarController(DigitalOutputDevice[] pins, boolean useHalfSteps) {
            super(Arrays.stream(pins).map(GpioPinOut::new).toArray(PinOut[]::new), useHalfSteps);
        }

        public GpioFiveWireUnipolarController(int[] pinNumbers) {
            this(pinNumbers, false);
        }

        public GpioFiveWireUnipolarController(int[] pinNumbers, boolean useHalfSteps) {
            super(Arrays.stream(pinNumbers)
                          .mapToObj(GpioFiveWireUnipolarController::apply)
                          .toArray(PinOut[]::new),
                  useHalfSteps);
        }

        private static GpioPinOut apply(int gpio) {
            return new GpioPinOut(new DigitalOutputDevice(gpio));
        }
    }
}
