package com.diozero.sandpit;

import java.io.Closeable;
import java.io.IOException;

import org.pmw.tinylog.Logger;

import com.diozero.api.I2CConstants;
import com.diozero.api.I2CDevice;
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
 */
public class I2CLcd implements Closeable {
	private static final boolean DEFAULT_BACKLIGHT_STATE = true;

	// I2C device address
	private static final int DEVICE_ADDRESS = 0x27;

	private static final byte[] LINE_ADDRESSES = {
			(byte) 0x80, // LCD RAM address for the 1st line
			(byte) 0xC0, // LCD RAM address for the 2nd line
			(byte) 0x94, // LCD RAM address for the 3rd line
			(byte) 0xD4  // LCD RAM address for the 4th line
	};
	
	// Instructions
	private static final byte INST_CLEAR_DISPLAY = 0x01;
	private static final byte INST_RETURN_HOME = 0x02;
	private static final byte INST_ENTRY_MODE_SET = 0x04;
	private static final byte INST_DISPLAY_CONTROL = 0x08;
	private static final byte INST_CURSOR_SHIFT = 0x10;
	/** Perform the function at the head of the program before executing any instructions (except for the
	 * read  busy  flag  and  address  instruction).  From  this  point,  the  function  set  instruction  cannot  be
	 * executed unless the interface data length is changed. */
	private static final byte INST_FUNCTION_SET = 0x20;
	private static final byte INST_SET_CGRAM_ADDR = 0x40;		// CGRAM = Character Generator RAM
	private static final byte INST_SET_DDRAM_ADDR = (byte)0x80;	// DDRAM = Display Data RAM
	
	// Flags for INST_ENTRY_MODE_SET
	/** Display shift control, 1=left, 0=right.
	 * If S is 1, it will seem as if the cursor does not move but the display does.
	 * The display does not shift if S is 0. */
	private static final byte ENTRY_MODE_SHIFT_LEFT = 0x02;
	/** Cursor increment/decrement control, 1=increment, 0=decrement.
	 * The cursor or blinking moves to the right when incremented by 1 and to the left when decremented by 1 */
	private static final byte ENTRY_MODE_INCREMENT = 0x01;

	// Flags for INST_DISPLAY_CONTROL
	/** Display on/off, 1=on, 0=off. */
	private static final byte DISPLAY_CONTROL_DISPLAY_ON = 0x04;
	/** Cursor on/off, 1=on, 0=off. */
	private static final byte DISPLAY_CONTROL_CURSOR_ON = 0x02;
	/** Cursor blink control, 1=blink, 0=no blink. */
	private static final byte DISPLAY_CONTROL_BLINK_ON = 0x01;

	// Flags for INST_CURSOR_SHIFT
	/** Shift the displayed text, 1=right, 0=left. */
	private static final byte CURSOR_SHIFT_DISPLAY_SHIFT_RIGHT = 0x08;
	/** Shift the cursor, 1=right, 0=left. */
	private static final byte CURSOR_SHIFT_CURSOR_SHIFT_RIGHT = 0x04;

	// Flags for INST_FUNCTION_SET
	/** Data is sent or received in 8-bit lengths (DB7 to DB0) when DL is 1,
	 * and  in  4-bit  lengths  (DB7  to  DB4)  when  DL  is  0. */
	private static final byte FUNCTION_SET_DATA_LENGTH_8BIT = 0x10;
	/** Sets the number of display lines. 1=2 lines, 0=1 line. */
	private static final byte FUNCTION_SET_DISPLAY_LINES = 0x08;
	/** Sets the character font. 1=5x10 dots (32 character fonts), 0=5x8 dots (208 character fonts) */
	private static final byte FUNCTION_SET_CHAR_FONT = 0x04;
	
	// Backlight control bit (1=on, 0=off)
	private static final int BACKLIGHT_CONTROL = 0b00001000;

	// Enable bit
	private static final byte ENABLE = 0b00000100;

	// Timing constants
	private static final double E_PULSE = 0.0005;
	private static final double E_DELAY = 0.0005;

	private I2CDevice device;
	private boolean backlight;
	private int rows;
	private int columns;

	public I2CLcd(int columns, int rows) {
		this(I2CConstants.BUS_1, DEVICE_ADDRESS, columns, rows);
	}

