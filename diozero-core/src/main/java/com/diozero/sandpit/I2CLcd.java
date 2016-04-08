package com.diozero.sandpit;

import java.io.Closeable;
import java.io.IOException;

import com.diozero.api.I2CConstants;
import com.diozero.api.I2CDevice;
import com.diozero.util.SleepUtil;

/**
 * Generic I2C LCD support, code based on <a href=
 * "http://www.raspberrypi-spy.co.uk/2015/05/using-an-i2c-enabled-lcd-screen-with-the-raspberry-pi/">
 * this Raspberry-Pi Spy article</a>, Python code: <a href=
 * "https://bitbucket.org/MattHawkinsUK/rpispy-misc/raw/master/python/lcd_i2c.py">
 * https://bitbucket.org/MattHawkinsUK/rpispy-misc/raw/master/python/lcd_i2c.py
 * </a>
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

	private static final int LCD_BACKLIGHT_BIT = 0x08; // On

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
		lcdWriteByte(Mode.COMMAND, (byte) 0x33); // 110011 Initialise
		lcdWriteByte(Mode.COMMAND, (byte) 0x32); // 110010 Initialise
		lcdWriteByte(Mode.COMMAND, (byte) 0x06); // 000110 Cursor move direction (right)
		lcdWriteByte(Mode.COMMAND, (byte) 0x0C); // 001100 Display On, Cursor Off, Blink Off
		lcdWriteByte(Mode.COMMAND, (byte) 0x28); // 101000 Data length, number of lines, font size
		lcdWriteByte(Mode.COMMAND, (byte) 0x01); // 000001 Clear display

		SleepUtil.sleepSeconds(E_DELAY);
	}

	// Send byte to data pins
	private void lcdWriteByte(Mode mode, byte data) {
		byte bits_high = (byte) (mode.getMode() | (data & 0xF0) | (backlight ? LCD_BACKLIGHT_BIT : 0));
		byte bits_low = (byte) (mode.getMode() | ((data << 4) & 0xF0) | (backlight ? LCD_BACKLIGHT_BIT : 0));

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
	 * @param line Line number
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
		lcdWriteByte(Mode.COMMAND, (byte) 0x01);
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
