package com.diozero.ws281xj;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - WS281x Java Wrapper
 * Filename:     WS281x.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2017 mattjlewis
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

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * <p>Provides support for
 * <a href="https://learn.adafruit.com/adafruit-neopixel-uberguide">WS2811B /
 * WS2812B aka Adafriut NeoPixel LEDs</a> via a JNI wrapper around the
 * <a href="https://github.com/jgarff/rpi_ws281x">rpi_ws281x C library</a>.</p>
 * <p>Also see <a href="https://github.com/626Pilot/RaspberryPi-NeoPixel-WS2812">this implementation</a>.</p>
 * <p>All colours are represented as 24bit RGB values.</p>
 */
public class WS281x implements Closeable {
	private static final int SIZE_OF_INT = 4;
	private static final int DEFAULT_FREQUENCY = 800_000; // Or 400_000
	// TODO Find out what options there are here... What do pigpio & wiringPi
	// use?
	private static final int DEFAULT_DMA_NUM = 5;

	private static final String LIB_NAME = "ws281xj";
	private static Boolean loaded = Boolean.FALSE;

	private static void init() {
		synchronized (loaded) {
			if (!loaded.booleanValue()) {
				try {
					Path path = Files.createTempFile("lib" + LIB_NAME, ".so");
					path.toFile().deleteOnExit();
					Files.copy(WS281x.class.getResourceAsStream("/lib/lib" + LIB_NAME + ".so"), path,
							StandardCopyOption.REPLACE_EXISTING);
					System.load(path.toString());
					loaded = Boolean.TRUE;
				} catch (IOException e) {
					System.out.println("Error loading library from classpath: " + e);
					e.printStackTrace();

					// Try load the usual way...
					System.loadLibrary(LIB_NAME);
					loaded = Boolean.TRUE;
				}

				Runtime.getRuntime().addShutdownHook(new Thread(WS281xNative::terminate, "WS281x Shutdown Handler"));
			}
		}
	}

	private ByteBuffer ch0LedBuffer;
	private int numPixels;

	/**
	 * @param gpioNum GPIO pin to use to drive the LEDs.
	 * @param brightness Brightness level (0..255).
	 * @param numPixels The number of pixels connected.
	 */
	public WS281x(int gpioNum, int brightness, int numPixels) {
		this(DEFAULT_FREQUENCY, DEFAULT_DMA_NUM, gpioNum, brightness, numPixels);
	}

	/**
	 * @param frequency Communication frequency, 800,000 or 400,000.
	 * @param dmaNum DMA number.
	 * @param gpioNum GPIO pin to use to drive the LEDs.
	 * @param brightness Brightness level (0..255).
	 * @param numPixels The number of pixels connected.
	 */
	public WS281x(int frequency, int dmaNum, int gpioNum, int brightness, int numPixels) {
		init();

		this.numPixels = numPixels;

		ch0LedBuffer = WS281xNative.initialise(frequency, dmaNum, gpioNum, brightness, numPixels);
		System.out.println("order=" + ch0LedBuffer.order());
		ch0LedBuffer.order(ByteOrder.LITTLE_ENDIAN);
	}

	@Override
	public void close() {
		System.out.println("close()");
		allOff();
		ch0LedBuffer = null;
		WS281xNative.terminate();
	}

	public int getNumPixels() {
		return numPixels;
	}

	/**
	 * Push any updated colours to the LED strip.
	 */
	@SuppressWarnings("static-method")
	public void render() {
		int rc = WS281xNative.render();
		if (rc != 0) {
			throw new RuntimeException("Error in render() - " + rc);
		}
	}

	/*
	public void testBufferOutOfBoundsError() {
		int pixel = numPixels + 1;
		try {
			// This should fail with index out of bounds error...
			int colour = ch0LedBuffer.getInt(pixel * SIZE_OF_INT);
			System.out.format("led[%d]=0x%08x%n", Integer.valueOf(pixel), Integer.valueOf(colour));
		} catch (IndexOutOfBoundsException e) {
			System.out.println("Error: " + e);
		}
	}
	*/

	private void validatePixel(int pixel) {
		if (pixel < 0 || pixel >= numPixels) {
			throw new IllegalArgumentException("pixel must be 0.." + (numPixels - 1));
		}
	}

	/**
	 * Turn off all pixels.
	 */
	public void allOff() {
		for (int i = 0; i < numPixels; i++) {
			setPixelColour(i, 0);
		}
		render();
	}

	/**
	 * Get the current colour for the specified pixel.
	 * @param pixel Pixel number.
	 * @return 24-bit RGB colour value.
	 */
	public int getPixelColour(int pixel) {
		validatePixel(pixel);
		return ch0LedBuffer.getInt(pixel * SIZE_OF_INT);
	}

