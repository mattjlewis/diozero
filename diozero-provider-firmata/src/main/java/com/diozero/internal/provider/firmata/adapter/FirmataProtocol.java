package com.diozero.internal.provider.firmata.adapter;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Firmata
 * Filename:     FirmataProtocol.java
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

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * https://github.com/firmata/protocol/blob/master/protocol.md
 */
public interface FirmataProtocol {
	// Message command bytes (128-255/0x80-0xFF)
	byte DIGITAL_IO_START = (byte) 0x90; // send data for a digital port (collection of 8 pins)
	byte DIGITAL_IO_END = (byte) 0x9F; // Max 15 ports
	byte ANALOG_IO_START = (byte) 0xE0; // send data for an analog pin (or PWM)
	byte ANALOG_IO_END = (byte) 0xEF; // The range of pins is limited to [0..15]
	byte REPORT_ANALOG_PIN = (byte) 0xC0; // enable analog input by pin #
	byte REPORT_DIGITAL_PORT = (byte) 0xD0; // enable digital input by port pair

	byte SET_PIN_MODE = (byte) 0xF4; // set a pin to INPUT/OUTPUT/PWM/etc
	byte SET_DIGITAL_PIN_VALUE = (byte) 0xF5; // set value of an individual digital pin

	byte PROTOCOL_VERSION = (byte) 0xF9; // report protocol version
	byte SYSTEM_RESET = (byte) 0xFF; // reset from MIDI

	byte START_SYSEX = (byte) 0xF0; // start a MIDI Sysex message
	byte END_SYSEX = (byte) 0xF7; // end a MIDI Sysex message

	// Extended command set using sysex (0-127/0x00-0x7F)
	// IDs 0x00 - 0x0F are reserved for user defined commands
	byte SERIAL_MESSAGE = 0x60; // communicate with serial devices, including other boards
	byte ENCODER_DATA = 0x61; // reply with encoder's current positions
	byte ACCELSTEPPER_DATA = 0x62; // control a stepper motor
	byte REPORT_DIGITAL_PIN = 0x63; // (reserved)
	byte EXTENDED_REPORT_ANALOG = 0x64; // (reserved)
	byte REPORT_FEATURES = 0x65; // Report the features supported by the device (proposed API)
	byte SPI_DATA = 0x68; // SPI Commands start with this byte
	byte ANALOG_MAPPING_QUERY = 0x69; // ask for mapping of analog to pin numbers
	byte ANALOG_MAPPING_RESPONSE = 0x6A; // reply with mapping info
	byte CAPABILITY_QUERY = 0x6B; // ask for supported modes and resolution of all pins
	byte CAPABILITY_RESPONSE = 0x6C; // reply with supported modes and resolution
	byte PIN_STATE_QUERY = 0x6D; // ask for a pin's current mode and state (different than value)
	byte PIN_STATE_RESPONSE = 0x6E; // reply with a pin's current mode and state (different than value)
	byte EXTENDED_ANALOG = 0x6F; // analog write (PWM, Servo, etc) to any pin
	byte SERVO_CONFIG = 0x70; // set max angle, minPulse, maxPulse, freq
	byte STRING_DATA = 0x71; // a string message with 14-bits per char
	byte STEPPER_DATA = 0x72; // control a stepper motor
	byte ONEWIRE_DATA = 0x73; // send an OneWire read/write/reset/select/skip/search request
	byte DHTSENSOR_DATA = 0x74; // Used by DhtFirmata
	byte SHIFT_DATA = 0x75; // a bitstream to/from a shift register
	byte I2C_REQUEST = (byte) 0x76; // send an I2C read/write request
	byte I2C_REPLY = (byte) 0x77; // a reply to an I2C read request
	byte I2C_CONFIG = (byte) 0x78; // config I2C settings such as delay times and power pins
	byte REPORT_FIRMWARE = 0x79; // report name and version of the firmware
	byte SAMPLING_INTERVAL = 0x7A; // the interval at which analog input is sampled (default = 19ms)
	byte SCHEDULER_DATA = 0x7B; // send a createtask/deletetask/addtotask/schedule/querytasks/querytask
								// request to the scheduler
	byte ANALOG_CONFIG = 0x7C; // (reserved)
	byte FREQUENCY_COMMAND = 0x7D; // Command for the Frequency module
	byte SYSEX_NON_REALTIME = 0x7E; // MIDI Reserved for non-realtime messages
	byte SYSEX_REALTIME = 0X7F; // MIDI Reserved for realtime messages

