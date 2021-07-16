package com.diozero.api;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     I2CDeviceInterface.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2021 diozero
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Methods for interacting with I2C devices that do not use the SMBus interface.
 *
 * <a href="https://en.wikipedia.org/wiki/I2C">Inter-Integrated Circuit (I2C)
 * Interface</a>
 */
public interface I2CDeviceInterface extends I2CSMBusInterface {
	/**
	 * Read the specified number of bytes from the device without the 32 byte limit
	 * imposed by SMBus.
	 *
	 * I2C commands:
	 *
	 * <pre>
	 * S Addr Rd [A] [Data] [A] [Data] [A] ... [A] [Data] NA P
	 * </pre>
	 *
	 * @param buffer byte array to populate, the length of the byte array indicates
	 *               the number of bytes to read
	 * @return the number of bytes read
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	int readBytes(byte[] buffer) throws RuntimeIOException;

	/**
	 * Write the specified byte array to the device without the 32 byte limit
	 * imposed by SMBus.
	 *
	 * I2C commands:
	 *
	 * <pre>
	 * S Addr Wr [A] [Data] [A] [Data] [A] ... [A] [Data] NA P
	 * </pre>
	 *
	 * @param data the data to write
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	void writeBytes(byte... data) throws RuntimeIOException;

	/**
	 * Utility method to simplify the {@link #readWrite(I2CMessage[], byte[])}
	 * method at the cost of a bit of performance.
	 *
	 * @param commands list of logical commands to perform; note that for read
	 *                 commands the data read from the device will be copied into
	 *                 the command's data buffer
	 */
	default void readWrite(Command... commands) {
		readWrite(0, commands);
	}

	/**
	 * <p>
	 * Low-level I2C interface to execute a series of commands in a single I2C
	 * transaction. Allows multiple read and write commands to be executed at the
	 * same time as well as control over the I2C flags that are sent with each
	 * command, e.g. NO-START.
	 * </p>
	 *
	 * <p>
	 * The data buffer MUST align with the commands that are being issued. For
	 * example, to read 2 bytes from register 0x40 and then write 3 bytes to
	 * register 0x50, the message array and buffer would be as follows:
	 * </p>
	 *
	 * <pre>
	 * Message array and corresponding data buffer:
	 * Idx  Flags     Len  Buffer
	 * [0]  I2C_M_WR  1    [0] 0x40 - the register address to read from
	 * [1]  I2C_M_RD  2    [1..2] leave blank, will be overridden with the data read from the device
	 * [2]  I2C_M_WR  1    [3] 0x50 - the register address to write to
	 * [3]  I2C_M_WR  3    [4..6] the 3 bytes of data to write
	 * </pre>
	 *
	 * @param messages array of commands to send to the device
	 * @param buffer   the data buffer that is associated with the commands
	 */
	void readWrite(I2CMessage[] messages, byte[] buffer);

	/**
	 * Utility method to simplify the {@link #readWrite(I2CMessage[], byte[])}
	 * method at the cost of a bit of performance.
	 *
	 * @param registerWriteFlags flags to apply to all register address writes
	 * @param commands           list of logical commands to perform; note that for
	 *                           read commands the data read from the device will be
	 *                           copied into the command's data buffer
	 */
	default void readWrite(int registerWriteFlags, Command... commands) {
		List<I2CMessage> messages = new ArrayList<>();
		byte[] buffer;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			for (Command command : commands) {
				// Write the address byte if present
				command.getRegisterAddress().ifPresent(register_address -> {
					// Note there is no way to specify flags for individual register address writes
					messages.add(new I2CMessage(I2CMessage.I2C_M_WR | registerWriteFlags, 1));
					baos.write(register_address.intValue());
				});
				messages.add(new I2CMessage(command.getFlags(), command.getLength()));
				if (command instanceof WriteCommand) {
					baos.write(((WriteCommand) command).getData());
				} else {
					// Read command so make space in the buffer for the data being read
					for (int i = 0; i < command.getLength(); i++) {
						baos.write(0);
					}
				}
			}

			buffer = baos.toByteArray();
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}

		readWrite(messages.toArray(new I2CMessage[messages.size()]), buffer);

