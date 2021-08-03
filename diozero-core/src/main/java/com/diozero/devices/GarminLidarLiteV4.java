package com.diozero.devices;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     GarminLidarLiteV4.java
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

import java.nio.ByteOrder;

import org.tinylog.Logger;

import com.diozero.api.DeviceInterface;
import com.diozero.api.I2CDevice;
import com.diozero.api.RuntimeIOException;
import com.diozero.util.BitManipulation;

/**
 * Full credit: https://github.com/garmin/LIDARLite_Arduino_Library
 */
public class GarminLidarLiteV4 implements DeviceInterface {
	public static final int HARDWARE_REV_A = 0x10;
	public static final int HARDWARE_REV_B = 0x08;

	public enum HardwareRevision {
		A(HARDWARE_REV_A), B(HARDWARE_REV_B), UNKNOWN(-1);

		private byte value;

		HardwareRevision(int value) {
			this.value = (byte) value;
		}

		public byte getValue() {
			return value;
		}

		public static HardwareRevision valueOf(int value) {
			HardwareRevision rev;
			switch (value) {
			case HARDWARE_REV_A:
				rev = HardwareRevision.A;
				break;
			case HARDWARE_REV_B:
				rev = HardwareRevision.B;
				break;
			default:
				rev = HardwareRevision.UNKNOWN;
			}
			return rev;
		}
	}

	private static final byte ASYNC_POWER_MODE = 0x00;
	private static final byte SYNC_POWER_MODE = 0x01;
	private static final byte ALWAYS_ON_POWER_MODE = (byte) 0xff;

	public enum PowerMode {
		/**
		 * The coprocessor is always OFF unless a distance measurement is requested or a
		 * register access is required.
		 */
		ASYNCHRONOUS(ASYNC_POWER_MODE),
		/**
		 * Distance measurement is tied to the ANT channel period. The coprocessor is
		 * turned on and off as required.
		 */
		SYNCHRONOUS(SYNC_POWER_MODE),
		/**
		 * The coprocessor is not turned off, allowing for the fastest measurements
		 * possible.
		 */
		ALWAYS_ON(ALWAYS_ON_POWER_MODE);

		private byte value;

		PowerMode(int value) {
			this.value = (byte) value;
		}

		byte getValue() {
			return value;
		}

		static PowerMode of(byte powerMode) {
			PowerMode pm;
			switch (powerMode) {
			case ASYNC_POWER_MODE:
				pm = ASYNCHRONOUS;
				break;
			case SYNC_POWER_MODE:
				pm = SYNCHRONOUS;
				break;
			case ALWAYS_ON_POWER_MODE:
				pm = ALWAYS_ON;
				break;
			case 127:
				Logger.warn("Got power mode 127...");
				pm = ALWAYS_ON;
				break;
			default:
				throw new IllegalArgumentException("Invalid power mode value: " + powerMode);
			}
			return pm;
		}
	}

	public enum Preset {
		MAXIMUM_RANGE(255, false), BALANCED(128, false), SHORT_RANGE_HIGH_SPEED(24, true),
		MID_RANGE_HIGH_SPEED(128, true), MAX_RANGE_HIGH_SPEED(255, true), VERY_SHORT_RANGE_HIGH_SPEED(4, true);

		private int maxAcquisitionCount;
		private boolean quickTerminationEnabled;

		Preset(int maxAcquisitionCount, boolean quickTerminationEnabled) {
			this.maxAcquisitionCount = maxAcquisitionCount;
			this.quickTerminationEnabled = quickTerminationEnabled;
		}

		public int getMaxAcquisitionCount() {
			return maxAcquisitionCount;
		}

		public boolean isQuickTerminationEnabled() {
			return quickTerminationEnabled;
		}
	}

	private static final int DEFAULT_ADDRESS = 0x62;

