package com.diozero.internal.provider.firmata.ds18b20;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Firmata
 * Filename:     FirmataDS18B20Device.java
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

import java.util.OptionalInt;

import org.tinylog.Logger;

import com.diozero.api.RuntimeIOException;
import com.diozero.devices.ThermometerInterface;
import com.diozero.internal.provider.firmata.adapter.FirmataAdapter;
import com.diozero.internal.provider.firmata.adapter.FirmataAdapter.OneWireReadResponse;
import com.diozero.internal.provider.firmata.example.FirmataTestConstants;
import com.diozero.util.Crc;

public class FirmataDS18B20Device implements ThermometerInterface, FirmataTestConstants {
	public static final byte DS18B20_FAMILY = 0x28;
	private static final int DALLAS_SCRATCHPAD_SIZE = 9;

	private static final byte CONVERT_TEMPERATURE_COMMAND = 0x44;
	private static final byte WRITE_SCRATCHPAD_COMMAND = 0x4E;
	private static final byte READ_SCRATCHPAD_COMMAND = (byte) 0xBE;
	private static final byte COPY_SCRATCHPAD_COMMAND = (byte) 0x48;
	private static final byte RECALL_EEPROM_COMMAND = (byte) 0xB8;
	private static final byte RECALL_POWER_SUPPLY_COMMAND = (byte) 0xB4;

	public static boolean validAddress(byte[] address) {
		return address[0] == DS18B20_FAMILY;
	}

	private final FirmataAdapter adapter;
	private final int oneWirePin;
	private final byte[] address;
	private final byte family;
	private final long serialNumber;
	private DS18B20Config config;

	public FirmataDS18B20Device(FirmataAdapter adapter, int oneWirePin, byte[] address) {
		this.adapter = adapter;
		this.oneWirePin = oneWirePin;
		this.address = address;

		// Each device has a unique 64-bit serial code, first byte is the family, next
		// 48 bits are the serial number, final byte is the CRC
		long l = 0;
		for (int i = 6; i > 0; i--) {
			l <<= 8;
			l += address[i] & 0xff;
		}
		serialNumber = l;

		family = address[0];
		if (family != DS18B20_FAMILY) {
			throw new IllegalArgumentException("Device family (" + family + ") isn't DS18B20 (" + DS18B20_FAMILY + ")");
		}

		config = readConfiguration(adapter, oneWirePin, address);
	}

	public byte[] getAddress() {
		return address;
	}

	public byte getFamily() {
		return family;
	}

	public long getSerialNumber() {
		return serialNumber;
	}

	public DS18B20Config getConfiguration() {
		return config;
	}

	public DS18B20Config readConfiguration() {
		config = getConfiguration();
		return config;
	}

	public DS18B20Resolution getResolution() {
		return config.getResolution();
	}

	public byte getAlarmHighTrigger() {
		return config.getAlarmHighTrigger();
	}

	public byte getAlarmLowTrigger() {
		return config.getAlarmLowTrigger();
	}

	public void setAlarmTriggers(byte alarmHighTrigger, byte alarmLowTrigger) {
		updateConfiguration(adapter, oneWirePin, address, alarmHighTrigger, alarmLowTrigger, config.getResolution());
		config.setAlarmHighTrigger(alarmHighTrigger);
		config.setAlarmLowTrigger(alarmLowTrigger);
	}

	public void setResolution(DS18B20Resolution resolution) {
		updateConfiguration(adapter, oneWirePin, address, config.getAlarmHighTrigger(), config.getAlarmLowTrigger(),
				resolution);
		config.setResolution(resolution);
	}

	@Override
	public float getTemperature() throws RuntimeIOException {
		return readTemperature(adapter, oneWirePin, address, config.getResolution());
	}

	@Override
	public void close() throws RuntimeIOException {
		// TODO Auto-generated method stub
	}

