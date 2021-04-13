package com.diozero.devices.imu;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - IMU device classes
 * Filename:     ImuDataFactory.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
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


import org.apache.commons.math3.complex.Quaternion;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class ImuDataFactory {
	static final int MPU9150_QUAT_W = 0;
	static final int MPU9150_QUAT_X = 1;
	static final int MPU9150_QUAT_Y = 2;
	static final int MPU9150_QUAT_Z = 3;

	public static Vector3D createVector(short[] data, double scale) {
		return new Vector3D(data[0]*scale, data[1]*scale, data[2]*scale);
	}

	public static Quaternion createQuaternion(int[] quat, double scale) {
		// This article suggests QUAT_W is [0]
		// https://github.com/vmayoral/bb_mpu9150/blob/master/src/linux-mpu9150/mpu9150/mpu9150.c
		Quaternion quaterion = new Quaternion(quat[MPU9150_QUAT_W]*scale, quat[MPU9150_QUAT_X]*scale,
				quat[MPU9150_QUAT_Y]*scale, quat[MPU9150_QUAT_Z]*scale);
		return quaterion.normalize();
	}

	public static ImuData newInstance(short[] gyro, short[] accel, short[] compass, float temperature,
			double gyroScale, double accelScale, double compassScale) {
		return new ImuData(createVector(gyro, gyroScale), createVector(accel, accelScale),
				null, createVector(compass, compassScale), temperature, System.currentTimeMillis());
	}
}
