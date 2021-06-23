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
 * Copyright (C) 2016 - 2021 diozero
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
	byte EXTENDED_ID = 0x00; // A value of 0x00 indicates the next 2 bytes define the extended ID
	// IDs 0x00 - 0x0F are reserved for user defined commands
	byte SERIAL_DATA = 0x60; // communicate with serial devices, including other boards
	byte ENCODER_DATA = 0x61; // reply with encoders current positions
	byte SERVO_CONFIG = 0x70; // set max angle, minPulse, maxPulse, freq
	byte STRING_DATA = 0x71; // a string message with 14-bits per char
	byte STEPPER_DATA = 0x72; // control a stepper motor
	byte ONEWIRE_DATA = 0x73; // send an OneWire read/write/reset/select/skip/search request
	byte SHIFT_DATA = 0x75; // a bitstream to/from a shift register
	byte I2C_REQUEST = (byte) 0x76; // send an I2C read/write request
	byte I2C_REPLY = (byte) 0x77; // a reply to an I2C read request
	byte I2C_CONFIG = (byte) 0x78; // config I2C settings such as delay times and power pins
	byte REPORT_FIRMWARE = 0x79; // report name and version of the firmware
	byte EXTENDED_ANALOG = 0x6F; // analog write (PWM, Servo, etc) to any pin
	byte PIN_STATE_QUERY = 0x6D; // ask for a pin's current mode and state (different than value)
	byte PIN_STATE_RESPONSE = 0x6E; // reply with a pin's current mode and state (different than value)
	byte CAPABILITY_QUERY = 0x6B; // ask for supported modes and resolution of all pins
	byte CAPABILITY_RESPONSE = 0x6C; // reply with supported modes and resolution
	byte ANALOG_MAPPING_QUERY = 0x69; // ask for mapping of analog to pin numbers
	byte ANALOG_MAPPING_RESPONSE = 0x6A; // reply with mapping info
	byte SAMPLING_INTERVAL = 0x7A; // the interval at which analog input is sampled (default = 19ms)
	byte SCHEDULER_DATA = 0x7B; // send a createtask/deletetask/addtotask/schedule/querytasks/querytask request
								// to the scheduler
	byte SYSEX_NON_REALTIME = 0x7E; // MIDI Reserved for non-realtime messages
	byte SYSEX_REALTIME = 0X7F; // MIDI Reserved for realtime messages

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
		UNKNOWN;

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
