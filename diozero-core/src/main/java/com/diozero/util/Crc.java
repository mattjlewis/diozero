package com.diozero.util;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     Crc.java
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

/**
 * CRC-8 and CRC-16 calculator.
 * 
 * See:
 * <ul>
 * <li>https://en.wikipedia.org/wiki/Cyclic_redundancy_check</li>
 * <li>https://crccalc.com</li>
 * <li>http://www.zlib.net/crc_v3.txt</li>
 * <li>https://github.com/sigurn/crc8/blob/master/crc8.go</li>
 * <li>https://github.com/sigurn/crc16/blob/master/crc16.go</li>
 * <li>https://www.lammertbies.nl/comm/info/crc-calculation</li>
 * </ul>
 */
public class Crc {
	public static class Params {
		private short polynomial;
		private short init;
		private boolean reverseIn;
		private boolean reverseOut;
		private short xorOut;
		private int[] data;

		public Params(int polynomial, int init, boolean reverseIn, boolean reverseOut, int xorOut) {
			this.polynomial = (short) polynomial;
			this.init = (short) init;
			this.reverseIn = reverseIn;
			this.reverseOut = reverseOut;
			this.xorOut = (short) xorOut;
		}

		public short getPolynomial() {
			return polynomial;
		}

		public short getInit() {
			return init;
		}

		public boolean isReverseIn() {
			return reverseIn;
		}

		public boolean isReverseOut() {
			return reverseOut;
		}

		public short getXorOut() {
			return xorOut;
		}

		private void makeTable() {
			data = new int[256];
			int crc;
			for (int n = 0; n < 256; n++) {
				crc = n << 8;
				for (int i = 0; i < 8; i++) {
					boolean bit = (crc & 0x8000) != 0;
					crc <<= 1;
					if (bit) {
						crc ^= polynomial;
					}
				}
				data[n] = crc & 0xffff;
			}
		}

		public int[] getData() {
			if (data == null) {
				makeTable();
			}
			return data;
		}
	}

	public static final Params CRC8 = new Params(0x07, 0x00, false, false, 0x00);
	public static final Params CRC8_CDMA2000 = new Params(0x9b, 0xff, false, false, 0x00);
	public static final Params CRC8_DARC = new Params(0x39, 0x00, true, true, 0x00);
	public static final Params CRC8_DVBS2 = new Params(0xd5, 0x00, false, false, 0x00);
	public static final Params CRC8_EBU = new Params(0x1d, 0xff, true, true, 0x00);
	public static final Params CRC8_ICODE = new Params(0x1d, 0xfd, false, false, 0x00);
	public static final Params CRC8_ITU = new Params(0x07, 0x00, false, false, 0x55);
	public static final Params CRC8_MAXIM = new Params(0x31, 0x00, true, true, 0x00);
	public static final Params CRC8_ROHC = new Params(0x07, 0xff, true, true, 0x00);
	public static final Params CRC8_WCDMA = new Params(0x9b, 0x00, true, true, 0x00);

	/**
	 * Calculate CRC-8 checksum using the default {@link Crc#CRC8 CRC8} parameters
	 * 
	 * @param data data to checksum (big endian)
	 * @return CRC-8 checksum
	 */
	public static int crc8(short data) {
		// Assumes Big Endian
		return crc8(CRC8, (byte) (data >> 8), (byte) (data & 0xff));
	}

	/**
	 * Calculate CRC-8 checksum using the specified {@link Params parameters}
	 * 
	 * @param params CRC {@link Params parameters}
	 * @param data   data to checksum (big endian)
	 * @return CRC-8 checksum
	 */
	public static int crc8(Params params, short data) {
		// Assumes Big Endian
		return crc8(params, (byte) (data >> 8), (byte) (data & 0xff));
	}

	/**
	 * Calculate CRC-8 checksum using the default {@link Crc#CRC8 CRC8} parameters
	 * 
	 * @param data data to checksum
	 * @return CRC-8 checksum
	 */
	public static int crc8(byte... data) {
		return crc8(CRC8, data);
	}

