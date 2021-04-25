package com.diozero.api;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     SerialDevice.java
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.tinylog.Logger;

import com.diozero.internal.spi.InternalSerialDeviceInterface;
import com.diozero.sbc.DeviceFactoryHelper;

/**
 * Serial device. The SerialDevice represents serial devices connected via USB
 * or via the serial RX/TX pins on the GPIO header.
 * <p>
 * On the Raspberry Pi, to use the serial RX/TX pins on the GPIO header, the
 * serial interface must be enabled and the login shell must be disabled. The
 * device file name for the serial RX/TX pins is /dev/serial0. See
 * <a href="https://www.raspberrypi.org/documentation/configuration/uart.md">
 * Raspberry Pi UART configuration</a> for additional detail.
 * </p>
 */
public class SerialDevice implements SerialConstants, SerialDeviceInterface {
	/**
	 * Provides descriptive information for a connected serial device. The
	 * information can be provided by the manufacturer of the device or by the UART
	 * used by the device.
	 * <p>
	 * Often the information can be used to identify specific devices connected to a
	 * serial port. If two identical devices are connected to serial ports, they
	 * cannot be differentiated using this information.
	 * </p>
	 * <p>
	 * The following fields are supported:
	 * </p>
	 * <dl>
	 * <dt>deviceFile</dt>
	 * <dd>the file system name for a serial device, e.g., /dev/ttyACM0 /dev/ttyS0,
	 * /dev/ttyAMA0</dd>
	 * <dt>deviceName</dt>
	 * <dd>generally a subset of deviceFile, e.g., ttyACM0</dd>
	 * <dt>description</dt>
	 * <dd>human readable, and theoretically unique, text that identifies the device
	 * attached to a serial port, e.g., Pololu A-Star 32U4; can be generic, e.g.,
	 * Physical Port</dd>
	 * <dt>manufacturer</dt>
	 * <dd>human readable text that identifies the manufacturer of the device
	 * attached to a serial port, e.g., Pololu Corporation; can be null</dd>
	 * <dt>driverName</dt>
	 * <dd>the name of the device driver, e.g., usb:cdc_acm, bcm2835-aux-uart,
	 * uart-pl011</dd>
	 * <dt>usbVendorId</dt>
	 * <dd>a theoretically unique number identifying the vendor, e.g., 1ffb; can be
	 * null</dd>
	 * <dt>usbProductId</dt>
	 * <dd>theoretically unique number identifying the product, e.g., 2300; can be
	 * null</dd>
	 * </dl>
	 */
	public static class DeviceInfo {
		private String deviceName;
		private String deviceFile;
		private String description;
		private String manufacturer;
		private String driverName;
		private String usbVendorId;
		private String usbProductId;

		public DeviceInfo(String deviceName, String deviceFile, String description, String manufacturer,
				String driverName, String usbVendorId, String usbProductId) {
			this.deviceName = deviceName;
			this.deviceFile = deviceFile;
			this.description = description;
			this.manufacturer = manufacturer;
			this.driverName = driverName;
			this.usbVendorId = usbVendorId;
			this.usbProductId = usbProductId;
		}

		public String getDeviceName() {
			return deviceName;
		}

		public String getDeviceFile() {
			return deviceFile;
		}

		public String getDescription() {
			return description;
		}

