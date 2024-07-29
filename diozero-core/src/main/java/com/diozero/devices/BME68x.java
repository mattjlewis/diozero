package com.diozero.devices;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     BME68x.java
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

import com.diozero.api.I2CConstants;
import com.diozero.api.I2CDevice;
import com.diozero.api.I2CDeviceInterface;
import com.diozero.util.BitManipulation;
import com.diozero.util.SleepUtil;

/*-
 *
 * https://www.bosch-sensortec.com/media/boschsensortec/downloads/datasheets/bst-bme680-ds001.pdf
 * https://www.bosch-sensortec.com/media/boschsensortec/downloads/datasheets/bst-bme688-ds000.pdf
 * https://github.com/BoschSensortec/BME68x-Sensor-API
 * https://github.com/pimoroni/bme680-python
 * https://github.com/knobtviker/bme680
 */
public class BME68x implements BarometerInterface, ThermometerInterface, AirQualitySensorInterface {
	// Chip vendor for the BME680
	private static final String CHIP_VENDOR = "Bosch";
	// Chip name for the BME680
	private static final String CHIP_NAME = "BME680";
	// Chip ID for the BME680
	private static final int CHIP_ID_BME680 = 0x61;
	// Variant ID for BME680
	public static final int VARIANT_ID_BM680 = 0x00;
	// Variant ID for BME688
	public static final int VARIANT_ID_BM688 = 0x01;

	public static int MIN_TEMPERATURE_CELSIUS = 0;
	public static int MAX_TEMPERATURE_CELSIUS = 60;
	public static int MIN_PRESSURE_HPA = 900;
	public static int MAX_PRESSURE_HPA = 1100;
	public static int MIN_HUMIDITY_PERCENT = 20;
	public static int MAX_HUMIDITY_PERCENT = 80;

	// Default I2C address for the sensor.
	public static final int DEVICE_ADDRESS = 0x76;
	// Alternative I2C address for the sensor.
	public static final int ALT_DEVICE_ADDRESS = 0x77;

	/**
	 * Operating mode. BME680 only supports SLEEP &amp; FORCED.
	 *
	 * Note SEQUENTIAL mode is not described in the datasheet.
	 *
	 * https://community.bosch-sensortec.com/t5/MEMS-sensors-forum/BME680-Gas-sensor-heater-unstable/m-p/23342#M6788
	 * "The working principle is you configure all heat parameters first, then set
	 * sensor into sequential mode, then sensor will start measurement like "T,P,H,
	 * GAS" sequence. and go through the heat parameters table one after the other.
	 * After one test period, you can read out value from data register."
	 */
	public enum OperatingMode {
		SLEEP(0b00), FORCED(0b01), PARALLEL(0b10), SEQUENTIAL(0b11);

		private byte value;

		OperatingMode(int value) {
			this.value = (byte) value;
		}

		byte getValue() {
			return value;
		}

		static OperatingMode valueOf(int value) {
			if (value < 0 || value > SEQUENTIAL.getValue()) {
				throw new IllegalArgumentException("Invalid OperatingMode value " + value);
			}

			return values()[value];
		}
	}

	/**
	 * After power-on, spi_mem_page is in its reset state and page 0 (0x80 to 0xff)
	 * will be active. Page 1 is (0x00 to 0x7f).
	 */
	enum SpiMemoryPage {
		PAGE_0(0), PAGE_1(1);

		private byte page;

		SpiMemoryPage(int page) {
			this.page = (byte) page;
		}

		byte getPage() {
			return page;
		}
	}

	/**
	 * Oversampling multiplier.
	 *
	 * A higher oversampling value means more stable sensor readings, with less
	 * noise and jitter. However each step of oversampling adds about 2ms to the
	 * latency, causing a slower response time to fast transients.
	 */
	public enum OversamplingMultiplier {
		NONE(0b000, 0), X1(0b001, 1), X2(0b010, 2), X4(0b011, 4), X8(0b100, 8), X16(0b101, 16);

		private byte value;
		private int cycles;

		OversamplingMultiplier(int value, int cycles) {
			this.value = (byte) value;
			this.cycles = cycles;
		}

		byte getValue() {
			return value;
		}

		public int getCycles() {
			return cycles;
		}

		static OversamplingMultiplier valueOf(int value) {
			if (value < 0 || value > X16.getValue()) {
				throw new IllegalArgumentException("Invalid OversamplingMultiplier value " + value);
			}

			return values()[value];
		}
	}

	/**
	 * Infinite Impulse Response (IIR) filter.
	 *
	 * Optionally remove short term fluctuations from the temperature and pressure
	 * readings, increasing their resolution but reducing their bandwidth. Enabling
	 * the IIR filter does not slow down the time a reading takes, but will slow
	 * down the BME680s response to changes in temperature and pressure. When the
	 * IIR filter is enabled, the temperature and pressure resolution is effectively
	 * 20bit. When it is disabled, it is 16bit + oversampling-1 bits.
	 */
	public enum IirFilterCoefficient {
		NONE(0b000, 0), _1(0b001, 1), _3(0b010, 3), _7(0b011, 7), _15(0b100, 15), _31(0b101, 31), _63(0b110, 63),
		_127(0b111, 127);

		private byte value;
		private int coefficient;

		IirFilterCoefficient(int value, int size) {
			this.value = (byte) value;
			this.coefficient = size;
		}

		byte getValue() {
			return value;
		}

		public int getCoefficient() {
			return coefficient;
		}

		static IirFilterCoefficient valueOf(int value) {
			if (value < 0 || value > _127.getValue()) {
				throw new IllegalArgumentException("Invalid IirFilterCoefficient value " + value);
			}

			return values()[value];
		}
	}

	/**
	 * Gas heater profile.
	 */
	public enum HeaterProfile {
		_0(0b0000), _1(0b0001), _2(0b0010), _3(0b0011), _4(0b0100), _5(0b0101), _6(0b0110), _7(0b0111), _8(0b1000),
		_9(0b1001);

		private byte value;

		HeaterProfile(int value) {
			this.value = (byte) value;
		}

		byte getValue() {
			return value;
		}

		static HeaterProfile valueOf(int value) {
			if (value < 0 || value > _9.getValue()) {
				throw new IllegalArgumentException("Invalid HeaterProfile value " + value);
			}

			return values()[value];
		}
	}

	/**
	 * Operating (Output?) Data Rate (ODR) - standby time.
	 *
	 * Standby time between sequential mode measurement profiles.
	 *
	 * Not documented in the datasheet, ref:
	 * https://github.com/BoschSensortec/BME68x-Sensor-API/blob/master/bme68x_defs.h#L265
	 */
	public enum StandbyDuration {
		_0_59_MS(0b0000, 0.59f), _62_5_MS(0b0001, 62.5f), _125_MS(0b0010, 125f), _250_MS(0b0011, 250f),
		_500_MS(0b0100, 500f), _1000_MS(0b0101, 1_000), _10_MS(0b0110, 10f), _20_MS(0b0111, 20), NONE(0b1000, 0f);

		private byte value;
		private float standbyTimeMs;

		StandbyDuration(int value, float standbyTimeMs) {
			this.value = (byte) value;
			this.standbyTimeMs = standbyTimeMs;
		}

		byte getValue() {
			return value;
		}

		public float getStandbyTimeMs() {
			return standbyTimeMs;
		}

		static StandbyDuration valueOf(int value) {
			if (value < 0 || value > NONE.getValue()) {
				throw new IllegalArgumentException("Invalid StandbyDuration value " + value);
			}

			return values()[value];
		}
	}

	//
	// Registers - START
	//
	private static final int REG_CHIP_ID = 0xd0;
	private static final int REG_SOFT_RESET = 0xe0;
	// Not in BME680
	private static final int REG_UNIQUE_ID = 0x83;
	// Not in BME680
	private static final int REG_VARIANT_ID = 0xf0;

	// Registers for 1st, 2nd and 3rd group of coefficients
	private static final int REG_COEFF1 = 0x8a;
	private static final int LEN_COEFF1 = 23;
	private static final int REG_COEFF2 = 0xe1;
	private static final int LEN_COEFF2 = 14;
	private static final int REG_COEFF3 = 0x00;
	private static final int LEN_COEFF3 = 5;

	// 0th field address. 0x1d..0x2b (len: 15)
	private static final int REG_FIELD0 = 0x1d;
	public static final int NUM_FIELDS = 3;
	// The number of bytes to read when getting data for a single field
	private static final int LEN_FIELD = 17;
	// Length between two fields
	private static final int LEN_FIELD_OFFSET = 17;

	// Heater settings
	// 0th Current DAC address. 0x50..0x59 (len: 10)
	private static final int REG_IDAC_HEAT0 = 0x50;
	// 0th Res heat address. 0x5a..0x63 (len: 10)
	private static final int REG_RES_HEAT0 = 0x5a;
	// 0th Gas wait address. 0x64..0x6d (len: 10)
	private static final int REG_GAS_WAIT0 = 0x64;
	// Shared heating duration address (REG_SHD_HEATR_DUR)
	private static final int REG_GAS_WAIT_SHARED = 0x6e;
	private static final int MAX_NUM_HEATER_PROFILES = 10;

	// Sensor configuration registers
	// [3] heat_off
	private static final int REG_CTRL_GAS_0 = 0x70;
	// [4] run_gas, [3:0] run_gas
	private static final int REG_CTRL_GAS_1 = 0x71;
	// [6] spi_3w_int_en, [2:0] osrs_h - see OversamplingMultiplier
	private static final int REG_CTRL_HUM = 0x72;
	// [7:5] osrs_t, [4:2] osrs_p, [1:0] mode
	private static final int REG_CTRL_MEAS = 0x74;
	// [4:2] filter, [0] spi_3w_en
	private static final int REG_CONFIG = 0x75;

	// Status register ([4] spi_mem_page)
	// private static final int REG_STATUS = 0x73;

	// Memory page
	// private static final int MEM_PAGE_REG = 0xf3;
	//
	// Registers - END
	//

