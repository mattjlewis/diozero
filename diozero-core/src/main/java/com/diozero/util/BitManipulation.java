package com.diozero.util;

public class BitManipulation {
	public static byte setBitValue(byte value, boolean on, int bit) {
		byte mask = getBitMask(bit);
		byte new_value = value;
		if (on) {
			new_value |= mask;
		} else {
			new_value &= ~mask;
		}
		
		return new_value;
	}
	
	public static byte getBitMask(int ... bits) {
		byte mask = 0;
		
		for (int bit : bits) {
			mask |= getBitMask(bit);
		}
		
		return mask;
	}
	
	public static byte getBitMask(int bit) {
		if (bit > 7) {
			throw new IllegalArgumentException("Bit (" + bit + ") out of range for byte");
		}
		
		return (byte)(1 << bit);
	}

	public static boolean isBitSet(byte value, byte bit) {
		return (value & getBitMask(bit)) != 0;
	}
}
