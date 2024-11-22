package com.diozero.devices;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     Ads1x15.java
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
import java.util.concurrent.atomic.AtomicBoolean;

import org.tinylog.Logger;

import com.diozero.api.AnalogInputEvent;
import com.diozero.api.DeviceInterface;
import com.diozero.api.DigitalInputDevice;
import com.diozero.api.I2CConstants;
import com.diozero.api.I2CDevice;
import com.diozero.api.I2CDeviceInterface;
import com.diozero.api.PinInfo;
import com.diozero.api.RuntimeIOException;
import com.diozero.api.function.FloatConsumer;
import com.diozero.internal.spi.AbstractDeviceFactory;
import com.diozero.internal.spi.AbstractInputDevice;
import com.diozero.internal.spi.AnalogInputDeviceFactoryInterface;
import com.diozero.internal.spi.AnalogInputDeviceInterface;
import com.diozero.sbc.BoardPinInfo;
import com.diozero.util.RangeUtil;
import com.diozero.util.SleepUtil;

/**
 * ADS1115 Datasheet: https://www.ti.com/lit/ds/symlink/ads1115.pdf ADS1015 Datasheet:
 * https://www.ti.com/lit/ds/symlink/ads1015.pdf
 * 
 * <pre>
 * Device  | Resolution | Max Sample Rate | # Channels | Interface | Features
 * ADS1115 |     16     |       860       |    2 (4)   |    I2C    | Comparator
 * ADS1114 |     16     |       860       |    1 (1)   |    I2C    | Comparator
 * ADS1015 |     12     |      3300       |    2 (4)   |    I2C    | Comparator
 * ADS1014 |     12     |      3300       |    1 (1)   |    I2C    | Comparator
 * ADS1118 |     16     |       860       |    2 (4)   |    SPI    | Temp. sensor
 * ADS1018 |     12     |      3300       |    2 (4)   |    SPI    | Temp. sensor
 * </pre>
 * 
 * Wiring:
 * 
 * <pre>
 * A3 | A2 | A1 | A0 | ALERT | ADDR | SDA | SCL | G | V
 * </pre>
 * 
 * ADDR (In) - I2C slave address select ALERT (Out) - Comparator output or conversion
 * ready (ADS1114 and ADS1115 only)
 *
 * ADDR - can be connected to GND, VDD, SDA, or SCL, allowing for four different addresses
 * to be selected
 * 
 * <pre>
 * GND | 0b01001000 (0x48)
 * VDD | 0b01001001 (0x49)
 * SDA | 0b01001010 (0x4a)
 * SCL | 0b01001011 (0x4b)
 * </pre>
 */
public class Ads1x15 extends AbstractDeviceFactory implements AnalogInputDeviceFactoryInterface, DeviceInterface {
	public static enum Model {
		ADS1015(4), ADS1115(4);

		private int numChannels;

		private Model(int numChannels) {
			this.numChannels = numChannels;
		}

		public int getNumChannels() {
			return numChannels;
		}
	}

	/**
	 * I2C address configuration
	 */
	public static enum Address {
		GND(0b01001000), VDD(0b01001001), SDA(0b01001010), SCL(0b01001011);

		private int value;