		public String getManufacturer() {
			return manufacturer;
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

	/**
	 * Attempt to discover the locally attached serial devices using Linux device
	 * tree.
	 * 
	 * @return A list of locally attached devices
	 */
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
		 * Standard is 7 characters I think - ttyYYYN where YYY is something like USB|ACM|AMA and N is the instance number
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
								Logger.error(e, "Error: {}", e);
							}
						} else {
							// Continue searching in sub-directories
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

		String description = null, driver_name = null, manufacturer = null, usb_vendor_id = null, usb_product_id = null;

		// Look for a product description file
		// For ttyUSBx devices
		Path device_root = p.getParent().getParent();
		Path product_file = device_root.resolve("product");
		if (!product_file.toFile().exists()) {
			// For ttyACMx devices
			device_root = p.getParent().getParent().getParent();
			product_file = device_root.resolve("product");
		}

		if (product_file.toFile().exists()) {
			Logger.debug("Processing device {} in {}", device_name, device_root);

			// I believe this means that this is a USB-connected device
			description = Files.lines(product_file).findFirst().orElse(null);

			// GEt the manufacturer
			Path manufacturer_path = device_root.resolve("manufacturer");
			if (manufacturer_path.toFile().exists()) {
				manufacturer = Files.lines(manufacturer_path).findFirst().orElse(null);
			}

			// Get the driver name, e.g. "usb-serial:ch341-uart"
			Path drivers_path = p.resolve("driver").resolve("module").resolve("drivers");
			if (!drivers_path.toFile().exists()) {
				drivers_path = p.resolve("device").resolve("driver").resolve("module").resolve("drivers");
			}
			if (drivers_path.toFile().exists()) {
				driver_name = Files.list(drivers_path).filter(path -> path.toFile().isDirectory())
						.filter(path -> !path.toFile().isHidden()).map(path -> path.getFileName().toString())
						.findFirst().orElse(null);
			}

			// Get the USB vendor and product identifiers
			usb_vendor_id = Files.lines(device_root.resolve("idVendor")).findFirst().orElse(null);
			usb_product_id = Files.lines(device_root.resolve("idProduct")).findFirst().orElse(null);

			// Sometimes the manufacturer isn't set - default it to the USB vendor id as per
			// udevadm
			// Example: Vendor=QinHeng Electronics, Product=HL-340 USB-Serial adapter
			if (manufacturer == null) {
				manufacturer = usb_vendor_id;
			}
		} else {
			// Must be a physical (or emulated) port
			description = "Physical Port";
			Path driver_path = p.resolve("device").resolve("driver");
			if (driver_path.toFile().exists()) {
				driver_name = Paths.get(driver_path.toFile().getCanonicalPath()).getFileName().toString();
			}
		}

		return new DeviceInfo(device_name, "/dev/" + device_name, description, manufacturer, driver_name, usb_vendor_id,
				usb_product_id);
	}

	/**
	 * Serial device builder. Default values:
	 * <ul>
	 * <li>baud: {@link SerialConstants#DEFAULT_BAUD}</li>
	 * <li>dataBits: {@link SerialConstants#DEFAULT_DATA_BITS}</li>
	 * <li>stopBits: {@link SerialConstants#DEFAULT_STOP_BITS}</li>
	 * <li>parity: {@link SerialConstants#DEFAULT_PARITY}</li>
	 * </ul>
	 * 
	 * The <a href="https://man7.org/linux/man-pages/man3/termios.3.html">termios
	 * man page</a> provide more detail on these options. Note that the serial
	 * device is opened in non-canonical mode so that "<em>input is available
	 * immediately (without the user having to type a line-delimiter
	 * character)</em>".
	 */
	public static class Builder {
		private String deviceFilename;
		private int baud = DEFAULT_BAUD;
		private DataBits dataBits = DEFAULT_DATA_BITS;
		private StopBits stopBits = DEFAULT_STOP_BITS;
		private Parity parity = DEFAULT_PARITY;
		private boolean readBlocking = DEFAULT_READ_BLOCKING;
		private int minReadChars = DEFAULT_MIN_READ_CHARS;
		private int readTimeoutMillis = DEFAULT_READ_TIMEOUT_MILLIS;

		protected Builder(String deviceFilename) {
			this.deviceFilename = deviceFilename;
		}

		public Builder setDeviceFilename(String deviceFilename) {
			this.deviceFilename = deviceFilename;
			return this;
		}

		public Builder setBaud(int baud) {
			this.baud = baud;
			return this;
		}

		public Builder setDataBits(DataBits dataBits) {
			this.dataBits = dataBits;
			return this;
		}

		public Builder setStopBits(StopBits stopBits) {
			this.stopBits = stopBits;
			return this;
		}

		public Builder setParity(Parity parity) {
			this.parity = parity;
			return this;
		}

