package com.diozero.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.tinylog.Logger;

import com.diozero.util.DeviceFactoryHelper;
import com.diozero.util.RuntimeIOException;

public class SerialDevice implements SerialConstants, SerialDeviceInterface {
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
	 * tree
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

	private SerialDeviceInterface delegate;
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
	 * @param baud           Baud rate, see {@link com.diozero.api.SerialConstants
	 *                       SerialConstants} for valid baud rate values
	 * @param dataBits       Number of
	 *                       {@link com.diozero.api.SerialConstants.DataBits data
	 *                       bits}
	 * @param stopBits       Number of
	 *                       {@link com.diozero.api.SerialConstants.StopBits stop
	 *                       bits}
	 * @param parity         Device error detection
	 *                       {@link com.diozero.api.SerialConstants.Parity parity}
	 * @throws RuntimeIOException If an I/O error occurs
	 */
	public SerialDevice(String deviceFilename, int baud, DataBits dataBits, StopBits stopBits, Parity parity)
			throws RuntimeIOException {
		this(deviceFilename, baud, dataBits, stopBits, parity, DEFAULT_READ_BLOCKING, DEFAULT_MIN_READ_CHARS,
				DEFAULT_READ_TIMEOUT_MILLIS);
	}

	/**
	 * Create a new serial device
	 *
	 * @param deviceFilename    The O/S file name for the device, e.g. /dev/ttyACM0
	 * @param baud              Baud rate, see
	 *                          {@link com.diozero.api.SerialConstants
	 *                          SerialConstants} for valid baud rate values
	 * @param dataBits          Number of
	 *                          {@link com.diozero.api.SerialConstants.DataBits data
	 *                          bits}
	 * @param stopBits          Number of
	 *                          {@link com.diozero.api.SerialConstants.StopBits stop
	 *                          bits}
	 * @param parity            Device error detection
	 *                          {@link com.diozero.api.SerialConstants.Parity
	 *                          parity}
	 * @param readBlocking      Should all read operations block until data is
	 *                          available?
	 * @param minReadChars      Minimum number of characters to read (note actually
	 *                          an unsigned char hence max value is 255)
	 * @param readTimeoutMillis The read timeout value in milliseconds (note
	 *                          converted to tenths of a second as an unsigned char)
	 * @throws RuntimeIOException If an I/O error occurs
	 */
	public SerialDevice(String deviceFilename, int baud, DataBits dataBits, StopBits stopBits, Parity parity,
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
	public String getKey() {
		return delegate.getKey();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean isOpen() {
		return delegate.isOpen();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void close() {
		Logger.trace("close()");
		delegate.close();
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
	public void write(byte[] buffer) throws RuntimeIOException {
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
