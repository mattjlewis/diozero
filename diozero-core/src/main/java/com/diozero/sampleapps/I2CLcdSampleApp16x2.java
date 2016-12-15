package com.diozero.sampleapps;

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

import org.pmw.tinylog.Logger;

import com.diozero.I2CLcd;
import com.diozero.api.I2CConstants;
import com.diozero.util.RuntimeIOException;
import com.diozero.util.SleepUtil;

/**
 * I2C LCD sample application. To run:
 * <ul>
 * <li>JDK Device I/O 1.0:<br>
 *  {@code sudo java -cp tinylog-1.1.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-jdkdio10-$DIOZERO_VERSION.jar:dio-1.0.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.I2CLcdSampleApp16x2 [i2c_address] [i2c_controller]}</li>
 * <li>JDK Device I/O 1.1:<br>
 *  {@code sudo java -cp tinylog-1.1.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-jdkdio11-$DIOZERO_VERSION.jar:dio-1.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.I2CLcdSampleApp16x2 [i2c_address] [i2c_controller]}</li>
 * <li>Pi4j:<br>
 *  {@code sudo java -cp tinylog-1.1.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-pi4j-$DIOZERO_VERSION.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.I2CLcdSampleApp16x2 [i2c_address] [i2c_controller]}</li>
 * <li>wiringPi:<br>
 *  {@code sudo java -cp tinylog-1.1.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-wiringpi-$DIOZERO_VERSION.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.I2CLcdSampleApp16x2 [i2c_address] [i2c_controller]}</li>
 * <li>pigpgioJ:<br>
 *  {@code sudo java -cp tinylog-1.1.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-pigpio-$DIOZERO_VERSION.jar:pigpioj-java-1.0.0.jar com.diozero.sampleapps.I2CLcdSampleApp16x2 [i2c_address] [i2c_controller]}</li>
 * <li>sysfs:<br>
 *  {@code sudo java -cp tinylog-1.1.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-sysfs-$DIOZERO_VERSION.jar com.diozero.sampleapps.I2CLcdSampleApp16x2 [i2c_address] [i2c_controller]}</li>
 * </ul>
 */
public class I2CLcdSampleApp16x2 {
	// Main program block
	public static void main(String[] args) {
		int device_address = I2CLcd.DEFAULT_DEVICE_ADDRESS;
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
		try (I2CLcd lcd = new I2CLcd(controller, device_address, columns, rows)) {
			/*Logger.info("Calling setText");
			lcd.setText(0, "Hello World!");
			SleepUtil.sleepSeconds(2);*/
			
			// 0, 14, 21, 31, 10, 4, 10, 17
			byte[] space_invader = I2CLcd.Characters.get("space_invader");
			byte[] smilie = I2CLcd.Characters.get("smilie");
			byte[] frownie = I2CLcd.Characters.get("frownie");
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
			
			lcd.createChar(3, I2CLcd.Characters.get("runninga"));
			lcd.createChar(4, I2CLcd.Characters.get("runningb"));
			lcd.clear();
			for (int i=0; i<20; i++) {
				lcd.setCursorPosition(0, 0);
				lcd.addText(3);
				SleepUtil.sleepMillis(200);
				lcd.setCursorPosition(0, 0);
				lcd.addText(4);
				SleepUtil.sleepMillis(200);
			}
			SleepUtil.sleepSeconds(1);
			lcd.clear();
			
			for (int i=0; i<2; i++) {
				// Send some text
				lcd.setText(0, "Hello -         ");
				lcd.setText(1, "World! " + i);
				SleepUtil.sleepSeconds(1);
				
				lcd.clear();
				SleepUtil.sleepSeconds(1);
			  
				// Send some more text
				lcd.setText(0, ">        diozero");
				lcd.setText(1, ">        I2C LCD");
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
			Logger.info("Sleeping for 5 seconds...");
			SleepUtil.sleepSeconds(5);
			
			lcd.clear();
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
		}
	}
}
