package com.diozero.devices.oled;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     SSD1351.java  
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

import java.awt.image.BufferedImage;

/**
 * <p>Encapsulates the serial interface to the 16-bit (5-6-5 RGB) and 18-bit
 * (6-6-6 RGB) colour SSD1351 128x128 OLED display hardware. On creation, an
 * initialisation sequence is pumped to the display to properly configure it.
 * Further control commands can then be called to affect the brightness and
 * other settings.</p>
 * 
 * <p>Wiring:</p>
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
 * <p>Links:</p>
 * <ul>
 *  <li><a href="https://www.newhavendisplay.com/app_notes/SSD1351.pdf">Datasheet</a></li>
 *  <li><a href="https://github.com/freetronics/FTOLED/">FTOLED</a></li>
 *  <li><a href="https://github.com/kirberich/teensy_ssd1351">Teensy</a></li>
 *  <li><a href="https://github.com/adafruit/Adafruit-SSD1351-library">Adafruit</a></li>
 * </ul>
 */
public class SSD1351 extends ColourSsdOled {
	private static final int WIDTH = 128;
	private static final int HEIGHT = 128;

	private static final byte SET_COLUMN_ADDRESS = 0x15;
	private static final byte SET_ROW_ADDRESS = 0x75;
	private static final byte WRITE_RAM_COMMAND = 0x5C;
	private static final byte READ_RAM_COMMAND = 0x5D;
	private static final byte REMAP_AND_COLOUR_DEPTH = (byte) 0xA0;
	private static final byte DISPLAY_START_LINE = (byte) 0xA1;
	private static final byte DISPLAY_OFFSET = (byte) 0xA2;
	private static final byte DISPLAY_MODE_ALL_OFF = (byte) 0xA4;
	private static final byte DISPLAY_MODE_ALL_ON = (byte) 0xA5;
	private static final byte DISPLAY_MODE_NORMAL = (byte) 0xA6;
	private static final byte DISPLAY_MODE_INVERSE = (byte) 0xA7;
	private static final byte FUNCTION_SELECTION = (byte) 0xAB;
	private static final byte SET_RESET_PRECHARGE_PERIOD = (byte) 0xB1;
	private static final byte DISPLAY_ENHANCEMENT = (byte) 0xB2;
	private static final byte DISPLAY_CLOCK_DIVIDER = (byte) 0xB3;
	private static final byte SET_VSL = (byte) 0xB4;
	private static final byte SET_GPIO = (byte) 0xB5;
	private static final byte SET_SECOND_PRECHARGE_PERIOD = (byte) 0xB6;
	private static final byte SET_GRAY = (byte) 0xB8;
	private static final byte USE_LUT = (byte) 0xB9;
	private static final byte PRECHARGE_VOLTAGE = (byte) 0xBB;
	private static final byte VCOMH_VOLTAGE = (byte) 0xBE;
	private static final byte CONTRAST_COLOUR = (byte) 0xC1;
	private static final byte MASTER_CONTRAST_CURRENT_CONTROL = (byte) 0xC7;
	private static final byte MULTIPLEX_RATIO = (byte) 0xCA;
	private static final byte COMMAND_LOCK = (byte) 0xFD;
	private static final byte HORIZONTAL_SCROLL = (byte) 0x96;
	private static final byte STOP_SCROLL = (byte) 0x9E;
	private static final byte START_SCROLL = (byte) 0x9F;
	
	public SSD1351(int controller, int chipSelect, DigitalOutputDevice dcPin, DigitalOutputDevice resetPin) {
		// Limit to 5-6-5 image type for now (65k colours)
		super(controller, chipSelect, dcPin, resetPin, WIDTH, HEIGHT, BufferedImage.TYPE_USHORT_565_RGB);
	}

	@Override
	protected void goTo(int x, int y) {
		writeCommandAndData(SET_COLUMN_ADDRESS, (byte) x, (byte) (width - 1));
		writeCommandAndData(SET_ROW_ADDRESS, (byte) y, (byte) (height - 1));
		writeCommand(WRITE_RAM_COMMAND);
	}

