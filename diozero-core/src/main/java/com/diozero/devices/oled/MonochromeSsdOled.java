package com.diozero.devices.oled;
/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     MonochromeSsdOled.java
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

import java.awt.image.BufferedImage;
import java.awt.image.Raster;

/**
 * Purportedly common items for "black/white" OLED screens.
 */
public abstract class MonochromeSsdOled extends SsdOled {
    /**
     * ibid.
     */
    public static int DEFAULT_I2C_ADDRESS = 0x3C;

    // Fundamental commands
    protected static final byte SET_CONTRAST = (byte)0x81;
    protected static final byte RESUME_TO_RAM_CONTENT_DISPLAY = (byte)0xA4;
    protected static final byte SET_MEMORY_ADDR_MODE = (byte)0x20;
    // Hardware Configuration (Panel resolution & layout related) Command Table
    protected static final byte SET_DISPLAY_START_LINE_0 = (byte)0x40; // Set display start line from 0-63 (0x40-7F)
    // Column address 127 is mapped to SEG0
    protected static final byte SET_SEGMENT_REMAP_ON = (byte)0xA1;
    // Set MUX ratio to N+1 MUX. From 16MUX to 64MUX, RESET=111111b (i.e. 63d, 64MUX)
    protected static final byte SET_MULTIPLEX_RATIO = (byte)0xA8;
    // enable internal IREF during display on
    protected static final byte SET_IREF_INTERNAL = (byte)0xAD;
    // Remapped mode. Scan from COM[N-1] to COM0 (vertically flipped)
    protected static final byte COM_OUTPUT_SCAN_DIR_REMAPPED = (byte)0xC8;
    // Set vertical shift by COM from 0d~63d (Default = 0x00)
    protected static final byte SET_DISPLAY_OFFSET = (byte)0xD3;
    // COM Pins Hardware Configuration
    protected static final byte SET_COM_PINS_HW_CONFIG = (byte)0xDA;
    // Timing & Driving Scheme Setting Command Table
    // Set Display Clock Divide Ratio/Oscillator Frequency
    protected static final byte DISPLAY_CLOCK_DIV_OSC_FREQ = (byte)0xD5;
    // Set Pre-charge Period
    protected static final byte SET_PRECHARGE_PERIOD = (byte)0xD9;
    protected static final byte SET_VCOMH_DESELECT_LEVEL = (byte)0xDB;

    protected static final byte ENTIRE_DISPLAY_ON = (byte)0xA5;
    protected static final byte INVERSE_DISPLAY = (byte)0xA7;
    protected static final byte NORMAL_DISPLAY = (byte)0xA6; // Default

    // Scrolling commands
    protected static final byte RIGHT_HORIZONTAL_SCROLL = (byte)0x26;
    protected static final byte LEFT_HORIZONTAL_SCROLL = (byte)0x27;
    protected static final byte VERTICAL_AND_RIGHT_HORIZONTAL_SCROLL = (byte)0x29;
    protected static final byte VERTICAL_AND_LEFT_HORIZONTAL_SCROLL = (byte)0x2A;
    /**
     * After sending 2Eh command to deactivate the scrolling
     * action, the ram data needs to be rewritten.
     */
    protected static final byte DEACTIVATE_SCROLL = (byte)0x2E;
    protected static final byte ACTIVATE_SCROLL = (byte)0x2F;
    protected static final byte SET_VERTICAL_SCROLL_AREA = (byte)0xA3;
    // Addressing Setting Command Table
    protected static final byte SET_LOWER_COLUMN_START_ADDR = (byte)0x00; // For Page addressing mode
    protected static final byte SET_HIGHER_COLUMN_START_ADDR = (byte)0x10; // For Page addressing mode
    protected static final byte SET_COLUMN_ADDR = (byte)0x21; // For Horiz or Vertical addressing modes
    protected static final byte SET_PAGE_ADDR = (byte)0x22; // For Horiz or Vertical addressing modes
    protected static final byte SET_PAGE_START_ADDR = (byte)0xB0; // For Page addressing mode (0xB0-B7)
    // Column address 0 is mapped to SEG0
    protected static final byte SET_SEGMENT_REMAP_OFF = (byte)0xA0;
    // Normal mode. Scan from COM0 to COM[N ?1] (Default)
    protected static final byte COM_OUTPUT_SCAN_DIR_NORMAL = (byte)0xC0;
    protected static final byte CONTRAST_EXTERNALVCC = (byte)0x9F;
    protected static final byte CONTRAST_SWITCHCAPVCC = (byte)0xCF;
    protected static final byte ADDR_MODE_VERT = 0b01;  // Vertical Addressing Mode
    protected static final byte ADDR_MODE_PAGE = 0b10;  // Page Addressing Mode (RESET)
    protected static final byte COM_PINS_SEQUENTIAL_REMAP = 0b0010_0010;
    protected static final byte COM_PINS_ALT_REMAP = 0b0011_0010;
    // Vcomh Deselect Levels (SET_VCOMH_DESELECT_LEVEL)
    protected static final byte VCOMH_DESELECT_LEVEL_065 = 0b0000_0000; // 0.65 x VCC
    protected static final byte VCOMH_DESELECT_LEVEL_077 = 0b0010_0000; // 0.77 x VCC (RESET)

