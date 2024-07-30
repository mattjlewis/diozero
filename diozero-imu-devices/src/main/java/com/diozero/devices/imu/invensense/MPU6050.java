/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - IMU device classes
 * Filename:     MPU6050.java
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

package com.diozero.devices.imu.invensense;

import java.util.HashMap;
import java.util.Map;

import org.hipparchus.geometry.Vector;
import org.hipparchus.geometry.euclidean.threed.Vector3D;

import com.diozero.api.I2CDevice;
import com.diozero.api.RuntimeIOException;
import com.diozero.devices.imu.ImuData;
import com.diozero.devices.imu.ImuInterface;
import com.diozero.devices.imu.OrientationListener;
import com.diozero.devices.imu.TapListener;
import com.diozero.util.SleepUtil;

/**
 * The TDK InvenSense MPU6050 is an I2c 6-axis (3 for accelerometer, 3 for gyroscope)
 * processor, with a high-precision temperature sensor.
 * <ul>
 * <li><a href=
 * "https://invensense.tdk.com/wp-content/uploads/2015/02/MPU-6000-Datasheet1.pdf">Spec
 * sheet</a></li>
 * <li><a href=
 * "https://invensense.tdk.com/wp-content/uploads/2015/02/MPU-6000-Register-Map1.pdf">Register
 * map</a></li>
 * </ul>
 */
public class MPU6050 implements ImuInterface {

	public static final int DEFAULT_ADDRESS = 0x68;
	public static final int OTHER_ADDRESS = 0x69;

	// default sensitivities
	private static final float GYRO_SENSITIVITY = 131f;
	private static final float ACCELERATOR_SENSITIVITY = 16384f;

	// temperature values
	private static final float TEMPERATURE_DIVISOR = 340f;
	private static final float TEMPERATURE_OFFSET = 36.53f;

	private final I2CDevice delegate;

	/**
	 * Creates the device on the given controller using the default address.
	 *
	 * @param controller the I2C controller bus
	 */
	public MPU6050(int controller) {
		this(controller, DEFAULT_ADDRESS);
	}

	/**
	 * Creates the device on the given controller and address. (Note that if pin `AD0` is
	 * <b>high</b>, the alternate address is used.)
	 *
	 * @param controller the I2C controller bus
	 * @param address    the I2C address of the sensor
	 */
	public MPU6050(int controller, int address) {
		delegate = I2CDevice.builder(address).setController(controller).build();

		writeConfiguration("Waking up device", Registers.POWER_MANAGEMENT_CONFIG, RegisterValues.WAKEUP);
		writeConfiguration("Configuring sample rate", Registers.SAMPLE_RATE_DIVISOR,
				RegisterValues.DEFAULT_SAMPLE_DIVISOR);
		writeConfiguration("Setting global config (digital low pass filter)", Registers.LOW_PASS_FILTER,
				RegisterValues.LOW_PASS_CONFIG);
		writeConfiguration("Configuring gyroscope", Registers.GYRO_CONFIGURATION,
				RegisterValues.DEFAULT_GYRO_CONFIGURATION);
		writeConfiguration("Configuring accelerometer", Registers.ACCELEROMETER_CONFIGURATION,
				RegisterValues.DEFAULT_ACCELERATOR_CONFIGURATION);
		writeConfiguration("Configuring interrupts", Registers.ENABLE_INTERRUPTS, RegisterValues.INTERRUPT_DISABLED);
		writeConfiguration("Configuring low power operations", Registers.STANDBY_MANAGEMENT_CONFIG,
				RegisterValues.STANDBY_DISABLED);
	}

	@Override
	public void close() throws RuntimeIOException {
		delegate.close();
	}

	/**
	 * Read all registers - this is the _raw_ value.
	 *
	 * @return the bytes for each register
	 */
	public Map<Integer, Byte> dumpRegisters() {
		Map<Integer, Byte> values = new HashMap<>();
		for (int i = 1; i <= 120; i++) {
			byte registerData = delegate.readByteData(i);
			values.put(i, registerData);
		}
		return values;
	}

	@Override
	public String getImuName() {
		return "MPU-6050";
	}

	@Override
	public int getPollInterval() {
		// TODO this is a wild guess
		return 10;
	}

	@Override
	public boolean hasGyro() {
		return true;
	}

	@Override
	public boolean hasAccelerometer() {
		return true;
	}

	@Override
	public boolean hasCompass() {
		return false;
	}

	@Override
	public void startRead() {
		throw new UnsupportedOperationException("Background reads are not supported.");
	}

