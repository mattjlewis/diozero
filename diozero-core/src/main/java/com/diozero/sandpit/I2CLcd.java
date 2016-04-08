package com.diozero.sandpit;

import java.io.Closeable;
import java.io.IOException;

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
 */
public class I2CLcd implements Closeable {

	// I2C device address
	private static final int DEVICE_ADDRESS = 0x27;

	private static final byte[] LCD_LINE_ADDRESSES = {
			(byte) 0x80, // LCD RAM address for the 1st line
			(byte) 0xC0, // LCD RAM address for the 2nd line
			(byte) 0x94, // LCD RAM address for the 3rd line
			(byte) 0xD4  // LCD RAM address for the 4th line
	};
	
	// Commands
	private static final byte LCD_CLEAR_DISPLAY = 0x01;
	private static final byte LCD_RETURN_HOME = 0x02;
	private static final byte LCD_SET_CGRAM_ADDR = 0x40;
	private static final byte LCD_SET_DDRAM_ADDR = (byte)0x80;
	
	private static final byte LCD_ENTRY_MODE_SET = 0x04;
	// Flags for display entry mode
	private static final byte LCD_ENTRY_RIGHT = 0x00;
	private static final byte LCD_ENTRY_LEFT = 0x02;
	private static final byte LCD_ENTRY_SHIFT_INCREMENT = 0x01;
	private static final byte LCD_ENTRY_SHIFT_DECREMENT = 0x00;

	private static final byte LCD_DISPLAY_CONTROL = 0x08;
	// Flags for display on/off control
	private static final byte LCD_DISPLAY_ON = 0x04;
	private static final byte LCD_DISPLAY_OFF = 0x00;
	private static final byte LCD_CURSOR_ON = 0x02;
	private static final byte LCD_CURSOR_OFF = 0x00;
	private static final byte LCD_BLINK_ON = 0x01;
	private static final byte LCD_BLINK_OFF = 0x00;

	private static final byte LCD_CURSOR_SHIFT = 0x10;
	// Flags for display/cursor shift
	private static final byte LCD_DISPLAY_MOVE = 0x08;
	private static final byte LCD_CURSOR_MOVE = 0x00;
	private static final byte LCD_MOVE_RIGHT = 0x04;
	private static final byte LCD_MOVE_LEFT = 0x00;

	private static final byte LCD_FUNCTION_SET = 0x20;
	// Flags for function set
	private static final byte LCD_8BIT_MODE = 0x10;
	private static final byte LCD_4BIT_MODE = 0x00;
	private static final byte LCD_2LINE = 0x08;
	private static final byte LCD_1LINE = 0x00;
	private static final byte LCD_5x10_DOTS = 0x04;
	private static final byte LCD_5x8_DOTS = 0x00;
	
	// Flags for backlight control
	private static final int LCD_BACKLIGHT_ON_BIT = 0x08;
	private static final int LCD_BACKLIGHT_OFF_BIT = 0x00;

	// Enable bit
	private static final byte ENABLE = 0b00000100;

	// Timing constants
	private static final double E_PULSE = 0.0005;
	private static final double E_DELAY = 0.0005;

	private I2CDevice device;
	private boolean backlight;
	private int rows;
	private int columns;

	public I2CLcd(int rows, int columns) {
		this(I2CConstants.BUS_1, DEVICE_ADDRESS, rows, columns);
	}

	public I2CLcd(int controller, int deviceAddress, int rows, int columns) {
		if (rows < 1 || rows > LCD_LINE_ADDRESSES.length) {
			throw new IllegalArgumentException(
					"Invalid number of rows (" + rows + "), must be 1.." + LCD_LINE_ADDRESSES.length);
		}

		this.rows = rows;
		this.columns = columns;

		device = new I2CDevice(controller, deviceAddress, I2CConstants.ADDR_SIZE_7,
				I2CConstants.DEFAULT_CLOCK_FREQUENCY);

		// Initialise display
		// TODO Understand these commands using the constants above
		lcdWriteByte(Mode.COMMAND, (byte) 0x33);		// 110011 Initialise
		lcdWriteByte(Mode.COMMAND, (byte) 0x32);		// 110010 Initialise
		lcdWriteByte(Mode.COMMAND, (byte) 0x06);		// 000110 Cursor move direction (right)
		lcdWriteByte(Mode.COMMAND, (byte) 0x0C);		// 001100 Display On, Cursor Off, Blink Off
		lcdWriteByte(Mode.COMMAND, (byte) 0x28);		// 101000 Data length, number of lines, font size
		lcdWriteByte(Mode.COMMAND, LCD_CLEAR_DISPLAY);	// 000001 Clear display

		SleepUtil.sleepSeconds(E_DELAY);
	}

	// Send byte to data pins
	private void lcdWriteByte(Mode mode, byte data) {
		byte bits_high = (byte) (mode.getMode() | (data & 0xF0) | (backlight ? LCD_BACKLIGHT_ON_BIT : LCD_BACKLIGHT_OFF_BIT));
		byte bits_low = (byte) (mode.getMode() | ((data << 4) & 0xF0) | (backlight ? LCD_BACKLIGHT_ON_BIT : LCD_BACKLIGHT_OFF_BIT));

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

	public int getRowCount() {
		return rows;
	}

	public int getColumnCount() {
		return columns;
	}

	/**
	 * Send string to display
	 * @param line Line number (starts at 0)
	 * @param text Text to display
	 */
	public void setText(int line, String text) {
		if (line < 0 || line >= rows) {
			throw new IllegalArgumentException("Invalid line (" + line + "), must be 0.." + (rows - 1));
		}

		String str = pad(text, columns);

		lcdWriteByte(Mode.COMMAND, LCD_LINE_ADDRESSES[line]);

		for (int i = 0; i < columns; i++) {
			lcdWriteByte(Mode.DATA, (byte) str.charAt(i));
		}
	}

	/**
	 * Clear the display
	 */
	public void clear() {
		lcdWriteByte(Mode.COMMAND, LCD_CLEAR_DISPLAY);
		lcdWriteByte(Mode.COMMAND, LCD_RETURN_HOME);
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
