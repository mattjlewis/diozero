package com.diozero.api.imu;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import com.diozero.util.RuntimeIOException;

public interface ImuInterface {
	ImuData getIMUData() throws RuntimeIOException;
	Vector3D getGyroData() throws RuntimeIOException;
	Vector3D getAccelerometerData() throws RuntimeIOException;
	Vector3D getCompassData() throws RuntimeIOException;
}
