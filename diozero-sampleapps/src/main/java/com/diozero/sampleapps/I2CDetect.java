package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Sample applications
 * Filename:     I2CDetect.java  
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

import org.tinylog.Logger;

import com.diozero.api.DeviceBusyException;
import com.diozero.api.I2CDevice;

public class I2CDetect {
	public static void main(String[] args) {
		if (args.length < 1) {
			Logger.error("Usage: {} <i2c-controller> [first last]", I2CDetect.class.getName());
			System.exit(1);
		}
		
		int controller = Integer.parseInt(args[0]);
		I2CDevice.ProbeMode mode = I2CDevice.ProbeMode.AUTO;
		int first = 0x03;
		int last = 0x77;
		if (args.length > 1) {
			first = Math.max(Integer.parseInt(args[1]), 0);
		}
		if (args.length > 2) {
			last = Math.min(Integer.parseInt(args[2]), 127);
		}
		
		scanI2CBus(controller, mode, first, last);
	}
	
	private static void scanI2CBus(int controller, I2CDevice.ProbeMode mode, int first, int last) {
		System.out.println("     0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f");
		for (int device_address = 0; device_address < 128; device_address++) {
			if ((device_address % 16) == 0) {
				System.out.print(String.format("%02x: ", Integer.valueOf(device_address)));
			}
			if (device_address < first || device_address > last) {
				System.out.print("   ");
			} else {
				try (I2CDevice device = new I2CDevice(controller, device_address)) {
					if (device.probe(mode)) {
						System.out.print(String.format("%02x ", Integer.valueOf(device_address)));
					} else {
						System.out.print("-- ");
					}
				} catch (DeviceBusyException e) {
					System.out.print("UU ");
				}
			}
			if ((device_address % 16) == 15) {
				System.out.println();
			}
		}
	}
}