	@Override
	protected void init() {
		reset(getResetPin());

		// A[7:0]: MCU protection status [reset = 12h]
		// A[7:0] = 12b, Unlock OLED driver IC MCU interface from entering command [reset]
		// A[7:0] = 16b, Lock OLED driver IC MCU interface from entering command
		// A[7:0] = B0b, Command A2,B1,B3,BB,BE,C1 inaccessible in both lock and unlock state [reset]
		// A[7:0] = B1b, Command A2,B1,B3,BB,BE,C1 accessible if in unlock state
		writeCommandAndData(COMMAND_LOCK, (byte) 0x12);
		writeCommandAndData(COMMAND_LOCK, (byte) 0xB1);
		// Display off
		setDisplayOn(false);
		// A[3:0] [reset=0001], Clock Div Ratio divide by DIVSET
		// A[7:4] = Oscillator Frequency [reset=1101b]
		writeCommandAndData(DISPLAY_CLOCK_DIVIDER, (byte) 0xF1); // 11110001
		writeCommandAndData(MULTIPLEX_RATIO, (byte) 127); // 10 to 127 [reset = 127]
		// A[0]=0b, Horizontal address increment [reset]
		// A[0]=1b, Vertical address increment
		// A[1]=0b, Column address 0 is mapped to SEG0 [reset]
		// A[1]=1b, Column address 127 is mapped to SEG0
		// A[2]=0b, Colour sequence: A > B > C [reset]
		// A[2]=1b, Colour sequence is swapped: C > B > A
		// A[3]=Reserved
		// A[4]=0b, Scan from COM0 to COM[N –1] [reset]
		// A[4]=1b, Scan from COM[N-1] to COM0. Where N is the Multiplex ratio.
		// A[5]=0b, Disable COM Split Odd Even
		// A[5]=1b, Enable COM Split Odd Even [reset]
		// A[7:6]=00/01, 65k Colour depth
		// A[7:6]=10, 262k Colour depth
		// A[7:6]=11, 262k Colour depth (16-bit format 2)
		writeCommandAndData(REMAP_AND_COLOUR_DEPTH, (byte) 0b00110100);   // 0b00110000
		//commandAndData(REMAP_AND_COLOUR_DEPTH, (byte) 0x74); // 0b01110100
		// A[6:0]: Start Address. [reset=0]
		// B[6:0]: End Address. [reset=127]
		writeCommandAndData(SET_COLUMN_ADDRESS, (byte) 0x00, (byte) (width-1));
		// A[6:0]: Start Address. [reset=0]
		// B[6:0]: End Address. [reset=127]
		writeCommandAndData(SET_ROW_ADDRESS, (byte) 0x00, (byte) (height-1));
		// Set start line - this needs to be 0 for a 128x128 display and 96 for a 128x96 display
		writeCommandAndData(DISPLAY_START_LINE, (byte) 0x00);
		writeCommandAndData(DISPLAY_OFFSET, (byte) 0x00);
		// A[1:0] GPIO0:
		//  00 pin HiZ, Input disabled
		//  01 pin HiZ, Input enabled
		//  10 pin output LOW [reset]
		//  11 pin output HIGH
		// A[3:2] GPIO1:
		//  00 pin HiZ, Input disabled
		//  01 pin HiZ, Input enabled
		//  10 pin output LOW [reset]
		//  11 pin output HIGH
		writeCommandAndData(SET_GPIO, (byte) 0x00);
		// A[0]=0b, Disable internal VDD regulator (for power save during sleep mode only)
		// A[0]=1b, Enable internal VDD regulator [reset]
		// A[7:6]=00b, Select 8-bit parallel interface [reset]
		// A[7:6]=01b, Select 16-bit parallel interface
		// A[7:6]=11b, Select 18-bit parallel interface
		writeCommandAndData(FUNCTION_SELECTION, (byte) 0x01);
		// A[3:0] Phase 1 period in N DCLK. 1~15 DCLK allowed.
		// A[7:4] Phase 2 period in N DCLK. 1~15 DCLK allowed.
		// Default: 0x74 (0111 0100)
		writeCommandAndData(SET_RESET_PRECHARGE_PERIOD, (byte) 0x32); // 0011 0010
		// Set COM deselect voltage level [reset = 05h]
		writeCommandAndData(VCOMH_VOLTAGE, (byte) 0x05);
		writeCommand(DISPLAY_MODE_NORMAL);
		// A[7:0] Contrast Value Color A [reset=10001010b = 0x8A]
		// B[7:0] Contrast Value Color B [reset=01010001b = 0x51]
		// C[7:0] Contrast Value Color C [reset=10001010b = 0x8A]
		setContrast((byte) 0xC8, (byte) 0x80, (byte) 0xC8);
		// A[3:0] :
		//  0000b reduce output currents for all colors to 1/16
		//  0001b reduce output currents for all colors to 2/16
		//  1110b reduce output currents for all colors to 15/16
		//  1111b no change [reset]
		writeCommandAndData(MASTER_CONTRAST_CURRENT_CONTROL, (byte) 0x0F);
		// A[1:0]=00 External VSL [reset]
		// A[1:0]=01,10,11 are invalid
		writeCommandAndData(SET_VSL, (byte) 0b10100000, (byte) 0b10110101, (byte) 0b01010101); // 0xA0, 0xB5, 0x55
		// A[3:0] Set Second Pre-charge Period
		//  0000b invalid
		//  0001b 1 DCLKS
		//  0010b 2 DCLKS
		//  1000b 8 DCLKS [reset]
		//  1111b 15 DCLKS
		writeCommandAndData(SET_SECOND_PRECHARGE_PERIOD, (byte) 0x01);

		clearDisplay();
		setDisplayOn(true);
	}

	@Override
	public void invertDisplay(boolean invert) {
		writeCommand(invert ? DISPLAY_MODE_INVERSE : DISPLAY_MODE_NORMAL);
	}

	@Override
	protected void transferDisplayBuffer(int offset, int length) {
		write(getDisplayBuffer(), offset, length);
		writeCommand(WRITE_RAM_COMMAND);
	}

	@Override
	protected void transferDisplayBuffer() {
		write(getDisplayBuffer());
		writeCommand(WRITE_RAM_COMMAND);
	}

	/**
	 * This command is used to set Contrast Setting of the display. The chip has 256
	 * contrast steps from 00h to FFh. The segment output current ISEG increases
	 * linearly with the contrast step, which results in brighter display.
	 */
	@Override
	public void setContrast(byte level) {
		writeCommandAndData(CONTRAST_COLOUR, level, level, level);
	}
	
	@Override
	public void setContrast(byte red, byte green, byte blue) {
		writeCommandAndData(CONTRAST_COLOUR, red, green, blue);
	}
}
