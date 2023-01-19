package com.diozero.devices.imu;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - IMU device classes
 * Filename:     ImuData.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2023 diozero
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

import org.hipparchus.complex.Quaternion;
import org.hipparchus.geometry.euclidean.threed.Vector3D;

public class ImuData {
	private Vector3D gyro;
	private Vector3D accel;
	private Quaternion quaternion;
	private Vector3D compass;
	private float temperature;
	private long timestamp;

	public ImuData(Vector3D gyro, Vector3D accel, Quaternion quaternion, Vector3D compass, float temperature,
			long timestamp) {
		this.gyro = gyro;
		this.accel = accel;
		this.accel = accel;
		this.quaternion = quaternion;
		this.compass = compass;
		this.temperature = temperature;
		this.timestamp = timestamp;
	}

	public Vector3D getGyro() {
		return gyro;
	}

	public void setGyro(Vector3D gyro) {
		this.gyro = gyro;
	}

	public Vector3D getAccel() {
		return accel;
	}

	public Quaternion getQuaternion() {
		return quaternion;
	}

	public Vector3D getCompass() {
		return compass;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public float getTemperature() {
		return temperature;
	}
}
