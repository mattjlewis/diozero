package com.diozero.devices;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     LEDTest.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2021 diozero
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.tinylog.Logger;

import com.diozero.api.RuntimeIOException;
import com.diozero.internal.provider.test.TestDeviceFactory;
import com.diozero.internal.provider.test.TestDigitalInputDevice;
import com.diozero.internal.provider.test.TestDigitalOutputDevice;
import com.diozero.internal.spi.NativeDeviceFactoryInterface;
import com.diozero.sbc.DeviceFactoryHelper;

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
