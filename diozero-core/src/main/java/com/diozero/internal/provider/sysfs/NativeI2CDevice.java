package com.diozero.internal.provider.sysfs;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 mattjlewis
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


import java.io.Closeable;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.diozero.util.LibraryLoader;
import com.diozero.util.RuntimeIOException;

/**
 * <p>Native Java implementation of the I2C SMBus commands using a single native method to select the slave address.</p>
 * <p>Reference <a href="https://www.kernel.org/doc/Documentation/i2c/dev-interface">Kernel I2C dev interface</a>
 * and <a href="https://www.kernel.org/doc/Documentation/i2c/smbus-protocol">SMBus Protocol</a>.</p>
 * <p><em>Warning</em> Not all methods have been tested!</p>
 */
public class NativeI2CDevice implements Closeable {
	static {
		LibraryLoader.loadLibrary(NativeI2CDevice.class, "diozero-system-utils");
	}
	
	private static native int selectSlave(int fd, int address);
	
	private RandomAccessFile i2cDeviceFile;
	private int controller;
	private int address;
	private int fd;

	@SuppressWarnings("restriction")
	public NativeI2CDevice(int controller, int address) {
		this.controller = controller;
		this.address = address;
		String device_file = "/dev/i2c-" + controller;
		
		try {
			i2cDeviceFile = new RandomAccessFile(device_file, "rw");
			fd = sun.misc.SharedSecrets.getJavaIOFileDescriptorAccess().get(i2cDeviceFile.getFD());
			
			selectSlave();
		} catch (IOException e) {
			throw new RuntimeIOException("Error opening i2c device " + device_file);
		}
	}
	
	private void selectSlave() {
		if (selectSlave(fd, address) < 0) {
			throw new RuntimeIOException("Error selecting I2C address " + address + " for controller " + controller);
		}
	}
	
	/**
	 * SMBus Receive Byte:  i2c_smbus_read_byte()
	 * This reads a single byte from a device, without specifying a device
	 * register. Some devices are so simple that this interface is enough; for
	 * others, it is a shorthand if you want to read the same register as in
	 * the previous SMBus command.
	 * S Addr Rd [A] [Data] NA P
	 * @return Unsigned byte
	 */
	public int readByte() {
		try {
			return i2cDeviceFile.readUnsignedByte();
		} catch (IOException e) {
			throw new RuntimeIOException("Error in I2C readByte: " + e, e);
		}
	}
	
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
	
	/**
	 * SMBus Send Byte:  i2c_smbus_write_byte()
	 * This operation is the reverse of Receive Byte: it sends a single byte
	 * to a device.  See Receive Byte for more information.
	 * S Addr Wr [A] Data [A] P
	 * @param data value to write
	 */
	public void writeByte(byte data) {
		try {
			i2cDeviceFile.writeByte(data & 0xff);
		} catch (IOException e) {
			throw new RuntimeIOException("Error in I2C writeByte: " + e, e);
		}
	}
	
	public void writeBytes(byte[] data) {
		try {
			i2cDeviceFile.write(data);
		} catch (IOException e) {
			throw new RuntimeIOException("Error in I2C writeBytes: " + e, e);
		}
	}
	
	/**
	 * SMBus Read Byte:  i2c_smbus_read_byte_data()
	 * This reads a single byte from a device, from a designated register.
	 * The register is specified through the Comm byte.
	 * S Addr Wr [A] Comm [A] S Addr Rd [A] [Data] NA P
	 * @param register the register to read from
	 * @return data read as unsigned byte
	 */
	public int readByteData(int register) {
		try {
			i2cDeviceFile.writeByte(register);
			return i2cDeviceFile.readUnsignedByte();
		} catch (IOException e) {
			throw new RuntimeIOException("Error in I2C readByteData(" + register + "): " + e, e);
		}
	}

