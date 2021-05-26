package com.diozero.sampleapps.sandpit;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     Max30102Test.java
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

import java.util.Queue;

import com.diozero.devices.sandpit.Max30102;
import com.diozero.devices.sandpit.Max30102.LedPulseWidth;
import com.diozero.devices.sandpit.Max30102.Mode;
import com.diozero.devices.sandpit.Max30102.Sample;
import com.diozero.devices.sandpit.Max30102.SampleAveraging;
import com.diozero.devices.sandpit.Max30102.SpO2AdcRange;
import com.diozero.devices.sandpit.Max30102.SpO2SampleRate;
import com.diozero.sbc.DeviceFactoryHelper;
import com.diozero.util.SleepUtil;

public class Max30102Test {
	public static void main(String[] args) {
		// Run for 10s
		int duration_ms = 30_000;
		SpO2SampleRate spo2_sample_rate = SpO2SampleRate._100;
		long sleep_ms = 1_000 / spo2_sample_rate.getSampleRate();

		try (Max30102 max = new Max30102(1)) {
			DeviceFactoryHelper.registerForShutdown(max);

			Queue<Sample> sample_queue = max.getSampleQueue();

			max.setup(SampleAveraging._4, false, 15, Mode.SPO2, SpO2AdcRange._4096, spo2_sample_rate,
					LedPulseWidth._411, 7.1f, 7.1f);

			long start_ms = System.currentTimeMillis();
			while ((System.currentTimeMillis() - start_ms) < duration_ms) {
				max.pollForData();

				SleepUtil.sleepMillis(sleep_ms);
			}

			sample_queue.stream().forEach(Max30102Test::printSample);
		} finally {
			DeviceFactoryHelper.shutdown();
		}
	}

	private static void printSample(Sample sample) {
		if (sample.getSpo2() == -1) {
			System.out.println(sample.getHeartRate());
		} else {
			System.out.println(sample.getHeartRate() + ", " + sample.getSpo2());
		}
	}
}
