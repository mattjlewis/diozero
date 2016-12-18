package com.diozero;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 mattjlewis
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


import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import com.diozero.api.I2CConstants;
import com.diozero.api.I2CDevice;
import com.diozero.util.RuntimeIOException;
import com.diozero.util.SleepUtil;

/**
 * <p>Generic I2C LCD support, code based on <a href=
 * "http://www.raspberrypi-spy.co.uk/2015/05/using-an-i2c-enabled-lcd-screen-with-the-raspberry-pi/">
 * this Raspberry-Pi Spy article</a>, Python code: <a href=
 * "https://bitbucket.org/MattHawkinsUK/rpispy-misc/raw/master/python/lcd_i2c.py">
 * https://bitbucket.org/MattHawkinsUK/rpispy-misc/raw/master/python/lcd_i2c.py
 * </a></p>
 * <p>Another source of information: <a href="https://gist.github.com/DenisFromHR/cc863375a6e19dce359d">https://gist.github.com/DenisFromHR/cc863375a6e19dce359d</a>.</p>
 * <p>Datasheet for HD44780: <a href="https://www.sparkfun.com/datasheets/LCD/HD44780.pdf">https://www.sparkfun.com/datasheets/LCD/HD44780.pdf</a>.</p>
 * <p>Note currently only tested with an NXP PCF8574 I2C backpack.</p>
 */
@SuppressWarnings("unused")
public class I2CLcd implements Closeable {
	private static final boolean DEFAULT_BACKLIGHT_STATE = true;

	// I2C device address
	public static final int DEFAULT_DEVICE_ADDRESS = 0x27;
	
	/*
	 * Instructions:
	 * Instruction | RS | RW | DB7 | DB6 | DB5 | DB4 | DB3 | DB2 | DB1 | DB0 | Description                  | Exec Time
	 * ------------+----+----+-----+-----+-----+-----+-----+-----+-----+-----+------------------------------+----------
	 * Clear       | 0  | 0  |  0  |  0  |  0  |  0  |  0  |  0  |  0  |  1  | Clears entire display and    |
	 * display     |    |    |     |     |     |     |     |     |     |     | sets DDRAM address 0 in      |
	 *             |    |    |     |     |     |     |     |     |     |     | address counter.             |
	 * ------------+----+----+-----+-----+-----+-----+-----+-----+-----+-----+------------------------------+----------
	 * Return      | 0  | 0  |  0  |  0  |  0  |  0  |  0  |  0  |  1  |  -  | Sets DDRAM address 0 in      | 1.52ms
	 * home        |    |    |     |     |     |     |     |     |     |     | address counter. Also        |
	 *             |    |    |     |     |     |     |     |     |     |     | returns display from being   |
	 *             |    |    |     |     |     |     |     |     |     |     | shifted to original position.|
	 *             |    |    |     |     |     |     |     |     |     |     | DDRAM contents remain        |
	 *             |    |    |     |     |     |     |     |     |     |     | unchanged.                   |
	 * ------------+----+----+-----+-----+-----+-----+-----+-----+-----+-----+------------------------------+----------
	 * Entry mode  | 0  | 0  |  0  |  0  |  0  |  0  |  0  |  1  | I/D |  S  | Sets cursor move direction   | 37us
	 * set         |    |    |     |     |     |     |     |     |     |     | and specifies display shift. |
	 *             |    |    |     |     |     |     |     |     |     |     | These operations are         |
	 *             |    |    |     |     |     |     |     |     |     |     | performed during data write  |
	 *             |    |    |     |     |     |     |     |     |     |     | and read.                    |
	 * ------------+----+----+-----+-----+-----+-----+-----+-----+-----+-----+------------------------------+----------
	 * Display on/ | 0  | 0  |  0  |  0  |  0  |  0  |  1  |  D  |  C  |  B  | Sets entire display (D) on/  | 37us
	 * off control |    |    |     |     |     |     |     |     |     |     | off, cursor on/off (C), and  |
	 *             |    |    |     |     |     |     |     |     |     |     | blinking of cursor position  |
	 *             |    |    |     |     |     |     |     |     |     |     | character (B).               |
	 * ------------+----+----+-----+-----+-----+-----+-----+-----+-----+-----+------------------------------+----------
	 * Cursor or   | 0  | 0  |  0  |  0  |  0  |  1  | S/C | R/L |  -  |  -  | Moves cursor and shifts      | 37us
	 * display     |    |    |     |     |     |     |     |     |     |     | display without changing     |
	 * shift       |    |    |     |     |     |     |     |     |     |     | DDRAM contents.              |
	 * ------------+----+----+-----+-----+-----+-----+-----+-----+-----+-----+------------------------------+----------
	 * Function    | 0  | 0  |  0  |  0  |  1  |  DL |  N  |  F  |  -  |  -  | Sets interface data length   | 37us
	 * set         |    |    |     |     |     |     |     |     |     |     | (DL), number of display lines|
	 *             |    |    |     |     |     |     |     |     |     |     | (N), and character font (F). |
	 * ------------+----+----+-----+-----+-----+-----+-----+-----+-----+-----+------------------------------+----------
	 * Set CGRAM   | 0  | 0  |  0  |  1  | ACG | ACG | ACG | ACG | ACG | ACG | Sets CGRAM address.          | 37us
	 * address     |    |    |     |     |     |     |     |     |     |     | CGRAM data is sent and       |
	 *             |    |    |     |     |     |     |     |     |     |     | received after this setting. |
	 * ------------+----+----+-----+-----+-----+-----+-----+-----+-----+-----+------------------------------+----------
	 * Set DDRAM   | 0  | 0  |  1  | ADD | ADD | ADD | ADD | ADD | ADD | ADD | Sets DDRAM address.          | 37us
	 * address     |    |    |     |     |     |     |     |     |     |     | DDRAM data is sent and       |
	 *             |    |    |     |     |     |     |     |     |     |     | received after this setting. |
	 * ------------+----+----+-----+-----+-----+-----+-----+-----+-----+-----+------------------------------+----------
	 * Write data  | 1  | 0  |  d  |  d  |  d  |  d  |  d  |  d  |  d  |  d  | Writes data into DDRAM or    | 37us
	 * to CG or    |    |    |     |     |     |     |     |     |     |     | CGRAM.                       |
	 * DDRAM       |    |    |     |     |     |     |     |     |     |     |                              |
	 * ------------+----+----+-----+-----+-----+-----+-----+-----+-----+-----+------------------------------+----------
	 * Read data   | 1  | 1  |  d  |  d  |  d  |  d  |  d  |  d  |  d  |  d  | Reads data from DDRAM or     | 37us
	 * from CG or  |    |    |     |     |     |     |     |     |     |     | CGRAM.                       |
	 * DDRAM       |    |    |     |     |     |     |     |     |     |     |                              |
	 * ------------+----+----+-----+-----+-----+-----+-----+-----+-----+-----+------------------------------+----------
	 * DDRAM = Display Data RAM.
	 * CGRAM = Character Generator RAM.
	 */
	/** Clears entire display and sets DDRAM address 0 in address counter. */
	private static final byte INST_CLEAR_DISPLAY = 0x01;
	/** Sets DDRAM address 0 in address counter.
	 * Also returns display from being shifted to original position.
	 * DDRAM contents remain unchanged. */
	private static final byte INST_RETURN_HOME = 0x02;
	/** Sets cursor move direction and specifies display shift.
	 * These operations are performed during data write and read. */
	private static final byte INST_ENTRY_MODE_SET = 0x04;
	/** Sets entire display (D) on/off, cursor on/off (C), and blinking of cursor position character (B). */
	private static final byte INST_DISPLAY_CONTROL = 0x08;
	/** Moves cursor and shifts display without changing DDRAM contents. */
	private static final byte INST_CURSOR_DISPLAY_SHIFT = 0x10;
	/** Perform the function at the head of the program before executing any instructions (except for the
	 * read  busy  flag  and  address  instruction).  From  this  point,  the  function  set  instruction  cannot  be
	 * executed unless the interface data length is changed. */
	private static final byte INST_FUNCTION_SET = 0x20;
	/**  Sets CGRAM address. CGRAM data is sent and received after this setting. */
	private static final byte INST_SET_CGRAM_ADDR = 0x40;
	/** Sets DDRAM address. DDRAM data is sent and received after this setting. */
	private static final byte INST_SET_DDRAM_ADDR = (byte) 0x80;
	