		// Copy the relevant read portions of the data buffer in the the read commands
		final AtomicInteger buffer_pos = new AtomicInteger();
		for (Command command : commands) {
			// Increment the buffer position if there was a register address present
			command.getRegisterAddress().ifPresent(address -> buffer_pos.incrementAndGet());
			if (command.isRead()) {
				System.arraycopy(buffer, buffer_pos.getAndAdd(command.getLength()), command.getData(), 0,
						command.getLength());
			}
		}
	}

	/**
	 * Utility I2C read method that allows control over the NO-START flag.
	 *
	 * @param registerAddress the register address to read from
	 * @param rxData          buffer to hold the data read
	 * @param repeatedStart   whether or not to use repeated starts
	 * @return the number of bytes read
	 */
	default int readNoStop(byte registerAddress, byte[] rxData, boolean repeatedStart) {
		I2CMessage[] messages = new I2CMessage[2];
		byte[] buffer = new byte[rxData.length + 1];
		messages[0] = new I2CMessage(I2CMessage.I2C_M_WR, 1);
		buffer[0] = registerAddress;
		messages[1] = new I2CMessage(
				I2CMessage.I2C_M_RD | I2CMessage.I2C_M_NO_RD_ACK | (repeatedStart ? 0 : I2CMessage.I2C_M_NOSTART),
				rxData.length);

		readWrite(messages, buffer);

		System.arraycopy(buffer, 1, rxData, 0, rxData.length);

		return rxData.length;
	}

	public static class I2CMessage {
		// https://www.kernel.org/doc/Documentation/i2c/i2c-protocol
		// Write data, from master to slave
		public static final int I2C_M_WR = 0;
		// Read data, from slave to master
		public static final int I2C_M_RD = 0x0001;
		// In a read message, master A/NA bit is skipped.
		public static final int I2C_M_NO_RD_ACK = 0x0800;
		/*- Normally message is interrupted immediately if there is [NA] from the
		 * client. Setting this flag treats any [NA] as [A], and all of
		 * message is sent. */
		public static final int I2C_M_IGNORE_NAK = 0x1000;
		/*- In a combined transaction, no 'S Addr Wr/Rd [A]' is generated at some
		 * point. For example, setting I2C_M_NOSTART on the second partial message
		 * generates something like:
		 * S Addr Rd [A] [Data] NA Data [A] P
		 * rather than:
		 * S Addr Rd [A] [Data] NA S Addr Wr [A] Data [A] P
		 */
		public static final int I2C_M_NOSTART = 0x4000;

		private int flags;
		private int len;

		public I2CMessage(int flags, int len) {
			this.flags = flags;
			this.len = len;
		}

		public int getFlags() {
			return flags;
		}

		public int getLength() {
			return len;
		}

		public boolean isRead() {
			return (flags & I2C_M_RD) == I2C_M_RD;
		}

		public boolean isWrite() {
			return !isRead();
		}
	}

	public static abstract class Command {
		private Optional<Integer> registerAddress;
		private int flags;
		private byte[] data;

		public Command(Optional<Integer> registerAddress, int flags, byte[] data) {
			this.registerAddress = registerAddress;
			this.flags = flags;
			this.data = data;
		}

		public Optional<Integer> getRegisterAddress() {
			return registerAddress;
		}

		public int getFlags() {
			return flags;
		}

		public byte[] getData() {
			return data;
		}

		public int getLength() {
			return data.length;
		}

		public boolean isRead() {
			return (flags & I2CMessage.I2C_M_RD) == I2CMessage.I2C_M_RD;
		}

		public boolean isWrite() {
			return !isRead();
		}
	}

	public static class ReadCommand extends Command {
		public ReadCommand(int additionalFlags, int length) {
			super(Optional.empty(), I2CMessage.I2C_M_RD | additionalFlags, new byte[length]);
		}

		public ReadCommand(int additionalFlags, byte[] buffer) {
			super(Optional.empty(), I2CMessage.I2C_M_RD | additionalFlags, buffer);
		}

		public ReadCommand(int additionalFlags, int registerAddress, int length) {
			super(Optional.of(Integer.valueOf(registerAddress)), I2CMessage.I2C_M_RD | additionalFlags,
					new byte[length]);
		}

		public ReadCommand(int additionalFlags, int registerAddress, byte[] buffer) {
			super(Optional.of(Integer.valueOf(registerAddress)), I2CMessage.I2C_M_RD | additionalFlags, buffer);
		}
	}

	public static class WriteCommand extends Command {
		public WriteCommand(int additionalFlags, byte... data) {
			super(Optional.empty(), I2CMessage.I2C_M_WR | additionalFlags, data);
		}

		public WriteCommand(int additionalFlags, int registerAddress, byte... data) {
			super(Optional.of(Integer.valueOf(registerAddress)), I2CMessage.I2C_M_WR | additionalFlags, data);
		}
	}

	int I2C_FUNC_I2C = 0x00000001;
	int I2C_FUNC_10BIT_ADDR = 0x00000002;
	int I2C_FUNC_PROTOCOL_MANGLING = 0x00000004; /* I2C_M_IGNORE_NAK etc. */
	int I2C_FUNC_SMBUS_PEC = 0x00000008;
	int I2C_FUNC_NOSTART = 0x00000010; /* I2C_M_NOSTART */
	int I2C_FUNC_SMBUS_BLOCK_PROC_CALL = 0x00008000; /* SMBus 2.0 */
	int I2C_FUNC_SMBUS_QUICK = 0x00010000;
	int I2C_FUNC_SMBUS_READ_BYTE = 0x00020000;
	int I2C_FUNC_SMBUS_WRITE_BYTE = 0x00040000;
	int I2C_FUNC_SMBUS_READ_BYTE_DATA = 0x00080000;
	int I2C_FUNC_SMBUS_WRITE_BYTE_DATA = 0x00100000;
	int I2C_FUNC_SMBUS_READ_WORD_DATA = 0x00200000;
	int I2C_FUNC_SMBUS_WRITE_WORD_DATA = 0x00400000;
	int I2C_FUNC_SMBUS_PROC_CALL = 0x00800000;
	int I2C_FUNC_SMBUS_READ_BLOCK_DATA = 0x01000000;
	int I2C_FUNC_SMBUS_WRITE_BLOCK_DATA = 0x02000000;
	int I2C_FUNC_SMBUS_READ_I2C_BLOCK = 0x04000000; /* I2C-like block xfer */
	int I2C_FUNC_SMBUS_WRITE_I2C_BLOCK = 0x08000000; /* w/ 1-byte reg. addr. */
}
