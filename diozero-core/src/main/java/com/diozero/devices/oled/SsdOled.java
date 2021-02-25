package com.diozero.devices.oled;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     SsdOled.java  
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
import com.diozero.util.SleepUtil;

import java.awt.image.BufferedImage;
import java.io.Closeable;

public abstract class SsdOled implements Closeable {
	protected static final int SPI_FREQUENCY = 8_000_000;

	private static final byte DISPLAY_OFF = (byte) 0xAE;
	private static final byte DISPLAY_ON = (byte) 0xAF;

	protected final int width;
	protected final int height;
	protected final int imageType;

	public SsdOled(int width, int height, int imageType) {
		this.width = width;
		this.height = height;
		this.imageType = imageType;
	}

	protected abstract void init();

	protected abstract void goTo(int x, int y);

	protected abstract void home();

	protected abstract void clearDisplay();

	@Override
	public void close() {
		clearDisplay();
		setDisplayOn(false);
	}

	public abstract void display(BufferedImage image);


	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public void display() {
		home();
		transferDisplayBuffer();
	}

	protected void reset(DigitalOutputDevice resetPin) {
		resetPin.setOn(true);
		SleepUtil.sleepMillis(1);
		resetPin.setOn(false);
		SleepUtil.sleepMillis(10);
		resetPin.setOn(true);
	}

	public void setDisplayOn(boolean on) {
		writeCommand(on ? DISPLAY_ON : DISPLAY_OFF);
	}

	protected abstract void transferDisplayBuffer();

	public int getNativeImageType() {
		return imageType;
	}

	public abstract void invertDisplay(boolean invert);

	protected abstract void writeCommand(byte... data);

}
