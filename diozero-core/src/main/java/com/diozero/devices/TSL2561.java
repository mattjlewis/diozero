package com.diozero;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 diozero
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

import java.io.Closeable;
import java.nio.ByteOrder;

import com.diozero.api.I2CConstants;
import com.diozero.api.I2CDevice;
import com.diozero.api.LuminositySensorInterface;
import com.diozero.util.RuntimeIOException;
import com.diozero.util.SleepUtil;

/**
 * <a href="https://cdn-shop.adafruit.com/datasheets/TSL2561.pdf">Datasheet</a>
 */
@SuppressWarnings("unused")
public class TSL2561 implements Closeable, LuminositySensorInterface {
	private static final int TSL2561_VISIBLE = 2; // channel 0 - channel 1
	private static final int TSL2561_INFRARED = 1; // channel 1
	private static final int TSL2561_FULLSPECTRUM = 0; // channel 0
	//
	// Device address for TSL2561
	private static final int DEVICE_ADDR = 0x39; // Default address (pin left floating)

	// Lux calculations differ slightly for CS package
	public static enum TSL2561Package {
		CHIP_SCALE,
		T_FN_CL;
	}

	private static final int TSL2561_COMMAND_BIT = 0x80; // Must be 1
	private static final int TSL2561_CLEAR_BIT = 0x40; // Clears any pending interrupt (write 1 to clear)
	private static final int TSL2561_WORD_BIT = 0x20; // 1 = read/write word (rather than byte)
	private static final int TSL2561_BLOCK_BIT = 0x10; // 1 = using block read/write

	private static final byte TSL2561_CONTROL_POWERON = 0x03;
	private static final byte TSL2561_CONTROL_POWEROFF = 0x00;

	private static final int TSL2561_LUX_LUXSCALE = 14; // Scale by 2^14
	private static final int TSL2561_LUX_RATIOSCALE = 9; // Scale ratio by 2^9
	private static final int TSL2561_LUX_CHSCALE = 10; // Scale channel values by 2^10
	private static final int TSL2561_LUX_CHSCALE_TINT0 = 0x7517; // 322/11 * 2^TSL2561_LUX_CHSCALE
	private static final int TSL2561_LUX_CHSCALE_TINT1 = 0x0FE7; // 322/81 * 2^TSL2561_LUX_CHSCALE

	// T, FN and CL package values
	private static final int TSL2561_LUX_K1T = 0x0040; // 0.125 * 2^RATIO_SCALE
	private static final int TSL2561_LUX_B1T = 0x01f2; // 0.0304 * 2^LUX_SCALE
	private static final int TSL2561_LUX_M1T = 0x01be; // 0.0272 * 2^LUX_SCALE
	private static final int TSL2561_LUX_K2T = 0x0080; // 0.250 * 2^RATIO_SCALE
	private static final int TSL2561_LUX_B2T = 0x0214; // 0.0325 * 2^LUX_SCALE
	private static final int TSL2561_LUX_M2T = 0x02d1; // 0.0440 * 2^LUX_SCALE
	private static final int TSL2561_LUX_K3T = 0x00c0; // 0.375 * 2^RATIO_SCALE
	private static final int TSL2561_LUX_B3T = 0x023f; // 0.0351 * 2^LUX_SCALE
	private static final int TSL2561_LUX_M3T = 0x037b; // 0.0544 * 2^LUX_SCALE
	private static final int TSL2561_LUX_K4T = 0x0100; // 0.50 * 2^RATIO_SCALE
	private static final int TSL2561_LUX_B4T = 0x0270; // 0.0381 * 2^LUX_SCALE
	private static final int TSL2561_LUX_M4T = 0x03fe; // 0.0624 * 2^LUX_SCALE
	private static final int TSL2561_LUX_K5T = 0x0138; // 0.61 * 2^RATIO_SCALE
	private static final int TSL2561_LUX_B5T = 0x016f; // 0.0224 * 2^LUX_SCALE
	private static final int TSL2561_LUX_M5T = 0x01fc; // 0.0310 * 2^LUX_SCALE
	private static final int TSL2561_LUX_K6T = 0x019a; // 0.80 * 2^RATIO_SCALE
	private static final int TSL2561_LUX_B6T = 0x00d2; // 0.0128 * 2^LUX_SCALE
	private static final int TSL2561_LUX_M6T = 0x00fb; // 0.0153 * 2^LUX_SCALE
	private static final int TSL2561_LUX_K7T = 0x029a; // 1.3 * 2^RATIO_SCALE
	private static final int TSL2561_LUX_B7T = 0x0018; // 0.00146 * 2^LUX_SCALE
	private static final int TSL2561_LUX_M7T = 0x0012; // 0.00112 * 2^LUX_SCALE
	private static final int TSL2561_LUX_K8T = 0x029a; // 1.3 * 2^RATIO_SCALE
	private static final int TSL2561_LUX_B8T = 0x0000; // 0.000 * 2^LUX_SCALE
	private static final int TSL2561_LUX_M8T = 0x0000; // 0.000 * 2^LUX_SCALE