	// Register address
	/** Device command (W) */
	private static final int ACQ_COMMANDS = 0x00;
	/** System status (R) */
	private static final int STATUS = 0x01;
	/** Maximum acquisition count (R/W) */
	private static final int ACQUISITION_COUNT = 0x05;
	/** Distance measurement low byte (R) */
	private static final int FULL_DELAY_LOW = 0x10;
	// Distance measurement high byte (R)
	// private static final int FULL_DELAY_HIGH = 0x11;
	/** Unit ID, byte 0 (R) */
	private static final int UNIT_ID_0 = 0x16;
	// Unit ID, byte 1 (R)
	// private static final int UNIT_ID_1 = 0x17;
	// Unit ID, byte 2 (R)
	// private static final int UNIT_ID_2 = 0x18;
	// Unit ID, byte 3 (R)
	// private static final int UNIT_ID_3 = 0x19;
	/** Write new I2C address after unlock (R/W) */
	private static final int I2C_SEC_ADDR = 0x1a;
	/** Default address response control (W) */
	private static final int I2C_CONFIG = 0x1b;
	/** Peak detection threshold bypass (R/W) */
	private static final int DETECTION_SENSITIVITY = 0x1c;
	/** Read Garmin software library version string (R) */
	private static final int LIB_VERSION = 0x30;
	/** Correlation record data control; up to 192 bytes (R/W) */
	private static final int CORR_DATA = 0x52;
	/** Coprocessor firmware version low byte (R) */
	private static final int CP_VER_LO = 0x72;
	// Coprocessor firmware version high byte (R)
	// private static final int CP_VER_HI = 0x73;
	/** Board temperature (R) */
	private static final int BOARD_TEMPERATURE = 0xe0;
	/** Board hardware version (R) */
	private static final int HARDWARE_VERSION = 0xe1;
	/** Power state control (R/W) */
	private static final int POWER_MODE = 0xe2;
	/** Automatic measurement rate (R/W). Ignore - ANT only? */
	private static final int MEASUREMENT_INTERVAL = 0xe3;
	/** Reset default settings (W) */
	private static final int FACTORY_RESET = 0xe4;
	/** Quick acquisition termination (R/W) */
	private static final int QUICK_TERMINATION = 0xe5;
	/** Start secure Bluetooth LE bootloader (W) */
	private static final int START_BOOTLOADER = 0xe6;
	/** Store register settings (R/W) */
	private static final int ENABLE_FLASH_STORAGE = 0xea;
	/** Improved accuracy setting (R/W) */
	private static final int HIGH_ACCURACY_MODE = 0xeb;
	/** SoC temperature (R) */
	private static final int SOC_TEMPERATURE = 0xec;

	// ACQ Commands
	private static final byte RECEIVER_BIAS_CORRECTION_DISABLED = 0x03;
	private static final byte RECEIVER_BIAS_CORRECTION_ENABLED = 0x04;

	// STATUS flag bits
	public static class Status {
		/** 1: Device is busy taking a measurement or powering on */
		public static final byte BUSY_BIT = 0;
		/**
		 * 1: Signal data in correlation record has reached the maximum value before
		 * overflow
		 */
		public static final byte SIGNAL_OVERFLOW_BIT = 1;
		/**
		 * 1: Reference data in correlation record has reached the maximum value before
		 * overflow
		 */
		public static final byte REFDATA_OVERFLOW_BIT = 2;
		/** 1: device is in low power mode */
		public static final byte LOW_POWER_BIT = 3;
		/** 1: noise is within tolerance */
		public static final byte DC_BIAS_DONE_BIT = 4;
		/** 1: Error detected */
		public static final byte DC_ERROR_BIT = 5;

		private byte value;

		Status(byte value) {
			this.value = value;
		}

		public byte getValue() {
			return value;
		}

		/**
		 * Device busy status.
		 *
		 * @return true if the device is busy taking a measurement or powering on
		 */
		public boolean isDeviceBusy() {
			return BitManipulation.isBitSet(value, BUSY_BIT);
		}

		/**
		 * Signal overflow flag.
		 *
		 * @return true if the signal data in correlation record has reached the maximum
		 *         value before overflow (this occurs with a strong received signal
		 *         strength)
		 */
		public boolean isSignalOverflow() {
			return BitManipulation.isBitSet(value, SIGNAL_OVERFLOW_BIT);
		}

		/**
		 * Reference overflow flag.
		 *
		 * @return reue if reference data in correlation record has reached the maximum
		 *         value before overflow (this occurs when taking measurements with
		 *         biasing enabled)
		 */
		public boolean isReferenceDataOverflow() {
			return BitManipulation.isBitSet(value, REFDATA_OVERFLOW_BIT);
		}

