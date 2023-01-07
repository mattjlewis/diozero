package com.diozero.sampleapps.lcd;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     I2CLcdSampleApp20x4.java
 *
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2022 diozero
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

import org.tinylog.Logger;

import com.diozero.api.I2CConstants;
import com.diozero.api.RuntimeIOException;
import com.diozero.devices.HD44780Lcd;
import com.diozero.devices.LcdConnection;
import com.diozero.util.SleepUtil;

/**
 * HD44780 controlled LCD sample application. To run:
 * <ul>
 * <li>sysfs:<br>
 *  {@code java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar com.diozero.sampleapps.I2CLcdSampleApp20x4 [i2c_address] [i2c_controller]}</li>
 * <li>JDK Device I/O 1.0:<br>
 *  {@code sudo java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-jdkdio10-$DIOZERO_VERSION.jar:dio-1.0.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.I2CLcdSampleApp20x4 [i2c_address] [i2c_controller]}</li>
 * <li>JDK Device I/O 1.1:<br>
 *  {@code sudo java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-jdkdio11-$DIOZERO_VERSION.jar:dio-1.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.I2CLcdSampleApp20x4 [i2c_address] [i2c_controller]}</li>
 * <li>Pi4j:<br>
 *  {@code sudo java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-pi4j-$DIOZERO_VERSION.jar:pi4j-core-1.2.jar com.diozero.sampleapps.I2CLcdSampleApp20x4 [i2c_address] [i2c_controller]}</li>
 * <li>wiringPi:<br>
 *  {@code sudo java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-wiringpi-$DIOZERO_VERSION.jar:pi4j-core-1.2.jar com.diozero.sampleapps.I2CLcdSampleApp20x4 [i2c_address] [i2c_controller]}</li>
 * <li>pigpgioJ:<br>
 *  {@code sudo java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-pigpio-$DIOZERO_VERSION.jar:pigpioj-java-2.4.jar com.diozero.sampleapps.I2CLcdSampleApp20x4 [i2c_address] [i2c_controller]}</li>
 * </ul>
 */
public class I2CLcdSampleApp20x4 {
	// Main program block
	public static void main(String[] args) {
		int device_address = LcdConnection.PCF8574LcdConnection.DEFAULT_DEVICE_ADDRESS;
		if (args.length > 0) {
			device_address = Integer.decode(args[0]).intValue();
		}
		int controller = I2CConstants.CONTROLLER_1;
		if (args.length > 1) {
			controller = Integer.parseInt(args[1]);
		}

		// Initialise display
		try (LcdConnection lcd_connection = new LcdConnection.PCF8574LcdConnection(controller, device_address);
				HD44780Lcd lcd = new HD44780Lcd(lcd_connection, 20, 4)) {
			byte[] space_invader = new byte[] { 0x00, 0x0e, 0x15, 0x1f, 0x0a, 0x04, 0x0a, 0x11 };
			byte[] smilie = new byte[] { 0x00, 0x00, 0x0a, 0x00, 0x00, 0x11, 0x0e, 0x00 };
			byte[] frownie = new byte[] { 0x00, 0x00, 0x0a, 0x00, 0x00, 0x00, 0x0e, 0x11 };
			lcd.createChar(0, space_invader);
			lcd.createChar(1, smilie);
			lcd.createChar(2, frownie);
			lcd.clear();

			for (int i=0; i<2; i++) {
				lcd.setCursorPosition(0, i*2);
				lcd.addText('H');
				lcd.addText('e');
				lcd.addText('l');
				lcd.addText('l');
				lcd.addText('o');
				lcd.addText(' ');
				lcd.addText(' ');
				lcd.addText((byte) 0);
				lcd.addText((byte) 1);
				lcd.addText((byte) 2);
				lcd.setCursorPosition(0, i*2+1);
				lcd.addText('W');
				lcd.addText('o');
				lcd.addText('r');
				lcd.addText('l');
				lcd.addText('d');
				lcd.addText('!');
				lcd.addText(' ');
				lcd.addText((byte) 0);
				lcd.addText((byte) 1);
				lcd.addText((byte) 2);
			}
			SleepUtil.sleepSeconds(5);
			lcd.clear();

			for (int i=0; i<2; i++) {
				// Send some text
				lcd.setText(0, "Hello -");
				lcd.setText(1, "World! " + i);
				lcd.setText(2, "Hello -");
				lcd.setText(3, "World! " + i);
				SleepUtil.sleepSeconds(1);

				lcd.clear();
				SleepUtil.sleepSeconds(1);

				// Send some more text
				lcd.setText(0, ">            diozero");
				lcd.setText(1, ">            I2C LCD");
				lcd.setText(2, ">            diozero");
				lcd.setText(3, ">            I2C LCD");
				SleepUtil.sleepSeconds(1);
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
					SleepUtil.sleepSeconds(.2);
				}
			}
			SleepUtil.sleepSeconds(1);
			lcd.clear();
			lcd.entryModeControl(true, false);

			for (int i=0; i<2; i++) {
				lcd.setCursorPosition(0, i*2);
				lcd.addText('H');
				lcd.addText('e');
				lcd.addText('l');
				lcd.addText('l');
				lcd.addText('o');
				lcd.addText(' ');
				lcd.addText(' ');
				lcd.addText((byte) 0);
				lcd.addText((byte) 1);
				lcd.addText((byte) 2);
				lcd.setCursorPosition(0, i*2+1);
				lcd.addText('W');
				lcd.addText('o');
				lcd.addText('r');
				lcd.addText('l');
				lcd.addText('d');
				lcd.addText('!');
				lcd.addText(' ');
				lcd.addText((byte) 0);
				lcd.addText((byte) 1);
				lcd.addText((byte) 2);
			}
			Logger.info("Sleeping for 10 seconds...");
			SleepUtil.sleepSeconds(10);

			lcd.clear();
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
		}
	}
}
