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

import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.SpiDevice;

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
 * <p>A byte of data in GDDRAM represents values for all rows of the current column and page.</p>
 * <p>Enlargement of GDDRAM for Page 2 (No row re-mapping and column-remapping):
 * Each + represents one bit of image data.</p>
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
public class SSD1306 extends AbstractSSD1306 {

	private final SpiDevice spiDevice;
	private final DigitalOutputDevice dcPin;
	private final DigitalOutputDevice resetPin;

	public SSD1306(int controller, int chipSelect, DigitalOutputDevice dcPin, DigitalOutputDevice resetPin, int width, int height, boolean externalVcc) {
		super(width, height, externalVcc);

		this.spiDevice = SpiDevice.builder(chipSelect).setController(controller).setFrequency(SPI_FREQUENCY).build();

		this.dcPin = dcPin;
		this.resetPin = resetPin;

		init();
	}

	public SSD1306(int controller, int chipSelect, DigitalOutputDevice dcPin, DigitalOutputDevice resetPin) {
		this(controller, chipSelect, dcPin, resetPin, WIDTH, HEIGHT, false);
	}

	@Override
	public void close() {
		super.close();
		spiDevice.close();
	}

	@Override
	protected void init() {
		reset(resetPin);
		super.init();
	}

//	protected void transferDisplayBuffer(int offset, int length) {
//		dcPin.setOn(true);
//		spiDevice.write(getDisplayBuffer(), offset, length);
//	}

	@Override
	protected void transferDisplayBuffer() {
		dcPin.setOn(true);
		spiDevice.write(getDisplayBuffer());
	}

	@Override
	protected void writeCommand(byte... commands) {
		dcPin.setOn(false);
		spiDevice.write(commands);
	}

}
