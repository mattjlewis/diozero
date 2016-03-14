package com.diozero;

import org.junit.Before;
import org.junit.Test;
import org.pmw.tinylog.Logger;

import com.diozero.internal.provider.test.HCSR04EchoPin;
import com.diozero.internal.provider.test.HCSR04TriggerPin;
import com.diozero.internal.provider.test.TestDeviceFactory;
import com.diozero.util.DioZeroScheduler;
import com.diozero.util.SleepUtil;

@SuppressWarnings("static-method")
public class HCSR04Test {
	@Before
	public void setup() {
		TestDeviceFactory.setDigitalInputDeviceClass(HCSR04EchoPin.class);
		TestDeviceFactory.setDigitalOutputDeviceClass(HCSR04TriggerPin.class);
	}
	
	@Test
	public void test() {
		// Purely to prime the scheduler
		DioZeroScheduler.getDaemonInstance().execute(() -> Logger.info(""));
		try (HCSR04 hcsr04 = new HCSR04(26, 4)) {
			for (int i=0; i<10; i++) {
				float distance = hcsr04.getDistanceCm();
				Logger.info("Distance={}", Float.valueOf(distance));
				SleepUtil.sleepSeconds(1);
			}
		}
	}
}
