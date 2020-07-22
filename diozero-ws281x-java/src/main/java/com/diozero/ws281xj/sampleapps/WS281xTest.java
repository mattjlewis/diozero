package com.diozero.ws281xj.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - WS281x Java Wrapper
 * Filename:     WS281xTest.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
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

import com.diozero.ws281xj.LedDriverInterface;
import com.diozero.ws281xj.PixelAnimations;
import com.diozero.ws281xj.PixelColour;
import com.diozero.ws281xj.rpiws281x.WS281x;

public class WS281xTest {
	public static void main(String[] args) {
		//int gpio_num = 18;
		int gpio_num = 10;
		int brightness = 64;	// 0..255
		//int num_pixels = 12;
		int num_pixels = 60;
		
		System.out.println("Using GPIO " + gpio_num);
		
		try (LedDriverInterface led_driver = new WS281x(gpio_num, brightness, num_pixels)) {
			rainbowColours(led_driver);
			test2(led_driver);
			hsbTest(led_driver);
			hslTest(led_driver);

			PixelAnimations.demo(led_driver);
		} catch (Throwable t) {
			System.out.println("Error: " + t);
			t.printStackTrace();
		}
	}
	
	private static void rainbowColours(LedDriverInterface ledDriver) {
		System.out.println("rainbowColours()");
		
		int[] colours = PixelColour.RAINBOW;
		
		for (int i=0; i<250; i++) {
			for (int pixel=0; pixel<ledDriver.getNumPixels(); pixel++) {
				ledDriver.setPixelColour(pixel, colours[(i+pixel) % colours.length]);
			}
			
			ledDriver.render();
			PixelAnimations.delay(50);
		}
	}
	
	private static void test2(LedDriverInterface ledDriver) {
		System.out.println("test2()");
		
		// Set all off
		ledDriver.allOff();
		
		int delay = 20;

		// Gradually add red
		System.out.println("Adding red...");
		for (int i=0; i<256; i+=2) {
			for (int pixel=0; pixel<ledDriver.getNumPixels(); pixel++) {
				ledDriver.setRedComponent(pixel, i);
			}
			
			ledDriver.render();
			PixelAnimations.delay(delay);
		}

		// Gradually add green
		System.out.println("Adding green...");
		for (int i=0; i<256; i+=2) {
			for (int pixel=0; pixel<ledDriver.getNumPixels(); pixel++) {
				ledDriver.setGreenComponent(pixel, i);
			}
			
			ledDriver.render();
			PixelAnimations.delay(delay);
		}

		// Gradually add blue
		System.out.println("Adding blue...");
		for (int i=0; i<256; i+=2) {
			for (int pixel=0; pixel<ledDriver.getNumPixels(); pixel++) {
				ledDriver.setBlueComponent(pixel, i);
			}
			
			ledDriver.render();
			PixelAnimations.delay(delay);
		}
		
		// Set all off
		ledDriver.allOff();
	}
	
	private static void hsbTest(LedDriverInterface ledDriver) {
		System.out.println("hsbTest()");
		float brightness = 0.5f;
		
		for (float hue=0; hue<1; hue+=0.05f) {
			for (float saturation=0; saturation<=1; saturation+=0.05f) {
				for (int pixel=0; pixel<ledDriver.getNumPixels(); pixel++) {
					ledDriver.setPixelColourHSB(pixel, hue, saturation, brightness);
				}
				ledDriver.render();
				PixelAnimations.delay(20);
			}
		}
	}
	
	private static void hslTest(LedDriverInterface ledDriver) {
		System.out.println("hslTest()");
		float luminance = 0.5f;
		
		for (float hue=0; hue<360; hue+=(360/20)) {
			for (float saturation=0; saturation<=1; saturation+=0.05f) {
				for (int pixel=0; pixel<ledDriver.getNumPixels(); pixel++) {
					ledDriver.setPixelColourHSL(pixel, hue, saturation, luminance);
				}
				ledDriver.render();
				PixelAnimations.delay(20);
			}
		}
	}
}
