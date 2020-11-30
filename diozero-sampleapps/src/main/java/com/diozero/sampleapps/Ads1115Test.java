package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Sample applications
 * Filename:     Ads1115Test.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
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

import com.diozero.api.AnalogInputDevice;
import com.diozero.api.DigitalInputDevice;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.devices.Ads1x15;
import com.diozero.devices.Ads1x15.Ads1115DataRate;
import com.diozero.devices.Ads1x15.PgaConfig;
import com.diozero.util.SleepUtil;

public class Ads1115Test {
	@SuppressWarnings("boxing")
	public static void main(String[] args) {
		try (Ads1x15 adc = new Ads1x15(PgaConfig.PGA_4096MV, Ads1115DataRate.DR_860HZ);
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
		}

		int channel = 3;
		int ready_gpio = 24;
		try (Ads1x15 adc = new Ads1x15(PgaConfig.PGA_4096MV, Ads1115DataRate.DR_8HZ);
				AnalogInputDevice ain = new AnalogInputDevice(adc, channel);
				DigitalInputDevice ready_pin = new DigitalInputDevice(ready_gpio, GpioPullUpDown.PULL_UP,
						GpioEventTrigger.BOTH)) {
			adc.setContinousMode(ready_pin, ain.getGpio(),
					(reading) -> System.out.format("Callback - Channel #%d : %.2f%% (%.2fv)%n", channel, reading,
							ain.convertToScaledValue(reading)));
			for (int i = 0; i < 10; i++) {
				float unscaled = ain.getUnscaledValue();
				float scaled = ain.convertToScaledValue(unscaled);
				System.out.format("Channel #%d : %.2f%% (%.2fv)%n", channel, unscaled, scaled);
				SleepUtil.sleepMillis(500);
			}
		}
	}
}