    private byte[] buffer;
    protected final int pages;
    protected final boolean externalVcc;

    public static final int DEFAULT_WIDTH = 128;
    protected static final int BPP = 8;

    /**
     * Uses default width of 132 and standard heights.
     *
     * @param device the connection
     * @param height standard height
     */
    public MonochromeSsdOled(SsdOledCommunicationChannel device, Height height) {
        this(device, DEFAULT_WIDTH, height.lines);
    }

    /**
     * Constructor.
     *
     * @param device the connection
     * @param width width of screen
     * @param height height of screen
     */
    public MonochromeSsdOled(SsdOledCommunicationChannel device, int width, int height) {
        super(device, width, height, BufferedImage.TYPE_BYTE_BINARY);
        pages = this.height / 8;
        externalVcc = false;

        init();
    }

    @Override
    protected void goTo(int x, int y) {
        // These commands are only for horizontal or vertical addressing modes
        command(SET_COLUMN_ADDR, (byte)x, (byte)(width - 1));
        command(SET_PAGE_ADDR, (byte)(y / BPP), (byte)(pages - 1));
    }

    /**
     * Sets the display contract. The effectiveness and/or granularity of this will vary from screen type to type
     * (and other factors).
     *
     * @param contrast Contrast
     */
    public void setContrast(byte contrast) {
        command(SET_CONTRAST, contrast);
    }

    /**
     * Sets the display "contrast" (basically the brightness). Small changes are not likely to have much
     * effect.
     * @param percentage the contrast {@code 0-> 1.0}
     */
    public void setContrast(float percentage) {
        byte byteVal = (byte)(Math.round(0xFF * percentage) & 0xFF);
        setContrast(byteVal);
    }

    /**
     * Sets if the display should be inverted
     *
     * @param invert Invert state
     */
    @Override
    public void invertDisplay(boolean invert) {
        command(invert ? INVERSE_DISPLAY : NORMAL_DISPLAY);
    }

    @Override
    protected byte[] getBuffer() {
        if (buffer == null) buffer = new byte[this.width * this.height / BPP];
        return buffer;
    }

    @Override
    public void display(BufferedImage image) {
        display(image, 1);
    }

    /**
     * Display the image with a given sampling threshold for turning a pixel on/off.
     *
     * @param image     the image
     * @param threshold the sampling threshold
     */
    public void display(BufferedImage image, int threshold) {
        if (image.getWidth() != width || image.getHeight() != height) {
            throw new IllegalArgumentException("Invalid input image dimensions, must be " + width + "x" + height);
        }
        /*
         * Unfortunately it isn't possible to render from a byte array even if the image is already in binary format
         */
        //byte[] image_data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        //System.arraycopy(image_data, 0, buffer, 0, image_data.length);
        Raster r = image.getRaster();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                //int[] pixel = r.getPixel(x, y, new int[] {});
                setPixel(x, y, r.getSample(x, y, 0) >= threshold);
            }
        }

        show();
    }

    /**
     * Set a pixel on/off.
     * @param x column
     * @param y row
     * @param on pixel value
     */
    public void setPixel(int x, int y, boolean on) {
        int index = x + (y / BPP) * width;
        if (on) {
            getBuffer()[index] |= (byte)(1 << (y & 7));
        }
        else {
            getBuffer()[index] &= (byte)~(1 << (y & 7));
        }
    }

    public enum Height {
        SHORT(32), TALL(64), LARGE(128);
        public final int lines;

        Height(int l) {
            lines = l;
        }
    }
}
