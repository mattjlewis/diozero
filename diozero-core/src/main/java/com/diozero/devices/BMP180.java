package com.diozero.devices;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     BMP180.java
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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import com.diozero.api.I2CConstants;
import com.diozero.api.I2CDevice;
import com.diozero.api.I2CDeviceInterface;
import com.diozero.api.RuntimeIOException;
import com.diozero.util.SleepUtil;

/**
 * Bosch BMP180 I2C temperature and pressure sensor
 */
public class BMP180 implements ThermometerInterface, BarometerInterface {
	/**
	 * Relationship between sampling mode and conversion delay (in ms) for each sampling mode
	 * Ultra low power: 4.5 ms minimum conversion delay Standard: 7.5 ms High Resolution: 13.5
	 * ms Ultra high Resolution: 25.5 ms
	 */
	public static enum Mode {
		ULTRA_LOW_POWER(0, 5), STANDARD(1, 8), HIGH_RESOLUTION(2, 14), ULTRA_HIGH_RESOLUTION(3, 26);

		/**
		 * Sampling mode
		 */
		private final int samplingMode;

		/**
		 * Minimum delay required when reading pressure
		 */
		private final int delay;

		/**
		 * Command byte to read pressure based on sampling mode
		 */
		private final byte pressureCommand;

		/**
		 * Create a new instance of a BMPMode
		 * 
		 * @param samplingMode
		 * @param delay
		 */
		Mode(int samplingMode, int delay) {
			this.samplingMode = samplingMode;
			this.delay = delay;
			this.pressureCommand = (byte) (GET_PRESSURE_COMMAND + ((samplingMode << 6) & 0xC0));
		}

		/**
		 * Return the conversion delay (in ms) associated with this sampling mode
		 *
		 * @return delay
		 */
		int getDelay() {
			return delay;
		}

		/**
		 * Return the pressure command to the control register for this sampling mode
		 *
		 * @return command
		 */
		byte getPressureCommand() {
			return pressureCommand;
		}

		/**
		 * Return this sampling mode
		 *
		 * @return Sampling mode
		 */
		int getSamplingMode() {
			return samplingMode;
		}
	}

	/**
	 * Device address BMP180 address is 0x77
	 */
	private static final int BMP180_ADDR = 0x77;

	/**
	 * Total number of bytes used for calibration
	 */
	private static final int CALIBRATION_BYTES = 22;

	// BMP085 EEPROM Registers
	private static final int CALIBRATION_START = 0xAA;
	private static final int CONTROL_REGISTER = 0xF4; // BMP180 control register
	private static final int TEMP_ADDR = 0xF6; // Temperature read address
	private static final int PRESS_ADDR = 0xF6; // Pressure read address

	// Commands
	private static final byte GET_TEMP_CMD = (byte) 0x2E; // Read temperature command
	private static final byte GET_PRESSURE_COMMAND = (byte) 0x34; // Read pressure command

	/**
	 * EEPROM registers - these represent calibration data
	 */
	private short calAC1;
	private short calAC2;
	private short calAC3;
	private int calAC4;
	private int calAC5;
	private int calAC6;
	private short calB1;
	private short calB2;
	private short calMB;
	private short calMC;
	private short calMD;

	// Barometer configuration
	private Mode mode;
	private I2CDeviceInterface i2cDevice;

	/**
	 * Constructor
	 * 
	 * @param mode BMP180 operating mode (low power .. ultra-high resolution)
	 * @throws RuntimeIOException if an I/O error occurs
	 **/
	public BMP180(Mode mode) throws RuntimeIOException {
		this(I2CConstants.CONTROLLER_1, mode);
	}

	public BMP180(int controller, Mode mode) throws RuntimeIOException {
		i2cDevice = I2CDevice.builder(BMP180_ADDR).setController(controller).setByteOrder(ByteOrder.BIG_ENDIAN).build();

		this.mode = mode;
	}

