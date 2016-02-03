package com.diozero;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;

import com.diozero.LED;
import com.diozero.api.DeviceFactoryHelper;
import com.diozero.internal.spi.NativeDeviceFactoryInterface;

/**
 * LED test case using the test device factory
 */
public class LEDTest {
	private static final Logger logger = LogManager.getLogger(LEDTest.class);
	
	@SuppressWarnings("static-method")
	@Test
	public void test() {
		NativeDeviceFactoryInterface df = DeviceFactoryHelper.getNativeDeviceFactory();
		
		int pin = 1;
		try (LED led = new LED(pin)) {
			// TODO Clean-up required, it is a bit ugly to have to know the DeviceStates key structure...
			Assert.assertTrue("Pin (" + pin + ") is opened", df.isDeviceOpened("Native-GPIO-" + pin));
			
			led.on();
			Assert.assertTrue("Pin (" + pin + ") is on", led.isOn());
			led.off();
			Assert.assertFalse("Pin (" + pin + ") is off", led.isOn());
			led.toggle();
			Assert.assertTrue("Pin (" + pin + ") is on", led.isOn());
			led.toggle();
			Assert.assertFalse("Pin (" + pin + ") is off", led.isOn());
			led.blink(0.1f, 0.1f, 5, false);
			Assert.assertFalse("Pin (" + pin + ") is off", led.isOn());
		} catch (IOException e) {
			logger.error("Error: " + e, e);
		}
		
		// TODO Clean-up required, it is a bit ugly to have to know the DeviceStates key structure...
		Assert.assertFalse("Pin (" + pin + ") is closed", df.isDeviceOpened("Native-GPIO-" + pin));
	}
}