	// Flags for INST_ENTRY_MODE_SET
	/** Cursor increment/decrement control, 1=increment, 0=decrement.
	 * The cursor or blinking moves to the right when incremented by 1 and to the left when decremented by 1. */
	private static final byte EMS_CURSOR_INCREMENT = 0x02;
	private static final byte EMS_CURSOR_DECREMENT = 0x00;
	/** Display shift control, 1=on, 0=off.
	 * Shifts the entire display either to the right (I/D = 0) or to the left (I/D = 1) when S is 1.
	 * If S is 1, it will seem as if the cursor does not move but the display does.
	 * The display does not shift if S is 0. */
	private static final byte EMS_DISPLAY_SHIFT_ON = 0x01;
	private static final byte EMS_DISPLAY_SHIFT_OFF = 0x00;

	// Flags for INST_DISPLAY_CONTROL
	/** Display on/off, 1=on, 0=off. */
	private static final byte DC_DISPLAY_ON = 0x04;
	private static final byte DC_DISPLAY_OFF = 0x00;
	/** Cursor on/off, 1=on, 0=off. */
	private static final byte DC_CURSOR_ON = 0x02;
	private static final byte DC_CURSOR_OFF = 0x00;
	/** Cursor blink control, 1=blink, 0=no blink. */
	private static final byte DC_BLINK_ON = 0x01;
	private static final byte DC_BLINK_OFF = 0x00;

	// Flags for INST_CURSOR_DISPLAY_SHIFT
	/** Shift the displayed text, 1=right, 0=left. */
	private static final byte CDS_DISPLAY_SHIFT = 0x08;
	private static final byte CDS_CURSOR_MOVE = 0x00;
	/** Shift the cursor, 1=right, 0=left. */
	private static final byte CDS_SHIFT_RIGHT = 0x04;
	private static final byte CDS_SHIFT_LEFT = 0x00;

