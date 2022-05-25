package com.diozero.ws281xj;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - WS281x Java Wrapper
 * Filename:     StripType.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2022 diozero
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

/**
 * <p>Summaries of different LED strip types:</p>
 * <ul>
 *  <li><a href="https://github.com/ManiacalLabs/AllPixel/wiki/Connecting-the-LEDs">Maniacal Labs Connecting the LEDs</a></li>
 *  <li><a href="https://github.com/FastLED/FastLED/wiki/Chipset-reference">FastLED Chipset Reference</a></li>
 * </ul>
 * <p>Known to work:</p>
 * <ul>
 *  <li><a href="https://learn.adafruit.com/adafruit-neopixel-uberguide/the-magic-of-neopixels">WS2812 (NeoPixel)</a></li>
 *  <li><a href="https://github.com/adafruit/Adafruit_DotStar_Pi">DotStar (APA102)</a></li>
 * </ul>
 * <p>Others:</p>
 * <ul>
 *  <li><a href="https://www.linuxpinguin.de/2013/12/howto-arduino-micro-aldi-led-strip-based-on-tmi1829/">TM1829</a></li>
 * </ul>
 */
public enum StripType {
	// WS2801
	// WS2801 LED Stripe    Raspberry Pi    (Switching-) Power Supply
	// 5V                   —               +V
	// CK / CI              Pin 23 (SCKL)   -
	// SI / DI              Pin 19 (MOSI)   -
	// GND                  Pin 6 (GND)     -V or COM

	// 4 colour R, G, B and W ordering
	SK6812_RGBW(3, 2, 1, 0),
	SK6812_RBGW(3, 2, 0, 1),
	SK6812_GRBW(3, 1, 2, 0),
	SK6812_GBRW(3, 1, 0, 2),
	SK6812_BRGW(3, 0, 2, 1),
	SK6812_BGRW(3, 0, 1, 2),

	// 3 colour R, G and B ordering
	WS2811_RGB(2, 1, 0),
	WS2811_RBG(2, 0, 1),
	WS2811_GRB(1, 2, 0),
	WS2811_GBR(1, 0, 2),
	WS2811_BRG(0, 2, 1),
	WS2811_BGR(0, 1, 2);

	private int whiteShift;
	private int redShift;
	private int greenShift;
	private int blueShift;
	
	private StripType(int redPos, int greenPos, int bluePos) {
		this(-1, redPos, greenPos, bluePos);
	}
	
	private StripType(int whitePos, int redPos, int greenPos, int bluePos) {
		if (whitePos != -1) {
			whiteShift = whitePos * 8;
		}
		redShift = redPos * 8;
		greenShift = greenPos * 8;
		blueShift = bluePos * 8;
	}
	
	public int getWhiteShift() {
		return whiteShift;
	}

	public int getRedShift() {
		return redShift;
	}

	public int getGreenShift() {
		return greenShift;
	}

	public int getBlueShift() {
		return blueShift;
	}

	public int getColourCount() {
		return whiteShift == 0 ? 3 : 4; // RGB or RGBW
	}

	// Predefined fixed LED types
	public static final StripType WS2812 = WS2811_GRB;
	public static final StripType SK6812 = WS2811_GRB;
	public static final StripType SK6812W = SK6812_GRBW;
}
