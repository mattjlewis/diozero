package com.diozero.internal.provider.sysfs;

import com.diozero.util.RuntimeIOException;

/**
 * <p>Native Java implementation of the I2C SMBus commands using a single native method to select the slave address.</p>
 * <p>Reference <a href="https://www.kernel.org/doc/Documentation/i2c/dev-interface">Kernel I2C dev interface</a>
 * and <a href="https://www.kernel.org/doc/Documentation/i2c/smbus-protocol">SMBus Protocol</a>.</p>
 * <p><em>Warning</em> Not all methods have been tested!</p>
 */
public class NativeI2CDeviceSMBus implements I2CSMBusInterface {
	private static native int smbusOpen(String adapter, int deviceAddress, boolean force);
	private static native void smbusClose(int fd);
	
	private static native int writeQuick(int fd, byte value);
	private static native int readByte(int fd);
	private static native int writeByte(int fd, byte value);
	private static native int readByteData(int fd, int registerAddress);
	private static native int writeByteData(int fd, int registerAddress, byte value);
	private static native int readWordData(int fd, int registerAddress);
	private static native int writeWordData(int fd, int registerAddress, short value);
	private static native int processCall(int fd, int registerAddress, short value);
	private static native int readBlockData(int fd, int registerAddress, byte[] rxData);
	private static native int writeBlockData(int fd, int registerAddress, int txLength, byte[] txData);
	private static native int readI2CBlockData(int fd, int registerAddress, int rxLength, byte[] rxData);
	private static native int writeI2CBlockData(int fd, int registerAddress, int txLength, byte[] txData);
	private static native int blockProcessCall(int fd, int registerAddress, int txLength, byte[] txData, byte[] rxData);
	
	private int controller;
	private int deviceAddress;
	private int fd;
	
	public NativeI2CDeviceSMBus(int controller, int deviceAddress, boolean force)
			throws I2CSMBusInterface.NotSupportedException {
		this.controller = controller;
		this.deviceAddress = deviceAddress;
		String device_file = "/dev/i2c-" + controller;
		
		fd = smbusOpen(device_file, deviceAddress, force);
		if (fd < 0) {
			throw new I2CSMBusInterface.NotSupportedException();
		}
	}

	@Override
	public void close() {
		smbusClose(fd);
	}

	@Override
	public byte readByte() {
		int rc = readByte(fd);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.readByte, rc=" + rc);
		}
		
		return (byte) rc;
	}

	@Override
	public void writeByte(byte data) {
		int rc = writeByte(fd, data);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.writeByte, rc=" + rc);
		}
	}
	
	@Override
	public byte[] readBytes(int length) {
		// TODO Test this actually works!
		return readI2CBlockData(0, length);
	}
	
	@Override
	public void writeBytes(byte[] data) {
		// TODO Test this actually works!
		writeI2CBlockData(0, data);
	}

	@Override
	public byte readByteData(int registerAddress) {
		int rc = readByteData(fd, registerAddress);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.readByteData, rc=" + rc);
		}
		
		return (byte) rc;
	}

	@Override
	public void writeByteData(int registerAddress, byte data) {
		int rc = writeByteData(fd, registerAddress, data);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.writeByteData, rc=" + rc);
		}
	}

	@Override
	public short readWordData(int registerAddress) {
		int rc = readWordData(fd, registerAddress);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.readWordData, rc=" + rc);
		}
		
		return (short) rc;
	}

	@Override
	public void writeWordData(int registerAddress, short data) {
		int rc = writeWordData(fd, registerAddress, data);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.writeWordData, rc=" + rc);
		}
	}

	@Override
	public short processCall(int registerAddress, short data) {
		int rc = processCall(fd, registerAddress, data);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.processCall, rc=" + rc);
		}
		
		return (short) rc;
	}

	@Override
	public byte[] readBlockData(int registerAddress) {
		byte[] data = new byte[MAX_I2C_BLOCK_SIZE];
		int rc = readBlockData(fd, registerAddress, data);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.readI2CBlockData, rc=" + rc);
		}
		
		return data;
	}

	@Override
	public void writeBlockData(int registerAddress, byte[] data) {
		int rc = writeBlockData(fd, registerAddress, data.length, data);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.writeI2CBlockData, rc=" + rc);
		}
	}

	@Override
	public byte[] blockProcessCall(int registerAddress, byte[] txData, int length) {
		byte[] rx_data = new byte[length];
		int rc = blockProcessCall(fd, registerAddress, txData.length, txData, rx_data);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.readBlockData, rc=" + rc);
		}
		
		return rx_data;
	}

	@Override
	public byte[] readI2CBlockData(int registerAddress, int length) {
		byte[] data = new byte[length];
		int rc = readI2CBlockData(fd, registerAddress, length, data);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.readI2CBlockData, rc=" + rc);
		}
		
		return data;
	}

	@Override
	public void writeI2CBlockData(int registerAddress, byte[] data) {
		int rc = writeI2CBlockData(fd, registerAddress, data.length, data);
		if (rc < 0) {
			throw new RuntimeIOException("Error in SMBus.writeI2CBlockData, rc=" + rc);
		}
	}
}