	// Mask definitions
	private static final short NBCONV_MASK = 0b00001111;
	private static final short FILTER_MASK = 0b00011100;
	private static final short ODR3_MASK = 0x80;
	private static final short ODR20_MASK = 0xe0;
	private static final short OVERSAMPLING_TEMPERATURE_MASK = 0b11100000;
	private static final short OVERSAMPLING_PRESSURE_MASK = 0b00011100;
	private static final short OVERSAMPLING_HUMIDITY_MASK = 0b00000111;
	private static final short HEATER_CONTROL_MASK = 0b00001000;
	private static final short RUN_GAS_MASK = 0b00110000;
	private static final short OPERATING_MODE_MASK = 0b00000011;
	private static final short RESISTANCE_HEAT_RANGE_MASK = 0b00110000;
	private static final short RANGE_SWITCHING_ERROR_MASK = 0b11110000;
	private static final short NEW_DATA_MASK = 0b10000000;
	private static final short GAS_INDEX_MASK = 0b00001111;
	private static final short GAS_RANGE_MASK = 0b00001111;
	private static final short GASM_VALID_MASK = 0b00100000;
	private static final short HEAT_STABLE_MASK = 0b00010000;
	// private static final short MEM_PAGE_MASK = 0b00010000;
	// private static final short SPI_RD_MASK = 0b10000000;
	// private static final short SPI_WR_MASK = 0b01111111;
	private static final short BIT_H1_DATA_MASK = 0b00001111;

	// Bit position definitions for sensor settings
	private static final byte NBCONV_POSITION = 0;
	private static final byte FILTER_POSITION = 2;
	private static final byte OVERSAMPLING_TEMPERATURE_POSITION = 5;
	private static final byte OVERSAMPLING_PRESSURE_POSITION = 2;
	private static final byte OVERSAMPLING_HUMIDITY_POSITION = 0;
	private static final byte ODR3_POSITION = 7;
	private static final byte ODR20_POSITION = 5;
	private static final byte RUN_GAS_POSITION = 4;
	private static final byte OPERATING_MODE_POSITION = 0;
	private static final byte HEATER_CONTROL_POSITION = 3;

	// Coefficient index macros
	private static final int IDX_T2_LSB = 0;
	private static final int IDX_T2_MSB = 1;
	private static final int IDX_T3 = 2;
	private static final int IDX_P1_LSB = 4;
	private static final int IDX_P1_MSB = 5;
	private static final int IDX_P2_LSB = 6;
	private static final int IDX_P2_MSB = 7;
	private static final int IDX_P3 = 8;
	private static final int IDX_P4_LSB = 10;
	private static final int IDX_P4_MSB = 11;
	private static final int IDX_P5_LSB = 12;
	private static final int IDX_P5_MSB = 13;
	private static final int IDX_P7 = 14;
	private static final int IDX_P6 = 15;
	private static final int IDX_P8_LSB = 18;
	private static final int IDX_P8_MSB = 19;
	private static final int IDX_P9_LSB = 20;
	private static final int IDX_P9_MSB = 21;
	private static final int IDX_P10 = 22;
	private static final int IDX_H2_MSB = 23;
	private static final int IDX_H2_LSB = 24;
	private static final int IDX_H1_LSB = 24;
	private static final int IDX_H1_MSB = 25;
	private static final int IDX_H3 = 26;
	private static final int IDX_H4 = 27;
	private static final int IDX_H5 = 28;
	private static final int IDX_H6 = 29;
	private static final int IDX_H7 = 30;
	private static final int IDX_T1_LSB = 31;
	private static final int IDX_T1_MSB = 32;
	private static final int IDX_GH2_LSB = 33;
	private static final int IDX_GH2_MSB = 34;
	private static final int IDX_GH1 = 35;
	private static final int IDX_GH3 = 36;
	private static final int IDX_RES_HEAT_VAL = 37;
	private static final int IDX_RES_HEAT_RANGE = 39;
	private static final int IDX_RANGE_SW_ERR = 41;

	// Soft reset command
	private static final int SOFT_RESET_COMMAND = 0xb6;

	private static final byte ENABLE_HEATER = 0;
	private static final byte DISABLE_HEATER = 1;

	private static final byte DISABLE_GAS_MEAS = 0;
	private static final byte ENABLE_GAS_MEAS_L = 1;
	private static final byte ENABLE_GAS_MEAS_H = 2;

	/*- This max value is used to provide precedence to multiplication or division
	 * in pressure compensation equation to achieve least loss of precision and
	 * avoiding overflows.
	 * i.e Comparing value, BME680_MAX_OVERFLOW_VAL = INT32_C(1 << 30)
	 * Other code has this at (1 << 31)
	 */
	static final int MAX_OVERFLOW_VAL = 0x40000000;

	private static final int HUMIDITY_REGISTER_SHIFT_VALUE = 4;
	private static final int RESET_PERIOD_MILLISECONDS = 10;
	private static final int POLL_PERIOD_MILLISECONDS = 10;

	// Look up tables for the possible gas range values
	static final long GAS_RANGE_LOOKUP_TABLE_1_INT[] = { 2147483647L, 2147483647L, 2147483647L, 2147483647L,
			2147483647L, 2126008810L, 2147483647L, 2130303777L, 2147483647L, 2147483647L, 2143188679L, 2136746228L,
			2147483647L, 2126008810L, 2147483647L, 2147483647L };
	static final long GAS_RANGE_LOOKUP_TABLE_2_INT[] = { 4096000000L, 2048000000L, 1024000000L, 512000000L, 255744255L,
			127110228L, 64000000L, 32258064L, 16016016L, 8000000L, 4000000L, 2000000L, 1000000L, 500000L, 250000L,
			125000L };
	static final float[] GAS_RANGE_LOOKUP_TABLE_1_FPU = { 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, -1.0f, 0.0f, -0.8f, 0.0f, 0.0f,
			-0.2f, -0.5f, 0.0f, -1.0f, 0.0f, 0.0f };
	static final float[] GAS_RANGE_LOOKUP_TABLE_2_FPU = { 0.0f, 0.0f, 0.0f, 0.0f, 0.1f, 0.7f, 0.0f, -0.8f, -0.1f, 0.0f,
			0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f };

	public static class Builder {
		public static Builder builder(final int controller) {
			return new Builder(controller);
		}

		public static Builder builder(final I2CDeviceInterface device) {
			return new Builder(device);
		}

		/*-
		 * chipId: 0x61, variantId: 0x1, uniqueId: 0x2c
		 * Defaults:
		 * Humidity Oversampling: NONE
		 * Temperature Oversampling: NONE
		 * Pressure Oversampling: NONE
		 * Filter: NONE
		 * Standy Duration: _0_59_MS
		 */
		private int controller;
		private int address = DEVICE_ADDRESS;
		private I2CDeviceInterface device;
		private OversamplingMultiplier humidityOversampling;
		private OversamplingMultiplier temperatureOversampling;
		private OversamplingMultiplier pressureOversampling;
		private IirFilterCoefficient filter;
		private StandbyDuration standbyDuration;

		public Builder(final int controller) {
			this.controller = controller;
		}

		public Builder(final I2CDeviceInterface device) {
			this.device = device;
		}

		public Builder setAddress(int address) {
			if (device != null) {
				throw new IllegalArgumentException("I2C device already assigned");
			}
			this.address = address;
			return this;
		}

		public Builder setHumidityOversampling(OversamplingMultiplier humidityOversampling) {
			this.humidityOversampling = humidityOversampling;
			return this;
		}

		public Builder setTemperatureOversampling(OversamplingMultiplier temperatureOversampling) {
			this.temperatureOversampling = temperatureOversampling;
			return this;
		}

		public Builder setPressureOversampling(OversamplingMultiplier pressureOversampling) {
			this.pressureOversampling = pressureOversampling;
			return this;
		}

		public Builder setFilter(IirFilterCoefficient filter) {
			this.filter = filter;
			return this;
		}

		public Builder setDataRate(StandbyDuration standbyDuration) {
			this.standbyDuration = standbyDuration;
			return this;
		}

		public BME68x build() {
			if (device == null) {
				device = I2CDevice.builder(address).setController(controller).build();
			}
			return new BME68x(device, humidityOversampling, temperatureOversampling, pressureOversampling, filter,
					standbyDuration);
		}
	}

	private I2CDeviceInterface device;
	private byte chipId;
	private byte variantId;
	private byte uniqueId;
	// Raw temperature data
	private int temperatureFineInt;
	private float temperatureFineFloat;
	/* ! Sensor calibration data */
	private Calibration calibration;
	private float ambientTemperature = 20;

	public BME68x() {
		this(I2CConstants.CONTROLLER_1, DEVICE_ADDRESS);
	}

	/**
	 * Create a new BME680 sensor driver connected on the given bus.
	 *
	 * @param controller I2C bus the sensor is connected to.
	 */
	public BME68x(final int controller) {
		this(controller, DEVICE_ADDRESS);
	}

	/**
	 * Create a new BME680 sensor driver connected on the given bus and address.
	 *
	 * @param controller I2C bus the sensor is connected to.
	 * @param address    I2C address of the sensor.
	 */
	public BME68x(final int controller, final int address) {
		this(I2CDevice.builder(address).setController(controller).build());
	}

	/**
	 * Create a new BME680 sensor driver connected on the given bus and address.
	 *
	 * @param device I2C device.
	 */
	public BME68x(final I2CDeviceInterface device) {
		this(device, OversamplingMultiplier.X1, OversamplingMultiplier.X1, OversamplingMultiplier.X1,
				IirFilterCoefficient._7, StandbyDuration._10_MS);
	}

