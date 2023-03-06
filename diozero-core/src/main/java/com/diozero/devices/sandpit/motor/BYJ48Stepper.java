package com.diozero.devices.sandpit.motor;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     BYJ48Stepper.java
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

/**
 * Commonly found 28BYJ-48 stepper motor. Typically driven by a ULN2003 driver chip. The default frequency is for a
 * freely-running motor with no load.
 * <p>
 * Datasheet - https://components101.com/sites/default/files/component_datasheet/28byj48-step-motor-datasheet.pdf
 * <p>
 * Notes:
 * <ul>
 *     <li>here are apparently two variations with different gear ratios - the default is for the 16:1</li>
 *     <li>motion is "smoother" when the controller is in "half-step" mode, but torque will be variable</li>
 * </ul>
 *
 * @author E. A. Graham Jr.
 */
public class BYJ48Stepper extends AbstractUnipolarStepperMotor {
    public static final int MAX_STEPS_PER_ROTATION = 512;

    /**
     * Default constructor for 16:1 gear box.
     *
     * @param controller the controller
     */
    public BYJ48Stepper(UnipolarStepperController controller) {
        this(controller, false);
    }

    /**
     * Constructor with settable gear ratio. This should be either 16 or 64
     * <p>
     * Note: these are not the <i>exact</i> values, but the accuracy is "good enough" for these motors.
     *
     * @param controller the controller
     * @param largeRatio {@code true} if the motor is using the larger gear-box ratio
     */
    public BYJ48Stepper(UnipolarStepperController controller, boolean largeRatio) {
        super(largeRatio ? (MAX_STEPS_PER_ROTATION * 4) : MAX_STEPS_PER_ROTATION, controller);

        // spec sheet calls for 100Hz pulse
        setDefaultFrequency(100);
    }
}
