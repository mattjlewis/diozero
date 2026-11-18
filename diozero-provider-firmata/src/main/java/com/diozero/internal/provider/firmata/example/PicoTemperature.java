package com.diozero.internal.provider.firmata.example;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Firmata
 * Filename:     PicoTemperature.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2026 diozero
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

import com.diozero.api.SerialConstants;
import com.diozero.api.SerialDevice;
import com.diozero.internal.provider.firmata.adapter.FirmataAdapter;
import com.diozero.internal.provider.firmata.adapter.FirmataEventListener.EventType;
import com.diozero.internal.provider.firmata.adapter.FirmataProtocol.PinMode;
import com.diozero.internal.provider.firmata.adapter.SerialFirmataTransport;
import com.diozero.util.PropertyUtil;

public class PicoTemperature implements FirmataTestConstants {
	public static void main(String[] args) {
		/*-
		 * https://github.com/DeimosHall/RP2040_CPU_Temperature/blob/main/src/CPU.cpp
		 * adc_init();
		 * adc_set_temp_sensor_enabled(true);
		 * // Select ADC input 4 for internal temperature sensor
		 * adc_select_input(4);
		 */
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
		try (FirmataAdapter adapter = new FirmataAdapter(new SerialFirmataTransport(serial_port_name,
				PropertyUtil.getIntProperty(SERIAL_BAUD_PROP, SerialConstants.BAUD_57600), data_bits, stop_bits, parity,
				SerialConstants.DEFAULT_READ_BLOCKING, SerialConstants.DEFAULT_MIN_READ_CHARS,
				SerialConstants.DEFAULT_READ_TIMEOUT_MILLIS), PicoTemperature::event)) {
			adapter.start();

			int max_26 = adapter.getMax(29, PinMode.ANALOG_INPUT);
			System.out.println("max_26: " + max_26);
			int max_27 = adapter.getMax(29, PinMode.ANALOG_INPUT);
			System.out.println("max_27: " + max_27);
			int max_28 = adapter.getMax(29, PinMode.ANALOG_INPUT);
			System.out.println("max_28: " + max_28);
			int max_29 = adapter.getMax(29, PinMode.ANALOG_INPUT);
			System.out.println("max_29: " + max_29);

			/*-
			 * According to the specifications of this sensor, a temperature of 27 degrees Celsius delivers a voltage of
			 * 0.706 V, with each additional degree reducing the voltage by 1.721 mV, or 0.001721 V.
			 */
			double conversion_factor = 3.3 / max_29;

			while (true) {
				for (int gp = 26; gp < 30; gp++) {
					int reading = adapter.getValue(gp);
					System.out.println("reading[" + gp + "]: " + reading);
					System.out.println("volts[" + gp + "]: " + (reading * conversion_factor));
					if (gp == 29) {
						double temperature = 27 - (reading * conversion_factor - 0.706) / 0.001721;
						System.out.println("Temperature: " + temperature);
					}
				}
				Thread.sleep(2000);
			}
		} catch (InterruptedException e) {
			// Ignore
		}
	}

	public static void event(EventType eventType, int gpio, int value, long epochTime, long nanoTime) {
		/*-
		 * System.out.println("event(" + eventType + ", " + gpio + ", " + value + ", " + epochTime + ", " + nanoTime + ")");
		 */
	}
}
