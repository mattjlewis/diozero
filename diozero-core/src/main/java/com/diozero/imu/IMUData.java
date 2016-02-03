package com.diozero.imu;

import org.apache.commons.math3.complex.Quaternion;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class IMUData {
	private Vector3D gyro;
	private Vector3D accel;
	private Quaternion quaternion;
	private Vector3D compass;
	private float temperature;
	private long timestamp;
	
	public IMUData(Vector3D gyro, Vector3D accel, Quaternion quaternion, Vector3D compass, float temperature, long timestamp) {
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
