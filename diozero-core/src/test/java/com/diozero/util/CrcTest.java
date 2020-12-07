package com.diozero.util;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class CrcTest {
	@Test
	public void testReverseByte() {
		Assertions.assertEquals((byte) 0, BitManipulation.reverseByte((byte) 0));
		Assertions.assertEquals((byte) 0b10000000, BitManipulation.reverseByte((byte) 0b00000001));
		Assertions.assertEquals((byte) 0b00000001, BitManipulation.reverseByte((byte) 0b10000000));
		Assertions.assertEquals((byte) 0b01010101, BitManipulation.reverseByte((byte) 0b10101010));
		Assertions.assertEquals((byte) 0b11111111, BitManipulation.reverseByte((byte) 0b11111111));
	}

	@Test
	public void testReverseShort() {
		Assertions.assertEquals((short) 0, BitManipulation.reverseShort((short) 0));
		Assertions.assertEquals((short) 0b1000000010000000, BitManipulation.reverseShort((short) 0b0000000100000001));
		Assertions.assertEquals((short) 0b0000000100000001, BitManipulation.reverseShort((short) 0b1000000010000000));
		Assertions.assertEquals((short) 0b0101010101010101, BitManipulation.reverseShort((short) 0b1010101010101010));
		Assertions.assertEquals((short) 0b1111111111111111, BitManipulation.reverseShort((short) 0b1111111111111111));
	}

	@Test
	public void crc8Test() {
		byte[] data = "123456789".getBytes(StandardCharsets.US_ASCII);

		Assertions.assertEquals(0xF4, Crc.crc8(Crc.CRC8, data));
		Assertions.assertEquals(0xDA, Crc.crc8(Crc.CRC8_CDMA2000, data));
		Assertions.assertEquals(0x15, Crc.crc8(Crc.CRC8_DARC, data));
		Assertions.assertEquals(0xBC, Crc.crc8(Crc.CRC8_DVBS2, data));
		Assertions.assertEquals(0x97, Crc.crc8(Crc.CRC8_EBU, data));
		Assertions.assertEquals(0x7E, Crc.crc8(Crc.CRC8_ICODE, data));
		Assertions.assertEquals(0xA1, Crc.crc8(Crc.CRC8_ITU, data));
		Assertions.assertEquals(0xA1, Crc.crc8(Crc.CRC8_MAXIM, data));
		Assertions.assertEquals(0xD0, Crc.crc8(Crc.CRC8_ROHC, data));
		Assertions.assertEquals(0x25, Crc.crc8(Crc.CRC8_WCDMA, data));
	}

	@Test
	public void crc16Test() {
		byte[] data = "123456789".getBytes(StandardCharsets.US_ASCII);

		Assertions.assertEquals(0x29B1, Crc.crc16(Crc.CRC16_CCITT_FALSE, data));
		Assertions.assertEquals(0xBB3D, Crc.crc16(Crc.CRC16_ARC, data));
		Assertions.assertEquals(0xE5CC, Crc.crc16(Crc.CRC16_AUG_CCITT, data));
		Assertions.assertEquals(0xFEE8, Crc.crc16(Crc.CRC16_BUYPASS, data));
		Assertions.assertEquals(0x4C06, Crc.crc16(Crc.CRC16_CDMA2000, data));
		Assertions.assertEquals(0x9ECF, Crc.crc16(Crc.CRC16_DDS_110, data));
		Assertions.assertEquals(0x007E, Crc.crc16(Crc.CRC16_DECT_R, data));
		Assertions.assertEquals(0x007F, Crc.crc16(Crc.CRC16_DECT_X, data));
		Assertions.assertEquals(0xEA82, Crc.crc16(Crc.CRC16_DNP, data));
		Assertions.assertEquals(0xC2B7, Crc.crc16(Crc.CRC16_EN_13757, data));
		Assertions.assertEquals(0xD64E, Crc.crc16(Crc.CRC16_GENIBUS, data));
		Assertions.assertEquals(0x44C2, Crc.crc16(Crc.CRC16_MAXIM, data));
		Assertions.assertEquals(0x6F91, Crc.crc16(Crc.CRC16_MCRF4XX, data));
		Assertions.assertEquals(0x63D0, Crc.crc16(Crc.CRC16_RIELLO, data));
		Assertions.assertEquals(0xD0DB, Crc.crc16(Crc.CRC16_T10_DIF, data));
		Assertions.assertEquals(0x0FB3, Crc.crc16(Crc.CRC16_TELEDISK, data));
		Assertions.assertEquals(0x26B1, Crc.crc16(Crc.CRC16_TMS37157, data));
		Assertions.assertEquals(0xB4C8, Crc.crc16(Crc.CRC16_USB, data));
		Assertions.assertEquals(0xBF05, Crc.crc16(Crc.CRC16_A, data));
		Assertions.assertEquals(0x2189, Crc.crc16(Crc.CRC16_KERMIT, data));
		Assertions.assertEquals(0x4B37, Crc.crc16(Crc.CRC16_MODBUS, data));
		Assertions.assertEquals(0x906E, Crc.crc16(Crc.CRC16_X25, data));
		Assertions.assertEquals(0x31C3, Crc.crc16(Crc.CRC16_XMODEM, data));

		// ADS112C04 CRC Test (CRC16 CCITT FALSE)
		Crc.Params ads112c04_crc_params = new Crc.Params(0b10001000000100001, 0xffff, false, false, 0x0000);
		Assertions.assertEquals(0x29B1, Crc.crc16(ads112c04_crc_params, data));

		// Misc. tests
		Assertions.assertEquals(0xF353, Crc.crc16(Crc.CRC16_ARC, "Hello".getBytes(StandardCharsets.US_ASCII)));
		Assertions.assertEquals(0x1CB0,
				Crc.crc16(Crc.CRC16_CCITT_FALSE, "Lammert".getBytes(StandardCharsets.US_ASCII)));
		Assertions.assertEquals(0xA74A, Crc.crc16(Crc.CRC16_CCITT_FALSE, new byte[] { (byte) 0xfc, 0x05, 0x11 }));
		Assertions.assertEquals(0x763A,
				Crc.crc16(Crc.CRC16_CCITT_FALSE, "9142656".getBytes(StandardCharsets.US_ASCII)));
	}
}
