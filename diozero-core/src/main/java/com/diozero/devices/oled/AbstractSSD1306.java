package com.diozero.devices.oled;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     SSD1306.java
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

import java.awt.image.BufferedImage;
import java.util.Arrays;

/**
 * <p>128x64 Dot Matrix OLED/PLED Segment/Common Driver (128 segments and 64 commons).<br>
 * Segment = column (x), Common = row (y)</p>
 * <p>The size of the RAM is 128 x 64 bits (1024 bytes) and the RAM is divided into eight pages, from
 * PAGE0 to PAGE7, which are used for monochrome 128x64 dot matrix display. Each page is 128 bytes.</p>
 * <pre>
 *        COM   Page   COM (Row re-mapping)
 *        0-7    0    63-56
 *        8-15   1    55-48
 *       16-23   2    47-40
 *       24-31   3    39-32
 *       32-39   4    31-24
 *       40-47   5    23-16
 *       48-55   6    15-8
 *       56-63   7     7-0
 * Segment   0 ..... 127
 * Segment 127 ..... 0   (Column re-mapping)
 * </pre>
 * <p>A byte of render in GDDRAM represents values for all rows of the current column and page.</p>
 * <p>Enlargement of GDDRAM for Page 2 (No row re-mapping and column-remapping):
 * Each + represents one bit of image render.</p>
 * <pre>
 *                       1 1 1 1 1
 *        &lt;- Segment -&gt;  2 2 2 2 2
 *     D 0 1 2 3 4 5 ... 3 4 5 6 7 COM
 * LSB 0 + + + + + + + + + + + + + 16
 *     1 + + + + + + + + + + + + + 17
 *     2 + + + + + + + + + + + + + 18
 *     3 + + + + + + + + + + + + + 19
 *     4 + + + + + + + + + + + + + 20
 *     5 + + + + + + + + + + + + + 21
 *     6 + + + + + + + + + + + + + 22
 * MSB 7 + + + + + + + + + + + + + 23
 * </pre>
 * <p>Wiring</p>
 * <pre>
 * GND .... Ground
 * Vcc .... 3v3
 * D0  .... SCLK (SPI)
 * D1  .... MOSI (SPI)
 * RES .... Reset (GPIO) [27]
 * DC  .... Data/Command Select (GPIO) [22]
 * CS  .... Chip Select (SPI)
 * </pre>
 *
 * <p>Links</p>
 * <ul>
 * <li><a href="https://cdn-shop.adafruit.com/datasheets/SSD1306.pdf">SSD1306 Datasheet</a></li>
 * <li><a href="https://github.com/ondryaso/pi-ssd1306-java/blob/master/src/eu/ondryaso/ssd1306/Display.java">Example Pi4j Java implementation</a></li>
 * <li><a href="https://github.com/LuciferAndDiablo/NTC-C.H.I.P.-JavaGPIOLib/blob/master/GPIOChipLib/src/main/java/free/lucifer/chiplib/modules/SSD1306.java">Example Java implementation on the CHIP</a></li>
 * <li><a href="https://community.oracle.com/docs/DOC-982272">Java ME / JDK Device IO implementation</a></li>
 * </ul>
 */
@SuppressWarnings("unused")
public abstract class AbstractSSD1306 extends SsdOled implements MonochromeSsdOled {
    // Fundamental commands
    private static final byte SET_CONTRAST = (byte) 0x81;
    private static final byte RESUME_TO_RAM_CONTENT_DISPLAY = (byte) 0xA4;
    private static final byte ENTIRE_DISPLAY_ON = (byte) 0xA5;
    private static final byte NORMAL_DISPLAY = (byte) 0xA6; // Default
    private static final byte INVERSE_DISPLAY = (byte) 0xA7;

    // Scrolling commands
    private static final byte RIGHT_HORIZONTAL_SCROLL = (byte) 0x26;
    private static final byte LEFT_HORIZONTAL_SCROLL = (byte) 0x27;
    private static final byte VERTICAL_AND_RIGHT_HORIZONTAL_SCROLL = (byte) 0x29;
    private static final byte VERTICAL_AND_LEFT_HORIZONTAL_SCROLL = (byte) 0x2A;
    /**
     * After sending 2Eh command to deactivate the scrolling
     * action, the ram render needs to be rewritten.
     */
    private static final byte DEACTIVATE_SCROLL = (byte) 0x2E;
    private static final byte ACTIVATE_SCROLL = (byte) 0x2F;
    private static final byte SET_VERTICAL_SCROLL_AREA = (byte) 0xA3;

