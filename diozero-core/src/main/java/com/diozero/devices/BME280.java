package com.diozero.devices;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     BME280.java
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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.tinylog.Logger;

import com.diozero.api.I2CConstants;
import com.diozero.api.I2CDevice;
import com.diozero.api.RuntimeIOException;
import com.diozero.api.SpiClockMode;
import com.diozero.api.SpiConstants;
import com.diozero.api.SpiDevice;
import com.diozero.util.SleepUtil;

/*-
 * Datasheet: https://cdn-shop.adafruit.com/datasheets/BST-BME280_DS001-10.pdf
 * Reference implementation: https://github.com/BoschSensortec/BME280_driver
 *
 * Sample implementations:
 * Java: https://github.com/ControlEverythingCommunity/BME280/blob/master/Java/BME280.java
 * Adafruit / Python: https://github.com/adafruit/Adafruit_BME280_Library/blob/master/Adafruit_BME280.cpp
 *
 * SPI Wiring:
 * SDO  | CSB | SDA/SDI | SCK / SCL | GND | VCC
 * MISO | CS  |  MOSI   |   SCLK    |
 *
 * I2C Wiring:
 * SDO  | CSB | SDA/SDI | SCK / SCL | GND | VCC
 *      |     |   SDA   |    SCL    |
 */

/**
 * Provides access to the Bosch BMx280 pressure and temperature sensor. The
 * BME280 includes an additional humidity sensor. Different constructors support
 * access via I2C or SPI.
 *
 * All constructors configure the device as follows:
 * <ul>
 * <li>Temperature oversampling: x1</li>
 * <li>Pressure oversampling: x1</li>
 * <li>Temperature oversampling: x1</li>
 * <li>Operating mode: Normal</li>
 * <li>Standby inactive duration: 1 second</li>
 * <li>IIR filter coefficient: Off</li>
 * </ul>
 *
 * @author gregflurry
 * @author mattjlewis
 */
public class BME280 implements BarometerInterface, ThermometerInterface, HygrometerInterface {
	public enum Model {
		BMP280(0x58), BME280(0x60);

		private int deviceId;

		Model(int deviceId) {
			this.deviceId = deviceId;
		}

		public int getDeviceId() {
			return deviceId;
		}
	}

	public static final int DEFAULT_I2C_ADDRESS = 0x76;

	private static final int CALIB_00_REG = 0x88;
	private static final int ID_REG = 0xD0;
	private static final int RESET_REG = 0xE0;
	private static final int CALIB_26_REG = 0xe1;
	private static final int CTRL_HUM_REG = 0xF2;
	private static final int STATUS_REG = 0xF3;
	private static final int CTRL_MEAS_REG = 0xF4;
	private static final int CONFIG_REG = 0xF5;
	private static final int PRESS_MSB_REG = 0xF7;

	// Flags for ctrl_hum and ctrl_meas registers
	private static final byte OVERSAMPLING_1_MASK = 0b001;
	private static final byte OVERSAMPLING_2_MASK = 0b010;
	private static final byte OVERSAMPLING_4_MASK = 0b011;
	private static final byte OVERSAMPLING_8_MASK = 0b100;
	private static final byte OVERSAMPLING_16_MASK = 0b101;

	/**
	 * Humidity oversampling multiplier; value can be OVERSAMPLING_1, _2, _4, _8,
	 * _16.
	 */
	public enum HumidityOversampling {
		OVERSAMPLING_1(OVERSAMPLING_1_MASK), OVERSAMPLING_2(OVERSAMPLING_2_MASK), OVERSAMPLING_4(OVERSAMPLING_4_MASK),
		OVERSAMPLING_8(OVERSAMPLING_8_MASK), OVERSAMPLING_16(OVERSAMPLING_16_MASK);

		private byte mask;

		HumidityOversampling(byte mask) {
			this.mask = mask;
		}

		public byte getMask() {
			return mask;
		}
	}

	/**
	 * Temperature oversampling multiplier; value can be OVERSAMPLING_1, _2, _4, _8,
	 * _16.
	 */
	public enum TemperatureOversampling {
		OVERSAMPLING_1(OVERSAMPLING_1_MASK), OVERSAMPLING_2(OVERSAMPLING_2_MASK), OVERSAMPLING_4(OVERSAMPLING_4_MASK),
		OVERSAMPLING_8(OVERSAMPLING_8_MASK), OVERSAMPLING_16(OVERSAMPLING_16_MASK);

		private byte mask;

		TemperatureOversampling(byte mask) {
			this.mask = (byte) (mask << 5);
		}

