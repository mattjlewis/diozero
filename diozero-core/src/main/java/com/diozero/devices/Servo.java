package com.diozero.devices;

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


import java.io.Closeable;
import java.util.Arrays;
import java.util.Collection;

import org.pmw.tinylog.Logger;

import com.diozero.api.GpioDevice;
import com.diozero.api.OutputDeviceCollection;
import com.diozero.api.OutputDeviceInterface;
import com.diozero.internal.provider.PwmOutputDeviceFactoryInterface;
import com.diozero.internal.provider.PwmOutputDeviceInterface;
import com.diozero.util.DeviceFactoryHelper;
import com.diozero.util.RangeUtil;
import com.diozero.util.RuntimeIOException;

public class Servo extends GpioDevice implements OutputDeviceInterface {
	private static final int DEFAULT_PWM_FREQUENCY = 50;

	private int pwmFrequency;
	private PwmOutputDeviceInterface device;
	private Trim trim;
	private boolean inverted;
	private OutputDeviceUnit outputDeviceUnit;

	public Servo(int gpio, float initialPulseWidthMs) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), gpio, initialPulseWidthMs, DEFAULT_PWM_FREQUENCY,
				Trim.DEFAULT);
	}

	public Servo(int gpio, float initialPulseWidthMs, Trim trim) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), gpio, initialPulseWidthMs, DEFAULT_PWM_FREQUENCY, trim);
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
		// Default the set / get value unit to pulse width ms
		outputDeviceUnit = OutputDeviceUnit.PULSE_WIDTH_MS;

		this.device = pwmDeviceFactory.provisionPwmOutputDevice(gpio, pwmFrequency,
				mapPulseWidthMsToPercentage(initialPulseWidthMs));
	}
	
	public OutputDeviceUnit getOutputDeviceUnit() {
		return outputDeviceUnit;
	}
	
	public Servo setOutputDeviceUnit(OutputDeviceUnit outputDeviceUnit) {
		this.outputDeviceUnit = outputDeviceUnit;
		
		return this;
	}
	
	public Servo setInverted(boolean inverted) {
		this.inverted = inverted;
		
		return this;
	}

	@Override
	public void close() {
		device.close();
		Logger.debug("device closed");
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

	@Override
	public void setValue(float value) {
		switch (outputDeviceUnit) {
		case PULSE_WIDTH_MS:
			setPulseWidthMs(value);
			break;
		case DEGREES:
			setAngle(value);
			break;
		case RADIANS:
			setAngle((float) Math.toDegrees(value));
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
	 * @param pulseWidthMs
	 *            Servo pulse width (milliseconds)
	 */
	public void setPulseWidthMs(float pulseWidthMs) {
		if (inverted) {
			pulseWidthMs = trim.getMidPulseWidthMs() - pulseWidthMs + trim.getMidPulseWidthMs();
		}
		device.setValue(mapPulseWidthMsToPercentage(pulseWidthMs));
	}
	
	/**
	 * Get the current servo angle where 90 degrees is the middle position
	 * @return Servo angle (90 degrees is middle)
	 */
	public float getAngle() {
		return trim.convertPulseWidthMsToAngle(getPulseWidthMs());
	}

	/**
	 * Turn the servo to the specified angle where 90 is the middle position
	 * @param angle Servo angle (90 degrees is middle)
	 */
	public void setAngle(float angle) {
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
		return RangeUtil.constrain(pulseWidthMs, trim.getMinPulseWidthMs(), trim.getMaxPulseWidthMs()) * pwmFrequency / 1000f;
	}
	
	public static enum OutputDeviceUnit {
		PULSE_WIDTH_MS, DEGREES, RADIANS;
	}
	
	public static class Array extends OutputDeviceCollection implements Closeable {
		private Collection<Servo> servos;
		
		public Array(Servo... servos) {
			super(servos);
			
			this.servos = Arrays.asList(servos);
		}
		
		@Override
		public void close() {
			servos.forEach(Servo::close);
		}
	}
	
	public static class Trim {
		public static final float MID_ANGLE = 90;
		
		private static final float DEFAULT_MID = 1.5f;
		private static final float DEFAULT_90_DELTA = 0.9f;
		// Default to 180 degree range, from 0.6ms to 2.4ms with 1.5ms centre
		public static final Trim DEFAULT = new Trim(DEFAULT_MID, DEFAULT_90_DELTA);
		
		// From my testing TowerPro SG90 has a range slightly greater than 180 degree
		private static final float TOWERPRO_SG90_RANGE = 2f;
		public static final Trim TOWERPRO_SG90 = new Trim(DEFAULT_MID, DEFAULT_90_DELTA, TOWERPRO_SG90_RANGE);
		
		// From my testing TowerPro SG5010 has a range slightly greater than 180 degree
		private static final float TOWERPRO_SG5010_RANGE = 2f;
		public static final Trim TOWERPRO_SG5010 = new Trim(DEFAULT_MID, DEFAULT_90_DELTA, TOWERPRO_SG5010_RANGE);
		
		private static final float MG996R_RANGE = 2f;
		public static final Trim MG996R = new Trim(DEFAULT_MID, DEFAULT_90_DELTA, MG996R_RANGE);

		private float midPulseWidthMs;
		private float ninetyDegPulseWidthMs;
		private float minPulseWidthMs;
		private float maxPulseWidthMs;
		private float minAngle;
		private float maxAngle;
		
		/**
		 * Assumes 180 degree range of movement
		 * @param midPulseWidthMs Pulse width in ms corresponding to the centre position (90 degrees)
		 * @param ninetyDegPulseWidthMs Pulse width in ms corresponding to a 90 degreee movement in either direction
		 */
		public Trim(float midPulseWidthMs, float ninetyDegPulseWidthMs) {
			this(midPulseWidthMs, ninetyDegPulseWidthMs, 2*ninetyDegPulseWidthMs);
		}
		
		public Trim(float midPulseWidthMs, float ninetyDegPulseWidthMs, float rangePulseWidthMs) {
			this(midPulseWidthMs, ninetyDegPulseWidthMs, midPulseWidthMs - rangePulseWidthMs / 2,
					midPulseWidthMs + rangePulseWidthMs / 2);
		}
		
		public Trim(float midPulseWidthMs, float ninetyDegPulseWidthMs, float minPulseWidthMs, float maxPulseWidthMs) {
			this.midPulseWidthMs = midPulseWidthMs;
			this.ninetyDegPulseWidthMs = ninetyDegPulseWidthMs;
			this.minPulseWidthMs = minPulseWidthMs;
			this.maxPulseWidthMs = maxPulseWidthMs;
			minAngle = RangeUtil.map(minPulseWidthMs, midPulseWidthMs - ninetyDegPulseWidthMs,
					midPulseWidthMs + ninetyDegPulseWidthMs, 0, 180, false);
			maxAngle = RangeUtil.map(maxPulseWidthMs, midPulseWidthMs - ninetyDegPulseWidthMs,
					midPulseWidthMs + ninetyDegPulseWidthMs, 0, 180, false);
		}

		public float getMidPulseWidthMs() {
			return midPulseWidthMs;
		}

		public float getNinetyDegPulseWidthMs() {
			return ninetyDegPulseWidthMs;
		}
		
		public float getMinPulseWidthMs() {
			return minPulseWidthMs;
		}

		public float getMaxPulseWidthMs() {
			return maxPulseWidthMs;
		}

		public float getMinAngle() {
			return minAngle;
		}

		public float getMaxAngle() {
			return maxAngle;
		}

		@SuppressWarnings("static-method")
		public float getMidAngle() {
			return MID_ANGLE;
		}
		
		public float convertPulseWidthMsToAngle(float pulseWidthMs) {
			return RangeUtil.map(pulseWidthMs, minPulseWidthMs, maxPulseWidthMs, minAngle, maxAngle);
		}
		
		public float convertAngleToPulseWidthMs(float angle) {
			return RangeUtil.map(angle, minAngle, maxAngle, minPulseWidthMs, maxPulseWidthMs);
		}
	}
}
