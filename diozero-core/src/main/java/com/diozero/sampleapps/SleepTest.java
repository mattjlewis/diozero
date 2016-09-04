package com.diozero.sampleapps;

import org.pmw.tinylog.Logger;

import com.diozero.util.SleepUtil;

public class SleepTest {
	public static void main(String[] args) {
		int sleep_ns = 10_000;
		
		Logger.info("JNI sleep");
		SleepUtil.sleepNanos(0, 100_000);
		for (int i=0; i<10; i++) {
			long start = System.nanoTime();
			SleepUtil.sleepNanos(0, sleep_ns);
			long duration = System.nanoTime() - start;
			Logger.info("Slept for " + duration + " nanos, difference=" + (duration - sleep_ns));
		}

		Logger.info("Java sleep");
		try {
			Thread.sleep(0, sleep_ns);
			for (int i=0; i<10; i++) {
				long start = System.nanoTime();
				Thread.sleep(0, sleep_ns);
				long duration = System.nanoTime() - start;
				Logger.info("Slept for " + duration + " nanos, difference=" + (duration - sleep_ns));
			}
		} catch (InterruptedException e) {
			Logger.error(e, "Error: ", e);
		}
		
		Logger.info("Busy sleep");
		SleepUtil.busySleep(sleep_ns);
		for (int i=0; i<10; i++) {
			long start = System.nanoTime();
			SleepUtil.busySleep(sleep_ns);
			long duration = System.nanoTime() - start;
			Logger.info("Slept for " + duration + " nanos, difference=" + (duration - sleep_ns));
		}
	}
}
