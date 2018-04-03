package com.diozero.ws281xj.rpiws281x;

/*-
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.diozero.ws281xj.LedDriverInterface;
import com.diozero.ws281xj.StripType;

/**
 * <p>Provides support for
 * <a href="https://learn.adafruit.com/adafruit-neopixel-uberguide">WS2811B /
 * WS2812B aka Adafriut NeoPixel LEDs</a> via a JNI wrapper around the
 * <a href="https://github.com/jgarff/rpi_ws281x">rpi_ws281x C library</a>.</p>
 * <p>Also see <a href="https://github.com/626Pilot/RaspberryPi-NeoPixel-WS2812">this implementation</a>.</p>
 * <p>All colours are represented as 24bit RGB values.</p>
 * <p>Valid GPIO numbers:</p>
 * <ul>
 *  <li>12 / 18 (PWM)</li>
 *  <li>21 / 31 (PCM)</li>
 *  <li>10 (SPI)</li>
 * </ul>
 */
public class WS281x implements LedDriverInterface {
	private static final int SIZE_OF_INT = 4;
	private static final int DEFAULT_FREQUENCY = 800_000; // Or 400_000
	// TODO Find out what options there are here... What do pigpio & wiringPi use?
	private static final int DEFAULT_DMA_NUM = 5;
	private static final StripType DEFAULT_STRIP_TYPE = StripType.WS2812;
	private static final int DEFAULT_CHANNEL = 0;

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
		this(DEFAULT_FREQUENCY, DEFAULT_DMA_NUM, gpioNum, brightness, numPixels, DEFAULT_STRIP_TYPE, DEFAULT_CHANNEL);
	}

	/**
	 * @param frequency Communication frequency, 800,000 or 400,000.
	 * @param dmaNum DMA number.
	 * @param gpioNum GPIO pin to use to drive the LEDs.
	 * @param brightness Brightness level (0..255).
	 * @param numPixels The number of pixels connected.
	 * @param stripType Strip type
	 */
	public WS281x(int frequency, int dmaNum, int gpioNum, int brightness, int numPixels, StripType stripType) {
		this(DEFAULT_FREQUENCY, DEFAULT_DMA_NUM, gpioNum, brightness, numPixels, stripType, DEFAULT_CHANNEL);
	}

	/**
	 * @param frequency Communication frequency, 800,000 or 400,000.
	 * @param dmaNum DMA number.
	 * @param gpioNum GPIO pin to use to drive the LEDs.
	 * @param brightness Brightness level (0..255).
	 * @param numPixels The number of pixels connected.
	 * @param stripType Strip type
	 * @param channel PWM channel
	 */
	public WS281x(int frequency, int dmaNum, int gpioNum, int brightness, int numPixels, StripType stripType, int channel) {
		init();

		this.numPixels = numPixels;

		ch0LedBuffer = WS281xNative.initialise(frequency, dmaNum, gpioNum, brightness, numPixels,
				getRpiStripType(stripType), channel);
		if (ch0LedBuffer == null) {
			throw new RuntimeException("Error initialising the WS281x strip");
		}
		System.out.println("order=" + ch0LedBuffer.order());
		ch0LedBuffer.order(ByteOrder.LITTLE_ENDIAN);
	}

	@Override
	public void close() {
		System.out.println("close()");
		if (ch0LedBuffer != null) {
			allOff();
			ch0LedBuffer = null;
		}
		WS281xNative.terminate();
	}

	@Override
	public int getNumPixels() {
		return numPixels;
	}

	@Override
	public void render() {
		int rc = WS281xNative.render();
		if (rc != 0) {
			throw new RuntimeException("Error in render() - " + rc);
		}
	}

	private void validatePixel(int pixel) {
		if (pixel < 0 || pixel >= numPixels) {
			throw new IllegalArgumentException("pixel must be 0.." + (numPixels - 1));
		}
	}

	@Override
	public void allOff() {
		for (int i = 0; i < numPixels; i++) {
			setPixelColour(i, 0);
		}
		render();
	}

	@Override
	public int getPixelColour(int pixel) {
		validatePixel(pixel);
		return ch0LedBuffer.getInt(pixel * SIZE_OF_INT);
	}

	@Override
	public void setPixelColour(int pixel, int colour) {
		validatePixel(pixel);
		ch0LedBuffer.putInt(pixel * SIZE_OF_INT, colour);
	}

	private static int getRpiStripType(StripType stripType) {
		switch (stripType) {
		case SK6812_RGBW:
			return 0x18100800;
		case SK6812_RBGW:
			return 0x18100008;
		case SK6812_GRBW:
			return 0x18081000;
		case SK6812_GBRW:
			return 0x18080010;
		case SK6812_BRGW:
			return 0x18001008;
		case SK6812_BGRW:
			return 0x18000810;
	
		case WS2811_RGB:
			return 0x00100800;
		case WS2811_RBG:
			return 0x00100008;
		case WS2811_GRB:
			return 0x00081000;
		case WS2811_GBR:
			return 0x00080010;
		case WS2811_BRG:
			return 0x00001008;
		case WS2811_BGR:
			return 0x00000810;
		default:
			return 0x00081000;
		}
	}
}