	@Override
	public void stopRead() {
		throw new UnsupportedOperationException("Background reads are not supported.");
	}

	@Override
	public ImuData getImuData() throws RuntimeIOException {
		return new ImuData(getGyroData(), getAccelerometerData(), null, null, getTemperature(),
				System.currentTimeMillis());
	}

	@Override
	public Vector3D getGyroData() throws RuntimeIOException {
		return new Vector3D(readRegister(Registers.GYRO_X_REGISTER) / GYRO_SENSITIVITY,
				readRegister(Registers.GYRO_Y_REGISTER) / GYRO_SENSITIVITY,
				readRegister(Registers.GYRO_Z_REGISTER) / GYRO_SENSITIVITY);
	}

	@Override
	public Vector3D getAccelerometerData() throws RuntimeIOException {
		return new Vector3D(readRegister(Registers.ACCELERATOR_X_REGISTER) / ACCELERATOR_SENSITIVITY,
				readRegister(Registers.ACCELERATOR_Y_REGISTER) / ACCELERATOR_SENSITIVITY,
				readRegister(Registers.ACCELERATOR_Z_REGISTER) / ACCELERATOR_SENSITIVITY);
	}

	@Override
	public Vector3D getCompassData() throws RuntimeIOException {
		return null;
	}

	@Override
	public void addTapListener(TapListener listener) {
		throw new UnsupportedOperationException("Taps are not supported.");
	}

	@Override
	public void addOrientationListener(OrientationListener listener) {
		throw new UnsupportedOperationException("Background reads are not supported.");
	}

	/**
	 * Get the temperature reading.
	 *
	 * @return the temperature in degrees C
	 */
	public float getTemperature() {
		return (readRegister(Registers.TEMPERATURE_REGISTER) / TEMPERATURE_DIVISOR) + TEMPERATURE_OFFSET;
	}

	/**
	 * Get the device's hardware I2C address.
	 *
	 * @return the I2C address
	 */
	public byte getI2CAddress() {
		return delegate.readByteData(Registers.WHO_AM_I);
	}

	/**
	 * Calibration data for the gyroscope: basically computes the average values.
	 * <p>
	 * <B>THE DEVICE MUST BE STILL UNTIL THIS COMPLETES!!!!</B>
	 * 
	 * <pre>
	 * Example:
	 *    Vector3D adjusted = sensor.getGyroData().subtract(calibrationData);
	 * </pre>
	 * 
	 * @param numberOfReadings how many readings to take
	 * @return the averaged readings for the gyroscope
	 * @see Vector3D#subtract(Vector)
	 */
	public Vector3D calibrateGyro(int numberOfReadings) {
		float x = 0f;
		float y = 0f;
		float z = 0f;

		for (int i = 0; i < numberOfReadings; i++) {
			Vector3D initialSpeeds = getGyroData();
			x += initialSpeeds.getX();
			y += initialSpeeds.getY();
			z += initialSpeeds.getZ();
			SleepUtil.sleepMillis(100);
		}

		return new Vector3D(x / numberOfReadings, y / numberOfReadings, z / numberOfReadings);
	}

	/**
	 * Write and verify a device configuration value.
	 *
	 * @param configurationName name of the config for error messages
	 * @param register          the register to write to
	 * @param data              the configuration data
	 */
	private void writeConfiguration(String configurationName, byte register, byte data) {
		delegate.writeByteData(register, data);
		byte actual = delegate.readByteData(register);
		if (actual != data) {
			throw new RuntimeIOException(
					String.format("%s: Tried to write '%02x' to register %02x, but value is '%02x'", configurationName,
							data, register, actual));
		}
	}

	/**
	 * Read a "double byte" from this register and the next one.
	 *
	 * @param register the register to read from
	 * @return the 2-byte integer value
	 */
	private int readRegister(int register) {
		byte high = delegate.readByteData(register);
		byte low = delegate.readByteData(register + 1);
		int value = (high << 8) + low;
		if (value >= 0x8000) {
			return -(65536 - value);
		}
		return value;
	}