	// Flags for INST_FUNCTION_SET
	/** Data is sent or received in 8-bit lengths (DB7 to DB0) when DL is 1,
	 * and  in  4-bit  lengths  (DB7  to  DB4)  when  DL  is  0. */
	private static final byte FS_DATA_LENGTH_8BIT = 0x10;
	private static final byte FS_DATA_LENGTH_4BIT = 0x00;
	/** Sets the number of display lines. 1=2 lines, 0=1 line. */
	private static final byte FS_DISPLAY_2LINES = 0x08;
	private static final byte FS_DISPLAY_1LINE = 0x00;
	/** Sets the character font. 1=5x10 dots (32 character fonts), 0=5x8 dots (208 character fonts).
	 * For some 1 line displays you can select a 10 pixel high font. */
	private static final byte FS_CHAR_FONT_5X10DOTS = 0x04;
	private static final byte FS_CHAR_FONT_5X8DOTS = 0x00;
	
	// Register select (0: instruction, 1:data)
	private static final byte REGISTER_SELECT_BIT			= 0;
	private static final byte REGISTER_SELECT_INSTRUCTION	= 0;
	private static final byte REGISTER_SELECT_DATA			= 1 << REGISTER_SELECT_BIT;
	// Select read or write (0: Write, 1: Read).
	private static final byte DATA_READ_WRITE_BIT			= 1;
	private static final byte DATA_WRITE					= 0;
	private static final byte DATA_READ						= 1 << DATA_READ_WRITE_BIT;
	// Enable bit, starts read/write.
	private static final byte ENABLE_BIT					= 2;
	private static final byte ENABLE						= 1 << ENABLE_BIT;
	// Backlight control bit (1=on, 0=off)
	private static final int BACKLIGHT_BIT					= 3;
	private static final int BACKLIGHT_OFF					= 0;
	private static final int BACKLIGHT_ON					= 1 << BACKLIGHT_BIT;

	// For 2-row LCDs
	private static final byte[] ROW_OFFSETS_2ROWS = { 0x00, 0x40 };
	// For 20x4 LCDs
	private static final byte[] ROW_OFFSETS_20x4 = { 0x00, 0x40, 20, 0x40 + 20 };
	// For 16x4 LCDs - special memory map layout
	private static final byte[] ROW_OFFSETS_16x4 = { 0, 0x40, 16, 0x40 + 16 };

	private GpioExpander gpioExpander;
	private boolean backlight;
	private int columns;
	private int rows;
	private boolean characterFont5x8;
	private boolean displayOn;
	private boolean cursorEnabled;
	private boolean blinkEnabled;
	private boolean increment;
	private boolean shiftDisplay;
	private byte[] rowOffsets;

	public I2CLcd(int columns, int rows) {
		this(I2CConstants.BUS_1, DEFAULT_DEVICE_ADDRESS, ByteOrder.LITTLE_ENDIAN, columns, rows);
	}

	public I2CLcd(int deviceAddress, int columns, int rows) {
		this(I2CConstants.BUS_1, deviceAddress, ByteOrder.LITTLE_ENDIAN, columns, rows);
	}

	public I2CLcd(int controller, int deviceAddress, int columns, int rows) {
		this(controller, deviceAddress, ByteOrder.LITTLE_ENDIAN, columns, rows);
	}

	@SuppressWarnings("resource")
	public I2CLcd(int controller, int deviceAddress, ByteOrder order, int columns, int rows) {
		this(new PCF8574(controller, deviceAddress, I2CConstants.ADDR_SIZE_7,
				I2CConstants.DEFAULT_CLOCK_FREQUENCY, order), columns, rows);
	}
	
	public I2CLcd(GpioExpander gpioExpander, int columns, int rows) {
		if (rows == 2) {
			rowOffsets = ROW_OFFSETS_2ROWS;
		} else if (rows == 4) {
			if (columns == 16) {
				rowOffsets = ROW_OFFSETS_16x4;
			} else if (columns == 20) {
				rowOffsets = ROW_OFFSETS_20x4;
			}
		}
		if (rowOffsets == null) {
			throw new IllegalArgumentException(columns + "x" + rows + " LCDs not supported");
		}
		if (rowOffsets == null) {
			throw new IllegalArgumentException(columns + "x" + rows + " LCDs not supported");
		}

		if (rows < 1 || rows > rowOffsets.length) {
			throw new IllegalArgumentException(
					"Invalid number of rows (" + rows + "), must be 1.." + rowOffsets.length);
		}

		this.columns = columns;
		this.rows = rows;
		backlight = DEFAULT_BACKLIGHT_STATE;
		characterFont5x8 = true;

		this.gpioExpander = gpioExpander;

		// Initialise the display. From p45/46 of the datasheet:
		// https://www.sparkfun.com/datasheets/LCD/HD44780.pdf
		// If the power supply conditions for correctly operating the internal reset
		// circuit are not met, initialisation by instructions becomes necessary.
		// Need to do this 3 times for the 4-bit interface (p46)
		
		// Function set (Interface is 8 bits long).
		write4Bits(true, (byte) (INST_FUNCTION_SET | FS_DATA_LENGTH_8BIT));
		// Wait for more than 4.1 ms
		SleepUtil.sleepMillis(4);
		// Function set (Interface is 8 bits long).
		write4Bits(true, (byte) (INST_FUNCTION_SET | FS_DATA_LENGTH_8BIT));
		// Wait for more than 100us
		SleepUtil.sleepMicros(100);
		// Function set (Interface is 8 bits long).
		write4Bits(true, (byte) (INST_FUNCTION_SET | FS_DATA_LENGTH_8BIT));
		// Now set it to 4-bit mode
		write4Bits(true, (byte) (INST_FUNCTION_SET | FS_DATA_LENGTH_4BIT));

		// Function set: 4-bit data length, lines & character font as requested
		writeInstruction((byte) (INST_FUNCTION_SET
				| FS_DATA_LENGTH_4BIT
				| (rows == 1 ? FS_DISPLAY_1LINE : FS_DISPLAY_2LINES)
				| (characterFont5x8 ? FS_CHAR_FONT_5X8DOTS : FS_CHAR_FONT_5X10DOTS)
				));
		// Display On, Cursor on, Blink on
		displayControl(true, true, true);
		// Cursor increment, display shift off
		entryModeControl(true, false);
		// Clear display
		clear();
	}

