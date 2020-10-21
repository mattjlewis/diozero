package com.diozero.internal.provider.sysfs;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     NativeDeviceTest.java  
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

import com.diozero.api.SerialConstants;
import com.diozero.api.SerialDevice;

public class NativeSerialDeviceTest {
	public static void main(String[] args) {
		SerialDevice.getLocalSerialDevices().forEach(device -> print(device));

		System.exit(1);

		try (NativeSerialDevice dev = new NativeSerialDevice(args[0], SerialConstants.DEFAULT_BAUD,
				SerialConstants.DEFAULT_DATA_BITS, SerialConstants.DEFAULT_STOP_BITS, SerialConstants.DEFAULT_PARITY,
				SerialConstants.DEFAULT_READ_BLOCKING, SerialConstants.DEFAULT_MIN_READ_CHARS,
				SerialConstants.DEFAULT_READ_TIMEOUT_MILLIS)) {
			dev.read();
		}
	}
	
	private static void print(SerialDevice.DeviceInfo deviceInfo) {
		System.out.format(
				"Name: %s, File: %s, Description: %s, Driver: %s, Manufacturer: %s, USB Vendor Id: %s, USB Product Id: %s%n",
				deviceInfo.getDeviceName(), deviceInfo.getDeviceFile(), deviceInfo.getDescription(), deviceInfo.getDriverName(),
				deviceInfo.getManufacturer(), deviceInfo.getUsbVendorId(), deviceInfo.getUsbProductId());
		if (deviceInfo.getUsbVendorId() != null) {
			String[] usb_info = SerialDevice.UsbInfo.resolve(deviceInfo.getUsbVendorId(), deviceInfo.getUsbProductId());
			System.out.format("USB device Vendor: %s; Product: %s%n", usb_info[0], usb_info[1]);
		}
	}
}
