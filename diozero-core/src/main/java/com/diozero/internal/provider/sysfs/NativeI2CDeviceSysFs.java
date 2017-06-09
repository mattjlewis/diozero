package com.diozero.internal.provider.sysfs;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 - 2017 mattjlewis
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */


import java.io.IOException;
import java.io.RandomAccessFile;

import org.pmw.tinylog.Logger;

import com.diozero.util.FileUtil;
import com.diozero.util.RuntimeIOException;

/**
 * <p>Native Java implementation of the I2C SMBus commands using sysfs and a single native method to select the slave address.</p>
 *
 * <p>Reference <a href="https://www.kernel.org/doc/Documentation/i2c/dev-interface">Kernel I2C dev interface</a>
 * and <a href="https://www.kernel.org/doc/Documentation/i2c/smbus-protocol">SMBus Protocol</a>.</p>
 * <p><em>Warning</em> Not all methods have been tested!</p>
 */
public class NativeI2CDeviceSysFs implements I2CSMBusInterface {
	private static native int selectSlave(int fd, int deviceAddress, boolean force);

	private RandomAccessFile deviceFile;
	private int controller;
	private int deviceAddress;

	public NativeI2CDeviceSysFs(int controller, int deviceAddress, boolean force) {
		this.controller = controller;
		this.deviceAddress = deviceAddress;
		String device_file = "/dev/i2c-" + controller;
		
		try {
			deviceFile = new RandomAccessFile(device_file, "rwd");
			int fd = FileUtil.getNativeFileDescriptor(deviceFile.getFD());
			if (selectSlave(fd, deviceAddress, force) < 0) {
				throw new RuntimeIOException("Error selecting I2C address " + deviceAddress + " for controller " + controller);
			}
		} catch (IOException e) {
			throw new RuntimeIOException("Error opening i2c device " + device_file);
		}
	}
	
	@Override
	public void close() {
		try {
			deviceFile.close();
		} catch (IOException e) {
			throw new RuntimeIOException("Error closing I2C device file: " + e, e);
		}
	}
	
	@Override
	public byte readByte() {
		try {
			return (byte) deviceFile.readUnsignedByte();
		} catch (IOException e) {
			throw new RuntimeIOException("Error in I2C readByte: " + e, e);
		}
	}
	
	@Override
	public void writeByte(byte data) {
		try {
			deviceFile.writeByte(data & 0xff);
		} catch (IOException e) {
			throw new RuntimeIOException("Error in I2C writeByte: " + e, e);
		}
	}
	
	@Override
	public byte[] readBytes(int length) {
		byte[] buffer = new byte[length];
		try {
			int read = deviceFile.read(buffer);
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
			deviceFile.write(data);
		} catch (IOException e) {
			throw new RuntimeIOException("Error in I2C writeBytes: " + e, e);
		}
	}
	
	@Override
	public byte readByteData(int register) {
		try {
			deviceFile.writeByte(register);
			return (byte) deviceFile.readUnsignedByte();
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
			deviceFile.write(buffer);
		} catch (IOException e) {
			throw new RuntimeIOException("Error in I2C writeByteData(" + register + "): " + e, e);
		}
	}

	@Override
	public short readWordData(int register) {
		try {
			deviceFile.writeByte(register);
			return (short) deviceFile.readUnsignedShort();
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
			deviceFile.write(buffer);
		} catch (IOException e) {
			throw new RuntimeIOException("Error in I2C writeWordData(" + register + "): " + e, e);
		}
	}
	
	@Override
	public short processCall(int register, short data) {
		writeWordData(register, data);
		try {
			return (short) deviceFile.readUnsignedShort();
		} catch (IOException e) {
			throw new RuntimeIOException("Error in I2C processCall(" + register + "): " + e, e);
		}
	}
	
	@Override
	public byte[] readBlockData(int register) {
		byte[] rx_data;
		try {
			deviceFile.write(register);
			byte[] buffer = new byte[MAX_I2C_BLOCK_SIZE+1];
			int rc = deviceFile.read(buffer);
			if (rc < 0) {
				throw new RuntimeIOException("Error reading from I2C device: " + rc);
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
			deviceFile.write(buffer);
		} catch (IOException e) {
			throw new RuntimeIOException("I2C Error in writeBlockData(" + register + "): " + e, e);
		}
	}
	
	@Override
	public byte[] blockProcessCall(int register, byte[] data, int length) {
		writeBlockData(register, data);
		byte[] buffer = new byte[length];
		try {
			int read = deviceFile.read(buffer);
			if (read == -1) {
				throw new RuntimeIOException("Error response from deviceFile.read");
			}
			if (read != length) {
				Logger.warn("Expected to read " + length + " bytes, read " + read + " bytes");
			}
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
			deviceFile.write(register);
			int read = deviceFile.read(buffer);
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
			deviceFile.write(buffer);
		} catch (IOException e) {
			throw new RuntimeIOException("I2C Error in writeBlockData(" + register + "): " + e, e);
		}
	}
}
