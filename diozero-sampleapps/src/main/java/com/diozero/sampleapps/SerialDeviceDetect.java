package com.diozero.sampleapps;

import com.diozero.api.SerialDevice;
import com.diozero.util.UsbInfo;

public class SerialDeviceDetect {
	public static void main(String[] args) {
		SerialDevice.getLocalSerialDevices().forEach(device -> print(device));
	}

	private static void print(SerialDevice.DeviceInfo deviceInfo) {
		System.out.format(
				"Name: %s, File: %s, Description: %s, Manufacturer: %s, Driver: %s, USB Vendor Id: %s, USB Product Id: %s%n",
				deviceInfo.getDeviceName(), deviceInfo.getDeviceFile(), deviceInfo.getDescription(),
				deviceInfo.getManufacturer(), deviceInfo.getDriverName(), deviceInfo.getUsbVendorId(),
				deviceInfo.getUsbProductId());

		if (deviceInfo.getUsbVendorId() != null) {
			System.out.format("USB Device Info: %s%n",
					UsbInfo.resolve(deviceInfo.getUsbVendorId(), deviceInfo.getUsbProductId())
							.map(usb_info -> String.format("Vendor=%s, Product=%s", usb_info[0], usb_info[1]))
							.orElse("Not Found"));
		}
	}
}
