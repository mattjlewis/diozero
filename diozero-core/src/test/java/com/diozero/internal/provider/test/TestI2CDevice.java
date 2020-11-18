package com.diozero.internal.provider.test;

import org.tinylog.Logger;

import com.diozero.api.I2CDevice;
import com.diozero.internal.provider.AbstractDevice;
import com.diozero.internal.provider.DeviceFactoryInterface;
import com.diozero.internal.provider.I2CDeviceInterface;
import com.diozero.util.RuntimeIOException;

public class TestI2CDevice extends AbstractDevice implements I2CDeviceInterface {
	public TestI2CDevice(String key, DeviceFactoryInterface deviceFactory, int controller, int address,
			int addressSize) {
		super(key, deviceFactory);
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.trace("closeDevice()");
	}

	@Override
	public boolean probe(I2CDevice.ProbeMode mode) {
		return true;
	}

	@Override
	public void writeQuick(byte bit) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public byte readByte() throws RuntimeIOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeByte(byte b) throws RuntimeIOException {
		// TODO Auto-generated method stub

	}

	@Override
	public byte readByteData(int register) throws RuntimeIOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeByteData(int register, byte b) throws RuntimeIOException {
		// TODO Auto-generated method stub
	}

	@Override
	public short readWordData(int register) throws RuntimeIOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeWordData(int register, short data) throws RuntimeIOException {
		// TODO Auto-generated method stub

	}

	@Override
	public short processCall(int register, short data) throws RuntimeIOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int readBytes(byte[] buffer) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeBytes(byte[] data) throws RuntimeIOException {
		// TODO Auto-generated method stub

	}

	@Override
	public int readBlockData(int register, byte[] buffer) throws RuntimeIOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeBlockData(int register, byte[] data) throws RuntimeIOException {
		// TODO Auto-generated method stub

	}

	@Override
	public byte[] blockProcessCall(int register, byte[] data) throws RuntimeIOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void readI2CBlockData(int register, byte[] buffer) throws RuntimeIOException {
		// TODO Auto-generated method stub

	}

	@Override
	public void writeI2CBlockData(int register, byte[] data) throws RuntimeIOException {
		// TODO Auto-generated method stub

	}
}
