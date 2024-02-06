package com.diozero.devices.oled;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     SsdOled.java
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

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import org.tinylog.Logger;

import com.diozero.api.DeviceInterface;

public abstract class SsdOled implements DeviceInterface {
	public static final byte DISPLAY_OFF = (byte) 0xAE;
	public static final byte DISPLAY_ON = (byte) 0xAF;

	protected final SsdOledCommunicationChannel device;
	protected final int width;
	protected final int height;

	protected int imageType;

	protected SsdOled(SsdOledCommunicationChannel device,  int width, int height, int imageType) {
		this.device = device;
		this.width = width;
		this.height = height;
		this.imageType = imageType;
	}

	/**
	 * Each device must maintain a <b>static</b> byte buffer of the screen contents.
	 * @return the screen contents to write out
	 */
	protected abstract byte[] getBuffer();

	protected abstract void init();

	protected void reset() {
		device.reset();
	}

	protected void command(byte... commands) {
		device.sendCommand(commands);
	}


	protected void data() {
		device.sendData(getBuffer());
	}

	protected void data(int offset, int length) {
		device.sendData(getBuffer(), offset, length);
	}

	protected abstract void goTo(int x, int y);

	protected void home() {
		goTo(0,0);
	}

	/**
	 * Displays the current buffer contents.
	 */
	@Deprecated
	public void display() {
		show();
	}

	/**
	 * Displays the current buffer contents.
	 */
	public void show() {
		home();
		data();
	}

	/**
	 * Fills the buffer with the image and immediately displays it
	 * @param image the image to display
	 */
	public abstract void display(BufferedImage image);

	public void clear() {
		Arrays.fill(getBuffer(), (byte) 0);
		show();
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public void setDisplayOn(boolean on) {
		command(on ? DISPLAY_ON : DISPLAY_OFF);
	}

	@Override
	public void close() {
		Logger.trace("close()");
		clear();
		setDisplayOn(false);
		device.close();
	}

	public int getNativeImageType() {
		return imageType;
	}

	public abstract void invertDisplay(boolean invert);

	/**
	 * Scales the image to fit. This will scale up or down, depending on the relative sizes.
	 * <p>
	 * This <b>DOES NOT</b> display the image. It will render the image in the "native type" supplied by the display.
	 * </p>
	 *
	 * @param image the image to scale
	 * @return the scaled image
	 */
	public BufferedImage scaleImage(BufferedImage image) {
		BufferedImage showThis = image;
		if (image.getWidth() != width || image.getHeight() != height) {
			float imageWd = image.getWidth();
			float imageHt = image.getHeight();
			float scale = Math.min(width / imageWd, height / imageHt);
			int w = (int)Math.floor(imageWd * scale);
			int y = (int)Math.floor(imageHt * scale);
			Image scaledInstance = image.getScaledInstance(w, y, Image.SCALE_DEFAULT);
			showThis = new BufferedImage(w, y, getNativeImageType());
			showThis.getGraphics().drawImage(scaledInstance, 0, 0, null);
		}
		return showThis;
	}

	/**
	 * Creates a default font (SERIF, PLAIN) that will fit in the specified number of "lines" with 0 spacing.
	 *
	 * @param numberOfLines number of lines
	 * @return the font
	 */
	public Font defaultFont(int numberOfLines) {
		return fitLines(Font.SANS_SERIF, Font.PLAIN, numberOfLines, 0);
	}

	/**
	 * Creates a font of the specified type that will fit on the full display with the specified number of lines,
	 * with the number of <b>pixels</b> between each line.
	 *
	 * @param fontName      name of the font
	 * @param fontType      font type
	 * @param numberOfLines number of lines
	 * @param lineSpacing   number of pixels between lines
	 * @return the font that can be used to match these parameters
	 */
	public Font fitLines(String fontName, int fontType, int numberOfLines, int lineSpacing) {
		// how big is the font in pixels
		int pixelsPerLine = height / numberOfLines - lineSpacing;
		int size = pixelsPerLine + 1;
		int fontSize = 48;    // TODO this is pretty big, but is it big enough?
		Font f = null;
		BufferedImage bufferedImage = new BufferedImage(width, height, getNativeImageType());
		Graphics2D g = bufferedImage.createGraphics();
		while (size > pixelsPerLine) {
			fontSize--;
			if (fontSize <= 0) throw new IllegalStateException("Font size is 0!");
			f = new Font(fontName, fontType, fontSize);
			size = g.getFontMetrics(f).getHeight();
		}
		return f;
	}
}
