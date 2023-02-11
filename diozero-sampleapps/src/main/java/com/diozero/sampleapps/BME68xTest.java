package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     BME68xTest.java
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

import java.util.ArrayList;
import java.util.List;

import com.diozero.devices.BME68x;
import com.diozero.devices.BME68x.Data;
import com.diozero.devices.BME68x.HeaterConfig;
import com.diozero.devices.BME68x.IirFilterCoefficient;
import com.diozero.devices.BME68x.OperatingMode;
import com.diozero.devices.BME68x.OversamplingMultiplier;
import com.diozero.devices.BME68x.StandbyDuration;
import com.diozero.util.Diozero;
import com.diozero.util.SleepUtil;

/**
 * BME68x temperature / pressure / humidity sensor sample application.
 */
public class BME68xTest {
	public static void main(String[] args) {
		int controller = 1;
		if (args.length > 0) {
			controller = Integer.parseInt(args[0]);
		}

		int address = BME68x.DEVICE_ADDRESS;
		if (args.length > 1) {
			address = Integer.parseInt(args[1]);
		}

		try (BME68x bme68x = new BME68x(controller, address)) {
			System.out.format("chipId: 0x%x, variantId: 0x%x, uniqueId: 0x%x%n", Integer.valueOf(bme68x.getChipId()),
					Integer.valueOf(bme68x.getVariantId()), Integer.valueOf(bme68x.getUniqueId()));
			System.out.format(
					"Humidity Oversampling: %s, Temperature Oversampling: %s, Pressure Oversampling: %s, Filter: %s, Standy Duration: %s%n",
					bme68x.getHumidityOversample(), bme68x.getTemperatureOversample(), bme68x.getPressureOversample(),
					bme68x.getIirFilterConfig(), bme68x.getStandbyDuration());

			if (bme68x.getVariantId() == BME68x.VARIANT_ID_BM688) {
				bme68x.setOperatingMode(OperatingMode.PARALLEL);
				System.out.println(bme68x.getOperatingMode());
				bme68x.setOperatingMode(OperatingMode.SEQUENTIAL);
				System.out.println(bme68x.getOperatingMode());
			}
			bme68x.setOperatingMode(OperatingMode.SLEEP);
			System.out.println(bme68x.getOperatingMode());

			// bme68x.lowGasSelfTestCheck();

			bme68x.softReset();

			for (int x = 0; x < 2; x++) {
				forcedModeTest(bme68x);

				bme68x.softReset();
			}

			if (bme68x.getVariantId() == BME68x.VARIANT_ID_BM688) {
				parallelModeTest(bme68x);
			}

			bme68x.softReset();

			iaqTest(bme68x);
		} finally {
			Diozero.shutdown();
		}
	}

	private static void forcedModeTest(BME68x bme68x) {
		System.out.format("hum os: %s, temp os: %s, press os: %s, IIR Filter: %s, ODR: %s%n",
				bme68x.getHumidityOversample(), bme68x.getTemperatureOversample(), bme68x.getPressureOversample(),
				bme68x.getIirFilterConfig(), bme68x.getStandbyDuration());
		bme68x.setConfiguration(OversamplingMultiplier.X2, OversamplingMultiplier.X2, OversamplingMultiplier.X2,
				IirFilterCoefficient._3, StandbyDuration.NONE);
		System.out.format("hum os: %s, temp os: %s, press os: %s, IIR Filter: %s, ODR: %s%n",
				bme68x.getHumidityOversample(), bme68x.getTemperatureOversample(), bme68x.getPressureOversample(),
				bme68x.getIirFilterConfig(), bme68x.getStandbyDuration());

		OperatingMode target_operating_mode = OperatingMode.FORCED;

		bme68x.setHeaterConfiguration(target_operating_mode, new HeaterConfig(true, 320, 150));

		// Calculate delay period in microseconds
		long measure_duration_ms = bme68x.calculateMeasureDuration(target_operating_mode) / 1000;
		// System.out.println("measure_duration_ms: " + measure_duration_ms + "
		// milliseconds");
		SleepUtil.sleepMillis(measure_duration_ms);

		for (int i = 0; i < 5; i++) {
			int reading = 0;
			for (Data data : bme68x.getSensorData(target_operating_mode)) {
				System.out.format(
						"Reading [%d]: Idx: %,d. Temperature: %,.2f C. Pressure: %,.2f hPa. Relative Humidity: %,.2f %%rH. Gas Idx: %,d. Gas Resistance: %,.2f Ohms. IDAC: %,.2f mA. Gas Wait: %,d (ms or multiplier). (heater stable: %b, gas valid: %b).%n",
						Integer.valueOf(reading), Integer.valueOf(data.getMeasureIndex()),
						Float.valueOf(data.getTemperature()), Float.valueOf(data.getPressure()),
						Float.valueOf(data.getHumidity()), Integer.valueOf(data.getGasMeasurementIndex()),
						Float.valueOf(data.getGasResistance()), Float.valueOf(data.getIdacHeatMA()),
						Short.valueOf(data.getGasWait()), Boolean.valueOf(data.isHeaterTempStable()),
						Boolean.valueOf(data.isGasMeasurementValid()));
				reading++;
			}

			SleepUtil.sleepSeconds(1);
		}
	}

