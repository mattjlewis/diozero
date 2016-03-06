package com.diozero.api.imu;

@FunctionalInterface
public interface TapListener {
	void tapped(TapEvent event);
}
