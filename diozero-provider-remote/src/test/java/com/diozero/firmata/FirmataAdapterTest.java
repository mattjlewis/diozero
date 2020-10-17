package com.diozero.firmata;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings("static-method")
public class FirmataAdapterTest {
	@Test
	public void testConversion() {
		byte[] bytes = { 0x7f, 0x01 };
		int value = 255;
		Assertions.assertEquals(value, FirmataAdapter.convertToValue(bytes[0], bytes[1]));
		Assertions.assertArrayEquals(bytes, FirmataAdapter.convertToLsbMsb(value));
		Assertions.assertArrayEquals(bytes, FirmataAdapter.convertToBytes(value));
		
		bytes = new byte[] { 0x7f, 0x07 };
		value = 1023;
		Assertions.assertEquals(value, FirmataAdapter.convertToValue(bytes[0], bytes[1]));
		Assertions.assertArrayEquals(bytes, FirmataAdapter.convertToLsbMsb(value));
		Assertions.assertArrayEquals(bytes, FirmataAdapter.convertToBytes(value));

		value = 127;
		bytes = new byte[] { 0x7f, 0x00 };
		Assertions.assertArrayEquals(bytes, FirmataAdapter.convertToLsbMsb(value));
		Assertions.assertEquals(value, FirmataAdapter.convertToValue(bytes[0], bytes[1]));
		bytes = new byte[] { 0x7f };
		Assertions.assertArrayEquals(bytes, FirmataAdapter.convertToBytes(value));
		Assertions.assertEquals(value, FirmataAdapter.convertToValue(bytes[0]));
		Assertions.assertEquals(value, FirmataAdapter.convertToValue(bytes));
		
		bytes = new byte[] { 0x7f };
		Assertions.assertArrayEquals(bytes, FirmataAdapter.convertToBytes(value));
		Assertions.assertEquals(value, FirmataAdapter.convertToValue(bytes));

		value = 128;
		bytes = new byte[] { 0x0, 0x01 };
		Assertions.assertArrayEquals(bytes, FirmataAdapter.convertToLsbMsb(value));
		Assertions.assertEquals(value, FirmataAdapter.convertToValue(bytes[0], bytes[1]));
		Assertions.assertEquals(value, FirmataAdapter.convertToValue(bytes));

		value = 129;
		bytes = new byte[] { 0x1, 0x01 };
		Assertions.assertArrayEquals(bytes, FirmataAdapter.convertToLsbMsb(value));
		Assertions.assertEquals(value, FirmataAdapter.convertToValue(bytes[0], bytes[1]));
		Assertions.assertEquals(value, FirmataAdapter.convertToValue(bytes));

		value = 16383;
		bytes = new byte[] { 0x7f, 0x7f };
		Assertions.assertArrayEquals(bytes, FirmataAdapter.convertToLsbMsb(value));
		Assertions.assertEquals(value, FirmataAdapter.convertToValue(bytes[0], bytes[1]));
		Assertions.assertEquals(value, FirmataAdapter.convertToValue(bytes));

		value = 16384;
		bytes = new byte[] { 0x00, 0x00, 0x01 };
		Assertions.assertArrayEquals(bytes, FirmataAdapter.convertToBytes(value));
		Assertions.assertEquals(value, FirmataAdapter.convertToValue(bytes));

		value = 16385;
		bytes = new byte[] { 0x01, 0x00, 0x01 };
		Assertions.assertArrayEquals(bytes, FirmataAdapter.convertToBytes(value));
		Assertions.assertEquals(value, FirmataAdapter.convertToValue(bytes));
		
		value = 2097151;
		bytes = new byte[] { 0x7f, 0x7f, 0x7f };
		Assertions.assertArrayEquals(bytes, FirmataAdapter.convertToBytes(value));
		Assertions.assertEquals(value, FirmataAdapter.convertToValue(bytes));
		
		value = 2097152;
		bytes = new byte[] { 0x00, 0x00, 0x00, 0x01 };
		Assertions.assertArrayEquals(bytes, FirmataAdapter.convertToBytes(value));
		Assertions.assertEquals(value, FirmataAdapter.convertToValue(bytes));
		
		value = 2097153;
		bytes = new byte[] { 0x01, 0x00, 0x00, 0x01 };
		Assertions.assertArrayEquals(bytes, FirmataAdapter.convertToBytes(value));
		Assertions.assertEquals(value, FirmataAdapter.convertToValue(bytes));
		
		value = 268435456;
		bytes = new byte[] { 0x00 };
		Assertions.assertArrayEquals(bytes, FirmataAdapter.convertToBytes(value));
		
		Assertions.assertEquals(0, 7 >> 3);
		Assertions.assertEquals(1, 8 >> 3);
		Assertions.assertEquals(1, 15 >> 3);
		Assertions.assertEquals(2, 16 >> 3);
		Assertions.assertEquals(2, 23 >> 3);
		Assertions.assertEquals(3, 24 >> 3);
		Assertions.assertEquals(3, 31 >> 3);
		Assertions.assertEquals(4, 32 >> 3);
	}
}
