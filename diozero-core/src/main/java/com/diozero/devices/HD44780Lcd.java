package com.diozero.devices;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     HD44780Lcd.java
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

import com.diozero.api.RuntimeIOException;
import com.diozero.util.SleepUtil;

/**
 * <p>
 * LCD with HD44780 controller.<br>
 * Code based on <a href=
 * "http://www.raspberrypi-spy.co.uk/2015/05/using-an-i2c-enabled-lcd-screen-with-the-raspberry-pi/">
 * this Raspberry-Pi Spy article</a>, <a href=
 * "https://bitbucket.org/MattHawkinsUK/rpispy-misc/raw/master/python/lcd_i2c.py">Python
 * code.</a>
 * </p>
 * <p>
 * Another source of information: <a href=
 * "https://gist.github.com/DenisFromHR/cc863375a6e19dce359d">https://gist.github.com/DenisFromHR/cc863375a6e19dce359d</a>.
 * </p>
 * <p>
 * <a href="https://www.sparkfun.com/datasheets/LCD/HD44780.pdf">HD44780
 * Datasheet</a>.
 * </p>
 */
public class HD44780Lcd implements LcdInterface {
	private static final boolean DEFAULT_BACKLIGHT_STATE = true;

	private static final byte DB0 = (byte) (1 << 0);
	private static final byte DB1 = (byte) (1 << 1);
	private static final byte DB2 = (byte) (1 << 2);
	private static final byte DB3 = (byte) (1 << 3);
	private static final byte DB4 = (byte) (1 << 4);
	private static final byte DB5 = (byte) (1 << 5);
	private static final byte DB6 = (byte) (1 << 6);
	private static final byte DB7 = (byte) (1 << 7);

	/*-
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
	 * Read Busy   | 0  | 1  |  BF |  a  |  a  |  a  |  a  |  a  |  a  |  a  | Reads busy flag (BF)         | 0us
	 * Flag and    |    |    |     |     |     |     |     |     |     |     | indicating internal operation|
	 * Address     |    |    |     |     |     |     |     |     |     |     | is being performed and reads |
	 *             |    |    |     |     |     |     |     |     |     |     | address counter contents.    |
	 * ------------+----+----+-----+-----+-----+-----+-----+-----+-----+-----+------------------------------+----------
	 * Write data  | 1  | 0  |  d  |  d  |  d  |  d  |  d  |  d  |  d  |  d  | Writes data into DDRAM or    | 37us
	 * to CG or    |    |    |     |     |     |     |     |     |     |     | CGRAM.                       | tADD=4us
	 * DDRAM       |    |    |     |     |     |     |     |     |     |     |                              |
	 * ------------+----+----+-----+-----+-----+-----+-----+-----+-----+-----+------------------------------+----------
	 * Read data   | 1  | 1  |  d  |  d  |  d  |  d  |  d  |  d  |  d  |  d  | Reads data from DDRAM or     | 37us
	 * from CG or  |    |    |     |     |     |     |     |     |     |     | CGRAM.                       | tADD=4us
	 * DDRAM       |    |    |     |     |     |     |     |     |     |     |                              |
	 * ------------+----+----+-----+-----+-----+-----+-----+-----+-----+-----+------------------------------+----------
	 * DDRAM = Display Data RAM.
	 * CGRAM = Character Generator RAM.
	 */
	/** Clears entire display and sets DDRAM address 0 in address counter. */
	private static final byte INST_CLEAR_DISPLAY = 0x01;
	/**
	 * Sets DDRAM address 0 in address counter. Also returns display from being
	 * shifted to original position. DDRAM contents remain unchanged.
	 */
	private static final byte INST_RETURN_HOME = 0x02;
	/**
	 * Sets cursor move direction and specifies display shift. These operations are
	 * performed during data write and read.
	 */
	private static final byte INST_ENTRY_MODE_SET = 0x04;
	/**
	 * Sets entire display (D) on/off, cursor on/off (C), and blinking of cursor
	 * position character (B).
	 */
	private static final byte INST_DISPLAY_CONTROL = 0x08;
	/** Moves cursor and shifts display without changing DDRAM contents. */
	private static final byte INST_CURSOR_DISPLAY_SHIFT = 0x10;
	/**
	 * Perform the function at the head of the program before executing any
	 * instructions (except for the read busy flag and address instruction). From
	 * this point, the function set instruction cannot be executed unless the
	 * interface data length is changed.
	 */
	private static final byte INST_FUNCTION_SET = 0x20;
	/** Sets CGRAM address. CGRAM data is sent and received after this setting. */
	private static final byte INST_SET_CGRAM_ADDR = 0x40;
	/** Sets DDRAM address. DDRAM data is sent and received after this setting. */
	private static final byte INST_SET_DDRAM_ADDR = (byte) 0x80;