		/**
		 * Low power flag.
		 * <dl>
		 * <dt>0</dt>
		 * <dd>Device is powered on. I2C commands can be issued at a normal rate.</dd>
		 * <dt>1</dt>
		 * <dd>The device is in low power mode. To allow the device to power on and
		 * perform the I2C command, a 10ms delay after each command is recommended.</dd>
		 * </dl>
		 *
		 * @return true if the device is in low power mode
		 */
		public boolean isInLowerPowerMode() {
			return BitManipulation.isBitSet(value, LOW_POWER_BIT);
		}

		/**
		 * Data correlation noise bias status.
		 * <dl>
		 * <dt>false</dt>
		 * <dd>The device is performing automatic DC noise bias corrections.</dd>
		 * <dt>true</dt>
		 * <dd>DC noise is within tolerance, and the automatic DC noise bias corrections
		 * are currently idle.</dd>
		 * </dl>
		 *
		 * @return the data correlation noise bias status
		 */
		public boolean isDataCorrelationNoiseBiasDone() {
			return BitManipulation.isBitSet(value, DC_BIAS_DONE_BIT);
		}

		/**
		 * Data correlation noise bias error flag.
		 *
		 * @return true if an error was detected in correcting DC noise bias, and
		 *         distance measurements are expected to be inaccurate
		 */
		public boolean isDataCorrelationNoiseBiasError() {
			return BitManipulation.isBitSet(value, DC_ERROR_BIT);
		}
	}

	private static final int MAX_CORELATION_DATA_READINGS = 192;
	private static final byte QUICK_TERMINATION_ENABLED = 0x00;
	private static final byte QUICK_TERMINATION_DISABLED = 0x08;
	private static final byte RAM_STORAGE = 0x0;
	private static final byte FLASH_STORAGE = 0x11;
	private static final byte HIGH_ACCURACY_MODE_DISABLED = 0x00;

	private I2CDevice device;

	public GarminLidarLiteV4() {
		device = I2CDevice.builder(DEFAULT_ADDRESS).setByteOrder(ByteOrder.LITTLE_ENDIAN).build();
	}

	/**
	 * Get the device's unit id (serial number).
	 *
	 * @return the device's unit id (serial number)
	 */
	public byte[] getUnitId() {
		byte[] unit_id = new byte[4];
		int read = device.readI2CBlockData(UNIT_ID_0, unit_id);
		if (read != unit_id.length) {
			Logger.warn("Expected {} bytes, read {}", Integer.valueOf(unit_id.length), Integer.valueOf(read));
		}
		return unit_id;
	}

	/**
	 * Get the device's hardware version identifier.
	 *
	 * @return hardware version
	 */
	public int getHardwareVersion() {
		return device.readByteData(HARDWARE_VERSION) & 0xff;
	}

	/**
	 * Get the device's coprocessor firmware version number. 210 == v2.10
	 *
	 * @return coprocessor firmware version
	 */
	public int getFirmwareVersion() {
		return device.readUShort(CP_VER_LO);
	}

	/**
	 * Get the Garmin software library version string
	 *
	 * @return Garmin software library version string
	 */
	public String getLibraryVersion() {
		byte[] buffer = new byte[11];
		int read = device.readI2CBlockData(LIB_VERSION, buffer);
		if (read != buffer.length) {
			Logger.warn("Expected {} bytes, read {}", Integer.valueOf(buffer.length), Integer.valueOf(read));
		}
		return new String(buffer);
	}

	/**
	 * Resets the NVM/Flash storage information back to default settings and
	 * executes a SoftDevice reset.
	 */
	public void factoryReset() {
		device.writeByteData(FACTORY_RESET, 0x01);
	}

	/**
	 * Read the board's temperature in degrees Celsius.
	 *
	 * @return the board temperature in degrees Celsius
	 */
	public int getBoardTemperature() {
		return device.readByteData(BOARD_TEMPERATURE) & 0xff;
	}

	/**
	 * Read the temperature of the nRF SoC in degrees Celsius.
	 *
	 * @return the board temperature in degrees Celsius
	 */
	public int getSoCTemperature() {
		return device.readByteData(SOC_TEMPERATURE) & 0xff;
	}

