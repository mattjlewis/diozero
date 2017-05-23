package com.diozero;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 diozero
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.pmw.tinylog.Logger;

import com.diozero.internal.DeviceFactoryHelper;
import com.diozero.internal.provider.NativeDeviceFactoryInterface;
import com.diozero.internal.provider.test.TestDeviceFactory;
import com.diozero.internal.provider.test.TestDigitalInputDevice;
import com.diozero.internal.provider.test.TestDigitalOutputDevice;
import com.diozero.util.RuntimeIOException;

/**
 * LED test case using the test device factory
 */
@SuppressWarnings("static-method")
public class LEDTest {
	@Before
	public void setup() {
		TestDeviceFactory.setDigitalInputDeviceClass(TestDigitalInputDevice.class);
		TestDeviceFactory.setDigitalOutputDeviceClass(TestDigitalOutputDevice.class);
	}
	
	@Test
	public void test() {
		try (NativeDeviceFactoryInterface df = DeviceFactoryHelper.getNativeDeviceFactory()) {
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
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
			}
			
			// TODO Clean-up required, it is a bit ugly to have to know the DeviceStates key structure...
			Assert.assertFalse("Pin (" + pin + ") is closed", df.isDeviceOpened("Native-GPIO-" + pin));
		}
	}
}
