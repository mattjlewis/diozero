package com.diozero.sampleapps;

import com.diozero.api.AnalogOutputDevice;
import com.diozero.devices.PCF8591;
import com.diozero.util.SleepUtil;

public class AnalogOutputTest {
	public static void main(String[] args) {
		try (PCF8591 dac = new PCF8591();
				AnalogOutputDevice aout = AnalogOutputDevice.Builder.builder(0).setDeviceFactory(dac).build()) {
			for (float f = 0; f < 1; f += 0.1) {
				aout.setValue(f);
				SleepUtil.sleepMillis(100);
			}
			for (float f = 1; f >= 0; f -= 0.1) {
				aout.setValue(f);
				SleepUtil.sleepMillis(100);
			}
		}
	}
}
