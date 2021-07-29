package com.diozero.api;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     ServoDevice.java
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

import com.diozero.internal.spi.InternalServoDeviceInterface;
import com.diozero.internal.spi.ServoDeviceFactoryInterface;
import com.diozero.sbc.DeviceFactoryHelper;

/**
 * Provide Servo device control.
 */
public class ServoDevice extends GpioDevice {
	public static final int DEFAULT_FREQUENCY = 50;

	public static Builder newBuilder(int pwmOrGpio) {
		return new Builder(pwmOrGpio);
	}

	public static Builder newBuilder(PinInfo pinInfo) {
		return new Builder(pinInfo);
	}

	public static class Builder {
		private int pwmOrGpio;
		private PinInfo pinInfo;
		private int frequency = DEFAULT_FREQUENCY;
		private ServoTrim trim = ServoTrim.DEFAULT;
		private Integer initialPulseWidthUs;
		private boolean inverted = false;
		private ServoDeviceFactoryInterface deviceFactory;

		public Builder(int pwmOrGpio) {
			this.pwmOrGpio = pwmOrGpio;
		}

		public Builder(PinInfo pinInfo) {
			this.pinInfo = pinInfo;
		}

		public Builder setFrequency(int frequency) {
			this.frequency = frequency;
			return this;
		}

		public Builder setTrim(ServoTrim trim) {
			this.trim = trim;
			return this;
		}

		public Builder setInitialPulseWidthUs(int initialPulseWidthUs) {
			this.initialPulseWidthUs = Integer.valueOf(initialPulseWidthUs);
			return this;
		}

		public Builder setInverted(boolean inverted) {
			this.inverted = inverted;
			return this;
		}

		public Builder setDeviceFactory(ServoDeviceFactoryInterface deviceFactory) {
			this.deviceFactory = deviceFactory;
			return this;
		}

		public ServoDevice build() {
			// Default to the native device factory if not set
			if (deviceFactory == null) {
				deviceFactory = DeviceFactoryHelper.getNativeDeviceFactory();
			}

			if (pinInfo == null) {
				pinInfo = deviceFactory.getBoardPinInfo().getByPwmOrGpioNumberOrThrow(pwmOrGpio);
			}

			if (initialPulseWidthUs == null) {
				initialPulseWidthUs = Integer.valueOf(trim.getMidPulseWidthUs());
			}

			return new ServoDevice(deviceFactory, pinInfo, frequency, trim, initialPulseWidthUs.intValue(), inverted);
		}
	}

	private InternalServoDeviceInterface delegate;
	private ServoTrim trim;

	/**
	 * @param deviceFactory       Device factory to use to provision this device.
	 * @param pinInfo             GPIO to which the output device is connected.
	 * @param frequency           Servo frequency (Hz).
	 * @param trim                Servo min / mid / max pulse widths and angle
	 *                            mapping
	 * @param initialPulseWidthUs Initial servo pulse width in microseconds
	 * @param inverted            If the output is inverted, i.e. min is output as
	 *                            max and vice versa
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public ServoDevice(ServoDeviceFactoryInterface deviceFactory, PinInfo pinInfo, int frequency, ServoTrim trim,
			int initialPulseWidthUs, boolean inverted) throws RuntimeIOException {
		super(pinInfo);

		this.delegate = deviceFactory.provisionServoDevice(pinInfo, frequency, trim.getMinPulseWidthUs(),
				trim.getMaxPulseWidthUs(), initialPulseWidthUs);
		this.trim = trim;
	}

	@Override
	public void close() {
		Logger.trace("close()");
		Logger.trace("Setting value to 0");
		try {
			delegate.setPulseWidthUs(0);
		} catch (RuntimeIOException e) {
			// Ignore
		}
		delegate.close();
		Logger.trace("device closed");
	}

	// Exposed operations

	/**
	 * Get the current servo pulse width in microseconds.
	 *
	 * @return Current output pulse width value.
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public int getPulseWidthUs() throws RuntimeIOException {
		return delegate.getPulseWidthUs();
	}

	/**
	 * Set the servo pulse width value in microseconds.
	 *
	 * @param pulseWidthUs New pulse width value in microseconds.
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public void setPulseWidthUs(int pulseWidthUs) throws RuntimeIOException {
		if (pulseWidthUs < trim.getMinPulseWidthUs() || pulseWidthUs > trim.getMaxPulseWidthUs()) {
			throw new IllegalArgumentException("Invalid pulse width value (" + pulseWidthUs + " us), range "
					+ trim.getMinPulseWidthUs() + ".." + trim.getMaxPulseWidthUs());
		}
		delegate.setPulseWidthUs(pulseWidthUs);
	}

	public int getServoFrequency() {
		return delegate.getServoFrequency();
	}

	public void setServoFrequency(int frequency) throws RuntimeIOException {
		delegate.setServoFrequency(frequency);
	}

	/**
	 * Get the device on / off status.
	 *
	 * @return Returns true if the device currently has a value &gt; 0.
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public boolean isOn() throws RuntimeIOException {
		return delegate.getPulseWidthUs() > 0;
	}

	/**
	 * Get the current servo angle where 90 degrees is the middle position
	 *
	 * @return Servo angle (90 degrees is middle)
	 */
	public float getAngle() {
		return Math.round(trim.convertPulseWidthUsToAngle(getPulseWidthUs()));
	}

	/**
	 * Turn the servo to the specified angle where 90 is the middle position
	 *
	 * @param angle Servo angle (90 degrees is middle)
	 */
	public void setAngle(float angle) {
		setPulseWidthUs(trim.convertAngleToPulseWidthUs(Math.round(angle)));
	}

	/**
	 * Set the servo relative value where -1 is the minimum angle and 1 is the
	 * maximum angle.
	 *
	 * @param value relative value -1..1 that maps to min..max
	 */
	public void setValue(float value) {
		setPulseWidthUs(trim.convertValueToPulseWidthUs(value));
	}

	public void min() {
		setPulseWidthUs(trim.getMinPulseWidthUs());
	}

	public void mid() {
		setPulseWidthUs(trim.getMidPulseWidthUs());
	}

	public void max() {
		setPulseWidthUs(trim.getMaxPulseWidthUs());
	}
}
