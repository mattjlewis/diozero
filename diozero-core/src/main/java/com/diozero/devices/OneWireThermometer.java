package com.diozero.devices;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     OneWireThermometer.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2023 diozero
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
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.diozero.api.RuntimeIOException;

/**
 * @see <a href=
 *      "https://www.maximintegrated.com/en/products/analog/sensors-and-sensor-interface/DS18B20.html">DS18B20
 *      Programmable Resolution 1-Wire Digital Thermometer</a>
 * @see <a href=
 *      "https://learn.adafruit.com/adafruits-raspberry-pi-lesson-11-ds18b20-temperature-sensing?view=all">Adafruit's
 *      Raspberry Pi Lesson 11. DS18B20 Temperature Sensing</a>
 * @see <a href="https://github.com/timofurrer/w1thermsensor">W1ThermSensor
 *      Python library</a>
 */
public class OneWireThermometer implements ThermometerInterface {
	private static final String BASE_DIRECTORY = "/sys/bus/w1/devices";
	private static final String SLAVE_FILE = "w1_slave";

	public static List<OneWireThermometer> getAvailableSensors() {
		return getAvailableSensors(BASE_DIRECTORY);
	}

	public static List<OneWireThermometer> getAvailableSensors(String folder) {
		Path sensor_path = Paths.get(folder);
		Predicate<Path> is_sensor = path -> path.toFile().isDirectory() && OneWireDeviceType.isValid(path);
		try {
			return Files.list(sensor_path).filter(is_sensor).map(OneWireThermometer::new).collect(Collectors.toList());
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	private OneWireDeviceType type;
	private Path slaveFilePath;
	private String serialNumber;

	private OneWireThermometer(Path path) {
		this.type = OneWireDeviceType.valueOf(path);
		slaveFilePath = path.resolve(SLAVE_FILE);
		serialNumber = slaveFilePath.getParent().toFile().getName().split("-")[1];
	}

	/**
	 * Get temperature in degrees celsius
	 * 
	 * @return Temperature (deg C)
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	@Override
	public float getTemperature() throws RuntimeIOException {
		/*-
		 * 53 01 4b 46 7f ff 0d 10 e9 : crc=e9 YES
		 * 53 01 4b 46 7f ff 0d 10 e9 t=21187
		 * (?s).*crc=[0-9a-f]+ ([A-Z]+).*t=([0-9]+)
		 */
		try {
			List<String> lines = Files.readAllLines(slaveFilePath);
			if (!lines.get(0).trim().endsWith("YES")) {
				throw new RuntimeIOException("1-wire slave not ready, serial='" + serialNumber + "'");
			}

			return Float.parseFloat(lines.get(1).split("=")[1]) / 1000;
		} catch (IOException e) {
			throw new RuntimeIOException("I/O error reading 1-wire slave, serial='" + serialNumber + "'");
		}
	}

	public OneWireDeviceType getType() {
		return type;
	}

	public String getSerialNumber() {
		return serialNumber;
	}

	@Override
	public void close() {
		// nop
	}
}
