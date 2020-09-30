package com.diozero.example;

import org.tinylog.Logger;

import com.diozero.api.I2CConstants;
import com.diozero.devices.HD44780Lcd;
import com.diozero.devices.HD44780Lcd.LcdConnection;
import com.diozero.devices.HD44780Lcd.PCF8574LcdConnection;
import com.diozero.util.DeviceFactoryHelper;
import com.diozero.util.RuntimeIOException;
import com.diozero.util.SleepUtil;

/**
 * I2C LCD sample application. To run:
 *  {@code java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-example-0.1.jar com.diozero.example.LcdApp [i2c_address] [i2c_controller]}</li>
 */
public class LcdApp {
	// Main program block
	public static void main(String[] args) {
		int device_address = HD44780Lcd.PCF8574LcdConnection.DEFAULT_DEVICE_ADDRESS;
		if (args.length > 0) {
			device_address = Integer.decode(args[0]).intValue();
		}
		int controller = I2CConstants.BUS_1;
		if (args.length > 1) {
			controller = Integer.parseInt(args[1]);
		}
		
		int columns = 16;
		int rows = 2;
		
		// Initialise display
		try (LcdConnection lcd_connection = new PCF8574LcdConnection(controller, device_address);
				HD44780Lcd lcd = new HD44780Lcd(lcd_connection, columns, rows)) {
			LcdApp.test(lcd);
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
		} finally {
			// Required if there are non-daemon threads that will prevent the
			// built-in clean-up routines from running
			DeviceFactoryHelper.getNativeDeviceFactory().close();
		}
	}
	
	public static void test(HD44780Lcd lcd) {
		lcd.setBacklightEnabled(true);
		
		/*Logger.info("Calling setText");
		lcd.setText(0, "Hello World!");
		SleepUtil.sleepSeconds(2);*/
		
		// 0, 14, 21, 31, 10, 4, 10, 17
		byte[] space_invader = HD44780Lcd.Characters.get("space_invader");
		byte[] smilie = HD44780Lcd.Characters.get("smilie");
		byte[] frownie = HD44780Lcd.Characters.get("frownie");
		lcd.createChar(0, space_invader);
		lcd.createChar(1, smilie);
		lcd.createChar(2, frownie);
		lcd.clear();

		Logger.info("Adding text");
		lcd.setCursorPosition(0, 0);
		lcd.addText('H');
		lcd.addText('e');
		lcd.addText('l');
		lcd.addText('l');
		lcd.addText('o');
		lcd.addText(' ');
		lcd.addText(' ');
		lcd.addText(0);
		lcd.addText(1);
		lcd.addText(2);
		lcd.setCursorPosition(0, 1);
		lcd.addText('W');
		lcd.addText('o');
		lcd.addText('r');
		lcd.addText('l');
		lcd.addText('d');
		lcd.addText('!');
		lcd.addText(' ');
		lcd.addText(0);
		lcd.addText(1);
		lcd.addText(2);
		SleepUtil.sleepSeconds(2);
		lcd.clear();
		
		lcd.createChar(3, HD44780Lcd.Characters.get("runninga"));
		lcd.createChar(4, HD44780Lcd.Characters.get("runningb"));
		lcd.clear();
		lcd.displayControl(true, false, false);
		for (int i=0; i<40; i++) {
			lcd.setCursorPosition(0, 0);
			lcd.addText(3);
			SleepUtil.sleepMillis(100);
			lcd.setCursorPosition(0, 0);
			lcd.addText(4);
			SleepUtil.sleepMillis(100);
		}
		SleepUtil.sleepSeconds(1);
		lcd.displayControl(true, true, true);
		lcd.clear();
		
		for (int i=0; i<4; i++) {
			// Send some text
			lcd.setText(0, "Hello -         ");
			lcd.setText(1, "World! " + i);
			SleepUtil.sleepSeconds(0.5);
			
			lcd.clear();

			// Send some more text
			lcd.setText(0, ">        diozero");
			lcd.setText(1, ">    HD44780 LCD");
			SleepUtil.sleepSeconds(0.5);
		}
		
		SleepUtil.sleepSeconds(1);
		lcd.clear();
		
		for (byte b : "Hello Matt!".getBytes()) {
			lcd.addText(b);
			SleepUtil.sleepSeconds(.2);
		}
		
		SleepUtil.sleepSeconds(1);
		lcd.clear();

		int x=0;
		for (int i=0; i<3; i++) {
			for (byte b : "Hello World! ".getBytes()) {
				if (x++ == lcd.getColumnCount()) {
					lcd.entryModeControl(true, true);
				}
				lcd.addText(b);
				SleepUtil.sleepSeconds(.1);
			}
		}
		SleepUtil.sleepSeconds(1);
		lcd.clear();
		lcd.entryModeControl(true, false);
		
		lcd.setCursorPosition(0, 0);
		lcd.addText('H');
		lcd.addText('e');
		lcd.addText('l');
		lcd.addText('l');
		lcd.addText('o');
		lcd.addText(' ');
		lcd.addText(' ');
		lcd.addText(0);
		lcd.addText(1);
		lcd.addText(2);
		lcd.setCursorPosition(0, 1);
		lcd.addText('W');
		lcd.addText('o');
		lcd.addText('r');
		lcd.addText('l');
		lcd.addText('d');
		lcd.addText('!');
		lcd.addText(' ');
		lcd.addText(0);
		lcd.addText(1);
		lcd.addText(2);
		
		Logger.info("Sleeping for 4 seconds...");
		SleepUtil.sleepSeconds(4);
		
		lcd.clear();
	}
}