	// CS package values
	private static final int TSL2561_LUX_K1C = 0x0043; // 0.130 * 2^RATIO_SCALE
	private static final int TSL2561_LUX_B1C = 0x0204; // 0.0315 * 2^LUX_SCALE
	private static final int TSL2561_LUX_M1C = 0x01ad; // 0.0262 * 2^LUX_SCALE
	private static final int TSL2561_LUX_K2C = 0x0085; // 0.260 * 2^RATIO_SCALE
	private static final int TSL2561_LUX_B2C = 0x0228; // 0.0337 * 2^LUX_SCALE
	private static final int TSL2561_LUX_M2C = 0x02c1; // 0.0430 * 2^LUX_SCALE
	private static final int TSL2561_LUX_K3C = 0x00c8; // 0.390 * 2^RATIO_SCALE
	private static final int TSL2561_LUX_B3C = 0x0253; // 0.0363 * 2^LUX_SCALE
	private static final int TSL2561_LUX_M3C = 0x0363; // 0.0529 * 2^LUX_SCALE
	private static final int TSL2561_LUX_K4C = 0x010a; // 0.520 * 2^RATIO_SCALE
	private static final int TSL2561_LUX_B4C = 0x0282; // 0.0392 * 2^LUX_SCALE
	private static final int TSL2561_LUX_M4C = 0x03df; // 0.0605 * 2^LUX_SCALE
	private static final int TSL2561_LUX_K5C = 0x014d; // 0.65 * 2^RATIO_SCALE
	private static final int TSL2561_LUX_B5C = 0x0177; // 0.0229 * 2^LUX_SCALE
	private static final int TSL2561_LUX_M5C = 0x01dd; // 0.0291 * 2^LUX_SCALE
	private static final int TSL2561_LUX_K6C = 0x019a; // 0.80 * 2^RATIO_SCALE
	private static final int TSL2561_LUX_B6C = 0x0101; // 0.0157 * 2^LUX_SCALE
	private static final int TSL2561_LUX_M6C = 0x0127; // 0.0180 * 2^LUX_SCALE
	private static final int TSL2561_LUX_K7C = 0x029a; // 1.3 * 2^RATIO_SCALE
	private static final int TSL2561_LUX_B7C = 0x0037; // 0.00338 * 2^LUX_SCALE
	private static final int TSL2561_LUX_M7C = 0x002b; // 0.00260 * 2^LUX_SCALE
	private static final int TSL2561_LUX_K8C = 0x029a; // 1.3 * 2^RATIO_SCALE
	private static final int TSL2561_LUX_B8C = 0x0000; // 0.000 * 2^LUX_SCALE
	private static final int TSL2561_LUX_M8C = 0x0000; // 0.000 * 2^LUX_SCALE
	// Auto-gain thresholds
	private static final int TSL2561_AGC_THI_13MS = 4850; // Max value at Ti 13ms = 5047
	private static final int TSL2561_AGC_TLO_13MS = 100;
	private static final int TSL2561_AGC_THI_101MS = 36000; // Max value at Ti 101ms = 37177
	private static final int TSL2561_AGC_TLO_101MS = 200;
	private static final int TSL2561_AGC_THI_402MS = 63000; // Max value at Ti 402ms = 65535
	private static final int TSL2561_AGC_TLO_402MS = 500;

	// Clipping thresholds
	private static final int TSL2561_CLIPPING_13MS = 4900;
	private static final int TSL2561_CLIPPING_101MS = 37000;
	private static final int TSL2561_CLIPPING_402MS = 65000;

