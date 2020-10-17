package com.diozero;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.tinylog.Logger;

import com.diozero.devices.LED;
import com.diozero.internal.provider.NativeDeviceFactoryInterface;
import com.diozero.internal.provider.test.TestDeviceFactory;
import com.diozero.internal.provider.test.TestDigitalInputDevice;
import com.diozero.internal.provider.test.TestDigitalOutputDevice;
import com.diozero.util.DeviceFactoryHelper;
import com.diozero.util.RuntimeIOException;

/**
 * LED test case using the test device factory
 */
@SuppressWarnings("static-method")
public class LEDTest {
	@BeforeAll
	public static void beforeAll() {
		TestDeviceFactory.setDigitalInputDeviceClass(TestDigitalInputDevice.class);
		TestDeviceFactory.setDigitalOutputDeviceClass(TestDigitalOutputDevice.class);
	}
	
	@Test
	public void test() {
		try (NativeDeviceFactoryInterface df = DeviceFactoryHelper.getNativeDeviceFactory()) {
			int pin = 1;
			try (LED led = new LED(pin)) {
				// TODO Clean-up required, it is a bit ugly to have to know the DeviceStates key structure...
				Assertions.assertTrue(df.isDeviceOpened("Native-GPIO-" + pin), "Pin (" + pin + ") is opened");
				
				led.on();
				Assertions.assertTrue(led.isOn(), "Pin (" + pin + ") is on");
				led.off();
				Assertions.assertFalse(led.isOn(), "Pin (" + pin + ") is off");
				led.toggle();
				Assertions.assertTrue(led.isOn(), "Pin (" + pin + ") is on");
				led.toggle();
				Assertions.assertFalse(led.isOn(), "Pin (" + pin + ") is off");
				led.blink(0.1f, 0.1f, 5, false);
				Assertions.assertFalse(led.isOn(), "Pin (" + pin + ") is off");
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
			}
			
			// TODO Clean-up required, it is a bit ugly to have to know the DeviceStates key structure...
			Assertions.assertFalse(df.isDeviceOpened("Native-GPIO-" + pin), "Pin (" + pin + ") is closed");
		}
	}
}