	// OneWire
	byte ONEWIRE_SEARCH_REQUEST = 0x40;
	byte ONEWIRE_CONFIG_REQUEST = 0x41;
	byte ONEWIRE_SEARCH_REPLY = 0x42;
	byte ONEWIRE_READ_REPLY = 0x43;
	byte ONEWIRE_SEARCH_ALARMS_REQUEST = 0x44;
	byte ONEWIRE_SEARCH_ALARMS_REPLY = 0x45;
	byte ONEWIRE_PARASITIC_POWER_ON = 0x00;
	byte ONEWIRE_PARASITIC_POWER_OFF = 0x01;

	// Scheduler
	byte MAX_TASK_ID = 0x7f;
	// Scheduler instructions
	byte CREATE_FIRMATA_TASK = 0;
	byte DELETE_FIRMATA_TASK = 1;
	byte ADD_TO_FIRMATA_TASK = 2;
	byte DELAY_FIRMATA_TASK = 3;
	byte SCHEDULE_FIRMATA_TASK = 4;
	byte QUERY_ALL_FIRMATA_TASKS = 5;
	byte QUERY_FIRMATA_TASK = 6;
	byte RESET_FIRMATA_TASKS = 7;
	// Scheduler replies
	byte ERROR_TASK_REPLY = 8;
	byte QUERY_ALL_TASKS_REPLY = 9;
	byte QUERY_TASK_REPLY = 10;

	// Report features (proposed)
	byte REPORT_FEATURES_QUERY = 0;
	byte REPORT_FEATURES_RESPONSE = 1;

	static String readString(ByteBuffer buffer) {
		// Each char is actually sent as 2 7-bit bytes (LSB first)
		byte[] chars = new byte[buffer.remaining() / 2];
		for (int i = 0; i < chars.length; i++) {
			chars[i] = (byte) ((buffer.get() & 0x7f) | ((buffer.get() & 0x01) << 7));
		}
		return new String(chars, StandardCharsets.UTF_8);
	}

	static byte[] convertToLsbMsb(int value) {
		return new byte[] { (byte) (value & 0x7f), (byte) ((value >> 7) & 0x7f) };
	}

	static byte[] createSetSamplingIntervalMessage(int intervalMs) {
		byte[] lsb_msb = convertToLsbMsb(intervalMs);
		return new byte[] { START_SYSEX, SAMPLING_INTERVAL, lsb_msb[0], lsb_msb[1], END_SYSEX };
	}

	static byte[] createEnableAnalogReportingMessage(int adcNum, boolean enabled) {
		return new byte[] { (byte) (REPORT_ANALOG_PIN | adcNum), (byte) (enabled ? 1 : 0) };
	}

	static byte[] createEnableDigitalReportingMessage(int gpio, boolean enabled) {
		return new byte[] { (byte) (REPORT_DIGITAL_PORT | (gpio >> 3)), (byte) (enabled ? 1 : 0) };
	}

	static byte[] createSetPinModeMessage(int gpio, PinMode pinMode) {
		return new byte[] { SET_PIN_MODE, (byte) gpio, (byte) pinMode.ordinal() };
	}

	static byte[] createSetDigitalValuesMessage(int port, byte values) {
		byte[] lsb_msb = convertToLsbMsb(values);
		return new byte[] { (byte) (DIGITAL_IO_START | (port & 0x0f)), lsb_msb[0], lsb_msb[1] };
	}

	static byte[] createSetDigitalValueMessage(int gpio, boolean value) {
		return new byte[] { SET_DIGITAL_PIN_VALUE, (byte) gpio, (byte) (value ? 1 : 0) };
	}

