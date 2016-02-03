package com.diozero.imu;

public class TapCallbackEvent {
	private short direction;
	private short count;

	public TapCallbackEvent(short direction, short count) {
		this.direction = direction;
		this.count = count;
	}

	public short getDirection() {
		return direction;
	}

	public short getCount() {
		return count;
	}

	@Override
	public String toString() {
		return "TapCallbackEvent [direction=" + direction + ", count=" + count + "]";
	}
}
