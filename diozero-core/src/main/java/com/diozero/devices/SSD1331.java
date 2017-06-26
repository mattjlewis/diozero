package com.diozero.sandpit;

import java.awt.Graphics2D;

/*-
 * #%L
 * Device I/O Zero - Core
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

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferUShort;

import org.pmw.tinylog.Logger;

import com.diozero.api.DigitalOutputDevice;

/**
 * <p>Encapsulates the serial interface to the 16-bit color (5-6-5 RGB) SSD1331
 * OLED display hardware. On creation, an initialization sequence is pumped to
 * the display to properly configure it. Further control commands can then be
 * called to affect the brightness and other settings.</p>
 * <p>Wiring</p>
 * <pre>
 * GND .... Ground
 * Vcc .... 3v3
 * SCL .... SCLK (SPI)
 * SDA .... MOSI (SPI)
 * RES .... Reset (GPIO) [27]
 * DC  .... Data/Command Select (GPIO) [22]
 * CS  .... Chip Select (SPI)
 * </pre>
 * 
 * <p>Links</p>
 * <ul>
 * <li><a href="https://www.parallax.com/sites/default/files/downloads/28087-SSD1331_1.2.pdf">Datasheet</a></li>
 * <li><a href="https://github.com/rm-hull/luma.oled/blob/master/luma/oled/device.py">Python code</a></li>
 * </ul>
 */
public class SSD1331 extends SsdOled {
	private static final int WIDTH = 96;
	private static final int HEIGHT = 64;
	
	private static final byte SET_COLUMN_ADDRESS = 0x15;
	private static final byte SET_ROW_ADDRESS = 0x75;
	private static final byte CONTRAST_COLOUR_A = (byte) 0x81;
	private static final byte CONTRAST_COLOUR_B = (byte) 0x82;
	private static final byte CONTRAST_COLOUR_C = (byte) 0x83;
	private static final byte MASTER_CURRENT_CONTROL = (byte) 0x87;
	private static final byte PRECHARGE_SPEED_COLOUR_A = (byte) 0x8A;
	private static final byte PRECHARGE_SPEED_COLOUR_B = (byte) 0x8B;
	private static final byte PRECHARGE_SPEED_COLOUR_C = (byte) 0x8C;
	private static final byte REMAP_AND_COLOUR_DEPTH = (byte) 0xA0;
	private static final byte DISPLAY_START_LINE = (byte) 0xA1;
	private static final byte DISPLAY_OFFSET = (byte) 0xA2;
	private static final byte DISPLAY_MODE_NORMAL = (byte) 0xA4;
	private static final byte DISPLAY_MODE_ALL_ON = (byte) 0xA5;
	private static final byte DISPLAY_MODE_ALL_OFF = (byte) 0xA6;
	private static final byte DISPLAY_MODE_INVERSE = (byte) 0xA7;
	private static final byte MULTIPLEX_RATIO = (byte) 0xA8;
	private static final byte DIM_MODE = (byte) 0xAB;
	private static final byte MASTER_CONFIG = (byte) 0xAD;
	private static final byte POWER_SAVE_MODE = (byte) 0xB0;
	private static final byte PHASE12_PERIOD = (byte) 0xB1;
	private static final byte DISPLAY_CLOCK_DIVIDER = (byte) 0xB3;
	private static final byte GRAY_SCALE_TABLE = (byte) 0xB8;
	private static final byte ENABLE_LINEAR_GRAY_SCALE_TABLE = (byte) 0xB9;
	private static final byte PRECHARGE_LEVEL = (byte) 0xBB;
	private static final byte VOLTAGE = (byte) 0xBE;
	
	/*
	 * Each pixel has 16-bit data.
	 * Three sub-pixels for colour A, B and C have 5 bits, 6 bits and 5 bits respectively.
	 */
	private static short getColour(byte red, byte green, byte blue) {
		// 65k format 1 in normal order (ABC = RGB)
		// (2 bytes): 1st byte C4C3C2C1C0B5B4B3, 2nd byte B2B1B0A4A3A2A1A0
		//                     B4B3B2B1B0G5G4G3           G2G1G0R4R3R2R1R0
		// Assume little endian, i.e. high byte is transmitted first
		int colour = blue & 0b11111;
		colour |= (green & 0b111111) << 5;
		colour |= (red & 0b11111) << 11;
		/*
		 * buf[i] = r & 0xF8 | g >> 5
		 * buf[i + 1] = g << 5 & 0xE0 | b >> 3
		 */
		return (short) colour;
	}

	public SSD1331(int controller, int chipSelect, DigitalOutputDevice dcPin, DigitalOutputDevice resetPin) {
		super(controller, chipSelect, dcPin, resetPin, WIDTH, HEIGHT, BufferedImage.TYPE_USHORT_565_RGB);
		
		// 16 bit colour hence 2x
		buffer = new byte[2 * width * height];
		
		init();
	}

