package com.diozero.devices.oled;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     ColourSsdOled.java  
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

import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.SpiDevice;
import com.diozero.util.ColourUtil;
import org.tinylog.Logger;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferUShort;
import java.util.Arrays;

public abstract class ColourSsdOled extends SsdOled {
	private static final int RED_BITS = 5;
	private static final int GREEN_BITS = 6;
	private static final int BLUE_BITS = 5;
	public static final byte MAX_RED = (byte) (Math.pow(2, RED_BITS) - 1);
	public static final byte MAX_GREEN = (byte) (Math.pow(2, GREEN_BITS) - 1);
	public static final byte MAX_BLUE = (byte) (Math.pow(2, BLUE_BITS) - 1);

	private final SpiDevice spiDevice;
	private final DigitalOutputDevice dcPin;
	private final DigitalOutputDevice resetPin;

	private final byte[] displayBuffer;

	public ColourSsdOled(int controller, int chipSelect, DigitalOutputDevice dcPin, DigitalOutputDevice resetPin, int width, int height, int imageType) {
		super(width, height, imageType);

		this.spiDevice = SpiDevice.builder(chipSelect).setController(controller).setFrequency(SPI_FREQUENCY).build();

		this.dcPin = dcPin;
		this.resetPin = resetPin;

		// 16 bit colour hence 2x
		displayBuffer = new byte[2 * width * height];

		init();
	}

	@Override
	public void clearDisplay() {
		Arrays.fill(displayBuffer, (byte) 0);
		display();
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
			Graphics2D g2d = image_to_display.createGraphics();

			g2d.drawImage(image, 0, 0, null);
			g2d.dispose();
		}

		short[] image_data = ((DataBufferUShort) image_to_display.getRaster().getDataBuffer()).getData();
		for (int i = 0; i < image_data.length; i++) {
			displayBuffer[2 * i] = (byte) ((image_data[i] >> 8) & 0xff);
			displayBuffer[2 * i + 1] = (byte) (image_data[i] & 0xff);
		}

		display();
	}

	public byte[] getDisplayBuffer() {
		return displayBuffer;
	}

	public DigitalOutputDevice getResetPin() {
		return resetPin;
	}

	@Override
	protected void home() {
		goTo(0, 0);
	}

	public void setPixel(int x, int y, byte red, byte green, byte blue, boolean display) {
		int index = 2 * (x + y * width);
		short colour = ColourUtil.createColour565(red, green, blue);
		// MSB is transmitted first
		displayBuffer[index] = (byte) ((colour >> 8) & 0xff);
		displayBuffer[index + 1] = (byte) (colour & 0xff);

		if (display) {
			goTo(x, y);
			transferDisplayBuffer(index, 2);
		}
	}

	protected void transferDisplayBuffer(int offset, int length) {
		dcPin.setOn(true);
		spiDevice.write(getDisplayBuffer(), offset, length);
	}

	@Override
	protected void transferDisplayBuffer() {
		dcPin.setOn(true);
		spiDevice.write(getDisplayBuffer());
	}

	protected void write(byte[] bytes) {
		dcPin.setOn(true);
		spiDevice.write(bytes);
	}

	protected void write(byte[] data, int offset, int length) {
		dcPin.setOn(true);
		spiDevice.write(data, offset, length);
	}

	@Override
	protected void writeCommand(byte... commands) {
		dcPin.setOn(false);
		spiDevice.write(commands);
	}

	protected void writeCommandAndData(byte command, byte... data) {
		// Single byte command (D/C# = 0)
		// Multiple byte command (D/C# = 0 for first byte, D/C# = 1 for other bytes)
		write(new byte[]{command});
		write(data);
	}

	public abstract void setContrast(byte level);

	public abstract void setContrast(byte red, byte green, byte blue);
}
