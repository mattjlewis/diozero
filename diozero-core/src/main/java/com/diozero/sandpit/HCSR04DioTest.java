package com.diozero.sandpit;

import org.pmw.tinylog.Logger;

import com.diozero.api.DigitalInputOutputDevice;
import com.diozero.internal.spi.GpioDeviceInterface;
import com.diozero.util.SleepUtil;

public class HCSR04DioTest {
	private static final double SPEED_OF_SOUND_CM_PER_S = 34029; // Approx Speed of Sound at sea level and 15 degC
	
	public static void main(String[] args) {
		try (DigitalInputOutputDevice dio = new DigitalInputOutputDevice(4, GpioDeviceInterface.Mode.DIGITAL_OUTPUT)) {
			for (int i=0; i<3; i++) {
				dio.setValue(true);
				SleepUtil.sleepSeconds(1);
				dio.setValue(false);
				SleepUtil.sleepSeconds(1);
			}
		}
		System.out.println("Starting");
		try (DigitalInputOutputDevice dio = new DigitalInputOutputDevice(20, GpioDeviceInterface.Mode.DIGITAL_OUTPUT)) {
			for (int i=0; i<3; i++) {
				dio.setMode(GpioDeviceInterface.Mode.DIGITAL_OUTPUT);
				System.out.println("setValue(true)");
				dio.setValue(true);
				SleepUtil.sleepNanos(10);
				System.out.println("setValue(false)");
				dio.setValue(false);
				dio.setMode(GpioDeviceInterface.Mode.DIGITAL_INPUT);
				SleepUtil.sleepSeconds(1);
			}

			for (int i=0; i<10; i++) {
				double distance = getDistance(dio);
				System.out.println("distance=" + distance);
				SleepUtil.sleepSeconds(1);
			}
		}
	}
	
	public static double getDistance(DigitalInputOutputDevice dio) {
		System.out.println("Starting test");
		dio.setMode(GpioDeviceInterface.Mode.DIGITAL_OUTPUT);
		System.out.println("setValue(true)");
		dio.setValue(true);
		SleepUtil.sleepNanos(10);
		dio.setValue(false);
		dio.setMode(GpioDeviceInterface.Mode.DIGITAL_INPUT);
		// Wait for the pin to go high
		long start = System.nanoTime();
		while (! dio.getValue()) {
			if (System.nanoTime() - start > 500_000_000) {
				Logger.error("Timeout exceeded waiting for echo to go high");
				return -1;
			}
		}
		long echo_on_time = System.nanoTime();
		// Wait for the pin to go low
		while (dio.getValue()) {
			if (System.nanoTime() - echo_on_time > 500_000_000) {
				Logger.error("Timeout exceeded waiting for echo to go low");
				return -1;
			}
		}
		long duration_ns = System.nanoTime() - echo_on_time;
		double ping_duration_s = (duration_ns) / (double) SleepUtil.NS_IN_SEC;
		
		// Distance = velocity * time taken
		// Half the ping duration as it is the time to the object and back
		return SPEED_OF_SOUND_CM_PER_S * (ping_duration_s / 2.0);
	}
}