		/*-
		public Builder setReadBlocking(boolean readBlocking) {
			this.readBlocking = readBlocking;
			return this;
		}
		
		public Builder setMinReadChars(int minReadChars) {
			this.minReadChars = minReadChars;
			return this;
		}
		
		public Builder setReadTimeoutMillis(int readTimeoutMillis) {
			this.readTimeoutMillis = readTimeoutMillis;
			return this;
		}
		*/

		public SerialDevice build() {
			return new SerialDevice(deviceFilename, baud, dataBits, stopBits, parity, readBlocking, minReadChars,
					readTimeoutMillis);
		}
	}

	public static Builder builder(String deviceFilename) {
		return new Builder(deviceFilename);
	}

	private InternalSerialDeviceInterface delegate;
	private String deviceFilename;

	/**
	 * Create a new serial device using default values for
	 * {@link SerialConstants#DEFAULT_BAUD baud},
	 * {@link SerialConstants#DEFAULT_DATA_BITS data bits},
	 * {@link SerialConstants#DEFAULT_STOP_BITS stop bits},
	 * {@link SerialConstants#DEFAULT_PARITY parity},
	 * {@link SerialConstants#DEFAULT_READ_BLOCKING read blocking},
	 * {@link SerialConstants#DEFAULT_MIN_READ_CHARS min read chars} and
	 * {@link SerialConstants#DEFAULT_READ_TIMEOUT_MILLIS read timeout}
	 *
	 * @param deviceFilename The O/S file name for the device, e.g. /dev/ttyACM0
	 * @throws RuntimeIOException If an I/O error occurs
	 */
	public SerialDevice(String deviceFilename) throws RuntimeIOException {
		this(deviceFilename, DEFAULT_BAUD, DEFAULT_DATA_BITS, DEFAULT_STOP_BITS, DEFAULT_PARITY, DEFAULT_READ_BLOCKING,
				DEFAULT_MIN_READ_CHARS, DEFAULT_READ_TIMEOUT_MILLIS);
	}

	/**
	 * Create a new serial device using default values for
	 * {@link SerialConstants#DEFAULT_READ_BLOCKING read blocking},
	 * {@link SerialConstants#DEFAULT_MIN_READ_CHARS min read chars} and
	 * {@link SerialConstants#DEFAULT_READ_TIMEOUT_MILLIS read timeout}
	 *
	 * @param deviceFilename The O/S file name for the device, e.g. /dev/ttyACM0
	 * @param baud           Baud rate, see {@link SerialConstants SerialConstants}
	 *                       for valid baud rate values
	 * @param dataBits       Number of {@link SerialConstants.DataBits data bits}
	 * @param stopBits       Number of {@link SerialConstants.StopBits stop bits}
	 * @param parity         Device error detection {@link SerialConstants.Parity
	 *                       parity}
	 * @throws RuntimeIOException If an I/O error occurs
	 */
	public SerialDevice(String deviceFilename, int baud, DataBits dataBits, StopBits stopBits, Parity parity)
			throws RuntimeIOException {
		this(deviceFilename, baud, dataBits, stopBits, parity, DEFAULT_READ_BLOCKING, DEFAULT_MIN_READ_CHARS,
				DEFAULT_READ_TIMEOUT_MILLIS);
	}

