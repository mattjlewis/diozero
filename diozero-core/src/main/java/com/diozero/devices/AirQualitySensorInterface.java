package com.diozero.devices;
/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     AirQualitySensorInterface.java
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

/**
 * An air-quality sensor uses relative humidity and a "resistance" value to measure air quaility.
 */
public interface AirQualitySensorInterface extends HygrometerInterface {
    /**
     * "Standard" humidity baseline typically used for indoor air-quality.
     */
    float STANDARD_INDOOR_HUMIDITY = 40f;

    float getGasResistance();

    /**
     * Calculates the indoor air quality as a percentage based on a baseline reading. Based off of Pimoroni's
     * <a href="https://github.com/pimoroni/bme680-python/blob/main/examples/indoor-air-quality.py">indoor-air-quality.py</a>
     * <p>
     * Under non-calibrated, general usage, it is recommended that the sensor "warm up" in the current mode for at least
     * 30 minutes before taking readings. After that time, any kind of statistical baseline (e.g. average) for the gas
     * and humidity readings can be used to get an indication of general air-quality.
     * </p>
     *
     * @param gasReading        current reading
     * @param gasBaseline       the "baseline" to score off of
     * @param humidityReading   current reading
     * @param humidityBaseline  the "baseline" to score off of
     * @param humidityWeighting weighting applied to scoring (a good default is 0.25f)
     */
    static float airQuality(float gasReading, float gasBaseline, float humidityReading, float humidityBaseline,
                                   float humidityWeighting) {
        float gasOffset = gasBaseline - gasReading;
        float humidityOffset = humidityReading - humidityBaseline;

        float humidityScore;
        if (humidityOffset > 0) {
            humidityScore = (100 - humidityBaseline - humidityOffset) / (100 - humidityBaseline) * (humidityWeighting * 100);
        }
        else {
            humidityScore = (humidityBaseline + humidityOffset) / humidityBaseline * (humidityWeighting * 100);
        }

        float gasScore;
        if (gasOffset > 0) {
            gasScore = (gasReading / gasBaseline) * (100 - (humidityWeighting * 100));
        }
        else {
            gasScore = 100 - (humidityWeighting * 100);
        }

        // Calculate air_quality_score.
        return humidityScore + gasScore;
    }

}
