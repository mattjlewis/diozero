package com.diozero.devices;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     BarometerInterface.java
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

import com.diozero.api.RuntimeIOException;

/*-
 * Useful environment calculations: https://github.com/finitespace/BME280/blob/master/src/EnvironmentCalculations.cpp
 * - Altitude (Hypsometric equation https://en.wikipedia.org/wiki/Hypsometric_equation)
 * - Equivalent sea level pressure (inverse of altitude calculation)
 * - Absolute humidity (ref https://carnotcycle.wordpress.com/2012/08/04/how-to-convert-relative-humidity-to-absolute-humidity/, https://www.eas.ualberta.ca/jdwilson/EAS372_13/Vomel_CIRES_satvpformulae.html)
 * - Heat index (https://ehp.niehs.nih.gov/1206273/, using both Rothfusz and Steadman's equations http://www.wpc.ncep.noaa.gov/html/heatindex_equation.shtml)
 * - Dew point (Brian McNoldy from http://andrew.rsmas.miami.edu)
 */
public interface BarometerInterface extends SensorInterface {
	static final float ABSOLUTE_ZERO = 273.15f;
	static final double HYPSOMETRIC_POWER = 1 / 5.257;
	static final double HYPSOMETRIC_DIVISOR = 0.0065;

	/**
	 * Get the pressure in hPa (Hectopascal).
	 * 
	 * 1 Kilopascal (kPa) = 10 Hectopascals = 1,000 Pascals (hPa)
	 * 
	 * @return pressure in hPa
	 * @throws RuntimeIOException if an IO error occurs
	 */
	float getPressure() throws RuntimeIOException;

	/**
	 * Calculate the altitude given the current reference sea level pressure,
	 * current outdoor temperature (Celsius) and current pressure.
	 * 
	 * Uses the
	 * <a href="https://en.wikipedia.org/wiki/Hypsometric_equation">Hypsometric
	 * formula</a>
	 * 
	 * @param seaLevelPressure   Reference sea level pressure (kPA)
	 * @param outdoorTempCelsius Current outdoor temperature (degrees Celsius)
	 * @return Altitude in meters
	 */
	default float calculateAltitude(float seaLevelPressure, float outdoorTempCelsius) throws RuntimeIOException {
		return (float) ((Math.pow(seaLevelPressure / getPressure(), HYPSOMETRIC_POWER) - 1)
				* ((outdoorTempCelsius + ABSOLUTE_ZERO) / HYPSOMETRIC_DIVISOR));
	}
}
