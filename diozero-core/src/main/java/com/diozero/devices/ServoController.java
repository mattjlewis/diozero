package com.diozero.devices;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     ServoController.java
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

import com.diozero.api.ServoDevice;
import com.diozero.api.ServoTrim;

/**
 * A "wrapper" around the PCA9685 PWM controller: the main purpose is to prevent re-use of a previously allocated PWM
 * channel.
 * <p>
 * <b>NOTEL</b> The servo creation allows for setting the initial angle of the servo (aka `initialPulseWidthUs`). This
 * is <i>intended</i> to prevent the servo from "jumping" to the default position when the servo is first powered on.
 * </p>
 */
public class ServoController implements AutoCloseable {
    private final PCA9685 pwmController;
    private final ServoDevice[] servos = new ServoDevice[16];

    /**
     * Default constructor. Uses the default PCA9685 controller.
     */
    public ServoController() {
        this(new PCA9685());
    }

    /**
     * Constructor.
     *
     * @param controller the PCA9685 controller to use
     */
    public ServoController(PCA9685 controller) {
        pwmController = controller;
    }

    // implement singleton pattern
    private static ServoController instance;

    /**
     * Get the singleton <b>default</b> instance of the ServoController.
     *
     * @return the default ServoController
     */
    public static synchronized ServoController getInstance() {
        if (instance == null) {
            instance = new ServoController();
        }
        return instance;
    }

    @Override
    public void close() throws Exception {
        pwmController.close();
    }

    /**
     * Get a ServoDevice for the specified channel. If the channel has not been used before, a new ServoDevice will be
     * created. If the channel has been used before, the existing ServoDevice will be returned. This will use the default
     * trim and the initial angle <b>should</b> be 90 degrees.
     *
     * @param servoNumber 0-15
     * @return the appropriate ServoDevice
     */
    public ServoDevice getServo(int servoNumber) {
        return createOrGetServo(servoNumber, ServoTrim.DEFAULT, null);
    }

    /**
     * Get a ServoDevice for the specified servo. If the servo has not been used before, a new ServoDevice will be
     * created. If it has been used before, the existing ServoDevice will be returned. This will use the default trim.
     *
     * @param servoNumber 0-15
     * @param startAngle  the initial angle
     * @return the appropriate ServoDevice
     */
    public ServoDevice getServo(int servoNumber, int startAngle) {
        return getServo(servoNumber, ServoTrim.DEFAULT, startAngle);
    }

    /**
     * Get a ServoDevice for the specified servo. If the servo has not been used before, a new ServoDevice will be
     * created. If it has been used before, the existing ServoDevice will be returned. The initial angle <b>should</b>
     * be 90 degrees.
     *
     * @param servoNumber 0-15
     * @param trim        Servo trim
     * @return the appropriate ServoDevice
     */
    public ServoDevice getServo(int servoNumber, ServoTrim trim) {
        return createOrGetServo(servoNumber, trim, null);
    }

    /**
     * Get a ServoDevice for the specified servo. If the servo has not been used before, a new ServoDevice will be
     * created. If it has been used before, the existing ServoDevice will be returned.
     * <p>
     * NOTE: {@code trim} and {@code startAngle} will only be applied on device creation.
     * </p>
     *
     * @param servoNumber 0-15
     * @param trim        Servo trim
     * @param startAngle  the initial angle
     * @return the appropriate ServoDevice
     */
    public ServoDevice getServo(int servoNumber, ServoTrim trim, int startAngle) {
        return createOrGetServo(servoNumber, trim, trim.convertAngleToPulseWidthUs(startAngle));
    }

    private synchronized ServoDevice createOrGetServo(int servoNumber, ServoTrim trim, Integer initialPulseWidthUs) {
        Objects.checkIndex(servoNumber, 16);

        if (servos[servoNumber] == null) {
            servos[servoNumber] = new ServoDevice.Builder(servoNumber)
                    .setDeviceFactory(pwmController)
                    .setTrim(trim)
                    .setFrequency(pwmController.getBoardPwmFrequency())
                    .setInitialPulseWidthUs(initialPulseWidthUs)
                    .build();
        }
        return servos[servoNumber];
    }
}
