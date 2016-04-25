package com.diozero;

import org.junit.Test;
import org.pmw.tinylog.Logger;

import com.diozero.util.SleepUtil;

@SuppressWarnings("static-method")
public class MilliNanoTest {
	@Test
	public void test() {
		double secs = 0.005123;
		
		int millis = (int) (secs * SleepUtil.MS_IN_SEC);
		int nanos = (int) (secs * SleepUtil.NS_IN_SEC % SleepUtil.NS_IN_MS);
		
		Logger.info(String.format("Seconds = %.9f, millis = %d, nanos = %d",
				Double.valueOf(secs), Integer.valueOf(millis), Integer.valueOf(nanos)));
		
		secs = 0.099789;
		
		millis = (int) (secs * SleepUtil.MS_IN_SEC);
		nanos = (int) (secs * SleepUtil.NS_IN_SEC % SleepUtil.NS_IN_MS);
		
		Logger.info(String.format("Seconds = %.9f, millis = %d, nanos = %d",
				Double.valueOf(secs), Integer.valueOf(millis), Integer.valueOf(nanos)));
		
		secs = 99.099789;
		
		millis = (int) (secs * SleepUtil.MS_IN_SEC);
		nanos = (int) (secs * SleepUtil.NS_IN_SEC % SleepUtil.NS_IN_MS);
		
		Logger.info(String.format("Seconds = %.9f, millis = %d, nanos = %d",
				Double.valueOf(secs), Integer.valueOf(millis), Integer.valueOf(nanos)));
	}
}
