package com.diozero.sampleapps;

import org.tinylog.Logger;

import com.diozero.devices.Ads112C04;
import com.diozero.devices.Ads112C04.Address;
import com.diozero.devices.Ads112C04.CrcConfig;
import com.diozero.devices.Ads112C04.DataRate;
import com.diozero.devices.Ads112C04.GainConfig;
import com.diozero.devices.Ads112C04.VRef;

public class Ads112C04Test {
	public static void main(String[] args) {
		int controller = 1;
		if (args.length > 0) {
			controller = Integer.parseInt(args[0]);
		}

		try (Ads112C04 ads = Ads112C04.builder(Address.GND_GND) //
				.setController(controller) //
				.setCrcConfig(CrcConfig.DISABLED) //
				.setDataCounterEnabled(false) //
				.setDataRate(DataRate._20HZ) //
				.setGainConfig(GainConfig._1) //
				.setPgaEnabled(false) //
				.setTurboModeEnabled(false) //
				.setVRef(VRef.ANALOG_SUPPLY) //
				.build()) {
			// First do some single shot tests
			ads.setSingleShotMode();

			ads.setCrcConfig(CrcConfig.DISABLED);
			ads.setDataCounterEnabled(false);
			for (int i = 0; i < 5; i++) {
				short reading = ads.getSingleShotReading(0);
				Logger.info("Single-shot reading: {}. CRC Config: {}, DC: {}", Short.valueOf(reading),
						ads.getCrcConfig(), Boolean.valueOf(ads.isDataCounterEnabled()));
			}

			ads.setCrcConfig(CrcConfig.INVERTED_DATA_OUTPUT);
			ads.setDataCounterEnabled(true);
			for (int i = 0; i < 5; i++) {
				short reading = ads.getSingleShotReading(0);
				Logger.info("Single-shot reading: {}. CRC Config: {}, DC: {}", Short.valueOf(reading),
						ads.getCrcConfig(), Boolean.valueOf(ads.isDataCounterEnabled()));
			}

			ads.setCrcConfig(CrcConfig.INVERTED_DATA_OUTPUT);
			ads.setDataCounterEnabled(false);
			for (int i = 0; i < 5; i++) {
				short reading = ads.getSingleShotReading(0);
				Logger.info("Single-shot reading: {}. CRC Config: {}, DC: {}", Short.valueOf(reading),
						ads.getCrcConfig(), Boolean.valueOf(ads.isDataCounterEnabled()));
			}

			ads.setCrcConfig(CrcConfig.CRC16);
			ads.setDataCounterEnabled(true);
			for (int i = 0; i < 5; i++) {
				short reading = ads.getSingleShotReading(0);
				Logger.info("Single-shot reading: {}. CRC Config: {}, DC: {}", Short.valueOf(reading),
						ads.getCrcConfig(), Boolean.valueOf(ads.isDataCounterEnabled()));
			}

			ads.setCrcConfig(CrcConfig.CRC16);
			ads.setDataCounterEnabled(false);
			for (int i = 0; i < 5; i++) {
				short reading = ads.getSingleShotReading(0);
				Logger.info("Single-shot reading: {}. CRC Config: {}, DC: {}", Short.valueOf(reading),
						ads.getCrcConfig(), Boolean.valueOf(ads.isDataCounterEnabled()));
			}

			ads.setCrcConfig(CrcConfig.DISABLED);
			ads.setDataCounterEnabled(true);
			for (int i = 0; i < 5; i++) {
				short reading = ads.getSingleShotReading(0);
				Logger.info("Single-shot reading: {}. CRC Config: {}, DC: {}", Short.valueOf(reading),
						ads.getCrcConfig(), Boolean.valueOf(ads.isDataCounterEnabled()));
			}

			// Then do continuous mode tests, check if observed frequency correlates with
			// that configured
			ads.setCrcConfig(CrcConfig.DISABLED);
			ads.setDataCounterEnabled(true);
			ads.setDataRate(DataRate._1000HZ);
			ads.setTurboModeEnabled(true);
			ads.setContinuousMode(0);

			int readings = 10_000;
			if (args.length > 1) {
				readings = Integer.parseInt(args[1]);
			}
			Logger.info("Starting readings with a data rate of {} SPS...", Integer.valueOf(ads.getDataRateFrequency()));
			float avg = 0;
			long start_ms = System.currentTimeMillis();
			for (int i = 1; i <= readings; i++) {
				avg += ((ads.getReadingOnDataCounterChange() - avg) / i);
			}
			long duration_ms = System.currentTimeMillis() - start_ms;
			double frequency = readings / (duration_ms / 1000.0);

			Logger.info("Average Value: {#,###.0}, # readings: {#,###}, duration: {#,###.#} ms, frequency: {#,###} Hz",
					Float.valueOf(avg), Integer.valueOf(readings), Long.valueOf(duration_ms),
					Double.valueOf(frequency));

			// Switch back to single shot mode
			short reading = ads.getSingleShotReading(0);
			Logger.info("Single-shot reading prior to power-down: {}", Short.valueOf(reading));

			// Finally power-down the ADS
			ads.powerDown();
		}
	}
}