	private static void parallelModeTest(BME68x bme68x) {
		System.out.format("hum os: %s, temp os: %s, press os: %s, IIR Filter: %s, ODR: %s%n",
				bme68x.getHumidityOversample(), bme68x.getTemperatureOversample(), bme68x.getPressureOversample(),
				bme68x.getIirFilterConfig(), bme68x.getStandbyDuration());
		bme68x.setConfiguration(OversamplingMultiplier.X2, OversamplingMultiplier.X2, OversamplingMultiplier.X2,
				IirFilterCoefficient.NONE, StandbyDuration.NONE);
		System.out.format("hum os: %s, temp os: %s, press os: %s, IIR Filter: %s, ODR: %s%n",
				bme68x.getHumidityOversample(), bme68x.getTemperatureOversample(), bme68x.getPressureOversample(),
				bme68x.getIirFilterConfig(), bme68x.getStandbyDuration());

		OperatingMode target_operating_mode = OperatingMode.PARALLEL;

		// Calculate TPHG measure duration and convert to milliseconds
		int measure_duration_ms = bme68x.calculateMeasureDuration(target_operating_mode) / 1000;
		System.out.println("measure_duration_ms: " + measure_duration_ms + " milliseconds");

		// Assume that 150ms is the required heater duration.
		// https://github.com/BoschSensortec/BME68x-Sensor-API/blob/master/examples/parallel_mode/parallel_mode.c#L78
		// p39: Measurement duration = gas_wait_X * (gas_wait_shared + TTPHG_duration)
		int heater_duration_ms = 150;
		int gas_wait_shared_ms = heater_duration_ms - measure_duration_ms;
		System.out.println("gas_wait_shared_ms: " + gas_wait_shared_ms);

		bme68x.setHeaterConfiguration(target_operating_mode, new HeaterConfig(true, //
				// Heater temperature in degree Celsius
				new int[] { 320, 100, 100, 100, 200, 200, 200, 320, 320, 320 },
				// Multiplier to the shared heater duration
				new int[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }, //
				// new int[] { 5, 2, 10, 30, 5, 5, 5, 5, 5, 5 },
				gas_wait_shared_ms));

		System.out.println("heater_duration_ms: " + heater_duration_ms);

		int last_gas_meas_idx = -1;
		long last_gas_meas_ms = System.currentTimeMillis();
		for (int i = 0; i < 100; i++) {
			// Note that longer sleep durations result in no data...
			SleepUtil.sleepMillis(heater_duration_ms);

			int reading = 0;
			for (Data data : bme68x.getSensorData(target_operating_mode)) {
				if (data.isNewData()) {
					if (data.isGasMeasurementValid()) {
						System.out.format(
								"Reading [%d]: Idx: %,d. Temperature: %,.2f C. Pressure: %,.2f hPa. Relative Humidity: %,.2f %%rH. Gas Idx: %,d. Gas Resistance: %,.2f Ohms. IDAC: %,.2f mA. Gas Wait: %,d (ms or multiplier). (heater stable: %b, gas valid: %b).%n",
								Integer.valueOf(reading), Integer.valueOf(data.getMeasureIndex() & 0xff),
								Float.valueOf(data.getTemperature()), Float.valueOf(data.getPressure()),
								Float.valueOf(data.getHumidity()), Integer.valueOf(data.getGasMeasurementIndex()),
								Float.valueOf(data.getGasResistance()), Float.valueOf(data.getIdacHeatMA()),
								Short.valueOf(data.getGasWait()), Boolean.valueOf(data.isHeaterTempStable()),
								Boolean.valueOf(data.isGasMeasurementValid()));
						if (data.getGasMeasurementIndex() != last_gas_meas_idx) {
							System.out.println("delta: " + (System.currentTimeMillis() - last_gas_meas_ms));
							last_gas_meas_ms = System.currentTimeMillis();
							last_gas_meas_idx = data.getGasMeasurementIndex();
						}
					} else {
						System.out.format(
								"Reading [%d]: Idx: %,d. Temperature: %,.2f C. Pressure: %,.2f hPa. Relative Humidity: %,.2f %%rH.%n",
								Integer.valueOf(reading), Integer.valueOf(data.getMeasureIndex() & 0xff),
								Float.valueOf(data.getTemperature()), Float.valueOf(data.getPressure()),
								Float.valueOf(data.getHumidity()));
					}
				}
				reading++;
			}
		}
	}

