package com.diozero.util;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     UsbInfo.java  
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
