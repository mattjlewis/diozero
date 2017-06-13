package com.diozero.sampleapps.sandpit;

import org.pmw.tinylog.Logger;

import com.diozero.McpEeprom;

public class EepromTest {
	private static final String LOREM_IPSUM = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, "
			+ "sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, "
			+ "quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis "
			+ "aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla "
			+ "pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia "
			+ "deserunt mollit anim id est laborum.";

	public static void main(String[] args) {
		try (McpEeprom eeprom = new McpEeprom(2, McpEeprom.Type.MCP_24xx512)) {
			// Validate write byte and random read
			for (int address = 0; address<2; address++) {
				Logger.info("Address: 0x{}", Integer.valueOf(address));
				for (int data=0; data<256; data++) {
					eeprom.writeByte(address, data);
					int data_read = eeprom.readByte(address) & 0xff;
					if (data_read != data) {
						Logger.error("For address 0x{} expected 0x{}, read 0x{}", Integer.toHexString(address),
								Integer.toHexString(data), Integer.toHexString(data_read));
					}
				}
			}
			
			// Validate write byte and current address read
			String text_to_write = "Hello World";
			int address = 0x10;
			for (byte b : text_to_write.getBytes()) {
				Logger.debug("Writing '" + ((char) b) + "'");
				eeprom.writeByte(address++, b);
			}
			address = 0x10;
			byte b = eeprom.readByte(address);
			Logger.debug("Read '" + ((char) b) + "'");
			for (int i=0; i<text_to_write.length()-1; i++) {
				b = eeprom.readCurrentAddress();
				Logger.debug("Read '" + ((char) b) + "'");
			}
			
			// Test writing more that a page size
			address = 0x1000;
			Logger.debug("Writing '" + LOREM_IPSUM + "'");
			eeprom.writeBytes(address, LOREM_IPSUM.getBytes());
			byte[] data = eeprom.readBytes(address, LOREM_IPSUM.length());
			Logger.debug("read " + data.length + " bytes");
			String text = new String(data);
			Logger.debug("Read '" + text + "'");
		}
	}
}
