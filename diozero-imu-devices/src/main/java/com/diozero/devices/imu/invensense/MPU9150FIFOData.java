package com.diozero.devices.imu.invensense;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - IMU device classes
 * Filename:     MPU9150FIFOData.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2021 diozero
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


import java.util.Arrays;

public class MPU9150FIFOData {
	// Gyro data in hardware units.
	private short[] gyro;
	// Accel data in hardware units.
	private short[] accel;
	// 3-axis quaternion data in hardware units.
	private int[] quat;
	// Timestamp in milliseconds.
    private long timestamp;
    // Mask of sensors read from FIFO.
    private short sensors;
    // Number of remaining packets.
    private int more;
    
	public MPU9150FIFOData(short[] gyro, short[] accel, int[] quat, long timestamp, short sensors, int more) {
		this.gyro = gyro;
		this.accel = accel;
		this.quat = quat;
		this.timestamp = timestamp;
		this.sensors = sensors;
		this.more = more;
	}

	public short[] getGyro() {
		return gyro;
	}

	public short[] getAccel() {
		return accel;
	}

	public int[] getQuat() {
		return quat;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public short getSensors() {
		return sensors;
	}

	public int getMore() {
		return more;
	}

	@Override
	public String toString() {
		return "FIFOData [gyro=" + Arrays.toString(gyro) + ", accel=" + Arrays.toString(accel) + ", quat="
				+ Arrays.toString(quat) + ", timestamp=" + timestamp + ", sensors=" + sensors + ", more=" + more + "]";
	}
}
