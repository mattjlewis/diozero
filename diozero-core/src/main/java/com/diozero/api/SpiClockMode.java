package com.diozero.api;

public enum SpiClockMode {
	// https://en.wikipedia.org/wiki/Serial_Peripheral_Interface_Bus#Mode_numbers
	MODE_0(0), MODE_1(1), MODE_2(2), MODE_3(3);
	
	private int mode;
	
	private SpiClockMode(int mode) {
		this.mode = mode;
	}
	
	public int getMode() {
		return mode;
	}
	
	public static SpiClockMode getByMode(int mode) {
		switch (mode) {
		case 0:
			return MODE_0;
		case 1:
			return MODE_1;
		case 2:
			return MODE_2;
		case 3:
			return MODE_3;
		default:
			return null;
		}
	}
}
