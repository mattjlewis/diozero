package com.diozero.imu.drivers.invensense;

/* Clock sources. */
/**
 * CLKSEL	Clock Source
 * 0		Internal 8MHz oscillator
 * 1		PLL with X axis gyroscope reference
 * 2		PLL with Y axis gyroscope reference
 * 3		PLL with Z axis gyroscope reference
 * 4		PLL with external 32.768kHz reference
 * 5		PLL with external 19.2MHz reference
 * 6		Reserved
 * 7		Stops the clock and keeps the timing generator in reset
 */
public enum ClockSource {
	INV_CLK_INTERNAL((byte)0),
	INV_CLK_PLL((byte)1);

	private byte val;
	
	private ClockSource(byte val) {
		this.val = val;
	}
	
	public byte getVal() {
		return val;
	}
}