	/**
	 * Create a new BME680 sensor driver connected on the given bus and address.
	 *
	 * @param controller               I2C bus the sensor is connected to.
	 * @param address                  I2C address of the sensor.
	 * @param humidityOversampling     Humidity oversampling.
	 * @param temperatureOversampling Temperature oversampling.
	 * @param pressureOversampling     Pressure oversampling.
	 * @param filter                   Infinite Impulse Response (IIR) filter.
	 * @param standbyDuration          Standby time between sequential mode
	 *                                 measurement profiles.
	 */
	public BME68x(final int controller, final int address, OversamplingMultiplier humidityOversampling,
			OversamplingMultiplier temperatureOversampling, OversamplingMultiplier pressureOversampling,
			IirFilterCoefficient filter, StandbyDuration standbyDuration) {
		this(I2CDevice.builder(address).setController(controller).build(), humidityOversampling,
				temperatureOversampling, pressureOversampling, filter, standbyDuration);
	}

	/**
	 * Create a new BME680 sensor driver connected on the given bus and address.
	 *
	 * @param device                   I2C device.
	 * @param humidityOversampling     Humidity oversampling.
	 * @param temperatureOversampling Temperature oversampling.
	 * @param pressureOversampling     Pressure oversampling.
	 * @param filter                   Infinite Impulse Response (IIR) filter.
	 * @param standbyDuration          Standby time between sequential mode
	 *                                 measurement profiles.
	 */
	public BME68x(final I2CDeviceInterface device, OversamplingMultiplier humidityOversampling,
			OversamplingMultiplier temperatureOversampling, OversamplingMultiplier pressureOversampling,
			IirFilterCoefficient filter, StandbyDuration standbyDuration) {
		this.device = device;

		initialise(humidityOversampling, temperatureOversampling, pressureOversampling, filter, standbyDuration);
	}

	/**
	 * Close the driver and the underlying device.
	 */
	@Override
	public void close() {
		if (device != null) {
			setOperatingMode(OperatingMode.SLEEP);
			try {
				device.close();
			} finally {
				calibration = null;
				device = null;
			}
		}
	}

	/**
	 * Initialise the device.
	 *
	 * bme68x_init
	 */
	public void initialise(OversamplingMultiplier humidityOversampling, OversamplingMultiplier temperatureOversampling,
			OversamplingMultiplier pressureOversampling, IirFilterCoefficient filter, StandbyDuration standbyDuration) {
		chipId = device.readByteData(REG_CHIP_ID);
		if (chipId != CHIP_ID_BME680) {
			throw new IllegalStateException(String.format("%s %s not found.", CHIP_VENDOR, CHIP_NAME));
		}

		variantId = device.readByteData(REG_VARIANT_ID);
		uniqueId = device.readByteData(REG_UNIQUE_ID);

		softReset();

		getCalibrationData();

		// setConfiguration(humidityOversampling, temperatureOversampling,
		// pressureOversampling, filter, standbyDuration);
	}

	/**
	 * Initiate a soft reset.
	 *
	 * bme68x_soft_reset
	 */
	public void softReset() {
		device.writeByteData(REG_SOFT_RESET, (byte) SOFT_RESET_COMMAND);

		SleepUtil.sleepMillis(RESET_PERIOD_MILLISECONDS);
	}

	/**
	 * Get the chip id.
	 *
	 * read_chip_id
	 *
	 * @return chip identifier
	 */
	public byte getChipId() {
		return chipId;
	}

	/**
	 * Get the chip variant id.
	 *
	 * read_variant_id
	 *
	 * @return chip variant identifier
	 */
	public byte getVariantId() {
		return variantId;
	}

	/**
	 * Get the chip's identifier.
	 *
	 * Note not documented.
	 *
	 * @return chip unique id
	 */
	public byte getUniqueId() {
		return uniqueId;
	}

	/**
	 * Get the operating mode.
	 *
	 * bme68x_get_op_mode
	 *
	 * @return Current operating mode
	 */
	public OperatingMode getOperatingMode() {
		return OperatingMode.valueOf(device.readByteData(REG_CTRL_MEAS) & OPERATING_MODE_MASK);
	}

	/**
	 * Set the operating mode.
	 *
	 * bme68x_set_op_mode
	 *
	 * @param mode Target operating mode
	 */
	public void setOperatingMode(final OperatingMode mode) {
		if (getOperatingMode() == mode) {
			return;
		}

		validateOperatingMode(mode);

		setRegByte(REG_CTRL_MEAS, (byte) OPERATING_MODE_MASK, OPERATING_MODE_POSITION, mode.getValue());

		// Wait for the power mode to switch to the requested value
		while (getOperatingMode() != mode) {
			SleepUtil.sleepMillis(POLL_PERIOD_MILLISECONDS);
		}
	}

	/**
	 * Set the oversampling, filter and standby configuration.
	 *
	 * bme68x_set_conf
	 *
	 * @param humidityOversampling    Humidity oversampling mode
	 * @param temperatureOversampling Temperature oversampling mode
	 * @param pressureOversampling    Pressure oversampling mode
	 * @param filter                  IIR Filter coefficient
	 * @param standbyDuration         Standby time between sequential mode
	 *                                measurement profiles (BME688 only).
	 */
	public void setConfiguration(OversamplingMultiplier humidityOversampling,
			OversamplingMultiplier temperatureOversampling, OversamplingMultiplier pressureOversampling,
			IirFilterCoefficient filter, StandbyDuration standbyDuration) {
		OperatingMode current_op_mode = getOperatingMode();

		if (current_op_mode != OperatingMode.SLEEP) {
			// Configure only in the sleep mode
			setOperatingMode(OperatingMode.SLEEP);
		}

		// Register data starting from REG_CTRL_GAS_1(0x71) up to REG_CONFIG(0x75)
		final int LEN_CONFIG = 5;
		byte[] data_array = new byte[LEN_CONFIG];
		device.readI2CBlockData(REG_CTRL_GAS_1, data_array);
		data_array[1] = BitManipulation.updateWithMaskedData(data_array[1], (byte) OVERSAMPLING_HUMIDITY_MASK,
				humidityOversampling.getValue(), OVERSAMPLING_HUMIDITY_POSITION);
		data_array[3] = BitManipulation.updateWithMaskedData(data_array[3], (byte) OVERSAMPLING_PRESSURE_MASK,
				pressureOversampling.getValue(), OVERSAMPLING_PRESSURE_POSITION);
		data_array[3] = BitManipulation.updateWithMaskedData(data_array[3], (byte) OVERSAMPLING_TEMPERATURE_MASK,
				temperatureOversampling.getValue(), OVERSAMPLING_TEMPERATURE_POSITION);
		data_array[4] = BitManipulation.updateWithMaskedData(data_array[4], (byte) FILTER_MASK, filter.getValue(),
				FILTER_POSITION);

		byte odr20 = 0;
		byte odr3 = 1;
		if (standbyDuration != StandbyDuration.NONE) {
			odr20 = standbyDuration.getValue();
			odr3 = 0;
		}
		data_array[4] = BitManipulation.updateWithMaskedData(data_array[4], (byte) ODR20_MASK, odr20, ODR20_POSITION);
		data_array[0] = BitManipulation.updateWithMaskedData(data_array[0], (byte) ODR3_MASK, odr3, ODR3_POSITION);

		writeDataWithIncrementingRegisterAddress(REG_CTRL_GAS_1, data_array);

		// Restore the previous operating mode
		if (current_op_mode != OperatingMode.SLEEP) {
			setOperatingMode(current_op_mode);
		}
	}

	/**
	 * Get humidity oversampling mode
	 *
	 * @return Current humidity oversampling mode
	 */
	public OversamplingMultiplier getHumidityOversample() {
		return OversamplingMultiplier.valueOf(
				(device.readByteData(REG_CTRL_HUM) & OVERSAMPLING_HUMIDITY_MASK) >> OVERSAMPLING_HUMIDITY_POSITION);
	}

	/**
	 * Get temperature oversampling mode
	 *
	 * @return Current temperature oversampling mode
	 */
	public OversamplingMultiplier getTemperatureOversample() {
		return OversamplingMultiplier.valueOf((device.readByteData(REG_CTRL_MEAS)
				& OVERSAMPLING_TEMPERATURE_MASK) >> OVERSAMPLING_TEMPERATURE_POSITION);
	}

	/**
	 * Get pressure oversampling mode
	 *
	 * @return Current pressure oversampling mode
	 */
	public OversamplingMultiplier getPressureOversample() {
		return OversamplingMultiplier.valueOf(
				(device.readByteData(REG_CTRL_MEAS) & OVERSAMPLING_PRESSURE_MASK) >> OVERSAMPLING_PRESSURE_POSITION);
	}

	/**
	 * Get the IIR filter configuration
	 *
	 * @return IIR filter configuration
	 */
	public IirFilterCoefficient getIirFilterConfig() {
		return IirFilterCoefficient.valueOf((device.readByteData(REG_CONFIG) & FILTER_MASK) >> FILTER_POSITION);
	}

	public StandbyDuration getStandbyDuration() {
		byte odr20 = (byte) ((device.readByteData(REG_CONFIG) & ODR20_MASK) >> ODR20_POSITION);
		byte odr3 = (byte) ((device.readByteData(REG_CTRL_GAS_1) & ODR3_MASK) >> ODR3_POSITION);

		return StandbyDuration.valueOf(odr3 << 3 | odr20);
	}

	/**
	 * Calculate the duration in microseconds to take to take TPHG readings using
	 * the currently configured humidity, temperature and pressure oversampling
	 * modes. Can be used to get the remaining duration that can be used for
	 * heating.
	 *
	 * bme68x_get_meas_dur
	 *
	 * @param targetOperatingMode Operating mode
	 * @return remaining duration in microseconds
	 */
	public int calculateMeasureDuration(OperatingMode targetOperatingMode) {
		return calculateMeasureDuration(getHumidityOversample(), getTemperatureOversample(), getPressureOversample(),
				targetOperatingMode);
	}