	// Flags for INST_ENTRY_MODE_SET
	/**
	 * Cursor increment/decrement control, 1=increment, 0=decrement. The cursor or
	 * blinking moves to the right when incremented by 1 and to the left when
	 * decremented by 1.
	 */
	private static final byte EMS_CURSOR_INCREMENT = DB1;
	private static final byte EMS_CURSOR_DECREMENT = 0x00;
	/**
	 * Display shift control, 1=on, 0=off. Shifts the entire display either to the
	 * right (I/D = 0) or to the left (I/D = 1) when S is 1. If S is 1, it will seem
	 * as if the cursor does not move but the display does. The display does not
	 * shift if S is 0.
	 */
	private static final byte EMS_DISPLAY_SHIFT_ON = DB0;
	private static final byte EMS_DISPLAY_SHIFT_OFF = 0x00;

	// Flags for INST_DISPLAY_CONTROL
	/** Display on/off, 1=on, 0=off. */
	private static final byte DC_DISPLAY_ON = DB2;
	private static final byte DC_DISPLAY_OFF = 0x00;
	/** Cursor on/off, 1=on, 0=off. */
	private static final byte DC_CURSOR_ON = DB1;
	private static final byte DC_CURSOR_OFF = 0x00;
	/** Cursor blink control, 1=blink, 0=no blink. */
	private static final byte DC_BLINK_ON = DB0;
	private static final byte DC_BLINK_OFF = 0x00;

	// Flags for INST_CURSOR_DISPLAY_SHIFT
	/** Shift either the display or the cursor, 1=display, 0=cursor. */
	private static final byte CDS_DISPLAY_SHIFT = DB3;
	private static final byte CDS_CURSOR_MOVE = 0x00;
	/** Shift the cursor, 1=right, 0=left. */
	private static final byte CDS_SHIFT_RIGHT = DB2;
	private static final byte CDS_SHIFT_LEFT = 0x00;

	// Flags for INST_FUNCTION_SET
	/**
	 * Data is sent or received in 8-bit lengths (DB7 to DB0) when DL is 1, and in
	 * 4-bit lengths (DB7 to DB4) when DL is 0.
	 */
	private static final byte FS_DATA_LENGTH_8BIT = DB4;
	private static final byte FS_DATA_LENGTH_4BIT = 0x00;
	/** Sets the number of display lines. 1=2 lines, 0=1 line. */
	private static final byte FS_DISPLAY_2LINES = DB3;
	private static final byte FS_DISPLAY_1LINE = 0x00;
	/**
	 * Sets the character font. 1=5x10 dots (32 character fonts), 0=5x8 dots (208
	 * character fonts). For some 1 line displays you can select a 10 pixel high
	 * font. Cannot select 2 lines with a 5x10 font.
	 */
	private static final byte FS_CHAR_FONT_5X10DOTS = DB2;
	private static final byte FS_CHAR_FONT_5X8DOTS = 0x00;

	private static final byte BUSY_FLAG = DB7;

	// For 2-row LCDs
	private static final byte[] ROW_OFFSETS_2ROWS = { 0x00, 0x40 };
	// For 20x4 LCDs
	private static final byte[] ROW_OFFSETS_20x4 = { 0x00, 0x40, 20, 0x40 + 20 };
	// For 16x4 LCDs - special memory map layout
	private static final byte[] ROW_OFFSETS_16x4 = { 0x00, 0x40, 16, 0x40 + 16 };

	private final LcdConnection lcdConnection;
	private final boolean dataInHighNibble;
	private final int registerSelectDataMask;
	private final int dataReadMask;
	private final int enableMask;
	private final int backlightOnMask;
	private boolean backlightEnabled;
	private final int columns;
	private final int rows;
	private final boolean characterFont5x8;
	private boolean displayOn;
	private boolean cursorEnabled;
	private boolean blinkEnabled;
	private boolean increment;
	private boolean shiftDisplay;
	private byte[] rowOffsets;