	private static DS18B20Config readConfiguration(FirmataAdapter adapter, int oneWirePin, byte[] address) {
		int correlation_id = 1111;

		// Read the configuration register
		OneWireReadResponse config_reg_read_response = adapter.oneWireDeviceReadRegister(oneWirePin, true, address,
				OptionalInt.empty(), READ_SCRATCHPAD_COMMAND, DALLAS_SCRATCHPAD_SIZE, correlation_id);
		/*-
		config_reg_read_response = adapter.oneWireCommands(oneWirePin, true, false, Optional.of(address),
				OptionalInt.of(DALLAS_SCRATCHPAD_SIZE), OptionalInt.of(correlation_id), OptionalInt.empty(),
				Optional.of(new byte[] { READ_SCRATCHPAD_COMMAND })).get();
		*/
		byte[] scratchpad_data = config_reg_read_response.getData();

		// Validate the CRC (assuming full scratchpad read)
		int crc = Crc.crc8(Crc.CRC8_MAXIM, scratchpad_data[0], scratchpad_data[1], scratchpad_data[2],
				scratchpad_data[3], scratchpad_data[4], scratchpad_data[5], scratchpad_data[6], scratchpad_data[7]);
		if (crc != (scratchpad_data[8] & 0xff)) {
			Logger.error("Wrong CRC, expected {}, got {}", Integer.valueOf(scratchpad_data[8] & 0xff),
					Integer.valueOf(crc));
		}

		// Note the power-on reset value of the temperature register should be +85 degC
		Logger.trace("Temp while reading config: {}", Float.valueOf(extractTemperature(scratchpad_data)));

		// Alarm high and low values are 2's complement signed bytes
		return new DS18B20Config(scratchpad_data[2], scratchpad_data[3],
				DS18B20Resolution.fromConfigReg(scratchpad_data[4]));
	}

	private static void updateConfiguration(FirmataAdapter adapter, int oneWirePin, byte[] address,
			byte alarmHighTrigger, byte alarmLowTrigger, DS18B20Resolution resolution) {
		adapter.oneWireDeviceWriteRegister(oneWirePin, true, address, WRITE_SCRATCHPAD_COMMAND, alarmHighTrigger,
				alarmLowTrigger, resolution.toConfigReg());
		/*-
		adapter.oneWireCommands(oneWirePin, true, false, Optional.of(address), OptionalInt.empty(), OptionalInt.empty(),
				OptionalInt.empty(), Optional.of(new byte[] { WRITE_SCRATCHPAD_COMMAND, alarmHighTrigger,
						alarmLowTrigger, resolution.toConfigReg() }));
		*/
	}

	private static float readTemperature(FirmataAdapter adapter, int oneWirePin, byte[] address,
			DS18B20Resolution resolution) {
		int correlation_id = 9999;

		adapter.oneWireDeviceWriteRegister(oneWirePin, true, address, CONVERT_TEMPERATURE_COMMAND);
		OneWireReadResponse read_response = adapter.oneWireDeviceReadRegister(correlation_id, true, address,
				OptionalInt.of(resolution.temperatureConversionTime()), READ_SCRATCHPAD_COMMAND, DALLAS_SCRATCHPAD_SIZE,
				correlation_id);
		/*-
		adapter.oneWireCommands(oneWirePin, true, false, Optional.of(address), OptionalInt.empty(), OptionalInt.empty(),
				OptionalInt.empty(), Optional.of(new byte[] { CONVERT_TEMPERATURE_COMMAND }));
		OneWireReadResponse read_response = adapter
				.oneWireCommands(oneWirePin, true, false, Optional.of(address), OptionalInt.of(DALLAS_SCRATCHPAD_SIZE),
						OptionalInt.of(correlation_id), OptionalInt.of(resolution.temperatureConversionTime()),
						Optional.of(new byte[] { READ_SCRATCHPAD_COMMAND }))
				.get();
		*/

		// Validate the correlation id
		if (read_response.getCorrelationId() != correlation_id) {
			Logger.error("Wrong correlation id, expected {} got ", Integer.valueOf(correlation_id),
					Integer.valueOf(read_response.getCorrelationId()));
		}

		byte[] scratchpad_data = read_response.getData();

		// Validate the CRC (assuming full scratchpad read)
		int crc = Crc.crc8(Crc.CRC8_MAXIM, scratchpad_data[0], scratchpad_data[1], scratchpad_data[2],
				scratchpad_data[3], scratchpad_data[4], scratchpad_data[5], scratchpad_data[6], scratchpad_data[7]);
		if (crc != (scratchpad_data[8] & 0xff)) {
			Logger.error("Wrong CRC, expected {}, got {}", Integer.valueOf((scratchpad_data[8] & 0xff)),
					Integer.valueOf(crc));
		}

		return extractTemperature(scratchpad_data);
	}

	private static float extractTemperature(byte[] scratchPadData) {
		short raw = (short) ((scratchPadData[1] << 8) | (scratchPadData[0] & 0xff));
		// 2's complement, with bit 4 representing 2^0, bit 0 2^-4, bit 10 2^6
		return raw / 16f;
	}
}
