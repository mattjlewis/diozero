package com.diozero.sampleapps.sandpit.motor;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     SilentStepStickTest.java
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

import java.util.regex.Pattern;

import com.diozero.devices.sandpit.motor.ChopperStepperController.FrequencyMultiplierChopperController;
import com.diozero.devices.sandpit.motor.SilentStepStick;
import com.diozero.util.SleepUtil;

/**
 * Sweep a "chopper" stepper (aka "silent step stick") back and forth using two GPIO DigitalOutput and one PWMOutput.
 * The pins default to 5,6,16 for <b>enable</b>, <b>direction</b>, and <b>frequency (PWM)</b>. To use a different set,
 * run with a single argument with the pins as comma-separated values in the same order.
 * <p>
 * The application rotates the stepper approximately 45 degrees in both a clockwise, then counter-clockwise direction.
 *
 * @author E. A. Graham Jr.
 */
public class SilentStepStickTest {
    public static void main(String[] args) throws Exception {
        int enablePin = 5;
        int directionPin =  6;
        int stepPin = 16;

        if (args.length == 1 && Pattern.matches("\\d+,\\d+,\\d+", args[0])) {
            String[] pins = args[0].split(",");
            enablePin = Integer.parseInt(pins[0]);
            directionPin = Integer.parseInt(pins[1]);
            stepPin = Integer.parseInt(pins[2]);
        }

        try (FrequencyMultiplierChopperController controller =
                     new FrequencyMultiplierChopperController(enablePin, directionPin, stepPin)){
            try (SilentStepStick stick = new SilentStepStick(controller)) {
                System.out.println("Rotating forward/clockwise 45 degrees (or there about)");
                stick.rotate(45f);
                SleepUtil.sleepSeconds(1);
                System.out.println("Rotating backward/counter-clockwise 45 degrees (approximately)");
                stick.rotate(-45f);
                SleepUtil.sleepSeconds(1);
                stick.release();
            }
        }
    }
}
