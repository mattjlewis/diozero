package com.diozero.util;

public class MutableByte {
	private byte value;
	
	public MutableByte() {
		this((byte)0);
	}
	
	public MutableByte(byte value) {
		this.value = value;
	}
	
	public void setBitValue(byte bit, boolean on) {
		value = BitManipulation.setBitValue(value, on, bit);
	}
	
	public void setBit(byte bit) {
		value = BitManipulation.setBitValue(value, true, bit);
	}
	
	public void unsetBit(byte bit) {
		value = BitManipulation.setBitValue(value, false, bit);
	}
	
	public byte getValue() {
		return value;
	}
	
	public boolean equals(byte b) {
		return value == b;
	}

	public boolean isBitSet(byte bit) {
		return BitManipulation.isBitSet(value, bit);
	}
}
