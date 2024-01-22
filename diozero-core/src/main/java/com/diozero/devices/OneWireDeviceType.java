package com.diozero.devices;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     OneWireDeviceType.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2024 diozero
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

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public enum OneWireDeviceType {
	DS18S20(0x10), DS1822(0x22), DS18B20(0x28), DS1825(0x3B), DS28EA00(0x42), MAX31850K(0x3B);

	private static Map<Integer, OneWireDeviceType> TYPES;

	private int id;

	private OneWireDeviceType(int id) {
		this.id = id;

		addType();
	}

	private synchronized void addType() {
		if (TYPES == null) {
			TYPES = new HashMap<>();
		}
		TYPES.put(Integer.valueOf(id), this);
	}

	public int getId() {
		return id;
	}

	public static boolean isValid(Path path) {
		return isValidId(path.getFileName().toString().substring(0, 2));
	}

	public static boolean isValidId(String idString) {
		try {
			return TYPES.containsKey(Integer.valueOf(idString, 16));
		} catch (NumberFormatException e) {
			return false;
		}
	}

	public static OneWireDeviceType valueOf(int id) {
		return TYPES.get(Integer.valueOf(id));
	}

	public static OneWireDeviceType valueOf(Path path) {
		OneWireDeviceType type = TYPES.get(Integer.valueOf(path.getFileName().toString().substring(0, 2), 16));
		if (type == null) {
			throw new IllegalArgumentException(
					"Invalid OneWireThermometer.Type slave='" + path.toFile().getName() + "'");
		}
		return type;
	}
}
