package com.diozero.sampleapps.sandpit;

import com.diozero.api.I2CDevice;

public class ReadBbbBoardId {
	public static void main(String[] args) {
		try (I2CDevice device = I2CDevice.builder(0x50).setController(0).build()) {
			byte[] address = { (byte) 0, 0 };
			device.writeBytes(address);
			for (int i=0; i<4; i++) {
				System.out.println("read 0x" + Integer.toHexString(0xff & device.readByte()));
			}
			
			device.writeBytes(address);
			for (int i=0; i<4; i++) {
				System.out.println("read 0x" + Integer.toHexString(0xff & device.readByte()));
			}
			
			device.writeBytes(address);
			byte[] eeprom_data = device.readBytes(28);
			for (int i=0; i<4; i++) {
				System.out.println("Header[" + i + "]: 0x" + Integer.toHexString(0xFF & eeprom_data[i]));
			}
			String board_name = new String(eeprom_data, 4, 8);
			System.out.println("Board Name: " + board_name);
			String version = new String(eeprom_data, 4+8, 4);
			System.out.println("Version: " + version);
			String serial_num = new String(eeprom_data, 4+8+4, 12);
			System.out.println("Serial Number: " + serial_num);
		}
	}
}
