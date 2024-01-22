package com.diozero.api;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     I2CSMBusInterface.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2024 diozero
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

/**
 * I2C device interface
 * <a href="https://www.kernel.org/doc/Documentation/i2c/smbus-protocol">Linux
 * SMBus interface</a>
 */
public interface I2CSMBusInterface extends DeviceInterface {
	int MAX_I2C_BLOCK_SIZE = 32;

	/**
	 * Probe this I2C device using {@link I2CDevice.ProbeMode#AUTO Auto} probe mode
	 *
	 * @return True if the probe is successful
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	default boolean probe() throws RuntimeIOException {
		return probe(I2CDevice.ProbeMode.AUTO);
	}

	/**
	 * Probe this I2C device to see if it is connected
	 *
	 * @param mode Probe mode
	 * @return True if the probe is successful and the device is connected
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	boolean probe(I2CDevice.ProbeMode mode) throws RuntimeIOException;

	/**
	 * <p>
	 * SMBus Quick Command
	 * </p>
	 * <p>
	 * This sends a single bit to the device, at the place of the Rd/Wr bit.
	 * </p>
	 *
	 * <pre>
	 * A Addr Rd/Wr [A] P
	 * </pre>
	 *
	 * @param bit The bit to write
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	void writeQuick(byte bit) throws RuntimeIOException;

	/**
	 * <p>
	 * SMBus Receive Byte: <code>i2c_smbus_read_byte()</code>
	 * </p>
	 * <p>
	 * This reads a single byte from a device, without specifying a device register.
	 * Some devices are so simple that this interface is enough; for others, it is a
	 * shorthand if you want to read the same register as in the previous SMBus
	 * command.
	 * </p>
	 *
	 * <pre>
	 * S Addr Rd [A] [Data] NA P
	 * </pre>
	 *
	 * @return The byte data read (note caller needs to handle conversion to
	 *         unsigned)
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	byte readByte() throws RuntimeIOException;

	/**
	 * <p>
	 * SMBus Send Byte: <code>i2c_smbus_write_byte()</code>
	 * </p>
	 * <p>
	 * This operation is the reverse of Receive Byte: it sends a single byte to a
	 * device. See Receive Byte for more information.
	 * </p>
	 *
	 * <pre>
	 * S Addr Wr [A] Data [A] P
	 * </pre>
	 *
	 * @param data value to write
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	void writeByte(byte data) throws RuntimeIOException;

	/**
	 * <p>
	 * SMBus Read Byte: <code>i2c_smbus_read_byte_data()</code>
	 * </p>
	 * <p>
	 * This reads a single byte from a device, from a designated register. The
	 * register is specified through the Comm byte.
	 * </p>
	 *
	 * <pre>
	 * S Addr Wr [A] Comm [A] S Addr Rd [A] [Data] NA P
	 * </pre>
	 *
	 * @param register the register to read from
	 * @return data read as byte (note caller needs to handle conversion to
	 *         unsigned)
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	byte readByteData(int register) throws RuntimeIOException;

	/**
	 * <p>
	 * SMBus Write Byte: <code>i2c_smbus_write_byte_data()</code>
	 * </p>
	 * <p>
	 * This writes a single byte to a device, to a designated register. The register
	 * is specified through the Comm byte. This is the opposite of the Read Byte
	 * operation.
	 * </p>
	 *
	 * <pre>
	 * S Addr Wr [A] Comm [A] Data [A] P
	 * </pre>
	 *
	 * @param register the register to write to
	 * @param data     value to write
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	void writeByteData(int register, byte data) throws RuntimeIOException;

	/**
	 * <p>
	 * SMBus Read Word: <code>i2c_smbus_read_word_data()</code>
	 * </p>
	 * <p>
	 * This operation is very like Read Byte; again, data is read from a device,
	 * from a designated register that is specified through the Comm byte. But this
	 * time, the data is a complete word (16 bits) in
	 * {@link java.nio.ByteOrder#LITTLE_ENDIAN Little Endian} order as per the SMBus
	 * specification.
	 * </p>
	 *
	 * <pre>
	 * S Addr Wr [A] Comm [A] S Addr Rd [A] [DataLow] A [DataHigh] NA P
	 * </pre>
	 *
	 * @param register the register to read from
	 * @return data read as a signed short in
	 *         {@link java.nio.ByteOrder#LITTLE_ENDIAN Little Endian} byte order
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	short readWordData(int register) throws RuntimeIOException;

	/**
	 * <p>
	 * SMBus Write Word: <code>i2c_smbus_write_word_data()</code>
	 * </p>
	 * <p>
	 * This is the opposite of the Read Word operation. 16 bits of data is written
	 * to a device, to the designated register that is specified through the Comm
	 * byte. Note that the data is written in
	 * {@link java.nio.ByteOrder#LITTLE_ENDIAN Little Endian} byte order as per the
	 * SMBus specification.
	 * </p>
	 *
	 * <pre>
	 * S Addr Wr [A] Comm [A] DataLow [A] DataHigh [A] P
	 * </pre>
	 *
	 * @param register the register to write to
	 * @param data     value to write in {@link java.nio.ByteOrder#LITTLE_ENDIAN
	 *                 Little Endian} byte order
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	void writeWordData(int register, short data) throws RuntimeIOException;

	/**
	 * <p>
	 * SMBus Read Word Swapped: <code>i2c_smbus_read_word_swapped()</code>
	 * </p>
	 * <p>
	 * This operation is very like Read Byte; again, data is read from a device,
	 * from a designated register that is specified through the Comm byte. But this
	 * time, the data is a complete word (16 bits). Note this is the convenience
	 * function for reads where the two data bytes are the other way around (not
	 * SMBus compliant, but very popular.)
	 * </p>
	 *
	 * <pre>
	 * S Addr Wr [A] Comm [A] S Addr Rd [A] [DataHigh] A [DataLow] NA P
	 * </pre>
	 *
	 * @param register the register to read from
	 * @return data read as a signed short in {@link java.nio.ByteOrder#BIG_ENDIAN
	 *         Big Endian} byte order
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	default short readWordSwapped(int register) throws RuntimeIOException {
		short value = readWordData(register);
		return (short) (((value & 0x00ff) << 8) | ((value & 0xff00) >> 8));
	}

	/**
	 * <p>
	 * SMBus Write Word Swapped: <code>i2c_smbus_write_word_swapped()</code>
	 * </p>
	 * <p>
	 * This is the opposite of the Read Word operation. 16 bits of data is written
	 * to a device, to the designated register that is specified through the Comm
	 * byte. Note that this is the convenience function for writes where the two
	 * data bytes are the other way around (not SMBus compliant, but very popular.)
	 * </p>
	 *
	 * <pre>
	 * S Addr Wr [A] Comm [A] DataHigh [A] DataLow [A] P
	 * </pre>
	 *
	 * @param register the register to write to
	 * @param data     value to write in {@link java.nio.ByteOrder#BIG_ENDIAN Big
	 *                 Endian} byte order
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	default void writeWordSwapped(int register, short data) throws RuntimeIOException {
		short swapped = (short) (((data & 0x00ff) << 8) | ((data & 0xff00) >> 8));
		writeWordData(register, swapped);
	}

	/**
	 * <p>
	 * SMBus Process Call
	 * </p>
	 * <p>
	 * This command selects a device register (through the Comm byte), sends 16 bits
	 * of data to it, and reads 16 bits of data in return.
	 * </p>
	 *
	 * <pre>
	 * S Addr Wr [A] Comm [A] DataLow [A] DataHigh [A]
	 * 		S Addr Rd [A] [DataLow] A [DataHigh] NA P
	 * </pre>
	 *
	 * @param register the register to write to / read from
	 * @param data     value to write
	 * @return the value read
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	short processCall(int register, short data) throws RuntimeIOException;

	/**
	 * <p>
	 * SMBus Block Read: <code>i2c_smbus_read_block_data()</code>
	 * </p>
	 * <p>
	 * This command reads a block of up to 32 bytes from a device, from a designated
	 * register that is specified through the Comm byte. The amount of data is
	 * specified by the device in the Count byte.
	 * </p>
	 *
	 * <pre>
	 * S Addr Wr [A] Comm [A]
	 * 		S Addr Rd [A] [Count] A [Data] A [Data] A ... A [Data] NA P
	 * </pre>
	 *
	 * @param register the register to read from
	 * @return the data read up to 32 bytes in length
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	byte[] readBlockData(int register) throws RuntimeIOException;

	/**
	 * <p>
	 * SMBus Block Write: <code>i2c_smbus_write_block_data()</code>
	 * </p>
	 * <p>
	 * The opposite of the Block Read command, this writes up to 32 bytes to a
	 * device, to a designated register that is specified through the Comm byte. The
	 * amount of data is specified in the Count byte.
	 * </p>
	 *
	 * <pre>
	 * S Addr Wr [A] Comm [A] Count [A] Data [A] Data [A] ... [A] Data [A] P
	 * </pre>
	 *
	 * @param register the register to write to
	 * @param data     the data to write (up to 32 bytes)
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	void writeBlockData(int register, byte... data) throws RuntimeIOException;

	/**
	 * <p>
	 * SMBus Block Write - Block Read Process Call
	 * </p>
	 * <p>
	 * SMBus Block Write - Block Read Process Call was introduced in Revision 2.0 of
	 * the specification.<br>
	 * This command selects a device register (through the Comm byte), sends 1 to 31
	 * bytes of data to it, and reads 1 to 31 bytes of data in return.
	 * </p>
	 *
	 * <pre>
	 * S Addr Wr [A] Comm [A] Count [A] Data [A] ...
	 * 		S Addr Rd [A] [Count] A [Data] ... A P
	 * </pre>
	 *
	 * @param register the register to write to and read from
	 * @param txData   the byte array from which the data is written (up to 32
	 *                 bytes)
	 * @return the data read (up to 32 bytes)
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	byte[] blockProcessCall(int register, byte... txData) throws RuntimeIOException;

	/*-
	 * I2C Block Transactions
	 * ======================
	 * The following I2C block transactions are supported by the SMBus layer and
	 * are described here for completeness. They are *NOT* defined by the SMBus
	 * specification. I2C block transactions do not limit the number of bytes
	 * transferred but the SMBus layer places a limit of 32 bytes.
	 */

