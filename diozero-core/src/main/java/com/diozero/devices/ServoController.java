package com.diozero.devices;

import java.util.Objects;

import com.diozero.api.ServoDevice;
import com.diozero.api.ServoTrim;

/**
 * A "wrapper" around the PCA9685 PWM controller: the main purpose is to prevent re-use of a previously allocated PWM
 * channel.
 */
public class ServoController {
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
     * @return the default ServoController
     */
    public static synchronized ServoController getInstance() {
        if (instance == null) {
            instance = new ServoController();
        }
        return instance;
    }

    /**
     * Get a ServoDevice for the specified channel. If the channel has not been used before, a new ServoDevice will be
     * created. If the channel has been used before, the existing ServoDevice will be returned. This will use the default
     * trim, if necessary.
     *
     * @param servoNumber 0-15
     * @return the appropriate ServoDevice
     * @see #getServo(int, ServoTrim)
     */
    public ServoDevice getServo(int servoNumber) {
        return getServo(servoNumber, ServoTrim.DEFAULT);
    }

    /**
     * Get a ServoDevice for the specified servo. If the servo has not been used before, a new ServoDevice will be
     * created. If it has been used before, the existing ServoDevice will be returned.
     * <p>
     * NOTE: trim will only be applied on device creation.
     * </p>
     *
     * @param servoNumber 0-15
     * @param trim        Servo trim
     * @return the appropriate ServoDevice
     */
    public synchronized ServoDevice getServo(int servoNumber, ServoTrim trim) {
        Objects.checkIndex(servoNumber, 16);

        if (servos[servoNumber] == null) {
            servos[servoNumber] = new ServoDevice.Builder(servoNumber)
                    .setDeviceFactory(pwmController)
                    .setTrim(trim)
                    .setFrequency(pwmController.getBoardPwmFrequency())
                    .build();
        }
        return servos[servoNumber];
    }
}