	private void writeInstruction(byte data) {
		writeByte(true, data);
	}

	private void writeData(byte data) {
		writeByte(false, data);
	}

	private void writeByte(boolean instruction, byte data) {
		// High bits
		write4Bits(instruction, (byte) (data & 0xF0));
		// Low bits
		write4Bits(instruction, (byte) (data << 4));
	}

	private void write4Bits(boolean instruction, byte value) {
		byte data = (byte) (value
				| (instruction ? REGISTER_SELECT_INSTRUCTION : REGISTER_SELECT_DATA)
				| (backlight ? BACKLIGHT_ON : BACKLIGHT_OFF));

		gpioExpander.setValues(0, (byte) (data | ENABLE));
		// 50us delay enough?
		SleepUtil.sleepMicros(50);
		gpioExpander.setValues(0, (byte) (data & ~ENABLE));
		// 50us delay enough?
		SleepUtil.sleepMicros(50);
	}

	public int getColumnCount() {
		return columns;
	}

	public int getRowCount() {
		return rows;
	}

	public boolean isBacklightOn() {
		return backlight;
	}

	public I2CLcd setBacklightOn(boolean backlight) {
		this.backlight = backlight;
		writeByte(true, (byte) 0);
		
		return this;
	}

	public I2CLcd setCursorPosition(int column, int row) {
		if (column < 0 || column >= columns) {
			throw new IllegalArgumentException("Invalid column (" + column + "), must be 0.." + (column - 1));
		}

		if (row < 0 || row >= rows) {
			throw new IllegalArgumentException("Invalid row (" + row + "), must be 0.." + (rows - 1));
		}

		byte[] row_offsets;
		writeInstruction((byte) (INST_SET_DDRAM_ADDR | (column + rowOffsets[row])));
		
		return this;
	}
	
	public I2CLcd setCharacter(int column, int row, char character) {
		setCursorPosition(column, row);
		writeData((byte) character);
		
		return this;
	}

	/**
	 * Send string to display
	 * @param row Row number (starts at 0)
	 * @param text Text to display
	 * @return This object instance
	 */
	public I2CLcd setText(int row, String text) {
		if (row < 0 || row >= rows) {
			throw new IllegalArgumentException("Invalid row (" + row + "), must be 0.." + (rows - 1));
		}

		// Trim the string to the length of the column
		if (text.length() >= columns)
			text = text.substring(0, columns);
		
		// Set the cursor position to the start of the specified row
		setCursorPosition(0, row);

		for (byte b : text.getBytes()) {
			writeData(b);
		}
		
		return this;
	}
	
	public I2CLcd addText(String text) {
		for (byte b : text.getBytes()) {
			writeData(b);
		}
		
		return this;
	}
	
	public I2CLcd addText(char character) {
		writeData((byte) character);
		
		return this;
	}
	
	public I2CLcd addText(int code) {
		writeData((byte) code);
		
		return this;
	}

	/**
	 * Clear the display
	 * @return This object instance
	 */
	public I2CLcd clear() {
		writeInstruction(INST_CLEAR_DISPLAY);
		// Seem to have to wait after clearing the display, encounter strange errors otherwise
		SleepUtil.sleepMillis(1);
		
		return this;
	}

	/**
	 * Return the cursor to the home position
	 * @return This object instance
	 */
	public I2CLcd returnHome() {
		writeInstruction(INST_RETURN_HOME);
		
		return this;
	}
	
