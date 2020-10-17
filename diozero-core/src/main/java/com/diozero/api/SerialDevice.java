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
import java.util.Iterator;
import java.util.List;

import org.tinylog.Logger;

import com.diozero.internal.provider.SerialDeviceInterface;
import com.diozero.util.DeviceFactoryHelper;

public class SerialDevice implements SerialConstants, Closeable {
	public static class DeviceInfo {
		private String deviceName;
		private String deviceFile;
		private String description;
		private String driverName;
		private String usbVendorId;
		// private String usbVendorName;
		private String usbProductId;
		// private String usbProductName;

		public DeviceInfo(String deviceName, String deviceFile, String description, String driverName,
				String usbVendorId, String usbProductId) {
			this.deviceName = deviceName;
			this.deviceFile = deviceFile;
			this.description = description;
			this.driverName = driverName;
			this.usbVendorId = usbVendorId;
			this.usbProductId = usbProductId;
		}

		public String getDeviceFile() {
			return deviceFile;
		}

		public String getDeviceName() {
			return deviceName;
		}

		public String getDescription() {
			return description;
		}

		public String getDriverName() {
			return driverName;
		}

		public String getUsbVendorId() {
			return usbVendorId;
		}

		public String getUsbProductId() {
			return usbProductId;
		}
	}

	public static List<DeviceInfo> getLocalSerialDevices() {
		/*-
		 * On Linux:
		 * > cd /sys/devices
		 * > find . -name \*tty\*
		 * ./platform/scb/fd500000.pcie/pci0000:00/0000:00:00.0/0000:01:00.0/usb1/1-1/1-1.2/1-1.2:1.0/ttyUSB0
		 * ./platform/scb/fd500000.pcie/pci0000:00/0000:00:00.0/0000:01:00.0/usb1/1-1/1-1.2/1-1.2:1.0/ttyUSB0/tty
		 * ./platform/scb/fd500000.pcie/pci0000:00/0000:00:00.0/0000:01:00.0/usb1/1-1/1-1.2/1-1.2:1.0/ttyUSB0/tty/ttyUSB0
		 * ./platform/soc/fe201000.serial/tty
		 * ./platform/soc/fe201000.serial/tty/ttyAMA0
		 * ./virtual/tty
		 * ./virtual/tty/tty58
		 * ...
		 * 
		 * > ls -l /sys/devices/platform/soc/fe201000.serial/driver
		 * /sys/devices/platform/soc/fe201000.serial/driver -> ../../../../bus/amba/drivers/uart-pl011
		 * 
		 * > find . -name product
		 * ./platform/scb/fd500000.pcie/pci0000:00/0000:00:00.0/0000:01:00.0/usb1/product
		 * ./platform/scb/fd500000.pcie/pci0000:00/0000:00:00.0/0000:01:00.0/usb1/1-1/product
		 * ./platform/scb/fd500000.pcie/pci0000:00/0000:00:00.0/0000:01:00.0/usb1/1-1/1-1.2/product
		 * ./platform/scb/fd500000.pcie/pci0000:00/0000:00:00.0/0000:01:00.0/usb2/product
		 * 
		 * For USB connected devices, the main files are under /sys/devices/platform/scb/<xxx.pcie>/.../usbx
		 * Search for a directory that starts with "tty" and is more than 3 characters in length
		 * Standard is 7 characters I think - ttyXXXN where XXX is something like USB|ACM|AMA and N is the instance number
		 * E.g. usb1/1-1/1-1.2/1-1.2:1.0/ttyUSB0
		 * Then navigate up two levels, e.g. usb1/1-1/1-1.2
		 * Important files in this directory:
		 * product (e.g. "USB2.0-Serial")
		 * idVendor (e.g. "1a86")
		 * idProduct (e.g. "7523")
		 * 
		 * Look up idVendor and idProduct in the USB id database /var/lib/usbutils/usb.ids
		 * vendorId "1a86" == "QinHeng Electronics"
		 * For this vendorId, productId "7523" == "CH340 serial converter"
		 * 
		 * Also:
		 *  /dev/ttyACM0 (Abstract Control Module), e.g.:
		 *   Pololu_A-Star_32U4 : Pololu_Corporation : Pololu_Corporation_Pololu_A-Star_32U4
		 *   USB_Roboclaw_2x15A : 03eb : 03eb_USB_Roboclaw_2x15A
		 *  /dev/ttyS0 - 9-pin serial connector
		 */
		List<DeviceInfo> serial_devices = new ArrayList<>();
		Path device_root = Paths.get("/sys/devices/platform");
		if (device_root.toFile().exists()) {
			lookForSerialDevices(device_root, serial_devices);
		}

		return serial_devices;
	}

