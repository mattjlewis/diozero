package com.diozero.devices;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     Ads112C04.java
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

/*-
 * Wiring:
 * # Con Name | Name Con  #
 * -----------+-------------
 * 1 GND   A0 | SCL  SCL  16 **
 * 2 GND   A1 | SDA  SDA  15 **
 * 3 3v3  RST | DRDY GP#X 14 *
 * 4 GND DGND | DVdd 3v3  13
 * 5 GND AVss | AVdd 3v3  12
 * 6     AIN3 | AIN0      11
 * 7     AIN2 | AIN1      10
 * 8     REFN | REFP      9
 *
 * * Active low therefore the GPIO must have the pull-up resistor enabled.
 * Otherwise connect to DVdd using a "weak" pull-up resistor (1 kOhm).
 *
 * ** The datasheet states 1 kOhm pull-up resistors for SCL and SDA
 * for standard and fast modes, and 350 Ohm for fast-mode plus.
 * Most boards have built-in pull-up resistors for SCL and SDA.
 */

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.function.Consumer;

import org.tinylog.Logger;

import com.diozero.api.AnalogInputEvent;
import com.diozero.api.DeviceInterface;
import com.diozero.api.DigitalInputDevice;
import com.diozero.api.Event;
import com.diozero.api.I2CDevice;
import com.diozero.api.I2CDeviceInterface;
import com.diozero.api.I2CDeviceInterface.I2CMessage;
import com.diozero.api.PinInfo;
import com.diozero.api.RuntimeIOException;
import com.diozero.api.sandpit.EventQueue;
import com.diozero.internal.spi.AbstractDeviceFactory;
import com.diozero.internal.spi.AbstractInputDevice;
import com.diozero.internal.spi.AnalogInputDeviceFactoryInterface;
import com.diozero.internal.spi.AnalogInputDeviceInterface;
import com.diozero.sbc.BoardPinInfo;
import com.diozero.util.Crc;
import com.diozero.util.DiozeroScheduler;
import com.diozero.util.Hex;
import com.diozero.util.PropertyUtil;
import com.diozero.util.SleepUtil;

public class Ads112C04 extends AbstractDeviceFactory implements AnalogInputDeviceFactoryInterface, DeviceInterface {
	private static final String NAME = "ADS112C04";
	
	private static final int NUM_CHANNELS = 4;

	/**
	 * The ADS112C04 has two address pins: A0 and A1. Each address pin can be tied
	 * to either DGND, DVDD, SDA, or SCL, providing 16 possible unique addresses.
	 * This configuration allows up to 16 different ADS112C04 devices to be present
	 * on the same I2C bus. Name format is A1_A0
	 */
	public enum Address {
		GND_GND(0b01000000), GND_VDD(0b01000001), GND_SDA(0b01000010), GND_SCL(0b01000011), //
		VDD_GND(0b01000100), VDD_VDD(0b01000101), VDD_SDA(0b01000110), VDD_SCL(0b01000111), //
		SDA_GND(0b01001000), SDA_VDD(0b01001001), SDA_SDA(0b01001010), SDA_SCL(0b01001011), //
		SCL_GND(0b01001100), SCL_VDD(0b01001101), SCL_SDA(0b01001110), SCL_SCL(0b01001111);

		private int value;

