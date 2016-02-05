package com.diozero.imu.drivers.invensense;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 diozero
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

import com.diozero.imu.IMUData;

public class IMUDataFactory {
	static final int MPU9150_QUAT_W = 0;
	static final int MPU9150_QUAT_X = 1;
	static final int MPU9150_QUAT_Y = 2;
	static final int MPU9150_QUAT_Z = 3;
	
	public static IMUData newInstance(FIFOData fifoData, short[] compassData, double gyroScaleFactor,
			double accelScaleFactor, double compassScaleFactor, double quatScaleFactor, float temperature) {
		Vector3D gyro = createVector(fifoData.getGyro(), gyroScaleFactor);
		Vector3D accel = createVector(fifoData.getAccel(), accelScaleFactor);
		// TODO What is the scale factor for the quaternion data?!
		Quaternion quaternion = createQuaternion(fifoData.getQuat(), quatScaleFactor);
		Vector3D compass = createVector(compassData, compassScaleFactor);
		
		// From https://github.com/sparkfun/MPU-9150_Breakout/blob/master/firmware/MPU6050/Examples/MPU9150_AHRS.ino
		//  The gyros and accelerometers can in principle be calibrated in addition to any factory calibration but they are generally
		//  pretty accurate. You can check the accelerometer by making sure the reading is +1 g in the positive direction for each axis.
		//  The gyro should read zero for each axis when the sensor is at rest. Small or zero adjustment should be needed for these sensors.
		//  The magnetometer is a different thing. Most magnetometers will be sensitive to circuit currents, computers, and 
		//  other both man-made and natural sources of magnetic field. The rough way to calibrate the magnetometer is to record
		//  the maximum and minimum readings (generally achieved at the North magnetic direction). The average of the sum divided by two
		//  should provide a pretty good calibration offset. Don't forget that for the MPU9150, the magnetometer x- and y-axes are switched 
		//  compared to the gyro and accelerometer!
		// Sensors x (y)-axis of the accelerometer is aligned with the y (x)-axis of the magnetometer;
		// the magnetometer z-axis (+ down) is opposite to z-axis (+ up) of accelerometer and gyro!
		// We have to make some allowance for this orientation mismatch in feeding the output to the quaternion filter.
		// For the MPU-9150, we have chosen a magnetic rotation that keeps the sensor forward along the x-axis just like
		// in the LSM9DS0 sensor. This rotation can be modified to allow any convenient orientation convention.
		// This is ok by aircraft orientation standards!
		
		// From Richards Tech (not sure if this is needed if the orientation has been set correctly!)
		// Sort out gyro axes
		//gyro = new Vector3D(gyro.getX(), -gyro.getY(), -gyro.getZ());
		// Sort out accel axes
		//accel = new Vector3D(-accel.getX(), accel.getY(), accel.getZ());
		// Sort out compass axes
		compass = new Vector3D(compass.getY(), -compass.getX(), -compass.getZ());
		
		long timestamp = fifoData.getTimestamp();
		
		return new IMUData(gyro, accel, quaternion, compass, temperature, timestamp);
	}

	private static Vector3D createVector(short[] data, double scale) {
		return new Vector3D(data[0]*scale, data[1]*scale, data[2]*scale);
	}

	private static Quaternion createQuaternion(int[] quat, double scale) {
		// This article suggests QUAT_W is [0]
		// https://github.com/vmayoral/bb_mpu9150/blob/master/src/linux-mpu9150/mpu9150/mpu9150.c
		Quaternion quaterion = new Quaternion(quat[MPU9150_QUAT_W]*scale, quat[MPU9150_QUAT_X]*scale,
				quat[MPU9150_QUAT_Y]*scale, quat[MPU9150_QUAT_Z]*scale);
		return quaterion.normalize();
	}
}
