package com.diozero.devices;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     LM73.java
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

import org.tinylog.Logger;

import com.diozero.api.I2CDevice;
import com.diozero.api.I2CDeviceInterface;
import com.diozero.api.RuntimeIOException;
import com.diozero.util.BitManipulation;

public class LM73 implements ThermometerInterface {
	public static enum Configuration {
		LM73_0_ADDRESS_PIN_FLOAT(0x48), LM73_0_ADDRESS_PIN_GROUND(0x49), LM73_0_ADDRESS_PIN_VDD(0x4a),
		LM73_1_ADDRESS_PIN_FLOAT(0x4c), LM73_1_ADDRESS_PIN_GROUND(0x4d), LM73_1_ADDRESS_PIN_VDD(0x4e);

		private int address;

		private Configuration(int address) {
			this.address = address;
		}

		public int getAddress() {
			return address;
		}
	}

	// Temperature resolutions
	public static enum Resolution {
		RES_11_BIT(0b00), RES_12_BIT(0b01), RES_13_BIT(0b10), RES_14_BIT(0b11);

		private static final byte CLEAR_MASK = (byte) 0b10011111;
		private byte mask;

		private Resolution(int value) {
			this.mask = (byte) (value << 5);
		}

		public byte getMask() {
			return mask;
		}

		public byte getValue(byte currentValue) {
			return (byte) ((currentValue &= CLEAR_MASK) | mask);
		}
	}

	// Registers (p17)
	private static final int TEMPERATURE_REG = 0b000;
	private static final int CONFIG_REG = 0b001;
	private static final int UPPER_LIMIT_TEMP_REG = 0b010;
	private static final int LOWER_LIMIT_TEMP_REG = 0b011;
	private static final int CTRL_STATUS_REG = 0b100;
	private static final int ID_REG = 0b111;

	// Control/Status register bits
	private static final byte CONTROL_DATA_AVAILABLE_BIT = 0;
	private static final byte TEMP_LOW_BIT = 1;
	private static final byte TEMP_HIGH_BIT = 2;
	private static final byte ALERT_STATUS_BIT = 3;
	private static final byte TIMEOUT_DISABLE_BIT = 7;

	// private static final byte LM73_BIT_STATUS = 0x0F;

	// Configuration register bits
	private static final byte ONE_SHOT_BIT = 2;
	private static final byte ALERT_RESET_BIT = 3;
	private static final byte ALERT_POLARITY_BIT = 4;
	private static final byte ALERT_ENABLE_BIT = 5;
	private static final byte POWER_DOWN_BIT = 7;

	private static final int LM73_ID = 0x190;
	// p19 states that the reset state is 0x08
	private static final Resolution DEFAULT_RESOLUTION = Resolution.RES_11_BIT;
	private static final float RANGE = 128f;

	private I2CDeviceInterface device;
	private Configuration config;
	private Resolution resolution;

	public LM73(int controller, Configuration config) {
		this.config = config;
		resolution = DEFAULT_RESOLUTION;

		device = I2CDevice.builder(config.getAddress()).setController(controller).build();
		int id = device.readUShort(ID_REG);
		if (id != LM73_ID) {
			Logger.warn("Expected device id 0x{}, got 0x{}", Integer.toHexString(LM73_ID), Integer.toHexString(id));
		}
	}

	@Override
	public void close() {
		device.close();
	}

	public Configuration getConfiguration() {
		return config;
	}

	public Resolution getResolution() {
		return resolution;
	}

	public void setResolution(Resolution resolution) {
		byte value = device.readByteData(CTRL_STATUS_REG);
		device.writeByteData(CTRL_STATUS_REG, resolution.getValue(value));
	}

	public void setPower(boolean on) {
		device.writeBit(CONFIG_REG, POWER_DOWN_BIT, !on);
		// TODO Wait for the specified maximum conversion time
	}

	public boolean isDataAvailable() {
		return BitManipulation.isBitSet(device.readByteData(CTRL_STATUS_REG), CONTROL_DATA_AVAILABLE_BIT);
	}

	@Override
	public float getTemperature() throws RuntimeIOException {
		return device.readShort(TEMPERATURE_REG) / RANGE;
	}

	public float oneShotRead() {
		// Assumes the device is powered down
		device.writeBit(CONFIG_REG, ONE_SHOT_BIT, true);
		while (!isDataAvailable()) {
			// TODO Wait the required number of ms with a timeout
		}
		return getTemperature();
	}