    // Addressing Setting Command Table
    private static final byte SET_LOWER_COLUMN_START_ADDR = (byte) 0x00; // For Page addressing mode
    private static final byte SET_HIGHER_COLUMN_START_ADDR = (byte) 0x10; // For Page addressing mode
    private static final byte SET_MEMORY_ADDR_MODE = (byte) 0x20;
    private static final byte SET_COLUMN_ADDR = (byte) 0x21; // For Horiz or Vertical addressing modes
    private static final byte SET_PAGE_ADDR = (byte) 0x22; // For Horiz or Vertical addressing modes
    private static final byte SET_PAGE_START_ADDR = (byte) 0xB0; // For Page addressing mode (0xB0-B7)

    // Hardware Configuration (Panel resolution & layout related) Command Table
    private static final byte SET_DISPLAY_START_LINE_0 = (byte) 0x40; // Set display start line from 0-63 (0x40-7F)
    // Column address 0 is mapped to SEG0
    private static final byte SET_SEGMENT_REMAP_OFF = (byte) 0xA0;
    // Column address 127 is mapped to SEG0
    private static final byte SET_SEGMENT_REMAP_ON = (byte) 0xA1;
    // Set MUX ratio to N+1 MUX. From 16MUX to 64MUX, RESET=111111b (i.e. 63d, 64MUX)
    private static final byte SET_MULTIPLEX_RATIO = (byte) 0xA8;
    // Normal mode. Scan from COM0 to COM[N ?1] (Default)
    private static final byte COM_OUTPUT_SCAN_DIR_NORMAL = (byte) 0xC0;
    // Remapped mode. Scan from COM[N-1] to COM0 (vertically flipped)
    private static final byte COM_OUTPUT_SCAN_DIR_REMAPPED = (byte) 0xC8;
    // Set vertical shift by COM from 0d~63d (Default = 0x00)
    private static final byte SET_DISPLAY_OFFSET = (byte) 0xD3;
    // COM Pins Hardware Configuration
    private static final byte SET_COM_PINS_HW_CONFIG = (byte) 0xDA;

    // Timing & Driving Scheme Setting Command Table
    // Set Display Clock Divide Ratio/Oscillator Frequency
    private static final byte DISPLAY_CLOCK_DIV_OSC_FREQ = (byte) 0xD5;

    // Set Pre-charge Period
    private static final byte SET_PRECHARGE_PERIOD = (byte) 0xD9;
    private static final byte SET_VCOMH_DESELECT_LEVEL = (byte) 0xDB;
    private static final byte PRECHARGE_PERIOD_EXTERNALVCC = 0x22;
    private static final byte PRECHARGE_PERIOD_SWITCHCAPVCC = (byte) 0xF1;

    // Charge Pump Command Table
    private static final byte SET_CHARGE_PUMP = (byte) 0x8D;
    private static final byte CHARGE_PUMP_DISABLED = 0x10;
    private static final byte CHARGE_PUMP_ENABLED = 0x14;

    private static final byte CONTRAST_EXTERNALVCC = (byte) 0x9F;
    private static final byte CONTRAST_SWITCHCAPVCC = (byte) 0xCF;

    // Memory addressing modes (SET_MEMORY_ADDR_MODE)
    private static final byte ADDR_MODE_HORIZ = 0b00; // Horizontal Addressing Mode
    private static final byte ADDR_MODE_VERT = 0b01;  // Vertical Addressing Mode
    private static final byte ADDR_MODE_PAGE = 0b10;  // Page Addressing Mode (RESET)

    // COM pins hardware config (SET_COM_PINS_HW_CONFIG)
    private static final byte COM_PINS_SEQUENTIAL_NO_REMAP = 0b0000_0010;
    private static final byte COM_PINS_ALT_NO_REMAP = 0b0001_0010;
    private static final byte COM_PINS_SEQUENTIAL_REMAP = 0b0010_0010;
    private static final byte COM_PINS_ALT_REMAP = 0b0011_0010;

