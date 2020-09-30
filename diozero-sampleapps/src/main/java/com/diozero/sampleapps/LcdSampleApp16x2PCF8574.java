package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Sample applications
 * Filename:     LcdSampleApp16x2PCF8574.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
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
import com.diozero.devices.HD44780Lcd;
import com.diozero.devices.HD44780Lcd.LcdConnection;
import com.diozero.devices.HD44780Lcd.PCF8574LcdConnection;
import com.diozero.util.DeviceFactoryHelper;
import com.diozero.util.RuntimeIOException;

/**
 * I2C LCD sample application. To run:
 * <ul>
 * <li>sysfs:<br>
 *  {@code java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar com.diozero.sampleapps.LcdSampleApp16x2PCF8574 [i2c_address] [i2c_controller]}</li>
 * <li>JDK Device I/O 1.0:<br>
 *  {@code sudo java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-jdkdio10-$DIOZERO_VERSION.jar:dio-1.0.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.LcdSampleApp16x2PCF8574 [i2c_address] [i2c_controller]}</li>
 * <li>JDK Device I/O 1.1:<br>
 *  {@code sudo java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-jdkdio11-$DIOZERO_VERSION.jar:dio-1.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.LcdSampleApp16x2PCF8574 [i2c_address] [i2c_controller]}</li>
 * <li>Pi4j:<br>
 *  {@code sudo java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-pi4j-$DIOZERO_VERSION.jar:pi4j-core-1.2.jar com.diozero.sampleapps.LcdSampleApp16x2PCF8574 [i2c_address] [i2c_controller]}</li>
 * <li>wiringPi:<br>
 *  {@code sudo java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-wiringpi-$DIOZERO_VERSION.jar:pi4j-core-1.2.jar com.diozero.sampleapps.LcdSampleApp16x2PCF8574 [i2c_address] [i2c_controller]}</li>
 * <li>pigpgioJ:<br>
 *  {@code sudo java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-pigpio-$DIOZERO_VERSION.jar:pigpioj-java-2.4.jar com.diozero.sampleapps.LcdSampleApp16x2PCF8574 [i2c_address] [i2c_controller]}</li>
 * </ul>
 */
public class LcdSampleApp16x2PCF8574 {
	// Main program block
	public static void main(String[] args) {
		int device_address = HD44780Lcd.PCF8574LcdConnection.DEFAULT_DEVICE_ADDRESS;
		if (args.length > 0) {
			device_address = Integer.decode(args[0]).intValue();
		}
		int controller = I2CConstants.BUS_0;
		if (args.length > 1) {
			controller = Integer.parseInt(args[1]);
		}
		
		int columns = 16;
		int rows = 2;
		
		// Initialise display
		try (LcdConnection lcd_connection = new PCF8574LcdConnection(controller, device_address);
				HD44780Lcd lcd = new HD44780Lcd(lcd_connection, columns, rows)) {
			LcdSampleApp16x2Base.test(lcd);
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
		} finally {
			// Required if there are non-daemon threads that will prevent the
			// built-in clean-up routines from running
			DeviceFactoryHelper.getNativeDeviceFactory().close();
		}
	}
}
