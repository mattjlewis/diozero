package com.diozero.internal.provider.firmata.adapter.example;

import java.util.Optional;
import java.util.OptionalInt;

import com.diozero.api.SerialConstants;
import com.diozero.api.SerialDevice;
import com.diozero.internal.provider.firmata.adapter.FirmataAdapter;
import com.diozero.internal.provider.firmata.adapter.FirmataAdapter.OneWireReadResponse;
import com.diozero.internal.provider.firmata.adapter.FirmataAdapter.OneWireSearchResponse;
import com.diozero.internal.provider.firmata.adapter.FirmataEventListener.EventType;
import com.diozero.internal.provider.firmata.adapter.FirmataProtocol.PinMode;
import com.diozero.internal.provider.firmata.adapter.SerialFirmataTransport;
import com.diozero.util.Crc;
import com.diozero.util.Hex;
import com.diozero.util.PropertyUtil;
import com.diozero.util.SleepUtil;

public class OneWireTest implements FirmataTestConstants {
	private static final byte DS18B20_FAMILY = 0x28;
	private static final int DALLAS_SCRATCHPAD_SIZE = 9;

	private static final byte CONVERT_TEMPERATURE_COMMAND = 0x44;
	private static final byte WRITE_SCRATCHPAD_COMMAND = 0x4E;
	private static final byte READ_SCRATCHPAD_COMMAND = (byte) 0xBE;
	private static final byte COPY_SCRATCHPAD_COMMAND = (byte) 0x48;
	private static final byte RECALL_EEPROM_COMMAND = (byte) 0xB8;
	private static final byte RECALL_POWER_SUPPLY_COMMAND = (byte) 0xB4;

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

