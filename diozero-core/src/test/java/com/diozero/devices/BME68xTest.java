package com.diozero.devices;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     BME68xTest.java
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.diozero.devices.BME68x.OperatingMode;
import com.diozero.devices.BME68x.OversamplingMultiplier;

@SuppressWarnings("static-method")
public class BME68xTest {
	private static final float DELTA = 0.0001f;

	/*-
	gr_l_int=705
	gr_l_float=704.999878
	gr_h_int=5600
	gr_h_float=5684.846191
	res_heat_int for temp 320=128
	res_heat_float for temp 320=129
	gas_wait for dur 150=101
	gas_wait for dur 4032=255
	gas_wait for dur 4031=254
	heatr_dur_shared for dur 150=147
	heatr_dur_shared for dur 1923=255
	heatr_dur_shared for dur 1922=254
	 */
	@Test
	public void validateData() {
		float ambient_temp = 25;

		OversamplingMultiplier hum_os = OversamplingMultiplier.X1;
		OversamplingMultiplier temp_os = OversamplingMultiplier.X2;
		OversamplingMultiplier press_os = OversamplingMultiplier.X16;

		int meas_dur_us = BME68x.calculateMeasureDuration(hum_os, temp_os, press_os, OperatingMode.FORCED);
		System.out.println("meas_dur_us: " + meas_dur_us);
		press_os = OversamplingMultiplier.X2;
		meas_dur_us = BME68x.calculateMeasureDuration(hum_os, temp_os, press_os, OperatingMode.FORCED);
		System.out.println("meas_dur_us: " + meas_dur_us);
		hum_os = OversamplingMultiplier.X16;
		temp_os = OversamplingMultiplier.X16;
		press_os = OversamplingMultiplier.X16;
		meas_dur_us = BME68x.calculateMeasureDuration(hum_os, temp_os, press_os, OperatingMode.FORCED);
		System.out.println("meas_dur_us: " + meas_dur_us);

		BME68x.Calibration calib = new BME68x.Calibration();
		calib.resistanceHeaterRange = 1;
		calib.resistanceHeaterValue = 39;
		calib.rangeSwitchingError = 0;
		calib.gasHeater[1] = -5686;
		calib.gasHeater[2] = 18;

		int gas_res_adc = 1023;
		int gas_range = 13;
		long gr_l_int = BME68x.calculateGasResistanceLowInt(gas_res_adc, gas_range, calib.rangeSwitchingError);
		Assertions.assertEquals(705, gr_l_int);
		float gr_l_float = BME68x.calculateGasResistanceLowFpu(gas_res_adc, gas_range, calib.rangeSwitchingError);
		Assertions.assertEquals(704.999878f, gr_l_float, DELTA);

		long gr_h_int = BME68x.calculateGasResistanceHighInt(gas_res_adc, gas_range);
		Assertions.assertEquals(5600, gr_h_int);
		float gr_h_float = BME68x.calculateGasResistanceHighFpu(gas_res_adc, gas_range);
		Assertions.assertEquals(5684.846191f, gr_h_float, DELTA);

		int temp = 320;
		long res_heat_int = BME68x.calculateHeaterResistanceRegValInt(calib, (int) ambient_temp, temp);
		Assertions.assertEquals(128, res_heat_int);
		float res_heat_float = BME68x.calculateHeaterResistanceRegValFpu(calib, ambient_temp, temp);
		Assertions.assertEquals(129, res_heat_float, DELTA);

		int dur = 150;
		long gas_wait = BME68x.calculateGasWaitRegVal(dur);
		Assertions.assertEquals(0x65, gas_wait);
		dur = 100; // From the datasheet - 100ms
		gas_wait = BME68x.calculateGasWaitRegVal(dur);
		Assertions.assertEquals(0x59, gas_wait);
		dur = 4;
		gas_wait = BME68x.calculateGasWaitRegVal(dur);
		Assertions.assertEquals(0b00000100, gas_wait);
		dur = 63;
		gas_wait = BME68x.calculateGasWaitRegVal(dur);
		Assertions.assertEquals(0b00111111, gas_wait);
		dur = 64;
		gas_wait = BME68x.calculateGasWaitRegVal(dur);
		Assertions.assertEquals(0b01010000, gas_wait);
		dur = 0xfc0; // 4032
		gas_wait = BME68x.calculateGasWaitRegVal(dur);
		Assertions.assertEquals(0xff, gas_wait);
		dur = 0xfc0 - 1;
		gas_wait = BME68x.calculateGasWaitRegVal(dur);
		Assertions.assertEquals(0xfe, gas_wait);

		dur = 150;
		long heatr_dur_shared = BME68x.calculateGasWaitSharedRegVal(dur);
		Assertions.assertEquals(0x93, heatr_dur_shared);
		dur = 0x783; // 1923 (max value that can be represented in the register value)
		heatr_dur_shared = BME68x.calculateGasWaitSharedRegVal(dur);
		Assertions.assertEquals(0xff, heatr_dur_shared);
		dur = 0x783 - 1;
		heatr_dur_shared = BME68x.calculateGasWaitSharedRegVal(dur);
		Assertions.assertEquals(0xfe, heatr_dur_shared);
	}
}