		public byte getMask() {
			return mask;
		}
	}

	/**
	 * Pressure oversampling multiplier; value can be OVERSAMPLING_1, _2, _4, _8,
	 * _16.
	 */
	public enum PressureOversampling {
		OVERSAMPLING_1(OVERSAMPLING_1_MASK), OVERSAMPLING_2(OVERSAMPLING_2_MASK), OVERSAMPLING_4(OVERSAMPLING_4_MASK),
		OVERSAMPLING_8(OVERSAMPLING_8_MASK), OVERSAMPLING_16(OVERSAMPLING_16_MASK);

		private byte mask;

		PressureOversampling(byte mask) {
			this.mask = (byte) (mask << 2);
		}

		public byte getMask() {
			return mask;
		}
	}

	/**
	 * Operating mode; value can be MODE_SLEEP, _FORCED, or _NORMAL.
	 */
	public enum OperatingMode {
		MODE_SLEEP(0b00), MODE_FORCED(0b01), MODE_NORMAL(0b11);

		private byte mask;

		OperatingMode(int mask) {
			this.mask = (byte) mask;
		}

		public byte getMask() {
			return mask;
		}
	}

	/**
	 * Inactive duration in standby mode; can be STANDBY_
	 * <ul>
	 * <li>500_US (0.5 ms)</li>
	 * <li>62_5_MS (62.5 ms)</li>
	 * <li>125_MS (125 ms)</li>
	 * <li>250_MS (250 ms)</li>
	 * <li>500_MS (500 ms)</li>
	 * <li>1_S (1 second)</li>
	 * <li>10_MS (10 ms)</li>
	 * <li>20_MS (20 ms)</li>
	 * </ul>
	 */
	public enum StandbyDuration {
		STANDBY_500_US(0b000), STANDBY_62_5_MS(0b001), STANDBY_125_MS(0b010), STANDBY_250_MS(0b011),
		STANDBY_500_MS(0b100), STANDBY_1_S(0b101), STANDBY_10_MS(0b110), STANDBY_20_MS(0b111);

		private byte mask;

		StandbyDuration(int mask) {
			this.mask = (byte) (mask << 5);
		}

		public byte getMask() {
			return mask;
		}
	}

	/**
	 * IIR Filter coefficient; can be FILTER_OFF, _2, _4, _8, _16.
	 */
	public enum FilterCoefficient {
		FILTER_OFF(0b000), FILTER_2(0b001), FILTER_4(0b010), FILTER_8(0b011), FILTER_16(0b100);

		private byte mask;

		FilterCoefficient(int mask) {
			this.mask = (byte) (mask << 2);
		}

		public byte getMask() {
			return mask;
		}
	}

	private I2CDevice deviceI;
	private SpiDevice deviceS;
	private Model model;
	private boolean useI2C;
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

	/**
	 * Creates an instance that uses I2C. Assumes I2C bus 1 and the default I2C
	 * address (0x76); all other I2C instantiation parameters are set to the
	 * default.
	 *
	 * @throws RuntimeIOException if instance cannot be created
	 */
	public BME280() throws RuntimeIOException {
		this(I2CConstants.CONTROLLER_1, DEFAULT_I2C_ADDRESS);
	}

	/**
	 * Creates an instance that uses I2C. The caller must provide the I2C bus number
	 * and the I2C address; all other I2C instantiation parameters are set to the
	 * default.
	 *
	 * @param bus     I2C bus number
	 * @param address device address
	 * @throws RuntimeIOException if instance cannot be created
	 */
	public BME280(int bus, int address) throws RuntimeIOException {
		useI2C = true;

		deviceI = I2CDevice.builder(address).setController(bus).setByteOrder(ByteOrder.LITTLE_ENDIAN).build();

		setUp280();
	}

	/**
	 * Creates an instance that uses SPI. The caller must provide the chip select
	 * line; all other SPI instantiation parameters are set to the default.
	 *
	 * @param chipSelect the chip select line used
	 * @throws RuntimeIOException if instance cannot be created
	 */
	public BME280(int chipSelect) throws RuntimeIOException {
		this(SpiConstants.DEFAULT_SPI_CONTROLLER, chipSelect, SpiConstants.DEFAULT_SPI_CLOCK_FREQUENCY,
				SpiConstants.DEFAULT_SPI_CLOCK_MODE);
	}

