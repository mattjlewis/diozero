package com.diozero.api;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     SerialDevice.java  
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

import java.io.Closeable;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.tinylog.Logger;

import com.diozero.internal.provider.SerialDeviceInterface;
import com.diozero.util.DeviceFactoryHelper;

public class SerialDevice implements SerialConstants, Closeable {
	public static class DeviceInfo {
		private String deviceName;
		private String friendlyName;

		public DeviceInfo(String deviceName, String friendlyName) {
			this.deviceName = deviceName;
			this.friendlyName = friendlyName;
		}

		public String getDeviceName() {
			return deviceName;
		}

		public String getFriendlyName() {
			return friendlyName;
		}
	}

	public static List<DeviceInfo> getLocalSerialDevices() {
		/*-
		 * On Linux:
		 * > cd /sys/devices > find . -name \*tty\*
		 * ./platform/scb/fd500000.pcie/pci0000:00/0000:00:00.0/0000:01:00.0/usb1/1-1/1-1.2/1-1.2:1.0/ttyUSB0
		 * ./platform/scb/fd500000.pcie/pci0000:00/0000:00:00.0/0000:01:00.0/usb1/1-1/1-1.2/1-1.2:1.0/ttyUSB0/tty
		 * ./platform/scb/fd500000.pcie/pci0000:00/0000:00:00.0/0000:01:00.0/usb1/1-1/1-1.2/1-1.2:1.0/ttyUSB0/tty/ttyUSB0
		 * ./platform/soc/fe201000.serial/tty
		 * ./platform/soc/fe201000.serial/tty/ttyAMA0
		 * ./virtual/tty
		 * ./virtual/tty/tty58
		 * ...
		 * 
		 * > find . -name product
		 * ./platform/scb/fd500000.pcie/pci0000:00/0000:00:00.0/0000:01:00.0/usb1/product
		 * ./platform/scb/fd500000.pcie/pci0000:00/0000:00:00.0/0000:01:00.0/usb1/1-1/product
		 * ./platform/scb/fd500000.pcie/pci0000:00/0000:00:00.0/0000:01:00.0/usb1/1-1/1-1.2/product
		 * ./platform/scb/fd500000.pcie/pci0000:00/0000:00:00.0/0000:01:00.0/usb2/product
		 * 
		 * > cat ./platform/scb/fd500000.pcie/pci0000:00/0000:00:00.0/0000:01:00.0/usb1/1-1/1-1.2/product
		 * USB2.0-Serial
		 */
		List<DeviceInfo> serial_devices = new ArrayList<>();
		lookForSerialDevices(Paths.get("/sys/devices/platform"), serial_devices);

		return serial_devices;
	}

	private static void lookForSerialDevices(Path parent, List<DeviceInfo> serialDevices) {
		if (!parent.toFile().exists()) {
			return;
		}
		
		try {
			// Ignore hidden files, symbolic links and "virtual"
			Files.newDirectoryStream(parent, p -> p.toFile().isDirectory() && !p.toFile().isHidden()
					&& !Files.isSymbolicLink(p) && !p.getFileName().toString().equals("virtual")).forEach(p -> {
						String file_name = p.getFileName().toString();
						if (file_name.length() > 3 && (file_name.startsWith("tty") || file_name.startsWith("rfc"))) {
							Logger.debug("Found device with path {}", p);
							String friendly_name;

							// Look for a product description file
							Path product_file = p.getParent().getParent().resolve("product");
							if (product_file.toFile().exists()) {
								try {
									friendly_name = Files.lines(product_file).findFirst().orElse("Unknown");
								} catch (IOException e) {
									Logger.error(e, "Error reading serial device product file: {}", e.getMessage());
									friendly_name = "Error";
								}
							} else {
								// TODO Can also look for p.resolve("/driver/module/drivers") then read the interface file in
								// p.getParent().resolve("interface") or p.resolve("device").getParent().resolve("interface")
								friendly_name = "Physical Port";
							}
							serialDevices.add(new DeviceInfo("/dev/" + file_name, friendly_name));
						} else {
							lookForSerialDevices(p, serialDevices);
						}
					});
		} catch (IOException e) {
			Logger.error(e, "Error looking for serial devices: {}", e.getMessage());
		}
	}

	private SerialDeviceInterface device;

	public SerialDevice(String deviceName) {
		this(deviceName, DEFAULT_BAUD, DEFAULT_DATA_BITS, DEFAULT_STOP_BITS, DEFAULT_PARITY, DEFAULT_READ_BLOCKING,
				DEFAULT_MIN_READ_CHARS, DEFAULT_READ_TIMEOUT_MILLIS);
	}

	public SerialDevice(String deviceName, int baud, DataBits dataBits, StopBits stopBits, Parity parity) {
		this(deviceName, baud, dataBits, stopBits, parity, DEFAULT_READ_BLOCKING, DEFAULT_MIN_READ_CHARS,
				DEFAULT_READ_TIMEOUT_MILLIS);
	}

	/**
	 * Create a new serial device
	 *
	 * @param deviceName        The O/S file name for the device
	 * @param baud              Baud rate, see
	 *                          {@link com.diozero.api.SerialConstants
	 *                          SerialConstants} for valid baud rate values
	 * @param dataBits          Number of data bits as per the enum
	 *                          {@link com.diozero.api.SerialConstants.DataBits
	 *                          DataBits}
	 * @param stopBits          Number of stop bits as per the enum
	 *                          {@link com.diozero.api.SerialConstants.StopBits
	 *                          StopBits}
	 * @param parity            Parity as per the enum
	 *                          {@link com.diozero.api.SerialConstants.Parity
	 *                          Parity}
	 * @param readBlocking      Should all read operations block until data is
	 *                          available?
	 * @param minReadChars      Minimum number of characters to read (note actually
	 *                          an unsigned char hence max value is 255)
	 * @param readTimeoutMillis The read timeout value in milliseconds (note
	 *                          converted to tenths of a second as an unsigned char)
	 */
	public SerialDevice(String deviceName, int baud, DataBits dataBits, StopBits stopBits, Parity parity,
			boolean readBlocking, int minReadChars, int readTimeoutMillis) {
		device = DeviceFactoryHelper.getNativeDeviceFactory().provisionSerialDevice(deviceName, baud, dataBits,
				stopBits, parity, readBlocking, minReadChars, readTimeoutMillis);
	}

	@Override
	public void close() throws IOException {
		Logger.trace("close()");
		device.close();
	}

	public int read() {
		return device.read();
	}

	public byte readByte() {
		return device.readByte();
	}

	public void readByte(byte bVal) {
		device.writeByte(bVal);
	}

	public void read(byte[] buffer) {
		device.read(buffer);
	}

	public void write(byte[] buffer) {
		device.write(buffer);
	}

	public int bytesAvailable() {
		return device.bytesAvailable();
	}
}
