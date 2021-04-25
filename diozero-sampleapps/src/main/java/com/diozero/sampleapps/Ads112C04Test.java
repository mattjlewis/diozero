package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Sample applications
 * Filename:     Ads112C04Test.java
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

import java.util.concurrent.atomic.AtomicInteger;

import org.tinylog.Logger;

import com.diozero.api.DigitalInputDevice;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.devices.Ads112C04;
import com.diozero.devices.Ads112C04.Address;
import com.diozero.devices.Ads112C04.ConfigRegister;
import com.diozero.devices.Ads112C04.CrcConfig;
import com.diozero.devices.Ads112C04.DataRate;
import com.diozero.devices.Ads112C04.GainConfig;
import com.diozero.devices.Ads112C04.VRef;
import com.diozero.util.SleepUtil;

public class Ads112C04Test {
	public static void main(String[] args) {
		int controller = 1;
		if (args.length > 0) {
			controller = Integer.parseInt(args[0]);
		}
		int readings = 100_000;
		if (args.length > 1) {
			readings = Integer.parseInt(args[1]);
		}
		int adc_num = 2;
		if (args.length > 2) {
			adc_num = Integer.parseInt(args[2]);
		}

		try (DigitalInputDevice intr = new DigitalInputDevice(60, GpioPullUpDown.PULL_UP, GpioEventTrigger.FALLING);
				Ads112C04 ads = Ads112C04.builder(Address.GND_GND) //
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
				short reading = ads.getSingleShotReadingNonDifferential(adc_num);
				Logger.info("Single-shot reading: {}. CRC Config: {}, DC: {}", Short.valueOf(reading),
						ads.getCrcConfig(), Boolean.valueOf(ads.isDataCounterEnabled()));
			}

			ads.setCrcConfig(CrcConfig.INVERTED_DATA_OUTPUT);
			ads.setDataCounterEnabled(true);
			for (int i = 0; i < 5; i++) {
				short reading = ads.getSingleShotReadingNonDifferential(adc_num);
				Logger.info("Single-shot reading: {}. CRC Config: {}, DC: {}", Short.valueOf(reading),
						ads.getCrcConfig(), Boolean.valueOf(ads.isDataCounterEnabled()));
			}

			ads.setCrcConfig(CrcConfig.INVERTED_DATA_OUTPUT);
			ads.setDataCounterEnabled(false);
			for (int i = 0; i < 5; i++) {
				short reading = ads.getSingleShotReadingNonDifferential(adc_num);
				Logger.info("Single-shot reading: {}. CRC Config: {}, DC: {}", Short.valueOf(reading),
						ads.getCrcConfig(), Boolean.valueOf(ads.isDataCounterEnabled()));
			}

			ads.setCrcConfig(CrcConfig.CRC16);
			ads.setDataCounterEnabled(true);
			for (int i = 0; i < 5; i++) {
				short reading = ads.getSingleShotReadingNonDifferential(adc_num);
				Logger.info("Single-shot reading: {}. CRC Config: {}, DC: {}", Short.valueOf(reading),
						ads.getCrcConfig(), Boolean.valueOf(ads.isDataCounterEnabled()));
			}

			ads.setCrcConfig(CrcConfig.CRC16);
			ads.setDataCounterEnabled(false);
			for (int i = 0; i < 5; i++) {
				short reading = ads.getSingleShotReadingNonDifferential(adc_num);
				Logger.info("Single-shot reading: {}. CRC Config: {}, DC: {}", Short.valueOf(reading),
						ads.getCrcConfig(), Boolean.valueOf(ads.isDataCounterEnabled()));
			}

			ads.setCrcConfig(CrcConfig.DISABLED);
			ads.setDataCounterEnabled(true);
			for (int i = 0; i < 5; i++) {
				short reading = ads.getSingleShotReadingNonDifferential(adc_num);
				Logger.info("Single-shot reading: {}. CRC Config: {}, DC: {}", Short.valueOf(reading),
						ads.getCrcConfig(), Boolean.valueOf(ads.isDataCounterEnabled()));
			}

			for (int i = 0; i < 40; i++) {
				short reading = ads.getSingleShotReadingNonDifferential(adc_num);
				Logger.info("Single-shot reading: {}. CRC Config: {}, DC: {}", Short.valueOf(reading),
						ads.getCrcConfig(), Boolean.valueOf(ads.isDataCounterEnabled()));
				SleepUtil.sleepMillis(250);
			}

			// Then do continuous mode tests, check if the observed frequency correlates
			// with that configured
			Logger.info(
					"Config register 1: 0x" + Integer.toHexString(ads.readConfigRegister(ConfigRegister._1) & 0xff));
			ads.setConfig1(DataRate._1000HZ, true, ads.getVRefConfig(), false);
			SleepUtil.sleepMillis(1);
			Logger.info(
					"Config register 1: 0x" + Integer.toHexString(ads.readConfigRegister(ConfigRegister._1) & 0xff));

			Logger.info(
					"Config register 2: 0x" + Integer.toHexString(ads.readConfigRegister(ConfigRegister._2) & 0xff));
			ads.setConfig2(true, CrcConfig.DISABLED, ads.getBurnoutCurrentSources(), ads.getIdacCurrent());
			SleepUtil.sleepMillis(1);
			Logger.info(
					"Config register 2: 0x" + Integer.toHexString(ads.readConfigRegister(ConfigRegister._2) & 0xff));

			ads.setContinuousModeNonDifferential(adc_num);

			Logger.info("Taking {#,###} readings with a target data rate of {#,###} SPS...", Integer.valueOf(readings),
					Integer.valueOf(ads.getDataRateFrequency()));
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

			// Disable the data counter
			ads.setConfig2(false, CrcConfig.DISABLED, ads.getBurnoutCurrentSources(), ads.getIdacCurrent());
			SleepUtil.sleepMillis(1);

			// 30 seconds
			int sleep_secs = 30;
			AtomicInteger counter = new AtomicInteger();
			intr.whenActivated(nano_time -> {
				// System.out.println("Activated!");
				short reading = ads.readData(2);
				counter.incrementAndGet();
			});
			SleepUtil.sleepSeconds(sleep_secs);
			intr.whenActivated(null);
			readings = counter.get();
			Logger.info("With interrupts - readings: {#,###}, duration: {#,###} s, frequency: {#,###} Hz",
					Integer.valueOf(readings), Integer.valueOf(sleep_secs),
					Double.valueOf(readings / (double) sleep_secs));

			counter.set(0);
			Object lock = ads.setContinuousModeNonDifferential(adc_num, intr, event -> {
				short reading = event.getReading();
				counter.incrementAndGet();
			});
			SleepUtil.sleepSeconds(sleep_secs);
			Logger.info("With event queue - readings: {#,###}, duration: {#,###} s, frequency: {#,###} Hz",
					Integer.valueOf(readings), Integer.valueOf(sleep_secs),
					Double.valueOf(readings / (double) sleep_secs));
			lock.notify();

			// Switch back to single shot mode
			short reading = ads.getSingleShotReadingNonDifferential(adc_num);
			Logger.info("Single-shot reading prior to power-down: {}", Short.valueOf(reading));

			// Finally power-down the ADS
			ads.powerDown();
		}
	}
}