	/**
	 * Calculate the duration in microseconds to take to take TPHG readings using
	 * the specified humidity, temperature and pressure oversampling modes. Can be
	 * used to get the remaining duration that can be used for heating.
	 *
	 * bme68x_get_meas_dur
	 *
	 * @param humidityOversampling    Humidity oversampling mode
	 * @param temperatureOversampling Temperature oversampling mode
	 * @param pressureOversampling    Pressure oversampling mode
	 * @param targetOperatingMode     Operating mode
	 * @return remaining duration in microseconds
	 */
	public static int calculateMeasureDuration(OversamplingMultiplier humidityOversampling,
			OversamplingMultiplier temperatureOversampling, OversamplingMultiplier pressureOversampling,
			OperatingMode targetOperatingMode) {
		int meas_cycles = humidityOversampling.getCycles() + temperatureOversampling.getCycles()
				+ pressureOversampling.getCycles();
		int meas_dur = meas_cycles * 1963;
		meas_dur += (477 * 4); // TPH switching duration
		meas_dur += (477 * 5); // Gas measurement duration

		if (targetOperatingMode != OperatingMode.PARALLEL) {
			meas_dur += 1000; // Wake up duration of 1ms
		}

		return meas_dur;
	}

	/**
	 * Read the pressure, temperature and humidity and gas data from the sensor,
	 * compensates the data and store it in the bme68x_data structure instance
	 * passed by the user.
	 *
	 * bme68x_get_data
	 *
	 * @return Sensor data
	 */
	public Data[] getSensorData() {
		return getSensorData(OperatingMode.FORCED);
	}

	/**
	 * Read the pressure, temperature and humidity and gas data from the sensor,
	 * compensates the data and store it in the bme68x_data structure instance
	 * passed by the user.
	 *
	 * bme68x_get_data
	 *
	 * @param operatingMode Device operating mode
	 * @return Sensor data
	 */
	public Data[] getSensorData(OperatingMode operatingMode) {
		validateOperatingMode(operatingMode);

		Data[] data;

		setOperatingMode(operatingMode);
		switch (operatingMode) {
		case FORCED:
			data = new Data[] { readFieldData(0) };
			break;
		case SEQUENTIAL:
		case PARALLEL:
			// Read all 3 fields
			data = readAllFieldData();

			// Sort the sensor data in parallel & sequential modes
			for (int i = 0; i < 2; i++) {
				for (int j = i + 1; j < 3; j++) {
					sortSensorData(i, j, data);
				}
			}
			break;
		default:
			data = null;
		}

		return data;
	}

	@Override
	public float getTemperature() {
		return getSensorData()[0].getTemperature();
	}

	@Override
	public float getPressure() {
		return getSensorData()[0].getPressure();
	}

	@Override
	public float getRelativeHumidity() {
		return getSensorData()[0].getHumidity();
	}

	@Override
	public float getGasResistance() {
		return getSensorData()[0].getGasResistance();
	}

	/**
	 * Get the gas configuration of the sensor.
	 *
	 * Note not fully working hence package-private.
	 *
	 * bme68x_get_heatr_conf
	 *
	 * @return the heater configuration
	 */
	public HeaterConfig getHeaterConfiguration() {
		// Turn off current injected to heater by setting bit to 1
		boolean heater_enabled = (device.readByteData(REG_CTRL_GAS_0) & HEATER_CONTROL_MASK) == 0;

		int nb_conv = (device.readByteData(REG_CTRL_GAS_1) & NBCONV_MASK) >> NBCONV_POSITION;

		byte[] data_array = new byte[nb_conv];

		device.readI2CBlockData(REG_RES_HEAT0, data_array);
		int[] temp_profiles = new int[nb_conv];
		for (int i = 0; i < data_array.length; i++) {
			// TODO Do reverse calculation of calculateHeaterResistanceRegValFpu
			temp_profiles[i] = data_array[i] & 0xff;
		}

		device.readI2CBlockData(REG_GAS_WAIT0, data_array);
		int[] dur_profiles = new int[nb_conv];
		for (int i = 0; i < data_array.length; i++) {
			dur_profiles[i] = gasWaitRegValToDuration(data_array[i]);
		}

		// Opposite calculation of calculateGasWaitSharedRegVal
		byte gas_wait_shared_reg_val = device.readByteData(REG_GAS_WAIT_SHARED);
		int mult_factor = (int) Math.pow(4, (gas_wait_shared_reg_val & 0b11000000) >> 6);
		int gas_wait_shared = (int) ((gas_wait_shared_reg_val & 0b00111111) * mult_factor * 0.477f);

		return new HeaterConfig(heater_enabled, temp_profiles, dur_profiles, gas_wait_shared);
	}

	/*
	 * Set the gas configuration of the sensor.
	 *
	 * bme68x_set_heatr_conf
	 */
	public void setHeaterConfiguration(OperatingMode targetOperatingMode, HeaterConfig heaterConfig) {
		validateOperatingMode(targetOperatingMode);

		// Configure only in sleep mode
		setOperatingMode(OperatingMode.SLEEP);

		byte nb_conv = setHeaterConfigInternal(heaterConfig, targetOperatingMode);

		byte[] ctrl_gas_data = new byte[2];
		device.readI2CBlockData(REG_CTRL_GAS_0, ctrl_gas_data);

		byte hctrl, run_gas;
		if (heaterConfig.isEnabled()) {
			hctrl = ENABLE_HEATER;
			// Note that this isn't covered in the datasheet, looks like the datasheet
			// hasn't been updated
			// run_gas bit position is 5 for BME688 and 4 for BME680
			if (VARIANT_ID_BM688 == variantId) {
				run_gas = ENABLE_GAS_MEAS_H;
			} else {
				run_gas = ENABLE_GAS_MEAS_L;
			}
		} else {
			hctrl = DISABLE_HEATER;
			run_gas = DISABLE_GAS_MEAS;
		}

		ctrl_gas_data[0] = BitManipulation.updateWithMaskedData(ctrl_gas_data[0], HEATER_CONTROL_MASK, hctrl,
				HEATER_CONTROL_POSITION);
		ctrl_gas_data[1] = BitManipulation.updateWithMaskedData(ctrl_gas_data[1], NBCONV_MASK, nb_conv,
				NBCONV_POSITION);
		ctrl_gas_data[1] = BitManipulation.updateWithMaskedData(ctrl_gas_data[1], RUN_GAS_MASK, run_gas,
				RUN_GAS_POSITION);

		writeDataWithIncrementingRegisterAddress(REG_CTRL_GAS_0, ctrl_gas_data);
	}

	/**
	 * Self-test of low gas variant of BME68X (i.e. the BME680)
	 *
	 * bme68x_low_gas_selftest_check
	 */
	public void lowGasSelfTestCheck() {
		Logger.info("Performing low gas self test...");

		final int HEATR_DUR1 = 1000;
		final int HEATR_DUR2 = 2000;
		final int LOW_TEMP = 150;
		final int HIGH_TEMP = 350;
		final int HEATR_DUR1_DELAY_MS = 1_000_000 / 1_000;
		final int HEATR_DUR2_DELAY_MS = 2_000_000 / 1_000;
		final int N_MEAS = 6;

		ambientTemperature = 25;

		OversamplingMultiplier os_hum = OversamplingMultiplier.X1;
		OversamplingMultiplier os_press = OversamplingMultiplier.X16;
		OversamplingMultiplier os_temp = OversamplingMultiplier.X2;

		HeaterConfig heatr_conf = new HeaterConfig(true, HEATR_DUR1, HIGH_TEMP);
		setHeaterConfiguration(OperatingMode.FORCED, heatr_conf);
		setConfiguration(os_hum, os_temp, os_press, IirFilterCoefficient.NONE, StandbyDuration._0_59_MS);
		setOperatingMode(OperatingMode.FORCED);

		Logger.info("Sleeping for {} ms", Integer.valueOf(HEATR_DUR1_DELAY_MS));
		SleepUtil.sleepMillis(HEATR_DUR1_DELAY_MS);

		Data[] data = new Data[N_MEAS];
		data[0] = getSensorData(OperatingMode.FORCED)[0];

		if ((data[0].idacHeatRegVal != 0x00) && (data[0].idacHeatRegVal != 0xFF) && data[0].gasMeasurementValid) {
			// Ok
		} else {
			// Error
			Logger.error("Error with idac {} or gasMeasurementValid {}", Short.valueOf(data[0].idacHeatRegVal),
					Boolean.valueOf(data[0].gasMeasurementValid));
		}

		heatr_conf.heaterDurationProfiles[0] = HEATR_DUR2;
		int i = 0;
		while (i < N_MEAS) {
			if ((i % 2) == 0) {
				heatr_conf.heaterTempProfiles[0] = HIGH_TEMP;
			} else {
				heatr_conf.heaterTempProfiles[0] = LOW_TEMP;
			}

			setHeaterConfiguration(OperatingMode.FORCED, heatr_conf);
			setConfiguration(os_hum, os_temp, os_press, IirFilterCoefficient.NONE, StandbyDuration._0_59_MS);
			setOperatingMode(OperatingMode.FORCED);

			Logger.info("Sleeping for {} ms", Integer.valueOf(HEATR_DUR2_DELAY_MS));
			SleepUtil.sleepMillis(HEATR_DUR2_DELAY_MS);

			data[i] = getSensorData(OperatingMode.FORCED)[0];

			i++;
		}

		analyseSensorData(data);
	}