	/*
	 * Credit:
	 * https://github.com/pimoroni/bme680-python/blob/master/examples/indoor-air-
	 * quality.py
	 */
	private static void iaqTest(BME68x bme68x) {
		System.out.format("hum os: %s, temp os: %s, press os: %s, IIR Filter: %s, ODR: %s%n",
				bme68x.getHumidityOversample(), bme68x.getTemperatureOversample(), bme68x.getPressureOversample(),
				bme68x.getIirFilterConfig(), bme68x.getStandbyDuration());
		bme68x.setConfiguration(OversamplingMultiplier.X1, OversamplingMultiplier.X2, OversamplingMultiplier.X16,
				IirFilterCoefficient.NONE, StandbyDuration._0_59_MS);
		System.out.format("hum os: %s, temp os: %s, press os: %s, IIR Filter: %s, ODR: %s%n",
				bme68x.getHumidityOversample(), bme68x.getTemperatureOversample(), bme68x.getPressureOversample(),
				bme68x.getIirFilterConfig(), bme68x.getStandbyDuration());

		OperatingMode target_operating_mode = OperatingMode.FORCED;

		bme68x.setHeaterConfiguration(target_operating_mode, new HeaterConfig(true, 320, 150));

		// Calculate delay period in microseconds
		long measure_duration_ms = bme68x.calculateMeasureDuration(target_operating_mode) / 1000;
		// System.out.println("measure_duration_ms: " + measure_duration_ms + "
		// milliseconds");
		SleepUtil.sleepMillis(measure_duration_ms);

		// start_time and curr_time ensure that the
		// burn_in_time (in seconds) is kept track of
		long start_time_ms = System.currentTimeMillis();
		int burn_in_time_sec = 30;

		// Collect gas resistance burn-in values, then use the average of the last 50
		// values to set the upper limit for calculating gas_baseline
		System.out.format("Collecting gas resistance burn-in data for %,d seconds...%n",
				Integer.valueOf(burn_in_time_sec));
		List<Float> gas_res_burn_in_data = new ArrayList<>();
		List<Float> hum_burn_in_data = new ArrayList<>();
		while ((System.currentTimeMillis() - start_time_ms) / 1000 < burn_in_time_sec) {
			Data[] data = bme68x.getSensorData(target_operating_mode);
			if (data != null && data.length > 0 && data[0].isHeaterTempStable()) {
				gas_res_burn_in_data.add(Float.valueOf(data[0].getGasMeasurementIndex()));
				hum_burn_in_data.add(Float.valueOf(data[0].getHumidity()));
			}
			SleepUtil.sleepSeconds(1);
			System.out.format("Gas: %,.2f Ohms. Remaining burn-in time: %,d secs%n",
					Float.valueOf(data[0].getGasResistance()),
					Long.valueOf(burn_in_time_sec - (System.currentTimeMillis() - start_time_ms) / 1000));
		}

		// Get the average of the last 50% of values
		int num_gas_samples = gas_res_burn_in_data.size();
		float gas_baseline = gas_res_burn_in_data.subList(num_gas_samples / 2, num_gas_samples).stream()
				.reduce(Float.valueOf(0f), Float::sum).floatValue() / num_gas_samples / 2;

		// Set the humidity baseline to 40%, an optimal indoor humidity.
		// float hum_baseline = 40f;
		int num_hum_samples = hum_burn_in_data.size();
		float hum_baseline = hum_burn_in_data.subList(num_hum_samples / 2, num_hum_samples).stream()
				.reduce(Float.valueOf(0f), Float::sum).floatValue() / num_hum_samples / 2;

		// This sets the balance between humidity and gas reading in the calculation of
		// air_quality_score (20:80, humidity:gas)
		float hum_weighting = 0.2f;

		System.out.format("Gas baseline: %,.2f Ohms, humidity baseline: %,.2f %%RH%n", Float.valueOf(gas_baseline),
				Float.valueOf(hum_baseline));

		while (true) {
			Data[] data = bme68x.getSensorData(target_operating_mode);
			if (data != null && data.length > 0 && data[0].isHeaterTempStable()) {
				float gas = data[0].getGasResistance();
				float gas_offset = gas_baseline - gas;

				float hum = data[0].getHumidity();
				float hum_offset = hum - hum_baseline;

				// Calculate hum_score as the distance from the hum_baseline.
				float hum_score;
				if (hum_offset > 0) {
					hum_score = (100 - hum_baseline - hum_offset);
					hum_score /= (100 - hum_baseline);
					hum_score *= (hum_weighting * 100);
				} else {
					hum_score = (hum_baseline + hum_offset);
					hum_score /= hum_baseline;
					hum_score *= (hum_weighting * 100);
				}

				// Calculate gas_score as the distance from the gas_baseline.
				float gas_score;
				if (gas_offset > 0) {
					gas_score = (gas / gas_baseline);
					gas_score *= (100 - (hum_weighting * 100));
				} else {
					gas_score = 100 - (hum_weighting * 100);
				}

				// Calculate air_quality_score.
				float air_quality_score = hum_score + gas_score;

				System.out.format("Gas: %,.2f Ohms, humidity: %,.2f %%RH, air quality: %,.2f%n", Float.valueOf(gas),
						Float.valueOf(hum), Float.valueOf(air_quality_score));

				SleepUtil.sleepSeconds(1);
			}
		}
	}
}
