package com.diozero.devices.sandpit.motor;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     BasicStepperMotor.java
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

import java.util.function.Consumer;

import com.diozero.devices.sandpit.motor.BasicStepperController.StepStyle;

/**
 * A stepper using a controller that moves the rotor by sending timed on/off to the appropriate pins. This class and
 * the controller only handle outputting the appropriate pin signals, but not the timing, since it must be software
 * controlled <b>and</b> depends upon the hardware involved.
 * <p>
 * For example, the common 5-wire 28BYJ-48 is rated at (about) 512 steps per rotation, which requires <b>2048</b>
 * "steps" on this class.
 * <p>
 * This class and the accompanying {@link BasicStepperController} are roughly equivalent to the Adafruit
 * <a href="https://github.com/adafruit/Adafruit_CircuitPython_Motor/blob/main/adafruit_motor/stepper.py">CircuitPython Stepper</a>
 *
 * @author E. A. Graham Jr.
 */
public class BasicStepperMotor extends AbstractStepperMotor {

    private Consumer<StepStyle> stepForward;
    private Consumer<StepStyle> stepBackward;

    /**
     * @param strideAngle how many degrees a "step" will rotate (angle = 360 / stepsPerRotation)
     * @param controller  the controller
     */
    public BasicStepperMotor(float strideAngle, BasicStepperController controller) {
        super(strideAngle, controller);
    }

    /**
     * @param stepsPerRotation how many steps required to complete a full rotation
     * @param controller       the controller
     */
    public BasicStepperMotor(int stepsPerRotation, BasicStepperController controller) {
        super(stepsPerRotation, controller);
    }

    /**
     * "Generic" constructor: this sets the steps per rotation to 512 as a "reasonable" default. This roughly
     * corresponds to the common hobby stepper.
     *
     * @param controller the controller
     * @see #step(Direction)
     */
    public BasicStepperMotor(BasicStepperController controller) {
        this(512, controller);
    }

    private Consumer<StepStyle> setupController(Direction direction) {
        BasicStepperController myController = (BasicStepperController)controller;
        if (direction.getDirection() < 0) {
            return myController::stepBackward;
        }
        return myController::stepForward;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Note that it takes <b>four</b> calls to this method to move the spindle one "cycle".
     */
    @Override
    public void step(Direction direction) {
        setupController(direction).accept(StepStyle.SINGLE);
    }

    /**
     * Move the rotor one "step" in the indicated direction and style. Note that the different styles will affect the
     * "steps per rotation".
     *
     * @param direction which way to rotate
     * @param style     how to apply the step
     */
    public void step(Direction direction, StepStyle style) {
        setupController(direction).accept(style);
    }

    /**
     * @param style the step style used
     * @return how many "steps" it takes to <b>actually</b> complete a single 360 degree rotation
     */
    public int getStepsForStyle(StepStyle style) {
        if (style == StepStyle.INTERLEAVE) return 8 * stepsPerRotation;
        return 4 * stepsPerRotation;
    }
}
