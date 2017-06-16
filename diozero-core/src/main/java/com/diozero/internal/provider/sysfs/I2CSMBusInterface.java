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


import java.io.Closeable;

public interface I2CSMBusInterface extends Closeable {
	static final int MAX_I2C_BLOCK_SIZE = 32;
	
	@Override
	void close();
	
	/**
	 * SMBus Receive Byte:  i2c_smbus_read_byte()
	 * This reads a single byte from a device, without specifying a device
	 * register. Some devices are so simple that this interface is enough; for
	 * others, it is a shorthand if you want to read the same register as in
	 * the previous SMBus command.
	 * S Addr Rd [A] [Data] NA P
	 * @return Unsigned byte
	 */
	byte readByte();

	/**
	 * SMBus Send Byte:  i2c_smbus_write_byte()
	 * This operation is the reverse of Receive Byte: it sends a single byte
	 * to a device.  See Receive Byte for more information.
	 * S Addr Wr [A] Data [A] P
	 * @param data value to write
	 */
	void writeByte(byte data);
	
	byte[] readBytes(int length);
	void writeBytes(byte[] data);
	
	/**
	 * SMBus Read Byte:  i2c_smbus_read_byte_data()
	 * This reads a single byte from a device, from a designated register.
	 * The register is specified through the Comm byte.
	 * S Addr Wr [A] Comm [A] S Addr Rd [A] [Data] NA P
	 * @param register the register to read from
	 * @return data read as unsigned byte
	 */
	byte readByteData(int register);
	
	/**
	 * SMBus Write Byte:  i2c_smbus_write_byte_data()
	 * This writes a single byte to a device, to a designated register. The
	 * register is specified through the Comm byte. This is the opposite of
	 * the Read Byte operation.
	 * S Addr Wr [A] Comm [A] Data [A] P
	 * @param register the register to write to
	 * @param data value to write
	 */
	void writeByteData(int register, byte data);
	
	/**
	 * SMBus Read Word:  i2c_smbus_read_word_data()
	 * This operation is very like Read Byte; again, data is read from a
	 * device, from a designated register that is specified through the Comm
	 * byte. But this time, the data is a complete word (16 bits).
	 * S Addr Wr [A] Comm [A] S Addr Rd [A] [DataLow] A [DataHigh] NA P
	 * @param register the register to read from
	 * @return data read as unsigned short
	 */
	short readWordData(int register);

	/**
	 * SMBus Write Word:  i2c_smbus_write_word_data()
	 * This is the opposite of the Read Word operation. 16 bits
	 * of data is written to a device, to the designated register that is
	 * specified through the Comm byte. 
	 * S Addr Wr [A] Comm [A] DataLow [A] DataHigh [A] P
	 * @param register the register to write to
	 * @param data value to write
	 */
	void writeWordData(int register, short data);

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
	short processCall(int register, short data);

	/**
	 * SMBus Block Read:  i2c_smbus_read_block_data()
	 * This command reads a block of up to 32 bytes from a device, from a 
	 * designated register that is specified through the Comm byte. The amount
	 * of data is specified by the device in the Count byte.
	 * S Addr Wr [A] Comm [A] 
	 * 		S Addr Rd [A] [Count] A [Data] A [Data] A ... A [Data] NA P
	 * @param register the register to read from
	 * @return data read
	 */
	byte[] readBlockData(int register);

	/**
	 * SMBus Block Write:  i2c_smbus_write_block_data()
	 * The opposite of the Block Read command, this writes up to 32 bytes to 
	 * a device, to a designated register that is specified through the
	 * Comm byte. The amount of data is specified in the Count byte.
	 * S Addr Wr [A] Comm [A] Count [A] Data [A] Data [A] ... [A] Data [A] P
	 * @param register the register to write to
	 * @param data values to write
	 */
	void writeBlockData(int register, byte[] data);

	/**
	 * SMBus Block Write - Block Read Process Call
	 * SMBus Block Write - Block Read Process Call was introduced in
	 * Revision 2.0 of the specification.
	 * This command selects a device register (through the Comm byte), sends
	 * 1 to 31 bytes of data to it, and reads 1 to 31 bytes of data in return.
	 * S Addr Wr [A] Comm [A] Count [A] Data [A] ...
	 * 		S Addr Rd [A] [Count] A [Data] ... A P
	 * @param register the register to write to
	 * @param data data to write
	 * @param length Length
	 * @return data read
	 */
	byte[] blockProcessCall(int register, byte[] data, int length);

	/**
	 * I2C Block Transactions
	 * ======================
	 * The following I2C block transactions are supported by the
	 * SMBus layer and are described here for completeness.
	 * They are *NOT* defined by the SMBus specification.
	 * I2C block transactions do not limit the number of bytes transferred
	 * but the SMBus layer places a limit of 32 bytes.
	 */
	
	/**
	 * I2C Block Read:  i2c_smbus_read_i2c_block_data()
	 * This command reads a block of bytes from a device, from a
	 * designated register that is specified through the Comm byte.
	 * S Addr Wr [A] Comm [A]
	 *      S Addr Rd [A] [Data] A [Data] A ... A [Data] NA P
	 * Functionality flag: I2C_FUNC_SMBUS_READ_I2C_BLOCK
	 * @param register the register to read from
	 * @param length amount of data to read
	 * @return values to read
	 */
	byte[] readI2CBlockData(int register, int length);

	/**
	 * I2C Block Write:  i2c_smbus_write_i2c_block_data()
	 * The opposite of the Block Read command, this writes bytes to
	 * a device, to a designated register that is specified through the
	 * Comm byte. Note that command lengths of 0, 2, or more bytes are
	 * supported as they are indistinguishable from data.
	 * S Addr Wr [A] Comm [A] Data [A] Data [A] ... [A] Data [A] P
	 * Functionality flag: I2C_FUNC_SMBUS_WRITE_I2C_BLOCK
	 * @param register the register to write to
	 * @param data values to write
	 */
	void writeI2CBlockData(int register, byte[] data);
	
	class NotSupportedException extends Exception {
		private static final long serialVersionUID = -6962554229986493047L;
	}
}
