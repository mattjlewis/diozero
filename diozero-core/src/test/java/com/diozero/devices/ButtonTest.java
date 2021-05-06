package com.diozero.devices;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     ButtonTest.java
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

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.tinylog.Logger;

import com.diozero.api.DigitalInputEvent;
import com.diozero.api.GpioPullUpDown;
import com.diozero.internal.provider.test.TestDeviceFactory;
import com.diozero.internal.provider.test.TestDigitalInputDevice;
import com.diozero.internal.provider.test.TestDigitalOutputDevice;
import com.diozero.util.SleepUtil;

public class ButtonTest {
	private int i;

	@BeforeAll
	public static void beforeAll() {
		TestDeviceFactory.setDigitalInputDeviceClass(TestDigitalInputDevice.class);
		TestDeviceFactory.setDigitalOutputDeviceClass(TestDigitalOutputDevice.class);
	}

	@Test
	public void test() {
		try (Button button = new Button(1, GpioPullUpDown.PULL_UP)) {
			button.whenPressed(nanoTime -> Logger.info("Pressed"));
			button.whenReleased(nanoTime -> Logger.info("Released"));
			button.addListener(event -> Logger.info("Event: {}", event));

			ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
			ScheduledFuture<?> future = executor.scheduleAtFixedRate(
					() -> button.accept(new DigitalInputEvent(button.getGpio(), System.currentTimeMillis(),
							System.nanoTime(), (i++ % 2) == 0)),
					500, 500, TimeUnit.MILLISECONDS);

			SleepUtil.sleepSeconds(5);

			future.cancel(true);
			executor.shutdownNow();
		}
	}
}
