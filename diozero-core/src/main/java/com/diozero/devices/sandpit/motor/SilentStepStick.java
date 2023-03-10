package com.diozero.devices.sandpit.motor;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     SilentStepStick.java
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
import com.diozero.devices.sandpit.motor.ChopperStepperController.FrequencyMultiplierChopperController;

/**
 * Represents a class of bipolar stepper motor drivers using 3 inputs: enable, direction, and "speed" (PWM frequency).
 * <p>
 * Originally developed for a <a href="https://learn.watterott.com/silentstepstick/">Watterott SilentStepStick</a>,
 * this should be compatible with the Pololu A4988 drivers and other compatible ADI Tinamic drivers.
 * <p>
 * The micro-step capabilities of the driver chip are configured externally to this driver. This does <b>NOT</b> use
 * the UART interface.
 * <p>
 * The "enable" pin turns the motor driver on and off. In most cases, if the driver is <b>disabled</b>, power
 * is not supplied to the motor and the shaft can be moved manually.
 *
 * @author Greg Flurry, E. A. Graham Jr.
 */
public class SilentStepStick extends AbstractChopperStepperMotor {
    public static final int DEFAULT_STEPS = 200;

    /**
     * Constructs a new stepper motor instance with default 200 steps per rotation.
     *
     * @param driver the driver
     */
    public SilentStepStick(ChopperStepperController.FrequencyMultiplierChopperController driver) {
        this(driver, DEFAULT_STEPS);
    }

    /**
     * Constructs a new stepper motor instance.
     *
     * @param driver             the driver
     * @param stepsPerRevolution steps (not counting micro-stepping)
     */
    public SilentStepStick(FrequencyMultiplierChopperController driver, int stepsPerRevolution) {
        super(stepsPerRevolution, driver);
    }

    /**
     * Whether the driver is currently enabled or not.
     *
     * @return {@code true} if enabled
     */
    public boolean isEnabled() {
        return myDriver().isEnabled();
    }

    /**
     * Enables or disables the stepper driver.
     * <p>
     * When disabled, typically the driver does not power the motor, and thus there is no torque applied. It can be
     * turned manually.
     *
     * @param enabled {@code true} to enable
     */
    public void setEnabled(boolean enabled) {
        myDriver().setEnabled(enabled);
    }

    @Override
    protected void run(Direction direction, float speed) {
        ChopperStepperController.FrequencyMultiplierChopperController driver = myDriver();

        if (!driver.isEnabled()) throw new RuntimeIOException("Driver is not enabled");
        driver.setDirection(direction);

        var frequencyFactor = stepsPerRotation / 60f;
        var multiplier = driver.getResolution().multiplier();
        var frequency = Math.round(speed * frequencyFactor * multiplier);

        driver.setFrequency(frequency);
        driver.run();
    }

    private FrequencyMultiplierChopperController myDriver() {
        return ((ChopperStepperController.FrequencyMultiplierChopperController)getController());
    }
}
