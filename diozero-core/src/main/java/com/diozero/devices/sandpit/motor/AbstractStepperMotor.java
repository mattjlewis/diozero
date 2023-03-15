package com.diozero.devices.sandpit.motor;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     AbstractStepperMotor.java
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

import com.diozero.api.RuntimeIOException;

/**
 * Basic management and physical characteristics for a stepper.
 *
 * @author E. A. Graham Jr.
 */
public abstract class AbstractStepperMotor implements StepperMotorInterface {
    protected final int stepsPerRotation;
    protected final float strideAngle;
    protected final StepperMotorController controller;

    /**
     * @param stepsPerRotation how many steps required to complete a full rotation
     * @param controller       the controller
     */
    public AbstractStepperMotor(int stepsPerRotation, StepperMotorController controller) {
        this.stepsPerRotation = stepsPerRotation;
        strideAngle = 360f / stepsPerRotation;
        this.controller = controller;
    }

    /**
     * @param strideAngle how many degrees a "step" will rotate (angle = 360 / stepsPerRotation)
     * @param controller  the controller
     */
    public AbstractStepperMotor(float strideAngle, StepperMotorController controller) {
        this.strideAngle = strideAngle;
        stepsPerRotation = (int)Math.floor(360f / strideAngle);
        this.controller = controller;
    }

    @Override
    public StepperMotorController getController() {
        return controller;
    }

    @Override
    public float getStrideAngle() {
        return strideAngle;
    }

    @Override
    public long getStepsPerRotation() {
        return stepsPerRotation;
    }

    @Override
    public void close() throws RuntimeIOException {
        controller.close();
    }
}