	private static final int TSL2561_REGISTER_CONTROL = 0x00;
	private static final int TSL2561_REGISTER_TIMING = 0x01;
	private static final int TSL2561_REGISTER_THRESHHOLDL_LOW = 0x02;
	private static final int TSL2561_REGISTER_THRESHHOLDL_HIGH = 0x03;
	private static final int TSL2561_REGISTER_THRESHHOLDH_LOW = 0x04;
	private static final int TSL2561_REGISTER_THRESHHOLDH_HIGH = 0x05;
	private static final int TSL2561_REGISTER_INTERRUPT = 0x06;
	private static final int TSL2561_REGISTER_CRC = 0x08;
	private static final int TSL2561_REGISTER_ID = 0x0A;
	private static final int TSL2561_REGISTER_CHAN0_LOW = 0x0C;
	private static final int TSL2561_REGISTER_CHAN0_HIGH = 0x0D;
	private static final int TSL2561_REGISTER_CHAN1_LOW = 0x0E;
	private static final int TSL2561_REGISTER_CHAN1_HIGH = 0x0F;

	private static final int TSL2561_INTEGRATIONTIME_13MS = 0x00; // 13.7ms
	private static final int TSL2561_INTEGRATIONTIME_101MS = 0x01; // 101ms
	private static final int TSL2561_INTEGRATIONTIME_402MS = 0x02; // 402ms

	private static final int TSL2561_GAIN_1X = 0x00; // No gain
	private static final int TSL2561_GAIN_16X = 0x10; // 16x gain

	private boolean initialised;
	private boolean autoGain;
	private int integrationTime;
	private int gain;
	private int broadband;
	private int ir;
	private TSL2561Package tsl2561Package;
	private I2CDevice i2cDevice;
	
	public TSL2561(TSL2561Package tsl2561Package) throws RuntimeIOException {
		this(I2CConstants.BUS_1, I2CConstants.ADDR_SIZE_7, I2CConstants.DEFAULT_CLOCK_FREQUENCY, tsl2561Package);
	}
	
	public TSL2561(int controllerNumber, int addressSize, int clockFreq, TSL2561Package tsl2561Package) throws RuntimeIOException {
		i2cDevice = new I2CDevice(controllerNumber, DEVICE_ADDR, addressSize, clockFreq);
		this.tsl2561Package = tsl2561Package;
		initialised = false;
		autoGain = false;
		integrationTime = TSL2561_INTEGRATIONTIME_13MS;
		gain = TSL2561_GAIN_1X;
		broadband = 0;
		ir = 0;
	}

	/**
	 * Enables or disables the auto-gain settings when reading data from the sensor
	 * @param autoGain enable/disable
	 */
	public void setAutoGain(boolean autoGain) {
		this.autoGain = autoGain;
	}

	private boolean begin() throws RuntimeIOException {
		int x = i2cDevice.readByte(TSL2561_REGISTER_ID);
		// if not(x & 0x0A):
		if ((x & 0x0A) == 0) {
			return false;
		}

		initialised = true;

		// Set default integration time and gain
		setIntegrationTime(integrationTime);
		setGain(gain);

		// Note by default the device is in power down mode on bootup
		disable();

		return true;
	}

	/**
	 * Enables the device
	 */
	private void enable() throws RuntimeIOException {
		i2cDevice.writeByte(TSL2561_COMMAND_BIT | TSL2561_REGISTER_CONTROL, TSL2561_CONTROL_POWERON);
	}

	/**
	 * Disables the device (putting it in lower power sleep mode)
	 */
	private void disable() throws RuntimeIOException {
		i2cDevice.writeByte(TSL2561_COMMAND_BIT | TSL2561_REGISTER_CONTROL, TSL2561_CONTROL_POWEROFF);
	}

	/**
	 * Private function to read luminosity on both channels
	 */
	private void getData() throws RuntimeIOException {
		enable();

		// Wait x ms for ADC to complete */
		if (integrationTime == TSL2561_INTEGRATIONTIME_13MS) {
			SleepUtil.sleepSeconds(0.014);
		} else if (integrationTime == TSL2561_INTEGRATIONTIME_101MS) {
			SleepUtil.sleepSeconds(0.102);
		} else {
			SleepUtil.sleepSeconds(0.403);
		}

		// Reads a two byte value from channel 0 (visible + infrared)
		broadband = i2cDevice.readUShort(TSL2561_COMMAND_BIT | TSL2561_WORD_BIT | TSL2561_REGISTER_CHAN0_LOW,
				I2CConstants.SUB_ADDRESS_SIZE_1_BYTE, ByteOrder.LITTLE_ENDIAN);

		// Reads a two byte value from channel 1 (infrared)
		ir = i2cDevice.readUShort(TSL2561_COMMAND_BIT | TSL2561_WORD_BIT | TSL2561_REGISTER_CHAN1_LOW,
				I2CConstants.SUB_ADDRESS_SIZE_1_BYTE, ByteOrder.LITTLE_ENDIAN);

		// Turn the device off to save power
		disable();
	}

