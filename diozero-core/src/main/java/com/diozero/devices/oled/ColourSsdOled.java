package com.diozero.devices.oled;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     ColourSsdOled.java
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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferUShort;

import org.tinylog.Logger;

import com.diozero.api.DigitalOutputDevice;
import com.diozero.devices.oled.SsdOledCommunicationChannel.SpiCommunicationChannel;
import com.diozero.util.ColourUtil;

import static com.diozero.devices.oled.SsdOledCommunicationChannel.SpiCommunicationChannel.SPI_FREQUENCY;

public abstract class ColourSsdOled extends SsdOled {
	private static final int RED_BITS = 5;
	private static final int GREEN_BITS = 6;
	private static final int BLUE_BITS = 5;
	public static final byte MAX_RED = (byte) (Math.pow(2, RED_BITS) - 1);
	public static final byte MAX_GREEN = (byte) (Math.pow(2, GREEN_BITS) - 1);
	public static final byte MAX_BLUE = (byte) (Math.pow(2, BLUE_BITS) - 1);

	private final byte[] buffer;

	public ColourSsdOled(int controller, int chipSelect, DigitalOutputDevice dcPin, DigitalOutputDevice resetPin,
			int width, int height, int imageType) {
		this(new SpiCommunicationChannel(controller, chipSelect, SPI_FREQUENCY, dcPin, resetPin), width, height,
				imageType);
	}

	public ColourSsdOled(SsdOledCommunicationChannel commChannel, int width, int height, int imageType) {
		super(commChannel, width, height, imageType);
		// 16 bit colour hence 2x
		buffer = new byte[2 * width * height];
		init();
	}

	@Override
	protected byte[] getBuffer() {
		return buffer;
	}

	@Override
	public void display(BufferedImage image) {
		if (image.getWidth() != width || image.getHeight() != height) {
			throw new IllegalArgumentException("Invalid input image dimensions (" + image.getWidth() + "x"
					+ image.getHeight() + "), must be " + width + "x" + height);
		}

		// Make sure the image is of the correct type
		BufferedImage image_to_display = image;
		if (image.getType() != imageType) {
			Logger.warn("Source image type ({}) doesn't match native image type ({}); converting",
					Integer.valueOf(image.getType()), Integer.valueOf(imageType));
			image_to_display = new BufferedImage(width, height, imageType);
			final Graphics2D g2d = image_to_display.createGraphics();

			g2d.drawImage(image, 0, 0, null);
			g2d.dispose();
		}

		short[] image_data = ((DataBufferUShort) image_to_display.getRaster().getDataBuffer()).getData();
		for (int i = 0; i < image_data.length; i++) {
			buffer[2 * i] = (byte) ((image_data[i] >> 8) & 0xff);
			buffer[2 * i + 1] = (byte) (image_data[i] & 0xff);
		}

		show();
	}

	public void setPixel(int x, int y, byte red, byte green, byte blue, boolean display) {
		int index = 2 * (x + y * width);
		short colour = ColourUtil.createColour565(red, green, blue);
		// MSB is transmitted first
		buffer[index] = (byte) ((colour >> 8) & 0xff);
		buffer[index + 1] = (byte) (colour & 0xff);

		if (display) {
			goTo(x, y);
			data(index, 2);
		}
	}

	public abstract void setContrast(byte level);

	public abstract void setContrast(byte red, byte green, byte blue);
}