	/**
	 * Control text entry mode.
	 * @param increment
	 *				The cursor or blinking moves to the right when incremented
	 *				by 1 and to the left when decremented by 1.
	 * @param shiftDisplay
	 *				Shifts the entire display either to the right (I/D = 0) or
	 *				to the left (I/D = 1) when true. The display does not shift
	 *				if false. If true, it will seem as if the cursor does not
	 *				move but the display does.
	 * @return This object instance
	 */
	public I2CLcd entryModeControl(boolean increment, boolean shiftDisplay) {
		this.increment = increment;
		this.shiftDisplay = shiftDisplay;
		writeInstruction((byte) (INST_ENTRY_MODE_SET
				| (increment ? EMS_CURSOR_INCREMENT : EMS_CURSOR_DECREMENT)
				| (shiftDisplay ? EMS_DISPLAY_SHIFT_ON : EMS_DISPLAY_SHIFT_OFF)
				));
		
		return this;
	}
	
	public I2CLcd autoscrollOn() {
		entryModeControl(true, true);
		return this;
	}
	
	public I2CLcd autoscrollOff() {
		entryModeControl(true, false);
		return this;
	}
	
	public boolean isIncrementOn() {
		return increment;
	}
	
	public boolean isShiftDisplayOn() {
		return shiftDisplay;
	}

	public I2CLcd displayControl(boolean displayOn, boolean cursorEnabled, boolean blinkEnabled) {
		this.displayOn = displayOn;
		this.cursorEnabled = cursorEnabled;
		this.blinkEnabled = blinkEnabled;
		writeInstruction((byte) (INST_DISPLAY_CONTROL
				| (displayOn ? DC_DISPLAY_ON : DC_DISPLAY_OFF)
				| (cursorEnabled ? DC_CURSOR_ON : DC_CURSOR_OFF)
				| (blinkEnabled ? DC_BLINK_ON : DC_BLINK_OFF)
				));
		
		return this;
	}
	
	public I2CLcd displayOn() {
		return displayControl(true, cursorEnabled, blinkEnabled);
	}
		
	public I2CLcd displayOff() {
		return displayControl(false, cursorEnabled, blinkEnabled);
	}
	
	public I2CLcd cursorOn() {
		return displayControl(displayOn, true, blinkEnabled);
	}
		
	public I2CLcd cursorOff() {
		return displayControl(displayOn, false, blinkEnabled);
	}
	
	public I2CLcd blinkOn() {
		return displayControl(displayOn, true, true);
	}
		
	public I2CLcd blinkOff() {
		return displayControl(displayOn, cursorEnabled, false);
	}
	
	public boolean isCursorEnabled() {
		return cursorEnabled;
	}
	
	public boolean isBlinkEnabled() {
		return blinkEnabled;
	}
	
	/**
	 * Cursor or display shift shifts the cursor position or display to the right
	 * or left without writing or reading display data. This function is used to
	 * correct or search the display. In a 2-line display, the cursor moves to
	 * the second line when it passes the 40th digit of the first line. Note that
	 * the first and second line displays will shift at the same time.
	 * When the displayed data is shifted repeatedly each line moves only horizontally.
	 * The second line display does not shift into the first line position.
	 * @param displayShift
	 * 				Shift the display if true, the cursor if false.
	 * @param shiftRight
	 *				Shift to the right if true, to the left if false.
	 * @return This object instance
	 */
	public I2CLcd cursorOrDisplayShift(boolean displayShift, boolean shiftRight) {
		writeInstruction((byte) (INST_CURSOR_DISPLAY_SHIFT
				| (displayShift ? CDS_DISPLAY_SHIFT : CDS_CURSOR_MOVE)
				| (shiftRight ? CDS_SHIFT_RIGHT : CDS_SHIFT_LEFT)
				));
		
		return this;
	}
	
	public I2CLcd shiftDisplayRight() {
		cursorOrDisplayShift(true, true);
		
		return this;
	}
	
	public I2CLcd shiftDisplayLeft() {
		cursorOrDisplayShift(true, false);
		
		return this;
	}
	
	public I2CLcd moveCursorRight() {
		cursorOrDisplayShift(false, true);
		
		return this;
	}
	
	public I2CLcd moveCursorLeft() {
		cursorOrDisplayShift(false, false);
		
		return this;
	}
	
	public I2CLcd createChar(int location, byte[] charMap) {
		/* In the character generator RAM, the user can rewrite character patterns by program.
		 * For 5×8 dots, eight character patterns can be written, and for 5×10 dots,
		 * four character patterns can be written. */
		if (characterFont5x8) {
			if (location < 0 || location >= 8) {
				throw new IllegalArgumentException("Invalid location (" + location + ") , must be 0..7");
			}
			if (charMap.length != 8) {
				throw new IllegalArgumentException("Invalid charMap length (" + charMap.length + ") , must be 8");
			}
		} else {
			if (location < 0 || location >= 4) {
				throw new IllegalArgumentException("Invalid location (" + location + ") , must be 0..3");
			}
			if (charMap.length != 10) {
				throw new IllegalArgumentException("Invalid charMap length (" + charMap.length + ") , must be 10");
			}
		}
	   
		writeInstruction((byte) (INST_SET_CGRAM_ADDR | (location<<3)));
		SleepUtil.sleepMicros(100);

		for (int i=0; i<charMap.length; i++) {
			writeData((byte) (charMap[i] & 0b11111));
			SleepUtil.sleepMicros(100);
		}
		
		return this;
	}