	void setIntegrationTime(int time) throws RuntimeIOException {
		// Enable the device by setting the control bit to 0x03
		enable();

		// Update the timing register
		i2cDevice.writeByte(TSL2561_COMMAND_BIT | TSL2561_REGISTER_TIMING, (byte) (time | gain));

		integrationTime = time;

		// Turn the device off to save power
		disable();
	}

	/**
	 * Adjusts the gain on the TSL2561 (adjusts the sensitivity to light)
	 * @param gain gain value
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void setGain(int gain) throws RuntimeIOException {
		// Enable the device by setting the control bit to 0x03
		enable();

		// Update the timing register
		i2cDevice.writeByte(TSL2561_COMMAND_BIT | TSL2561_REGISTER_TIMING, (byte) (integrationTime | gain));

		this.gain = gain;

		// Turn the device off to save power
		disable();
	}

	private void getRawLuminosity() throws RuntimeIOException {
		if (!initialised) {
			begin();
		}

		// If auto gain disabled get a single reading and continue
		if (!autoGain) {
			getData();
			return;
		}

		boolean valid = false;
		boolean auto_gain_check = false;
		while (!valid) {
			int hi;
			int lo;
			// Get the hi/low threshold for the current integration time
			if (integrationTime == TSL2561_INTEGRATIONTIME_13MS) {
				hi = TSL2561_AGC_THI_13MS;
				lo = TSL2561_AGC_TLO_13MS;
			} else if (integrationTime == TSL2561_INTEGRATIONTIME_101MS) {
				hi = TSL2561_AGC_THI_101MS;
				lo = TSL2561_AGC_TLO_101MS;
			} else {
				hi = TSL2561_AGC_THI_402MS;
				lo = TSL2561_AGC_TLO_402MS;
			}

			getData();

			// Run an auto-gain check if we haven't already done so
			if (!auto_gain_check) {
				if ((broadband < lo) && (gain == TSL2561_GAIN_1X)) {
					// Increase the gain and try again
					setGain(TSL2561_GAIN_16X);
					// Drop the previous conversion results
					getData();
					// Set a flag to indicate we've adjusted the gain
					auto_gain_check = true;
				} else if ((broadband > hi) && (gain == TSL2561_GAIN_16X)) {
					// Drop gain to 1x and try again
					setGain(TSL2561_GAIN_1X);
					// Drop the previous conversion results
					getData();
					// Set a flag to indicate we've adjusted the gain
					auto_gain_check = true;
				} else {
					// Nothing to look at here, keep moving ....
					// Reading is either valid, or we're already at the chips
					// limits
					valid = true;
				}
			} else {
				// If we've already adjusted the gain once, just return the new
				// results.
				// This avoids endless loops where a value is at one extreme
				// pre-gain,
				// and the the other extreme post-gain
				valid = true;
			}
		}
	}

	/**
	 * Converts the raw sensor values to the standard SI lux equivalent. Returns
	 * 0 if the sensor is saturated and the values are unreliable.
	 */
	@Override
	public float getLuminosity() throws RuntimeIOException {
		getRawLuminosity();

		// Make sure the sensor isn't saturated!
		int clipThreshold;
		if (integrationTime == TSL2561_INTEGRATIONTIME_13MS) {
			clipThreshold = TSL2561_CLIPPING_13MS;
		} else if (integrationTime == TSL2561_INTEGRATIONTIME_101MS) {
			clipThreshold = TSL2561_CLIPPING_101MS;
		} else {
			clipThreshold = TSL2561_CLIPPING_402MS;
		}

		// Return 0 lux if the sensor is saturated
		if ((broadband > clipThreshold) || (ir > clipThreshold)) {
			return 0;
		}

		// Get the correct scale depending on the integration time
		int chScale;
		if (integrationTime == TSL2561_INTEGRATIONTIME_13MS) {
			chScale = TSL2561_LUX_CHSCALE_TINT0;
		} else if (integrationTime == TSL2561_INTEGRATIONTIME_101MS) {
			chScale = TSL2561_LUX_CHSCALE_TINT1;
		} else {
			chScale = (1 << TSL2561_LUX_CHSCALE);
		}

		// Scale for gain (1x or 16x)
		if (gain == 0) {
			chScale = chScale << 4;
		}

		// Scale the channel values
		int channel0 = (broadband * chScale) >> TSL2561_LUX_CHSCALE;
		int channel1 = (ir * chScale) >> TSL2561_LUX_CHSCALE;

		// Find the ratio of the channel values (Channel1/Channel0)
		int ratio1 = 0;
		if (channel0 != 0) {
			ratio1 = (channel1 << (TSL2561_LUX_RATIOSCALE + 1)) / channel0;
		}

		// round the ratio value
		int ratio = (ratio1 + 1) >> 1;
		// ratio = (ratio1 + 1) >> 1;

		int b = 0;
		int m = 0;
		switch (tsl2561Package) {
		case CHIP_SCALE:
			if ((ratio >= 0) && (ratio <= TSL2561_LUX_K1C)) {
				b = TSL2561_LUX_B1C;
				m = TSL2561_LUX_M1C;
			} else if (ratio <= TSL2561_LUX_K2C) {
				b = TSL2561_LUX_B2C;
				m = TSL2561_LUX_M2C;
			} else if (ratio <= TSL2561_LUX_K3C) {
				b = TSL2561_LUX_B3C;
				m = TSL2561_LUX_M3C;
			} else if (ratio <= TSL2561_LUX_K4C) {
				b = TSL2561_LUX_B4C;
				m = TSL2561_LUX_M4C;
			} else if (ratio <= TSL2561_LUX_K5C) {
				b = TSL2561_LUX_B5C;
				m = TSL2561_LUX_M5C;
			} else if (ratio <= TSL2561_LUX_K6C) {
				b = TSL2561_LUX_B6C;
				m = TSL2561_LUX_M6C;
			} else if (ratio <= TSL2561_LUX_K7C) {
				b = TSL2561_LUX_B7C;
				m = TSL2561_LUX_M7C;
			} else if (ratio > TSL2561_LUX_K8C) {
				b = TSL2561_LUX_B8C;
				m = TSL2561_LUX_M8C;
			}
			break;
		case T_FN_CL:
			if ((ratio >= 0) && (ratio <= TSL2561_LUX_K1T)) {
				b = TSL2561_LUX_B1T;
				m = TSL2561_LUX_M1T;
			} else if (ratio <= TSL2561_LUX_K2T) {
				b = TSL2561_LUX_B2T;
				m = TSL2561_LUX_M2T;
			} else if (ratio <= TSL2561_LUX_K3T) {
				b = TSL2561_LUX_B3T;
				m = TSL2561_LUX_M3T;
			} else if (ratio <= TSL2561_LUX_K4T) {
				b = TSL2561_LUX_B4T;
				m = TSL2561_LUX_M4T;
			} else if (ratio <= TSL2561_LUX_K5T) {
				b = TSL2561_LUX_B5T;
				m = TSL2561_LUX_M5T;
			} else if (ratio <= TSL2561_LUX_K6T) {
				b = TSL2561_LUX_B6T;
				m = TSL2561_LUX_M6T;
			} else if (ratio <= TSL2561_LUX_K7T) {
				b = TSL2561_LUX_B7T;
				m = TSL2561_LUX_M7T;
			} else if (ratio > TSL2561_LUX_K8T) {
				b = TSL2561_LUX_B8T;
				m = TSL2561_LUX_M8T;
			}
			break;
		}
		// endif

		int temp = ((channel0 * b) - (channel1 * m));

		// Do not allow negative lux value
		if (temp < 0) {
			temp = 0;
		}

		// Round lsb (2^(LUX_SCALE-1))
		temp += (1 << (TSL2561_LUX_LUXSCALE - 1));

		// Strip off fractional portion
		int lux = temp >> TSL2561_LUX_LUXSCALE;
		// FIXME Work with floating point numbers rather than integers!

		// Signal I2C had no errors
		return lux;
	}

	@Override
	public void close() {
		i2cDevice.close();
	}
}