	public HD44780Lcd(LcdConnection lcdConnection, int columns, int rows) {
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

		this.columns = columns;
		this.rows = rows;
		backlightEnabled = DEFAULT_BACKLIGHT_STATE;
		characterFont5x8 = true;

		this.lcdConnection = lcdConnection;
		dataInHighNibble = lcdConnection.isDataInHighNibble();
		registerSelectDataMask = 1 << lcdConnection.getRegisterSelectBit();
		dataReadMask = 1 << lcdConnection.getDataReadWriteBit();
		enableMask = 1 << lcdConnection.getEnableBit();
		backlightOnMask = 1 << lcdConnection.getBacklightBit();

		// Initialise the display. From p45/46 of the datasheet:
		// https://www.sparkfun.com/datasheets/LCD/HD44780.pdf
		// If the power supply conditions for correctly operating the internal reset
		// circuit are not met, initialisation by instructions becomes necessary.
		// Need to do this 3 times for the 4-bit interface (p46)

		/*
		 * From p39 (4-bit operation, 8-digit × 1-line display with internal reset):
		 *
		 * The program must set all functions prior to the 4-bit operation (Table 12).
		 * When the power is turned on, 8-bit operation is automatically selected and
		 * the first write is performed as an 8-bit operation. Since DB0 to DB3 are not
		 * connected, a rewrite is then required. However, since one operation is
		 * completed in two accesses for 4-bit operation, a rewrite is needed to set the
		 * functions (see Table 12). Thus, DB4 to DB7 of the function set instruction is
		 * written twice.
		 */
		// Function set (Interface is 8 bits long).
		write4Bits(true, (byte) (INST_FUNCTION_SET | FS_DATA_LENGTH_8BIT));
		// Wait for more than 4.1 ms
		SleepUtil.sleepMillis(5);
		// Function set (Interface is 8 bits long).
		write4Bits(true, (byte) (INST_FUNCTION_SET | FS_DATA_LENGTH_8BIT));
		// Wait for more than 100us
		// SleepUtil.sleepMicros(100);
		SleepUtil.busySleep(110_000);
		// Function set (Interface is 8 bits long).
		write4Bits(true, (byte) (INST_FUNCTION_SET | FS_DATA_LENGTH_8BIT));
		// Now set it to 4-bit mode
		write4Bits(true, (byte) (INST_FUNCTION_SET | FS_DATA_LENGTH_4BIT));

		// Function set: 4-bit data length, lines & character font as requested
		writeInstruction(
				(byte) (INST_FUNCTION_SET | FS_DATA_LENGTH_4BIT | (rows == 1 ? FS_DISPLAY_1LINE : FS_DISPLAY_2LINES)
						| (characterFont5x8 ? FS_CHAR_FONT_5X8DOTS : FS_CHAR_FONT_5X10DOTS)));
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
		// High nibble first
		write4Bits(instruction, (byte) (data & 0xF0));
		// Then the low nibble
		write4Bits(instruction, (byte) (data << 4));
	}

	private void write4Bits(boolean instruction, byte value) {
		byte data;
		if (dataInHighNibble) {
			data = (byte) (value & 0xF0);
		} else {
			data = (byte) ((value >> 4) & 0x0F);
		}
		data |= (byte) (instruction ? 0 : registerSelectDataMask) | (backlightEnabled ? backlightOnMask : 0);

		lcdConnection.write((byte) (data | enableMask));
		// 50us delay enough?
		// SleepUtil.sleepMicros(50);
		SleepUtil.busySleep(50_000);
		lcdConnection.write(data);
		// 50us delay enough?
		// SleepUtil.sleepMicros(50);
		SleepUtil.busySleep(50_000);
	}

	public int getColumnCount() {
		return columns;
	}

	public int getRowCount() {
		return rows;
	}

	public boolean isBacklightEnabled() {
		return backlightEnabled;
	}

	public HD44780Lcd setBacklightEnabled(boolean backlightEnabled) {
		this.backlightEnabled = backlightEnabled;
		writeByte(true, (byte) 0);

		return this;
	}

	public HD44780Lcd setCursorPosition(int column, int row) {
		columnCheck(column);
		rowCheck(row);

		writeInstruction((byte) (INST_SET_DDRAM_ADDR | (column + rowOffsets[row])));

		return this;
	}

	public HD44780Lcd addText(char character) {
		writeData((byte) character);

		return this;
	}

	public HD44780Lcd addText(int code) {
		writeData((byte) code);

		return this;
	}