	private interface Registers {
		/**
		 * Set the sample rate (the values are divided by this).
		 */
		byte SAMPLE_RATE_DIVISOR = 0x19;
		/**
		 * Set up the low-pass filter
		 */
		byte LOW_PASS_FILTER = 0x1a;
		/**
		 * Set up the gyroscope
		 */
		byte GYRO_CONFIGURATION = 0x1b;
		/**
		 * Accelerometer configuration
		 */
		byte ACCELEROMETER_CONFIGURATION = 0x1c;
		/**
		 * FIFO (not used)
		 */
		byte FIFO_CONFIGURATION = 0x23;
		/**
		 * Enable interrupts (disabling)
		 */
		byte ENABLE_INTERRUPTS = 0x38;
		/**
		 * Register for high bits of a 2's complement 16-bit value. The low bits are assumed to be
		 * in the next register (this + 1).
		 * <p>
		 * This is the most recent reading from the accelerometer's X axis.
		 */
		byte ACCELERATOR_X_REGISTER = 0x3b;
		/**
		 * Register for high bits of a 2's complement 16-bit value. The low bits are assumed to be
		 * in the next register (this + 1).
		 * <p>
		 * This is the most recent reading from the accelerometer's Y axis.
		 */
		byte ACCELERATOR_Y_REGISTER = 0x3d;
		/**
		 * Register for high bits of a 2's complement 16-bit value. The low bits are assumed to be
		 * in the next register (this + 1).
		 * <p>
		 * This is the most recent reading from the accelerometer's Z axis.
		 */
		byte ACCELERATOR_Z_REGISTER = 0x3f;
		/**
		 * Register for high bits of a 2's complement 16-bit value. The low bits are assumed to be
		 * in the next register (this + 1).
		 * <p>
		 * This is the most recent reading from the temperature sensor.
		 */
		byte TEMPERATURE_REGISTER = 0x41;
		/**
		 * Register for high bits of a 2's complement 16-bit value. The low bits are assumed to be
		 * in the next register (this + 1).
		 * <p>
		 * This is the most recent reading from the gyro's X axis.
		 */
		byte GYRO_X_REGISTER = 0x43;
		/**
		 * Register for high bits of a 2's complement 16-bit value. The low bits are assumed to be
		 * in the next register (this + 1).
		 * <p>
		 * This is the most recent reading from the gyro's Y axis.
		 */
		byte GYRO_Y_REGISTER = 0x45;
		/**
		 * Register for high bits of a 2's complement 16-bit value. The low bits are assumed to be
		 * in the next register (this + 1).
		 * <p>
		 * This is the most recent reading from the gyro's Z axis.
		 */
		byte GYRO_Z_REGISTER = 0x47;
		/**
		 * Resets the various signals.
		 */
		byte SIGNAL_PATH_RESET = 0x68;
		/**
		 * I2C management
		 */
		byte I2C_CONFIG = 0x6a;
		/**
		 * Basic power management
		 */
		byte POWER_MANAGEMENT_CONFIG = 0x6b;
		/**
		 * The I2C address (6-bits)
		 */
		byte WHO_AM_I = 0x75;
		/**
		 * Standby mode management.
		 */
		byte STANDBY_MANAGEMENT_CONFIG = 0x6c;
	}

	private interface RegisterValues {
		/**
		 * Just wakes the device up, because it sets the sleep bit to 0. Also sets the clock
		 * source to internal.
		 */
		byte WAKEUP = 0x0;
		/**
		 * Sets the full scale range of the gyroscopes to ± 2000 °/s
		 */
		byte DEFAULT_GYRO_CONFIGURATION = 0x18;
		/**
		 * Sets the sample rate divider for the gyroscopes and accelerometers. This means<br>
		 * acc-rate = 1kHz / 1+ sample-rate<br>
		 * and <br>
		 * gyro-rate = 8kHz / 1+ sample-rate. <br>
		 * <br>
		 * The concrete value 0 leaves the sample rate on default, which means 1kHz for acc-rate
		 * and 8kHz for gyr-rate.
		 */
		byte DEFAULT_SAMPLE_DIVISOR = 0x0;
		/**
		 * Setting the digital low pass filter to <br>
		 * Acc Bandwidth (Hz) = 184 <br>
		 * Acc Delay (ms) = 2.0 <br>
		 * Gyro Bandwidth (Hz) = 188 <br>
		 * Gyro Delay (ms) = 1.9 <br>
		 * Fs (kHz) = 1
		 */
		byte LOW_PASS_CONFIG = 0x1;
		/**
		 * Setting accelerometer sensitivity to ± 2g
		 */
		byte DEFAULT_ACCELERATOR_CONFIGURATION = 0x0;
		/**
		 * Disabling FIFO buffer
		 */
		byte FIFO_DISABLED = 0x0;
		/**
		 * Disabling interrupts
		 */
		byte INTERRUPT_DISABLED = 0x0;
		/**
		 * Disabling standby modes
		 */
		byte STANDBY_DISABLED = 0x0;
	}
}
