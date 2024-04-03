package com.diozero.devices;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     PCA9685.java
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

import java.nio.ByteOrder;
import java.util.EnumSet;

import org.tinylog.Logger;

import com.diozero.api.DeviceInterface;
import com.diozero.api.DeviceMode;
import com.diozero.api.I2CConstants;
import com.diozero.api.I2CDevice;
import com.diozero.api.I2CDeviceInterface;
import com.diozero.api.PinInfo;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.AbstractDeviceFactory;
import com.diozero.internal.spi.InternalPwmOutputDeviceInterface;
import com.diozero.internal.spi.InternalServoDeviceInterface;
import com.diozero.internal.spi.PwmOutputDeviceFactoryInterface;
import com.diozero.internal.spi.ServoDeviceFactoryInterface;
import com.diozero.sbc.BoardPinInfo;
import com.diozero.util.BitManipulation;
import com.diozero.util.SleepUtil;

/**
 * PCA9685 I2C-bus controlled 16-channel 12-bit PWM controller as used in the popular
 * Adafruit PWM add-on board:
 * <a href="http://www.nxp.com/documents/data_sheet/PCA9685.pdf">Datasheet</a>
 */
@SuppressWarnings("unused")
public class PCA9685 extends AbstractDeviceFactory
		implements PwmOutputDeviceFactoryInterface, ServoDeviceFactoryInterface, DeviceInterface {
	public static final int DEFAULT_ADDRESS = 0x40;
	private static final String DEVICE_NAME = "PCA9685";
	private static final int NUM_CHANNELS = 16;
	private static final int RANGE = (int) Math.pow(2, 12);

	private static final int CLOCK_FREQ = 25_000_000; // 25MHz default osc clock

	// Registers/etc.
	private static final int MODE1 = 0x00;
	private static final int MODE2 = 0x01;
	private static final int SUBADR1 = 0x02;
	private static final int SUBADR2 = 0x03;
	private static final int SUBADR3 = 0x04;
	private static final int ALL_CALL = 0x05; // LED All Call I2C-bus address
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
	/**
	 * 0: Output logic state not inverted. Value to use when external driver used
	 *
	 * 1: Output logic state inverted. Value to use when no external driver used
	 */
	private static final byte INVRT_BIT = 4;
	private static final byte INVRT_MASK = BitManipulation.getBitMask(INVRT_BIT);
	/**
	 * 0: The 16 LED outputs are configured with an open-drain structure
	 *
	 * 1: The 16 LED outputs are configured with a totem pole structure
	 */
	private static final byte OUTDRV_BIT = 2;
	private static final byte OUTDRV_MASK = BitManipulation.getBitMask(OUTDRV_BIT);

	private static final int MIN_PWM_FREQUENCY = 40;
	private static final int MAX_PWM_FREQUENCY = 1000;
	public static final int DEFAULT_PWM_FREQUENCY = 50;

	private final I2CDeviceInterface device;
	private String keyPrefix;
	private int boardPwmFrequency = DEFAULT_PWM_FREQUENCY;
	private double periodUs = 1_000_000.0 / DEFAULT_PWM_FREQUENCY;
	private final BoardPinInfo boardPinInfo;

	public PCA9685() throws RuntimeIOException {
		this(I2CConstants.CONTROLLER_1, DEFAULT_ADDRESS, DEFAULT_PWM_FREQUENCY);
	}

	public PCA9685(int pwmFrequency) throws RuntimeIOException {
		this(I2CConstants.CONTROLLER_1, DEFAULT_ADDRESS, pwmFrequency);
	}

	public PCA9685(int controller, int pwmFrequency) throws RuntimeIOException {
		this(controller, DEFAULT_ADDRESS, pwmFrequency);
	}

	public PCA9685(int controller, int address, int pwmFrequency) throws RuntimeIOException {
		super(DEVICE_NAME + "-" + controller + "-" + address);

		device = I2CDevice.builder(address).setController(controller).setByteOrder(ByteOrder.BIG_ENDIAN).build();
		boardPinInfo = new PCA9685BoardPinInfo();

		reset();

		setPwmFreq(pwmFrequency);
	}

	private void reset() throws RuntimeIOException {
		device.writeByteData(MODE1, 0); // Normal mode
		device.writeByteData(MODE2, OUTDRV_MASK); // Set output driver to totem pole mode (rather than open drain)
	}

	/**
	 * Sets the PWM frequency
	 *
	 * @param pwmFrequency desired frequency. 40Hz to 1000Hz using internal 25MHz oscillator
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	private void setPwmFreq(int pwmFrequency) throws RuntimeIOException {
		if (pwmFrequency < MIN_PWM_FREQUENCY || pwmFrequency > MAX_PWM_FREQUENCY) {
			throw new IllegalArgumentException("Invalid PWM Frequency value (" + pwmFrequency + ") must be "
					+ MIN_PWM_FREQUENCY + ".." + MAX_PWM_FREQUENCY);
		}

		// TODO Check if there are any devices currently provisioned

		float prescale_flt = (((float) CLOCK_FREQ) / RANGE / pwmFrequency) - 1;
		int prescale_int = Math.round(prescale_flt);
		Logger.debug("Setting PWM frequency to {} Hz, float pre-scale: {}, int prescale {}", pwmFrequency,
				String.format("%.2f", prescale_flt), prescale_int);

		byte oldmode = device.readByteData(MODE1);
		device.writeByteData(MODE1, (byte) ((oldmode & 0x7F) | SLEEP_MASK)); // Enter low power mode (set the sleep
																				// bit)
		device.writeByteData(PRESCALE, (byte) (prescale_int));
		boardPwmFrequency = pwmFrequency;
		periodUs = 1_000_000.0 / pwmFrequency;
		device.writeByteData(MODE1, oldmode); // Restore the previous mode1 value
		SleepUtil.sleepMillis(1); // Wait min 500us for the oscillator to stabilise
		device.writeByteData(MODE1, (byte) (oldmode | RESTART_MASK)); // Set restart enabled
	}

	private int[] getPwm(int channel) throws RuntimeIOException {
		validateChannel(channel);

		short on_l = device.readUByte(LED0_ON_L + 4 * channel);
		short on_h = device.readUByte(LED0_ON_H + 4 * channel);
		int on = (on_h << 8) | on_l;

		short off_l = device.readUByte(LED0_OFF_L + 4 * channel);
		short off_h = device.readUByte(LED0_OFF_H + 4 * channel);
		int off = (off_h << 8) | off_l;

		Logger.debug("channel={}, on={}, off={}", channel, on, off);

		return new int[] { on, off };
	}

	/**
	 * Sets a single PWM channel
	 *
	 * Example 1: (assumes that the LED0 output is used and (delay time) + (PWM duty cycle)
	 * &lt;= 100 %)
	 *
	 * <pre>
	 * Delay time = 10 %;
	 * PWM duty cycle = 20 % (LED on time = 20 %;
	 * LED off time = 80 %).
	 * Delay time = 10 % = 409.6 ~ 410 counts
	 * </pre>
	 *
	 * Since the counter starts at 0 and ends at 4095, we will subtract 1, so:
	 *
	 * <pre>
	 * delay time = 409
	 * counts LED on time = 20 % = 819.2 ~ 819
	 * counts LED off time = (decimal 409 + 819 = 1228)
	 * </pre>
	 *
	 * @param channel PWM channel
	 * @param on      on time
	 * @param off     off time
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	private void setPwm(int channel, int on, int off) throws RuntimeIOException {
		validateChannel(channel);
		validateOnOff(on, off);

		// Logger.debug("channel: {}, on: {}, off: {}", Integer.valueOf(channel),
		// Integer.valueOf(on),
		// Integer.valueOf(off));

		// TODO Replace with writeWordData()?
		// device.writeWordData(LED0_ON_L + 4 * channel, (short) on);
		// device.writeWordData(LED0_OFF_L + 4 * channel, (short) off);
		device.writeByteData(LED0_ON_L + 4 * channel, on & 0xFF);
		device.writeByteData(LED0_ON_H + 4 * channel, on >> 8);
		device.writeByteData(LED0_OFF_L + 4 * channel, off & 0xFF);
		device.writeByteData(LED0_OFF_H + 4 * channel, off >> 8);
		// SleepUtil.sleepMillis(50);
	}

	private static void validateOnOff(int on, int off) {
		if (on < 0 || on >= RANGE) {
			throw new IllegalArgumentException(String.format("Error: on (" + on + ") must be 0.." + (RANGE - 1)));
		}
		if (off < 0 || off >= RANGE) {
			throw new IllegalArgumentException(String.format("Error: off (" + off + ") must be 0.." + (RANGE - 1)));
		}
		// Off must be after on
		if (off < on) {
			throw new IllegalArgumentException("Off value (" + off + ") must be > on value (" + on + ")");
		}
		// Total must be < 4096
		if (on + off >= RANGE) {
			throw new IllegalArgumentException(String.format("Error: on (%d) + off (%d) must be < %d", on, off, RANGE));
		}
	}

	private static void validateChannel(int channel) {
		if (channel < 0 || channel >= NUM_CHANNELS) {
			throw new IllegalArgumentException(
					"Invalid PWM channel number (" + channel + "); channel must be 0.." + (NUM_CHANNELS - 1));
		}
	}

	/**
	 * Sets a all PWM channels
	 *
	 * @param on  on time
	 * @param off off time
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	private void setAllPwm(int on, int off) throws RuntimeIOException {
		validateOnOff(on, off);
		// TODO Replace with writeShort()?
		// device.writeShort(ALL_LED_ON_L, (short)on);
		// device.writeShort(ALL_LED_OFF_L, (short)off);
		device.writeByteData(ALL_LED_ON_L, on & 0xFF);
		device.writeByteData(ALL_LED_ON_H, on >> 8);
		device.writeByteData(ALL_LED_OFF_L, off & 0xFF);
		device.writeByteData(ALL_LED_OFF_H, off >> 8);
	}

	@Override
	public String getName() {
		return DEVICE_NAME;
	}

	@Override
	public void close() throws RuntimeIOException {
		Logger.trace("close()");
		if (isClosed()) {
			return;
		}

		// Close all open pins before closing the I2C device itself
		super.close();
		device.close();
	}

	public void closeChannel(int channel) throws RuntimeIOException {
		Logger.trace("closeChannel({})", channel);
		setPwm(channel, 0, 0);
	}

	@Override
	public InternalPwmOutputDeviceInterface createPwmOutputDevice(String key, PinInfo pinInfo, int pwmFrequency,
			float initialValue) throws RuntimeIOException {
		// Note pwmFrequency is ignored; make sure you setup the board's PWM frequency
		// first
		if (pwmFrequency != boardPwmFrequency) {
			Logger.warn("Specified PWM frequency ({}) is different to that configured for the board ({})"
					+ "; this device has a common PWM frequency that is used for all outputs"
					+ " - the requested frequency will be ignored", pwmFrequency, boardPwmFrequency);
		}
		return new PCA9685ServoOrPwmOutputDevice(this, key, DeviceMode.PWM_OUTPUT, pinInfo.getDeviceNumber(),
				initialValue);
	}

	@Override
	public InternalServoDeviceInterface createServoDevice(String key, PinInfo pinInfo, int frequency,
			int minPulseWidthUs, int maxPulseWidthUs, int initialPulseWidthUs) throws RuntimeIOException {
		// Note frequency is ignored; make sure you setup the board's PWM frequency
		// first
		if (frequency != boardPwmFrequency) {
			Logger.warn("Specified Servo frequency ({}) is different to that configured for the board ({})"
					+ "; this device has a common PWM frequency that is used for all outputs"
					+ " - the requested frequency will be ignored", frequency, boardPwmFrequency);
		}
		return new PCA9685ServoOrPwmOutputDevice(this, key, DeviceMode.SERVO, pinInfo.getDeviceNumber(),
				(float) (initialPulseWidthUs / periodUs));
	}

	/**
	 * Get the PWM output percentage for the specified channel, range 0..1
	 *
	 * @param channel PWM channel
	 * @return output PWM "on" percentage, range 0..1
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public float getValue(int channel) throws RuntimeIOException {
		int[] on_off = getPwm(channel);
		return (on_off[1] - on_off[0]) / (float) RANGE;
	}

	/**
	 * Set PWM output percentage for the specified channel, value must be 0..1
	 *
	 * @param channel PWM channel
	 * @param value   Must be 0..1
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void setValue(int channel, float value) throws RuntimeIOException {
		if (value < 0 || value > 1) {
			throw new IllegalArgumentException("PWM value must 0..1, you requested " + value);
		}
		int off = Math.round(value * RANGE);
		setPwm(channel, 0, off);
	}

	public int getDutyUs(int channel) {
		return (int) Math.round(getValue(channel) * periodUs);
	}

	/**
	 * Set PWM duty cycle output in microseconds for the specified channel. A standard servo
	 * has a minimum range of 1-2 milliseconds. The actual range varies between devices. E.g.
	 * My TowerPro SG90 has a pulse width range of 500-2,400 us.
	 *
	 * @param channel PWM channel
	 * @param dutyUs  New duty value in microseconds
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void setDutyUs(int channel, int dutyUs) throws RuntimeIOException {
		// TODO Bounds checking

		int off = (int) Math.round((dutyUs / periodUs) * RANGE);
		if (Logger.isDebugEnabled()) {
			Logger.debug("Requested duty value: {}, Scale: {} microseconds per bit, Off: {}",
					String.format("%.2f", (double) dutyUs), String.format("%.4f", periodUs / RANGE), off);
		}

		setPwm(channel, 0, off);
	}

	@Override
	public int getBoardPwmFrequency() {
		return boardPwmFrequency;
	}

	@Override
	public void setBoardPwmFrequency(int pwmFrequency) {
		setPwmFreq(pwmFrequency);
	}

	@Override
	public int getBoardServoFrequency() {
		return boardPwmFrequency;
	}

	@Override
	public void setBoardServoFrequency(int pwmFrequency) {
		setPwmFreq(pwmFrequency);
	}

	@Override
	public BoardPinInfo getBoardPinInfo() {
		return boardPinInfo;
	}

	public static class PCA9685BoardPinInfo extends BoardPinInfo {
		public PCA9685BoardPinInfo() {
			for (int i = 0; i < 8; i++) {
				addGpioPinInfo(i, 6 + i, EnumSet.of(DeviceMode.PWM_OUTPUT, DeviceMode.SERVO));
			}
			for (int i = 8; i < 16; i++) {
				addGpioPinInfo(i, 7 + i, EnumSet.of(DeviceMode.PWM_OUTPUT, DeviceMode.SERVO));
			}
		}
	}

	private static class PCA9685ServoOrPwmOutputDevice extends AbstractDevice
			implements InternalPwmOutputDeviceInterface, InternalServoDeviceInterface {
		private final PCA9685 pca9685;
		private final DeviceMode mode;
		private final int channel;

		public PCA9685ServoOrPwmOutputDevice(PCA9685 pca9685, String key, DeviceMode mode, int channel,
				float initialValue) {
			super(key, pca9685);

			this.pca9685 = pca9685;
			this.mode = mode;
			this.channel = channel;

			setValue(initialValue);
		}

		@Override
		public DeviceMode getMode() {
			return mode;
		}

		@Override
		public int getGpio() {
			return channel;
		}

		@Override
		public int getPwmNum() {
			return channel;
		}

		@Override
		public int getServoNum() {
			return channel;
		}

		@Override
		public float getValue() throws RuntimeIOException {
			return pca9685.getValue(channel);
		}

		@Override
		public void setValue(float value) throws RuntimeIOException {
			pca9685.setValue(channel, value);
		}

		@Override
		public int getPulseWidthUs() throws RuntimeIOException {
			return pca9685.getDutyUs(channel);
		}

		@Override
		public void setPulseWidthUs(int pulseWidthUs) throws RuntimeIOException {
			pca9685.setDutyUs(channel, pulseWidthUs);
		}

		@Override
		protected void closeDevice() throws RuntimeIOException {
			Logger.trace("closeDevice() {}", getKey());
			pca9685.closeChannel(channel);
		}

		@Override
		public int getPwmFrequency() {
			return pca9685.getBoardPwmFrequency();
		}

		@Override
		public void setPwmFrequency(int frequency) throws RuntimeIOException {
			throw new UnsupportedOperationException(
					"The PCA9685 has a PWM output frequency that is shared across all channels hence "
							+ "cannot be changed for individual channels");
		}

		@Override
		public int getServoFrequency() {
			return pca9685.getBoardServoFrequency();
		}

		@Override
		public void setServoFrequency(int frequency) throws RuntimeIOException {
			throw new UnsupportedOperationException(
					"The PCA9685 has a PWM output frequency that is shared across all channels hence "
							+ "cannot be changed for individual channels");
		}
	}
}
