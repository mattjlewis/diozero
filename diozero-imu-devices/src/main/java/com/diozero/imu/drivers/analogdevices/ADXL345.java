package com.diozero.imu.drivers.analogdevices;

import java.nio.ByteBuffer;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import com.diozero.api.*;
import com.diozero.api.imu.ImuData;
import com.diozero.api.imu.ImuInterface;
import com.diozero.imu.drivers.invensense.ImuDataFactory;
import com.diozero.util.BitManipulation;
import com.diozero.util.RuntimeIOException;

/**
 * http://www.analog.com/media/en/technical-documentation/data-sheets/ADXL345.PDF
 */
public class ADXL345 implements ImuInterface {
	// 13-bit
	private static final int RESOLUTION = 13;
	private static final int RANGE = (int)Math.pow(2, RESOLUTION);
	// From -16 to 16
	private static final double MAX_RANGE = 16*2;
	private static final double SCALE_FACTOR = MAX_RANGE / RANGE;
	
	private static final int ADXL345_ADDRESS = 0x53;

	// Registers
	private static final byte POWER_CTL = 0x2D;
	
	private static final byte POWER_CTL_MEASURE_BIT = 3;
	private static final byte POWER_CTL_MEASURE = BitManipulation.getBitMask(POWER_CTL_MEASURE_BIT);

	private I2CDevice device;
	
	public ADXL345() {
		device = new I2CDevice(I2CConstants.BUS_1, ADXL345_ADDRESS,
				I2CConstants.ADDR_SIZE_7, I2CConstants.DEFAULT_CLOCK_FREQUENCY);
	}
	
	@Override
	public ImuData getIMUData() throws RuntimeIOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector3D getGyroData() throws RuntimeIOException {
		throw new UnsupportedOperationException("ADXL345 doesn't hava a gyro");
	}

	@Override
	public Vector3D getAccelerometerData() throws RuntimeIOException {
		// Enable measure mode
		device.writeByte(POWER_CTL, POWER_CTL_MEASURE);
		
		ByteBuffer data = ByteBuffer.wrap(device.readBytes(0x32, 6));
		short x = data.getShort();
		short y = data.getShort();
		short z = data.getShort();
		
		return ImuDataFactory.createVector(new short[] {x, y, z}, SCALE_FACTOR);
	}
	
	@Override
	public Vector3D getCompassData() throws RuntimeIOException {
		throw new UnsupportedOperationException("ADXL345 doesn't hava a compass");
	}
}
