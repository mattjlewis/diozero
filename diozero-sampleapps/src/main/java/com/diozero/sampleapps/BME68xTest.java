package com.diozero.sampleapps;

import java.util.ArrayList;
import java.util.List;

import com.diozero.devices.BME68x;
import com.diozero.devices.BME68x.Data;
import com.diozero.devices.BME68x.HeaterConfig;
import com.diozero.devices.BME68x.IirFilterCoefficient;
import com.diozero.devices.BME68x.ODR;
import com.diozero.devices.BME68x.OperatingMode;
import com.diozero.devices.BME68x.OversamplingMultiplier;
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

		try (BME68x bme68x = new BME68x(controller)) {
			System.out.format("chipId: 0x%x, variantId: 0x%x, uniqueId: 0x%x%n", Integer.valueOf(bme68x.getChipId()),
					Integer.valueOf(bme68x.getVariantId()), Integer.valueOf(bme68x.getUniqueId()));

			bme68x.setOperatingMode(OperatingMode.PARALLEL);
			System.out.println(bme68x.getOperatingMode());
			bme68x.setOperatingMode(OperatingMode.SEQUENTIAL);
			System.out.println(bme68x.getOperatingMode());
			bme68x.setOperatingMode(OperatingMode.SLEEP);
			System.out.println(bme68x.getOperatingMode());

			bme68x.lowGasSelfTestCheck();

			bme68x.softReset();

			for (int x = 0; x < 2; x++) {
				forcedModeTest(bme68x);

				bme68x.softReset();
			}

			parallelModeTest(bme68x);

			bme68x.softReset();

			iaqTest(bme68x);
		}
	}

	private static void forcedModeTest(BME68x bme68x) {
		System.out.format("hum os: %s, temp os: %s, press os: %s, IIR Filter: %s, ODR: %s%n",
				bme68x.getHumidityOversample(), bme68x.getTemperatureOversample(), bme68x.getPressureOversample(),
				bme68x.getIirFilterConfig(), bme68x.getOdr());
		bme68x.setConfiguration(OversamplingMultiplier.X2, OversamplingMultiplier.X2, OversamplingMultiplier.X2,
				IirFilterCoefficient._3, ODR.NONE);
		System.out.format("hum os: %s, temp os: %s, press os: %s, IIR Filter: %s, ODR: %s%n",
				bme68x.getHumidityOversample(), bme68x.getTemperatureOversample(), bme68x.getPressureOversample(),
				bme68x.getIirFilterConfig(), bme68x.getOdr());

		OperatingMode target_operating_mode = OperatingMode.FORCED;

		bme68x.setHeaterConfiguration(target_operating_mode, new HeaterConfig(true, 320, 150));

		// Calculate delay period in microseconds
		long remaining_duration_us = bme68x.getRemainingMeasureDuration(target_operating_mode);
		// System.out.println("remaining_duration_us: " + remaining_duration_us + "
		// microseconds");
		SleepUtil.sleepMillis(remaining_duration_us / 1_000 + 1);

		for (int i = 0; i < 5; i++) {
			int reading = 0;
			for (Data data : bme68x.getSensorData(target_operating_mode)) {
				System.out.format(
						"Reading [%d]: Temperature: %.2f C. Pressure: %.2f hPa. Relative Humidity: %.2f %%rH. Gas Resistance: %.2f Ohms (heater stable: %b, gas valid: %b).%n",
						Integer.valueOf(reading), Float.valueOf(data.getTemperature()),
						Float.valueOf(data.getPressure()), Float.valueOf(data.getHumidity()),
						Float.valueOf(data.getGasResistance()), Boolean.valueOf(data.isHeaterTempStable()),
						Boolean.valueOf(data.isGasMeasurementValid()));
				reading++;
			}

			SleepUtil.sleepSeconds(1);
		}
	}

	private static void parallelModeTest(BME68x bme68x) {
		System.out.format("hum os: %s, temp os: %s, press os: %s, IIR Filter: %s, ODR: %s%n",
				bme68x.getHumidityOversample(), bme68x.getTemperatureOversample(), bme68x.getPressureOversample(),
				bme68x.getIirFilterConfig(), bme68x.getOdr());
		bme68x.setConfiguration(OversamplingMultiplier.X1, OversamplingMultiplier.X2, OversamplingMultiplier.X16,
				IirFilterCoefficient.NONE, ODR._0_59_MS);
		System.out.format("hum os: %s, temp os: %s, press os: %s, IIR Filter: %s, ODR: %s%n",
				bme68x.getHumidityOversample(), bme68x.getTemperatureOversample(), bme68x.getPressureOversample(),
				bme68x.getIirFilterConfig(), bme68x.getOdr());

		OperatingMode target_operating_mode = OperatingMode.PARALLEL;

		int shared_heatr_dur = BME68x.GAS_WAIT_SHARED
				- (bme68x.getRemainingMeasureDuration(target_operating_mode) / 1000);
		System.out.println("shared_heatr_dur: " + shared_heatr_dur);

		bme68x.setHeaterConfiguration(target_operating_mode,
				new HeaterConfig(true, new int[] { 320, 100, 100, 100, 200, 200, 200, 320, 320, 320 },
						new int[] { 5, 2, 10, 30, 5, 5, 5, 5, 5, 5 }, shared_heatr_dur));

		// Calculate delay period in microseconds
		int remaining_duration_us = bme68x.getRemainingMeasureDuration(target_operating_mode);
		// System.out.println("remaining_duration_us: " + remaining_duration_us + "
		// microseconds");

		for (int i = 0; i < 10; i++) {
			SleepUtil.sleepMillis(remaining_duration_us / 1_000 + 1);

			int reading = 0;
			for (Data data : bme68x.getSensorData(target_operating_mode)) {
				System.out.format(
						"Reading [%d]: Temperature: %.2f C. Pressure: %.2f hPa. Relative Humidity: %.2f %%rH. Gas Resistance: %.2f Ohms (heater stable: %b, gas valid: %b).%n",
						Integer.valueOf(reading), Float.valueOf(data.getTemperature()),
						Float.valueOf(data.getPressure()), Float.valueOf(data.getHumidity()),
						Float.valueOf(data.getGasResistance()), Boolean.valueOf(data.isHeaterTempStable()),
						Boolean.valueOf(data.isGasMeasurementValid()));
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
				bme68x.getIirFilterConfig(), bme68x.getOdr());
		bme68x.setConfiguration(OversamplingMultiplier.X1, OversamplingMultiplier.X2, OversamplingMultiplier.X16,
				IirFilterCoefficient.NONE, ODR._0_59_MS);
		System.out.format("hum os: %s, temp os: %s, press os: %s, IIR Filter: %s, ODR: %s%n",
				bme68x.getHumidityOversample(), bme68x.getTemperatureOversample(), bme68x.getPressureOversample(),
				bme68x.getIirFilterConfig(), bme68x.getOdr());

		OperatingMode target_operating_mode = OperatingMode.FORCED;

		bme68x.setHeaterConfiguration(target_operating_mode, new HeaterConfig(true, 320, 150));

		// Calculate delay period in microseconds
		long remaining_duration_us = bme68x.getRemainingMeasureDuration(target_operating_mode);
		// System.out.println("remaining_duration_us: " + remaining_duration_us + "
		// microseconds");
		SleepUtil.sleepMillis(remaining_duration_us / 1_000 + 1);

		// start_time and curr_time ensure that the
		// burn_in_time (in seconds) is kept track of
		long start_time_ms = System.currentTimeMillis();
		int burn_in_time_sec = 300;

		// Collect gas resistance burn-in values, then use the average of the last 50
		// values to set the upper limit for calculating gas_baseline
		System.out.format("Collecting gas resistance burn-in data for %,d seconds%n",
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
			System.out.format("Gas: %,.2f Ohms. Remaining burn-in time: %,d%n",
					Float.valueOf(data[0].getGasResistance()),
					Long.valueOf(burn_in_time_sec - (System.currentTimeMillis() - start_time_ms) / 1000));
		}

		// Get the average of the last 50 values
		float gas_baseline = gas_res_burn_in_data.subList(gas_res_burn_in_data.size() - 50, gas_res_burn_in_data.size())
				.stream().reduce(Float.valueOf(0f), Float::sum).floatValue() / 50;

		// Set the humidity baseline to 40%, an optimal indoor humidity.
		// float hum_baseline = 40f;
		float hum_baseline = hum_burn_in_data.subList(hum_burn_in_data.size() - 50, hum_burn_in_data.size()).stream()
				.reduce(Float.valueOf(0f), Float::sum).floatValue() / 50;

		// This sets the balance between humidity and gas reading in the calculation of
		// air_quality_score (25:75, humidity:gas)
		float hum_weighting = 0.25f;

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
