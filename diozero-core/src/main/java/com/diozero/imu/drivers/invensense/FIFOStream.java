package com.diozero.imu.drivers.invensense;

public class FIFOStream {
	private byte[] data;
	private short more;

	public FIFOStream(byte[] data, short more) {
		this.data = data;
		this.more = more;
	}

	public byte[] getData() {
		return data;
	}

	public short getMore() {
		return more;
	}
}