	public static void main(String[] args) {
		// FIXME Remove this temperature conversion test code
		short raw;
		float expected, actual;
		float range = 128f;

		raw = 0x4b00;
		expected = 150;
		actual = raw / range;
		System.out.format("raw: 0x%02x, expected: %.5f, actual: %.5f%n", Integer.valueOf(raw & 0xffff),
				Float.valueOf(expected), Float.valueOf(actual));

		raw = 0x0c80;
		expected = 25;
		actual = raw / range;
		System.out.format("raw: 0x%02x, expected: %.5f, actual: %.5f%n", Integer.valueOf(raw & 0xffff),
				Float.valueOf(expected), Float.valueOf(actual));

		raw = 0x0080;
		expected = 1;
		actual = raw / range;
		System.out.format("raw: 0x%02x, expected: %.5f, actual: %.5f%n", Integer.valueOf(raw & 0xffff),
				Float.valueOf(expected), Float.valueOf(actual));

		raw = 0x0020;
		expected = 0.25f;
		actual = raw / range;
		System.out.format("raw: 0x%02x, expected: %.5f, actual: %.5f%n", Integer.valueOf(raw & 0xffff),
				Float.valueOf(expected), Float.valueOf(actual));

		raw = 0x0010;
		expected = 0.125f;
		actual = raw / range;
		System.out.format("raw: 0x%02x, expected: %.5f, actual: %.5f%n", Integer.valueOf(raw & 0xffff),
				Float.valueOf(expected), Float.valueOf(actual));

		raw = 0x0008;
		expected = 0.0625f;
		actual = raw / range;
		System.out.format("raw: 0x%02x, expected: %.5f, actual: %.5f%n", Integer.valueOf(raw & 0xffff),
				Float.valueOf(expected), Float.valueOf(actual));

		raw = 0x0004;
		expected = 0.03125f;
		actual = raw / range;
		System.out.format("raw: 0x%02x, expected: %.5f, actual: %.5f%n", Integer.valueOf(raw & 0xffff),
				Float.valueOf(expected), Float.valueOf(actual));

		raw = 0x0000;
		expected = 0;
		actual = raw / range;
		System.out.format("raw: 0x%02x, expected: %.5f, actual: %.5f%n", Integer.valueOf(raw & 0xffff),
				Float.valueOf(expected), Float.valueOf(actual));

		raw = (short) 0xfffc;
		expected = -0.03125f;
		actual = raw / range;
		System.out.format("raw: 0x%02x, expected: %.5f, actual: %.5f%n", Integer.valueOf(raw & 0xffff),
				Float.valueOf(expected), Float.valueOf(actual));

		raw = (short) 0xfff8;
		expected = -0.0625f;
		actual = raw / range;
		System.out.format("raw: 0x%02x, expected: %.5f, actual: %.5f%n", Integer.valueOf(raw & 0xffff),
				Float.valueOf(expected), Float.valueOf(actual));

		raw = (short) 0xfff0;
		expected = -0.125f;
		actual = raw / range;
		System.out.format("raw: 0x%02x, expected: %.5f, actual: %.5f%n", Integer.valueOf(raw & 0xffff),
				Float.valueOf(expected), Float.valueOf(actual));

		raw = (short) 0xffe0;
		expected = -0.25f;
		actual = raw / range;
		System.out.format("raw: 0x%02x, expected: %.5f, actual: %.5f%n", Integer.valueOf(raw & 0xffff),
				Float.valueOf(expected), Float.valueOf(actual));

		raw = (short) 0xff80;
		expected = -1f;
		actual = raw / range;
		System.out.format("raw: 0x%02x, expected: %.5f, actual: %.5f%n", Integer.valueOf(raw & 0xffff),
				Float.valueOf(expected), Float.valueOf(actual));

		raw = (short) 0xf380;
		expected = -25;
		actual = raw / range;
		System.out.format("raw: 0x%02x, expected: %.5f, actual: %.5f%n", Integer.valueOf(raw & 0xffff),
				Float.valueOf(expected), Float.valueOf(actual));

		raw = (short) 0xec00;
		expected = -40;
		actual = raw / range;
		System.out.format("raw: 0x%02x, expected: %.5f, actual: %.5f%n", Integer.valueOf(raw & 0xffff),
				Float.valueOf(expected), Float.valueOf(actual));
	}
}
