package com.diozero.sampleapps.mfrc522;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Sample applications
 * Filename:     ReadAndWrite.java  
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


import org.pmw.tinylog.Logger;

import com.diozero.devices.MFRC522;
import com.diozero.devices.MFRC522.StatusCode;
import com.diozero.util.Hex;
import com.diozero.util.SleepUtil;

public class ReadAndWrite {
	public static void main(String[] args) {
		if (args.length < 4) {
			Logger.error("Usage: {} <spi-controller> <chip-select> <rst-gpio> <auth-key-hex>", ReadAndWrite.class.getName());
			System.exit(1);
		}
		int index = 0;
		int controller = Integer.parseInt(args[index++]);
		int chip_select = Integer.parseInt(args[index++]);
		int reset_pin = Integer.parseInt(args[index++]);
		byte[] auth_key = Hex.decodeHex(args[index++]);
		
		MFRC522.UID uid = null;
		try (MFRC522 rfid = new MFRC522(controller, chip_select, reset_pin)) {
			Logger.info("Scanning for RFID tags...");
			while (uid == null) {
				SleepUtil.sleepSeconds(1);
				if (rfid.isNewCardPresent()) {
					uid = rfid.readCardSerial();
				}
			}
			Logger.info("Found card with UID 0x{}, type: {}", Hex.encodeHexString(uid.getUidBytes()), uid.getType());
			if (uid.getType() != MFRC522.PiccType.MIFARE_MINI
					&& uid.getType() != MFRC522.PiccType.MIFARE_1K
					&& uid.getType() != MFRC522.PiccType.MIFARE_4K) {
				Logger.error("This example only works with Mifare Classic cards");
				return;
			}
			
			readAndWrite(rfid, uid, auth_key, false);

		    // Halt PICC
		    rfid.haltA();
		    // Stop encryption on PCD
		    rfid.stopCrypto1();
		}
	}
	
	private static void readAndWrite(MFRC522 rfid, MFRC522.UID uid, byte[] authKey, boolean write) {
		// In this sample we use the second sector, that is: sector #1,
		// covering block #4 up to and including block #7
		byte blocks_per_sector = 4;
		byte sector = 15;
		byte blockAddr = (byte) (sector * blocks_per_sector);
		byte trailerBlock = (byte) (blockAddr + (blocks_per_sector - 1));

		// Authenticate using key A
		Logger.info("Authenticating using key A...");
		StatusCode status = rfid.authenticate(true, trailerBlock, authKey, uid);
		if (status != StatusCode.OK) {
			Logger.error("PCD_Authenticate() failed: {}", status);
			return;
		}

		// Show the whole sector as it currently is
		Logger.info("Current data in sector:");
		rfid.dumpMifareClassicSectorToConsole(uid, authKey, sector);

		// Read data from the block
		Logger.info("Reading data from block 0x{} ...", Integer.toHexString(blockAddr));
		byte[] buffer = rfid.mifareRead(blockAddr);
		if (buffer == null) {
			Logger.error("MIFARE_Read() failed: buffer was null");
			return;
		}
		Logger.info("Data in block 0x{} : 0x{}", Integer.toHexString(blockAddr), Hex.encodeHexString(buffer, 4));

		// Authenticate using key B
		Logger.info("Authenticating again using key B...");
		status = rfid.authenticate(false, trailerBlock, authKey, uid);
		if (status != StatusCode.OK) {
			Logger.error("PCD_Authenticate() failed: {}", status);
			return;
		}

		if (write) {
			byte[] dataBlock = {
					0x01, 0x02, 0x03, 0x04,			//  1,  2,   3,  4,
					0x05, 0x06, 0x07, 0x08,			//  5,  6,   7,  8,
					0x08, 0x09, (byte) 0xff, 0x0b,	//  9, 10, 255, 12,
					0x0c, 0x0d, 0x0e, 0x0f			// 13, 14,  15, 16
			};
			// Write data to the block
			Logger.info("Writing data into block 0x{} : 0x{} ...", Integer.toHexString(blockAddr), Hex.encodeHexString(dataBlock, 4));
			status = rfid.mifareWrite(blockAddr, dataBlock);
			if (status != StatusCode.OK) {
				Logger.error("MIFARE_Write() failed: {}", status);
				return;
			}
	
		    // Read data from the block (again, should now be what we have written)
			Logger.info("Reading data from block 0x{} ...", Integer.toHexString(blockAddr));
			buffer = rfid.mifareRead(blockAddr);
			if (buffer == null) {
				Logger.error("MIFARE_Read() failed: buffer was null");
				return;
			}
			Logger.info("Data in block 0x{}: 0x{}", Integer.toHexString(blockAddr), Hex.encodeHexString(buffer, 4));
		        
			// Check that data in block is what we have written
			// by counting the number of bytes that are equal
			Logger.info("Checking result...");
			byte count = 0;
			for (int i=0; i<16; i++) {
				// Compare buffer (= what we've read) with dataBlock (= what we've written)
				if (buffer[i] == dataBlock[i]) {
					count++;
				}
			}
			Logger.info("Number of bytes that match = {}", Integer.valueOf(count));
		    if (count == 16) {
		    	Logger.info("Success :-)");
		    } else {
		    	Logger.info("Failure, no match :-(");
		    	Logger.info("  perhaps the write didn't work properly...");
		    }
		        
		    // Dump the sector data
		    Logger.info("Current data in sector:");
		    rfid.dumpMifareClassicSectorToConsole(uid, authKey, sector);
		}
	}
}