	@Override
	public void close() throws RuntimeIOException {
		backlight = false;
		clear();
		displayControl(false, false, false);
		try {
			gpioExpander.close();
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}
	
	public static class Characters {
		private static final Map<String, byte[]> CHARACTERS = new HashMap<>();
		static {
			CHARACTERS.put("0",					new byte[] { 0xe, 0x1b, 0x1b, 0x1b, 0x1b, 0x1b, 0xe });
			CHARACTERS.put("1",					new byte[] { 0x2, 0x6, 0xe, 0x6, 0x6, 0x6, 0x6 });
			CHARACTERS.put("2",					new byte[] { 0xe, 0x1b, 0x3, 0x6, 0xc, 0x18, 0x1f });
			CHARACTERS.put("3",					new byte[] { 0xe, 0x1b, 0x3, 0xe, 0x3, 0x1b, 0xe });
			CHARACTERS.put("4",					new byte[] { 0x3, 0x7, 0xf, 0x1b, 0x1f, 0x3, 0x3 });
			CHARACTERS.put("5",					new byte[] { 0x1f, 0x18, 0x1e, 0x3, 0x3, 0x1b, 0xe });
			CHARACTERS.put("6",					new byte[] { 0xe, 0x1b, 0x18, 0x1e, 0x1b, 0x1b, 0xe });
			CHARACTERS.put("7",					new byte[] { 0x1f, 0x3, 0x6, 0xc, 0xc, 0xc, 0xc });
			CHARACTERS.put("8",					new byte[] { 0xe, 0x1b, 0x1b, 0xe, 0x1b, 0x1b, 0xe });
			CHARACTERS.put("9",					new byte[] { 0xe, 0x1b, 0x1b, 0xf, 0x3, 0x1b, 0xe });
			CHARACTERS.put("10",				new byte[] { 0x17, 0x15, 0x15, 0x15, 0x17, 0x0, 0x1f });
			CHARACTERS.put("11",				new byte[] { 0xa, 0xa, 0xa, 0xa, 0xa, 0x0, 0x1f });
			CHARACTERS.put("12",				new byte[] { 0x17, 0x11, 0x17, 0x14, 0x17, 0x0, 0x1f });
			CHARACTERS.put("13",				new byte[] { 0x17, 0x11, 0x13, 0x11, 0x17, 0x0, 0x1f });
			CHARACTERS.put("14",				new byte[] { 0x15, 0x15, 0x17, 0x11, 0x11, 0x0, 0x1f });
			CHARACTERS.put("15",				new byte[] { 0x17, 0x14, 0x17, 0x11, 0x17, 0x0, 0x1f });
			CHARACTERS.put("16",				new byte[] { 0x17, 0x14, 0x17, 0x15, 0x17, 0x0, 0x1f });
			CHARACTERS.put("17",				new byte[] { 0x17, 0x11, 0x12, 0x12, 0x12, 0x0, 0x1f });
			CHARACTERS.put("18",				new byte[] { 0x17, 0x15, 0x17, 0x15, 0x17, 0x0, 0x1f });
			CHARACTERS.put("19",				new byte[] { 0x17, 0x15, 0x17, 0x11, 0x17, 0x0, 0x1f });
			CHARACTERS.put("circle",			new byte[] { 0x0, 0xe, 0x11, 0x11, 0x11, 0xe, 0x0 });
			CHARACTERS.put("cdot",				new byte[] { 0x0, 0xe, 0x11, 0x15, 0x11, 0xe, 0x0 });
			CHARACTERS.put("donut",				new byte[] { 0x0, 0xe, 0x1f, 0x1b, 0x1f, 0xe, 0x0 });
			CHARACTERS.put("ball",				new byte[] { 0x0, 0xe, 0x1f, 0x1f, 0x1f, 0xe, 0x0 });
			CHARACTERS.put("square",			new byte[] { 0x0, 0x1f, 0x11, 0x11, 0x11, 0x1f, 0x0 });
			CHARACTERS.put("sdot",				new byte[] { 0x0, 0x1f, 0x11, 0x15, 0x11, 0x1f, 0x0 });
			CHARACTERS.put("fbox",				new byte[] { 0x0, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x0 });
			CHARACTERS.put("sbox",				new byte[] { 0x0, 0x0, 0xe, 0xa, 0xe, 0x0, 0x0 });
			CHARACTERS.put("sfbox",				new byte[] { 0x0, 0x0, 0xe, 0xe, 0xe, 0x0, 0x0 });
			CHARACTERS.put("bigpointerright",	new byte[] { 0x8, 0xc, 0xa, 0x9, 0xa, 0xc, 0x8 });
			CHARACTERS.put("bigpointerleft",	new byte[] { 0x2, 0x6, 0xa, 0x12, 0xa, 0x6, 0x2 });
			CHARACTERS.put("arrowright",		new byte[] { 0x8, 0xc, 0xa, 0x9, 0xa, 0xc, 0x8 });
			CHARACTERS.put("arrowleft",			new byte[] { 0x2, 0x6, 0xa, 0x12, 0xa, 0x6, 0x2 });
			CHARACTERS.put("ascprogress1",		new byte[] { 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10, 0x10 });
			CHARACTERS.put("ascprogress2",		new byte[] { 0x18, 0x18, 0x18, 0x18, 0x18, 0x18, 0x18, 0x18 });
			CHARACTERS.put("ascprogress3",		new byte[] { 0x1c, 0x1c, 0x1c, 0x1c, 0x1c, 0x1c, 0x1c, 0x1c });
			CHARACTERS.put("ascprogress4",		new byte[] { 0x1e, 0x1e, 0x1e, 0x1e, 0x1e, 0x1e, 0x1e, 0x1e });
			CHARACTERS.put("fullprogress",		new byte[] { 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f, 0x1f });
			CHARACTERS.put("descprogress1",		new byte[] { 1, 1, 1, 1, 1, 1, 1, 1 });
			CHARACTERS.put("descprogress2",		new byte[] { 3, 3, 3, 3, 3, 3, 3, 3 });
			CHARACTERS.put("descprogress3",		new byte[] { 7, 7, 7, 7, 7, 7, 7, 7 });
			CHARACTERS.put("descprogress4",		new byte[] { 15, 15, 15, 15, 15, 15, 15, 15 });
			CHARACTERS.put("ascchart1",			new byte[] { 31, 0, 0, 0, 0, 0, 0, 0 });
			CHARACTERS.put("ascchart2",			new byte[] { 31, 31, 0, 0, 0, 0, 0, 0 });
			CHARACTERS.put("ascchart3",			new byte[] { 31, 31, 31, 0, 0, 0, 0, 0 });
			CHARACTERS.put("ascchart4",			new byte[] { 31, 31, 31, 31, 0, 0, 0, 0 });
			CHARACTERS.put("ascchart5",			new byte[] { 31, 31, 31, 31, 31, 0, 0, 0 });
			CHARACTERS.put("ascchart6",			new byte[] { 31, 31, 31, 31, 31, 31, 0, 0 });
			CHARACTERS.put("ascchart7",			new byte[] { 31, 31, 31, 31, 31, 31, 31, 0 });
			CHARACTERS.put("descchart1",		new byte[] { 0, 0, 0, 0, 0, 0, 0, 31 });
			CHARACTERS.put("descchart2",		new byte[] { 0, 0, 0, 0, 0, 0, 31, 31 });
			CHARACTERS.put("descchart3",		new byte[] { 0, 0, 0, 0, 0, 31, 31, 31 });
			CHARACTERS.put("descchart4",		new byte[] { 0, 0, 0, 0, 31, 31, 31, 31 });
			CHARACTERS.put("descchart5",		new byte[] { 0, 0, 0, 31, 31, 31, 31, 31 });
			CHARACTERS.put("descchart6",		new byte[] { 0, 0, 31, 31, 31, 31, 31, 31 });
			CHARACTERS.put("descchart7",		new byte[] { 0, 31, 31, 31, 31, 31, 31, 31 });
			CHARACTERS.put("borderleft1",		new byte[] { 1, 1, 1, 1, 1, 1, 1, 1 });
			CHARACTERS.put("borderleft2",		new byte[] { 3, 2, 2, 2, 2, 2, 2, 3 });
			CHARACTERS.put("borderleft3",		new byte[] { 7, 4, 4, 4, 4, 4, 4, 7 });
			CHARACTERS.put("borderleft4",		new byte[] { 15, 8, 8, 8, 8, 8, 8, 15 });
			CHARACTERS.put("borderleft5",		new byte[] { 31, 16, 16, 16, 16, 16, 16, 31 });
			CHARACTERS.put("bordertopbottom5",	new byte[] { 31, 0, 0, 0, 0, 0, 0, 31 });
			CHARACTERS.put("borderright1",		new byte[] { 16, 16, 16, 16, 16, 16, 16, 16 });
			CHARACTERS.put("borderright2",		new byte[] { 24, 8, 8, 8, 8, 8, 8, 24 });
			CHARACTERS.put("borderright3",		new byte[] { 28, 4, 4, 4, 4, 4, 4, 28 });
			CHARACTERS.put("borderright4",		new byte[] { 30, 2, 2, 2, 2, 2, 2, 30 });
			CHARACTERS.put("borderright5",		new byte[] { 31, 1, 1, 1, 1, 1, 1, 31 });
			CHARACTERS.put("box1",				new byte[] { 3, 3, 3, 0, 0, 0, 0 });
			CHARACTERS.put("box2",				new byte[] { 24, 24, 24, 0, 0, 0, 0 });
			CHARACTERS.put("box3",				new byte[] { 27, 27, 27, 0, 0, 0, 0 });
			CHARACTERS.put("box4",				new byte[] { 0, 0, 0, 0, 3, 3, 3 });
			CHARACTERS.put("box5",				new byte[] { 3, 3, 3, 0, 3, 3, 3 });
			CHARACTERS.put("box6",				new byte[] { 24, 24, 24, 0, 3, 3, 3 });
			CHARACTERS.put("box7",				new byte[] { 27, 27, 27, 0, 3, 3, 3 });
			CHARACTERS.put("box8",				new byte[] { 0, 0, 0, 0, 24, 24, 24 });
			CHARACTERS.put("box9",				new byte[] { 3, 3, 3, 0, 24, 24, 24 });
			CHARACTERS.put("box10",				new byte[] { 24, 24, 24, 0, 24, 24, 24 });
			CHARACTERS.put("box11",				new byte[] { 27, 27, 27, 0, 24, 24, 24 });
			CHARACTERS.put("box12",				new byte[] { 0, 0, 0, 0, 27, 27, 27 });
			CHARACTERS.put("box13",				new byte[] { 3, 3, 3, 0, 27, 27, 27 });
			CHARACTERS.put("box14",				new byte[] { 24, 24, 24, 0, 27, 27, 27 });
			CHARACTERS.put("box15",				new byte[] { 27, 27, 27, 0, 27, 27, 27 });
			CHARACTERS.put("euro",				new byte[] { 3, 4, 30, 8, 30, 8, 7 });
			CHARACTERS.put("cent",				new byte[] { 0, 0, 14, 17, 16, 21, 14, 8 });
			CHARACTERS.put("speaker",			new byte[] { 1, 3, 15, 15, 15, 3, 1 });
			CHARACTERS.put("sound",				new byte[] { 8, 16, 0, 24, 0, 16, 8 });
			CHARACTERS.put("x",					new byte[] { 0, 27, 14, 4, 14, 27, 0 });
			CHARACTERS.put("target",			new byte[] { 0, 10, 17, 21, 17, 10, 0 });
			CHARACTERS.put("pointerright",		new byte[] { 0, 8, 12, 14, 12, 8, 0 });
			CHARACTERS.put("pointerup",			new byte[] { 0, 0, 4, 14, 31, 0, 0 });
			CHARACTERS.put("pointerleft",		new byte[] { 0, 2, 6, 14, 6, 2, 0 });
			CHARACTERS.put("pointerdown",		new byte[] { 0, 0, 31, 14, 4, 0, 0 });
			CHARACTERS.put("arrowne",			new byte[] { 0, 15, 3, 5, 9, 16, 0 });
			CHARACTERS.put("arrownw",			new byte[] { 0, 30, 24, 20, 18, 1, 0 });
			CHARACTERS.put("arrowsw",			new byte[] { 0, 1, 18, 20, 24, 30, 0 });
			CHARACTERS.put("arrowse",			new byte[] { 0, 16, 9, 5, 3, 15, 0 });
			CHARACTERS.put("dice1",				new byte[] { 0, 0, 0, 4, 0, 0, 0 });
			CHARACTERS.put("dice2",				new byte[] { 0, 16, 0, 0, 0, 1, 0 });
			CHARACTERS.put("dice3",				new byte[] { 0, 16, 0, 4, 0, 1, 0 });
			CHARACTERS.put("dice4",				new byte[] { 0, 17, 0, 0, 0, 17, 0 });
			CHARACTERS.put("dice5",				new byte[] { 0, 17, 0, 4, 0, 17, 0 });
			CHARACTERS.put("dice6",				new byte[] { 0, 17, 0, 17, 0, 17, 0 });
			CHARACTERS.put("bell",				new byte[] { 4, 14, 14, 14, 31, 0, 4 });
			CHARACTERS.put("smile",				new byte[] { 0, 10, 0, 17, 14, 0, 0 });
			CHARACTERS.put("note",				new byte[] { 2, 3, 2, 14, 30, 12, 0 });
			CHARACTERS.put("clock",				new byte[] { 0, 14, 21, 23, 17, 14, 0 });
			CHARACTERS.put("heart",				new byte[] { 0, 10, 31, 31, 31, 14, 4, 0 });
			CHARACTERS.put("duck",				new byte[] { 0, 12, 29, 15, 15, 6, 0 });
			CHARACTERS.put("check",				new byte[] { 0, 1, 3, 22, 28, 8, 0 });
			CHARACTERS.put("retarrow",			new byte[] { 1, 1, 5, 9, 31, 8, 4 });
			CHARACTERS.put("runninga",			new byte[] { 6, 6, 5, 14, 20, 4, 10, 17 });
			CHARACTERS.put("runningb",			new byte[] { 6, 6, 4, 14, 14, 4, 10, 10 });
			CHARACTERS.put("space_invader",		new byte[] { 0x00, 0x0e, 0x15, 0x1f, 0x0a, 0x04, 0x0a, 0x11 });
			CHARACTERS.put("smilie",			new byte[] { 0x00, 0x00, 0x0a, 0x00, 0x00, 0x11, 0x0e, 0x00 });
			CHARACTERS.put("frownie",			new byte[] { 0x00, 0x00, 0x0a, 0x00, 0x00, 0x00, 0x0e, 0x11 });
		}
		
		public static byte[] get(String code) {
			return CHARACTERS.get(code);
		}
	}
	
	public static class PCF8574 implements GpioExpander {
		private I2CDevice device;
		
		public PCF8574(int controller, int deviceAddress, int addressSize, int frequency, ByteOrder order) {
			device = new I2CDevice(controller, deviceAddress, addressSize, frequency, order);
		}

		@Override
		public void setDirections(int port, byte directions) {
		}

		@Override
		public void setValues(int port, byte values) {
			device.writeByte(values);
		}
		
		@Override
		public void close() {
			device.close();
		}
	}
}