	private Data extractTphgReading(byte[] buffer) {
		Data data = new Data();

		// Set to 1 during measurements, goes to 0 when measurements are completed
		data.newData = (buffer[0] & NEW_DATA_MASK) == 0 ? true : false;

		data.gasMeasurementIndex = buffer[0] & GAS_INDEX_MASK;
		data.measureIndex = buffer[1] & 0xff;

		if (VARIANT_ID_BM688 == variantId) {
			data.gasMeasurementValid = (buffer[16] & GASM_VALID_MASK) != 0;
			data.heaterTempStable = (buffer[16] & HEAT_STABLE_MASK) != 0;
		} else {
			data.gasMeasurementValid = (buffer[14] & GASM_VALID_MASK) != 0;
			data.heaterTempStable = (buffer[14] & HEAT_STABLE_MASK) != 0;
		}

		if (data.newData) {
			// Extract the raw ADC data from the sensor reading
			final int adc_pres = ((buffer[2] & 0xff) << 12) | ((buffer[3] & 0xff) << 4) | ((buffer[4] & 0xff) >> 4);
			final int adc_temp = ((buffer[5] & 0xff) << 12) | ((buffer[6] & 0xff) << 4) | ((buffer[7] & 0xff) >> 4);
			final int adc_hum = ((buffer[8] & 0xff) << 8) | (buffer[9] & 0xff);

			// data.temperature = calculateTemperatureInt(adc_temp) / 100.0f;
			data.temperature = calculateTemperatureFpu(adc_temp);
			// Update the ambient temperature for subsequent heater calculations
			ambientTemperature = data.temperature;
			// data.pressure = calculatePressureInt(adc_pres) / 100.0f;
			data.pressureHPa = calculatePressureFpu(adc_pres);
			// data.humidity = calculateHumidityInt(adc_hum) / 1000.0f;
			data.humidity = calculateHumidityFpu(adc_hum);

			if (data.gasMeasurementValid) {
				if (VARIANT_ID_BM688 == variantId) {
					final int adc_gas_res_high = ((buffer[15] & 0xff) << 2) | ((buffer[16] & 0xff) >> 6);
					// System.out.println("adc_gas_res_high: " + adc_gas_res_high);
					final int gas_range_h = buffer[16] & GAS_RANGE_MASK;
					// System.out.println("gas_range_h: " + gas_range_h);
					/*-
					data.gasResistance = calculateGasResistanceHighInt(adc_gas_res_high, gas_range_h);
					 */
					data.gasResistance = calculateGasResistanceHighFpu(adc_gas_res_high, gas_range_h);
				} else {
					final int adc_gas_res_low = ((buffer[13] & 0xff) << 2) | ((buffer[14] & 0xff) >> 6);
					// System.out.println("adc_gas_res_low: " + adc_gas_res_low);
					final int gas_range_l = buffer[14] & GAS_RANGE_MASK;
					/*-
					data.gasResistance = calculateGasResistanceLowInt(adc_gas_res_low, gas_range_l,
							calibration.rangeSwitchingError);
					*/
					data.gasResistance = calculateGasResistanceLowFpu(adc_gas_res_low, gas_range_l,
							calibration.rangeSwitchingError);
				}
			}
		}

		return data;
	}

	/*
	 * read_field_data
	 */
	private Data readFieldData(int index) {
		Data data;

		final byte[] buffer = new byte[LEN_FIELD];
		int tries = 5;
		do {
			// Read data from the sensor
			device.readI2CBlockData(REG_FIELD0 + index * LEN_FIELD_OFFSET, buffer);

			data = extractTphgReading(buffer);

			if (data.newData) {
				data.idacHeatRegVal = (short) (device.readByteData(REG_IDAC_HEAT0 + data.gasMeasurementIndex) & 0xff);
				// Note that these fields aren't really required - they aren't changed
				// TODO Do reverse calculation of calculateHeaterResistanceRegValFpu
				data.heaterResistance = (short) (device.readByteData(REG_RES_HEAT0 + data.gasMeasurementIndex) & 0xff);
				data.gasWaitMs = (short) gasWaitRegValToDuration(
						device.readByteData(REG_GAS_WAIT0 + data.gasMeasurementIndex));

				break;
			}

			/* Delay to poll the data */
			SleepUtil.sleepMillis(POLL_PERIOD_MILLISECONDS);
		} while (--tries >= 0);

		return data;
	}

	/*
	 * Not applicable to BME680.
	 *
	 * read_all_field_data
	 */
	private Data[] readAllFieldData() {
		Data[] data = new Data[NUM_FIELDS];

		byte[][] buffer = new byte[NUM_FIELDS][LEN_FIELD];
		for (int i = 0; i < NUM_FIELDS; i++) {
			// Get around the 32 byte limit when using SMBus commands - 51 bytes to read
			// TODO Switch to I2C readWrite when supported in pigpio
			int read = device.readI2CBlockData(REG_FIELD0 + i * LEN_FIELD_OFFSET, buffer[i]);
			if (read != LEN_FIELD) {
				Logger.error("Expected to read {} bytes, read {}", Integer.valueOf(LEN_FIELD), Integer.valueOf(read));
			}
		}

		// idac, res_heat, gas_wait
		byte[] set_val = new byte[NUM_FIELDS * MAX_NUM_HEATER_PROFILES];
		device.readI2CBlockData(REG_IDAC_HEAT0, set_val);

		for (int i = 0; i < NUM_FIELDS; i++) {
			data[i] = extractTphgReading(buffer[i]);

			if (data[i].newData) {
				data[i].idacHeatRegVal = (short) (set_val[data[i].gasMeasurementIndex] & 0xff);
				// Note that these fields aren't really required - they aren't changed
				// TODO Do reverse calculation of calculateHeaterResistanceRegValFpu
				data[i].heaterResistance = (short) (set_val[10 + data[i].gasMeasurementIndex] & 0xff);
				data[i].gasWaitMs = (short) gasWaitRegValToDuration(set_val[20 + data[i].gasMeasurementIndex]);
			}
		}

		return data;
	}

	/*
	 * sort_sensor_data
	 */
	private static void sortSensorData(int lowIndex, int highIndex, Data[] dataArray) {
		int meas_index1 = dataArray[lowIndex].measureIndex;
		int meas_index2 = dataArray[highIndex].measureIndex;
		if (dataArray[lowIndex].newData && dataArray[highIndex].newData) {
			int diff = meas_index2 - meas_index1;
			if (((diff > -3) && (diff < 0)) || (diff > 2)) {
				swapFields(lowIndex, highIndex, dataArray);
			}
		} else if (dataArray[highIndex].newData) {
			swapFields(lowIndex, highIndex, dataArray);
		}
		/*-
		 * Sorting field data
		 *
		 * The 3 fields are filled in a fixed order with data in an incrementing
		 * 8-bit sub-measurement index which looks like
		 * Field index | Sub-meas index
		 *      0      |        0
		 *      1      |        1
		 *      2      |        2
		 *      0      |        3
		 *      1      |        4
		 *      2      |        5
		 *      ...
		 *      0      |        252
		 *      1      |        253
		 *      2      |        254
		 *      0      |        255
		 *      1      |        0
		 *      2      |        1
		 *
		 * The fields are sorted in a way so as to always deal with only a snapshot
		 * of comparing 2 fields at a time. The order being
		 * field0 & field1
		 * field0 & field2
		 * field1 & field2
		 * Here the oldest data should be in field0 while the newest is in field2.
		 * In the following documentation, field0's position would referred to as
		 * the lowest and field2 as the highest.
		 *
		 * In order to sort we have to consider the following cases,
		 *
		 * Case A: No fields have new data
		 *     Then do not sort, as this data has already been read.
		 *
		 * Case B: Higher field has new data
		 *     Then the new field get's the lowest position.
		 *
		 * Case C: Both fields have new data
		 *     We have to put the oldest sample in the lowest position. Since the
		 *     sub-meas index contains in essence the age of the sample, we calculate
		 *     the difference between the higher field and the lower field.
		 *     Here we have 3 sub-cases,
		 *     Case 1: Regular read without overwrite
		 *         Field index | Sub-meas index
		 *              0      |        3
		 *              1      |        4
		 *
		 *         Field index | Sub-meas index
		 *              0      |        3
		 *              2      |        5
		 *
		 *         The difference is always <= 2. There is no need to swap as the
		 *         oldest sample is already in the lowest position.
		 *
		 *     Case 2: Regular read with an overflow and without an overwrite
		 *         Field index | Sub-meas index
		 *              0      |        255
		 *              1      |        0
		 *
		 *         Field index | Sub-meas index
		 *              0      |        254
		 *              2      |        0
		 *
		 *         The difference is always <= -3. There is no need to swap as the
		 *         oldest sample is already in the lowest position.
		 *
		 *     Case 3: Regular read with overwrite
		 *         Field index | Sub-meas index
		 *              0      |        6
		 *              1      |        4
		 *
		 *         Field index | Sub-meas index
		 *              0      |        6
		 *              2      |        5
		 *
		 *         The difference is always > -3. There is a need to swap as the
		 *         oldest sample is not in the lowest position.
		 *
		 *     Case 4: Regular read with overwrite and overflow
		 *         Field index | Sub-meas index
		 *              0      |        0
		 *              1      |        254
		 *
		 *         Field index | Sub-meas index
		 *              0      |        0
		 *              2      |        255
		 *
		 *         The difference is always > 2. There is a need to swap as the
		 *         oldest sample is not in the lowest position.
		 *
		 * To summarise, we have to swap when
		 *     - The higher field has new data and the lower field does not.
		 *     - If both fields have new data, then the difference of sub-meas index
		 *         between the higher field and the lower field creates the
		 *         following condition for swapping.
		 *         - (diff > -3) && (diff < 0), combination of cases 1, 2, and 3.
		 *         - diff > 2, case 4.
		 *
		 *     Here the limits of -3 and 2 derive from the fact that there are 3 fields.
		 *     These values decrease or increase respectively if the number of fields increases.
		 */
	}

	/*
	 * swap_fields
	 */
	private static void swapFields(int index1, int index2, Data[] dataArray) {
		Data temp = dataArray[index1];
		dataArray[index1] = dataArray[index2];
		dataArray[index2] = temp;
	}