	public HD44780Lcd clear() {
		writeInstruction(INST_CLEAR_DISPLAY);
		// Seem to have to wait after clearing the display, encounter strange errors
		// otherwise
		SleepUtil.sleepMillis(1);

		return this;
	}

	public HD44780Lcd returnHome() {
		writeInstruction(INST_RETURN_HOME);

		return this;
	}

	public HD44780Lcd entryModeControl(boolean increment, boolean shiftDisplay) {
		this.increment = increment;
		this.shiftDisplay = shiftDisplay;
		writeInstruction((byte) (INST_ENTRY_MODE_SET | (increment ? EMS_CURSOR_INCREMENT : EMS_CURSOR_DECREMENT)
				| (shiftDisplay ? EMS_DISPLAY_SHIFT_ON : EMS_DISPLAY_SHIFT_OFF)));

		return this;
	}

	public HD44780Lcd autoscrollOn() {
		entryModeControl(displayOn, true);
		return this;
	}

	public HD44780Lcd autoscrollOff() {
		entryModeControl(displayOn, false);
		return this;
	}

	public boolean isIncrementOn() {
		return increment;
	}

	public boolean isShiftDisplayOn() {
		return shiftDisplay;
	}

	public HD44780Lcd displayControl(boolean displayOn, boolean cursorEnabled, boolean blinkEnabled) {
		this.displayOn = displayOn;
		this.cursorEnabled = cursorEnabled;
		this.blinkEnabled = blinkEnabled;
		writeInstruction((byte) (INST_DISPLAY_CONTROL | (displayOn ? DC_DISPLAY_ON : DC_DISPLAY_OFF)
				| (cursorEnabled ? DC_CURSOR_ON : DC_CURSOR_OFF) | (blinkEnabled ? DC_BLINK_ON : DC_BLINK_OFF)));

		return this;
	}

	public HD44780Lcd displayOn() {
		return displayControl(true, cursorEnabled, blinkEnabled);
	}

	public HD44780Lcd displayOff() {
		return displayControl(false, cursorEnabled, blinkEnabled);
	}

	public HD44780Lcd cursorOn() {
		return displayControl(displayOn, true, blinkEnabled);
	}

	public HD44780Lcd cursorOff() {
		// also turns off "blink" as that makes the cursor position still visible if enabled
		return displayControl(displayOn, false, false);
	}

	public HD44780Lcd blinkOn() {
		return displayControl(displayOn, true, true);
	}

	public HD44780Lcd blinkOff() {
		return displayControl(displayOn, cursorEnabled, false);
	}

	public boolean isCursorEnabled() {
		return cursorEnabled;
	}

	public boolean isBlinkEnabled() {
		return blinkEnabled;
	}


	public HD44780Lcd cursorOrDisplayShift(boolean displayShift, boolean shiftRight) {
		writeInstruction((byte) (INST_CURSOR_DISPLAY_SHIFT | (displayShift ? CDS_DISPLAY_SHIFT : CDS_CURSOR_MOVE)
				| (shiftRight ? CDS_SHIFT_RIGHT : CDS_SHIFT_LEFT)));

		return this;
	}

	public HD44780Lcd shiftDisplayRight() {
		cursorOrDisplayShift(true, true);

		return this;
	}

	public HD44780Lcd shiftDisplayLeft() {
		cursorOrDisplayShift(true, false);

		return this;
	}

	public HD44780Lcd moveCursorRight() {
		cursorOrDisplayShift(false, true);

		return this;
	}

	public HD44780Lcd moveCursorLeft() {
		cursorOrDisplayShift(false, false);

		return this;
	}

	public HD44780Lcd createChar(int location, byte[] charMap) {
		/*
		 * In the character generator RAM, the user can rewrite character patterns by
		 * program. For 5x8 dots, eight character patterns can be written, and for 5x10
		 * dots, four character patterns can be written.
		 */
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

		writeInstruction((byte) (INST_SET_CGRAM_ADDR | (location << 3)));
		// SleepUtil.sleepMicros(100);
		SleepUtil.busySleep(100_000);

		for (int i = 0; i < charMap.length; i++) {
			writeData((byte) (charMap[i] & 0b11111));
			// SleepUtil.sleepMicros(100);
			SleepUtil.busySleep(100_000);
		}

		return this;
	}

	@Override
	public void close() throws RuntimeIOException {
		backlightEnabled = false;
		clear();
		displayControl(false, false, false);
	}
}
