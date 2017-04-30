package com.diozero.internal.provider.sysfs;

import java.io.IOException;
import java.io.RandomAccessFile;

import com.diozero.util.RuntimeIOException;

/**
 * Emulate the SMBus commands using sysfs
 */
public class NativeI2CDeviceSysFs implements I2CSMBusInterface {
	private static native int selectSlave(int fd, int deviceAddress, boolean force);

	private RandomAccessFile i2cDeviceFile;
	private int controller;
	private int deviceAddress;
	private int fd;

	@SuppressWarnings("restriction")
	public NativeI2CDeviceSysFs(int controller, int deviceAddress, boolean force) {
		this.controller = controller;
		this.deviceAddress = deviceAddress;
		String device_file = "/dev/i2c-" + controller;
		
		try {
			i2cDeviceFile = new RandomAccessFile(device_file, "rwd");
			fd = sun.misc.SharedSecrets.getJavaIOFileDescriptorAccess().get(i2cDeviceFile.getFD());
			
			selectSlave(force);
		} catch (IOException e) {
			throw new RuntimeIOException("Error opening i2c device " + device_file);
		}
	}
	
	private void selectSlave(boolean force) {
		if (selectSlave(fd, deviceAddress, force) < 0) {
			throw new RuntimeIOException("Error selecting I2C address " + deviceAddress + " for controller " + controller);
		}
	}
	
	@Override
	public byte readByte() {
		try {
			return (byte) i2cDeviceFile.readUnsignedByte();
		} catch (IOException e) {
			throw new RuntimeIOException("Error in I2C readByte: " + e, e);
		}
	}
	
	@Override
	public void writeByte(byte data) {
		try {
			i2cDeviceFile.writeByte(data & 0xff);
		} catch (IOException e) {
			throw new RuntimeIOException("Error in I2C writeByte: " + e, e);
		}
	}
	
	@Override
	public byte[] readBytes(int length) {
		byte[] buffer = new byte[length];
		try {
			int read = i2cDeviceFile.read(buffer);
			if (read < 0 || read != length) {
				throw new RuntimeIOException("Didn't read correct number of bytes, read " + read + ", expected " + length);
			}
		} catch (IOException e) {
			throw new RuntimeIOException("I2C Error in readBytes: " + e, e);
		}
		
		return buffer;
	}
	
	@Override
	public void writeBytes(byte[] data) {
		try {
			i2cDeviceFile.write(data);
		} catch (IOException e) {
			throw new RuntimeIOException("Error in I2C writeBytes: " + e, e);
		}
	}
	
	@Override
	public byte readByteData(int register) {
		try {
			i2cDeviceFile.writeByte(register);
			return (byte) i2cDeviceFile.readUnsignedByte();
		} catch (IOException e) {
			throw new RuntimeIOException("Error in I2C readByteData(" + register + "): " + e, e);
		}
	}
	
	@Override
	public void writeByteData(int register, byte data) {
		byte [] buffer = new byte[2];
		buffer[0] = (byte) register; 
		buffer[1] = data;
		try {
			i2cDeviceFile.write(buffer);
		} catch (IOException e) {
			throw new RuntimeIOException("Error in I2C writeByteData(" + register + "): " + e, e);
		}
	}

	@Override
	public short readWordData(int register) {
		try {
			i2cDeviceFile.writeByte(register);
			return (short) i2cDeviceFile.readUnsignedShort();
		} catch (IOException e) {
			throw new RuntimeIOException("Error in I2C readWordData(" + register + "): " + e, e);
		}
	}
	
	@Override
	public void writeWordData(int register, short data) {
		byte [] buffer = new byte[3];
		buffer[0] = (byte) register; 
		buffer[1] = (byte) (data & 0xff);
		buffer[2] = (byte) ((data >> 8) & 0xff);
		try {
			i2cDeviceFile.write(buffer);
		} catch (IOException e) {
			throw new RuntimeIOException("Error in I2C writeWordData(" + register + "): " + e, e);
		}
	}
	
	@Override
	public short processCall(int register, short data) {
		writeWordData(register, data);
		try {
			return (short) i2cDeviceFile.readUnsignedShort();
		} catch (IOException e) {
			throw new RuntimeIOException("Error in I2C processCall(" + register + "): " + e, e);
		}
	}
	
	@Override
	public byte[] readBlockData(int register) {
		byte[] rx_data;
		try {
			i2cDeviceFile.write(register);
			byte[] buffer = new byte[MAX_I2C_BLOCK_SIZE+1];
			int rc = i2cDeviceFile.read(buffer);
			if (rc < 0) {
				throw new RuntimeIOException("Error reading from I2C device");
			}
			int read = buffer[0] & 0xff;
			rx_data = new byte[read];
			System.arraycopy(buffer, 1, rx_data, 0, read);
		} catch (IOException e) {
			throw new RuntimeIOException("I2C Error in readBlockData: " + e, e);
		}
		
		return rx_data;
	}
	
	@Override
	public void writeBlockData(int register, byte[] data) {
		byte[] buffer = new byte[data.length+2];
		buffer[0] = (byte) register;
		buffer[1] = (byte) data.length;
		System.arraycopy(data, 0, buffer, 2, data.length);
		
		try {
			i2cDeviceFile.write(buffer);
		} catch (IOException e) {
			throw new RuntimeIOException("I2C Error in writeBlockData(" + register + "): " + e, e);
		}
	}
	
	@Override
	public byte[] blockProcessCall(int register, byte[] data, int length) {
		writeBlockData(register, data);
		byte[] buffer = new byte[length];
		try {
			i2cDeviceFile.read(buffer);
		} catch (IOException e) {
			throw new RuntimeIOException("I2C Error in blockProcessCall(" + register + "): " + e, e);
		}
		return buffer;
	}
	
	@Override
	public byte[] readI2CBlockData(int register, int length) {
		if (length >= MAX_I2C_BLOCK_SIZE) {
			length = MAX_I2C_BLOCK_SIZE;
		}
		
		byte[] buffer = new byte[length];
		try {
			i2cDeviceFile.write(register);
			int read = i2cDeviceFile.read(buffer);
			if (read < 0 || read != length) {
				throw new RuntimeIOException("Didn't read correct number of bytes, read " + read + ", expected " + length);
			}
		} catch (IOException e) {
			throw new RuntimeIOException("I2C Error in readBlockData: " + e, e);
		}
		
		return buffer;
	}
	
	@Override
	public void writeI2CBlockData(int register, byte[] data) {
		byte[] buffer = new byte[data.length+1];
		buffer[0] = (byte) register;
		System.arraycopy(data, 0, buffer, 1, data.length);
		
		try {
			i2cDeviceFile.write(buffer);
		} catch (IOException e) {
			throw new RuntimeIOException("I2C Error in writeBlockData(" + register + "): " + e, e);
		}
	}
	
	@Override
	public void close() {
		try {
			i2cDeviceFile.close();
		} catch (IOException e) {
			throw new RuntimeIOException("Error closing I2C device file: " + e, e);
		}
	}
}
