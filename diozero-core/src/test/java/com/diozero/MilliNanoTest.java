package com.diozero;

import org.junit.jupiter.api.Test;
import org.tinylog.Logger;

import com.diozero.util.SleepUtil;

@SuppressWarnings("static-method")
public class MilliNanoTest {
	@Test
	public void test() {
		double secs = 0.005123;
		
		long millis = Math.round(secs * SleepUtil.MS_IN_SEC);
		long nanos = Math.round(secs * SleepUtil.NS_IN_SEC % SleepUtil.NS_IN_MS);
		
		Logger.info(String.format("Seconds = %.9f, millis = %d, nanos = %d",
				Double.valueOf(secs), Long.valueOf(millis), Long.valueOf(nanos)));
		
		secs = 0.099789;
		
		millis = Math.round(secs * SleepUtil.MS_IN_SEC);
		nanos = Math.round(secs * SleepUtil.NS_IN_SEC % SleepUtil.NS_IN_MS);
		
		Logger.info(String.format("Seconds = %.9f, millis = %d, nanos = %d",
				Double.valueOf(secs), Long.valueOf(millis), Long.valueOf(nanos)));
		
		secs = 99.099789;
		
		millis = Math.round(secs * SleepUtil.MS_IN_SEC);
		nanos = Math.round(secs * SleepUtil.NS_IN_SEC % SleepUtil.NS_IN_MS);
		
		Logger.info(String.format("Seconds = %.9f, millis = %d, nanos = %d",
				Double.valueOf(secs), Long.valueOf(millis), Long.valueOf(nanos)));
	}
}