	private static void lookForSerialDevices(Path path, List<DeviceInfo> serialDevices) {
		try {
			// Ignore hidden files, symbolic links and "virtual"
			Files.list(path).filter(p -> p.toFile().isDirectory()).filter(p -> !p.toFile().isHidden())
					.filter(p -> !Files.isSymbolicLink(p)).filter(p -> !p.getFileName().toString().equals("virtual"))
					.forEach(p -> {
						if (isSerialDevice(p.getFileName().toString())) {
							try {
								serialDevices.add(getDeviceInfo(p));
							} catch (IOException e) {

							}
						} else {
							lookForSerialDevices(p, serialDevices);
						}
					});
		} catch (IOException e) {
			Logger.error(e, "Error looking for serial devices: {}", e.getMessage());
		}
	}

	private static boolean isSerialDevice(String fileName) {
		return fileName != null && fileName.length() > 3 && (fileName.startsWith("tty") || fileName.startsWith("rfc"));
	}

	private static DeviceInfo getDeviceInfo(Path p) throws IOException {
		String device_name = p.getFileName().toString();
		Logger.debug("Found device with path {}", p);

		String description, driver_name, usb_vendor_id, usb_product_id;

		// Look for a product description file
		Path product_file = p.getParent().getParent().resolve("product");
		boolean primary_method = true;
		if (!product_file.toFile().exists()) {
			product_file = p.resolve("device").getParent().resolve("product");
			if (product_file.toFile().exists()) {
				primary_method = false;
			}
		}
		if (product_file.toFile().exists()) {
			// I believe this means that this is a USB-connected device
			Path usb_root = primary_method ? p.getParent().getParent() : p.resolve("device").getParent();
			description = Files.lines(product_file).findFirst().orElse(null);
			// Get the driver name, e.g. "usb-serial:ch341-uart"
			driver_name = Files.list(p.resolve("driver").resolve("module").resolve("drivers"))
					.filter(path -> path.toFile().isDirectory()).filter(path -> !path.toFile().isHidden())
					.map(path -> path.getFileName().toString()).findFirst().orElse(null);
			// Look for USB vendor and product identifiers
			usb_vendor_id = Files.lines(usb_root.resolve("idVendor")).findFirst().orElse(null);
			usb_product_id = Files.lines(usb_root.resolve("idProduct")).findFirst().orElse(null);
		} else {
			// Must be a physical (or emulated) port
			description = "Physical Port";
			Path driver_path = p.resolve("device").resolve("driver");
			if (driver_path.toFile().exists()) {
				driver_name = Paths.get(driver_path.toFile().getCanonicalPath()).getFileName().toString();
			} else {
				driver_name = "Unknown";
			}
			usb_vendor_id = null;
			usb_product_id = null;
		}

		return new DeviceInfo(device_name, "/dev/" + device_name, description, driver_name, usb_vendor_id, usb_product_id);
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

	public static class UsbInfo {
		public static String[] resolve(String vendorId, String productId) {
			String vendor_name = null;
			String product_name = null;
			
			try {
				Iterator<String> it = Files.lines(Paths.get("/var/lib/usbutils/usb.ids"))
						.filter(line -> !line.startsWith("#")).filter(line -> !line.trim().isEmpty()).iterator();
				while (it.hasNext()) {
					String line = it.next();
					if (line.startsWith(vendorId)) {
						vendor_name = line.substring(vendorId.length()).trim();
						// Now search for the product name
						do {
							line = it.next();
							if (!line.startsWith("\t")) {
								break;
							}
							if (line.trim().startsWith(productId)) {
								product_name = line.trim().substring(productId.length()).trim();
								break;
							}
						} while (true);
						break;
					}
				}
			} catch (IOException e) {
				Logger.error(e);
			}
			
			return new String[] { vendor_name, product_name };
		}
	}
}
