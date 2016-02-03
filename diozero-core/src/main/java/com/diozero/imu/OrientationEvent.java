package com.diozero.imu;

public class OrientationEvent {
	private short orientation;

	public OrientationEvent(short orientation) {
		this.orientation = orientation;
	}

	public short getOrientation() {
		return orientation;
	}

	@Override
	public String toString() {
		return "OrientationEvent [orientation=" + orientation + "]";
	}
}
