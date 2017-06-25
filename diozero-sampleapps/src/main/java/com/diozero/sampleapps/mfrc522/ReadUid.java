package com.diozero.sampleapps.mfrc522;

/*
 * #%L
 * Device I/O Zero - Sample applications
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


import java.util.ArrayList;
import java.util.List;

import org.pmw.tinylog.Logger;

import com.diozero.devices.MFRC522;
import com.diozero.devices.MFRC522.UID;
import com.diozero.util.Hex;
import com.diozero.util.SleepUtil;

public class ReadUid {
	private static List<UID> uids;
	
	public static void main(String[] args) {
		if (args.length < 3) {
			Logger.error("Usage: {} <spi-controller> <chip-select> <rst-gpio>", ReadUid.class.getName());
			System.exit(1);
		}
		int index = 0;
		int controller = Integer.parseInt(args[index++]);
		int chip_select = Integer.parseInt(args[index++]);
		int reset_pin = Integer.parseInt(args[index++]);
		
		uids = new ArrayList<>();
		
		try (MFRC522 rfid = new MFRC522(controller, chip_select, reset_pin)) {
			Logger.info("Scanning for RFID tags...");
			while (true) {
				loop(rfid);
				SleepUtil.sleepSeconds(1);
			}
		}
	}
	
	private static void loop(MFRC522 rfid) {
		// Look for new cards
		if ( ! rfid.isNewCardPresent()) {
			return;
		}

		// Verify if the NUID has been readed
		UID uid = rfid.readCardSerial();
		if (uid == null) {
			return;
		}

		if (! uids.contains(uid)) {
			Logger.info("A new card (type '{}') has been detected, UID 0x{}.", uid.getType().getName(), Hex.encodeHexString(uid.getUidBytes()));

			// Store NUID into uids array
			uids.add(uid);
		} else {
			Logger.info("Card (type '{}') with UID 0x{} has been read previously.", uid.getType().getName(), Hex.encodeHexString(uid.getUidBytes()));
		}

		// Halt PICC
		rfid.haltA();
		
		// Stop encryption on PCD
		rfid.stopCrypto1();
	}
}