	/**
	 * SMBus Read Word:  i2c_smbus_read_word_data()
	 * This operation is very like Read Byte; again, data is read from a
	 * device, from a designated register that is specified through the Comm
	 * byte. But this time, the data is a complete word (16 bits).
	 * S Addr Wr [A] Comm [A] S Addr Rd [A] [DataLow] A [DataHigh] NA P
	 * @param register the register to read from
	 * @return data read as unsigned short
	 */
	public int readWordData(int register) {
		try {
			i2cDeviceFile.writeByte(register);
			return i2cDeviceFile.readUnsignedShort();
		} catch (IOException e) {
			throw new RuntimeIOException("Error in I2C readWordData(" + register + "): " + e, e);
		}
	}
	
	/**
	 * SMBus Write Byte:  i2c_smbus_write_byte_data()
	 * This writes a single byte to a device, to a designated register. The
	 * register is specified through the Comm byte. This is the opposite of
	 * the Read Byte operation.
	 * S Addr Wr [A] Comm [A] Data [A] P
	 * @param register the register to write to
	 * @param data value to write
	 */
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
	
	/**
	 * SMBus Write Word:  i2c_smbus_write_word_data()
	 * This is the opposite of the Read Word operation. 16 bits
	 * of data is written to a device, to the designated register that is
	 * specified through the Comm byte. 
	 * S Addr Wr [A] Comm [A] DataLow [A] DataHigh [A] P
	 * @param register the register to write to
	 * @param data value to write
	 */
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
	
	/**
	 * SMBus Process Call:
	 * This command selects a device register (through the Comm byte), sends
	 * 16 bits of data to it, and reads 16 bits of data in return.
	 * S Addr Wr [A] Comm [A] DataLow [A] DataHigh [A] 
	 * 		S Addr Rd [A] [DataLow] A [DataHigh] NA P
	 * @param register the register to write to / read from
	 * @param data value to write
	 * @return data read
	 */
	public int processCall(int register, short data) {
		writeWordData(register, data);
		try {
			return i2cDeviceFile.readUnsignedShort();
		} catch (IOException e) {
			throw new RuntimeIOException("Error in I2C processCall(" + register + "): " + e, e);
		}
	}
	
	/**
	 * SMBus Block Read:  i2c_smbus_read_block_data()
	 * This command reads a block of up to 32 bytes from a device, from a 
	 * designated register that is specified through the Comm byte. The amount
	 * of data is specified by the device in the Count byte.
	 * S Addr Wr [A] Comm [A] 
	 * 		S Addr Rd [A] [Count] A [Data] A [Data] A ... A [Data] NA P
	 * @param register the register to read from
	 * @param length amount of data to read
	 * @return data read
	 */
	public byte[] readBlockData(int register, int length) {
		if (length >= 32) {
			throw new IllegalArgumentException("Can only read up to 32 bytes");
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
	
	/**
	 * SMBus Block Write:  i2c_smbus_write_block_data()
	 * The opposite of the Block Read command, this writes up to 32 bytes to 
	 * a device, to a designated register that is specified through the
	 * Comm byte. The amount of data is specified in the Count byte.
	 * S Addr Wr [A] Comm [A] Count [A] Data [A] Data [A] ... [A] Data [A] P
	 * @param register the register to write to
	 * @param data values to write
	 */
	public void writeBlockData(int register, byte[] data) {
		byte[] buffer = new byte[data.length+1];
		buffer[0] = (byte) register;
		System.arraycopy(data, 0, buffer, 1, data.length);
		
		try {
			i2cDeviceFile.write(buffer);
		} catch (IOException e) {
			throw new RuntimeIOException("I2C Error in writeBlockData(" + register + "): " + e, e);
		}
	}
	
	/**
	 * SMBus Block Write - Block Read Process Call
	 * SMBus Block Write - Block Read Process Call was introduced in
	 * Revision 2.0 of the specification.
	 * This command selects a device register (through the Comm byte), sends
	 * 1 to 31 bytes of data to it, and reads 1 to 31 bytes of data in return.
	 * S Addr Wr [A] Comm [A] Count [A] Data [A] ...
	 * 		S Addr Rd [A] [Count] A [Data] ... A P
	 * @param register the register to write to
	 * @param data data read
	 */
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
	public void close() {
		try {
			i2cDeviceFile.close();
		} catch (IOException e) {
			throw new RuntimeIOException("Error closing I2C device file: " + e, e);
		}
	}
}
