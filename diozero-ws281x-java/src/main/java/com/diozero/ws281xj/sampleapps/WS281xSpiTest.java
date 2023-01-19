package com.diozero.ws281xj.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - WS281x Java Wrapper
 * Filename:     WS281xSpiTest.java
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

import org.tinylog.Logger;

import com.diozero.util.SleepUtil;
import com.diozero.ws281xj.LedDriverInterface;
import com.diozero.ws281xj.PixelAnimations;
import com.diozero.ws281xj.StripType;
import com.diozero.ws281xj.spi.WS281xSpi;

public class WS281xSpiTest {
	public static void main(String[] args) {
		StripType strip_type = StripType.WS2812;

		int pixels = 60;
		if (args.length > 0) {
			pixels = Integer.parseInt(args[0]);
		}
		int brightness = 127;

		try (LedDriverInterface led_driver = new WS281xSpi(2, 0, strip_type, pixels, brightness)) {
			Logger.debug("All off");
			led_driver.allOff();
			SleepUtil.sleepMillis(500);

			for (int i = 0; i < 5; i++) {
				Logger.debug("Incremental red");
				int red = 0;
				for (int pixel = 0; pixel < pixels; pixel++) {
					led_driver.setPixelColourRGB(pixel, red, 0, 0);
					red += 255 / pixels;
				}
				led_driver.render();
				SleepUtil.sleepMillis(500);

				Logger.debug("Incremental green");
				int green = 0;
				for (int pixel = 0; pixel < pixels; pixel++) {
					led_driver.setPixelColourRGB(pixel, 0, green, 0);
					green += 255 / pixels;
				}
				led_driver.render();
				SleepUtil.sleepMillis(500);

				Logger.debug("Incremental blue");
				int blue = 0;
				for (int pixel = 0; pixel < pixels; pixel++) {
					led_driver.setPixelColourRGB(pixel, 0, 0, blue);
					blue += 255 / pixels;
				}
				led_driver.render();
				SleepUtil.sleepMillis(500);
			}

			Logger.debug("All off");
			led_driver.allOff();
			led_driver.render();
			SleepUtil.sleepMillis(500);

			PixelAnimations.demo(led_driver);
		}
	}
}
