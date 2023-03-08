package com.diozero.internal.provider.firmata.adapter.example;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.diozero.util.Crc;
import com.diozero.util.Hex;

public class OneWireCrcTest {
	public static void main(String[] args) {
		long serial_number = 0x02157190F0FFL;
		System.out.println(serial_number);

		// 28FFF090711502E4
		byte[] address = { 0x28, (byte) 0xFF, (byte) 0xF0, (byte) 0x90, 0x71, 0x15, 0x02, (byte) 0xE4 };
		System.out.println(address[7] & 0xff);
		byte[] data = new byte[address.length - 1];
		System.arraycopy(address, 0, data, 0, data.length);
		System.out.println(Hex.encodeHexString(address, 1, ':'));
		System.out.println(Hex.encodeHexString(data, 1, ':'));

		System.out.println(Crc.crc8(Crc.CRC8_MAXIM, data));

		/*-
		 * 0x03E7 == 999, the correlation id
		 *       1 2 3 4 5 6 7 8 9
		 * E703 DB004B017FFF0C1010
		 * E703 DB004B017FFF0C1010
		 * E703 D4004B017FFF0C10EA
		 * E703 D4004B017FFF0C10EA
		 * E703 D4004B017FFF0C10EA
		 * E703 D4004B017FFF0C10EA
		 * E703 D4004B017FFF0C10EA
		 * E703 D4004B017FFF0C10EA
		 * E703 D4004B017FFF0C10EA
		 * E703 D4004B017FFF0C10EA
		 * E703 D4004B017FFF0C10EA
		 * E703 D4004B017FFF0C10EA
		 * E703 D4004B017FFF0C10EA
		 * E703 D4004B017FFF0C10EA
		 * E703 D4004B017FFF0C10EA
		 * E703 D4004B017FFF0C10EA
		 * E703 D4004B017FFF0C10EA
		 * E703 D4004B017FFF0C10EA
		 * E703 D4004B017FFF0C10EA
		 * E703 D4004B017FFF0C10EA
		 * E703 D4004B017FFF0C10EA
		 */
		data = new byte[] { (byte) 0xDB, 0x00, 0x4B, 0x01, 0x7F, (byte) 0xFF, 0x0C, 0x10, 0x10 };
		int crc = Crc.crc8(Crc.CRC8_MAXIM, data[0], data[1], data[2], data[3], data[4], data[5], data[6], data[7]);
		System.out.println(crc + ": " + (data[8] & 0xff));
		int value = ((data[1] & 0xff) << 8) | (data[0] & 0xff);
		ByteBuffer buffer = ByteBuffer.wrap(data);
		buffer.order(ByteOrder.LITTLE_ENDIAN);
		System.out.format("Config register: 0x%02x%n", data[4]);
		int cfg = (data[4] >> 5) & 0b11;
		System.out.format("cfg: 0x%02x%n", cfg);
		value <<= (3 - cfg);
		switch (cfg) {
		case 0b00: // 9 bit resolution, 93.75 ms
			System.out.println("9-bit resolution");
			break;
		case 0b01: // 10 bit res, 187.5 ms
			System.out.println("10-bit resolution");
			break;
		case 0b10: // 11 bit res, 375 ms
			System.out.println("11-bit resolution");
			break;
		case 0b11:
		default: // default is 12 bit resolution, 750 ms conversion time
			System.out.println("default - 12-bit resolution");
		}

		double temp = value / 16.0;
		System.out.println(value + ": " + temp);
	}
}