	/**
	 * Read the STATUS register byte value.
	 *
	 * @return status register byte value
	 */
	public byte getStatusByte() {
		return device.readByteData(STATUS);
	}

	/**
	 * Read the STATUS register byte and wrap in a {@link Status} object.
	 *
	 * @return status register value
	 */
	public Status getStatus() {
		return new Status(getStatusByte());
	}

	/**
	 * Read the STATUS register to determine if the device is busy or not
	 *
	 * @return true if the device is busy taking a measurement or powering on
	 */
	public boolean isDeviceBusy() {
		return BitManipulation.isBitSet(getStatusByte(), Status.BUSY_BIT);
	}

	/**
	 * Get the {@link PowerMode}
	 *
	 * @return the {@link PowerMode}
	 */
	public PowerMode getPowerMode() {
		return PowerMode.of(device.readByteData(POWER_MODE));
	}

	/**
	 * NOTE: You must disable HIGH_ACCURACY_MODE before you adjust the power mode.
	 *
	 * @param powerMode the new power mode
	 */
	public void setPowerMode(PowerMode powerMode) {
		if (isHighAccuracyModeEnabled()) {
			throw new IllegalArgumentException("High accuracy mode must be disabled before adjusting the power mode");
		}
		device.writeByteData(POWER_MODE, powerMode.getValue());
	}

	/**
	 * Storage to be used for register settings.
	 *
	 * @return whether or not to use Flash/NVM storage for registry settings
	 */
	public boolean isFlashStorageEnabled() {
		return device.readByteData(ENABLE_FLASH_STORAGE) == FLASH_STORAGE;
	}

	/**
	 * <p>
	 * Storage for register settings.
	 * </p>
	 * <p>
	 * <strong>NOTE</strong>: Use caution when enabling flash storage. The total
	 * number of writes and erases is limited to 10,000.
	 * </p>
	 *
	 * <dl>
	 * <dt>Enabled</dt>
	 * <dd>Use RAM storage only. When the device is reset, default values are
	 * loaded.</dd>
	 * <dt>Disabled</dt>
	 * <dd>Use FLASH/NVM storage. Any register that supports both read and write
	 * operations is stored in NVM and persists over power cycles. When the device
	 * is reset, the values stored in NVM are loaded instead of the default
	 * values.</dd>
	 * </dl>
	 *
	 * @param enabled whether or not to use Flash/NVM storage for register settings
	 */
	public void setFlashStorage(boolean enabled) {
		device.writeByteData(FLASH_STORAGE, enabled ? FLASH_STORAGE : RAM_STORAGE);
	}

	/**
	 * Get the high accuracy mode. While high accuracy mode is disabled, you can
	 * adjust the {@link PowerMode} to {@link PowerMode.ASYNCHRONOUS Asynchronous}
	 * or {@link PowerMode.SYNCHRONOUS Synchronous} if required.
	 *
	 * <dl>
	 * <dt>0x00</dt>
	 * <dd>High accuracy mode is disabled</dd>
	 * <dt>0x01 to 0xFF</dt>
	 * <dd>High accuracy mode is enabled. The value is used as the number of
	 * distance measurements to accumulate and average before returning them to the
	 * user.</dd>
	 * </dl>
	 *
	 * @return the number of distance measurements to accumulate and average before
	 *         returning them to the user (0 == disabled)
	 */
	public int getHighAccuracyMode() {
		return device.readByteData(HIGH_ACCURACY_MODE) & 0xff;
	}

	/**
	 * Check if high accuracy mode is enabled.
	 *
	 * @return high accuracy mode status
	 */
	public boolean isHighAccuracyModeEnabled() {
		return getHighAccuracyMode() != HIGH_ACCURACY_MODE_DISABLED;
	}