	/**
	 * Calculate CRC-8 checksum using the specified {@link Params parameters}
	 * 
	 * @param params CRC {@link Params parameters}
	 * @param data   data to checksum
	 * @return CRC-8 checksum
	 */
	public static int crc8(Params params, byte... data) {
		byte crc = (byte) params.getInit();

		// Calculate the 8-Bit checksum for the given polynomial
		for (byte b : data) {
			if (params.isReverseIn()) {
				b = BitManipulation.reverseByte(b);
			}
			crc ^= (b & 0xff);
			for (int crc_bit = 8; crc_bit > 0; --crc_bit) {
				boolean bit = (crc & 0x80) != 0;
				crc <<= 1;
				if (bit) {
					crc = (byte) (crc ^ params.getPolynomial());
				}
			}
		}

		if (params.isReverseOut()) {
			crc = BitManipulation.reverseByte(crc);
		}

		return (crc ^ params.getXorOut()) & 0xff;
	}

	public static final Params CRC16_CCITT_FALSE = new Params(0x1021, 0xFFFF, false, false, 0x0000);
	public static final Params CRC16_ARC = new Params(0x8005, 0x0000, true, true, 0x0000);
	public static final Params CRC16_AUG_CCITT = new Params(0x1021, 0x1D0F, false, false, 0x0000);
	public static final Params CRC16_BUYPASS = new Params(0x8005, 0x0000, false, false, 0x0000);
	public static final Params CRC16_CDMA2000 = new Params(0xC867, 0xFFFF, false, false, 0x0000);
	public static final Params CRC16_DDS_110 = new Params(0x8005, 0x800D, false, false, 0x0000);
	public static final Params CRC16_DECT_R = new Params(0x0589, 0x0000, false, false, 0x0001);
	public static final Params CRC16_DECT_X = new Params(0x0589, 0x0000, false, false, 0x0000);
	public static final Params CRC16_DNP = new Params(0x3D65, 0x0000, true, true, 0xFFFF);
	public static final Params CRC16_EN_13757 = new Params(0x3D65, 0x0000, false, false, 0xFFFF);
	public static final Params CRC16_GENIBUS = new Params(0x1021, 0xFFFF, false, false, 0xFFFF);
	public static final Params CRC16_MAXIM = new Params(0x8005, 0x0000, true, true, 0xFFFF);
	public static final Params CRC16_MCRF4XX = new Params(0x1021, 0xFFFF, true, true, 0x0000);
	public static final Params CRC16_RIELLO = new Params(0x1021, 0xB2AA, true, true, 0x0000);
	public static final Params CRC16_T10_DIF = new Params(0x8BB7, 0x0000, false, false, 0x0000);
	public static final Params CRC16_TELEDISK = new Params(0xA097, 0x0000, false, false, 0x0000);
	public static final Params CRC16_TMS37157 = new Params(0x1021, 0x89EC, true, true, 0x0000);
	public static final Params CRC16_USB = new Params(0x8005, 0xFFFF, true, true, 0xFFFF);
	public static final Params CRC16_A = new Params(0x1021, 0xC6C6, true, true, 0x0000);
	public static final Params CRC16_KERMIT = new Params(0x1021, 0x0000, true, true, 0x0000);
	public static final Params CRC16_MODBUS = new Params(0x8005, 0xFFFF, true, true, 0x0000);
	public static final Params CRC16_X25 = new Params(0x1021, 0xFFFF, true, true, 0xFFFF);
	public static final Params CRC16_XMODEM = new Params(0x1021, 0x0000, false, false, 0x0000);

	/**
	 * Calculate CRC-16 using the specified {@link Params parameters}
	 * 
	 * @param params CRC {@link Params parameters}
	 * @param data   The data to generate the CRC for (big endian)
	 * @return the calculated CRC-16 value
	 */
	public static int crc16Short(Params params, short data) {
		return crc16(params, (byte) (data >> 8), (byte) data);
	}

	public static int crc16(Params params, byte... data) {
		int[] table = params.getData();
		int crc = params.getInit();

		for (byte b : data) {
			if (params.isReverseIn()) {
				b = BitManipulation.reverseByte(b);
			}

			crc = (crc << 8) ^ table[(crc >> 8 ^ b) & 0xff];
		}

		if (params.isReverseOut()) {
			crc = BitManipulation.reverseShort((short) crc);
		}

		return (crc ^ params.getXorOut()) & 0xffff;
	}
}
