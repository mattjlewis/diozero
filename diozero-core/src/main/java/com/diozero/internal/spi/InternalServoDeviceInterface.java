package com.diozero.internal.spi;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     InternalServoDeviceInterface.java
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

import com.diozero.api.DeviceMode;
import com.diozero.api.RuntimeIOException;

public interface InternalServoDeviceInterface extends GpioDeviceInterface {
	/**
	 * Get the device Servo device number
	 *
	 * @return Device native Servo device number
	 */
	int getServoNum();

	/**
	 * Get the current Servo pulse width in microseconds.
	 *
	 * @return Servo pulse width value in microseconds
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	int getPulseWidthUs() throws RuntimeIOException;

	/**
	 * Set the Servo output pulse width in microseconds.
	 *
	 * @param pulseWidthUs New pulse width value in microseconds, range
	 *                     minPulseWidth to maxPulseWidth
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	void setPulseWidthUs(int pulseWidthUs) throws RuntimeIOException;

	/**
	 * Get the Servo frequency in Hz
	 *
	 * @return frequency in Hz
	 */
	int getServoFrequency();

	/**
	 * Set the Servo frequency, most servos operate at 50Hz.
	 *
	 * @param frequencyHz frequency in Hz
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	void setServoFrequency(int frequencyHz) throws RuntimeIOException;

	@Override
	default DeviceMode getMode() {
		return DeviceMode.SERVO;
	}
}
