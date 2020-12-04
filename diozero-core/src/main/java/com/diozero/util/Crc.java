package com.diozero.util;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     Crc.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
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

import java.nio.charset.StandardCharsets;

/**
 * See https://www.lammertbies.nl/comm/info/crc-calculation
 */
public class Crc {
	public static void main(String[] args) {
		int polynomial = 0x1021;
		int init = 0xffff;
		
		byte[] data = "123456789".getBytes(StandardCharsets.US_ASCII);
		int calc_crc = crcCcitt(polynomial, init, data);
		System.out.println("0x" + Integer.toHexString(calc_crc));
		int expected_crc = 0x29b1;
		if (expected_crc != calc_crc) {
			System.out.println("CRC error");
		}
		
		data = "Lammert".getBytes(StandardCharsets.US_ASCII);
		calc_crc = crcCcitt(polynomial, init, data);
		System.out.println("0x" + Integer.toHexString(calc_crc));
		expected_crc = 0x1CB0;
		if (expected_crc != calc_crc) {
			System.out.println("CRC error");
		}
		
		data = new byte[] { (byte) 0xfc, 0x05, 0x11 };
		calc_crc = crcCcitt(polynomial, init, data);
		System.out.println("0x" + Integer.toHexString(calc_crc));
		expected_crc = 0xA74A;
		if (expected_crc != calc_crc) {
			System.out.println("CRC error");
		}
		
		data = "9142656".getBytes(StandardCharsets.US_ASCII);
		calc_crc = crcCcitt(polynomial, init, data);
		System.out.println("0x" + Integer.toHexString(calc_crc));
		expected_crc = 0x763A;
		if (expected_crc != calc_crc) {
			System.out.println("CRC error");
		}
	}
	
	public static int generateCrc8(int polynomial, int init, short data) {
		// Assumes Big Endian
		return generateCrc8(polynomial, 0xff, (byte) (data >> 8), (byte) (data & 0xff));
	}

	public static int generateCrc8(int polynomial, int init, byte... data) {
		int crc = init;

		/* calculates 8-Bit checksum with given polynomial */
		for (int i = 0; i < data.length; ++i) {
			crc ^= ((data[i]) & 0xff);
			for (int crc_bit = 8; crc_bit > 0; --crc_bit) {
				if ((crc & 0x80) != 0) {
					crc = (crc << 1) ^ polynomial;
				} else {
					// crc = (crc << 1);
					crc <<= 1;
				}
			}
		}

		return crc & 0xff;
	}

	public static int crc16Ccitt(int polynomial, int init, short data) {
		return crcCcitt(polynomial, (byte) ((data & 0xff00) >> 8), (byte) (data & 0xff));
	}
	
	public static int crcCcitt(int polynomial, int init, byte... data) {
		int crc = init;
		
		for (byte b : data) {
            for (int i = 0; i < 8; i++) {
                boolean bit = ((b   >> (7-i) & 1) == 1);
                boolean c15 = ((crc >> 15    & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit) crc ^= polynomial;
            }
        }

        return crc & 0xffff;
	}
}
