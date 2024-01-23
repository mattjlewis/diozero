package com.diozero.sampleapps.sandpit.motor;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     BasicStepperTest.java
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
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.diozero.api.DigitalOutputDevice;
import com.diozero.devices.sandpit.motor.BasicStepperController;
import com.diozero.devices.sandpit.motor.BasicStepperController.GpioStepperPin;
import com.diozero.devices.sandpit.motor.BasicStepperController.UnipolarBasicController;
import com.diozero.devices.sandpit.motor.BasicStepperMotor;
import com.diozero.devices.sandpit.motor.StepperMotorInterface.Direction;
import com.diozero.util.SleepUtil;

/**
 * Sweep a "basic" stepper back and forth using four GPIO DigitalOutput. The pins default to 25,5,6,16 <b>in that
 * order</b>. To use a different set, run with a single argument with the pins as comma-separated values.
 * <p>
 * For the common-garden variety of hobby steppers, the 28BYJ-48 with the gear box, 1024 steps will complete
 * approximately 1/2 rotation.
 *
 * @author E. A. Graham Jr.
 */
public class BasicStepperTest {
    public static void main(String[] args) {
        List<Integer> pins = List.of(25,5,6,16);

        if (args.length == 1 && Pattern.matches("\\d+,\\d+,\\d+,\\d+", args[0])) {
            pins = Arrays.stream(args[0].split(",")).map(Integer::parseInt).collect(Collectors.toList());
        }

        GpioStepperPin[] stepperPins = pins.stream().map(DigitalOutputDevice::new).map(GpioStepperPin::new)
                .collect(Collectors.toList()).toArray(new GpioStepperPin[0]);

        try(BasicStepperController controller = new UnipolarBasicController(stepperPins)) {
            try(BasicStepperMotor motor = new BasicStepperMotor(controller)) {
                System.out.println("Rotating forward/clockwise 1024 steps");
                for (int i = 0; i < 1024; i++) {
                    motor.step(Direction.FORWARD);
                    SleepUtil.busySleep(Duration.ofMillis(5).toNanos());
                }
                System.out.println("Rotating backward/counter-clockwise 1024 steps");
                for (int i = 0; i < 1024; i++) {
                    motor.step(Direction.CCW);
                    SleepUtil.busySleep(Duration.ofMillis(5).toNanos());
                }
            }
        }
    }
}
