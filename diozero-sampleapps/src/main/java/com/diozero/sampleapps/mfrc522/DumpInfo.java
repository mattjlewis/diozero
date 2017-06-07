package com.diozero.sampleapps.mfrc522;

import org.pmw.tinylog.Logger;

import com.diozero.sandpit.MFRC522;
import com.diozero.util.Hex;
import com.diozero.util.SleepUtil;

public class DumpInfo {
	public static void main(String[] args) {
		if (args.length < 4) {
			Logger.error("Usage: {} <spi-controller> <chip-select> <rst-gpio> <auth-key-hex>", ReadBlock.class.getName());
			System.exit(1);
		}
		int index = 0;
		int controller = Integer.parseInt(args[index++]);
		int chip_select = Integer.parseInt(args[index++]);
		int reset_pin = Integer.parseInt(args[index++]);
		byte[] auth_key = Hex.decodeHex(args[index++]);
		
		try (MFRC522 rfid = new MFRC522(controller, chip_select, reset_pin)) {
			rfid.dumpVersionToConsole();
			rfid.setLogReadsAndWrites(true);
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
				
				rfid.dumpToConsole(uid, auth_key);
			}
		}
	}
}
