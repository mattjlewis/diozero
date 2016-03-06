package com.diozero.api.imu;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import com.diozero.util.RuntimeIOException;

public interface ImuInterface {
	
	String getImuName();
	/** Get the recommended poll interval in mS */
	int getPollInterval();
	
	boolean hasGyro();
	boolean hasAccelerometer();
	boolean hasCompass();
	
	void startRead();
	void stopRead();
	
	ImuData getImuData() throws RuntimeIOException;
	Vector3D getGyroData() throws RuntimeIOException;
	Vector3D getAccelerometerData() throws RuntimeIOException;
	Vector3D getCompassData() throws RuntimeIOException;
	
	void addTapListener(TapListener listener);
	void addOrientationListener(OrientationListener listener);
}
