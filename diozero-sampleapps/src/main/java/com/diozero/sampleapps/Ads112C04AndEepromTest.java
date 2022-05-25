package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     Ads112C04AndEepromTest.java
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

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.tinylog.Logger;

import com.diozero.devices.Ads112C04;
import com.diozero.devices.Ads112C04.Address;
import com.diozero.devices.Ads112C04.CrcConfig;
import com.diozero.devices.Ads112C04.DataRate;
import com.diozero.devices.Ads112C04.GainConfig;
import com.diozero.devices.Ads112C04.VRef;
import com.diozero.devices.McpEeprom;
import com.diozero.util.DiozeroScheduler;
import com.diozero.util.SleepUtil;

public class Ads112C04AndEepromTest {
	private static AtomicBoolean running;

	public static void main(String[] args) {
		int c = 1;
		if (args.length > 0) {
			c = Integer.parseInt(args[0]);
		}
		final int controller = c;
		int readings = 1_000_000;
		if (args.length > 1) {
			readings = Integer.parseInt(args[1]);
		}
		int adc_num = 2;
		if (args.length > 2) {
			adc_num = Integer.parseInt(args[2]);
		}

		McpEeprom.Type eeprom_type = McpEeprom.Type.MCP_24xx512;

		Future<?> f = DiozeroScheduler.getNonDaemonInstance().submit(() -> {
			String text_to_write = "Hello Matt";
			Logger.info("Running MCP thread...");
			int address = 0x1000;
			try (McpEeprom eeprom = new McpEeprom(controller, eeprom_type)) {
				while (running.get()) {
					eeprom.writeBytes(address, text_to_write.getBytes());
					String text_read = new String(eeprom.readBytes(address, text_to_write.length()));
					if (!text_read.equals(text_to_write)) {
						Logger.error("Error, read " + text_read + " didn't match that written " + text_to_write);
					}
					SleepUtil.sleepMillis(10);
				}
			}
			Logger.info("Finished MCP thread...");
		});

		try (Ads112C04 ads = Ads112C04.builder(Address.GND_GND) //
				.setController(controller) //
				.setCrcConfig(CrcConfig.DISABLED) //
				.setDataCounterEnabled(true) //
				.setDataRate(DataRate._1000HZ) //
				.setGainConfig(GainConfig._1) //
				.setPgaEnabled(false) //
				.setTurboModeEnabled(true) //
				.setVRef(VRef.ANALOG_SUPPLY) //
				.build()) {
			ads.setContinuousModeNonDifferential(adc_num);

			Logger.info("Starting readings with a target data rate of {} SPS...",
					Integer.valueOf(ads.getDataRateFrequency()));
			float avg = 0;
			long start_ms = System.currentTimeMillis();
			for (int i = 1; i <= readings; i++) {
				short reading = ads.getReadingOnDataCounterChange();
				avg += ((reading - avg) / i);
			}
			long duration_ms = System.currentTimeMillis() - start_ms;
			double frequency = readings / (duration_ms / 1000.0);

			Logger.info("Average Value: {#,###.0}, # readings: {#,###}, duration: {#,###.#} ms, frequency: {#,###} Hz",
					Float.valueOf(avg), Integer.valueOf(readings), Long.valueOf(duration_ms),
					Double.valueOf(frequency));

			// Switch back to single shot mode
			short reading = ads.getSingleShotReadingNonDifferential(adc_num);
			Logger.info("Single-shot reading prior to power-down: {}", Short.valueOf(reading));

			// Finally power-down the ADS
			ads.powerDown();
		} finally {
			Logger.info("Stopping thread");
			running.set(false);
			f.cancel(true);
		}
	}
}
