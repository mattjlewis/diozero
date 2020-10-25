package com.diozero.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Optional;

import org.tinylog.Logger;

public class UsbInfo {
	private static final String USB_ID_DATABASE_PROP = "diozero.usb.ids";
	private static final String DEFAULT_USB_ID_DATABASE = "/var/lib/usbutils/usb.ids";
	private static final String USB_ID_DATABASE;
	
	static {
		USB_ID_DATABASE = PropertyUtil.getProperty(USB_ID_DATABASE_PROP, DEFAULT_USB_ID_DATABASE);
	}

	public static Optional<String[]> resolve(String vendorId, String productId) {
		String vendor_name = null;
		String product_name = null;

		try {
			Iterator<String> it = Files.lines(Paths.get(USB_ID_DATABASE))
					.filter(line -> !line.startsWith("#")).filter(line -> !line.trim().isEmpty()).iterator();
			while (it.hasNext()) {
				String line = it.next();
				if (line.startsWith("C ")) {
					break;
				}
				if (line.startsWith(vendorId)) {
					vendor_name = line.substring(vendorId.length()).trim();
					// Now search for the product name
					while (it.hasNext()) {
						line = it.next();
						if (!line.startsWith("\t")) {
							break;
						}
						if (line.trim().startsWith(productId)) {
							product_name = line.trim().substring(productId.length()).trim();
							break;
						}
					}
					break;
				}
			}
		} catch (IOException e) {
			Logger.error(e);
		}

		if (vendor_name == null) {
			return Optional.empty();
		}

		return Optional.of(new String[] { vendor_name, product_name });
	}
}
