package com.diozero.api;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     PwmOutputDeviceTest.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2023 diozero
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

import java.util.EnumSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.diozero.sbc.BoardInfo;
import com.diozero.sbc.DeviceFactoryHelper;

@SuppressWarnings("static-method")
public class PwmOutputDeviceTest {
	private static final int DIGITAL_GPIO = 10;
	private static final int PWM_GPIO = 12;

	@BeforeAll
	public static void beforeAll() {
		BoardInfo board_info = DeviceFactoryHelper.getNativeDeviceFactory().getBoardInfo();

		board_info.addGpioPinInfo(DIGITAL_GPIO, DIGITAL_GPIO, EnumSet.of(DeviceMode.DIGITAL_OUTPUT));
		board_info.addGpioPinInfo(PWM_GPIO, PWM_GPIO, EnumSet.of(DeviceMode.PWM_OUTPUT));
	}

	@Test
	public void testPwm() {
		try (PwmOutputDevice device = PwmOutputDevice.Builder.builder(PWM_GPIO).build()) {
			device.setValue(0);
			Assertions.assertEquals(0, device.getValue());

			device.setValue(1);
			Assertions.assertEquals(1, device.getValue());
		}
	}

	@Test
	public void testSoftwarePwm() {
		try (PwmOutputDevice device = PwmOutputDevice.Builder.builder(DIGITAL_GPIO).build()) {
			device.setValue(0);
			Assertions.assertEquals(0, device.getValue());

			device.setValue(1);
			Assertions.assertEquals(1, device.getValue());
		}
	}
}