	static byte[] createSetValueMessage(int gpio, int value) {
		// Non-extended analog accommodates 16 ports (E0-Ef), with a max value of 16384
		// (2^14)
		if (gpio < 16 && value < 16384) {
			byte[] lsb_msb = convertToLsbMsb(value);
			return new byte[] { (byte) (ANALOG_IO_START | gpio), lsb_msb[0], lsb_msb[1] };
		}

		byte[] bytes = encodeValue(value);
		byte[] data = new byte[4 + bytes.length];
		data[0] = START_SYSEX;
		data[1] = EXTENDED_ANALOG;
		data[2] = (byte) gpio;
		System.arraycopy(bytes, 0, data, 3, bytes.length);
		data[data.length - 1] = END_SYSEX;
		return data;
	}

	static byte[] createServoConfigMessage(int gpio, int minPulse, int maxPulse) {
		byte[] min_pulse_lsb_msb = convertToLsbMsb(minPulse);
		byte[] max_pulse_lsb_msb = convertToLsbMsb(maxPulse);
		return new byte[] { START_SYSEX, SERVO_CONFIG, (byte) gpio, min_pulse_lsb_msb[0], min_pulse_lsb_msb[1],
				max_pulse_lsb_msb[0], max_pulse_lsb_msb[1], END_SYSEX };
	}

	static byte[] createCreateTaskMessage(int taskId, int length) {
		byte[] length_lsb_msb = convertToLsbMsb(length);
		return new byte[] { START_SYSEX, SCHEDULER_DATA, CREATE_FIRMATA_TASK, (byte) taskId, length_lsb_msb[0],
				length_lsb_msb[1], END_SYSEX };
	}

	static byte[] createAddToTaskMessage(int taskId, byte[] taskData) {
		byte[] taskdata_encoded = to7BitArray(taskData);

		byte[] data = new byte[4 + taskdata_encoded.length + 1];
		int index = 0;
		data[index++] = START_SYSEX;
		data[index++] = SCHEDULER_DATA;
		data[index++] = ADD_TO_FIRMATA_TASK;
		data[index++] = (byte) taskId;
		System.arraycopy(taskdata_encoded, 0, data, index, taskdata_encoded.length);
		index += taskdata_encoded.length;
		data[index++] = END_SYSEX;

		return data;
	}

	static byte[] createScheduleTaskMessage(int taskId, int delayMs) {
		byte[] time_data = new byte[4];
		time_data[0] = (byte) (delayMs & 0xff);
		time_data[1] = (byte) ((delayMs >> 8) & 0xff);
		time_data[2] = (byte) ((delayMs >> 16) & 0xff);
		time_data[3] = (byte) ((delayMs >> 24) & 0xff);
		byte[] enc = to7BitArray(time_data);

		byte[] data = new byte[4 + enc.length + 1];
		int index = 0;
		data[index++] = START_SYSEX;
		data[index++] = SCHEDULER_DATA;
		data[index++] = SCHEDULE_FIRMATA_TASK;
		data[index++] = (byte) taskId;
		System.arraycopy(enc, 0, data, index, enc.length);
		index += enc.length;
		data[index++] = END_SYSEX;

		return data;
	}

	static byte[] createSchedulerDelayMessage(int delayMs) {
		int index = 0;

		byte[] delay_bytes = new byte[4];
		delay_bytes[index++] = (byte) (delayMs & 0xff);
		delay_bytes[index++] = (byte) ((delayMs >> 8) & 0xff);
		delay_bytes[index++] = (byte) ((delayMs >> 16) & 0xff);
		delay_bytes[index++] = (byte) ((delayMs >> 24) & 0xff);
		byte[] delay_bytes_enc = to7BitArray(delay_bytes);

		byte[] delay_command_bytes = new byte[3 + delay_bytes_enc.length + 1];
		index = 0;
		delay_command_bytes[index++] = START_SYSEX;
		delay_command_bytes[index++] = SCHEDULER_DATA;
		delay_command_bytes[index++] = DELAY_FIRMATA_TASK;
		System.arraycopy(delay_bytes_enc, 0, delay_command_bytes, index, delay_bytes_enc.length);
		index += delay_bytes_enc.length;
		delay_command_bytes[index++] = END_SYSEX;

		return delay_command_bytes;
	}

	static byte[] createDeleteTaskMessage(int taskId) {
		return new byte[] { START_SYSEX, SCHEDULER_DATA, DELETE_FIRMATA_TASK, (byte) taskId, END_SYSEX };
	}

