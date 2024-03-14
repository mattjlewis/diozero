package com.diozero.devices.oled;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     SSD1306.java
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

import com.diozero.api.DigitalOutputDevice;
import com.diozero.devices.oled.SsdOledCommunicationChannel.SpiCommunicationChannel;

/**
 * <p>
 * 128x64 Dot Matrix OLED/PLED Segment/Common Driver (128 segments and 64
 * commons).<br>
 * Segment = column (x), Common = row (y)
 * </p>
 * <p>
 * The size of the RAM is 128 x 64 bits (1024 bytes) and the RAM is divided into
 * eight pages, from PAGE0 to PAGE7, which are used for monochrome 128x64 dot
 * matrix display. Each page is 128 bytes.
 * </p>
 *
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
 * <p>
 * A byte of data in GDDRAM represents values for all rows of the current column
 * and page.
 * </p>
 * <p>
 * Enlargement of GDDRAM for Page 2 (No row re-mapping and column-remapping):
 * Each + represents one bit of image data.
 * </p>
 *
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
 * <p>
 * Wiring
 * </p>
 *
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
 * <p>
 * Links
 * </p>
 * <ul>
 * <li><a href="https://cdn-shop.adafruit.com/datasheets/SSD1306.pdf">SSD1306
 * Datasheet</a></li>
 * <li><a href=
 * "https://github.com/ondryaso/pi-ssd1306-java/blob/master/src/eu/ondryaso/ssd1306/Display.java">Example
 * Pi4j Java implementation</a></li>
 * <li><a href=
 * "https://github.com/LuciferAndDiablo/NTC-C.H.I.P.-JavaGPIOLib/blob/master/GPIOChipLib/src/main/java/free/lucifer/chiplib/modules/SSD1306.java">Example
 * Java implementation on the CHIP</a></li>
 * <li><a href="https://community.oracle.com/docs/DOC-982272">Java ME / JDK
 * Device IO implementation</a></li>
 * </ul>
 */
@SuppressWarnings("unused")
public class SSD1306 extends MonochromeSsdOled {
	private static final byte PRECHARGE_PERIOD_EXTERNALVCC = 0x22;
	private static final byte PRECHARGE_PERIOD_SWITCHCAPVCC = (byte) 0xF1;

	// Charge Pump Command Table
	private static final byte SET_CHARGE_PUMP = (byte) 0x8D;
	private static final byte CHARGE_PUMP_DISABLED = 0x10;
	private static final byte CHARGE_PUMP_ENABLED = 0x14;
	// Memory addressing modes (SET_MEMORY_ADDR_MODE)
	private static final byte ADDR_MODE_HORIZ = 0b00; // Horizontal Addressing Mode
	// COM pins hardware config (SET_COM_PINS_HW_CONFIG)
	private static final byte COM_PINS_SEQUENTIAL_NO_REMAP = 0b0000_0010;
	private static final byte COM_PINS_ALT_NO_REMAP = 0b0001_0010;
	private static final byte VCOMH_DESELECT_LEVEL_083 = 0b0011_0000; // 0.83 x VCC

	@Deprecated
	public SSD1306(int controller, int chipSelect, DigitalOutputDevice dcPin, DigitalOutputDevice resetPin) {
		this(new SpiCommunicationChannel(controller, chipSelect, SpiCommunicationChannel.SPI_FREQUENCY, dcPin,
				resetPin), Height.TALL);
	}

	/**
	 * Only known to come in two variations, based on height
	 * @param commChannel the comms
	 * @param heightType  how tall
	 */
	public SSD1306(SsdOledCommunicationChannel commChannel, Height heightType) {
		super(commChannel, heightType);
	}

	@Override
	protected void init() {
		reset();

		/*-
		// From the docs (doesn't work):
		// Set MUX Ratio
		command(SET_MULTIPLEX_RATIO, (byte) 0x3F);
		// Set Display Offset
		command(SET_DISPLAY_OFFSET, (byte) 0x00);
		// Set Display Start Line
		command(SET_DISPLAY_START_LINE);
		// Set Segment re-map
		command(SET_SEGMENT_REMAP_ON);
		// Set COM Output Scan Direction
		command(COM_OUTPUT_SCAN_DIR_REMAPPED);
		// Set COM Pins hardware configuration
		command(SET_COM_PINS_HW_CONFIG, (byte) 0x02);
		// Set Contrast Control
		command(SET_CONTRAST, (byte) 0x7F);
		// Disable Entire Display On
		command(RESUME_TO_RAM_CONTENT_DISPLAY);
		// Set Normal Display
		command(NORMAL_DISPLAY);
		// Set Osc Frequency
		command(DISPLAY_CLOCK_DIV_OSC_FREQ, (byte) 0x80);
		// Enable charge pump regulator
		command(SET_CHARGE_PUMP, externalVcc ? CHARGE_PUMP_DISABLED : CHARGE_PUMP_ENABLED);
		// Display on
		command(DISPLAY_ON);
		 */

		int commPinsConfig = this.width > this.height * 2 ? COM_PINS_SEQUENTIAL_NO_REMAP : COM_PINS_ALT_NO_REMAP;

		setDisplayOn(false);
		command(DISPLAY_CLOCK_DIV_OSC_FREQ, (byte) 0x80);
		command(SET_MULTIPLEX_RATIO, (byte) (this.height - 1));
		command(SET_DISPLAY_OFFSET, (byte) 0x00);
		command(SET_DISPLAY_START_LINE_0);
		command(SET_CHARGE_PUMP, externalVcc ? CHARGE_PUMP_DISABLED : CHARGE_PUMP_ENABLED);
		command(SET_MEMORY_ADDR_MODE, ADDR_MODE_HORIZ);
		command(SET_SEGMENT_REMAP_ON);
		command(COM_OUTPUT_SCAN_DIR_REMAPPED);
//		command(SET_COM_PINS_HW_CONFIG, COM_PINS_ALT_NO_REMAP);
		command(SET_COM_PINS_HW_CONFIG, (byte) commPinsConfig);
//		setContrast(externalVcc ? CONTRAST_EXTERNALVCC : CONTRAST_SWITCHCAPVCC);
		setContrast((byte) 0xFF);
		command(SET_PRECHARGE_PERIOD, externalVcc ? PRECHARGE_PERIOD_EXTERNALVCC : PRECHARGE_PERIOD_SWITCHCAPVCC);
		// adafruit indicates this should be 0.83*Vcc (0x30)
		// specs for ssd1306 64x32 oled screens imply this should be 0x40
		command(SET_VCOMH_DESELECT_LEVEL, VCOMH_DESELECT_LEVEL_083);
//		command(SET_VCOMH_DESELECT_LEVEL, VCOMH_DESELECT_LEVEL_077);
		command(RESUME_TO_RAM_CONTENT_DISPLAY);
		command(NORMAL_DISPLAY);
		// command(DEACTIVATE_SCROLL);
		command(SET_IREF_INTERNAL, (byte) 0x30);
		setDisplayOn(true);
	}

}
