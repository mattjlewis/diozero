package com.diozero.sampleapps.sandpit;

import org.pmw.tinylog.Logger;

import com.diozero.sandpit.MFRC522;
import com.diozero.sandpit.MFRC522.Response;

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


import com.diozero.util.SleepUtil;

/**
 * Control an LED with a button. To run:
 * <ul>
 * <li>sysfs:<br>
 *  {@code java -cp tinylog-1.1.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar com.diozero.sandpit.MFRC522Test 25}
 * <li>JDK Device I/O 1.0:<br>
 *  {@code sudo java -cp tinylog-1.1.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-jdkdio10-$DIOZERO_VERSION.jar:dio-1.0.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sandpit.MFRC522Test 25}
 * <li>JDK Device I/O 1.1:<br>
 *  {@code sudo java -cp tinylog-1.1.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-jdkdio11-$DIOZERO_VERSION.jar:dio-1.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sandpit.MFRC522Test 25}
 * <li>Pi4j:<br>
 *  {@code sudo java -cp tinylog-1.1.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-pi4j-$DIOZERO_VERSION.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sandpit.MFRC522Test 25}
 * <li>wiringPi:<br>
 *  {@code sudo java -cp tinylog-1.1.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-wiringpi-$DIOZERO_VERSION.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sandpit.MFRC522Test 25}
 * <li>pigpgioJ:<br>
 *  {@code sudo java -cp tinylog-1.1.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-pigpio-$DIOZERO_VERSION.jar:pigpioj-java-1.0.1.jar com.diozero.sandpit.MFRC522Test 25}
 * </ul>
 */
public class MFRC522Test {
	public static void main(String[] args) {
		if (args.length < 3) {
			Logger.error("Usage: {} <spi-controller> <chip-select> <rst-gpio>", MFRC522Test.class.getName());
			System.exit(1);
		}
		int controller = Integer.parseInt(args[0]);
		int chip_select = Integer.parseInt(args[1]);
		int rst_pin = Integer.parseInt(args[2]);
		
		try (MFRC522 mfrc522 = new MFRC522(controller, chip_select, rst_pin)) {
			while (true) {
				// Scan for cards
				Logger.info("Waiting for cards");
				Response resp = mfrc522.request(MFRC522.PICC_REQIDL);
				if (resp.getStatus() == MFRC522.MI_OK) {
					System.out.println("Card detected");
					
					mfrc522.setLog(true);
					
					// Get the UID of the card
					Response ac_resp = mfrc522.anticoll();
					if (ac_resp.getStatus() == MFRC522.MI_OK) {
						byte[] uid = ac_resp.getBackData();
						Logger.info("uid.length=" + uid.length);
						Logger.info(String.format("Card UID: 0x%02x%02x%02x%02x%n", Byte.valueOf(uid[0]), Byte.valueOf(uid[1]), Byte.valueOf(uid[2]), Byte.valueOf(uid[3])));
						
						// This is the default key for authentication
				        byte[] key = { (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF };
				        
				        // Select the scanned tag
				        mfrc522.selectTag(uid);

				        // Authenticate
				        byte status = mfrc522.auth(MFRC522.PICC_AUTHENT1A, (byte) 8, key, uid);

				        // Check if authenticated
				        if (status == MFRC522.MI_OK) {
				        	mfrc522.read((byte) 8);
				        	mfrc522.stopCrypto1();
				        } else {
				        	Logger.error("Authentication error");
				        }
					} else {
						Logger.info("Got bad response status from anticoll: " + resp.getStatus());
					}
					mfrc522.setLog(false);
				}
				SleepUtil.sleepSeconds(1);
			}
		}
	}
}
