package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     DigitalInputTest.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2024 diozero
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

import java.util.function.LongConsumer;

import org.tinylog.Logger;

import com.diozero.api.DigitalInputDevice;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.util.SleepUtil;

public class DigitalInputTest {
	public static void main(String[] args) {
		int delay_sec = 10;

		LongConsumer activated = new LongConsumer() {
			@Override
			public void accept(long timestampNs) {
				System.out.println("hit " + timestampNs + ", delta = " + (System.nanoTime() - timestampNs));
			}
		};

		LongConsumer deactivated = new LongConsumer() {
			@Override
			public void accept(long timestampNs) {
				System.out.println("away " + timestampNs + ", delta = " + (System.nanoTime() - timestampNs));
			}
		};

		System.out.println("Falling only...");
		try (DigitalInputDevice d = DigitalInputDevice.Builder.builder(5).setActiveHigh(false)
				.setPullUpDown(GpioPullUpDown.NONE).setTrigger(GpioEventTrigger.FALLING).build()) {
			d.whenActivated(activated);
			d.whenDeactivated(deactivated);

			System.out.println("Sleeping for " + delay_sec + " seconds...");
			SleepUtil.sleepSeconds(delay_sec);
			System.out.println("Done.");

			SleepUtil.sleepSeconds(1);
			System.out.println("Waiting for button to be pressed...");
			d.waitForActive();
		} catch (InterruptedException e) {
			Logger.error(e, "Interrupted: {}", e);
		}

		System.out.println("Falling and rising...");
		try (DigitalInputDevice d = DigitalInputDevice.Builder.builder(5).setActiveHigh(false)
				.setPullUpDown(GpioPullUpDown.NONE).setTrigger(GpioEventTrigger.BOTH).build()) {
			d.whenActivated(activated);
			d.whenDeactivated(deactivated);

			System.out.println("Sleeping for " + delay_sec + " seconds...");
			SleepUtil.sleepSeconds(delay_sec);
			System.out.println("Done.");

			SleepUtil.sleepSeconds(1);
			System.out.println("Waiting for button to be pressed...");
			d.waitForActive();
		} catch (InterruptedException e) {
			Logger.error(e, "Interrupted: {}", e);
		}
	}

	/**
	 * Simply capturing the various combinations of digital input configurations
	 */
	public static void combinations() {
		// NO switch with internal pull-up using GPIO #18
		try (DigitalInputDevice did1 = DigitalInputDevice.Builder.builder(18).setPullUpDown(GpioPullUpDown.PULL_UP)
				.build()) {
			// Do stuff, noting that active-high will have been defaulted to
			// false as diozero assumes the switch is wired NO
		}

		// NC switch with internal pull-up using GPIO #18
		try (DigitalInputDevice did1 = DigitalInputDevice.Builder.builder(18).setPullUpDown(GpioPullUpDown.PULL_UP)
				.setActiveHigh(true).build()) {
			// Do stuff, noting that active-high has been overridden to true
			// as the application developer knows that the switch is wired NC
		}

		// NO switch with internal pull-down using GPIO #18
		try (DigitalInputDevice did1 = DigitalInputDevice.Builder.builder(18).setPullUpDown(GpioPullUpDown.PULL_DOWN)
				.build()) {
			// Do stuff, noting that active-high will have been defaulted to
			// true as diozero assumes the switch is wired NO
		}

		// NC switch with internal pull-down using GPIO #18
		try (DigitalInputDevice did1 = DigitalInputDevice.Builder.builder(18).setPullUpDown(GpioPullUpDown.PULL_DOWN)
				.setActiveHigh(false).build()) {
			// Do stuff, noting that active-high has been overridden to false
			// as the application developer knows that the switch is wired NC
		}

		// NO switch with external pull-up using GPIO #18
		try (DigitalInputDevice did1 = DigitalInputDevice.Builder.builder(18).setPullUpDown(GpioPullUpDown.NONE)
				.setActiveHigh(false).build()) {
			// Do stuff, noting that active-high has been overridden to false as the
			// application developer knows that the switch is wired NO with an external
			// pull-up
		}

		// NC switch with external pull-up using GPIO #18
		try (DigitalInputDevice did1 = DigitalInputDevice.Builder.builder(18).setPullUpDown(GpioPullUpDown.NONE)
				.setActiveHigh(true).build()) {
			// Do stuff, noting that active-high has been overridden to true as the
			// application developer knows that the switch is wired NC with an external
			// pull-up
		}
	}
}
