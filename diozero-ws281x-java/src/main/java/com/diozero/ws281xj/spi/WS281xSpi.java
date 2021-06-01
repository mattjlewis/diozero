package com.diozero.ws281xj.spi;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - WS281x Java Wrapper
 * Filename:     WS281xSpi.java
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

import java.util.Arrays;

import org.tinylog.Logger;

import com.diozero.api.SpiDevice;
import com.diozero.util.SleepUtil;
import com.diozero.ws281xj.LedDriverInterface;
import com.diozero.ws281xj.StripType;

/**
 * References:
 * https://os.mbed.com/users/JacobBramley/code/PixelArray/file/47802e75974e/neopixel.cpp
 * https://github.com/jgarff/rpi_ws281x/blob/master/ws2811.c
 */
public class WS281xSpi implements LedDriverInterface {
	private static final int LED_COLOURS = 4;
	private static final int LED_RESET_uS = 55;
	/* Minimum time to wait for reset to occur in nanoseconds. */
	private static final int LED_RESET_WAIT_TIME = 300_000;

	// Symbol definitions
	private static final byte SYMBOL_HIGH = 0b110;
	private static final byte SYMBOL_LOW = 0b100;
	// Symbol definitions for software inversion (PCM and SPI only)
	private static final byte SYMBOL_HIGH_INV = 0b001;
	private static final byte SYMBOL_LOW_INV = 0b011;

	/*
	 * 4 colours (R, G, B + W), 8 bits per byte, 3 symbols per bit + 55uS low for
	 * reset signal
	 */

	private SpiDevice device;
	private Protocol protocol;
	private StripType stripType;
	private int numLeds;
	private long lastRenderTime;
	private int renderWaitTime;
	// Brightness value between 0 and 255
	private int brightness;
	private int[] leds;
	private byte[] gamma;
	private byte[] pixelRaw;

	public WS281xSpi(int controller, int chipSelect, StripType stripType, int numLeds, int brightness) {
		this(controller, chipSelect, Protocol.PROTOCOL_800KHZ, stripType, numLeds, brightness);
	}

	public WS281xSpi(int controller, int chipSelect, Protocol protocol, StripType stripType, int numLeds,
			int brightness) {
		device = SpiDevice.builder(chipSelect).setController(controller).setFrequency(protocol.getFrequency() * 3)
				.build();

		this.protocol = protocol;
		this.stripType = stripType;
		this.numLeds = numLeds;
		this.brightness = brightness & 0xff;
		leds = new int[numLeds];
		gamma = new byte[256];

		// Set default uncorrected gamma table
		for (int x = 0; x < 256; x++) {
			gamma[x] = (byte) x;
		}

		// Allocate SPI transmit buffer (same size as PCM)
		pixelRaw = new byte[PCM_BYTE_COUNT(numLeds, protocol.getFrequency())];

		// 1.25us per bit (1250ns)
		renderWaitTime = numLeds * stripType.getColourCount() * 8 * 1250 + LED_RESET_WAIT_TIME;
	}

	@Override
	public void close() {
		Logger.trace("Turning all off before close");
		allOff();
		device.close();
	}

	@Override
	public int getNumPixels() {
		return numLeds;
	}

	@Override
	public int getPixelColour(int pixel) {
		return leds[pixel];
	}

	@Override
	public void setPixelColour(int pixel, int colour) {
		leds[pixel] = colour;
	}

