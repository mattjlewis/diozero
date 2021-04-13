package com.diozero.sampleapps.sandpit;

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
		int duration_ms = 10_000;
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
