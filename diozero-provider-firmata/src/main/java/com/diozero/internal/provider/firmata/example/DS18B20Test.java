package com.diozero.internal.provider.firmata.example;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Firmata
 * Filename:     DS18B20Test.java
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

import java.util.List;
import java.util.stream.Collectors;

import com.diozero.api.SerialConstants;
import com.diozero.api.SerialDevice;
import com.diozero.internal.provider.firmata.adapter.FirmataAdapter;
import com.diozero.internal.provider.firmata.adapter.FirmataEventListener.EventType;
import com.diozero.internal.provider.firmata.adapter.SerialFirmataTransport;
import com.diozero.internal.provider.firmata.ds18b20.DS18B20Resolution;
import com.diozero.internal.provider.firmata.ds18b20.FirmataDS18B20Device;
import com.diozero.util.PropertyUtil;
import com.diozero.util.SleepUtil;

public class DS18B20Test implements FirmataTestConstants {
	public static void main(String[] args) {
		String serial_port_name = PropertyUtil.getProperty(SERIAL_PORT_PROP, null);
		if (serial_port_name == null) {
			return;
		}

		SerialDevice.DataBits data_bits = SerialConstants.DEFAULT_DATA_BITS;
		String val = PropertyUtil.getProperty(SERIAL_DATA_BITS_PROP, null);
		if (val != null) {
			data_bits = SerialDevice.DataBits.valueOf(val.trim());
		}

		SerialDevice.StopBits stop_bits = SerialConstants.DEFAULT_STOP_BITS;
		val = PropertyUtil.getProperty(SERIAL_STOP_BITS_PROP, null);
		if (val != null) {
			stop_bits = SerialDevice.StopBits.valueOf(val.trim());
		}

		SerialDevice.Parity parity = SerialConstants.DEFAULT_PARITY;
		val = PropertyUtil.getProperty(SERIAL_PARITY_PROP, null);
		if (val != null) {
			parity = SerialDevice.Parity.valueOf(val.trim());
		}

		if (args.length < 1) {
			System.out.println("Usage: " + DS18B20Test.class.getName() + " <1-wire pin>");
			return;
		}

		int one_wire_pin = Integer.parseInt(args[0]);

		try (FirmataAdapter adapter = new FirmataAdapter(new SerialFirmataTransport(serial_port_name,
				PropertyUtil.getIntProperty(SERIAL_BAUD_PROP, SerialConstants.BAUD_57600), data_bits, stop_bits, parity,
				SerialConstants.DEFAULT_READ_BLOCKING, SerialConstants.DEFAULT_MIN_READ_CHARS,
				SerialConstants.DEFAULT_READ_TIMEOUT_MILLIS), DS18B20Test::event)) {
			adapter.start();

			List<byte[]> ds18b20_addresses = adapter.detectOneWireDevices(one_wire_pin).stream()
					.filter(FirmataDS18B20Device::validAddress).collect(Collectors.toList());

			if (ds18b20_addresses.size() == 0) {
				System.out.println("Didn't detect any DS18B20 devices on pin " + one_wire_pin);
				return;
			}

			System.out.println("Detected " + ds18b20_addresses.size() + " DS18B20 devices on pin " + one_wire_pin);

			try (FirmataDS18B20Device ds18b20 = new FirmataDS18B20Device(adapter, one_wire_pin,
					ds18b20_addresses.get(0))) {
				for (int i = 0; i < 10; i++) {
					System.out.println("Temperature (1-wire pin #" + one_wire_pin + "): " + ds18b20.getTemperature()
							+ " (resolution: " + ds18b20.getResolution() + ")");
					SleepUtil.sleepSeconds(1);
				}

				for (DS18B20Resolution res : DS18B20Resolution.values()) {
					ds18b20.setResolution(res);
					System.out.println(ds18b20.getConfiguration().getResolution());

					for (int i = 0; i < 10; i++) {
						System.out.println("Temperature (1-wire pin #" + one_wire_pin + "): " + ds18b20.getTemperature()
								+ " (resolution: " + ds18b20.getResolution() + ")");
						SleepUtil.sleepSeconds(0.5);
					}
				}
			}
		}
	}

	public static void event(EventType eventType, int gpio, int value, long epochTime, long nanoTime) {
		System.out
				.println("event(" + eventType + ", " + gpio + ", " + value + ", " + epochTime + ", " + nanoTime + ")");
	}
}
