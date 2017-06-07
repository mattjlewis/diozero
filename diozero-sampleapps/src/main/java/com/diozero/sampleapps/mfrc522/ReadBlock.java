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


import org.pmw.tinylog.Logger;

import com.diozero.sandpit.MFRC522;
import com.diozero.sandpit.MFRC522.StatusCode;
import com.diozero.util.Hex;
import com.diozero.util.SleepUtil;

public class ReadBlock {
	public static void main(String[] args) {
		if (args.length < 5) {
			Logger.error("Usage: {} <spi-controller> <chip-select> <rst-gpio> <auth-key-hex> <sector> [use-key-a=true]", ReadBlock.class.getName());
			System.exit(1);
		}
		int index = 0;
		int controller = Integer.parseInt(args[index++]);
		int chip_select = Integer.parseInt(args[index++]);
		int reset_pin = Integer.parseInt(args[index++]);
		byte[] auth_key = Hex.decodeHex(args[index++]);
		byte sector = Byte.parseByte(args[index++]);
		boolean use_key_a = true;
		if (args.length > 5) {
			use_key_a = Boolean.parseBoolean(args[index++]);
		}
		
		int blocks_per_sector = 4;
		byte block_start_addr = (byte) (sector * blocks_per_sector);
		byte block_end_addr = (byte) (block_start_addr + blocks_per_sector - 1);
		
		try (MFRC522 rfid = new MFRC522(controller, chip_select, reset_pin)) {
			while (true) {
				Logger.info("Scanning for RFID tags...");
				MFRC522.UID uid = null;
				while (uid == null) {
					SleepUtil.sleepSeconds(1);
					if (rfid.isNewCardPresent()) {
						uid = rfid.readCardSerial();
					}
				}
				Logger.info("Found card with UID 0x{}, type: {}", Hex.encodeHexString(uid.getUidBytes()), uid.getType());
				
				// Authenticate using key A or B
				Logger.info("Authenticating using key {}...", use_key_a ? "A" : "B");
				StatusCode status = rfid.authenticate(use_key_a, block_end_addr, auth_key, uid);
				Logger.info("Authentication status: {}", status);
				if (status == StatusCode.OK) {
					/*
					 * MIFARE Classic 1K (MF1S503x):
 					 * 	Has 16 sectors * 4 blocks/sector * 16 bytes/block = 1024 bytes.
 					 * 	The blocks are numbered 0-63.
 					 * 	Block 3 in each sector is the Sector Trailer. See http://www.mouser.com/ds/2/302/MF1S503x-89574.pdf sections 8.6 and 8.7:
 					 * 		Bytes 0-5:   Key A
 					 * 		Bytes 6-8:   Access Bits
 					 * 		Bytes 9:     User data
 					 * 		Bytes 10-15: Key B (or user data)
 					 * 	Block 0 is read-only manufacturer data.
 					 * 	To access a block, an authentication using a key from the block's sector must be performed first.
 					 * 	Example: To read from block 10, first authenticate using a key from sector 3 (blocks 8-11).
					 */
					byte[] data = rfid.mifareRead(block_start_addr);
					if (data == null) {
						Logger.info("Unable to read data on block 0x{}", Integer.toHexString(block_start_addr));
					} else {
						Logger.info("Data on block 0x{}: 0x{}", Integer.toHexString(block_start_addr), Hex.encodeHexString(data));
					}
					rfid.dumpMifareClassicSectorToConsole(uid, auth_key, sector);
				}
				
				// Halt PICC
				rfid.haltA();
				
				// Stop encryption on PCD
				rfid.stopCrypto1();
			}
		}
	}
}
