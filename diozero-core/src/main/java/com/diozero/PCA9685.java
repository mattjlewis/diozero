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

import org.pmw.tinylog.Logger;

import com.diozero.api.DeviceAlreadyOpenedException;
import com.diozero.api.I2CConstants;
import com.diozero.api.I2CDevice;
import com.diozero.internal.provider.pca9685.PCA9685PwmOutputDevice;
import com.diozero.internal.spi.AbstractDeviceFactory;
import com.diozero.internal.spi.PwmOutputDeviceFactoryInterface;
import com.diozero.internal.spi.PwmOutputDeviceInterface;
import com.diozero.util.*;

/**
 * PCA9685 I2C-bus controlled 16-channel 12-bit PWM controller as used in the popular Adafruit PWM add-on board
 * Datasheet: http://www.nxp.com/documents/data_sheet/PCA9685.pdf
 */
@SuppressWarnings("unused")
public class PCA9685 extends AbstractDeviceFactory implements PwmOutputDeviceFactoryInterface, Closeable {
	private static final int DEVICE_ADDRESS = 0x40;
	private static final String DEVICE_NAME = "PCA9685";
	private static final int NUM_CHANNELS = 16;
	private static final int RANGE = (int)Math.pow(2, 12);
	
	private static final int CLOCK_FREQ = 25_000_000; // 25MHz default osc clock
	
	// Registers/etc.
	private static final int MODE1 = 0x00;
	private static final int MODE2 = 0x01;
	private static final int SUBADR1 = 0x02;
	private static final int SUBADR2 = 0x03;
	private static final int SUBADR3 = 0x04;
	private static final int ALL_CALL = 0x05; //LED All Call I2C-bus address
	private static final int LED0 = 0x06;
	private static final int LED0_ON_L = LED0;
	private static final int LED0_ON_H = 0x07;
	private static final int LED0_OFF_L = 0x08;
	private static final int LED0_OFF_H = 0x09;
	private static final int ALL_LED_ON_L = 0xFA; // Load all the LEDn_ON registers, byte 0 (turn 0-7 channels on)
	private static final int ALL_LED_ON_H = 0xFB; // Load all the LEDn_ON registers, byte 1 (turn 8-15 channels on)
	private static final int ALL_LED_OFF_L = 0xFC; // Load all the LEDn_OFF registers, byte 0 (turn 0-7 channels off)
	private static final int ALL_LED_OFF_H = 0xFD; // Load all the LEDn_OFF registers, byte 1 (turn 8-15 channels off)
	private static final int PRESCALE = 0xFE; // Prescaler for output frequency

	// MODE1 bits
	private static final byte RESTART_BIT = 7;
	private static final byte RESTART_MASK = BitManipulation.getBitMask(RESTART_BIT);
	private static final byte SLEEP_BIT = 4;
	private static final byte SLEEP_MASK = BitManipulation.getBitMask(SLEEP_BIT);
	private static final byte ALLCALL_BIT = 0;
	private static final byte ALLCALL_MASK = BitManipulation.getBitMask(ALLCALL_BIT);
	
	// MODE2 bits
	/** 0: Output logic state not inverted. Value to use when external driver used
	 *  1: Output logic state inverted. Value to use when no external driver used */
	private static final byte INVRT_BIT = 4;
	private static final byte INVRT_MASK = BitManipulation.getBitMask(INVRT_BIT);
	/** 0: The 16 LED outputs are configured with an open-drain structure
	 *  1: The 16 LED outputs are configured with a totem pole structure */
	private static final byte OUTDRV_BIT = 2;
	private static final byte OUTDRV_MASK = BitManipulation.getBitMask(OUTDRV_BIT);

	private static final int MIN_PWM_FREQUENCY = 40;
	private static final int MAX_PWM_FREQUENCY = 1000;
	// TODO Qualify this value
	private static final int DEFAULT_PWM_FREQUENCY = 60;
	
	private I2CDevice i2cDevice;
	private String keyPrefix;
	private int pwmFrequency = DEFAULT_PWM_FREQUENCY;
	private double pulseMsPerBit = ServoUtil.calcPulseMsPerBit(pwmFrequency, RANGE);

	public PCA9685(int pwmFrequency) throws RuntimeIOException {
		this(I2CConstants.BUS_1, DEVICE_ADDRESS, pwmFrequency);
	}

	public PCA9685(int controller, int address, int pwmFrequency) throws RuntimeIOException {
		i2cDevice = new I2CDevice(controller, address, I2CConstants.ADDR_SIZE_7, I2CConstants.DEFAULT_CLOCK_FREQUENCY);
		
		keyPrefix = DEVICE_NAME + "-" + controller + "-" + address + "-";
		
		reset();
		
		setPwmFreq(pwmFrequency);
	}
	