	/**
	 * Set the high accuracy mode. While high accuracy mode is disabled, you can
	 * adjust the {@link PowerMode} to {@link PowerMode.ASYNCHRONOUS Asynchronous}
	 * or {@link PowerMode.SYNCHRONOUS Synchronous} if required.
	 *
	 * <dl>
	 * <dt>0x00</dt>
	 * <dd>High accuracy mode is disabled</dd>
	 * <dt>0x01 to 0xFF</dt>
	 * <dd>Enable high accuracy mode. The value is used as the number of distance
	 * measurements to accumulate and average before returning them to the
	 * user.</dd>
	 * </dl>
	 *
	 * Note you must set the {@link PowerMode} to {@link PowerMode.ALWAYS_ON Always
	 * On} before you adjust to a non-zero value.
	 *
	 * @param accuracyMode the number of distance measurements to accumulate and
	 *                     average before returning them to the user (0 == disabled)
	 */
	public void setHighAccuracyMode(int accuracyMode) {
		if (accuracyMode < 0 || accuracyMode > 255) {
			throw new IllegalArgumentException("Invalid high accuracy mode, must be 0..255");
		}
		if (accuracyMode != 0 && getPowerMode() != PowerMode.ALWAYS_ON) {
			throw new IllegalArgumentException(
					"PowerMode must be set to ALWAYS_ON prior to setting a non-zero accuracy mode");
		}
		device.writeByteData(HIGH_ACCURACY_MODE, accuracyMode);
	}

	public void disableHighAccuracyMode() {
		setHighAccuracyMode(HIGH_ACCURACY_MODE_DISABLED);
	}

	/**
	 * Enable / disable the distance measurement receiver bias correction.
	 *
	 * @param enabled receiver bias correction status
	 */
	public void setReceivedBiasCorrectionEnabled(boolean enabled) {
		device.writeByteData(ACQ_COMMANDS,
				enabled ? RECEIVER_BIAS_CORRECTION_ENABLED : RECEIVER_BIAS_CORRECTION_DISABLED);
	}

	/**
	 * Get the maximum number of acquisitions during measurement
	 *
	 * @return maximum acquisition count.
	 */
	public int getMaximumAcquisitionCount() {
		return (short) (device.readByteData(ACQUISITION_COUNT) & 0xff);
	}

	/**
	 * Set the maximum number of acquisitions during measurement.
	 *
	 * @param acquisitionCount maximum acquisition count.
	 */
	public void setMaximumAcquisitionCount(int acquisitionCount) {
		// FIXME Should this be 192?
		if (acquisitionCount < 0 || acquisitionCount > 255) {
			throw new IllegalArgumentException("Invalid acquisition count value, must be 0..255");
		}
		device.writeByteData(ACQUISITION_COUNT, (byte) acquisitionCount);
	}

	/**
	 * <p>
	 * Get the detection sensitivity.
	 * </p>
	 *
	 * <dl>
	 * <dt>0x00</dt>
	 * <dd>Use default valid measurement detection algorithm based on the peak
	 * value, signal strength, and noise in the correlation record.</dd>
	 * <dt>0x01 to 0xFF</dt>
	 * <dd>Set simple threshold for valid measurement detection.</dd>
	 * </dl>
	 * <p>
	 * Values 0x20 to 0x60 generally perform well.
	 * </p>
	 *
	 * @param detectionSensitivity detection sensitivity value (0..255)
	 */
	public int getDetectionSensitivity() {
		return device.readByteData(DETECTION_SENSITIVITY) & 0xff;
	}

	/**
	 * <p>
	 * Set the detection sensitivity.
	 * </p>
	 *
	 * <dl>
	 * <dt>0x00</dt>
	 * <dd>Use default valid measurement detection algorithm based on the peak
	 * value, signal strength, and noise in the correlation record.</dd>
	 * <dt>0x01 to 0xFF</dt>
	 * <dd>Set simple threshold for valid measurement detection.</dd>
	 * </dl>
	 * <p>
	 * Values 0x20 to 0x60 generally perform well.
	 * </p>
	 *
	 * @param detectionSensitivity detection sensitivity value (0..255)
	 */
	public void setDetectionSensitivity(int detectionSensitivity) {
		if (detectionSensitivity < 0 || detectionSensitivity > 255) {
			throw new IllegalArgumentException(
					"Invalid detection sensitivity value (" + detectionSensitivity + ") must be 0..255");
		}
		device.writeByteData(DETECTION_SENSITIVITY, (byte) detectionSensitivity);
	}

