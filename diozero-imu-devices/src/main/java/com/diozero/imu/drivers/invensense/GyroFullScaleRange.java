package com.diozero.imu.drivers.invensense;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - IMU device classes
 * Filename:     GyroFullScaleRange.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */


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
