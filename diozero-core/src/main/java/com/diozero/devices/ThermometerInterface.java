package com.diozero.devices;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     ThermometerInterface.java
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

import com.diozero.api.RuntimeIOException;

public interface ThermometerInterface extends SensorInterface {
	/**
	 * Get temperature in degrees Celsius
	 * 
	 * @return Temperature (deg C)
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	float getTemperature() throws RuntimeIOException;

	/**
	 * Get temperature in degrees Celsius
	 * 
	 * @return Temperature (deg C)
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	default float getTemperatureCelsius() throws RuntimeIOException {
		return getTemperature();
	}

	/**
	 * Get temperature in degrees Fahrenheit
	 * 
	 * @return Temperature (deg F)
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	default float getTemperatureFahrenheit() throws RuntimeIOException {
		return celsiusToFahrenheit(getTemperature());
	}

	static final float CONVERSION_OFFSET = 32;
	static final float CONVERSION_RATIO = 9 / 5f;

	/**
	 * Convert from degrees Celsius to degrees Fahrenheit
	 *
	 * @param celsius temperature in degrees Celsius
	 * @return the temperature in degrees Fahrenheit
	 */
	static float celsiusToFahrenheit(float celsius) {
		return celsius * CONVERSION_RATIO + CONVERSION_OFFSET;
	}

	/**
	 * Convert from degrees Fahrenheit to degrees Celsius
	 *
	 * @param fahrenheit temperature in degrees Fahrenheit
	 * @return the temperature in degrees Celsius
	 */
	public static float fahrenheitToCelsius(float fahrenheit) {
		return (fahrenheit - CONVERSION_OFFSET) / CONVERSION_RATIO;
	}
}