		private Address(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public enum GainConfig {
		_1(1, 0b000), _2(2, 0b001), _4(4, 0b010), _8(8, 0b011), _16(16, 0b100), _32(32, 0b101), _64(64, 0b110),
		_128(128, 0b111);

		private int gain;
		private byte mask;

		private GainConfig(int gain, int mask) {
			this.gain = gain;
			this.mask = (byte) (mask << C0_GAIN_BIT_START);
		}

		public int getGain() {
			return gain;
		}

		byte getMask() {
			return mask;
		}
	}

	public enum Pga {
		ENABLED(0), DISABLED(1);

		private byte mask;

		private Pga(int mask) {
			this.mask = (byte) (mask << C0_PGA_BIT_START);
		}

		byte getMask() {
			return mask;
		}

		public boolean isEnabled() {
			return this == ENABLED;
		}
	}

	public enum DataRate {
		_20HZ(20, 0b000), _45HZ(45, 0b001), _90HZ(90, 0b010), _175HZ(175, 0b011), _330HZ(330, 0b100),
		_600HZ(600, 0b101), _1000HZ(1000, 0b110);

		private int dateRate;
		private byte mask;

		private DataRate(int dateRate, int mask) {
			this.dateRate = dateRate;
			this.mask = (byte) (mask << C1_DATA_RATE_BIT_START);
		}

		public int getDataRate() {
			return dateRate;
		}

		byte getMask() {
			return mask;
		}
	}

	public enum OperatingMode {
		NORMAL(1, 0b0), TURBO(2, 0b1);

		private int multiplier;
		private byte mask;

		private OperatingMode(int multiplier, int mask) {
			this.multiplier = multiplier;
			this.mask = (byte) (mask << C1_OP_MODE_BIT_START);
		}

		public int getMultiplier() {
			return multiplier;
		}

		byte getMask() {
			return mask;
		}
	}

	public enum ConversionMode {
		SINGLE_SHOT(0b0), CONTINUOUS(0b1);

		private byte mask;

		private ConversionMode(int mask) {
			this.mask = (byte) (mask << C1_CONV_MODE_BIT_START);
		}

		byte getMask() {
			return mask;
		}
	}

	public enum VRef {
		INTERNAL(0b00), EXTERNAL(0b01), ANALOG_SUPPLY(0b10);

		private byte mask;

		private VRef(int mask) {
			this.mask = (byte) (mask << C1_VREF_BIT_START);
		}

		public int getMask() {
			return mask;
		}
	}

	public enum TemperatureSensorMode {
		ENABLED(1), DISABLED(0);

		private byte mask;

		private TemperatureSensorMode(int mask) {
			this.mask = (byte) (mask << C1_TEMP_SENSOR_BIT_START);
		}

		byte getMask() {
			return mask;
		}

		public boolean isEnabled() {
			return this == ENABLED;
		}
	}

	public enum DataCounter {
		ENABLED(1), DISABLED(0);

		private byte mask;

		private DataCounter(int mask) {
			this.mask = (byte) (mask << C2_DATA_CNT_EN_BIT_START);
		}

		byte getMask() {
			return mask;
		}

		public boolean isEnabled() {
			return this == ENABLED;
		}
	}

	public enum CrcConfig {
		DISABLED(0b00), INVERTED_DATA_OUTPUT(0b01), CRC16(0b10);

		private byte mask;

		private CrcConfig(int mask) {
			this.mask = (byte) (mask << C2_CRC_EN_BIT_START);
		}

		byte getMask() {
			return mask;
		}
	}

	public enum BurnoutCurrentSources {
		ENABLED(1), DISABLED(0);

		private byte mask;

		private BurnoutCurrentSources(int mask) {
			this.mask = (byte) (mask << C2_BCS_BIT_START);
		}

		byte getMask() {
			return mask;
		}

		public boolean isEnabled() {
			return this == ENABLED;
		}
	}

	public enum IdacCurrent {
		OFF(0, 0b000), _10UA(10, 0b001), _50UA(50, 0b010), _100UA(100, 0b011), _250UA(250, 0b100), _500UA(500, 0b101),
		_1000UA(1000, 0b110), _1500UA(1500, 0b111);

		private int microAmps;
		private byte mask;

		private IdacCurrent(int microAmps, int mask) {
			this.microAmps = microAmps;
			this.mask = (byte) (mask << C2_IDAC_CRNT_BIT_START);
		}

		public int getMicroAmps() {
			return microAmps;
		}

		byte getMask() {
			return mask;
		}
	}

	public enum Idac1RoutingConfig {
		DISABLED(0b000), AIN0(0b001), AIN1(0b010), AIN2(0b011), AIN3(0b100), REFP(0b101), REFN(0b110);

		private byte mask;

		private Idac1RoutingConfig(int mask) {
			this.mask = (byte) (mask << C3_I1MUX_BIT_START);
		}

		byte getMask() {
			return mask;
		}
	}

	public enum Idac2RoutingConfig {
		DISABLED(0b000), AIN0(0b001), AIN1(0b010), AIN2(0b011), AIN3(0b100), REFP(0b101), REFN(0b110);

		private byte mask;

		private Idac2RoutingConfig(int mask) {
			this.mask = (byte) (mask << C3_I2MUX_BIT_START);
		}

		byte getMask() {
			return mask;
		}
	}

	/**
	 * Input multiplexer configuration
	 */
	public enum InputMultiplexerConfig {
		/** AINP = AIN0, AINN = AIN1 (default) */
		AIN0_AIN1(0b0000), //
		/** AINP = AIN0, AINN = AIN2 */
		AIN0_AIN2(0b0001), //
		/** AINP = AIN0, AINN = AIN3 */
		AIN0_AIN3(0b0010), //
		
		/** AINP = AIN1, AINN = AIN0 */
		AIN1_AIN0(0b0011), //
		/** AINP = AIN1, AINN = AIN2 */
		AIN1_AIN2(0b0100), //
		/** AINP = AIN3, AINN = AIN2 */
		AIN1_AIN3(0b0101), //
		
		/** AINP = AIN2, AINN = AIN3 */
		AIN2_AIN3(0b0110), //
		/** AINP = AIN1, AINN = AIN3 */
		AIN3_AIN2(0b0111), //
		
		/** AINP = AIN0, AINN = AVSS */
		AIN0_AVSS(0b1000, true), //
		/** AINP = AIN1, AINN = AVSS */
		AIN1_AVSS(0b1001), //
		/** AINP = AIN2, AINN = AVSS */
		AIN2_AVSS(0b1010), //
		/** AINP = AIN3, AINN = AVSS */
		AIN3_AVSS(0b1011), //
		
		/** (V(REFP) – V(REFN)) / 4 monitor (PGA bypassed) */
		VREFP_VREFN_DIV_4(0b1100), //
		/** (AVDD – AVSS) / 4 monitor (PGA bypassed) */
		AVDD_AVSS_DIV_4(0b1101), //
		/** AINP and AINN shorted to (AVDD + AVSS) / 2 */
		AVDD_PLUS_AVSS_DIV_2(0b1110);
		
		private byte mask;
		private boolean compareToAvss;
		
		private InputMultiplexerConfig(int value) {
			this(value, false);
		}
		
		private InputMultiplexerConfig(int value, boolean compareToAvss) {
			this.mask = (byte) (value << C0_MUX_BIT_START);
			this.compareToAvss = compareToAvss;
		}
		
		byte getMask() {
			return mask;
		}
		
		public boolean isCompareToAvss() {
			return compareToAvss;
		}
	}

	/*
	 * The device has four 8-bit configuration registers that are accessible through
	 * the I2C interface using the RREG and WREG commands. After power-up or reset,
	 * all registers are set to the default values (which are all 0). All register
	 * values are retained during power-down mode.
	 */
	public enum ConfigRegister {
		_0(0b00), _1(0b01), _2(0b10), _3(0b11);

		private byte mask;

		private ConfigRegister(int mask) {
			this.mask = (byte) (mask << 2);
		}

		byte getMask() {
			return mask;
		}
	}

	/*- Config register 0
	 * MUX 7:4 (R/W), Gain 3:1 (R/W), PGA Enabled 0:0 (R/W)
	 */
	private static final int C0_MUX_BIT_START = 4;
	private static final int C0_GAIN_BIT_START = 1;
	private static final int C0_PGA_BIT_START = 0;
	/*- Config register 1
	 * Data Rate 7:5 (R/W), Operating Mode 4 (R/W), Conversion Mode 3 (R/W),
	 * VRef 2:1 (R/W), Temp. Sensor Mode 0 (R/W)
	 */
	private static final int C1_DATA_RATE_BIT_START = 5;
	private static final int C1_OP_MODE_BIT_START = 4;
	private static final int C1_CONV_MODE_BIT_START = 3;
	private static final int C1_VREF_BIT_START = 1;
	private static final int C1_TEMP_SENSOR_BIT_START = 0;
	/*- Config register 2
	 * Data Ready 7 (R), Data Counter Enable 6 (R/W), CRC Enable 5:4 (R/W),
	 * Burn-out Current Sources 3 (R/W), IDAC Current Setting 0 (R/W)
	 */
	private static final int C2_DATA_RDY_BIT_START = 7;
	private static final int C2_DATA_RDY_MASK = 1 << C2_DATA_RDY_BIT_START;
	private static final int C2_DATA_CNT_EN_BIT_START = 6;
	private static final int C2_CRC_EN_BIT_START = 4;
	private static final int C2_BCS_BIT_START = 3;
	private static final int C2_IDAC_CRNT_BIT_START = 0;
	/*- Config register 3
	 * I1MUX 7:5 (R/W), I2MUX 4:2 (R/W), Reserved 1:0 (R) Always 0
	 */
	private static final int C3_I1MUX_BIT_START = 5;
	private static final int C3_I2MUX_BIT_START = 2;

	private static final byte COMMAND_RESET = (byte) 0b00000110;
	private static final byte COMMAND_START = (byte) 0b00001000;
	private static final byte COMMAND_POWER_DOWN = (byte) 0b00000010;
	private static final byte COMMAND_RDATA = (byte) 0b00010000;
	private static final byte COMMAND_READ_REG = (byte) 0b00100000;
	private static final byte COMMAND_WRITE_REG = (byte) 0b01000000;

	// The CRC is based on the CRC-16-CCITT polynomial: x16 + x12 + x5 + 1 with an
	// initial value of FFFFh.
	private static final Crc.Params CRC_PARAMS = new Crc.Params(0b10001000000100001, 0xffff, false, false, 0x0000);

	public static class Builder {
		private int controller;
		private Address address;
		private GainConfig gainConfig = GainConfig._1;
		private Pga pga = Pga.ENABLED;
		private DataRate dataRate = DataRate._20HZ;
		private OperatingMode operatingMode = OperatingMode.NORMAL;
		private VRef vRef = VRef.INTERNAL;
		private TemperatureSensorMode tsMode = TemperatureSensorMode.DISABLED;
		private DataCounter dataCounter = DataCounter.DISABLED;
		private CrcConfig crcConfig = CrcConfig.DISABLED;
		private BurnoutCurrentSources burnoutCurrentSources = BurnoutCurrentSources.DISABLED;
		private IdacCurrent idacCurrent = IdacCurrent.OFF;
		private Idac1RoutingConfig idac1RoutingConfig = Idac1RoutingConfig.DISABLED;
		private Idac2RoutingConfig idac2RoutingConfig = Idac2RoutingConfig.DISABLED;
		private InputMultiplexerConfig inputMultiplexerConfig = InputMultiplexerConfig.AIN0_AIN1;

		protected Builder(Address address) {
			this.address = address;
		}

		public Builder setController(int controller) {
			this.controller = controller;
			return this;
		}

		public Builder setGainConfig(GainConfig gainConfig) {
			this.gainConfig = gainConfig;
			return this;
		}

		public Builder setPga(Pga pga) {
			this.pga = pga;
			return this;
		}

		public Builder setPgaEnabled(boolean pgaEnabled) {
			this.pga = pgaEnabled ? Pga.ENABLED : Pga.DISABLED;
			return this;
		}

		public Builder setDataRate(DataRate dataRate) {
			this.dataRate = dataRate;
			return this;
		}

		public Builder setOperatingMode(OperatingMode operatingMode) {
			this.operatingMode = operatingMode;
			return this;
		}

		public Builder setTurboModeEnabled(boolean turboModeEnabled) {
			this.operatingMode = turboModeEnabled ? OperatingMode.TURBO : OperatingMode.NORMAL;
			return this;
		}

		public Builder setVRef(VRef vRef) {
			this.vRef = vRef;
			return this;
		}

		public Builder setTemperatureSensorMode(TemperatureSensorMode tsMode) {
			this.tsMode = tsMode;
			return this;
		}

		public Builder setTemperatureSensorEnabled(boolean tsEnabled) {
			this.tsMode = tsEnabled ? TemperatureSensorMode.ENABLED : TemperatureSensorMode.DISABLED;
			return this;
		}

		public Builder setDataCounter(DataCounter dataCounter) {
			this.dataCounter = dataCounter;
			return this;
		}

		public Builder setDataCounterEnabled(boolean dcEnabled) {
			this.dataCounter = dcEnabled ? DataCounter.ENABLED : DataCounter.DISABLED;
			return this;
		}

		public Builder setCrcConfig(CrcConfig crcConfig) {
			this.crcConfig = crcConfig;
			return this;
		}

		public Builder setBurnoutCurrentSources(BurnoutCurrentSources burnoutCurrentSources) {
			this.burnoutCurrentSources = burnoutCurrentSources;
			return this;
		}

		public Builder setBurnoutCurrentSourcesEnabled(boolean burnoutCurrentSourcesEnabled) {
			this.burnoutCurrentSources = burnoutCurrentSourcesEnabled ? BurnoutCurrentSources.ENABLED
					: BurnoutCurrentSources.DISABLED;
			return this;
		}

		public Builder setIdacCurrent(IdacCurrent idacCurrent) {
			this.idacCurrent = idacCurrent;
			return this;
		}

		public Builder setIdac1RoutingConfig(Idac1RoutingConfig idac1RoutingConfig) {
			this.idac1RoutingConfig = idac1RoutingConfig;
			return this;
		}

		public Builder setIdac2RoutingConfig(Idac2RoutingConfig idac2RoutingConfig) {
			this.idac2RoutingConfig = idac2RoutingConfig;
			return this;
		}
		
		public Builder setInputMultiplexerConfig(InputMultiplexerConfig inputMultiplexerConfig) {
			this.inputMultiplexerConfig = inputMultiplexerConfig;
			return this;
		}

		public Ads112C04 build() {
			return new Ads112C04(controller, address, gainConfig, pga, dataRate, operatingMode, vRef, tsMode,
					dataCounter, crcConfig, burnoutCurrentSources, idacCurrent, idac1RoutingConfig, idac2RoutingConfig,
					inputMultiplexerConfig);
		}
	}

	public static Builder builder(Address address) {
		return new Builder(address);
	}

	private BoardPinInfo boardPinInfo;
	private I2CDevice device;
	private GainConfig gainConfig;
	private Pga pga;
	private DataRate dataRate;
	private OperatingMode operatingMode;
	private ConversionMode conversionMode;
	private VRef vRef;
	private TemperatureSensorMode tsMode;
	private DataCounter dataCounter;
	private CrcConfig crcConfig;
	private BurnoutCurrentSources burnoutCurrentSources;
	private IdacCurrent idacCurrent;
	private Idac1RoutingConfig idac1RoutingConfig;
	private Idac2RoutingConfig idac2RoutingConfig;
	private InputMultiplexerConfig inputMultiplexerConfig;
	private int lastDataCounter;

	// Testing
	private boolean repeatedStart;
	private boolean method1;

	protected Ads112C04(int controller, Address address, GainConfig gainConfig, Pga pga, DataRate dataRate,
			OperatingMode operatingMode, VRef vRef, TemperatureSensorMode tsMode, DataCounter dataCounter,
			CrcConfig crcConfig, BurnoutCurrentSources burnoutCurrentSources, IdacCurrent idacCurrent,
			Idac1RoutingConfig idac1RoutingConfig, Idac2RoutingConfig idac2RoutingConfig,
			InputMultiplexerConfig inputMultiplexerConfig) {
		super(NAME + "-" + controller + "-" + address.getValue());
		
		this.gainConfig = gainConfig;
		this.pga = pga;
		this.dataRate = dataRate;
		this.operatingMode = operatingMode;
		this.vRef = vRef;
		this.tsMode = tsMode;
		this.dataCounter = dataCounter;
		this.crcConfig = crcConfig;
		this.burnoutCurrentSources = burnoutCurrentSources;
		this.idacCurrent = idacCurrent;
		this.idac1RoutingConfig = idac1RoutingConfig;
		this.idac2RoutingConfig = idac2RoutingConfig;
		this.inputMultiplexerConfig = inputMultiplexerConfig;

		conversionMode = ConversionMode.SINGLE_SHOT;
		lastDataCounter = -1;

		repeatedStart = PropertyUtil.getBooleanProperty("diozero.ads112c04.repeatedStart", true);
		Logger.debug("repeatedStart: {}", Boolean.valueOf(repeatedStart));
		method1 = PropertyUtil.getBooleanProperty("diozero.ads112c04.method1", true);
		Logger.debug("method1: {}", Boolean.valueOf(method1));

		boardPinInfo = new Ads112C04BoardPinInfo();
		device = I2CDevice.builder(address.getValue()).setController(controller).setByteOrder(ByteOrder.BIG_ENDIAN)
				.build();

		sendResetCommand();

		writeConfig0();
		writeConfig1();
		writeConfig2();
		writeConfig3();
	}

	public void sendResetCommand() {
		Logger.debug("sendResetCommand");
		device.writeByte(COMMAND_RESET);
		// TODO Check delays
		SleepUtil.sleepMillis(1);
	}

	public void sendStartCommand() {
		Logger.debug("sendStartCommand");
		device.writeByte(COMMAND_START);
		// TODO Check delays
		SleepUtil.busySleep(10_000);
	}

	public void powerDown() {
		Logger.debug("powerDown");
		device.writeByte(COMMAND_POWER_DOWN);
		// TODO Check delays
		SleepUtil.sleepMillis(1);
	}

	public byte readConfigRegister(ConfigRegister register) {
		if (crcConfig == CrcConfig.DISABLED) {
			return device.readByteData(COMMAND_READ_REG | register.getMask());
		}

		byte[] buffer = new byte[crcConfig == CrcConfig.CRC16 ? 3 : 2];
		device.readI2CBlockData(COMMAND_READ_REG, buffer);
		if (Logger.isTraceEnabled()) {
			Hex.dumpByteArray(buffer);
		}
		if (crcConfig == CrcConfig.CRC16) {
			int calc_crc = Crc.crc16(CRC_PARAMS, buffer[0]);
			int crc = (buffer[1] & 0xff) << 8 | (buffer[2] & 0xff);
			if (crc != calc_crc) {
				Logger.warn("CRC-16 error calculated {}, got {} for value {}", Integer.valueOf(calc_crc),
						Integer.valueOf(crc), Byte.valueOf(buffer[0]));
			}
		} else {
			byte calc_val_inverted = (byte) (~buffer[0]);
			if (buffer[1] != calc_val_inverted) {
				Logger.warn("Data Integrity error calculated {}, got {} for value {}",
						Integer.valueOf(calc_val_inverted), Integer.valueOf(buffer[1]), Byte.valueOf(buffer[0]));
			}
		}

		return buffer[0];
	}

	private void writeConfigRegister(ConfigRegister register, byte value) {
		device.writeByteData(COMMAND_WRITE_REG | register.getMask(), value);
		// TODO Check delays
	}

	private void writeConfig0() {
		/*-
		 * 1. Input Multiplexer
		 * For settings where AINN = AVSS, the PGA must be disabled (PGA_BYPASS = 1) and only gains 1, 2, and 4 can
		 * be used.
		 */
		if ((inputMultiplexerConfig.isCompareToAvss())) {
			// For settings where AINN = AVSS, the PGA must be disabled (PGA_BYPASS = 1) and
			// only gains 1, 2, and 4 can be used.
			if (pga.isEnabled()) {
				throw new IllegalArgumentException("The PGA must be disabled for non-differential reads");
			}
			if (gainConfig.getGain() > GainConfig._4.getGain()) {
				throw new IllegalArgumentException("Only gains 1, 2, and 4 can be used for non-differential reads");
			}
		}

		/*-
		 * 2. Gain
		 * The PGA can only be disabled for gains 1, 2, and 4.
		 * The PGA is always enabled for gain settings 8 to 128, regardless of the PGA_BYPASS setting.
		 */
		if (!pga.isEnabled() && gainConfig.getGain() > GainConfig._4.getGain()) {
			throw new IllegalArgumentException(
					"The PGA can only be disabled for gains 1, 2, and 4 (requested gain: " + gainConfig + ")");
		}

		// Logger.debug("setConfig0");
		writeConfigRegister(ConfigRegister._0,
				(byte) (inputMultiplexerConfig.getMask() | gainConfig.getMask() | pga.getMask()));
	}

	private void writeConfig1() {
		// Logger.debug("setConfig1");
		writeConfigRegister(ConfigRegister._1, (byte) (dataRate.getMask() | operatingMode.getMask()
				| conversionMode.getMask() | vRef.getMask() | tsMode.getMask()));
	}

	private void writeConfig2() {
		// Logger.debug("setConfig2");
		writeConfigRegister(ConfigRegister._2, (byte) (dataCounter.getMask() | crcConfig.getMask()
				| burnoutCurrentSources.getMask() | idacCurrent.getMask()));
	}

	private void writeConfig3() {
		// Logger.debug("setConfig3");
		writeConfigRegister(ConfigRegister._3, (byte) (idac1RoutingConfig.getMask() | idac2RoutingConfig.getMask()));
	}

	public InputMultiplexerConfig getInputMultiplexerConfig() {
		return inputMultiplexerConfig;
	}

	public GainConfig getGainConfig() {
		return gainConfig;
	}

	public void setGainConfig(GainConfig gainConfig) {
		this.gainConfig = gainConfig;
		writeConfig0();
	}

	public Pga getPga() {
		return pga;
	}

	public void setPga(Pga pga) {
		this.pga = pga;
		writeConfig0();
	}

	public DataRate getDataRate() {
		return dataRate;
	}

	public void setDataRate(DataRate dataRate) {
		this.dataRate = dataRate;
		writeConfig1();
	}

	public boolean isTurboModeEnabled() {
		return operatingMode == OperatingMode.TURBO;
	}

	public void setTurboModeEnabled(boolean enabled) {
		this.operatingMode = enabled ? OperatingMode.TURBO : OperatingMode.NORMAL;
		writeConfig1();
	}

	public int getDataRateFrequency() {
		return dataRate.getDataRate() * operatingMode.getMultiplier();
	}
	
	public VRef getVRefConfig() {
		return vRef;
	}

	public void setVRefConfig(VRef vRef) {
		this.vRef = vRef;
		writeConfig1();
	}

	public boolean isTemperatureSensorModeEnabled() {
		return tsMode.isEnabled();
	}

	public void setTemperatureSensorModeEnabled(boolean enabled) {
		this.tsMode = enabled ? TemperatureSensorMode.ENABLED : TemperatureSensorMode.DISABLED;
		writeConfig1();
	}

	public boolean isDataCounterEnabled() {
		return dataCounter.isEnabled();
	}

	public void setDataCounterEnabled(boolean enabled) {
		this.dataCounter = enabled ? DataCounter.ENABLED : DataCounter.DISABLED;
		writeConfig2();
	}

	public CrcConfig getCrcConfig() {
		return crcConfig;
	}

	public void setCrcConfig(CrcConfig crcConfig) {
		this.crcConfig = crcConfig;
		writeConfig2();
	}

	public BurnoutCurrentSources getBurnoutCurrentSources() {
		return burnoutCurrentSources;
	}

	public void setBurnoutCurrentSources(BurnoutCurrentSources burnoutCurrentSources) {
		this.burnoutCurrentSources = burnoutCurrentSources;
		writeConfig2();
	}

	public IdacCurrent getIdacCurrent() {
		return idacCurrent;
	}

	public void setIdacCurrent(IdacCurrent idacCurrent) {
		this.idacCurrent = idacCurrent;
		writeConfig2();
	}

	public Idac1RoutingConfig getIdac1RoutingConfig() {
		return idac1RoutingConfig;
	}

	public void setIdac1RoutingConfig(Idac1RoutingConfig idac1RoutingConfig) {
		this.idac1RoutingConfig = idac1RoutingConfig;
		writeConfig3();
	}

	public Idac2RoutingConfig getIdac2RoutingConfig() {
		return idac2RoutingConfig;
	}

	public void setIdac2RoutingConfig(Idac2RoutingConfig idac2RoutingConfig) {
		this.idac2RoutingConfig = idac2RoutingConfig;
		writeConfig3();
	}

	public void setConfig0(InputMultiplexerConfig inputMultiplexerConfig, GainConfig gainConfig, Pga pga) {
		this.inputMultiplexerConfig = inputMultiplexerConfig;
		this.gainConfig = gainConfig;
		this.pga = pga;
		writeConfig0();
	}

	public void setConfig1(DataRate dataRate, boolean turboModeEnabled, VRef vRef, boolean temperatureSensorEnabled) {
		this.dataRate = dataRate;
		this.operatingMode = turboModeEnabled ? OperatingMode.TURBO : OperatingMode.NORMAL;
		this.vRef = vRef;
		this.tsMode = temperatureSensorEnabled ? TemperatureSensorMode.ENABLED : TemperatureSensorMode.DISABLED;
		writeConfig1();
	}

	public void setConfig2(boolean dataCounterEnabled, CrcConfig crcConfig, BurnoutCurrentSources burnoutCurrentSources,
			IdacCurrent idacCurrent) {
		this.dataCounter = dataCounterEnabled ? DataCounter.ENABLED : DataCounter.DISABLED;
		this.crcConfig = crcConfig;
		this.burnoutCurrentSources = burnoutCurrentSources;
		this.idacCurrent = idacCurrent;
		writeConfig2();
	}

	public void setConfig3(Idac1RoutingConfig idac1RoutingConfig, Idac2RoutingConfig idac2RoutingConfig) {
		this.idac1RoutingConfig = idac1RoutingConfig;
		this.idac2RoutingConfig = idac2RoutingConfig;
		writeConfig3();
	}

	private static InputMultiplexerConfig getInputMultiplexerConfig(int adcNumber) {
		InputMultiplexerConfig im;
		switch (adcNumber) {
		case 0:
			im = InputMultiplexerConfig.AIN0_AVSS;
			break;
		case 1:
			im = InputMultiplexerConfig.AIN1_AVSS;
			break;
		case 2:
			im = InputMultiplexerConfig.AIN2_AVSS;
			break;
		case 3:
			im = InputMultiplexerConfig.AIN3_AVSS;
			break;
		default:
			throw new IllegalArgumentException("Invalid input channel number - " + adcNumber);
		}
		return im;
	}

	public void setSingleShotMode() {
		// System.out.println("getValueSingle");
		conversionMode = ConversionMode.SINGLE_SHOT;
		writeConfig1();

		// Start command must be issued each time the CM bit is changed
		sendStartCommand();
	}

	/**
	 * Disable continuous readings and take a single-shot reading on the specified
	 * ADC number (non-differential reads).
	 * 
	 * @param adcNumber The ADC number to read from
	 * @return The current raw Analog reading
	 */
	public short getSingleShotReadingNonDifferential(int adcNumber) {
		return getSingleShotReading(getInputMultiplexerConfig(adcNumber));
	}

	/**
	 * Disable continuous readings and take a single-shot reading on the specified
	 * ADC number.
	 * 
	 * For settings where AINN = AVSS, the PGA must be disabled (PGA_BYPASS = 1) and
	 * only gains 1, 2, and 4 can be used.
	 * 
	 * @param inputMultiplexerConfig the input multiplexer configuration
	 * @return The current raw Analog reading
	 */
	public short getSingleShotReading(InputMultiplexerConfig inputMultiplexerConfig) {
		if (this.inputMultiplexerConfig != inputMultiplexerConfig) {
			this.inputMultiplexerConfig = inputMultiplexerConfig;
			writeConfig0();
		}
		
		if (conversionMode != ConversionMode.SINGLE_SHOT) {
			conversionMode = ConversionMode.SINGLE_SHOT;
			writeConfig1();
		}

		// Must issue a start command to trigger a new reading
		sendStartCommand();

		return method1 ? getReadingOnDataReadyBit() : getReadingOnDataReadyBit2();
	}

	/**
	 * Enable continuous read mode for the specified ADC number (AINp =
	 * AIN{adcNumber}, AINn = AVSS).
	 * Note the PGA must be disabled and only gains 1, 2, and 4 can be used.
	 * 
	 * @param adcNumber The ADC to continuously read from (non-differential mode)
	 */
	public void setContinuousModeNonDifferential(int adcNumber) {
		setContinuousMode(getInputMultiplexerConfig(adcNumber));
	}
	
	/**
	 * Enable continuous read mode for the specified input multiplexer value.
	 * For settings where AINN = AVSS, the PGA must be disabled and only gains 1, 2, and 4 can be used.
	 * 
	 * @param inputMultiplexerConfig The input multiplexer configuration
	 */
	public void setContinuousMode(InputMultiplexerConfig inputMultiplexerConfig) {
		if (this.inputMultiplexerConfig != inputMultiplexerConfig) {
			this.inputMultiplexerConfig = inputMultiplexerConfig;
			writeConfig0();
		}
		
		if (conversionMode != ConversionMode.CONTINUOUS) {
			conversionMode = ConversionMode.CONTINUOUS;
			writeConfig1();
		}

		sendStartCommand();
	}

	public Object setContinuousModeNonDifferential(int adcNumber, DigitalInputDevice intr, Consumer<AdcEvent> listener) {
		final Object lock = new Object();
		DiozeroScheduler.getNonDaemonInstance().submit(() -> {
			final EventQueue<AdcEvent> event_queue = new EventQueue<>();
			event_queue.addListener(listener);
			intr.whenActivated(nano_time -> {
				short reading = readData(2);
				event_queue.accept(new AdcEvent(nano_time / 1_000_000, nano_time, reading));
			});
			setContinuousModeNonDifferential(adcNumber);
			
			try {
				// Wait forever until interrupted
				lock.wait();
			} catch (InterruptedException e) {
				Logger.info("Interrupted");
			}
			intr.whenActivated(null);
			
			event_queue.stop();
		});
		
		return lock;
	}

	/**
	 * Read data whenever the data read bit is set in Config Register #2
	 * 
	 * @return the raw analog data reading in signed short format
	 */
	public short getReadingOnDataReadyBit() {
		/*-
		 * DC enabled and CRC disabled is 3 bytes (1 DC, 2 data)
		 * DC enabled and CRC enabled is 5 bytes (1 DC, 2 data, 2 CRC). 
		 * DC disabled and CRC enabled is 4 bytes (2 data, 2 CRC). 
		 * DC enabled and CRC inverted is 6 bytes (1 DC, 2 data, 1 DC inv. and 2 data inv.).
		 * DC disabled and CRC inverted is 4 bytes (2 data, 2 data inv.).
		 */
		int bytes_to_read = 2;
		if (dataCounter.isEnabled()) {
			bytes_to_read++;
		}
		if (crcConfig != CrcConfig.DISABLED) {
			bytes_to_read += 2;
			if (dataCounter.isEnabled() && crcConfig == CrcConfig.INVERTED_DATA_OUTPUT) {
				bytes_to_read++;
			}
		}

		// Logger.debug("Waiting for data to be available...");
		// Wait for the Data Ready bit to be set in config register #2
		while (true) {
			if ((readConfigRegister(ConfigRegister._2) & C2_DATA_RDY_MASK) != 0) {
				break;
			}
			// 100 nS
			SleepUtil.busySleep(100);
		}
		// Logger.debug("Data available");

		// SleepUtil.sleepMillis(2);

		return readData(bytes_to_read);
	}

	public short readData(int bytes_to_read) {
		/*-
		byte[] buffer = device.readI2CBlockDataByteArray(COMMAND_RDATA, bytes_to_read);
		Logger.debug("Read {} bytes:", Integer.valueOf(buffer.length));
		*/
		byte[] buffer = new byte[1 + bytes_to_read];
		// device.readNoStop(COMMAND_RDATA, bytes_to_read, buffer, repeatedStart);
		buffer[0] = COMMAND_RDATA;
		I2CMessage messages[] = new I2CMessage[2];
		messages[0] = new I2CMessage(I2CMessage.I2C_M_WR, 1);
		messages[1] = new I2CMessage(I2CMessage.I2C_M_RD, bytes_to_read);

		device.readWrite(messages, buffer);
		if (Logger.isTraceEnabled()) {
			Hex.dumpByteArray(buffer);
		}

		ByteBuffer bb = ByteBuffer.wrap(buffer, 1, bytes_to_read);
		bb.order(ByteOrder.BIG_ENDIAN);
		int counter = -1;
		if (dataCounter.isEnabled()) {
			counter = bb.get() & 0xff;
			Logger.debug("Conversion counter: {}", Integer.valueOf(counter));
		}

		short value = bb.getShort();
		if (crcConfig != CrcConfig.DISABLED) {
			// Validate the CRC value
			if (crcConfig == CrcConfig.INVERTED_DATA_OUTPUT) {
				// A bitwise-inverted version of the data
				if (dataCounter.isEnabled()) {
					int counter_inverted = bb.get() & 0xff;
					int calc_counter_inverted = ~counter & 0xff;
					if (calc_counter_inverted != counter_inverted) {
						Logger.warn("Inversion error for counter {}, calculated {}, got {}", Integer.valueOf(counter),
								Integer.valueOf(calc_counter_inverted), Integer.valueOf(counter_inverted));
					}
				}
				short value_inverted = bb.getShort();
				short calc_val_inverted = (short) (~value);
				if (calc_val_inverted != value_inverted) {
					Logger.warn("Inversion error for data {}, calculated {}, got {}. DC Enabled: {}{}",
							Short.valueOf(value), Short.valueOf(calc_val_inverted), Short.valueOf(value_inverted),
							Boolean.valueOf(dataCounter.isEnabled()), dataCounter.isEnabled() ? " - " + counter : "");
				}
			} else if (crcConfig == CrcConfig.CRC16) {
				int crc_val = bb.getShort() & 0xffff;
				// In CRC mode, the checksum bytes are the 16-bit remainder of the bitwise
				// exclusive-OR (XOR) of the data bytes with a CRC polynomial
				/*-
				 * The CRC is "for the entire data being returned"
				 * i.e. includes the data counter if present
				 * https://e2e.ti.com/support/data-converters/f/73/t/758829
				 * From the datasheet:
				 * The optional data counter word that precedes conversion data is covered by both data
				 * integrity options.
				 */
				int calc_crc_val;
				if (dataCounter.isEnabled()) {
					calc_crc_val = Crc.crc16(CRC_PARAMS, (byte) counter, (byte) (value >> 8), (byte) value);
				} else {
					calc_crc_val = Crc.crc16Short(CRC_PARAMS, value);
				}
				if (calc_crc_val != crc_val) {
					Logger.warn("CRC error for value {}, calculated {}, got {}. DC Enabled: {}{}", Short.valueOf(value),
							Integer.valueOf((calc_crc_val)), Integer.valueOf(crc_val),
							Boolean.valueOf(dataCounter.isEnabled()), dataCounter.isEnabled() ? " - " + counter : "");
				}
			}
		}

		return value;
	}

	/**
	 * Read data whenever the data read bit is set in Config Register #2
	 * 
	 * @return the raw analog data reading in signed short format
	 */
	public short getReadingOnDataReadyBit2() {
		/*-
		 * DC disabled and CRC disabled is 2 bytes (2 data)
		 * DC enabled and CRC disabled is 3 bytes (1 DC, 2 data)
		 * DC enabled and CRC enabled is 5 bytes (1 DC, 2 data, 2 CRC). 
		 * DC disabled and CRC enabled is 4 bytes (2 data, 2 CRC). 
		 * DC enabled and CRC inverted is 6 bytes (1 DC, 2 data, 1 DC inv. and 2 data inv.).
		 * DC disabled and CRC inverted is 4 bytes (2 data, 2 data inv.).
		 */
		int bytes_to_read = 2;
		if (dataCounter.isEnabled()) {
			// The data counter prefix
			bytes_to_read++;
		}
		if (crcConfig != CrcConfig.DISABLED) {
			// The data integrity check for data
			bytes_to_read += 2;
			if (dataCounter.isEnabled() && crcConfig == CrcConfig.INVERTED_DATA_OUTPUT) {
				// The inverted data counter
				bytes_to_read++;
			}
		}

		I2CDeviceInterface.I2CMessage[] messages = { //
				new I2CDeviceInterface.I2CMessage(I2CDeviceInterface.I2CMessage.I2C_M_WR, 1), // Write config register 2
				new I2CDeviceInterface.I2CMessage(I2CDeviceInterface.I2CMessage.I2C_M_RD, 1), // Read the value
				new I2CDeviceInterface.I2CMessage(I2CDeviceInterface.I2CMessage.I2C_M_WR, 1), // Write data register
				new I2CDeviceInterface.I2CMessage(I2CDeviceInterface.I2CMessage.I2C_M_RD, bytes_to_read) // Read the
																											// value
		};

		// WRITE Config Reg 2 Addr, READ Config Reg 2 val, WRITE RDATA Addr, READ RDATA
		// value
		byte[] buffer = new byte[3 + bytes_to_read];
		// Write Config Register #2
		buffer[0] = (byte) (COMMAND_READ_REG | ConfigRegister._2.getMask());
		// Placeholder for Config Register #2 data
		buffer[1] = 0;
		// Write Data Register
		buffer[2] = COMMAND_RDATA;
		// Buffer 3..end RDATA value

		// Wait for the Data Ready bit to be set in config register #2
		// Logger.debug("Waiting for data to be available...");
		while (true) {
			device.readWrite(messages, buffer);
			if ((buffer[1] & C2_DATA_RDY_MASK) != 0) {
				break;
			}
			// 100 nS before trying again
			SleepUtil.busySleep(100);
		}
		// Logger.debug("Data available");

		if (Logger.isTraceEnabled()) {
			Hex.dumpByteArray(buffer);
		}

		ByteBuffer bb = ByteBuffer.wrap(buffer, 3, bytes_to_read);
		bb.order(ByteOrder.BIG_ENDIAN);
		int counter = -1;
		if (dataCounter.isEnabled()) {
			counter = bb.get() & 0xff;
			Logger.debug("Conversion counter: {}", Integer.valueOf(counter));
		}

		short value = bb.getShort();
		if (crcConfig != CrcConfig.DISABLED) {
			// Validate the CRC value
			if (crcConfig == CrcConfig.INVERTED_DATA_OUTPUT) {
				// A bitwise-inverted version of the data
				if (dataCounter.isEnabled()) {
					int counter_inverted = bb.get() & 0xff;
					int calc_counter_inverted = ~counter & 0xff;
					if (calc_counter_inverted != counter_inverted) {
						Logger.warn("Inversion error for counter {}, calculated {}, got {}", Integer.valueOf(counter),
								Integer.valueOf(calc_counter_inverted), Integer.valueOf(counter_inverted));
					}
				}
				short value_inverted = bb.getShort();
				short calc_val_inverted = (short) (~value);
				if (calc_val_inverted != value_inverted) {
					Logger.warn("Inversion error for data {}, calculated {}, got {}. DC Enabled: {}{}",
							Short.valueOf(value), Short.valueOf(calc_val_inverted), Short.valueOf(value_inverted),
							Boolean.valueOf(dataCounter.isEnabled()), dataCounter.isEnabled() ? " - " + counter : "");
				}
			} else if (crcConfig == CrcConfig.CRC16) {
				int crc_val = bb.getShort() & 0xffff;
				// In CRC mode, the checksum bytes are the 16-bit remainder of the bitwise
				// exclusive-OR (XOR) of the data bytes with a CRC polynomial
				/*-
				 * The CRC is "for the entire data being returned"
				 * i.e. includes the data counter if present
				 * https://e2e.ti.com/support/data-converters/f/73/t/758829
				 * From the datasheet:
				 * The optional data counter word that precedes conversion data is covered by both data
				 * integrity options.
				 */
				int calc_crc_val;
				if (dataCounter.isEnabled()) {
					calc_crc_val = Crc.crc16(CRC_PARAMS, (byte) counter, (byte) (value >> 8), (byte) value);
				} else {
					calc_crc_val = Crc.crc16Short(CRC_PARAMS, value);
				}
				if (calc_crc_val != crc_val) {
					Logger.warn("CRC error for value {}, calculated {}, got {}. DC Enabled: {}{}", Short.valueOf(value),
							Integer.valueOf((calc_crc_val)), Integer.valueOf(crc_val),
							Boolean.valueOf(dataCounter.isEnabled()), dataCounter.isEnabled() ? " - " + counter : "");
				}
			}
		}

		return value;
	}

	public short getReadingOnDataCounterChange() {
		// Data counter must be available for this method to work
		if (dataCounter == DataCounter.DISABLED) {
			throw new IllegalArgumentException("Data counter must be enabled");
		}

		byte[] buffer;
		switch (crcConfig) {
		case CRC16:
			buffer = new byte[5];
			break;
		case INVERTED_DATA_OUTPUT:
			buffer = new byte[6];
			break;
		case DISABLED:
		default:
			buffer = new byte[3];
		}

		short value;
		while (true) {
			device.readI2CBlockData(COMMAND_RDATA, buffer);
			int new_dc = buffer[0] & 0xff;
			if (new_dc != lastDataCounter) {
				if (lastDataCounter != -1 && (new_dc != lastDataCounter + 1)) {
					Logger.info("Missed a reading - last DC: {}, new DC: {}", Integer.valueOf(lastDataCounter),
							Integer.valueOf(new_dc));
				}
				lastDataCounter = new_dc;
				// TODO If DI is set to inverted, buffer[1] is the inversion of the DC
				value = (short) ((buffer[1] << 8) | (buffer[2] & 0xff));
				// TODO Data Integrity validation
				break;
			}
			SleepUtil.busySleep(100);
		}

		return value;
	}

	@Override
	public String getName() {
		return NAME + "-" + device.getController() + "-" + device.getAddress();
	}

	@Override
	public BoardPinInfo getBoardPinInfo() {
		return boardPinInfo;
	}

	@Override
	public AnalogInputDeviceInterface createAnalogInputDevice(String key, PinInfo pinInfo) {
		return new Ads112C04AnalogInputDevice(this, key, pinInfo.getDeviceNumber());
	}

	@Override
	public float getVRef() {
		// FIXME Can this actually be determined?
		return 3.3f;
	}

	@Override
	public void close() {
		Logger.trace("close()");
		// Close all open pins before closing the I2C device itself
		super.close();
		device.close();
	}
	
	public class AdcEvent extends Event {
		private short reading;

		public AdcEvent(long epochTime, long nanoTime, short reading) {
			super(epochTime, nanoTime);
			this.reading = reading;
		}

		public short getReading() {
			return reading;
		}
	}
	
	private static class Ads112C04BoardPinInfo extends BoardPinInfo {
		public Ads112C04BoardPinInfo() {
			for (int i = 0; i < NUM_CHANNELS; i++) {
				addAdcPinInfo(i, i);
			}
		}
	}

	private static final class Ads112C04AnalogInputDevice extends AbstractInputDevice<AnalogInputEvent>
			implements AnalogInputDeviceInterface {
		private Ads112C04 ads112C04;
		private int adcNumber;

		public Ads112C04AnalogInputDevice(Ads112C04 ads112C04, String key, int adcNumber) {
			super(key, ads112C04);

			this.ads112C04 = ads112C04;
			this.adcNumber = adcNumber;
		}

		@Override
		protected void closeDevice() {
			Logger.trace("closeDevice()");
			// Nothing to do?
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public float getValue() throws RuntimeIOException {
			return ads112C04.getSingleShotReadingNonDifferential(adcNumber);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public int getAdcNumber() {
			return adcNumber;
		}
	}
}