    // Vcomh Deselect Levels (SET_VCOMH_DESELECT_LEVEL)
    private static final byte VCOMH_DESELECT_LEVEL_065 = 0b0000_0000; // 0.65 x VCC
    private static final byte VCOMH_DESELECT_LEVEL_077 = 0b0010_0000; // 0.77 x VCC (RESET)
    private static final byte VCOMH_DESELECT_LEVEL_083 = 0b0011_0000; // 0.83 x VCC

    public static final int WIDTH = 128;
    public static final int HEIGHT = 64;
    private static final int PAGES = 8;

    private final boolean externalVcc;
    private final byte[] displayBuffer;

    public AbstractSSD1306(int width, int height, boolean externalVcc) {
        super(width, height, BufferedImage.TYPE_BYTE_BINARY);
        this.displayBuffer = new byte[width * height / 8];
        this.externalVcc = externalVcc;
    }

    @Override
    public void clearDisplay() {
        Arrays.fill(displayBuffer, (byte) 0);
        display();
    }

    @Override
    public void display(BufferedImage image) {
        draw(image);
        display();
    }

    @Override
    public byte[] getDisplayBuffer() {
        return displayBuffer;
    }

    @Override
    protected void goTo(int x, int y) {
        // These commands are only for horizontal or vertical addressing modes
        writeCommand(SET_COLUMN_ADDR, (byte) x, (byte) (getWidth() - 1));
        writeCommand(SET_PAGE_ADDR, (byte) (y / PAGES), (byte) (getHeight() - 1));
    }

    @Override
    protected void home() {
        writeCommand(SET_COLUMN_ADDR, (byte) 0, (byte) (getWidth() - 1));
        writeCommand(SET_PAGE_ADDR, (byte) 0, (byte) (getHeight() - 1));
    }

    @Override
    protected void init() {
        setDisplayOn(false);

        writeCommand(DISPLAY_CLOCK_DIV_OSC_FREQ, (byte) 0x80);
        writeCommand(SET_MULTIPLEX_RATIO, (byte) 0x3F);
        writeCommand(SET_DISPLAY_OFFSET, (byte) 0x00);
        writeCommand(SET_DISPLAY_START_LINE_0);
        writeCommand(SET_CHARGE_PUMP, externalVcc ? CHARGE_PUMP_DISABLED : CHARGE_PUMP_ENABLED);
        writeCommand(SET_MEMORY_ADDR_MODE, ADDR_MODE_HORIZ);

        writeCommand(SET_SEGMENT_REMAP_ON);
        writeCommand(COM_OUTPUT_SCAN_DIR_REMAPPED);
        writeCommand(SET_COM_PINS_HW_CONFIG, COM_PINS_ALT_NO_REMAP);

        setContrast(externalVcc ? CONTRAST_EXTERNALVCC : CONTRAST_SWITCHCAPVCC);
        writeCommand(SET_PRECHARGE_PERIOD, externalVcc ? PRECHARGE_PERIOD_EXTERNALVCC : PRECHARGE_PERIOD_SWITCHCAPVCC);

        writeCommand(SET_VCOMH_DESELECT_LEVEL, VCOMH_DESELECT_LEVEL_077);
        writeCommand(RESUME_TO_RAM_CONTENT_DISPLAY);
        writeCommand(NORMAL_DISPLAY);
        //command(DEACTIVATE_SCROLL);

        setDisplayOn(true);
    }

    /**
     * Sets if the display should be inverted
     *
     * @param invert Invert state
     */
    @Override
    public void invertDisplay(boolean invert) {
        writeCommand(invert ? INVERSE_DISPLAY : NORMAL_DISPLAY);
    }

    /**
     * Sets the display contract. Apparently not really working.
     *
     * @param contrast Contrast
     */
    public void setContrast(byte contrast) {
        writeCommand(SET_CONTRAST, contrast);
    }

    public void setPixel(int x, int y, boolean on) {
        int index = x + (y / PAGES) * width;
        if (on) {
            displayBuffer[index] |= (1 << (y & 7));
        } else {
            displayBuffer[index] &= ~(1 << (y & 7));
        }
    }
}