		private Address(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	/**
	 * Number of samples per second
	 */
	public static enum Ads1015DataRate {
		_128HZ(128, 0b000), _250HZ(250, 0b001), _490HZ(490, 0b010), _920HZ(920, 0b011), _1600HZ(1600, 0b100),
		_2400HZ(2400, 0b101), _3300HZ(3300, 0b110);

		private int dataRate;
		private byte mask;

		private Ads1015DataRate(int dataRate, int mask) {
			this.dataRate = dataRate;
			this.mask = (byte) (mask << CONFIG_LSB_DR_BIT_START);
		}

		public int getDataRate() {
			return dataRate;
		}

		public byte getMask() {
			return mask;
		}
	}

	/**
	 * Number of samples per second
	 */
	public static enum Ads1115DataRate {
		_8HZ(8, 0b000), _16HZ(16, 0b001), _32HZ(32, 0b010), _64HZ(64, 0b011), _128HZ(128, 0b100), _250HZ(250, 0b101),
		_475HZ(475, 0b110), _860HZ(860, 0b111);

		private int dataRate;
		private byte mask;

		private Ads1115DataRate(int dataRate, int mask) {
			this.dataRate = dataRate;
			this.mask = (byte) (mask << CONFIG_LSB_DR_BIT_START);
		}

		public int getDataRate() {
			return dataRate;
		}

		public byte getMask() {
			return mask;
		}
	}

	/**
	 * Programmable Gain Amplifier configuration. Ensure that ADC input voltage does not
	 * exceed this value
	 */
	public static enum PgaConfig {
		_6144MV(6.144f, 0b000), _4096MV(4.096f, 0b001), _2048MV(2.048f, 0b010), _1024MV(1.024f, 0b011),
		_512MV(0.512f, 0b100), _256MV(0.256f, 0b101);

		private float voltage;
		private byte mask;

		private PgaConfig(float voltage, int value) {
			this.voltage = voltage;
			this.mask = (byte) (value << CONFIG_MSB_PGA_BIT_START);
		}

		public float getVoltage() {
			return voltage;
		}

		public byte getMask() {
			return mask;
		}
	}

	/**
	 * Read mode - continuous or single
	 */
	public static enum Mode {
		SINGLE(CONFIG_MSB_MODE_SINGLE), CONTINUOUS(0);

		private byte mask;

		private Mode(int mask) {
			this.mask = (byte) mask;
		}

		public byte getMask() {
			return mask;
		}
	}

	public static enum ComparatorMode {
		TRADITIONAL(0), WINDOW(CONFIG_LSB_COMP_MODE_WINDOW);

		private byte mask;

		private ComparatorMode(int mask) {
			this.mask = (byte) mask;
		}

		public byte getMask() {
			return mask;
		}
	}

	public static enum ComparatorPolarity {
		ACTIVE_LOW(0), ACTIVE_HIGH(CONFIG_LSB_COMP_POL_ACTIVE_HIGH);

		private byte mask;

		private ComparatorPolarity(int mask) {
			this.mask = (byte) mask;
		}

		public byte getMask() {
			return mask;
		}
	}

	/**
	 * Comparator queue configuration
	 */
	public static enum ComparatorQueue {
		ASSERT_ONE_CONV(0b00), ASSERT_TWO_CONV(0b01), ASSERT_FOUR_CONV(0b10), DISABLE(0b11);

		private byte mask;

		private ComparatorQueue(int mask) {
			this.mask = (byte) mask;
		}

		public byte getMask() {
			return mask;
		}
	}

	private static final Address DEFAULT_ADDRESS = Address.GND;

	// The device has four registers that are selected by specifying the Address
	// Pointer Register
	private static final byte ADDR_POINTER_CONV = 0b00;
	private static final byte ADDR_POINTER_CONFIG = 0b01;
	private static final byte ADDR_POINTER_LO_THRESH = 0b10;
	private static final byte ADDR_POINTER_HIGH_THRESH = 0b11;

	// When the MODE bit in the config register is set to 1 the device enters
	// power-down state and operates in single-shot mode. The devices remains in
	// power-down state until 1 is written to the Operational Status (OS) bit. To
	// switch to continuous-conversion mode, write a 0 to the MODE bit in the Config
	// register.
	// Operational Status bit [15] (Write: 0 = No effect, 1 = Single; Read: 0 =
	// Busy, 1 = Ready)
	private static final int CONFIG_MSB_OS_SINGLE = 1 << 7;
	// Input multiplexer
	private static final int CONFIG_MSB_MUX_BIT_START = 4;
	private static final byte CONFIG_MSB_MUX_COMP_OFF = 0b100 << CONFIG_MSB_MUX_BIT_START;
	private static final int CONFIG_MSB_PGA_BIT_START = 1;
	// Device operating mode
	// 0 : Continuous-conversion mode
	// 1 : Single-shot mode or power-down state (default)
	private static final int CONFIG_MSB_MODE_SINGLE = 1 << 0;
	private static final int CONFIG_LSB_DR_BIT_START = 5;
	// Comparator mode
	// 0 : Traditional comparator (default)
	// 1 : Window comparator
	private static final int CONFIG_LSB_COMP_MODE_WINDOW = 1 << 4;
	// Comparator polarity
	// 0 : Active low (default)
	// 1 : Active high
	private static final int CONFIG_LSB_COMP_POL_ACTIVE_HIGH = 1 << 3;
	// Latching comparator
	// 0 : Nonlatching comparator . The ALERT/RDY pin does not latch when asserted
	// (default).
	// 1 : Latching comparator. The asserted ALERT/RDY pin remains latched until
	// conversion data are read by the master or an appropriate SMBus alert response
	// is sent by the master. The device responds with its address, and it is the
	// lowest address currently asserting the ALERT/RDY bus line.
	private static final int CONFIG_LSB_COMP_LATCHING = 1 << 2;

	private I2CDeviceInterface device;
	private Model model;
	private BoardPinInfo boardPinInfo;
	private PgaConfig pgaConfig;
	private int dataRate;
	private Mode mode;

	private int dataRateSleepMillis;
	private byte dataRateMask;
	private ComparatorMode comparatorMode;
	private ComparatorPolarity comparatorPolarity;
	private boolean latchingComparator;
	private ComparatorQueue comparatorQueue;

	// For continuous mode
	private AtomicBoolean gettingValues;
	private DigitalInputDevice readyPin;
	private float lastResult;

	// for tracing
	private boolean amTracing = false;

	/**
	 * 
	 * @param pgaConfig Programmable Gain Amplifier configuration - make sure this is set
	 *                  correctly and that the ADC input voltage does not exceed this value
	 * @param dataRate  Data read frequency (Hz)
	 */
	public Ads1x15(PgaConfig pgaConfig, Ads1115DataRate dataRate) {
		this(I2CConstants.CONTROLLER_1, DEFAULT_ADDRESS, pgaConfig, dataRate);
	}

	public Ads1x15(int controller, Address address, PgaConfig pgaConfig, Ads1115DataRate adsDataRate) {
		this(controller, Model.ADS1115, address, pgaConfig, adsDataRate.getDataRate(), adsDataRate.getMask());
	}

	public Ads1x15(PgaConfig pgaConfig, Ads1015DataRate dataRate) {
		this(I2CConstants.CONTROLLER_1, DEFAULT_ADDRESS, pgaConfig, dataRate);
	}

	public Ads1x15(int controller, Address address, PgaConfig pgaConfig, Ads1015DataRate ads1015DataRate) {
		this(controller, Model.ADS1015, address, pgaConfig, ads1015DataRate.getDataRate(), ads1015DataRate.getMask());
	}

	private Ads1x15(int controller, Model model, Address address, PgaConfig pgaConfig, int dataRate,
			byte dataRateMask) {
		super(Model.ADS1015.name() + "-" + controller + "-" + address.getValue());

		this.model = model;
		this.pgaConfig = pgaConfig;
		this.mode = Mode.SINGLE;
		setDataRate(dataRate, dataRateMask);
		comparatorMode = ComparatorMode.TRADITIONAL;
		comparatorPolarity = ComparatorPolarity.ACTIVE_LOW;
		latchingComparator = false;
		comparatorQueue = ComparatorQueue.DISABLE;

		boardPinInfo = new Ads1x15BoardPinInfo(model, pgaConfig.getVoltage());
		device = I2CDevice.builder(address.getValue()).setController(controller).setByteOrder(ByteOrder.BIG_ENDIAN)
				.build();

		amTracing = Logger.isTraceEnabled();
	}

	@Override
	public String getName() {
		return model.name() + "-" + device.getController() + "-" + device.getAddress();
	}

	@Override
	public BoardPinInfo getBoardPinInfo() {
		return boardPinInfo;
	}

	@Override
	public AnalogInputDeviceInterface createAnalogInputDevice(String key, PinInfo pinInfo) {
		return new Ads1x15AnalogInputDevice(this, key, pinInfo.getDeviceNumber());
	}

	@Override
	public void close() {
		Logger.trace("close()");
		// Close all open pins before closing the I2C device itself
		super.close();
		device.close();
	}

	public Model getModel() {
		return model;
	}

	public PgaConfig getPgaConfig() {
		return pgaConfig;
	}

	public int getDataRate() {
		return dataRate;
	}

	public void setDataRate(Ads1115DataRate ads1115DataRate) {
		if (model != Model.ADS1115) {
			throw new IllegalArgumentException(
					"Invalid model (" + model + "). Ads1115DataRate can only be used with the ADS1115");
		}
		setDataRate(ads1115DataRate.getDataRate(), ads1115DataRate.getMask());
	}

	public void setDataRate(Ads1015DataRate ads1015DataRate) {
		if (model != Model.ADS1015) {
			throw new IllegalArgumentException(
					"Invalid model (" + model + "). Ads1015DataRate can only be used with the ADS1015");
		}
		setDataRate(ads1015DataRate.getDataRate(), ads1015DataRate.getMask());
	}

	public void setContinousMode(DigitalInputDevice readyPin, int adcNumber, FloatConsumer callback) {
		gettingValues = new AtomicBoolean(false);

		mode = Mode.CONTINUOUS;
		comparatorPolarity = ComparatorPolarity.ACTIVE_HIGH;
		comparatorQueue = ComparatorQueue.ASSERT_ONE_CONV;

		setConfig(adcNumber);

		/*-
		 * The ALERT/RDY pin can also be configured as a conversion ready pin.
		 * Set the most-significant bit of the Hi_thresh register to 1 and the
		 * most-significant bit of Lo_thresh register to 0 to enable the pin
		 * as a conversion ready pin. The COMP_POL bit continues to function
		 * as expected. Set the COMP_QUE[1:0] bits to any 2-bit value other
		 * than 11 to keep the ALERT/RDY pin enabled, and allow the conversion
		 * ready signal to appear at the ALERT/RDY pin output. The COMP_MODE
		 * and COMP_LAT bits no longer control any function. When configured
		 * as a conversion ready pin, ALERT/RDY continues to require a
		 * pull-up resistor.
		 */
		// SleepUtil.sleepMillis(1);
		device.writeI2CBlockData(ADDR_POINTER_HIGH_THRESH, (byte) 0x80, (byte) 0x00);
		// SleepUtil.sleepMillis(1);
		device.writeI2CBlockData(ADDR_POINTER_LO_THRESH, (byte) 0x00, (byte) 0x00);

		this.readyPin = readyPin;
		readyPin.whenActivated(nanoTime -> {
			lastResult = RangeUtil.map(readConversionData(adcNumber), 0, Short.MAX_VALUE, 0, 1f);
			callback.accept(lastResult);
		});
		readyPin.whenDeactivated(nanoTime -> Logger.debug("Deactive!!!"));
	}

	public float getLastResult() {
		return lastResult;
	}

	public void setSingleMode(int adcNumber) {
		this.mode = Mode.SINGLE;
		comparatorPolarity = ComparatorPolarity.ACTIVE_LOW;
		comparatorQueue = ComparatorQueue.DISABLE;

		if (readyPin != null) {
			readyPin.whenActivated(null);
			readyPin.whenDeactivated(null);
			readyPin = null;
		}

		setConfig(adcNumber);
	}

	private void setDataRate(int dataRate, byte dataRateMask) {
		this.dataRate = dataRate;
		this.dataRateSleepMillis = Math.max(1_000 / dataRate, 1);
		this.dataRateMask = dataRateMask;
	}

	public void waitUntilReady() {
		long t0 = 0;

		if (amTracing) { // only want to hit currentTimeMillis() if necessary
			Logger.trace("waitUntilReady hit");
			t0 = System.currentTimeMillis();
		}

		SleepUtil.sleepMillis(dataRateSleepMillis);
		int incrementalSleepMillis = Math.max(dataRateSleepMillis / 20, 1);

		int i = 0;
		while (true) {
			short data = device.readShort(ADDR_POINTER_CONFIG);
			if ((data & 0x8000) != 0) break;
			SleepUtil.sleepMillis(incrementalSleepMillis);
			i++;
		}

		if (amTracing) {
			long t1 = System.currentTimeMillis();
			Logger.trace("waitUntilReady done, took {} ms, {} iterations", t1 - t0, i);
		}
	}

	public float getValue(int adcNumber) {
		Logger.debug("Reading channel {}, mode={}", adcNumber, mode);

		// TODO Protect against concurrent reads

		if (mode == Mode.SINGLE) {
			setConfig(adcNumber);
			waitUntilReady();
		} else if (readyPin != null) {
			return lastResult;
		}

		short conversionData = readConversionData(adcNumber);
		// need to set constrain to false: we *could* get a negative if doing differential
		float rv = RangeUtil.map(conversionData, 0, 0x8000, 0, 1f, false);
		Logger.trace ("mapped {} to {}", conversionData, rv);
		return rv;
	}

	protected void setConfig(int adcNumber) {
		byte config_msb = (byte) (CONFIG_MSB_OS_SINGLE | CONFIG_MSB_MUX_COMP_OFF
				| (adcNumber << CONFIG_MSB_MUX_BIT_START) | pgaConfig.getMask() | mode.getMask());
		byte config_lsb = (byte) (dataRateMask | comparatorMode.getMask() | comparatorPolarity.getMask()
				| (latchingComparator ? CONFIG_LSB_COMP_LATCHING : 0) | comparatorQueue.getMask());
		device.writeI2CBlockData(ADDR_POINTER_CONFIG, config_msb, config_lsb);
		Logger.trace("setConfig: 0x{} 0x{}", Integer.toHexString(config_msb & 0xff),
			Integer.toHexString(config_lsb & 0xff));
	}

	private short readConversionData(int adcNumber) {
		// byte[] data = device.readI2CBlockDataByteArray(ADDR_POINTER_CONV, 2);
		// short value = (short) ((data[0] & 0xff) << 8 | (data[1] & 0xff));
		short value = device.readShort(ADDR_POINTER_CONV);

		Logger.trace("readConversionData: 0x{}, {} dec", Integer.toHexString(value), value);

		return value;
	}

	private static class Ads1x15BoardPinInfo extends BoardPinInfo {
		public Ads1x15BoardPinInfo(Model model, float adcVRef) {
			for (int i = 0; i < model.getNumChannels(); i++) {
				addAdcPinInfo(i, i, adcVRef);
			}
		}
	}

	private static final class Ads1x15AnalogInputDevice extends AbstractInputDevice<AnalogInputEvent>
			implements AnalogInputDeviceInterface {
		private Ads1x15 ads1x15;
		private int adcNumber;

		public Ads1x15AnalogInputDevice(Ads1x15 ads1x15, String key, int adcNumber) {
			super(key, ads1x15);

			this.ads1x15 = ads1x15;
			this.adcNumber = adcNumber;
		}

		@Override
		protected void closeDevice() {
			Logger.trace("closeDevice() {}", getKey());
			ads1x15.setConfig(adcNumber);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public float getValue() throws RuntimeIOException {
			return ads1x15.getValue(adcNumber);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int getAdcNumber() {
			return adcNumber;
		}
	}

	@SuppressWarnings("boxing")
	public static void main(String[] args) {
		for (Ads1115DataRate dr : Ads1115DataRate.values()) {
			int data_rate = dr.getDataRate();
			int sleep_ms = 1_000 / data_rate;
			int sleep_us = 1_000_000 / data_rate;
			int sleep_ns = 1_000_000_000 / data_rate;

			System.out.format("DR: %s, %dms, %dus, %dns%n", data_rate, sleep_ms, sleep_us, sleep_ns);
		}

		for (Ads1015DataRate dr : Ads1015DataRate.values()) {
			int data_rate = dr.getDataRate();
			int sleep_ms = 1_000 / data_rate;
			int sleep_us = 1_000_000 / data_rate;
			int sleep_ns = 1_000_000_000 / data_rate;

			System.out.format("DR: %s, %dms, %dus, %dns%n", data_rate, sleep_ms, sleep_us, sleep_ns);
		}
	}
}
