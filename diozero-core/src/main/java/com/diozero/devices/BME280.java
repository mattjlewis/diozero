package com.diozero.devices;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     BME280.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
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


import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.diozero.api.BarometerInterface;
import com.diozero.api.HygrometerInterface;
import com.diozero.api.I2CConstants;
import com.diozero.api.I2CDevice;
import com.diozero.api.ThermometerInterface;
import com.diozero.util.RuntimeIOException;

/**
 * <a href="https://cdn-shop.adafruit.com/datasheets/BST-BME280_DS001-10.pdf">Datasheet</a>
 * Sample implementations:
 * <a href="https://github.com/ControlEverythingCommunity/BME280/blob/master/Java/BME280.java">Java</a>
 * <a href="https://github.com/adafruit/Adafruit_BME280_Library/blob/master/">Adafruit / Python</a>
 * Adafruit_BME280.cpp
 */
public class BME280 implements BarometerInterface, ThermometerInterface, HygrometerInterface {
	private static final int DEFAULT_ADDRESS = 0x76;
	
	private static final int CALIB_00_REG = 0x88;
	private static final int CALIB_26_REG = 0xe1;
	private static final int CTRL_HUM_REG = 0xF2;
	//private static final int STATUS_REG = 0xF3;
	private static final int CTRL_MEAS_REG = 0xF4;
	private static final int CONFIG_REG = 0xF5;
	private static final int PRESS_MSB_REG = 0xF7;
	
	// Flags for ctrl_hum and ctrl_meas registers
	private static final byte OVERSAMPLING_1_MASK = 0b001;
	private static final byte OVERSAMPLING_2_MASK = 0b010;
	private static final byte OVERSAMPLING_4_MASK = 0b011;
	private static final byte OVERSAMPLING_8_MASK = 0b100;
	private static final byte OVERSAMPLING_16_MASK = 0b101;
	public static enum HumidityOversampling {
		OVERSAMPLING_1(OVERSAMPLING_1_MASK), OVERSAMPLING_2(OVERSAMPLING_2_MASK),
		OVERSAMPLING_4(OVERSAMPLING_4_MASK), OVERSAMPLING_8(OVERSAMPLING_8_MASK), OVERSAMPLING_16(OVERSAMPLING_16_MASK);
		
		private byte mask;
		
		private HumidityOversampling(byte mask) {
			this.mask = mask;
		}
		
		public byte getMask() {
			return mask;
		}
	}
	public static enum TemperatureOversampling {
		OVERSAMPLING_1(OVERSAMPLING_1_MASK), OVERSAMPLING_2(OVERSAMPLING_2_MASK),
		OVERSAMPLING_4(OVERSAMPLING_4_MASK), OVERSAMPLING_8(OVERSAMPLING_8_MASK), OVERSAMPLING_16(OVERSAMPLING_16_MASK);
		
		private byte mask;
		
		private TemperatureOversampling(byte mask) {
			this.mask = (byte) (mask << 5);
		}
		
		public byte getMask() {
			return mask;
		}
	}
	public static enum PressureOversampling {
		OVERSAMPLING_1(OVERSAMPLING_1_MASK), OVERSAMPLING_2(OVERSAMPLING_2_MASK),
		OVERSAMPLING_4(OVERSAMPLING_4_MASK), OVERSAMPLING_8(OVERSAMPLING_8_MASK), OVERSAMPLING_16(OVERSAMPLING_16_MASK);
		
		private byte mask;
		
		private PressureOversampling(byte mask) {
			this.mask = (byte) (mask << 2);
		}
		
		public byte getMask() {
			return mask;
		}
	}
	public static enum OperatingMode {
		MODE_SLEEP(0b00), MODE_FORCED(0b01), MODE_NORMAL(0b11);
		
		private byte mask;
		
		private OperatingMode(int mask) {
			this.mask = (byte) mask;
		}
		