	/**
	 * Creates an instance that uses SPI. The caller must provide all SPI
	 * instantiation parameters.
	 *
	 * @param controller the SPI controller used
	 * @param chipSelect the chip select line used
	 * @param frequency  the frequency used
	 * @param mode       the clock mode used
	 * @throws RuntimeIOException if an error occurs
	 */
	public BME280(int controller, int chipSelect, int frequency, SpiClockMode mode) throws RuntimeIOException {
		useI2C = false;

		deviceS = SpiDevice.builder(chipSelect).setController(controller).setFrequency(frequency).setClockMode(mode)
				.build();

		setUp280();
	}

	private void setUp280() {
		readDeviceModel();
		readCoefficients();

		setOperatingModes(TemperatureOversampling.OVERSAMPLING_1, PressureOversampling.OVERSAMPLING_1,
				HumidityOversampling.OVERSAMPLING_1, OperatingMode.MODE_NORMAL);
		setStandbyAndFilterModes(StandbyDuration.STANDBY_1_S, FilterCoefficient.FILTER_OFF);
	}

	private void readCoefficients() {
		// Wait for NVM to be copied
		while ((readByte(STATUS_REG) & 0x01) != 0) {
			SleepUtil.sleepMillis(10);
		}

		// Read 26 bytes of data from address 0x88(136)
		ByteBuffer buffer = readByteBlock(CALIB_00_REG, model == Model.BMP280 ? 24 : 26);

		// Temperature coefficients
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

		if (model == Model.BME280) {
			// Skip 1 byte
			buffer.get();
			// Read 1 byte of data from address 0xA1(161)
			digH1 = (short) (buffer.get() & 0xff);

			// Read 7 bytes of data from address 0xE1(225)
			// buffer = device.readI2CBlockDataByteBuffer(CALIB_26_REG, 7);
			buffer = readByteBlock(CALIB_26_REG, 7);

			// Humidity coefficients
			digH2 = buffer.getShort();
			digH3 = buffer.get() & 0xff;
			byte b1_3 = buffer.get();
			byte b1_4 = buffer.get();
			digH4 = (b1_3 << 4) | (b1_4 & 0xF);
			digH5 = ((b1_4 & 0xF0) >> 4) | (buffer.get() << 4);
			digH6 = buffer.get();
		}
	}

	/**
	 * Sets the oversampling multipliers and operating mode.
	 *
	 * @param tempOversampling  oversampling multiplier for temperature
	 * @param pressOversampling oversampling multiplier for pressure
	 * @param humOversampling   oversampling multiplier for humidity
	 * @param operatingMode     operating mode
	 */
	public void setOperatingModes(TemperatureOversampling tempOversampling, PressureOversampling pressOversampling,
			HumidityOversampling humOversampling, OperatingMode operatingMode) {
		// You must set CTRL_MEAS_REG after setting the CTRL_HUM_REG register, otherwise
		// the values won't be applied (see DS 5.4.3)
		if (model == Model.BME280) {
			// Humidity over sampling rate = 1
			writeByte(CTRL_HUM_REG, humOversampling.getMask());
		}
		// Normal mode, temp and pressure oversampling rate = 1
		writeByte(CTRL_MEAS_REG,
				(byte) (tempOversampling.getMask() | pressOversampling.getMask() | operatingMode.getMask()));
	}

	/**
	 * Sets the standby duration for normal mode and the IIR filter coefficient.
	 *
	 * @param standbyDuration   standby duration
	 * @param filterCoefficient IIR filter coefficient
	 */
	public void setStandbyAndFilterModes(StandbyDuration standbyDuration, FilterCoefficient filterCoefficient) {
		// Stand_by time = 1000 ms, filter off
		writeByte(CONFIG_REG, (byte) (standbyDuration.getMask() | filterCoefficient.getMask()));
	}

	/**
	 * Waits for data to become available.
	 *
	 * @param interval     sleep interval
	 * @param maxIntervals maximum number of intervals to wait
	 * @return true if data available, false if not
	 */
	public boolean waitDataAvailable(int interval, int maxIntervals) {
		for (int i = 0; i < maxIntervals; i++) {
			// check data ready
			if (isDataAvailable()) {
				return true;
			}

			SleepUtil.sleepMillis(interval);
		}
		return false;
	}

	/**
	 * Indicates if data is available.
	 *
	 * @return rue if data available, false if not
	 */
	public boolean isDataAvailable() {
		return (readByte(STATUS_REG) & 0x08) == 0;
	}

