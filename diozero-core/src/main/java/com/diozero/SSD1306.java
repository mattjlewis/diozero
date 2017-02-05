package com.diozero;

/*
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
import java.awt.image.Raster;
import java.io.Closeable;
import java.nio.ByteBuffer;
import java.util.Arrays;

import com.diozero.api.*;
import com.diozero.util.SleepUtil;

/**
 * <ul>
 * <li><a href="https://cdn-shop.adafruit.com/datasheets/SSD1306.pdf">SSD1306 Datasheet</a></li>
 * <li><a href="https://github.com/ondryaso/pi-ssd1306-java/blob/master/src/eu/ondryaso/ssd1306/Display.java">Example Pi4j Java implementation</a></li>
 * <li><a href="https://github.com/LuciferAndDiablo/NTC-C.H.I.P.-JavaGPIOLib/blob/master/GPIOChipLib/src/main/java/free/lucifer/chiplib/modules/SSD1306.java">Example Java implementation on the C.H.I.P.</a></li>
 * <li><a href="https://community.oracle.com/docs/DOC-982272">Java ME / JDK Device IO implementation</a></li>
 * </ul>
 */
public class SSD1306 implements Closeable {
	// Fundamental commands
	private static final byte SET_CONTRAST = (byte) 0x81;
	private static final byte ENTIRE_DISPLAY_ON_RESET = (byte) 0xA4;
	private static final byte ENTIRE_DISPLAY_ON = ENTIRE_DISPLAY_ON_RESET | 0x01;
	private static final byte NORMAL_DISPLAY = (byte) 0xA6;
	private static final byte INVERT_DISPLAY = NORMAL_DISPLAY | 0x01;
	private static final byte DISPLAY_OFF = (byte) 0xAE;
	private static final byte DISPLAY_ON = DISPLAY_OFF | 0x01;
	
	// Scrolling commands
	private static final byte RIGHT_HORIZONTAL_SCROLL = (byte) 0x26;
	private static final byte LEFT_HORIZONTAL_SCROLL = RIGHT_HORIZONTAL_SCROLL | 0x01;
	private static final byte VERTICAL_AND_RIGHT_HORIZONTAL_SCROLL = (byte) 0x29;
	private static final byte VERTICAL_AND_LEFT_HORIZONTAL_SCROLL = (byte) 0x2A;
	/** After sending 2Eh command to deactivate the scrolling
	 * action, the ram data needs to be rewritten. */
	private static final byte DEACTIVATE_SCROLL = (byte) 0x2E;
	private static final byte ACTIVATE_SCROLL = DEACTIVATE_SCROLL | 0x01;
	private static final byte SET_VERTICAL_SCROLL_AREA = (byte) 0xA3;
	
	// Addressing Setting Command Table
	private static final byte SET_LOW_COLUMN = (byte) 0x00;
	private static final byte SET_HIGH_COLUMN = (byte) 0x10;
	private static final byte SET_MEMORY_ADDR_MODE = (byte) 0x20;
	private static final byte SET_COLUMN_ADDR = (byte) 0x21;
	private static final byte SET_PAGE_ADDR = (byte) 0x22;
	
	// Hardware Configuration (Panel resolution & layout related) Command Table
	private static final byte SET_DISPLAY_START_LINE = (byte) 0x40;
	private static final byte SET_SEGMENT_REMAP = (byte) 0xA0;
	private static final byte SET_MULTIPLEX_RATIO = (byte) 0xA8;
	private static final byte SET_COM_SCAN_DIR_INC = (byte) 0xC0;
	private static final byte SET_COM_SCAN_DIR_DEC = SET_COM_SCAN_DIR_INC | 0x08;
	private static final byte SET_DISPLAY_OFFSET = (byte) 0xD3;
	private static final byte SET_COM_PINS_HW_CONFIG = (byte) 0xDA;
	
