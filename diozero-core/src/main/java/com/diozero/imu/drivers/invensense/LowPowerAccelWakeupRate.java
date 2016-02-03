package com.diozero.imu.drivers.invensense;

/* Low-power accel wakeup rates. */
public enum LowPowerAccelWakeupRate {
	INV_LPA_1_25HZ((byte)0, 1.25),
	INV_LPA_5HZ((byte)1, 5),
	INV_LPA_20HZ((byte)2, 20),
	INV_LPA_40HZ((byte)3, 40);
	
	private byte value;
	private double rate;
	
	private LowPowerAccelWakeupRate(byte value, double rate) {
		this.value = value;
		this.rate = rate;
	}
	
	public byte getValue() {
		return value;
	}
	
	public double getRate() {
		return rate;
	}
}
