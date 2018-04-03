package com.diozero.internal.provider.sysfs;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Core
 * Filename:     I2CSMBusInterface.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
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

import com.diozero.api.I2CDevice;

public interface I2CSMBusInterface extends Closeable {
	static final int MAX_I2C_BLOCK_SIZE = 32;
	
	@Override
	void close();
	
	/**
	 * Probe this I2C device
	 * @param mode Probe mode
	 * @return True if the probe is successful
	 */
	boolean probe(I2CDevice.ProbeMode mode);
	
	/**
	 * <p>SMBus Quick Command</p>
	 * <p>This sends a single bit to the device, at the place of the Rd/Wr bit.</p>
	 * <pre>A Addr Rd/Wr [A] P</pre>
	 * @param bit The bit to write
	 */
	void writeQuick(byte bit);
	
	/**
	 * <p>SMBus Receive Byte:  i2c_smbus_read_byte()</p>
	 * <p>This reads a single byte from a device, without specifying a device
	 * register. Some devices are so simple that this interface is enough; for
	 * others, it is a shorthand if you want to read the same register as in
	 * the previous SMBus command.</p>
	 * <pre>S Addr Rd [A] [Data] NA P</pre>
	 * @return Unsigned byte
	 */
	byte readByte();

	/**
	 * <p>SMBus Send Byte:  i2c_smbus_write_byte()</p>
	 * <p>This operation is the reverse of Receive Byte: it sends a single byte
	 * to a device.  See Receive Byte for more information.</p>
	 * <pre>S Addr Wr [A] Data [A] P</pre>
	 * @param data value to write
	 */
	void writeByte(byte data);
	
	byte[] readBytes(int length);
	void writeBytes(byte[] data);
	
	/**
	 * <p>SMBus Read Byte:  i2c_smbus_read_byte_data()</p>
	 * <p>This reads a single byte from a device, from a designated register.
	 * The register is specified through the Comm byte.</p>
	 * <pre>S Addr Wr [A] Comm [A] S Addr Rd [A] [Data] NA P</pre>
	 * @param register the register to read from
	 * @return data read as unsigned byte
	 */
	byte readByteData(int register);
	
	/**
	 * <p>SMBus Write Byte:  i2c_smbus_write_byte_data()</p>
	 * <p>This writes a single byte to a device, to a designated register. The
	 * register is specified through the Comm byte. This is the opposite of
	 * the Read Byte operation.</p>
	 * <pre>S Addr Wr [A] Comm [A] Data [A] P</pre>
	 * @param register the register to write to
	 * @param data value to write
	 */
	void writeByteData(int register, byte data);
	
	/**
	 * <p>SMBus Read Word:  i2c_smbus_read_word_data()</p>
	 * <p>This operation is very like Read Byte; again, data is read from a
	 * device, from a designated register that is specified through the Comm
	 * byte. But this time, the data is a complete word (16 bits).</p>
	 * <pre>S Addr Wr [A] Comm [A] S Addr Rd [A] [DataLow] A [DataHigh] NA P</pre>
	 * @param register the register to read from
	 * @return data read as unsigned short
	 */
	short readWordData(int register);

	/**
	 * <p>SMBus Write Word:  i2c_smbus_write_word_data()</p>
	 * <p>This is the opposite of the Read Word operation. 16 bits
	 * of data is written to a device, to the designated register that is
	 * specified through the Comm byte.</p> 
	 * <pre>S Addr Wr [A] Comm [A] DataLow [A] DataHigh [A] P</pre>
	 * @param register the register to write to
	 * @param data value to write
	 */
	void writeWordData(int register, short data);

	/**
	 * <p>SMBus Process Call</p>
	 * <p>This command selects a device register (through the Comm byte), sends
	 * 16 bits of data to it, and reads 16 bits of data in return.</p>
	 * <pre>S Addr Wr [A] Comm [A] DataLow [A] DataHigh [A] 
	 * 		S Addr Rd [A] [DataLow] A [DataHigh] NA P</pre>
	 * @param register the register to write to / read from
	 * @param data value to write
	 * @return data read
	 */
	short processCall(int register, short data);

	/**
	 * <p>SMBus Block Read:  i2c_smbus_read_block_data()</p>
	 * <p>This command reads a block of up to 32 bytes from a device, from a 
	 * designated register that is specified through the Comm byte. The amount
	 * of data is specified by the device in the Count byte.</p>
	 * <pre>S Addr Wr [A] Comm [A] 
	 * 		S Addr Rd [A] [Count] A [Data] A [Data] A ... A [Data] NA P</pre>
	 * @param register the register to read from
	 * @return data read
	 */
	byte[] readBlockData(int register);

	/**
	 * <p>SMBus Block Write:  i2c_smbus_write_block_data()</p>
	 * <p>The opposite of the Block Read command, this writes up to 32 bytes to 
	 * a device, to a designated register that is specified through the
	 * Comm byte. The amount of data is specified in the Count byte.</p>
	 * <pre>S Addr Wr [A] Comm [A] Count [A] Data [A] Data [A] ... [A] Data [A] P</pre>
	 * @param register the register to write to
	 * @param data values to write
	 */
	void writeBlockData(int register, byte[] data);

	/**
	 * <p>SMBus Block Write - Block Read Process Call</p>
	 * <p>SMBus Block Write - Block Read Process Call was introduced in
	 * Revision 2.0 of the specification.<br>
	 * This command selects a device register (through the Comm byte), sends
	 * 1 to 31 bytes of data to it, and reads 1 to 31 bytes of data in return.</p>
	 * <pre>S Addr Wr [A] Comm [A] Count [A] Data [A] ...
	 * 		S Addr Rd [A] [Count] A [Data] ... A P</pre>
	 * @param register the register to write to
	 * @param data data to write
	 * @param length Length
	 * @return data read
	 */
	byte[] blockProcessCall(int register, byte[] data, int length);

	/*
	 * I2C Block Transactions
	 * ======================
	 * The following I2C block transactions are supported by the SMBus layer and
	 * are described here for completeness. They are *NOT* defined by the SMBus
	 * specification. I2C block transactions do not limit the number of bytes
	 * transferred but the SMBus layer places a limit of 32 bytes.
	 */
	
	/**
	 * <p>I2C Block Read:  i2c_smbus_read_i2c_block_data()</p>
	 * <p>This command reads a block of bytes from a device, from a
	 * designated register that is specified through the Comm byte.</p>
	 * <pre>S Addr Wr [A] Comm [A]
	 *      S Addr Rd [A] [Data] A [Data] A ... A [Data] NA P</pre>
	 * @param register the register to read from
	 * @param length amount of data to read
	 * @return values to read
	 */
	byte[] readI2CBlockData(int register, int length);

	/**
	 * <p>I2C Block Write:  i2c_smbus_write_i2c_block_data()</p>
	 * <p>The opposite of the Block Read command, this writes bytes to
	 * a device, to a designated register that is specified through the
	 * Comm byte. Note that command lengths of 0, 2, or more bytes are
	 * supported as they are indistinguishable from data.</p>
	 * <pre>S Addr Wr [A] Comm [A] Data [A] Data [A] ... [A] Data [A] P</pre>
	 * @param register the register to write to
	 * @param data values to write
	 */
	void writeI2CBlockData(int register, byte[] data);
}
