package com.diozero.sandpit;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 - 2017 mattjlewis
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

import org.pmw.tinylog.Logger;

import com.diozero.api.GpioDevice;
import com.diozero.api.OutputDeviceInterface;
import com.diozero.internal.DeviceFactoryHelper;
import com.diozero.internal.spi.PwmOutputDeviceFactoryInterface;
import com.diozero.internal.spi.PwmOutputDeviceInterface;
import com.diozero.util.RangeUtil;
import com.diozero.util.RuntimeIOException;

public class Servo extends GpioDevice implements OutputDeviceInterface {
	private static final int DEFAULT_PWM_FREQUENCY = 50;

	private int pwmFrequency;
	private PwmOutputDeviceInterface device;
	private Trim trim;

	public Servo(int gpio, float initialPulseWidthMs) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), gpio, initialPulseWidthMs, DEFAULT_PWM_FREQUENCY,
				Trim.DEFAULT);
	}

	public Servo(int gpio, float initialPulseWidthMs, Trim trim) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), gpio, initialPulseWidthMs, DEFAULT_PWM_FREQUENCY, Trim.DEFAULT);
	}

	public Servo(int gpio, float initialPulseWidthMs, int pwmFrequency) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), gpio, initialPulseWidthMs, pwmFrequency, Trim.DEFAULT);
	}

	public Servo(int gpio, float initialPulseWidthMs, int pwmFrequency, Trim trim) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), gpio, initialPulseWidthMs, pwmFrequency, trim);
	}

	public Servo(PwmOutputDeviceFactoryInterface pwmDeviceFactory, int gpio, float initialPulseWidthMs,
			int pwmFrequency) throws RuntimeIOException {
		this(pwmDeviceFactory, gpio, initialPulseWidthMs, pwmFrequency, Trim.DEFAULT);
	}

	public Servo(PwmOutputDeviceFactoryInterface pwmDeviceFactory, int gpio, float initialPulseWidthMs,
			int pwmFrequency, Trim trim) throws RuntimeIOException {
		super(gpio);

		this.pwmFrequency = pwmFrequency;
		this.trim = trim;

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
		float value = calcValue(pulseWidthMs);
		Logger.debug("pulseWidthMs: {}, value: {}", Float.valueOf(pulseWidthMs), Float.valueOf(value));
		device.setValue(value);
	}

	public void setAngle(float angle) {
		Logger.debug("angle: {}", Float.valueOf(angle));
		setPulseWidthMs(RangeUtil.map(angle, trim.getMinAngle(), trim.getMaxAngle(), trim.getMinPulseWidthMs(),
				trim.getMaxPulseWidthMs()));
	}

	public void min() {
		setPulseWidthMs(trim.getMinPulseWidthMs());
	}

	public void centre() {
		setPulseWidthMs(trim.getMidPulseWidthMs());
	}

	public void max() {
		setPulseWidthMs(trim.getMaxPulseWidthMs());
	}

	private float calcValue(float pulseWidthMs) {
		return RangeUtil.constrain(pulseWidthMs, trim.getMinPulseWidthMs(), trim.getMaxPulseWidthMs()) * pwmFrequency / 1000f;
	}
	
	public static class Trim {
		public static final Trim DEFAULT = new Trim(0.6f, 2.4f, 0.6f, 2.4f);
		public static final Trim TOWERPRO_SG90 = new Trim(0.6f, 2.4f, 0.5f, 2.5f);
		public static final Trim TOWERPRO_SG5010 = new Trim(0.6f, 2.4f, 0.5f, 2.5f);

		private float minPulseWidthMs;
		private float maxPulseWidthMs;
		private float midPulseWidthMs;
		private float minAngle;
		private float maxAngle;
		
		public Trim(float minus90PulseWidthMs, float plus90PulseWidthMs, float minPulseWidthMs, float maxPulseWidthMs) {
			this.minPulseWidthMs = minPulseWidthMs;
			this.maxPulseWidthMs = maxPulseWidthMs;
			this.midPulseWidthMs = (minus90PulseWidthMs + plus90PulseWidthMs) / 2;
			minAngle = RangeUtil.map(minPulseWidthMs, minus90PulseWidthMs, plus90PulseWidthMs, 0, 180, false);
			maxAngle = RangeUtil.map(maxPulseWidthMs, minus90PulseWidthMs, plus90PulseWidthMs, 0, 180, false);
		}
		
		public float getMinPulseWidthMs() {
			return minPulseWidthMs;
		}

		public float getMaxPulseWidthMs() {
			return maxPulseWidthMs;
		}

		public float getMidPulseWidthMs() {
			return midPulseWidthMs;
		}

		public float getMinAngle() {
			return minAngle;
		}

		public float getMaxAngle() {
			return maxAngle;
		}

		@SuppressWarnings("static-method")
		public float getMidAngle() {
			return 90;
		}
	}
}