	private void reset() throws RuntimeIOException {
		i2cDevice.writeByte(MODE1, 0); // Normal mode
		i2cDevice.writeByte(MODE2, OUTDRV_MASK); // Set output driver to totem pole mode (rather than open drain)
	}
	
	/**
	 * Sets the PWM frequency
	 * @param pwmFrequency desired frequency. 40Hz to 1000Hz using internal 25MHz oscillator
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	private void setPwmFreq(int pwmFrequency) throws RuntimeIOException {
		if (pwmFrequency < MIN_PWM_FREQUENCY || pwmFrequency > MAX_PWM_FREQUENCY) {
			throw new IllegalArgumentException("Invalid PWM Frequency value (" + pwmFrequency +
					") must be " + MIN_PWM_FREQUENCY + ".." + MAX_PWM_FREQUENCY);
		}
		
		double prescale_dbl = (((double)CLOCK_FREQ) / RANGE / pwmFrequency) - 1;
		int prescale_int = (int)Math.round(prescale_dbl);
		Logger.debug("Setting PWM frequency to {} Hz, double pre-scale: {}, int prescale {}",
				Integer.valueOf(pwmFrequency), String.format("%.2f", Double.valueOf(prescale_dbl)),
				Integer.valueOf(prescale_int));

		byte oldmode = i2cDevice.readByte(MODE1);
		i2cDevice.writeByte(MODE1, (byte)((oldmode & 0x7F) | SLEEP_MASK));	// Enter low power mode (set the sleep bit)
		i2cDevice.writeByte(PRESCALE, (byte)(prescale_int));
		this.pwmFrequency = pwmFrequency;
		pulseMsPerBit = ServoUtil.calcPulseMsPerBit(pwmFrequency, RANGE);
		i2cDevice.writeByte(MODE1, oldmode);								// Restore the previous mode1 value
		SleepUtil.sleepMillis(1);											// Wait min 500us for the oscillator to stabilise
		i2cDevice.writeByte(MODE1, (byte)(oldmode | RESTART_MASK));			// Set restart enabled
	}

	private int[] getPwm(int channel) throws RuntimeIOException {
		validateChannel(channel);
		
		// TODO Replace with readUShort()?
		int on_with_read_short = i2cDevice.readUShort(LED0_ON_L + 4*channel, 8);
		byte on_l = i2cDevice.readByte(LED0_ON_L + 4*channel);
		byte on_h = i2cDevice.readByte(LED0_ON_H + 4*channel);
		int on = ((on_h << 8) & 0xff00) | (on_l | 0xff);
		
		// TODO Replace with readUShort()?
		int off_with_read_short = i2cDevice.readUShort(LED0_OFF_L + 4*channel, 8);
		byte off_l = i2cDevice.readByte(LED0_OFF_L + 4*channel);
		byte off_h = i2cDevice.readByte(LED0_OFF_H + 4*channel);
		int off = ((off_h << 8) & 0xff00) | (off_l | 0xff);
		Logger.debug("on={}, on_with_read_short={}, off={}, off_with_read_short={}",
				Integer.valueOf(on), Integer.valueOf(on_with_read_short),
				Integer.valueOf(off), Integer.valueOf(off_with_read_short));
		
		return new int[] { on, off };
	}

	/**
	 * Sets a single PWM channel
	 * Example 1: (assumes that the LED0 output is used and (delay time) + (PWM duty cycle) &lt;= 100 %)
	 * Delay time = 10 %; PWM duty cycle = 20 % (LED on time = 20 %; LED off time = 80 %).
	 * Delay time = 10 % = 409.6 ~ 410 counts
	 * Since the counter starts at 0 and ends at 4095, we will subtract 1, so delay time = 409 counts
	 * LED on time = 20 % = 819.2 ~ 819 counts
	 * LED off time = (decimal 409 + 819 = 1228)
	 * @param channel PWM channel
	 * @param on on time
	 * @param off off time
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	private void setPwm(int channel, int on, int off) throws RuntimeIOException {
		validateChannel(channel);
		validateOnOff(on, off);
		
		// TODO Replace with writeShort()?
		//writeShort(LED0_ON_L+4*channel, (short)on);
		//writeShort(LED0_OFF_L+4*channel, (short)off);
		i2cDevice.writeByte(LED0_ON_L + 4*channel, on & 0xFF);
		i2cDevice.writeByte(LED0_ON_H + 4*channel, on >> 8);
		i2cDevice.writeByte(LED0_OFF_L + 4*channel, off & 0xFF);
		i2cDevice.writeByte(LED0_OFF_H + 4*channel, off >> 8);
	}
	
	private static void validateOnOff(int on, int off) {
		if (on < 0 || on >= RANGE) {
			throw new IllegalArgumentException(String.format("Error: on (" + on + ") must be 0.." + (RANGE-1)));
		}
		if (off < 0 || off >= RANGE) {
			throw new IllegalArgumentException(String.format("Error: off (" + off + ") must be 0.." + (RANGE-1)));
		}
		// Off must be after on
		if (off < on) {
			throw new IllegalArgumentException("Off value (" + off + ") must be > on value (" + on + ")");
		}
		// Total must be < 4096
		if (on+off >= RANGE) {
			throw new IllegalArgumentException(String.format("Error: on (%d) + off (%d) must be < %d",
					Integer.valueOf(on), Integer.valueOf(off), Integer.valueOf(RANGE)));
		}
	}

	private static void validateChannel(int channel) {
		if (channel < 0 || channel >= NUM_CHANNELS) {
			throw new IllegalArgumentException("Invalid PWM channel number (" + channel +
					"); channel must be 0.." + (NUM_CHANNELS-1));
		}
	}

	/**
	 * Sets a all PWM channels
	 * @param on on time
	 * @param off off time
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	private void setAllPwm(int on, int off) throws RuntimeIOException {
		validateOnOff(on, off);
		// TODO Replace with writeShort()?
		//i2cDevice.writeShort(ALL_LED_ON_L, (short)on);
		//i2cDevice.writeShort(ALL_LED_OFF_L, (short)off);
		i2cDevice.writeByte(ALL_LED_ON_L, on & 0xFF);
		i2cDevice.writeByte(ALL_LED_ON_H, on >> 8);
		i2cDevice.writeByte(ALL_LED_OFF_L, off & 0xFF);
		i2cDevice.writeByte(ALL_LED_OFF_H, off >> 8);
	}
	
	/**
	 * Set the pulse duration (micro-seconds)
	 * E.g. For TowerPro SG90 servo pulse width range = 500-2400 us
	 * TowerPro SG5010 servo pulse width range = 1ms-2ms
	 * @param channel PWM channel
	 * @param pulseWidthMs The desired pulse width in milli-seconds
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void setServoPulseWidthMs(int channel, double pulseWidthMs) throws RuntimeIOException {
		int pulse = ServoUtil.calcServoPulseBits(pulseWidthMs, pulseMsPerBit);
		Logger.debug("Requested pulse width: {}, Scale: {} ms per bit, Pulse: {}",
				String.format("%.2f", Double.valueOf(pulseWidthMs)),
				String.format("%.4f", Double.valueOf(pulseMsPerBit)), Integer.valueOf(pulse));
		
		setPwm(channel, 0, pulse);
	}

	@Override
	public String getName() {
		return DEVICE_NAME;
	}
	
	@Override
	public void close() throws RuntimeIOException {
		Logger.debug("close()");
		// Close all open pins before closing the I2C device itself
		shutdown();
		i2cDevice.close();
	}
	
	public void closeChannel(int channel) throws RuntimeIOException {
		Logger.debug("closeChannel({})", Integer.valueOf(channel));
		//setPwm(channel, 0, 0);
	}

	@Override
	public PwmOutputDeviceInterface provisionPwmOutputPin(int channel, float initialValue)
			throws RuntimeIOException {
		validateChannel(channel);
		
		String key = keyPrefix + channel;
		
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}
		
		PwmOutputDeviceInterface device = new PCA9685PwmOutputDevice(this, key, channel);
		deviceOpened(device);
		device.setValue(initialValue);
		
		return device;
	}

	public float getValue(int channel) throws RuntimeIOException {
		int[] on_off = getPwm(channel);
		return (on_off[1] - on_off[0]) / (float)RANGE;
	}

	/**
	 * Set PWM output on a specific channel, value must be 0..1
	 * 
	 * @param channel PWM channel
	 * @param value Must be 0..1
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void setValue(int channel, float value) throws RuntimeIOException {
		if (value < 0 || value > 1) {
			throw new IllegalArgumentException("PWM value must 0..1, you requested " + value);
		}
		int off = (int)Math.floor(value * RANGE);
		setPwm(channel, 0, off);
	}
	
	@Override
	public int getPwmFrequency(int pinNumber) {
		return pwmFrequency;
	}
	
	@Override
	public void setPwmFrequency(int pinNumber, int pwmFrequency) {
		setPwmFreq(pwmFrequency);
	}
}