		try (FirmataAdapter adapter = new FirmataAdapter(new SerialFirmataTransport(serial_port_name,
				PropertyUtil.getIntProperty(SERIAL_BAUD_PROP, SerialConstants.BAUD_57600), data_bits, stop_bits, parity,
				SerialConstants.DEFAULT_READ_BLOCKING, SerialConstants.DEFAULT_MIN_READ_CHARS,
				SerialConstants.DEFAULT_READ_TIMEOUT_MILLIS), OneWireTest::event)) {
			System.out.println("Connecting...");
			adapter.start();
			System.out.println("Connected");

			int one_wire_pin = 2;

			// For some reason this doesn't set the pin mode to one-wire
			adapter.oneWireConfig(one_wire_pin, false);
			adapter.setPinMode(one_wire_pin, PinMode.ONEWIRE);

			// Search for one-wire devices attacked to this pin...
			OneWireSearchResponse search = adapter.oneWireSearch(one_wire_pin, false);
			System.out.println("Got " + search.getAddresses().length + " addresses on pin #" + search.getGpio());
			for (byte[] address : search.getAddresses()) {
				byte[] data = new byte[7];
				System.arraycopy(address, 0, data, 0, data.length);

				int family = address[0] & 0xff;
				// Each device has a unique 64-bit serial code, first byte is the family, next
				// 48 bits are the serial number, final byte is the CRC
				long serial_number = 0;
				for (int i = 6; i > 0; i--) {
					serial_number <<= 8;
					serial_number += address[i] & 0xff;
				}
				int crc = Crc.crc8(Crc.CRC8_MAXIM, data);
				System.out.format("Detected device with address: %s, serial_number: %d, CRC: %d (%s)%n",
						Hex.encodeHexString(data, 1, ':'), Long.valueOf(serial_number), Integer.valueOf(crc),
						crc == (address[7] & 0xff) ? "Correct" : "Incorrect");

				if (family == DS18B20_FAMILY) {
					System.out.println("Detected a 1-wire Dallas DS18B20 thermometer");
				}
			}

			DS18B20Config config = readResolution(adapter, one_wire_pin, search.getAddresses()[0]);

			for (int i = 0; i < 10; i++) {
				readTemp(adapter, one_wire_pin, search.getAddresses()[0], config.getResolution());
				SleepUtil.sleepSeconds(1);
			}

			for (DS18B20Resolution res : DS18B20Resolution.values()) {
				adapter.oneWireCommands(one_wire_pin, true, false, Optional.of(search.getAddresses()[0]),
						OptionalInt.empty(), OptionalInt.empty(), OptionalInt.empty(),
						Optional.of(new byte[] { WRITE_SCRATCHPAD_COMMAND, config.getAlarmHigh(), config.getAlarmLow(),
								res.toConfigReg() }));

				readResolution(adapter, one_wire_pin, search.getAddresses()[0]);

				for (int i = 0; i < 10; i++) {
					readTemp(adapter, one_wire_pin, search.getAddresses()[0], config.getResolution());
					SleepUtil.sleepSeconds(0.5);
				}
			}
		}
	}

	private static DS18B20Config readResolution(FirmataAdapter adapter, int oneWirePin, byte[] address) {
		int correlation_id = 1111;

		// Read the configuration register
		OneWireReadResponse config_reg_read_response = adapter.oneWireCommands(oneWirePin, true, false,
				Optional.of(address), OptionalInt.of(DALLAS_SCRATCHPAD_SIZE), OptionalInt.of(correlation_id),
				OptionalInt.empty(), Optional.of(new byte[] { READ_SCRATCHPAD_COMMAND })).get();
		byte[] scratchpad_data = config_reg_read_response.getData();

		// Validate the CRC (assuming full scratchpad read)
		int crc = Crc.crc8(Crc.CRC8_MAXIM, scratchpad_data[0], scratchpad_data[1], scratchpad_data[2],
				scratchpad_data[3], scratchpad_data[4], scratchpad_data[5], scratchpad_data[6], scratchpad_data[7]);
		if (crc != (scratchpad_data[8] & 0xff)) {
			System.out.println("Error, wrong CRC, expected " + (scratchpad_data[8] & 0xff) + ", got " + crc);
		}

		// Note the power-on reset value of the temperature register is +85 degC
		double temp = getTemp(scratchpad_data);
		System.out.println("temp at initialisation: " + temp);

		DS18B20Resolution res = DS18B20Resolution.fromConfigReg(scratchpad_data[4]);
		System.out.println("Detected resolution: " + res);

		// Alarm high and low values are 2's complement
		byte alarm_high_trigger = scratchpad_data[2];
		byte alarm_low_trigger = scratchpad_data[3];
		System.out.println("alarm_high_trigger: " + alarm_high_trigger + ", alarm_low_trigger: " + alarm_low_trigger);

		return new DS18B20Config(res, alarm_high_trigger, alarm_low_trigger);
	}

	private static void readTemp(FirmataAdapter adapter, int oneWirePin, byte[] address, DS18B20Resolution resolution) {
		int correlation_id = 9999;

		adapter.oneWireCommands(oneWirePin, true, false, Optional.of(address), OptionalInt.empty(), OptionalInt.empty(),
				OptionalInt.empty(), Optional.of(new byte[] { CONVERT_TEMPERATURE_COMMAND }));
		OneWireReadResponse read_response = adapter
				.oneWireCommands(oneWirePin, true, false, Optional.of(address), OptionalInt.of(DALLAS_SCRATCHPAD_SIZE),
						OptionalInt.of(correlation_id), OptionalInt.of(resolution.temperatureConversionTime()),
						Optional.of(new byte[] { READ_SCRATCHPAD_COMMAND }))
				.get();

		// Validate the correlation id
		if (read_response.getCorrelationId() != correlation_id) {
			System.out.println("Error, wrong correlation id, expected " + correlation_id + ", got "
					+ read_response.getCorrelationId());
		}

		byte[] scratchpad_data = read_response.getData();

		// Validate the CRC (assuming full scratchpad read)
		int crc = Crc.crc8(Crc.CRC8_MAXIM, scratchpad_data[0], scratchpad_data[1], scratchpad_data[2],
				scratchpad_data[3], scratchpad_data[4], scratchpad_data[5], scratchpad_data[6], scratchpad_data[7]);
		if (crc != (scratchpad_data[8] & 0xff)) {
			System.out.println("Error, wrong CRC, expected " + (scratchpad_data[8] & 0xff) + ", got " + crc);
		}

		System.out.println("Temperature read response on pin #" + read_response.getGpio() + " (correlation id "
				+ read_response.getCorrelationId() + "), value: " + getTemp(scratchpad_data));
	}

	public static void event(EventType eventType, int gpio, int value, long epochTime, long nanoTime) {
		System.out
				.println("event(" + eventType + ", " + gpio + ", " + value + ", " + epochTime + ", " + nanoTime + ")");
	}

	private static float getTemp(byte[] data) {
		short raw = (short) ((data[1] << 8) | (data[0] & 0xff));
		// 2's complement, with bit 4 representing 2^0, bit 0 2^-4, bit 10 2^6
		return raw / 16f;
	}
}
