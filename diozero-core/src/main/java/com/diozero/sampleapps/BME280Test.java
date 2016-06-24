package com.diozero.sampleapps;

import com.diozero.BME280;
import com.diozero.util.SleepUtil;

public class BME280Test {
	public static void main(String[] args) {
		try (BME280 bme280 = new BME280()) {
			for (int i=0; i<10; i++) {
				float[] tph = bme280.getValues();
				System.out.printf("Temperature in Celsius : %.2f C %n", Double.valueOf(tph[0]));
				System.out.printf("Pressure : %.2f hPa %n", Double.valueOf(tph[1]));
				System.out.printf("Relative Humidity : %.2f %% RH %n", Double.valueOf(tph[2]));
				
				SleepUtil.sleepSeconds(1);
			}
		}
	}
}
