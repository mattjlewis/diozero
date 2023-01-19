package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     Ads1115AndEepromTest.java
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

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.diozero.api.AnalogInputDevice;
import com.diozero.api.DigitalInputDevice;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.devices.Ads1x15;
import com.diozero.devices.McpEeprom;
import com.diozero.util.DiozeroScheduler;
import com.diozero.util.SleepUtil;

public class Ads1115AndEepromTest implements Runnable {
	private static AtomicBoolean running;

	@SuppressWarnings("boxing")
	public static void main(String[] args) {
		int controller = 1;
		if (args.length > 0) {
			controller = Integer.parseInt(args[0]);
		}
		int ready_gpio = 24;
		if (args.length > 1) {
			ready_gpio = Integer.parseInt(args[1]);
		}
		
		running = new AtomicBoolean(true);
		Future<?> f = DiozeroScheduler.getNonDaemonInstance().submit(new Ads1115AndEepromTest());

		try (Ads1x15 adc = new Ads1x15(controller, Ads1x15.Address.GND, Ads1x15.PgaConfig._4096MV,
				Ads1x15.Ads1115DataRate._860HZ);
				DigitalInputDevice ready_pin = new DigitalInputDevice(ready_gpio, GpioPullUpDown.PULL_UP,
						GpioEventTrigger.BOTH);
				AnalogInputDevice ain0 = new AnalogInputDevice(adc, 0);
				AnalogInputDevice ain1 = new AnalogInputDevice(adc, 1);
				AnalogInputDevice ain2 = new AnalogInputDevice(adc, 2);
				AnalogInputDevice ain3 = new AnalogInputDevice(adc, 3)) {
			AnalogInputDevice[] ains = new AnalogInputDevice[] { ain0, ain1, ain2, ain3 };
			System.out.println("Range: " + adc.getVRef());
			for (int i = 0; i < 10; i++) {
				for (int channel = 0; channel < adc.getModel().getNumChannels(); channel++) {
					float unscaled = ains[channel].getUnscaledValue();
					float scaled = ains[channel].convertToScaledValue(unscaled);
					System.out.format("Channel #%d : %.2f%% (%.2fv)%n", channel, unscaled, scaled);
				}
				SleepUtil.sleepMillis(500);
			}

			AtomicInteger count = new AtomicInteger();
			adc.setContinousMode(ready_pin, ain3.getGpio(),
					(reading) -> { System.out.format("Callback - Channel #%d : %.2f%% (%.2fv)%n", 3, reading,
							ain3.convertToScaledValue(reading)); count.getAndIncrement(); });
			int wait_secs = 20;
			System.err.println("Sleeping for " + wait_secs + " s");
			SleepUtil.sleepSeconds(wait_secs);
			System.err.println("Got " + count.get() + " callbacks in " + wait_secs + " s");
		} finally {
			System.err.println("Stopping thread");
			running.set(false);
			f.cancel(true);
		}
		System.err.println("Finished");
	}

	@Override
	public void run() {
		String text_to_write = "Hello Matt";
		System.err.println("Running MCP thread...");
		int address = 0x1000;
		try (McpEeprom eeprom = new McpEeprom(2, McpEeprom.Type.MCP_24xx512)) {
			while (running.get()) {
				eeprom.writeBytes(address, text_to_write.getBytes());
				String text_read = new String(eeprom.readBytes(address, text_to_write.length()));
				if (!text_read.equals(text_to_write)) {
					System.err.println("Error, read " + text_read + " didn't match that written " + text_to_write);
				}
				SleepUtil.sleepMillis(10);
			}
		}
		System.err.println("Finished MCP thread...");
	}
}