	/**
	 * Pixels are sent as follows: - The first transmitted pixel is the pixel
	 * closest to the transmitter. - The most significant bit is always sent first.
	 *
	 * g7,g6,g5,g4,g3,g2,g1,g0,r7,r6,r5,r4,r3,r2,r1,r0,b7,b6,b5,b4,b3,b2,b1,b0
	 * \_____________________________________________________________________/ |
	 * _________________... | / __________________... | / / ___________________... |
	 * / / / GRB,GRB,GRB,GRB,...
	 *
	 * For BYTE_ORDER_RGB, the order of the first two bytes are reversed.
	 */
	@Override
	public void render() {
		int bitpos = 7;
		int bytepos = 0;
		int scale = brightness + 1;
		// int array_size = stripType.getWhiteShift() == 0 ? 3 : 4; // RGB or RGBW
		int colour_count = stripType.getColourCount();

		for (int i = 0; i < numLeds; i++) {
			// Swap the colours around based on the led strip type
			byte[] colour = { gamma[(((leds[i] >> stripType.getRedShift()) & 0xff) * scale) >> 8],
					gamma[(((leds[i] >> stripType.getGreenShift()) & 0xff) * scale) >> 8],
					gamma[(((leds[i] >> stripType.getBlueShift()) & 0xff) * scale) >> 8],
					gamma[(((leds[i] >> stripType.getWhiteShift()) & 0xff) * scale) >> 8] };

			// Colour
			for (int j = 0; j < colour_count; j++) {
				// Bit
				for (int k = 7; k >= 0; k--) {
					int symbol = ((colour[j] & (1 << k)) != 0) ? SYMBOL_HIGH : SYMBOL_LOW;

					// Symbol
					for (int l = 2; l >= 0; l--) {
						/*
						 * volatile byte *byteptr = &pxl_raw[bytepos]; byteptr &= ~(1 << bitpos); if
						 * (symbol & (1 << l)) { byteptr |= (1 << bitpos); }
						 */
						pixelRaw[bytepos] &= ~(1 << bitpos);
						if ((symbol & (1 << l)) != 0) {
							pixelRaw[bytepos] |= (1 << bitpos);
						}

						bitpos--;
						if (bitpos < 0) {
							bytepos++;
							bitpos = 7;
						}
					}
				}
			}
		}

		if (lastRenderTime != 0) {
			int diff = (int) (System.nanoTime() - lastRenderTime);
			if (renderWaitTime > diff) {
				SleepUtil.busySleep(renderWaitTime - diff);
			}
		}

		device.write(pixelRaw);
		lastRenderTime = System.nanoTime();
	}

	@Override
	public void allOff() {
		Arrays.fill(leds, 0);
		render();
	}

	private static final int LED_BIT_COUNT(int numLeds, int frequency) {
		return (numLeds * LED_COLOURS * 8 * 3) + ((LED_RESET_uS * (frequency * 3)) / 1000000);
	}

	private static final int PCM_BYTE_COUNT(int numLeds, int frequency) {
		return (((LED_BIT_COUNT(numLeds, frequency) >> 3) & ~0x7) + 4) + 4;
	}

	public enum Protocol {
		/**
		 *
		 * 800kHz bit encodings: '0': ----________ '1': --------____ The period is
		 * 1.25us, giving a basic frequency of 800kHz. Getting the mark-space ratio
		 * right is trickier, though. There are a number of different timings, and the
		 * correct (documented) values depend on the controller chip.
		 *
		 * The _real_ timing restrictions are much simpler though, and someone has
		 * published a lovely analysis here:
		 * http://cpldcpu.wordpress.com/2014/01/14/light_ws2812-library-v2-0-part-i-understanding-the-ws2812/
		 *
		 * In summary: - The period should be at least 1.25us. - The '0' high time can
		 * be anywhere from 0.0625us to 0.5us. - The '1' high time should be longer than
		 * 0.625us.
		 *
		 * These constraints are easy to meet by splitting each bit into three and
		 * packing them into SPI packets. '0': 100 mark: 0.42us, space: 0.83us '1': 110
		 * mark: 0.83us, space: 0.42us
		 */
		PROTOCOL_800KHZ(800_000),
		/**
		 * 400kHz bit encodings: '0': --________ '1': -----_____
		 *
		 * Timing requirements are derived from this document:
		 * http://www.adafruit.com/datasheets/WS2811.pdf
		 *
		 * The period is 2.5us, and we use a 10-bit packet for this encoding: '0':
		 * 1100000000 mark: 0.5us, space: 2us '1': 1111100000 mark: 1.25us, space:
		 * 1.25us
		 */
		PROTOCOL_400KHZ(400_000);

		int frequency;

		Protocol(int frequency) {
			this.frequency = frequency;
		}

		public int getFrequency() {
			return frequency;
		}
	}
}