		public byte getMask() {
			return mask;
		}
	}
	// Flags for config register
	public static enum StandbyMode {
		STANDBY_500_US(0b000), STANDBY_62_5_MS(0b001), STANDBY_125_MS(0b010), STANDBY_250_MS(0b011),
		STANDBY_500_MS(0b100), STANDBY_1_S(0b101), STANDBY_10_MS(0b110), STANDBY_20_MS(0b111);
		
		private byte mask;
		
		private StandbyMode(int mask) {
			this.mask = (byte) (mask << 5);
		}
		
		public byte getMask() {
			return mask;
		}
	}
	public static enum FilterMode {
		FILTER_OFF(0b000), FILTER_2(0b001), FILTER_4(0b010), FILTER_8(0b011), FILTER_16(0b100);
		
		private byte mask;
		
		private FilterMode(int mask) {
			this.mask = (byte) (mask << 2);
		}
		
		public byte getMask() {
			return mask;
		}
	}
	
	private I2CDevice device;
	private int digT1;
	private short digT2;
	private short digT3;
	private int digP1;
	private short digP2;
	private short digP3;
	private short digP4;
	private short digP5;
	private short digP6;
	private short digP7;
	private short digP8;
	private short digP9;
	private short digH1;
	private short digH2;
	private int digH3;
	private int digH4;
	private int digH5;
	private byte digH6;

	public BME280() {
		this(I2CConstants.BUS_1, DEFAULT_ADDRESS);
	}
	
	public BME280(int bus, int address) {
		device = new I2CDevice(bus, address, I2CConstants.ADDR_SIZE_7,
				I2CConstants.DEFAULT_CLOCK_FREQUENCY, ByteOrder.LITTLE_ENDIAN);
		
		readCoefficients();
		setOperatingModes(TemperatureOversampling.OVERSAMPLING_1,
				PressureOversampling.OVERSAMPLING_1, HumidityOversampling.OVERSAMPLING_1,
				OperatingMode.MODE_NORMAL);
		setStandbyAndFilterModes(StandbyMode.STANDBY_1_S, FilterMode.FILTER_OFF);
	}

	private void readCoefficients() {
		// Read 24 bytes of data from address 0x88(136)
		ByteBuffer buffer = device.read(CALIB_00_REG, 26);

		// Temp coefficients
		digT1 = buffer.getShort() & 0xffff;
		digT2 = buffer.getShort();
		digT3 = buffer.getShort();

		// Pressure coefficients
		digP1 = buffer.getShort() & 0xffff;
		digP2 = buffer.getShort();
		digP3 = buffer.getShort();
		digP4 = buffer.getShort();
		digP5 = buffer.getShort();
		digP6 = buffer.getShort();
		digP7 = buffer.getShort();
		digP8 = buffer.getShort();
		digP9 = buffer.getShort();
		// Skip 1 byte
		buffer.get();
		// Read 1 byte of data from address 0xA1(161)
		digH1 = (short) (buffer.get() & 0xff);

		// Read 7 bytes of data from address 0xE1(225)
		buffer = device.read(CALIB_26_REG, 7);

		// Humidity coefficients
		digH2 = buffer.getShort();
		digH3 = buffer.get() & 0xff;
		byte b1_3 = buffer.get();
		byte b1_4 = buffer.get();
		digH4 = (b1_3 << 4) | (b1_4 & 0xF);
		digH5 = ((b1_4 & 0xF0) >> 4) | (buffer.get() << 4);
		digH6 = buffer.get();
	}
	
	public void setOperatingModes(TemperatureOversampling tempOversampling,
			PressureOversampling pressOversampling, HumidityOversampling humOversampling,
			OperatingMode operatingMode) {
		// Humidity over sampling rate = 1
		device.writeByte(CTRL_HUM_REG, humOversampling.getMask());
		// Normal mode, temp and pressure oversampling rate = 1
		device.writeByte(CTRL_MEAS_REG, (byte) (tempOversampling.getMask() |
				pressOversampling.getMask() | operatingMode.getMask()));
	}