	private static void analyseSensorData(Data[] data) {
		if ((data[0].temperature < MIN_TEMPERATURE_CELSIUS) || (data[0].temperature > MAX_TEMPERATURE_CELSIUS)) {
			Logger.error("Temperature {} out of range", Float.valueOf(data[0].temperature));
		}
		if ((data[0].pressureHPa < MIN_PRESSURE_HPA) || (data[0].pressureHPa > MAX_PRESSURE_HPA)) {
			Logger.error("Pressure {} hPa out of range", Float.valueOf(data[0].pressureHPa));
		}
		if ((data[0].humidity < MIN_HUMIDITY_PERCENT) || (data[0].humidity > MAX_HUMIDITY_PERCENT)) {
			Logger.error("Humidity {} %rh out of range", Float.valueOf(data[0].humidity));
		}

		// Every gas measurement should be valid
		for (int i = 0; i < data.length; i++) {
			if (!data[i].gasMeasurementValid) {
				Logger.error("Gas measurement should be valid");
			}
		}

		float cent_res = 0;
		if (data.length >= 6) {
			cent_res = ((5 * (data[3].gasResistance + data[5].gasResistance)) / (2 * data[4].gasResistance));
		}

		if (cent_res < 6) {
			Logger.error("cent_res {} should be >= 6 (??)", Float.valueOf(cent_res));
		}
	}

	private void validateOperatingMode(OperatingMode mode) {
		if (variantId == VARIANT_ID_BM680 && (mode == OperatingMode.PARALLEL || mode == OperatingMode.SEQUENTIAL)) {
			throw new IllegalArgumentException("Invalid operating mode (" + mode + ") for BME680");
		}
	}

	/*
	 * calc_temperature (using integer math)
	 */
	int calculateTemperatureInt(final int temperatureAdc) {
		// Convert the raw temperature to degrees C using calibration_data.
		int var1 = (temperatureAdc >> 3) - (calibration.temperature[0] << 1);
		int var2 = (var1 * calibration.temperature[1]) >> 11;
		int var3 = ((var1 >> 1) * (var1 >> 1)) >> 12;
		var3 = (var3 * (calibration.temperature[2] << 4)) >> 14;

		// Save temperature data for humidity and pressure calculations
		temperatureFineInt = var2 + var3;

		return ((temperatureFineInt * 5) + 128) >> 8;
	}

	/*
	 * calc_temperature (using FPU math)
	 */
	float calculateTemperatureFpu(final int temperatureAdc) {
		/* calculate var1 data */
		float var1 = (((temperatureAdc / 16384.0f) - (calibration.temperature[0] / 1024.0f))
				* calibration.temperature[1]);

		/* calculate var2 data */
		float var2 = ((((temperatureAdc / 131072.0f) - (calibration.temperature[0] / 8192.0f))
				* ((temperatureAdc / 131072.0f) - (calibration.temperature[0] / 8192.0f)))
				* (calibration.temperature[2] * 16.0f));

		// Save temperature data for humidity and pressure calculations
		temperatureFineFloat = var1 + var2;

		/* compensated temperature data */
		return (temperatureFineFloat / 5120.0f);
	}

	/**
	 * Calculate current pressure from the ADC in pascal (100 Pa = 1 hPa).
	 *
	 * calc_pressure (using int math)
	 *
	 * @param pressureAdc Raw pressure reading from the ADC
	 *
	 * @return Pressure in pascals (100 Pa = 1 hPa)
	 */
	long calculatePressureInt(final int pressureAdc) {
		// Convert the raw pressure using calibration data.
		int var1 = (temperatureFineInt >> 1) - 64000;
		int var2 = ((((var1 >> 2) * (var1 >> 2)) >> 11) * calibration.pressure[5]) >> 2;
		var2 = var2 + ((var1 * calibration.pressure[4]) << 1);
		var2 = (var2 >> 2) + (calibration.pressure[3] << 16);
		var1 = (((((var1 >> 2) * (var1 >> 2)) >> 13) * (calibration.pressure[2] << 5)) >> 3)
				+ ((calibration.pressure[1] * var1) >> 1);
		var1 = var1 >> 18;

		var1 = ((32768 + var1) * calibration.pressure[0]) >> 15;
		int calculated_pressure = 1048576 - pressureAdc;
		calculated_pressure = (calculated_pressure - (var2 >> 12)) * 3125;

		if (calculated_pressure >= MAX_OVERFLOW_VAL) {
			calculated_pressure = ((calculated_pressure / var1) << 1);
		} else {
			calculated_pressure = ((calculated_pressure << 1) / var1);
		}

		var1 = (calibration.pressure[8] * (((calculated_pressure >> 3) * (calculated_pressure >> 3)) >> 13)) >> 12;
		var2 = ((calculated_pressure >> 2) * calibration.pressure[7]) >> 13;
		int var3 = ((calculated_pressure >> 8) * (calculated_pressure >> 8) * (calculated_pressure >> 8)
				* calibration.pressure[9]) >> 17;

		calculated_pressure = calculated_pressure + ((var1 + var2 + var3 + (calibration.pressure[6] << 7)) >> 4);

		// uint32_t
		return calculated_pressure & 0xffffffff;
	}

	/**
	 * Calculate current pressure from the ADC in hPA.
	 *
	 * calc_pressure (using FPU math)
	 *
	 * @param pressureAdc Raw pressure reading from the ADC
	 *
	 * @return Pressure in hPA
	 */
	float calculatePressureFpu(int pressureAdc) {
		float var1 = (temperatureFineFloat / 2.0f) - 64000.0f;
		float var2 = var1 * var1 * (calibration.pressure[5] / (131072.0f));
		var2 = var2 + (var1 * calibration.pressure[4] * 2.0f);
		var2 = (var2 / 4.0f) + (calibration.pressure[3] * 65536.0f);
		var1 = (((calibration.pressure[2] * var1 * var1) / 16384.0f) + (calibration.pressure[1] * var1)) / 524288.0f;
		var1 = (1.0f + (var1 / 32768.0f)) * calibration.pressure[0];

		/* Avoid exception caused by division by zero */
		if ((int) var1 == 0) {
			return 0;
		}

		float calc_pres = 1048576.0f - pressureAdc;
		calc_pres = ((calc_pres - (var2 / 4096.0f)) * 6250.0f) / var1;
		var1 = (calibration.pressure[8] * calc_pres * calc_pres) / 2147483648.0f;
		var2 = calc_pres * (calibration.pressure[7] / 32768.0f);
		float var3 = (calc_pres / 256.0f) * (calc_pres / 256.0f) * (calc_pres / 256.0f)
				* (calibration.pressure[9] / 131072.0f);
		calc_pres = calc_pres + (var1 + var2 + var3 + (calibration.pressure[6] * 128.0f)) / 16.0f;
		return calc_pres / 100;
	}

	/*
	 * calc_humidity (using Int math)
	 */
	int calculateHumidityInt(final int humidityAdc) {
		int temp_scaled = ((temperatureFineInt * 5) + 128) >> 8;
		int var1 = humidityAdc - (calibration.humidity[0] * 16)
				- (((temp_scaled * calibration.humidity[2]) / 100) >> 1);
		int var2 = (calibration.humidity[1] * (((temp_scaled * calibration.humidity[3]) / 100)
				+ (((temp_scaled * ((temp_scaled * calibration.humidity[4]) / 100)) >> 6) / 100) + (1 << 14))) >> 10;
		int var3 = var1 * var2;
		int var4 = calibration.humidity[5] << 7;
		var4 = (var4 + ((temp_scaled * calibration.humidity[6]) / 100)) >> 4;
		int var5 = ((var3 >> 14) * (var3 >> 14)) >> 10;
		int var6 = (var4 * var5) >> 1;
		int calc_hum = (((var3 + var6) >> 10) * 1000) >> 12;

		// Cap at 100%rH
		return Math.min(100_000, Math.max(0, calc_hum));
	}

	/*
	 * calc_humidity (using FPU math)
	 */
	float calculateHumidityFpu(final int humidityAdc) {
		/* compensated temperature data */
		float temp_comp = (temperatureFineFloat / 5120.0f);
		float var1 = humidityAdc - ((calibration.humidity[0] * 16.0f) + ((calibration.humidity[2] / 2.0f) * temp_comp));
		float var2 = var1
				* ((calibration.humidity[1] / 262144.0f) * (1.0f + ((calibration.humidity[3] / 16384.0f) * temp_comp)
						+ ((calibration.humidity[4] / 1048576.0f) * temp_comp * temp_comp)));
		float var3 = calibration.humidity[5] / 16384.0f;
		float var4 = calibration.humidity[6] / 2097152.0f;
		float calc_hum = var2 + ((var3 + (var4 * temp_comp)) * var2 * var2);

		// Make sure the calculated value is in the range 0..100
		return Math.min(100, Math.max(0, calc_hum));
	}

	/*
	 * calc_gas_resistance_low (using Int math)
	 */
	static long calculateGasResistanceLowInt(final int gasResistanceAdc, final int gasRange,
			final int rangeSwitchingError) {
		final long var1 = (1340 + (5L * rangeSwitchingError)) * GAS_RANGE_LOOKUP_TABLE_1_INT[gasRange] >> 16;
		final long var2 = (((((long) gasResistanceAdc) << 15) - 16777216L) + var1);
		final long var3 = ((GAS_RANGE_LOOKUP_TABLE_2_INT[gasRange] * var1) >> 9);

		// uint32_t
		return ((var3 + (var2 >> 1)) / var2) & 0xffffffff;
	}

	/*
	 * calc_gas_resistance_low (using FPU math)
	 */
	static float calculateGasResistanceLowFpu(final int gasResistanceAdc, final int gasRange,
			final int rangeSwitchingError) {
		float gas_range_f = (1 << gasRange);

		float var1 = (1340.0f + (5.0f * rangeSwitchingError));
		float var2 = (var1) * (1.0f + GAS_RANGE_LOOKUP_TABLE_1_FPU[gasRange] / 100.0f);
		float var3 = 1.0f + (GAS_RANGE_LOOKUP_TABLE_2_FPU[gasRange] / 100.0f);
		return 1.0f / (var3 * (0.000000125f) * gas_range_f * (((gasResistanceAdc - 512.0f) / var2) + 1.0f));
	}

