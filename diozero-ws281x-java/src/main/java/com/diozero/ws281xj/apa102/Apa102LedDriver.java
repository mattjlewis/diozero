package com.diozero.ws281xj.apa102;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - WS281x Java Wrapper
 * Filename:     Apa102LedDriver.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
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

import java.util.Arrays;

import com.diozero.api.SpiDevice;
import com.diozero.ws281xj.LedDriverInterface;

/**
 * Reference: https://cpldcpu.com/2014/11/30/understanding-the-apa102-superled/
 * Also: https://github.com/androidthings/contrib-drivers/blob/master/apa102/src/main/java/com/google/android/things/contrib/driver/apa102/Apa102.java
 */
public class Apa102LedDriver implements LedDriverInterface {
	private SpiDevice device;
	private int brightness;
	private int[] leds;
	private byte[] spiBuffer;
	
	public Apa102LedDriver(int controller, int chipSelect, int frequency, int numLeds, int brightness) {
		device = SpiDevice.builder(chipSelect).setController(controller).setFrequency(frequency).build();
		
		this.brightness = brightness & 0x1F;
		
		leds = new int[numLeds];
		// A start frame of 32 zero bits (<0x00> <0x00> <0x00> <0x00>)
		// A 32 bit LED frame for each LED in the string (<0xE0+brightness> <blue> <green> <red>)
		// An end frame consisting of at least (n/2) bits of 1, where n is the number of LEDs in the string.
		// 1-64 LEDs => 32bits
		// 65-128 LEDs => 
		spiBuffer = new byte[4 + 4*numLeds + ((numLeds+63)/64)*4];
	}

	@Override
	public void close() {
		allOff();
		device.close();
	}

	@Override
	public int getNumPixels() {
		return leds.length;
	}

	@Override
	public void render() {
		device.write(spiBuffer);
	}

	@Override
	public void allOff() {
		Arrays.fill(leds, 0);
		Arrays.fill(spiBuffer, (byte) 0);
	}

	@Override
	public int getPixelColour(int pixel) {
		return leds[pixel];
	}

	@Override
	public void setPixelColour(int pixel, int colour) {
		leds[pixel] = colour;
		int spi_pixel_index = 4*(1+pixel);
		// TODO Validate this order
		// A 32 bit LED frame for each LED in the string (<0xE0+brightness> <blue> <green> <red>)
		spiBuffer[spi_pixel_index] = (byte) (0xe0 | brightness);
		// FIXME The calls to get<Colour>Component isn't optimal!
		spiBuffer[spi_pixel_index+1] = (byte) getBlueComponent(pixel);
		spiBuffer[spi_pixel_index+2] = (byte) getGreenComponent(pixel);
		spiBuffer[spi_pixel_index+3] = (byte) getRedComponent(pixel);
	}
}
