package com.diozero.devices.imu.invensense;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - IMU device classes
 * Filename:     AccelFullScaleRange.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2022 diozero
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