	/**
	 * Create a new serial device. Package private - note that readBlocking,
	 * minReadChars and readTimeoutMillis are fixed as per the default values.
	 *
	 * @param deviceFilename    The O/S file name for the device, e.g. /dev/ttyACM0
	 * @param baud              Baud rate, see {@link SerialConstants
	 *                          SerialConstants} for valid baud rate values
	 * @param dataBits          Number of {@link SerialConstants.DataBits data bits}
	 * @param stopBits          Number of {@link SerialConstants.StopBits stop bits}
	 * @param parity            Device error detection {@link SerialConstants.Parity
	 *                          parity}
	 * @param readBlocking      Should all read operations block until data is
	 *                          available?
	 * @param minReadChars      Minimum number of characters to read (note actually
	 *                          an unsigned char hence max value is 255)
	 * @param readTimeoutMillis The read timeout value in milliseconds (note
	 *                          converted to tenths of a second as an unsigned char)
	 * @throws RuntimeIOException If an I/O error occurs
	 */
	SerialDevice(String deviceFilename, int baud, DataBits dataBits, StopBits stopBits, Parity parity,
			boolean readBlocking, int minReadChars, int readTimeoutMillis) throws RuntimeIOException {
		delegate = DeviceFactoryHelper.getNativeDeviceFactory().provisionSerialDevice(deviceFilename, baud, dataBits,
				stopBits, parity, readBlocking, minReadChars, readTimeoutMillis);

		this.deviceFilename = deviceFilename;
	}

	/**
	 * Get the device filename
	 * 
	 * @return the device filename, e.g. /dev/ttyUSB0
	 */
	public String getDeviceFilename() {
		return deviceFilename;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() {
		Logger.trace("close()");
		if (delegate.isOpen()) {
			delegate.close();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int read() throws RuntimeIOException {
		return delegate.read();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public byte readByte() throws RuntimeIOException {
		return delegate.readByte();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void writeByte(byte bVal) throws RuntimeIOException {
		delegate.writeByte(bVal);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int read(byte[] buffer) throws RuntimeIOException {
		return delegate.read(buffer);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void write(byte... buffer) throws RuntimeIOException {
		delegate.write(buffer);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int bytesAvailable() throws RuntimeIOException {
		return delegate.bytesAvailable();
	}
}
/*-
 * Parked text 
 * <li>readBlocking: {@link SerialConstants#DEFAULT_READ_BLOCKING}</li>
 * <li>minReadChars: {@link SerialConstants#DEFAULT_MIN_READ_CHARS}</li>
 * <li>readTimeoutMillis: {@link SerialConstants#DEFAULT_READ_TIMEOUT_MILLIS}</li>
 * 
 * Translating the information on the termios man page to diozero:
 * 
 * The settings of minReadChars and readTimeoutMillis determine the circumstances in which a read(2) completes;
 * there are four distinct cases:
 * <dl>
 * <dt>1) minReadChars == 0, readTimeoutMillis == 0 (polling read)</dt>
 *    <dd>If data is available, read(2) returns immediately, with the lesser of the number of bytes available,
 *    or the number of bytes requested.  If no data is available, read(2) returns 0.</dd>
 *
 * <dt>2) minReadChars > 0, readTimeoutMillis == 0 (blocking read)</dt>
 *    <dd>read(2) blocks until minReadChars bytes are available, and returns up to the number of bytes requested.</dd>
 * 
 * <dt>3) minReadChars == 0, readTimeoutMillis > 0 (read with timeout)</dt>
 *    <dd>TIME specifies the limit for a timer in millis (converted to tenths of a second).
 *    The timer is started when read(2) is called. read(2) returns either when at least one byte of data is
 *    available, or when the timer expires. If the timer expires without any input becoming available, read(2)
 *    returns 0. If data is already available at the time of the call to read(2), the call behaves as though
 *    the data was received immediately after the call.</dd>
 * 
 * <dt>4) minReadChars > 0, readTimeoutMillis > 0 (read with interbyte timeout)</dt>
 *    <dd>readTimeoutMillis specifies the limit for a timer in tenths of a second. Once an initial byte of input
 *    becomes available, the timer is restarted after each further byte is received.
 *    read(2) returns when any of the following conditions are met:
 *      <ul>
 *        <li>minReadChars bytes have been received.</li>
 *        <li>The interbyte timer expires.</li>
 *        <li>The number of bytes requested by read(2) has been received. (POSIX does not specify this termination
 *          condition, and on some other implementations read(2) does not return in this case.)</li>
 *      </ul>
 *    </dd>
 * </dl>
 * 
 * Because the timer is started only after the initial byte becomes available, at least one byte will be read.
 * If data is already available at the time of the call to read(2), the call behaves as though the data was
 * received immediately after the call.
 */