	public I2CLcd(int controller, int deviceAddress, int columns, int rows) {
		if (rows < 1 || rows > LINE_ADDRESSES.length) {
			throw new IllegalArgumentException(
					"Invalid number of rows (" + rows + "), must be 1.." + LINE_ADDRESSES.length);
		}

		this.columns = columns;
		this.rows = rows;
		backlight = DEFAULT_BACKLIGHT_STATE;

		device = new I2CDevice(controller, deviceAddress, I2CConstants.ADDR_SIZE_7,
				I2CConstants.DEFAULT_CLOCK_FREQUENCY);

		// Initialise display
		// 110011 Initialise, 8-bit (INST_FUNCTION_SET | FUNCTION_SET_DATA_LENGTH | 0b11)
		//lcdWriteByte(Mode.COMMAND, (byte) 0x33);
		// 110010 Initialise, 8-bit (INST_FUNCTION_SET | FUNCTION_SET_DATA_LENGTH | 0b10)
		//lcdWriteByte(Mode.COMMAND, (byte) 0x32);
		// 101000 Function set: 4-bit data length, 2 lines, 5x8 character font
		lcdWriteByte(Mode.COMMAND, (byte) (INST_FUNCTION_SET | FUNCTION_SET_DISPLAY_LINES));
		// 001100 Display On, Cursor on, Blink on
		lcdWriteByte(Mode.COMMAND, (byte) (INST_DISPLAY_CONTROL | DISPLAY_CONTROL_DISPLAY_ON | DISPLAY_CONTROL_CURSOR_ON | DISPLAY_CONTROL_BLINK_ON));
		// 000110 Display shift to left, decrement cursor
		lcdWriteByte(Mode.COMMAND, (byte) (INST_ENTRY_MODE_SET | ENTRY_MODE_SHIFT_LEFT));
		// 000001 Clear display
		lcdWriteByte(Mode.COMMAND, INST_CLEAR_DISPLAY);

		SleepUtil.sleepSeconds(E_DELAY);
	}

	// Send byte to data pins
	private void lcdWriteByte(Mode mode, byte data) {
		byte bits_high = (byte) (mode.getMode() | (data & 0xF0) | (backlight ? BACKLIGHT_CONTROL : 0));
		byte bits_low = (byte) (mode.getMode() | ((data << 4) & 0xF0) | (backlight ? BACKLIGHT_CONTROL : 0));

		// High bits
		device.writeByte(bits_high);
		lcdToggleEnable(bits_high);

		// Low bits
		device.writeByte(bits_low);
		lcdToggleEnable(bits_low);
	}

	// Toggle enable
	private void lcdToggleEnable(byte bits) {
		SleepUtil.sleepSeconds(E_DELAY);
		device.writeByte((byte) (bits | ENABLE));
		SleepUtil.sleepSeconds(E_PULSE);
		device.writeByte((byte) (bits & ~ENABLE));
		SleepUtil.sleepSeconds(E_DELAY);
	}

	public boolean isBacklightOn() {
		return backlight;
	}

	public void setBacklightOn(boolean backlight) {
		this.backlight = backlight;
	}

	public int getColumnCount() {
		return columns;
	}

	public int getRowCount() {
		return rows;
	}

	/**
	 * Send string to display
	 * @param row Row number (starts at 0)
	 * @param text Text to display
	 */
	public void setText(int row, String text) {
		if (row < 0 || row >= rows) {
			throw new IllegalArgumentException("Invalid row (" + row + "), must be 0.." + (rows - 1));
		}

		String str = pad(text, columns);

		lcdWriteByte(Mode.COMMAND, LINE_ADDRESSES[row]);

		for (byte b : str.getBytes()) {
			lcdWriteByte(Mode.DATA, b);
		}
	}

	/**
	 * Clear the display
	 */
	public void clear() {
		lcdWriteByte(Mode.COMMAND, INST_CLEAR_DISPLAY);
	}

	@Override
	public void close() throws IOException {
		clear();
		device.close();
	}

	static enum Mode {
		/** Sending command */
		COMMAND(0),
		/** Sending data */
		DATA(1);

		private int mode;

		private Mode(int mode) {
			this.mode = mode;
		}

		public int getMode() {
			return mode;
		}
	}

	public static String pad(String str, int length) {
		return String.format("%1$-" + length + "s", str).substring(0, length);
	}
}