	/**
	 * This method reads the calibration data common for the Temperature sensor and Barometer
	 * sensor included in the BMP180
	 * 
	 * @throws RuntimeIOException if an I/O error occurs
	 **/
	public void readCalibrationData() throws RuntimeIOException {
		// Read all of the calibration data into a byte array
		ByteBuffer calibData = i2cDevice.readI2CBlockDataByteBuffer(CALIBRATION_START, CALIBRATION_BYTES);
		// Read each of the pairs of data as a signed short
		calibData.rewind();
		calAC1 = calibData.getShort();
		calAC2 = calibData.getShort();
		calAC3 = calibData.getShort();

		// Unsigned short values
		calAC4 = calibData.getShort() & 0xffff;
		calAC5 = calibData.getShort() & 0xffff;
		calAC6 = calibData.getShort() & 0xffff;

		// Signed sort values
		calB1 = calibData.getShort();
		calB2 = calibData.getShort();
		calMB = calibData.getShort();
		calMC = calibData.getShort();
		calMD = calibData.getShort();
	}

	private int readRawTemperature() throws RuntimeIOException {
		// Write the read temperature command to the command register
		i2cDevice.writeByteData(CONTROL_REGISTER, GET_TEMP_CMD);

		// Wait 5m before reading the temperature
		SleepUtil.sleepMillis(5);

		// Read uncompressed data
		return i2cDevice.readUShort(TEMP_ADDR);
	}

	/**
	 * Method for reading the temperature. Remember the sensor will provide us with raw data,
	 * and we need to transform in some analysed value to make sense. All the calculations are
	 * normally provided by the manufacturer. In our case we use the calibration data
	 * collected at construction time.
	 *
	 * @return Temperature in Celsius as a double
	 * @throws RuntimeIOException If there is an IO error reading the sensor
	 */
	@Override
	public float getTemperature() throws RuntimeIOException {
		int UT = readRawTemperature();

		// Calculate the actual temperature
		int X1 = ((UT - calAC6) * calAC5) >> 15;
		int X2 = (calMC << 11) / (X1 + calMD);
		int B5 = X1 + X2;

		return ((B5 + 8) >> 4) / 10.0f;
	}

	private int readRawPressure() throws RuntimeIOException {
		// Write the read pressure command to the command register
		i2cDevice.writeByteData(CONTROL_REGISTER, mode.getPressureCommand());

		// Delay before reading the pressure - use the value determined by the
		// sampling mode
		SleepUtil.sleepMillis(mode.getDelay());

		// Read the non-compensated pressure value
		long val = i2cDevice.readUInt(PRESS_ADDR, 3);

		// ((msb << 16) + (lsb << 8) + xlsb) >> (8 - self._mode)

		return (int) (val >> (8 - mode.getSamplingMode()));
	}

	/**
	 * Read the barometric pressure (in hPa) from the device.
	 *
	 * @return Pressure measurement in hPa
	 */
	@Override
	public float getPressure() throws RuntimeIOException {
		int sampling_mode = mode.getSamplingMode();
		int UT = readRawTemperature();
		int UP = readRawPressure();

		// Calculate true temperature coefficient B5.
		int X1 = ((UT - calAC6) * calAC5) >> 15;
		int X2 = (calMC << 11) / (X1 + calMD);
		int B5 = X1 + X2;

		// Calculate the true pressure
		int B6 = B5 - 4000;
		X1 = (calB2 * (B6 * B6) >> 12) >> 11;
		X2 = calAC2 * B6 >> 11;
		int X3 = X1 + X2;
		int B3 = ((((calAC1 * 4) + X3) << sampling_mode) + 2) / 4;
		X1 = calAC3 * B6 >> 13;
		X2 = (calB1 * ((B6 * B6) >> 12)) >> 16;
		X3 = ((X1 + X2) + 2) >> 2;
		int B4 = (calAC4 * (X3 + 32768)) >> 15;
		int B7 = (UP - B3) * (50000 >> sampling_mode);

		int pa;
		// FIXME Vacuous comparison of integer value - 0x80000000 is Math.MIN_VALUE!
		if (B7 < 0x80000000) {
			pa = (B7 * 2) / B4;
		} else {
			pa = (B7 / B4) * 2;
		}

		X1 = (pa >> 8) * (pa >> 8);
		X1 = (X1 * 3038) >> 16;
		X2 = (-7357 * pa) >> 16;

		pa += ((X1 + X2 + 3791) >> 4);

		return pa / 100f;
	}

	@Override
	public void close() {
		i2cDevice.close();
	}
}
