package com.diozero.imu.drivers.invensense;

import java.util.Arrays;

public class FIFOData {
	// Gyro data in hardware units.
	private short[] gyro;
	// Accel data in hardware units.
	private short[] accel;
	// 3-axis quaternion data in hardware units.
	private int[] quat;
	// Timestamp in milliseconds.
    private long timestamp;
    // Mask of sensors read from FIFO.
    private short sensors;
    // Number of remaining packets.
    private int more;
    
	public FIFOData(short[] gyro, short[] accel, int[] quat, long timestamp, short sensors, int more) {
		this.gyro = gyro;
		this.accel = accel;
		this.quat = quat;
		this.timestamp = timestamp;
		this.sensors = sensors;
		this.more = more;
	}

	public short[] getGyro() {
		return gyro;
	}

	public short[] getAccel() {
		return accel;
	}

	public int[] getQuat() {
		return quat;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public short getSensors() {
		return sensors;
	}

	public int getMore() {
		return more;
	}

	@Override
	public String toString() {
		return "FIFOData [gyro=" + Arrays.toString(gyro) + ", accel=" + Arrays.toString(accel) + ", quat="
				+ Arrays.toString(quat) + ", timestamp=" + timestamp + ", sensors=" + sensors + ", more=" + more + "]";
	}
}
