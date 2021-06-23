package com.diozero.devices;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     PwmServo.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2021 diozero
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

import com.diozero.api.GpioDevice;
import com.diozero.api.PinInfo;
import com.diozero.api.RuntimeIOException;
import com.diozero.api.ServoTrim;
import com.diozero.internal.spi.InternalPwmOutputDeviceInterface;
import com.diozero.internal.spi.PwmOutputDeviceFactoryInterface;
import com.diozero.sbc.DeviceFactoryHelper;
import com.diozero.util.RangeUtil;

/**
 * @deprecated Use {@link com.diozero.api.ServoDevice} instead.
 */
@Deprecated
public class PwmServo extends GpioDevice {
	private static final int DEFAULT_PWM_FREQUENCY = 50;

	private int pwmFrequency;
	private InternalPwmOutputDeviceInterface device;
	private ServoTrim trim;
	private boolean inverted;
	private OutputDeviceUnit outputDeviceUnit;

	public PwmServo(int gpio, float initialPulseWidthMs) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), gpio, initialPulseWidthMs, DEFAULT_PWM_FREQUENCY,
				ServoTrim.DEFAULT);
	}

	public PwmServo(int gpio, float initialPulseWidthMs, ServoTrim trim) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), gpio, initialPulseWidthMs, DEFAULT_PWM_FREQUENCY, trim);
	}

	public PwmServo(int gpio, float initialPulseWidthMs, int pwmFrequency) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), gpio, initialPulseWidthMs, pwmFrequency, ServoTrim.DEFAULT);
	}

	public PwmServo(int gpio, float initialPulseWidthMs, int pwmFrequency, ServoTrim trim) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), gpio, initialPulseWidthMs, pwmFrequency, trim);
	}

	public PwmServo(PwmOutputDeviceFactoryInterface pwmDeviceFactory, int gpio, float initialPulseWidthMs,
			int pwmFrequency) throws RuntimeIOException {
		this(pwmDeviceFactory, gpio, initialPulseWidthMs, pwmFrequency, ServoTrim.DEFAULT);
	}

	public PwmServo(PwmOutputDeviceFactoryInterface pwmDeviceFactory, int gpio, float initialPulseWidthMs,
			int pwmFrequency, ServoTrim trim) throws RuntimeIOException {
		this(pwmDeviceFactory, pwmDeviceFactory.getBoardPinInfo().getByPwmOrGpioNumberOrThrow(gpio),
				initialPulseWidthMs, pwmFrequency, trim);
	}

	public PwmServo(PwmOutputDeviceFactoryInterface pwmDeviceFactory, PinInfo pinInfo, float initialPulseWidthMs,
			int pwmFrequency, ServoTrim trim) throws RuntimeIOException {
		super(pinInfo);

		this.pwmFrequency = pwmFrequency;
		this.trim = trim;
		// Default the set / get value unit to pulse width ms
		outputDeviceUnit = OutputDeviceUnit.PULSE_WIDTH_MS;

		device = pwmDeviceFactory.provisionPwmOutputDevice(pinInfo, pwmFrequency, (int) initialPulseWidthMs * 1000);
	}

	public OutputDeviceUnit getOutputDeviceUnit() {
		return outputDeviceUnit;
	}

	public PwmServo setOutputDeviceUnit(OutputDeviceUnit outputDeviceUnit) {
		this.outputDeviceUnit = outputDeviceUnit;

		return this;
	}

	public PwmServo setInverted(boolean inverted) {
		this.inverted = inverted;

		return this;
	}

	@Override
	public void close() {
		device.close();
		Logger.trace("device closed");
	}

	public int getPwmFrequency() {
		return pwmFrequency;
	}

	public float getValue() {
		float value = 0;
		switch (outputDeviceUnit) {
		case PULSE_WIDTH_MS:
			value = getPulseWidthMs();
			break;
		case DEGREES:
			value = getAngle();
			break;
		case RADIANS:
			value = (float) Math.toRadians(getAngle());
			break;
		}

		return value;
	}

	public void setValue(float value) {
		switch (outputDeviceUnit) {
		case PULSE_WIDTH_MS:
			setPulseWidthMs(value);
			break;
		case DEGREES:
			setAngle((int) value);
			break;
		case RADIANS:
			setAngle((int) Math.toDegrees(value));
			break;
		}
	}

	/**
	 * Get the current servo pulse width in milliseconds
	 *
	 * @return The servo pulse width (milliseconds)
	 */
	public float getPulseWidthMs() {
		return device.getValue() * 1000f / pwmFrequency;
	}

	/**
	 * Set the servo pulse width in milliseconds
	 *
	 * @param pulseWidthMs Servo pulse width (milliseconds)
	 */
	public void setPulseWidthMs(float pulseWidthMs) {
		if (inverted) {
			pulseWidthMs = trim.getMidPulseWidthMs() - pulseWidthMs + trim.getMidPulseWidthMs();
		}
		device.setValue(mapPulseWidthMsToPercentage(pulseWidthMs));
	}

	/**
	 * Get the current servo angle where 90 degrees is the middle position
	 *
	 * @return Servo angle (90 degrees is middle)
	 */
	public float getAngle() {
		return trim.convertPulseWidthMsToAngle(getPulseWidthMs());
	}

	/**
	 * Turn the servo to the specified angle where 90 is the middle position
	 *
	 * @param angle Servo angle (90 degrees is middle)
	 */
	public void setAngle(int angle) {
		setPulseWidthMs(trim.convertAngleToPulseWidthMs(angle));
	}

	public void min() {
		setPulseWidthMs(trim.getMinPulseWidthMs());
	}

	public void max() {
		setPulseWidthMs(trim.getMaxPulseWidthMs());
	}

	public void centre() {
		setPulseWidthMs(trim.getMidPulseWidthMs());
	}

	private float mapPulseWidthMsToPercentage(float pulseWidthMs) {
		return RangeUtil.constrain(pulseWidthMs, trim.getMinPulseWidthMs(), trim.getMaxPulseWidthMs()) * pwmFrequency
				/ 1000f;
	}

	public enum OutputDeviceUnit {
		PULSE_WIDTH_MS, DEGREES, RADIANS;
	}
}