	static byte[] createSchedulerResetMessage() {
		return new byte[] { START_SYSEX, SCHEDULER_DATA, RESET_FIRMATA_TASKS, END_SYSEX };
	}

	static byte[] to7BitArray(byte[] data) {
		int shift = 0;
		int previous = 0;
		ByteArrayOutputStream output = new ByteArrayOutputStream();

		for (byte b : data) {
			int i = b & 0xff;
			if (shift == 0) {
				output.write(i & 0x7f);
				shift++;
				previous = i >> 7;
			} else {
				output.write(((i << shift) & 0x7f) | previous);
				if (shift == 6) {
					output.write(i >> 1);
					shift = 0;
				} else {
					shift++;
					previous = i >> (8 - shift);
				}
			}
		}

		if (shift > 0) {
			output.write(previous);
		}

		return output.toByteArray();
	}

	static byte[] encodeValue(int value) {
		int num_bytes;
		if (value >= 0 && value < 128) { // 2^7
			num_bytes = 1;
		} else if (value >= 0 && value < 16384) { // 2^14
			num_bytes = 2;
		} else if (value >= 0 && value < 2097152) { // 2^21
			num_bytes = 3;
		} else if (value >= 0 && value < 268435456) { // 2^28
			num_bytes = 4;
		} else {
			// Error?
			num_bytes = 5;
		}
		byte[] bytes = new byte[num_bytes];
		for (int i = 0; i < num_bytes; i++) {
			bytes[i] = (byte) ((value >> (i * 7)) & 0x7f);
		}
		return bytes;
	}

	static byte[] from7BitArray(byte[] encoded) {
		final int expected_bytes = encoded.length * 7 >> 3;
		final byte[] decoded = new byte[expected_bytes];

		for (int i = 0; i < expected_bytes; i++) {
			final int j = i << 3;
			final int pos = (j / 7) >>> 0;
			final int shift = j % 7;
			if (pos + 1 >= encoded.length) {
				break;
			}
			decoded[i] = (byte) ((encoded[pos] >> shift) | ((encoded[pos + 1] << (7 - shift)) & 0xFF));
		}

		return decoded;
	}

	static int decodeValue(byte... values) {
		int value = 0;
		for (int i = 0; i < values.length; i++) {
			value |= ((values[i] & 0x7f) << (i * 7));
		}
		return value;
	}

	public enum PinMode {
		DIGITAL_INPUT, // 0x00
		DIGITAL_OUTPUT, // 0x01
		ANALOG_INPUT, // 0x02
		PWM, // 0x03
		SERVO, // 0x04
		SHIFT, // 0x05
		I2C, // 0x06
		ONEWIRE, // 0x07
		STEPPER, // 0x08
		ENCODER, // 0x09
		SERIAL, // 0x0A
		INPUT_PULLUP, // 0x0B
		// Extensions under development
		SPI, // 0x0C
		SONAR, // 0x0D - HC-SR04
		TONE, // 0x0E
		DHT, // 0x0F
		FREQUENCY, // 0x10 - frequency measurement
		UNKNOWN;
		// IGNORE 0x7F - pin configured to be ignored by digitalWrite and
		// capabilityResponse

		private static PinMode[] MODES = values();

		public static PinMode valueOf(int mode) {
			if (mode < 0 || mode >= MODES.length) {
				return UNKNOWN;
			}

			return MODES[mode];
		}

		public boolean isOutput() {
			return this == DIGITAL_OUTPUT || this == PWM || this == SERVO;
		}

		public boolean isDigitalInput() {
			return this == DIGITAL_INPUT || this == INPUT_PULLUP;
		}
	}

	public static class PinCapability {
		private PinMode mode;
		private int resolution;
		private int max;

		public PinCapability(PinMode mode, int resolution) {
			this.mode = mode;
			this.resolution = resolution;
			this.max = (int) Math.pow(2, resolution) - 1;
		}

		public PinMode getMode() {
			return mode;
		}

		public int getResolution() {
			return resolution;
		}

		public int getMax() {
			return max;
		}

		@Override
		public String toString() {
			return "PinCapability [mode=" + mode + ", resolution=" + resolution + ", max=" + max + "]";
		}
	}
}
