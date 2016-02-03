package com.diozero.imu.drivers.invensense;

/* Full scale ranges. */
public enum GyroFullScaleRange {
	INV_FSR_250DPS((byte)0, 250/*, 131*/),
	INV_FSR_500DPS((byte)1, 500/*, 65.5*/),
	INV_FSR_1000DPS((byte)2, 1000/*, 32.8*/),
	INV_FSR_2000DPS((byte)3, 2000/*, 16.4*/);
	
	private byte val;
	private byte bitVal;
	private int dps;
	private double sensitivityScaleFactor;
	private double gyroScale;
	
	private GyroFullScaleRange(byte val, int dps) {
		this.val = val;
		bitVal = (byte)(val << 3);
		this.dps = dps;
		this.sensitivityScaleFactor = MPU9150Constants.HARDWARE_UNIT / (double)dps;
		//gyroScale = Math.PI / (sensitivityScaleFactor*180);
		gyroScale = 1.0 / sensitivityScaleFactor;
	}

	public byte getVal() {
		return val;
	}
	
	public byte getBitVal() {
		return bitVal;
	}
	
	public int getDps() {
		return dps;
	}

	public double getSensitivityScaleFactor() {
		return sensitivityScaleFactor;
	}
	
	public double getScale() {
		return gyroScale;
	}
}