	/*
	 * calc_gas_resistance_high (using Int math)
	 */
	static long calculateGasResistanceHighInt(final int gasResistanceAdc, final int gasRange) {
		long var1 = 262144L >> gasRange;
		int var2 = gasResistanceAdc - 512;
		var2 *= 3;
		var2 = 4096 + var2;

		/*
		 * Multiplying 10000 then dividing then multiplying by 100 instead of
		 * multiplying by 1000000 to prevent overflow
		 */
		return ((10000 * var1) / var2) * 100;
	}

	/*
	 * calc_gas_resistance_high (using FPU math)
	 */
	static float calculateGasResistanceHighFpu(final int gasResistanceAdc, final int gasRange) {
		long var1 = 262144L >> gasRange;
		int var2 = gasResistanceAdc - 512;

		var2 *= 3;
		var2 = 4096 + var2;

		return 1000000.0f * var1 / var2;
	}

	/*
	 * Internal API used to calculate the heater resistance value.
	 *
	 * calc_res_heat (using Int math)
	 */
	static int calculateHeaterResistanceRegValInt(final Calibration calibration, final int ambientTemperature,
			final int targetHeaterTemperature) {
		/* Cap temperature */
		final int normalised_temperature = Math.min(targetHeaterTemperature, 400);

		final int var1 = ((ambientTemperature * calibration.gasHeater[2]) / 1000) * 256;
		final int var2 = (calibration.gasHeater[0] + 784)
				* (((((calibration.gasHeater[1] + 154009) * normalised_temperature * 5) / 100) + 3276800) / 10);
		final int var3 = var1 + (var2 / 2);
		final int var4 = (var3 / (calibration.resistanceHeaterRange + 4));
		final int var5 = (131 * calibration.resistanceHeaterValue) + 65536;
		final int heater_res_x100 = ((var4 / var5) - 250) * 34;

		// uint8_t
		return ((heater_res_x100 + 50) / 100) & 0xff;
	}

	static int calculateHeaterResistanceRegValFpu(final Calibration calibration, final float ambientTemperature,
			final int targetHeaterTemperature) {
		/* Cap temperature */
		final int normalised_temperature = Math.min(targetHeaterTemperature, 400);

		float var1 = (calibration.gasHeater[0] / 16.0f) + 49.0f;
		float var2 = ((calibration.gasHeater[1] / 32768.0f) * 0.0005f) + 0.00235f;
		float var3 = calibration.gasHeater[2] / 1024.0f;
		float var4 = var1 * (1.0f + (var2 * normalised_temperature));
		float var5 = var4 + (var3 * ambientTemperature);

		return (int) (3.4f * ((var5 * (4f / (4f + calibration.resistanceHeaterRange))
				* (1f / (1f + (calibration.resistanceHeaterValue * 0.002f)))) - 25f));
	}

	/**
	 * Internal API used to calculate the gas wait
	 *
	 * 64 timer values with 1ms step sizes.
	 *
	 * <pre>
	 * Bits[7:6] : multiplication factor (0b00: x1, 0b01: x4, 0b10: x16, 0b11: x64)
	 * Bits[5:0] : number of 1ms step sizes
	 * 0b00000000 = 0ms
	 * 0b00000001 = 1ms
	 * 0b00111111 = 63 * 1ms = 63ms
	 * 0b00000100 = 4 * 1ms = 4ms (Note the 4ms can also be 0b01000001 = 4 * 1ms)
	 * 0b01010000 = 4 * 16ms = 64ms (Note that 64ms can also be 0b11000001 = 64 * 1ms)
	 * 0b11111111 = 64 * 63 * 1ms = 4032ms (0xFC0 = 4032)
	 * </pre>
	 *
	 * calc_gas_wait
	 */
	static int calculateGasWaitRegVal(final int durationMs) {
		int factor = 0;
		int durval;

		int dur = durationMs;

		// 0xfc0 = 4032ms (max duration)
		if (dur >= 0xfc0) {
			durval = 0xff; /* Max duration */
		} else {
			while (dur > 0x3F) {
				dur = dur / 4;
				factor += 1;
			}
			durval = dur + (factor * 64);
		}

		// uint8_t
		return durval & 0xff;
	}

	/*
	 * Opposite calculation of calculateGasWaitRegVal
	 */
	static int gasWaitRegValToDuration(byte gasWaitRegVal) {
		int mult_factor = (int) Math.pow(4, (gasWaitRegVal & 0b11000000) >> 6);
		return (gasWaitRegVal & 0b00111111) * mult_factor;
	}

	/**
	 * Internal API used to calculate the register value for shared heater duration.
	 * Called from setHeaterConfigInternal when in parallel mode
	 *
	 * <pre>
	 * Bits[7:6] : multiplication factor (0b00: x1, 0b01: x4, 0b10: x16, 0b11: x64)
	 * Bits[5:0] : number of 0.477ms step sizes
	 * Examples:
	 * 0b00000000 = 0b00000001 = 0.477ms
	 * 0b00111111 = 63 * 0.477ms = 30.051ms
	 * 0b01000001 = 4 * 0.477ms = 1.908ms
	 * 0b11000001 = 64 * 0.477ms = 30.528ms
	 * 0b11111111 = 64 * 63 * 0.477ms = 1,923.264ms (0x783 = 1923)
	 * </pre>
	 *
	 * calc_heatr_dur_shared
	 *
	 * @param durationMs Shared duration in microseconds
	 * @return Value to write to the gas_wait_shared register.
	 */
	static int calculateGasWaitSharedRegVal(final int durationMs) {
		int factor = 0;
		int gas_wait_shared;

		int dur = durationMs;

		if (dur >= 0x783) {
			gas_wait_shared = 0xff; /* Max duration */
		} else {
			/* Step size of 0.477ms */
			dur = (dur * 1000) / 477;
			while (dur > 0x3F) {
				dur = dur >> 2;
				factor += 1;
			}

			gas_wait_shared = dur + (factor * 64);
		}

		// uint8_t
		return gas_wait_shared & 0xff;
	}

	/**
	 * Internal API used to set heater configurations.
	 *
	 * set_conf
	 */
	private byte setHeaterConfigInternal(final HeaterConfig heaterConfig, final OperatingMode targetOperatingMode) {
		byte nb_conv = 0;

		switch (targetOperatingMode) {
		case FORCED:
			// In forced mode nb_conv is the index of the heater step (0..9)
			// Always use heater profile 0 in forced mode (nb_conv = 0)
			nb_conv = 0;

			device.writeByteData(REG_RES_HEAT0,
					calculateHeaterResistanceRegValFpu(calibration, ambientTemperature, heaterConfig.getHeaterTemp(0)));
			device.writeByteData(REG_GAS_WAIT0, calculateGasWaitRegVal(heaterConfig.getHeaterDuration(0)));
			break;
		case SEQUENTIAL:
			// In sequential and parallel modes nb_conv is the number of steps in the heater
			// profile (1..10)
			nb_conv = (byte) heaterConfig.getProfileLength();

			setRegByte(REG_CTRL_GAS_1, (byte) NBCONV_MASK, NBCONV_POSITION, nb_conv);

			byte[] rh_reg_data = new byte[nb_conv];
			byte[] gw_reg_data = new byte[nb_conv];
			for (int i = 0; i < nb_conv; i++) {
				rh_reg_data[i] = (byte) calculateHeaterResistanceRegValFpu(calibration, ambientTemperature,
						heaterConfig.getHeaterTemp(i));
				gw_reg_data[i] = (byte) calculateGasWaitRegVal(heaterConfig.getHeaterDuration(i));
			}
			writeDataWithIncrementingRegisterAddress(REG_RES_HEAT0, rh_reg_data);
			writeDataWithIncrementingRegisterAddress(REG_GAS_WAIT0, gw_reg_data);
			break;
		case PARALLEL:
			// In sequential and parallel modes nb_conv is the number of steps in the heater
			// profile (1..10)
			nb_conv = (byte) heaterConfig.getProfileLength();

			setRegByte(REG_CTRL_GAS_1, (byte) NBCONV_MASK, NBCONV_POSITION, nb_conv);

			int gas_wait_shared = calculateGasWaitSharedRegVal(heaterConfig.getSharedHeaterDurationMs());
			device.writeByteData(REG_GAS_WAIT_SHARED, gas_wait_shared);

			rh_reg_data = new byte[nb_conv];
			gw_reg_data = new byte[nb_conv];
			for (int i = 0; i < nb_conv; i++) {
				rh_reg_data[i] = (byte) calculateHeaterResistanceRegValFpu(calibration, ambientTemperature,
						heaterConfig.getHeaterTemp(i));
				// Note that there isn't a call to calculateGasWait here
				gw_reg_data[i] = (byte) heaterConfig.getHeaterDuration(i);
			}
			writeDataWithIncrementingRegisterAddress(REG_RES_HEAT0, rh_reg_data);
			writeDataWithIncrementingRegisterAddress(REG_GAS_WAIT0, gw_reg_data);
			break;
		case SLEEP:
		default:
		}

		return nb_conv;
	}

	// Read calibration array
	private byte[] readCalibrationData() {
		final byte[] part1 = new byte[LEN_COEFF1];
		device.readI2CBlockData(REG_COEFF1, part1);
		final byte[] part2 = new byte[LEN_COEFF2];
		device.readI2CBlockData(REG_COEFF2, part2);
		final byte[] part3 = new byte[LEN_COEFF3];
		device.readI2CBlockData(REG_COEFF3, part3);

		final byte[] calibration_data = new byte[LEN_COEFF1 + LEN_COEFF2 + LEN_COEFF3];
		System.arraycopy(part1, 0, calibration_data, 0, part1.length);
		System.arraycopy(part2, 0, calibration_data, part1.length, part2.length);
		System.arraycopy(part3, 0, calibration_data, part1.length + part2.length, part3.length);

		return calibration_data;
	}

