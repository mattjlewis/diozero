package com.diozero.sampleapps.mfrc522;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Sample applications
 * Filename:     MFRC522Test.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2017 mattjlewis
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

import com.diozero.devices.MFRC522;
import com.diozero.util.Hex;
import com.diozero.util.SleepUtil;

/**
 * Control an LED with a button. To run:
 * <ul>
 * <li>sysfs:<br>
 *  {@code java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar com.diozero.sampleapps.sandpit.MFRC522Test 0 0 25}
 * <li>JDK Device I/O 1.0:<br>
 *  {@code sudo java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-jdkdio10-$DIOZERO_VERSION.jar:dio-1.0.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.sandpit.MFRC522Test 0 0 25}
 * <li>JDK Device I/O 1.1:<br>
 *  {@code sudo java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-jdkdio11-$DIOZERO_VERSION.jar:dio-1.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.sandpit.MFRC522Test 0 0 25}
 * <li>Pi4j:<br>
 *  {@code sudo java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-pi4j-$DIOZERO_VERSION.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.sandpit.MFRC522Test 0 0 25}
 * <li>wiringPi:<br>
 *  {@code sudo java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-wiringpi-$DIOZERO_VERSION.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.sandpit.MFRC522Test 0 0 25}
 * <li>pigpgioJ:<br>
 *  {@code sudo java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-pigpio-$DIOZERO_VERSION.jar:pigpioj-java-1.0.1.jar com.diozero.sampleapps.sandpit.MFRC522Test 0 0 25}
 * </ul>
 */
public class MFRC522Test {
	public static void main(String[] args) {
		if (args.length < 3) {
			Logger.error("Usage: {} <spi-controller> <chip-select> <rst-gpio>", MFRC522Test.class.getName());
			System.exit(1);
		}
		int index = 0;
		int controller = Integer.parseInt(args[index++]);
		int chip_select = Integer.parseInt(args[index++]);
		int reset_pin = Integer.parseInt(args[index++]);
		
		waitForCard(controller, chip_select, reset_pin);
	}
	
	private static void waitForCard(int controller, int chipSelect, int resetPin) {
		try (MFRC522 mfrc522 = new MFRC522(controller, chipSelect, resetPin)) {
//			if (mfrc522.performSelfTest()) {
//				Logger.debug("Self test passed");
//			} else {
//				Logger.debug("Self test failed");
//			}
			// Wait for a card
			MFRC522.UID uid = null;
			while (uid == null) {
				Logger.info("Waiting for a card");
				uid = getID(mfrc522);
				Logger.debug("uid: {}", uid);
				SleepUtil.sleepSeconds(1);
			}
		}
	}
	
	private static MFRC522.UID getID(MFRC522 mfrc522) {
		// If a new PICC placed to RFID reader continue
		if (! mfrc522.isNewCardPresent()) {
			return null;
		}
		Logger.debug("A card is present!");
		// Since a PICC placed get Serial and continue
		MFRC522.UID uid = mfrc522.readCardSerial();
		if (uid == null) {
			return null;
		}
		
		// There are Mifare PICCs which have 4 byte or 7 byte UID care if you use 7 byte PICC
		// I think we should assume every PICC as they have 4 byte UID
		// Until we support 7 byte PICCs
		Logger.info("Scanned PICC's UID: {}", Hex.encodeHexString(uid.getUidBytes()));
		
		mfrc522.haltA();
		
		return uid;
	}
}
