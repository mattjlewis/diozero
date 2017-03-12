package com.diozero.sandpit;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 mattjlewis
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

import java.util.LinkedList;

import org.pmw.tinylog.Logger;

import com.diozero.api.Animation;
import com.diozero.api.GpioDevice;
import com.diozero.api.OutputDeviceInterface;
import com.diozero.internal.DeviceFactoryHelper;
import com.diozero.internal.spi.PwmOutputDeviceFactoryInterface;
import com.diozero.internal.spi.PwmOutputDeviceInterface;
import com.diozero.util.RangeUtil;
import com.diozero.util.RuntimeIOException;

public class Servo extends GpioDevice implements OutputDeviceInterface {
	private static final int DEFAULT_PWM_FREQUENCY = 50;
	private static final float DEFAULT_MIN_PULSE_WIDTH_MS = 0.75f;
	private static final float DEFAULT_MAX_PULSE_WIDTH_MS = 2.25f;
	private static final float DEFAULT_MINUS90_PULSE_WIDTH_MS = 1f;
	private static final float DEFAULT_PLUS90_PULSE_WIDTH_MS = 2f;

	private int pwmFrequency;
	private PwmOutputDeviceInterface device;
	private float minPulseWidthMs;
	private float maxPulseWidthMs;
	private float midPulseWidthMs;
	private float minAngle;
	private float maxAngle;
	private LinkedList<Animation> movementQueue;

	public Servo(int gpio, float initialPulseWidthMs) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), gpio, DEFAULT_PWM_FREQUENCY, DEFAULT_MIN_PULSE_WIDTH_MS,
				DEFAULT_MAX_PULSE_WIDTH_MS, DEFAULT_MINUS90_PULSE_WIDTH_MS, DEFAULT_PLUS90_PULSE_WIDTH_MS,
				initialPulseWidthMs);
	}

	public Servo(int gpio, float minPulseWidthMs, float maxPulseWidthMs, float minus90PulseWidthMs,
			float plus90PulseWidthMs, float initialPulseWidthMs) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), gpio, DEFAULT_PWM_FREQUENCY, minPulseWidthMs,
				maxPulseWidthMs, minus90PulseWidthMs, plus90PulseWidthMs, initialPulseWidthMs);
	}

	public Servo(int gpio, int pwmFrequency, float initialPulseWidthMs) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), gpio, pwmFrequency, DEFAULT_MIN_PULSE_WIDTH_MS,
				DEFAULT_MAX_PULSE_WIDTH_MS, DEFAULT_MINUS90_PULSE_WIDTH_MS, DEFAULT_PLUS90_PULSE_WIDTH_MS,
				initialPulseWidthMs);
	}

	public Servo(int gpio, int pwmFrequency, float minPulseWidthMs, float maxPulseWidthMs, float minus90PulseWidthMs,
			float plus90PulseWidthMs, float initialPulseWidthMs) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), gpio, pwmFrequency, minPulseWidthMs, maxPulseWidthMs,
				minus90PulseWidthMs, plus90PulseWidthMs, initialPulseWidthMs);
	}

	public Servo(PwmOutputDeviceFactoryInterface pwmDeviceFactory, int gpio, int pwmFrequency,
			float initialPulseWidthMs) throws RuntimeIOException {
		this(pwmDeviceFactory, gpio, pwmFrequency, DEFAULT_MIN_PULSE_WIDTH_MS, DEFAULT_MAX_PULSE_WIDTH_MS,
				DEFAULT_MINUS90_PULSE_WIDTH_MS, DEFAULT_PLUS90_PULSE_WIDTH_MS, initialPulseWidthMs);
	}

	public Servo(PwmOutputDeviceFactoryInterface pwmDeviceFactory, int gpio, int pwmFrequency, float minPulseWidthMs,
			float maxPulseWidthMs, float minus90PulseWidthMs, float plus90PulseWidthMs, float initialPulseWidthMs)
			throws RuntimeIOException {
		super(gpio);

		movementQueue = new LinkedList<>();
		this.pwmFrequency = pwmFrequency;
		this.minPulseWidthMs = minPulseWidthMs;
		this.maxPulseWidthMs = maxPulseWidthMs;
		this.midPulseWidthMs = (minus90PulseWidthMs + plus90PulseWidthMs) / 2;
		minAngle = RangeUtil.map(minPulseWidthMs, minus90PulseWidthMs, plus90PulseWidthMs, 0, 180, false);
		maxAngle = RangeUtil.map(maxPulseWidthMs, minus90PulseWidthMs, plus90PulseWidthMs, 0, 180, false);

		this.device = pwmDeviceFactory.provisionPwmOutputDevice(gpio, pwmFrequency, calcValue(initialPulseWidthMs));
	}

	@Override
	public void close() {
		Logger.debug("close()");
		device.close();
		Logger.debug("device closed");
	}

	public int getPwmFrequency() {
		return pwmFrequency;
	}

	public float getValue() {
		return getPulseWidthMs();
	}

	@Override
	public void setValue(float value) {
		setPulseWidthMs(value);
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
	 * @param pulseWidthMs
	 *            Servo pulse width (milliseconds)
	 */
	public void setPulseWidthMs(float pulseWidthMs) {
		if (pulseWidthMs < minPulseWidthMs || pulseWidthMs > maxPulseWidthMs) {
			throw new IllegalArgumentException(
					"Invalid pulse width (" + pulseWidthMs + "), must be " + minPulseWidthMs + ".." + maxPulseWidthMs);
		}
		device.setValue(calcValue(pulseWidthMs));
	}

	public void setAngle(float angle) {
		setPulseWidthMs(RangeUtil.map(angle, minAngle, maxAngle, minPulseWidthMs, maxPulseWidthMs));
	}

	public void min() {
		setPulseWidthMs(minPulseWidthMs);
	}

	public void centre() {
		setPulseWidthMs(midPulseWidthMs);
	}

	public void max() {
		setPulseWidthMs(maxPulseWidthMs);
	}

	private float calcValue(float pulseWidthMs) {
		return pulseWidthMs * pwmFrequency / 1000f;
	}
}
