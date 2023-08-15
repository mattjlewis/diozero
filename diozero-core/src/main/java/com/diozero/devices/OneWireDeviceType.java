package com.diozero.devices;

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
