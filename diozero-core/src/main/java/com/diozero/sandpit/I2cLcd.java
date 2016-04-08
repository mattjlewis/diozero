package com.diozero.sandpit;

import java.io.Closeable;
import java.io.IOException;

import com.diozero.api.I2CConstants;
import com.diozero.api.I2CDevice;
import com.diozero.util.SleepUtil;

public class I2cLcd implements Closeable {
	
	// I2C device address
	private static final int DEVICE_ADDRESS = 0x27;
	// Maximum characters per line
	private static final int LCD_WIDTH = 16;

	// Define some device constants
	private static final int LCD_CHR = 1; // Mode - Sending data
	private static final int LCD_CMD = 0; // Mode - Sending command

	private static final byte LCD_LINE_1 = (byte)0x80; // LCD RAM address for the 1st line
	private static final byte LCD_LINE_2 = (byte)0xC0; // LCD RAM address for the 2nd line
	private static final byte LCD_LINE_3 = (byte)0x94; // LCD RAM address for the 3rd line
	private static final byte LCD_LINE_4 = (byte)0xD4; // LCD RAM address for the 4th line

	private static final int LCD_BACKLIGHT  = 0x08; // On
	//private static final int LCD_BACKLIGHT = 0x00; // Off

	// Enable bit
	private static final byte ENABLE = 0b00000100;

	// Timing constants
	private static final double E_PULSE = 0.0005;
	private static final double E_DELAY = 0.0005;
	
	private I2CDevice device;
	
	public I2cLcd() {
		this(I2CConstants.BUS_1, DEVICE_ADDRESS);
	}
	
	public I2cLcd(int controller, int deviceAddress) {
		device = new I2CDevice(controller, deviceAddress, I2CConstants.ADDR_SIZE_7,
				I2CConstants.DEFAULT_CLOCK_FREQUENCY);
		
		// Initialise display
		lcdByte((byte)0x33, LCD_CMD); // 110011 Initialise
		lcdByte((byte)0x32, LCD_CMD); // 110010 Initialise
		lcdByte((byte)0x06, LCD_CMD); // 000110 Cursor move direction
		lcdByte((byte)0x0C, LCD_CMD); // 001100 Display On, Cursor Off, Blink Off 
		lcdByte((byte)0x28, LCD_CMD); // 101000 Data length, number of lines, font size
		lcdByte((byte)0x01, LCD_CMD); // 000001 Clear display
		SleepUtil.sleepSeconds(E_DELAY);
	}
	
	private void lcdByte(byte bits, int mode) {
		// Send byte to data pins
		// bits = the data
		// mode = 1 for data
		//        0 for command

		byte bits_high = (byte)(mode | (bits & 0xF0) | LCD_BACKLIGHT);
		byte bits_low = (byte)(mode | ((bits<<4) & 0xF0) | LCD_BACKLIGHT);

		// High bits
		device.writeByte(bits_high);
		lcdToggleEnable(bits_high);

		// Low bits
		device.writeByte(bits_low);
		lcdToggleEnable(bits_low);
	}
	
	private void lcdToggleEnable(byte bits) {
		// Toggle enable
		SleepUtil.sleepSeconds(E_DELAY);
		device.writeByte((byte)(bits | ENABLE));
		SleepUtil.sleepSeconds(E_PULSE);
		device.writeByte((byte)(bits & ~ENABLE));
		SleepUtil.sleepSeconds(E_DELAY);
	}
	
	// Send string to display
	public void lcdString(String message, byte line) {
		message = ljust(LCD_WIDTH, " ");

		lcdByte(line, LCD_CMD);

		for (int i=0; i<LCD_WIDTH; i++) {
			lcdByte((byte)message.charAt(i), LCD_CHR);
		}
	}

	// Main program block
	public static void main(String[] args) {
		// Initialise display
		I2cLcd lcd = new I2cLcd();

		while (true) {
			// Send some text
			lcd.lcdString("RPiSpy         <", LCD_LINE_1);
			lcd.lcdString("I2C LCD        <", LCD_LINE_2);

			SleepUtil.sleepSeconds(3);
		  
			// Send some more text
			lcd.lcdString(">         RPiSpy", LCD_LINE_1);
			lcd.lcdString(">        I2C LCD", LCD_LINE_2);

			SleepUtil.sleepSeconds(3);
		}
	}
	
	public static String ljust(int width, String str) {
		return String.format("%1$-" + width + "s", str).substring(0, width);
	}

	@Override
	public void close() throws IOException {
		lcdByte((byte)0x01, LCD_CMD);
		device.close();
	}
}
