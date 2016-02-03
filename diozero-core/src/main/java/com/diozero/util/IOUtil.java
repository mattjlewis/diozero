package com.diozero.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class IOUtil {
	public static final ByteOrder DEFAULT_BYTE_ORDER = ByteOrder.BIG_ENDIAN;

	/**
	 *
	 * @param b
	 * @return byte values from -127..128 convert 128..255
	 */
	public static int asInt(byte b) {
		return b & 0xff;
	}
	
	public static short getShort(ByteBuffer buffer) {
		return getShort(buffer, DEFAULT_BYTE_ORDER);
	}
	
	public static short getShort(ByteBuffer buffer, ByteOrder order) {
		buffer.order(order);
		return buffer.getShort();
	}
	
	public static int getUShort(ByteBuffer buffer) {
		return getUShort(buffer, DEFAULT_BYTE_ORDER);
	}
	
	public static int getUShort(ByteBuffer buffer, ByteOrder order) {
		buffer.order(order);
		return buffer.getShort() & 0xffff;
	}
	
	public static long getUInt(ByteBuffer buffer)  {
		return getUInt(buffer, buffer.capacity(), DEFAULT_BYTE_ORDER);
	}
	
	public static long getUInt(ByteBuffer buffer, ByteOrder order)  {
		return getUInt(buffer, buffer.capacity(), order);
	}
	
	public static long getUInt(ByteBuffer buffer, int length)  {
		return getUInt(buffer, length, DEFAULT_BYTE_ORDER);
	}
	
	public static long getUInt(ByteBuffer buffer, int length, ByteOrder order)  {
		byte[] data = new byte[length];
		buffer.get(data);
		
		// Normal order:
		// ((data[0] << 8) & 0xFF00) | (data[1] & 0xFF);
		// Reverse order:
		// ((data[1] << 8) & 0xFF00) | (data[0] & 0xFF);

		long val = 0;
		for (int i=0; i<length; i++) {
			val |= (data[order == ByteOrder.LITTLE_ENDIAN ? length-i-1 : i] & 0xff) << (8 * (length - i - 1));
		}

		return val;
	}
}
