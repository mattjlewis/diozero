package com.diozero.sampleapps;

import com.diozero.devices.sandpit.BME68x;
import com.diozero.devices.sandpit.BME68x.OperatingMode;
import com.diozero.util.SleepUtil;

/**
 * BME680 temperature / pressure / humidity sensor sample application. To run:
 * <ul>
 * <li>Built-in:<br>
 * {@code java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar com.diozero.sampleapps.BME680Test}</li>
 * <li>pigpgioj:<br>
 * {@code sudo java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-pigpio-$DIOZERO_VERSION.jar:pigpioj-java-2.4.jar com.diozero.sampleapps.BME680Test}</li>
 * </ul>
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

			for (int x = 0; x < 2; x++) {
				System.out.format("hum os: %s, temp os: %s, press os: %s, IIR Filter: %s, ODR: %s%n",
						bme68x.getHumidityOversample(), bme68x.getTemperatureOversample(),
						bme68x.getPressureOversample(), bme68x.getIirFilterConfig(), bme68x.getOdr());
				// bme68x.setHumidityOversample(BME680.OversamplingMultiplier.X2);
				// bme68x.setTemperatureOversample(BME680.OversamplingMultiplier.X8);
				// bme68x.setPressureOversample(BME680.OversamplingMultiplier.X4);
				// bme68x.setFilter(BME680.FilterSize.SIZE_3);
				bme68x.setConfiguration(BME68x.OversamplingMultiplier.X2, BME68x.OversamplingMultiplier.X2,
						BME68x.OversamplingMultiplier.X2, BME68x.IirFilterCoefficient._3, BME68x.ODR.NONE);
				System.out.format("hum os: %s, temp os: %s, press os: %s, IIR Filter: %s, ODR: %s%n",
						bme68x.getHumidityOversample(), bme68x.getTemperatureOversample(),
						bme68x.getPressureOversample(), bme68x.getIirFilterConfig(), bme68x.getOdr());

				OperatingMode target_operating_mode = OperatingMode.FORCED;

				bme68x.setHeaterConfiguration(target_operating_mode, new BME68x.HeaterConfig(true, 320, 150));

				// Calculate delay period in microseconds
				long remaining_duration = bme68x.getRemainingMeasureDuration(target_operating_mode);
				System.out.println("remaining_duration: " + remaining_duration + " microseconds");

				for (int i = 0; i < 5; i++) {
					int reading = 0;
					for (BME68x.Data data : bme68x.getSensorData(target_operating_mode)) {
						System.out.format(
								"Reading: %d. Temperature: %.2f C. Pressure: %.2f hPa. Relative Humidity: %.2f %%rH. Gas Resistance: %.2f Ohms (heater stable: %b, gas valid: %b).%n",
								Integer.valueOf(reading), Float.valueOf(data.getTemperature()),
								Float.valueOf(data.getPressure()), Float.valueOf(data.getHumidity()),
								Float.valueOf(data.getGasResistance()), Boolean.valueOf(data.isHeaterTempStable()),
								Boolean.valueOf(data.isGasMeasurementValid()));
						reading++;
					}

					SleepUtil.sleepSeconds(1);
				}

				bme68x.softReset();
			}

			System.out.format("hum os: %s, temp os: %s, press os: %s, IIR Filter: %s, ODR: %s%n",
					bme68x.getHumidityOversample(), bme68x.getTemperatureOversample(), bme68x.getPressureOversample(),
					bme68x.getIirFilterConfig(), bme68x.getOdr());
			// bme68x.setHumidityOversample(BME680.OversamplingMultiplier.X2);
			// bme68x.setTemperatureOversample(BME680.OversamplingMultiplier.X8);
			// bme68x.setPressureOversample(BME680.OversamplingMultiplier.X4);
			// bme68x.setFilter(BME680.FilterSize.SIZE_3);
			bme68x.setConfiguration(BME68x.OversamplingMultiplier.X1, BME68x.OversamplingMultiplier.X2,
					BME68x.OversamplingMultiplier.X16, BME68x.IirFilterCoefficient.NONE, BME68x.ODR._0_59_MS);
			System.out.format("hum os: %s, temp os: %s, press os: %s, IIR Filter: %s, ODR: %s%n",
					bme68x.getHumidityOversample(), bme68x.getTemperatureOversample(), bme68x.getPressureOversample(),
					bme68x.getIirFilterConfig(), bme68x.getOdr());

			OperatingMode target_operating_mode = OperatingMode.PARALLEL;

			int shared_heatr_dur = BME68x.GAS_WAIT_SHARED
					- (bme68x.getRemainingMeasureDuration(target_operating_mode) / 1000);
			System.out.println("shared_heatr_dur: " + shared_heatr_dur);

			bme68x.setHeaterConfiguration(target_operating_mode,
					new BME68x.HeaterConfig(true, new int[] { 320, 100, 100, 100, 200, 200, 200, 320, 320, 320 },
							new int[] { 5, 2, 10, 30, 5, 5, 5, 5, 5, 5 }, shared_heatr_dur));

			// Calculate delay period in microseconds
			int remaining_duration = bme68x.getRemainingMeasureDuration(target_operating_mode);
			System.out.println("remaining_duration: " + remaining_duration + " microseconds");

			for (int i = 0; i < 10; i++) {
				SleepUtil.sleepMillis(remaining_duration / 1_000 + 1);

				int reading = 0;
				for (BME68x.Data data : bme68x.getSensorData(target_operating_mode)) {
					System.out.format(
							"Reading: %d. Temperature: %.2f C. Pressure: %.2f hPa. Relative Humidity: %.2f %%rH. Gas Resistance: %.2f Ohms (heater stable: %b, gas valid: %b).%n",
							Integer.valueOf(reading), Float.valueOf(data.getTemperature()),
							Float.valueOf(data.getPressure()), Float.valueOf(data.getHumidity()),
							Float.valueOf(data.getGasResistance()), Boolean.valueOf(data.isHeaterTempStable()),
							Boolean.valueOf(data.isGasMeasurementValid()));
					reading++;
				}

				// SleepUtil.sleepSeconds(1);
			}

			bme68x.softReset();

			bme68x.lowGasSelfTestCheck();
		}
	}
}