	/**
	 * Set the colour for the specified pixel.
	 * @param pixel Pixel number.
	 * @param colour Colour represented as a 24bit RGB integer (0x0RGB).
	 */
	public void setPixelColour(int pixel, int colour) {
		validatePixel(pixel);
		ch0LedBuffer.putInt(pixel * SIZE_OF_INT, colour);
	}

	/**
	 * Set the colour for the specified pixel using individual red / green / blue 8-bit values.
	 * @param pixel Pixel number.
	 * @param red 8-bit value for the red component.
	 * @param green 8-bit value for the green component.
	 * @param blue 8-bit value for the blue component.
	 */
	public void setPixelColourRGB(int pixel, int red, int green, int blue) {
		validatePixel(pixel);
		ch0LedBuffer.putInt(pixel * SIZE_OF_INT, PixelColour.createColourRGB(red, green, blue));
	}

	/**
	 * Set the colour for the specified pixel using Hue Saturation Brightness (HSB) values.
	 * @param pixel Pixel number.
	 * @param hue Float value in the range 0..1 representing the hue.
	 * @param saturation Float value in the range 0..1 representing the colour saturation.
	 * @param brightness Float value in the range 0..1 representing the colour brightness.
	 */
	public void setPixelColourHSB(int pixel, float hue, float saturation, float brightness) {
		validatePixel(pixel);
		ch0LedBuffer.putInt(pixel * SIZE_OF_INT, PixelColour.createColourHSB(hue, saturation, brightness));
	}

	/**
	 * <p>Set the colour for the specified pixel using Hue Saturation Luminance (HSL) values.</p>
	 * <p>HSL colour mapping code taken from <a href="https://tips4java.wordpress.com/2009/07/05/hsl-color/">this HSL Color class by Rob Camick</a>.</p>
	 * @param pixel Pixel number.
	 * @param hue Represents the colour (think colours of the rainbow), specified in degrees from 0 - 360. Red is 0, green is 120 and blue is 240.
	 * @param saturation Represents the purity of the colour. Range is 0..1 with 1 fully saturated and 0 gray.
	 * @param luminance Represents the brightness of the colour. Range is 0..1 with 1 white 0 black.
	 */
	public void setPixelColourHSL(int pixel, float hue, float saturation, float luminance) {
		validatePixel(pixel);
		ch0LedBuffer.putInt(pixel * SIZE_OF_INT, PixelColour.createColourHSL(hue, saturation, luminance));
	}

	/**
	 * Get the 8-bit red component value for the specified pixel.
	 * @param pixel Pixel number.
	 * @return 8-bit red component value.
	 */
	public int getRedComponent(int pixel) {
		validatePixel(pixel);
		return PixelColour.getRedComponent(ch0LedBuffer.getInt(pixel * SIZE_OF_INT));
	}

	/**
	 * Set the 8-bit red component value for the specified pixel.
	 * @param pixel Pixel number.
	 * @param red 8-bit red component value.
	 */
	public void setRedComponent(int pixel, int red) {
		validatePixel(pixel);
		int index = pixel * SIZE_OF_INT;
		ch0LedBuffer.putInt(index, PixelColour.setRedComponent(ch0LedBuffer.getInt(index), red));
	}

	/**
	 * Get the 8-bit green component value for the specified pixel.
	 * @param pixel Pixel number.
	 * @return 8-bit green component value.
	 */
	public int getGreenComponent(int pixel) {
		validatePixel(pixel);
		return PixelColour.getGreenComponet(ch0LedBuffer.getInt(pixel * SIZE_OF_INT));
	}

	/**
	 * Set the 8-bit green component value for the specified pixel.
	 * @param pixel Pixel number.
	 * @param green 8-bit green component value.
	 */
	public void setGreenComponent(int pixel, int green) {
		validatePixel(pixel);
		int index = pixel * SIZE_OF_INT;
		ch0LedBuffer.putInt(index, PixelColour.setGreenComponent(ch0LedBuffer.getInt(index), green));
	}

	/**
	 * Get the 8-bit blue component value for the specified pixel.
	 * @param pixel Pixel number.
	 * @return 8-bit blue component value.
	 */
	public int getBlueComponent(int pixel) {
		validatePixel(pixel);
		return PixelColour.getBlueComponent(ch0LedBuffer.getInt(pixel * SIZE_OF_INT));
	}

	/**
	 * Set the 8-bit blue component value for the specified pixel.
	 * @param pixel Pixel number.
	 * @param blue 8-bit blue component value.
	 */
	public void setBlueComponent(int pixel, int blue) {
		validatePixel(pixel);
		int index = pixel * SIZE_OF_INT;
		ch0LedBuffer.putInt(index, PixelColour.setBlueComponent(ch0LedBuffer.getInt(index), blue));
	}
}