	/**
	 * Reads the temperature, pressure, and humidity registers; compensates the raw
	 * values to provide meaningful results.
	 *
	 * @return array in order of: temperature, pressure, humidity
	 */
	public float[] getValues() {
		// Read the pressure, temperature, and humidity registers
		ByteBuffer buffer = readByteBlock(PRESS_MSB_REG, model == Model.BMP280 ? 6 : 8);

		// Unpack the raw 20-bit unsigned pressure value
		int adc_p = ((buffer.get() & 0xff) << 12) | ((buffer.get() & 0xff) << 4) | ((buffer.get() & 0xf0) >> 4);
		// Unpack the raw 20-bit unsigned temperature value
		int adc_t = ((buffer.get() & 0xff) << 12) | ((buffer.get() & 0xff) << 4) | ((buffer.get() & 0xf0) >> 4);

		int adc_h = 0;
		if (model == Model.BME280) {
			// Unpack the raw 16-bit unsigned humidity value
			adc_h = ((buffer.get() & 0xff) << 8) | (buffer.get() & 0xff);
		}

		Logger.debug("adc_p={}, adc_t={}, adc_h={}", Integer.valueOf(adc_p), Integer.valueOf(adc_t),
				Integer.valueOf(adc_h));

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

		long humidity = 0;
		if (model == Model.BME280) {
			int v_x1_u32r = t_fine - 76800;
			v_x1_u32r = ((((adc_h << 14) - (digH4 << 20) - (digH5 * v_x1_u32r)) + 16384) >> 15)
					* (((((((v_x1_u32r * digH6) >> 10) * (((v_x1_u32r * digH3) >> 11) + 32768)) >> 10) + 2097152)
							* digH2 + 8192) >> 14);
			v_x1_u32r = v_x1_u32r - (((((v_x1_u32r >> 15) * (v_x1_u32r >> 15)) >> 7) * digH1) >> 4);
			v_x1_u32r = v_x1_u32r < 0 ? 0 : v_x1_u32r;
			v_x1_u32r = v_x1_u32r > 419430400 ? 419430400 : v_x1_u32r;
			humidity = ((long) v_x1_u32r) >> 12;
		}

		return new float[] { temp / 100f, pressure / 25600f, humidity / 1024f };
	}

	/**
	 * Reads the temperature, pressure, and humidity registers; compensates the raw
	 * values to provide meaningful results.
	 *
	 * @return temperature
	 */
	@Override
	public float getTemperature() {
		return getValues()[0];
	}

	/**
	 * Reads the temperature, pressure, and humidity registers; compensates the raw
	 * values to provide meaningful results.
	 *
	 * @return pressure in hectoPascals (hPa)
	 */
	@Override
	public float getPressure() {
		return getValues()[1];
	}

	/**
	 * Reads the temperature, pressure, and humidity registers; compensates the raw
	 * values to provide meaningful results.
	 *
	 * @return humidity
	 */
	@Override
	public float getRelativeHumidity() {
		return getValues()[2];
	}

	/**
	 * Closes the device
	 *
	 * @throws RuntimeIOException if close fails
	 */
	@Override
	public void close() throws RuntimeIOException {
		if (useI2C) {
			deviceI.close();
		} else {
			deviceS.close();
		}
	}

	/**
	 * Resets the device.
	 */
	public void reset() {
		writeByte(RESET_REG, (byte) 0xB6);
	}

	private byte readByte(int register) {
		if (useI2C) {
			return deviceI.readByteData(register);
		}

		byte[] tx = { (byte) (register | 0x80), 0 };

		byte[] ret = deviceS.writeAndRead(tx);

		return ret[1];
	}

	private void writeByte(int register, byte value) {
		if (useI2C) {
			deviceI.writeByteData(register, value);
		} else {
			byte[] tx = new byte[2];
			tx[0] = (byte) (register & 0x7f); // MSB must be 0
			tx[1] = value;

			deviceS.write(tx);
		}
	}

	private ByteBuffer readByteBlock(int register, int length) {
		byte[] data = new byte[length];

		if (useI2C) {
			deviceI.readI2CBlockData(register, data);
		} else {
			byte[] tx = new byte[length + 1];

			tx[0] = (byte) (register | 0x80);
			/* NOTE: rest of array initialized to 0 */

			byte[] ret = deviceS.writeAndRead(tx);

			System.arraycopy(ret, 1, data, 0, length);
		}

		ByteBuffer buffer = ByteBuffer.wrap(data);
		buffer.order(ByteOrder.LITTLE_ENDIAN);

		return buffer;
	}

	private void readDeviceModel() throws RuntimeIOException {
		// Detect the device model by reading the ID register
		int id = readByte(ID_REG);
		if (id == Model.BMP280.getDeviceId()) {
			model = Model.BMP280;
		} else if (id == Model.BME280.getDeviceId()) {
			model = Model.BME280;
		} else {
			throw new RuntimeIOException("Unexpected device id: " + id);
		}
	}
}