	@Override
	protected void init() {
		reset();
		
		// Display off
		setDisplayOn(false);
		// Seg remap:
		//  0 -> Horizontal address increment
		//  1 -> RAM Column 0 to 95 maps to Pin Seg (SA,SB,SC) 95 to 0
		//  0 -> RGB
		//  0 -> Disable left-right swapping on COM
		//  1 -> Scan from COM [N-1] to COM0
		//  1 -> Enable COM split odd-even
		// 01 -> 65k colour
		command(REMAP_AND_COLOUR_DEPTH, (byte) 0b01110010);
		// Set Display start line
		command(DISPLAY_START_LINE, (byte) 0x00);
		// Set display offset
		command(DISPLAY_OFFSET, (byte) 0x00);
		// Normal display
		command(DISPLAY_MODE_NORMAL);
		// Set multiplex (0x3F is the default)
		command(MULTIPLEX_RATIO, (byte) 0x3F);
		// Master configure (Bit A[0] must be set to 0b after RESET)
		command(MASTER_CONFIG, (byte) 0b10001110);
		// Power save mode (0x1A = Enable, 0x0B = Disable)
		command(POWER_SAVE_MODE, (byte) 0x0B);
		// Phase12 period (phase 1 = 4, phase 2 = 7)
		command(PHASE12_PERIOD, (byte) 0b0111_0100);
		// Clock divider (divide ratio = 1, Fosc frequency = 13)
		command(DISPLAY_CLOCK_DIVIDER, (byte) 0b1101_0000);
		// Set precharge speeds
		command(PRECHARGE_SPEED_COLOUR_A, (byte) 0x80, PRECHARGE_SPEED_COLOUR_B, (byte) 0x80, PRECHARGE_SPEED_COLOUR_C, (byte) 0x80);
		// Set pre-charge voltage (default is 0x3E 0b111110)
		command(PRECHARGE_LEVEL, (byte) 0b00111110);
		// Set voltage (default is 0x3E which is 0.83 * Vcc)
		command(VOLTAGE, (byte) 0x3E);
		// Master current control (default is 0x0F)
		command(MASTER_CURRENT_CONTROL, (byte) 0x0F);
		
		setContrast(0xFF);
		clear();
		setDisplayOn(true);
	}

	@Override
	protected void goTo(int x, int y) {
		command(SET_COLUMN_ADDRESS, (byte) x, (byte) (width - 1));
		command(SET_ROW_ADDRESS, (byte) y, (byte) (height - 1));
	}
	
	@Override
	protected void home() {
		goTo(0, 0);
	}

	@Override
	public void display(BufferedImage image) {
		if (image.getWidth() != width || image.getHeight() != height) {
			throw new IllegalArgumentException("Invalid input image dimensions (" + image.getWidth() + "x"
					+ image.getHeight() + "), must be " + width + "x" + height);
		}

		// Make sure the image is of the correct type
		BufferedImage image_to_display = image;
		if (image.getType() != imageType ) {
			Logger.warn("Source image type ({}) doesn't match native image type ({}); converting",
					Integer.valueOf(image.getType()), Integer.valueOf(imageType));
			image_to_display = new BufferedImage(width, height, imageType);
			Graphics2D g2d = image_to_display.createGraphics();
			
			g2d.drawImage(image, 0, 0, null);
			g2d.dispose();
		}

		short[] image_data = ((DataBufferUShort) image_to_display.getRaster().getDataBuffer()).getData();
		for (int i=0; i<image_data.length; i++) {
			buffer[2*i] = (byte) ((image_data[i] >> 8) & 0xff);
			buffer[2*i+1] = (byte) (image_data[i] & 0xff);
		}

		display();
	}
	
	public void setPixel(int x, int y, byte red, byte green, byte blue, boolean display) {
		// 65k format 1 in normal order (ABC = RGB)
		// (2 bytes): 1st byte C4C3C2C1C0B5B4B3, 2nd byte B2B1B0A4A3A2A1A0
		int index = 2 * (x + y*width);
		short colour = getColour(red, green, blue);
		// (2 bytes): 1st byte C4C3C2C1C0B5B4B3, 2nd byte B2B1B0A4A3A2A1A0
		//                     B4B3B2B1B0G5G4G3           G2G1G0R4R3R2R1R0
		// Assume little endian, i.e. high byte is transmitted first
		buffer[index] = (byte) ((colour >> 8) & 0xff);
		buffer[index+1] = (byte) (colour & 0xff);
		
		if (display) {
			goTo(x, y);
			data(index, 2);
		}
	}
	
	/**
	 * Switches the display contrast to the desired level, in the range
	 * 0-255. Note that setting the level to a low (or zero) value will
	 * not necessarily dim the display to nearly off. In other words,
	 * this method is **NOT** suitable for fade-in/out animation.
	 * @param level Desired contrast level in the range 0..255
	 */
	public void setContrast(int level) {
		command(CONTRAST_COLOUR_A, (byte) level, CONTRAST_COLOUR_B, (byte) level, CONTRAST_COLOUR_C, (byte) level);
	}

	/**
	 * Sets if the display should be inverted
	 * 
	 * @param invert
	 *            Invert state
	 */
	public void invertDisplay(boolean invert) {
		command(invert ? DISPLAY_MODE_INVERSE : DISPLAY_MODE_NORMAL);
	}
}
