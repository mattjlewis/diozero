package com.diozero.imu.drivers.invensense;

/* Full scale ranges. */
public enum AccelFullScaleRange {
	INV_FSR_2G((byte)0, 2/*, 16_384*/),
	INV_FSR_4G((byte)1, 4/*, 8_192*/),
	INV_FSR_8G((byte)2, 8/*, 4_096*/),
	INV_FSR_16G((byte)3, 16/*, 2_048*/);
	
	private byte bit;
	private byte bitVal;
	private int g;
	private int sensitivityScaleFactor;
	private double accelScale;
	
	private AccelFullScaleRange(byte bit, int g) {
		this.bit = bit;
		bitVal = (byte)(bit << 3);
		this.g = g;
		this.sensitivityScaleFactor = MPU9150Constants.HARDWARE_UNIT / g;
		accelScale = 1.0 / sensitivityScaleFactor;
	}
	
	public byte getBit() {
		return bit;
	}
	
	public byte getBitVal() {
		return bitVal;
	}
	
	public int getG() {
		return g;
	}

	public int getSensitivityScaleFactor() {
		return sensitivityScaleFactor;
	}

	public double getScale() {
		return accelScale;
	}
}