	// Timing & Driving Scheme Setting Command Table 
	private static final byte SET_DISPLAY_CLOCK_DIV = (byte) 0xD5;
	private static final byte SET_PRECHARGE_PERIOD = (byte) 0xD9;
	private static final byte SET_VCOMH_DESELECT_LEVEL = (byte) 0xDB;
	private static final byte PRECHARGE_PERIOD_EXTERNALVCC = (byte) 0x22;
	private static final byte PRECHARGE_PERIOD_SWITCHCAPVCC = (byte) 0xF1;
	
	// Charge Pump Command Table
	private static final byte SET_CHARGE_PUMP = (byte) 0x8D;
	private static final byte CHARGE_PUMP_DISABLED = (byte) 0x10;
	private static final byte CHARGE_PUMP_ENABLED = CHARGE_PUMP_DISABLED | 0x04;

	private static final byte CONTRAST_EXTERNALVCC = (byte) 0x9F;
	private static final byte CONTRAST_SWITCHCAPVCC = (byte) 0xCF;
	
	private static final int WIDTH = 128;
	private static final int HEIGHT = 64;
	
	// TODO I2C support
	//private I2CDevice i2cDevice;
	private SpiDevice spiDevice;
	private DigitalOutputDevice dcPin;
	private DigitalOutputDevice resetPin;
	private int width;
	private int height;
	private int pages;
	private boolean externalVcc;
	private byte[] buffer;
	
	public SSD1306(int controller, int chipSelect, DigitalOutputDevice dcPin, DigitalOutputDevice resetPin) {
		spiDevice = new SpiDevice(controller, chipSelect, 8_000_000, SpiClockMode.MODE_0, false);
		
		this.dcPin = dcPin;
		this.resetPin = resetPin;
		
		width = WIDTH;
		height = HEIGHT;
		pages = height / 8;
		buffer = new byte[width*pages];
		externalVcc = false;
		
		byte ratio = (byte) 0x80;
		byte multiplex = (byte) 0x3F;
		byte compins = (byte) 0x12;
		
		init(ratio, multiplex, compins);
	}
	
	private void reset() {
		resetPin.setOn(true);
		SleepUtil.sleepMillis(1);
		resetPin.setOn(false);
		SleepUtil.sleepMillis(10);
		resetPin.setOn(true);
	}
	
	private void init(byte ratio, byte multiplex, byte compins) {
		reset();
		
		/*
		// From the docs (doesn't work):
		// Set MUX Ratio
		command(SET_MULTIPLEX_RATIO);
		command(multiplex);
		// Set Display Offset
		command(SET_DISPLAY_OFFSET);
		command((byte) 0x00);
		// Set Display Start Line
		command(SET_DISPLAY_START_LINE);
		// Set Segment re-map
		command((byte) (SET_SEGMENT_REMAP | 0x01));
		// Set COM Output Scan Direction
		command(SET_COM_SCAN_DIR_DEC);
		// Set COM Pins hardware configuration
		command(SET_COM_PINS_HW_CONFIG);
		//command((byte) 0x02);
		command(compins);
		// Set Contrast Control
		command(SET_CONTRAST);
		command((byte) 0x7F);
		// Disable Entire Display On
		command(ENTIRE_DISPLAY_ON_RESET);
		// Set Normal Display
		command(NORMAL_DISPLAY);
		// Set Osc Frequency
		command(SET_DISPLAY_CLOCK_DIV);
		command(ratio);
		// Enable charge pump regulator
		command(SET_CHARGE_PUMP);
		command(externalVcc ? CHARGE_PUMP_DISABLED : CHARGE_PUMP_ENABLED);
		// Display on
		command(DISPLAY_ON);
		*/
		
		command(DISPLAY_OFF);
		command(SET_DISPLAY_CLOCK_DIV);
		command(ratio);
		command(SET_MULTIPLEX_RATIO);
		command(multiplex);
		command(SET_DISPLAY_OFFSET);
		command((byte) 0x00);
		command(SET_DISPLAY_START_LINE);
		command(SET_CHARGE_PUMP);
		command(externalVcc ? CHARGE_PUMP_DISABLED : CHARGE_PUMP_ENABLED);
		command(SET_MEMORY_ADDR_MODE);
		command((byte) 0x00);
		command((byte) (SET_SEGMENT_REMAP | 0x01));
		command(SET_COM_SCAN_DIR_DEC);
		command(SET_COM_PINS_HW_CONFIG);
		command(compins);
		setContrast(externalVcc ? CONTRAST_EXTERNALVCC : CONTRAST_SWITCHCAPVCC);
		command(SET_PRECHARGE_PERIOD);
		command(externalVcc ? PRECHARGE_PERIOD_EXTERNALVCC : PRECHARGE_PERIOD_SWITCHCAPVCC);
		command(SET_VCOMH_DESELECT_LEVEL);
		command((byte) 0x40);
		command(ENTIRE_DISPLAY_ON_RESET);
		command(NORMAL_DISPLAY);
		//command(DEACTIVATE_SCROLL);
		command(DISPLAY_ON);
	}
	