	/**
	 * <p>
	 * I2C Block Read: <code>i2c_smbus_read_i2c_block_data()</code>
	 * </p>
	 * <p>
	 * This command reads a block of up to 32 bytes from a device, using the
	 * specified register address.
	 * </p>
	 *
	 * I2C commands:
	 *
	 * <pre>
	 * S Addr Wr [A] Comm [A]
	 *      S Addr Rd [A] [Data] A [Data] A ... A [Data] NA P
	 * </pre>
	 *
	 * @param register the register to read from
	 * @param buffer   the buffer to read the data into, the buffer length specifies
	 *                 the number of bytes to read up to a maximum of 32 bytes
	 * @return the number of bytes actually read
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	int readI2CBlockData(int register, byte[] buffer) throws RuntimeIOException;

	/**
	 * <p>
	 * I2C Block Write: <code>i2c_smbus_write_i2c_block_data()</code>
	 * </p>
	 * <p>
	 * The opposite of the Block Read command, this writes up to 32 bytes of data to
	 * a device, to the specified register address.
	 * </p>
	 *
	 *
	 * <pre>
	 * S Addr Wr [A] Comm [A] Data [A] Data [A] ... [A] Data [A] P
	 * </pre>
	 *
	 * <p>
	 * Note that command lengths of 0, 2, or more bytes are supported as they are
	 * indistinguishable from data.
	 * </p>
	 *
	 * @param register the register to write to
	 * @param data     values to write with a maximum length of 32 bytes
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	void writeI2CBlockData(int register, byte... data) throws RuntimeIOException;
}