	public void setStandbyAndFilterModes(StandbyMode standbyMode, FilterMode filterMode) {
		// Stand_by time = 1000 ms, filter off
		device.writeByte(CONFIG_REG, (byte) (standbyMode.getMask() | filterMode.getMask()));
	}
	
	public float[] getValues() {
		// Read the 3 pressure registers
		ByteBuffer buffer = device.read(PRESS_MSB_REG, 8);

		// Unpack the raw 20-bit unsigned pressure value
		int adc_p = ((buffer.get() & 0xff) << 12) |
				((buffer.get() & 0xff) << 4) |
				((buffer.get() & 0xf0) >> 4);
		// Unpack the raw 20-bit unsigned temperature value
		int adc_t = ((buffer.get() & 0xff) << 12) |
				((buffer.get() & 0xff) << 4) |
				((buffer.get() & 0xf0) >> 4);
		// Unpack the raw 16-bit unsigned humidity value
		int adc_h = ((buffer.get() & 0xff) << 8) | (buffer.get() & 0xff);
		
		System.out.println("adc_p=" + adc_p + ", adc_t=" + adc_t + ", adc_h=" + adc_h);
		
		int tvar1 = (((adc_t >> 3) - (digT1 << 1)) * digT2) >> 11;
		int tvar2 = (((((adc_t >> 4) - digT1) * ((adc_t >> 4) - digT1)) >> 12) * digT3) >> 14;
		int t_fine = tvar1 + tvar2;
		
		int temp = (t_fine * 5 + 128) >> 8;
		
		long pvar1 = ((long) t_fine) - 128000;
		long pvar2 = pvar1 * pvar1 * digP6;
		pvar2 = pvar2 + ((pvar1 * digP5) << 17);
		pvar2 = pvar2 + (((long) digP4) << 35);
		pvar1 = ((pvar1 * pvar1 * digP3) >> 8) + ((pvar1 * digP2) << 12);
		pvar1 = (((((long) 1) << 47) + pvar1)) * digP1 >> 33;
		long pressure;
		if (pvar1 == 0) {
			pressure = 0; // Avoid exception caused by division by zero
		} else {
			pressure = 1048576 - adc_p;
			pressure = (((pressure << 31) - pvar2) * 3125) / pvar1;
			pvar1 = (digP9 * (pressure >> 13) * (pressure >> 13)) >> 25;
			pvar2 = (digP8 * pressure) >> 19;
			pressure = ((pressure + pvar1 + pvar2) >> 8) + (((long) digP7) << 4);
		}
		
		int v_x1_u32r = t_fine - 76800;
		v_x1_u32r = ((((adc_h << 14) - (digH4 << 20) - (digH5 * v_x1_u32r)) + 16384) >> 15) *
				(((((((v_x1_u32r * digH6) >> 10) * (((v_x1_u32r * digH3) >> 11) + 32768)) >> 10) + 2097152) * digH2 + 8192) >> 14);
		v_x1_u32r = v_x1_u32r - (((((v_x1_u32r >> 15) * (v_x1_u32r >> 15)) >> 7) * digH1) >> 4);
		v_x1_u32r = v_x1_u32r < 0 ? 0 : v_x1_u32r;
		v_x1_u32r = v_x1_u32r > 419430400 ? 419430400 : v_x1_u32r;
		long humidity = ((long) v_x1_u32r) >> 12;
		
		return new float[] { temp / 100.0f, pressure / 2560.0f, humidity / 1024.0f };
	}
	
	@Override
	public float getTemperature() {
		return getValues()[0];
	}
	
	@Override
	public float getPressure() {
		return getValues()[1];
	}
	
	@Override
	public float getRelativeHumidity() {
		return getValues()[2];
	}

	@Override
	public void close() throws RuntimeIOException {
		device.close();
	}
}
