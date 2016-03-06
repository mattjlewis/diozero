package com.diozero.api.imu;

@FunctionalInterface
public interface OrientationListener {
	void orientationChange(OrientationEvent event);
}