	private void command(byte command) {
		ByteBuffer buffer = ByteBuffer.allocateDirect(1);
		buffer.put(command);
		buffer.flip();
		
		dcPin.setOn(false);
		spiDevice.write(buffer);
	}
	
	private void data(byte[] values) {
		ByteBuffer bb = ByteBuffer.allocateDirect(values.length);
		bb.put(values);
		bb.flip();
		
		dcPin.setOn(true);
		spiDevice.write(bb);
	}
	
	public void display() {
		command(SET_COLUMN_ADDR);
		command((byte) 0);
		command((byte) (width - 1));
		command(SET_PAGE_ADDR);
		command((byte) 0);
		command((byte) (pages - 1));

		data(buffer);		
	}
	
	public void setPixel(int x, int y, boolean on) {
		if (on) {
			buffer[x + (y/pages) * width] |= (1 << (y & 7));
		} else {
			buffer[x + (y/pages) * width] &= ~(1 << (y & 7));
		}
	}
	
	public void clear() {
		Arrays.fill(buffer, (byte) 0);
		display();
	}
	
	public void display(BufferedImage image, float threshold) {
		if (image.getWidth() != width || image.getHeight() != height) {
			throw new IllegalArgumentException("Invalid input image dimensions, must be " + width + "x" + height);
		}
		/*
		 * Is it possible to optimise this if the image is already in binary format?
		 * This renders a messed up image.
		if (image.getType() == BufferedImage.TYPE_BYTE_BINARY) {
			byte[] b = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
			Logger.debug("buffer.length={}, b.length={}", buffer.length, b.length);
			int index = 0;
			for (int page=0; page<pages; page++) {
				for (int x=0; x<width; x++) {
					buffer[index++] = b[x + page*width];
				}
			}
		}
		 */
		Raster r = image.getRaster();
		for (int y=0; y<height; y++) {
			for (int x=0; x<width; x++) {
				setPixel(x, y, r.getSample(x, y, 0) >= threshold);
			}
		}
		display();
	}
	
	/**
	 * Sets the display contract. Apparently not really working.
	 * @param contrast Contrast
	 */
	public void setContrast(byte contrast) {
		command(SET_CONTRAST);
		command(contrast);
	}

	/**
	 * Sets if the display should be inverted
	 * 
	 * @param invert
	 *            Invert state
	 */
	public void invertDisplay(boolean invert) {
		command(invert ? INVERT_DISPLAY : NORMAL_DISPLAY);
	}
	
	public void setDisplayOn(boolean on) {
		command((byte) (DISPLAY_OFF & (on ? 1 : 0)));
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
	@Override
	public void close() {
		spiDevice.close();
	}
}