	/*
	 * get_calib_data
	 */
	private void getCalibrationData() {
		calibration = new Calibration();

		// Read the raw calibration data
		final byte[] calibration_data = readCalibrationData();

		/* Temperature related coefficients */
		calibration.temperature[0] = BitManipulation.bytesToWord(calibration_data[IDX_T1_MSB],
				calibration_data[IDX_T1_LSB], false);
		calibration.temperature[1] = BitManipulation.bytesToWord(calibration_data[IDX_T2_MSB],
				calibration_data[IDX_T2_LSB], true);
		calibration.temperature[2] = calibration_data[IDX_T3];

		/* Pressure related coefficients */
		calibration.pressure[0] = BitManipulation.bytesToWord(calibration_data[IDX_P1_MSB],
				calibration_data[IDX_P1_LSB], false);
		calibration.pressure[1] = BitManipulation.bytesToWord(calibration_data[IDX_P2_MSB],
				calibration_data[IDX_P2_LSB], true);
		calibration.pressure[2] = calibration_data[IDX_P3];
		calibration.pressure[3] = BitManipulation.bytesToWord(calibration_data[IDX_P4_MSB],
				calibration_data[IDX_P4_LSB], true);
		calibration.pressure[4] = BitManipulation.bytesToWord(calibration_data[IDX_P5_MSB],
				calibration_data[IDX_P5_LSB], true);
		calibration.pressure[5] = calibration_data[IDX_P6];
		calibration.pressure[6] = calibration_data[IDX_P7];
		calibration.pressure[7] = BitManipulation.bytesToWord(calibration_data[IDX_P8_MSB],
				calibration_data[IDX_P8_LSB], true);
		calibration.pressure[8] = BitManipulation.bytesToWord(calibration_data[IDX_P9_MSB],
				calibration_data[IDX_P9_LSB], true);
		calibration.pressure[9] = calibration_data[IDX_P10] & 0xFF;

		/* Humidity related coefficients */
		calibration.humidity[0] = (((calibration_data[IDX_H1_MSB] & 0xff) << HUMIDITY_REGISTER_SHIFT_VALUE)
				| (calibration_data[IDX_H1_LSB] & BIT_H1_DATA_MASK)) & 0xffff;
		calibration.humidity[1] = (((calibration_data[IDX_H2_MSB] & 0xff) << HUMIDITY_REGISTER_SHIFT_VALUE)
				| ((calibration_data[IDX_H2_LSB] & 0xff) >> HUMIDITY_REGISTER_SHIFT_VALUE)) & 0xffff;
		calibration.humidity[2] = calibration_data[IDX_H3];
		calibration.humidity[3] = calibration_data[IDX_H4];
		calibration.humidity[4] = calibration_data[IDX_H5];
		calibration.humidity[5] = calibration_data[IDX_H6] & 0xff;
		calibration.humidity[6] = calibration_data[IDX_H7];

		// Gas heater related coefficients
		calibration.gasHeater[0] = calibration_data[IDX_GH1];
		calibration.gasHeater[1] = BitManipulation.bytesToWord(calibration_data[IDX_GH2_MSB],
				calibration_data[IDX_GH2_LSB], true);
		calibration.gasHeater[2] = calibration_data[IDX_GH3];

		/* Other coefficients */
		calibration.resistanceHeaterRange = ((calibration_data[IDX_RES_HEAT_RANGE] & RESISTANCE_HEAT_RANGE_MASK) / 16);
		calibration.resistanceHeaterValue = calibration_data[IDX_RES_HEAT_VAL];
		calibration.rangeSwitchingError = ((calibration_data[IDX_RANGE_SW_ERR] & RANGE_SWITCHING_ERROR_MASK)) / 16;

		/*-
		System.out.println("calib.res_heat_range: " + calibration.resistanceHeaterRange);
		System.out.println("calib.res_heat_val: " + calibration.resistanceHeaterValue);
		System.out.println("calib.range_sw_err: " + calibration.rangeSwitchingError);
		System.out.println("calib.par_gh2: " + calibration.gasHeater[1]);
		System.out.println("calib.par_gh3: " + calibration.gasHeater[2]);
		*/
	}

	/**
	 * Write the data array to the device in one transaction, auto-incrementing the
	 * register address for each byte in the array.
	 *
	 * See section 6.2.1 I2C Write (p39) - register addresses do not auto-increment
	 * when doing I2C multiple byte writes.
	 *
	 * @param registerStart The register address
	 * @param data          The data to write
	 */
	private void writeDataWithIncrementingRegisterAddress(int registerStart, byte[] data) {
		// Cannot do writeI2CBlockData as the register address does _not_ auto-increment
		// device.writeI2CBlockData(registerStart, data_array);
		byte[] buffer = new byte[data.length * 2];
		for (int i = 0; i < data.length; i++) {
			buffer[2 * i] = (byte) (registerStart + i);
			buffer[2 * i + 1] = data[i];
		}
		device.writeBytes(buffer);
		// Alternative is to do multiple individual byte data writes
		/*-
		for (int i = 0; i < data_array.length; i++) {
			device.writeByteData(registerStart + i, data_array[i]);
		}
		*/
	}

	private void setRegByte(final int address, final byte mask, final byte position, final byte value) {
		device.writeByteData(address,
				BitManipulation.updateWithMaskedData(device.readByteData(address), mask, value, position));
	}

	static class Calibration {
		final int[] temperature = new int[3];
		final int[] pressure = new int[10];
		final int[] humidity = new int[7];
		final int[] gasHeater = new int[3];
		// Heater resistance range
		int resistanceHeaterRange;
		// Heater resistance value
		int resistanceHeaterValue;
		// Switching error range
		int rangeSwitchingError;
	}

	public static class HeaterConfig {
		// Enable gas measurement. Refer en_dis
		boolean enabled;
		// Store the heater temperature profile in degree Celsius
		int[] heaterTempProfiles;
		// Store the heating duration profile in milliseconds
		int[] heaterDurationProfiles;
		// Store heating duration for parallel mode in milliseconds
		int sharedHeaterDurationMs;

		public HeaterConfig(boolean enabled, int heaterTemp, int heaterDuration) {
			this(enabled, new int[] { heaterTemp }, new int[] { heaterDuration });
		}

		public HeaterConfig(boolean enabled, int[] heaterTempProfiles, int[] heaterDurationProfiles) {
			if (heaterTempProfiles.length == 0 || heaterTempProfiles.length > MAX_NUM_HEATER_PROFILES) {
				throw new IllegalArgumentException(
						"Invalid number of heater temperature profiles: " + heaterTempProfiles.length);
			}
			if (heaterDurationProfiles.length == 0 || heaterDurationProfiles.length > MAX_NUM_HEATER_PROFILES) {
				throw new IllegalArgumentException(
						"Invalid number of heater duration profiles: " + heaterDurationProfiles.length);
			}
			if (heaterTempProfiles.length != heaterDurationProfiles.length) {
				throw new IllegalArgumentException("Number of heater temperature profiles (" + heaterTempProfiles.length
						+ ") != number of duration profiles (" + heaterDurationProfiles.length + ")");
			}

			this.enabled = enabled;
			this.heaterTempProfiles = heaterTempProfiles;
			this.heaterDurationProfiles = heaterDurationProfiles;
		}

		public HeaterConfig(boolean enabled, int[] heaterTempProfiles, int[] heaterDurationProfiles,
				int sharedHeaterDurationMs) {
			this(enabled, heaterTempProfiles, heaterDurationProfiles);

			this.sharedHeaterDurationMs = sharedHeaterDurationMs;
		}

		public boolean isEnabled() {
			return enabled;
		}

		public int getProfileLength() {
			return heaterTempProfiles.length;
		}

		public int getHeaterTemp(int profile) {
			return heaterTempProfiles[profile];
		}

		public int getHeaterDuration(int profile) {
			return heaterDurationProfiles[profile];
		}

		public int getSharedHeaterDurationMs() {
			return sharedHeaterDurationMs;
		}
	}

	public static class Data {
		// Contains new_data, gasm_valid & heat_stab
		boolean newData;
		boolean heaterTempStable;
		boolean gasMeasurementValid;
		// The index of the heater profile used
		int gasMeasurementIndex;
		// Measurement index to track order
		int measureIndex;
		// Temperature in degree celsius
		float temperature;
		// Pressure in hectopascals (hPa)
		float pressureHPa;
		// Humidity in % relative humidity
		float humidity;
		// Gas resistance in Ohms
		float gasResistance;
		// Heater Current (needs conversion to mA) using idacHeatRegVal[7:1] + 1) / 8
		short idacHeatRegVal;
		// Heater resistance
		short heaterResistance;
		// Gas wait period (ms)
		short gasWaitMs;

		public boolean isNewData() {
			return newData;
		}

		public boolean isHeaterTempStable() {
			return heaterTempStable;
		}

		public boolean isGasMeasurementValid() {
			return gasMeasurementValid;
		}

		public int getGasMeasurementIndex() {
			return gasMeasurementIndex;
		}

		public int getMeasureIndex() {
			return measureIndex;
		}

		public float getTemperature() {
			return temperature;
		}

		public float getPressure() {
			return pressureHPa;
		}

		public float getHumidity() {
			return humidity;
		}

		public float getGasResistance() {
			return gasResistance;
		}

		public short getIdacHeatRegVal() {
			return idacHeatRegVal;
		}

		public float getIdacHeatMA() {
			// Calculate IDAC value in mA - (idac_heat[7:1] + 1) / 8
			return ((idacHeatRegVal >> 1) + 1) / 8f;
		}

		public short getHeaterResistance() {
			return heaterResistance;
		}

		public short getGasWait() {
			return gasWaitMs;
		}

		@Override
		public String toString() {
			return "Data [newData=" + newData + ", heaterTempStable=" + heaterTempStable + ", gasMeasurementValid="
					+ gasMeasurementValid + ", gasMeasurementIndex=" + gasMeasurementIndex + ", measureIndex="
					+ measureIndex + ", temperature=" + temperature + ", pressureHPa=" + pressureHPa + ", humidity="
					+ humidity + ", gasResistance=" + gasResistance + ", idacHeatRegVal=" + idacHeatRegVal
					+ ", heaterResistance=" + heaterResistance + ", gasWaitMs=" + gasWaitMs + "]";
		}
	}

}
