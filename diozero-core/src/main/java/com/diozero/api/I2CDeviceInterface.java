package com.diozero.api;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     I2CDeviceInterface.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
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

/**
 * <a href="https://en.wikipedia.org/wiki/I2C">Inter-Integrated Circuit (I2C) Interface</a>
 */
public interface I2CDeviceInterface extends I2CSMBusInterface {
	//
	default int readNoStop(byte registerAddress, int rxLength, byte[] rxData, boolean repeatedStart) {
		throw new UnsupportedOperationException("I2C readNoStop not supported");
	}
	
	default void readWrite(I2CMessage[] messages, byte[] buffer) {
		throw new UnsupportedOperationException("I2C readWrite not supported");
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
		
		public int flags;
		public int len;
		
		public I2CMessage(int flags, int len) {
			this.flags = flags;
			this.len = len;
		}
	}
}
