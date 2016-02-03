package com.diozero.imu.drivers.invensense;

/* Filter configurations. */
public enum LowPassFilter {
	INV_FILTER_256HZ_NOLPF2((byte)0, 256),
	INV_FILTER_188HZ((byte)1, 188),
	INV_FILTER_98HZ((byte)2, 98),
	INV_FILTER_42HZ((byte)3, 42),
	INV_FILTER_20HZ((byte)4, 20),
	INV_FILTER_10HZ((byte)5, 10),
	INV_FILTER_5HZ((byte)6, 5),
	INV_FILTER_2100HZ_NOLPF((byte)7, 2100);
	
	private byte bit;
	private byte bitVal;
	private int freq;
	
	private LowPassFilter(byte bit, int freq) {
		this.bit = bit;
		bitVal = bit;
		this.freq = freq;
	}
	
	public byte getBit() {
		return bit;
	}
	
	public byte getBitVal() {
		return bitVal;
	}
	
	public int getFreq() {
		return freq;
	}
	
	public static LowPassFilter getForFrequency(int frequency) {
		LowPassFilter lpf;
		if (frequency >= LowPassFilter.INV_FILTER_188HZ.getFreq()) {
			lpf = LowPassFilter.INV_FILTER_188HZ;
		} else if (frequency >= LowPassFilter.INV_FILTER_98HZ.getFreq()) {
			lpf = LowPassFilter.INV_FILTER_98HZ;
		} else if (frequency >= LowPassFilter.INV_FILTER_42HZ.getFreq()) {
			lpf = LowPassFilter.INV_FILTER_42HZ;
		} else if (frequency >= LowPassFilter.INV_FILTER_20HZ.getFreq()) {
			lpf = LowPassFilter.INV_FILTER_20HZ;
		} else if (frequency >= LowPassFilter.INV_FILTER_10HZ.getFreq()) {
			lpf = LowPassFilter.INV_FILTER_10HZ;
		} else {
			lpf = LowPassFilter.INV_FILTER_5HZ;
		}
		
		return lpf;
	}
}