	/**
	 * Get the status of the quick acquisition termination flag. If enabled the
	 * device terminates the distance measurement early if it anticipates the signal
	 * peak in the correlation record will reach the maximum value.
	 *
	 * @return acquisition quick termination status
	 */
	public boolean isQuickAcquistionTerminationEnabled() {
		byte val = device.readByteData(QUICK_TERMINATION);
		if (val != QUICK_TERMINATION_ENABLED && val != QUICK_TERMINATION_DISABLED) {
			Logger.warn("Unrecognised quick termination value ({})", Byte.valueOf(val));
		}
		return val == QUICK_TERMINATION_ENABLED;
	}

	/**
	 * Set the quick acquisition termination flag. If enabled the device terminates
	 * the distance measurement early if it anticipates the signal peak in the
	 * correlation record will reach the maximum value.
	 *
	 * @param enabled quick acquisition termination value
	 */
	public void setQuickTerminationEnabled(boolean enabled) {
		device.writeByteData(QUICK_TERMINATION, enabled ? QUICK_TERMINATION_ENABLED : QUICK_TERMINATION_DISABLED);
	}

	/**
	 * <p>
	 * The correlation record used to calculate distance can be read from the
	 * device. It has a bipolar wave shape, transitioning from a positive going
	 * portion to a roughly symmetrical negative going pulse. The point where the
	 * signal crosses zero represents the effective delay for the reference and
	 * return signals.
	 * </p>
	 * <p>
	 * Process:
	 * </p>
	 * <ol>
	 * <li>Take a distance reading (there is no correlation record without at least
	 * one distance reading being taken)</li>
	 * <li>For as many points as you want to read from the record (max is 192) read
	 * the two byte signed correlation data point.</li>
	 * </ol>
	 *
	 * @param numberOfReadings The number of readings to take up to a maximum of 192
	 * @return the correlation data
	 */
	public short[] getCorrelationData(int numberOfReadings) {
		if (numberOfReadings < 0 || numberOfReadings > MAX_CORELATION_DATA_READINGS) {
			throw new IllegalArgumentException(
					"Invalid number of readings (" + numberOfReadings + ") must be 0.." + MAX_CORELATION_DATA_READINGS);
		}

		// The memory index is incremented automatically, and successive two-byte reads
		// produce sequential data.
		short[] readings = new short[numberOfReadings];
		for (int i = 0; i < numberOfReadings; i++) {
			readings[i] = device.readShort(CORR_DATA);
		}
		return readings;
	}

	/**
	 * Reset the correlation data pointer
	 */
	public void resetCorrelationData() {
		device.writeByteData(CORR_DATA, 0);
	}

	/**
	 * Selects one of several preset configurations as per the Garmin Arduino
	 * library <a href=
	 * "https://github.com/garmin/LIDARLite_Arduino_Library/blob/master/src/LIDARLite_v4LED.cpp#L55">configure</a>
	 * function.
	 *
	 * @param preset
	 */
	public void configure(Preset preset) {
		setMaximumAcquisitionCount(preset.getMaxAcquisitionCount());
		setQuickTerminationEnabled(preset.isQuickTerminationEnabled());
	}

	/**
	 * Get the distance measurement result in centimetres.
	 *
	 * @return distance measurement result in centimetres
	 */
	public int getDistanceMeasurement() {
		return device.readUShort(FULL_DELAY_LOW);
	}

	/**
	 * Take a single distance measurement reading in centimetres.
	 *
	 * @return distance in centimetres
	 * @throws InterruptedException if interrupted while taking the reading
	 */
	public int getSingleReading() throws InterruptedException {
		device.writeByteData(ACQ_COMMANDS, RECEIVER_BIAS_CORRECTION_ENABLED);
		long start_ms = System.currentTimeMillis();
		while (true) {
			if (!isDeviceBusy()) {
				break;
			}
			Thread.sleep(5);
		}
		int duration_ms = (int) (System.currentTimeMillis() - start_ms);
		Logger.debug("Took {} ms for reading to become available", Integer.valueOf(duration_ms));

		return getDistanceMeasurement();
	}

	@Override
	public void close() throws RuntimeIOException {
		disableHighAccuracyMode();
		setPowerMode(PowerMode.ASYNCHRONOUS);

		if (device != null) {
			device.close();
		}
	}
}
