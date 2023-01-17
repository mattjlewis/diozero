package com.diozero.devices.sandpit;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     SilentStepStick.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2022 diozero
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

import org.tinylog.Logger;

import com.diozero.api.DeviceInterface;
import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.PwmOutputDevice;
import com.diozero.api.RuntimeIOException;
import com.diozero.util.SleepUtil;

/**
 * Stepper represents a stepper motor driven by a stepper motor controller.
 * Originally developed for a Watterott SilentStepStick, it should be compatible
 * with the Wetterott StepStick and the Pololu A4988 drivers. It might be
 * compatible with other drivers that use the following control signals: enable
 * (low), direction, and step.
 * <p>
 * The implementation supports up to 16 micro-steps/step. Configuration of
 * micro-stepping must be done external to the implementation.
 * </p>
 * <p>
 * The implementation assumes a stepper motor with 200 steps/rotation.
 * </p>
 * <p>
 * The enable pin turns the motor driver on and off. When disabled, the driver
 * does not power the motor and the shaft can be moved manually. When enabled,
 * the powers the motor and and it cannot be moved manually.
 * </p>
 *
 * @author Greg Flurry
 */
public class SilentStepStick implements DeviceInterface {
	public enum Direction {
		CW, CCW;
	}

	public enum Resolution {
		FULL(1), HALF(2), QUARTER(4), EIGTH(8), SIXTEENTH(16);

		private final int resolution;

		Resolution(int resolution) {
			this.resolution = resolution;
		}

		public int getResolution() {
			return resolution;
		}
	}

	private final int stepPin;
	private final Resolution resolution;

	private final DigitalOutputDevice directionOutputDevice;
	private final DigitalOutputDevice enableOutputDevice;
	private PwmOutputDevice stepPwmOutputDevice;
	private int frequency;
	private boolean enabled = false;

	private static final float FREQUENCY_FACTOR = 200f / 60f;

	/**
	 * Constructs a new stepper motor instance.The direction defaults to
	 * clockwise.The speed defaults to 1 RPM.
	 *
	 * @param enablePin    GPIO pin used to enable the driver.
	 * @param directionPin GPIO pin used to set direction of rotation (looking
	 *                     shaft)
	 * @param stepPin      GPIO pin used to step the motor
	 * @param resolution   resolution set by external configuration
	 * @param speed        speed of rotation in RPM
	 * @throws RuntimeIOException if initialisation error
	 */
	public SilentStepStick(int enablePin, int directionPin, int stepPin, Resolution resolution, float speed)
			throws RuntimeIOException {
		this.stepPin = stepPin;
		this.resolution = resolution;

		// Set up GPIOs
		enableOutputDevice = new DigitalOutputDevice(enablePin, false, false);
		directionOutputDevice = new DigitalOutputDevice(directionPin, true, false);

		// Set speed to default
		setSpeed(speed);
	}

	/**
	 * Closes the device.
	 */
	@Override
	public void close() {
		directionOutputDevice.close();
		enableOutputDevice.close();
		if (stepPwmOutputDevice != null) {
			stepPwmOutputDevice.close();
		}
	}

	/**
	 * Sets the frequency of the PWM generator to attain the requested rotational
	 * speed.
	 *
	 * @param speedRPM in RPM
	 * @throws InterruptedException
	 * @throws RuntimeIOException
	 */
	private void setSpeed(float speedRPM) throws RuntimeIOException {
		float multiplier = resolution.getResolution();

		frequency = (int) (speedRPM * FREQUENCY_FACTOR * multiplier);
		Logger.debug("frequency = " + frequency + " multiplier = " + multiplier + " freq factor = " + FREQUENCY_FACTOR);

		// Off
		stepPwmOutputDevice = new PwmOutputDevice(stepPin, frequency, 0);
	}

	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * Enables or disables the stepper driver.
	 * <p>
	 * When disabled, the driver does not power the motor, and thus there is no
	 * torque applied. It can be turned manually. When enabled, the driver powers
	 * the motor, and thus there is torque. It cannot be turned manually.
	 * </p>
	 *
	 * @param enabled true to enable, false to disable
	 * @throws RuntimeIOException for IO errors
	 */
	public void setEnabled(boolean enabled) throws RuntimeIOException {
		if (enabled) {
			enableOutputDevice.on();
		} else {
			enableOutputDevice.off();
		}
		this.enabled = enabled;
	}

	public Direction getDirection() {
		return directionOutputDevice.isOn() ? Direction.CCW : Direction.CW;
	}

	/**
	 * Sets the direction of rotation, either clockwise or counterclockwise.
	 *
	 * @param direction the new direction
	 * @throws RuntimeIOException for IO errors
	 */
	public void setDirection(Direction direction) throws RuntimeIOException {
		if (direction == Direction.CW) {
			directionOutputDevice.off();
		} else {
			directionOutputDevice.on();
		}
	}

	/**
	 * Turns on/off the stepper motor. Technically it turns on/off the step signal
	 * to the driver.
	 *
	 * @param on true to turn on, false to turn off
	 * @throws RuntimeIOException if an I/O error occurs when changing the PWM
	 *                            output device value
	 */
	public void setOn(boolean on) throws RuntimeIOException {
		float pwm = 0f;
		if (on) {
			// Let the motor rest (see p.9 of datasheet)
			SleepUtil.sleepMillis(100);
			pwm = 0.5f;
		}
		stepPwmOutputDevice.setValue(pwm);
	}
}
