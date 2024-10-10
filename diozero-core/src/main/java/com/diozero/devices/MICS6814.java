package com.diozero.devices;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     MICS6814.java
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

import java.io.Closeable;
import java.nio.ByteOrder;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.tinylog.Logger;

import com.diozero.api.DigitalInputDevice;
import com.diozero.api.I2CDevice;
import com.diozero.api.I2CDeviceInterface;
import com.diozero.api.I2CDeviceInterface.I2CMessage;
import com.diozero.util.Hex;
import com.diozero.util.SleepUtil;

/**
 * Pimoroni I2C packaged with Nuvoton MS51XB9AE MCU for ADC
 * 
 * I2C reference:
 * https://github.com/pimoroni/mics6814-python/blob/master/library/mics6814/__init__.py
 * 
 * https://shop.pimoroni.com/products/mics6814-gas-sensor-breakout?variant=39296409305171
 * 
 * Interpreting readings:
 * https://learn.pimoroni.com/article/getting-started-with-enviro-plus
 */
public class MICS6814 implements Closeable {
	public static void main(String[] args) {
		try (MICS6814 sensor = new MICS6814(0)) {
			for (int i = 0; i < 10; i++) {
				System.out.println(sensor.readAll());
				SleepUtil.sleepSeconds(1);
			}
		}
	}

	public static enum ClockDivider {
		_1(1, 0b000), _2(2, 0b001), _4(4, 0b010), _8(8, 0b011), _16(16, 0b100), _32(32, 0b101), _64(64, 0b110),
		_128(128, 0b111);

		public static ClockDivider of(int value) {
			switch (value) {
			case 1:
				return _1;
			case 2:
				return _2;
			case 4:
				return _4;
			case 8:
				return _8;
			case 16:
				return _16;
			case 32:
				return _32;
			case 64:
				return _64;
			case 128:
				return _128;
			default:
				throw new IllegalArgumentException("Invalid ClockDivider value " + value);
			}
		}

		private int value;
		private int pwmdiv2;

		private ClockDivider(int value, int pwmdiv1) {
			this.value = value;
		}

		public int value() {
			return value;
		}

		public int pwmdiv2() {
			return pwmdiv2;
		}
	}

	private static interface PinModeConstants {
		int PIN_MODE_IO = 0b00000;
		int PIN_MODE_QB = 0b00000; // Output, Quasi-Bidirectional mode
		int PIN_MODE_PP = 0b00001;
		int PIN_MODE_IN = 0b00010;
		int PIN_MODE_PU = 0b10000;
		int PIN_MODE_OD = 0b00011;
		int PIN_MODE_PWM = 0b00101;
		int PIN_MODE_ADC = 0b01010;

		/*-
		static final String[] MODE_NAMES = { "IO", "PWM", "ADC" };
		static final String[] GPIO_NAMES = { "QB", "PP", "IN", "OD" };
		static final String[] STATE_NAMES = { "LOW", "HIGH" };
		*/

		// More convenient names for the pin functions
		/*-
		int IN = PIN_MODE_IN;
		int IN_PULL_UP = PIN_MODE_PU;
		int IN_PU = PIN_MODE_PU;
		int OUT = PIN_MODE_PP;
		int PWM = PIN_MODE_PWM;
		int ADC = PIN_MODE_ADC;
		*/
	}

	public static enum PinMode implements PinModeConstants {
		// These values encode our desired pin function: IO, ADC, PWM
		// alongside the GPIO MODE for that port and pin (section 8.1)
		// the 5th bit additionally encodes the default output state
		DIGITAL_INPUT_OUTPUT(PIN_MODE_IO), // General IO mode, IE: not ADC or PWM
		DIGITAL_OUTPUT_QUASI_BIDIRECTIONAL(PIN_MODE_QB), // Output, Quasi-Bidirectional mode
		DIGITAL_OUTPUT_PUSH_PULL(PIN_MODE_PP), // Output, Push-Pull mode
		DIGITAL_INPUT(PIN_MODE_IN), // Input-only (high-impedance)
		DIGITAL_INPUT_PULLUP(PIN_MODE_PU), // Input (with pull-up)
		DIGITAL_OUTPUT_OPEN_DRAIN(PIN_MODE_OD), // Output, Open-Drain mode
		PWM_OUTPUT(PIN_MODE_PWM), // PWM, Output, Push-Pull mode
		ANALOG_INPUT(PIN_MODE_ADC); // ADC, Input-only (high-impedance)

		private byte value;

		private PinMode(int value) {
			this.value = (byte) value;
		}

		public byte value() {
			return value;
		}
	}

	public static class MICS6814Reading {
		private final double oxidising;
		private final double reducing;
		private final double nh3;
		private final double adc;

		public MICS6814Reading(double oxidising, double reducing, double nh3, double adc) {
			this.oxidising = oxidising;
			this.reducing = reducing;
			this.nh3 = nh3;
			this.adc = adc;
		}

		@Override
		public String toString() {
			return String.format(
					"MICS6814Reading [Oxidising (NO2): %.2f Ohms, Reducing (CO): %.2f Ohms, NH3: %.2f Ohms, ADC (ref): %.2f Volts]",
					Double.valueOf(oxidising), Double.valueOf(reducing), Double.valueOf(nh3), Double.valueOf(adc));
		}
	}

	private static interface Constants {
		int DEFAULT_I2C_ADDR = 0x18;

		int CHIP_ID = 0xE26A;
		int CHIP_VERSION = 2;

		int LED_R_PIN = 3; // P1.2
		int LED_G_PIN = 7; // P1.1
		int LED_B_PIN = 2; // P1.0

		int VREF_PIN = 14; // P1.4 AIN0 - 3v3
		int REDUCING_PIN = 13; // P0.7 AIN2 - Reducing (CO)
		int NH3_PIN = 11; // P0.6 AIN3 - NH3
		int OXIDISING_PIN = 12; // P0.5 AIN4 - Oxidising (NO2)

		int HEATER_ENABLE_PIN = 1; // P1.5 Heater Enable
	}

	private PimoroniIOExpander ioe;
	private final Optional<DigitalInputDevice> interrupt;
	private final int chipId;
	private float ledBrightnessPercentage;

	public MICS6814(final int controller) {
		this(controller, Constants.DEFAULT_I2C_ADDR, Optional.empty());
	}

	public MICS6814(final int controller, final int address, Optional<DigitalInputDevice> interrupt) {
		ioe = new PimoroniIOExpander(controller, address, false);

		this.interrupt = interrupt;

		chipId = ioe.getChipId();
		if (chipId != Constants.CHIP_ID) {
			Logger.error("Expected chip id 0x{}, got 0x{}", Hex.encode(Constants.CHIP_ID), Hex.encode(chipId));
		}

		Logger.info("Chip version: {} (expected {})", Integer.valueOf(ioe.getChipVersion()),
				Integer.valueOf(Constants.CHIP_VERSION));

		ioe.setPwmPeriod(5100);
		ioe.setPwmControl(ClockDivider._1);

		setLedBrightness(1);

		ioe.setMode(Constants.LED_R_PIN, PinMode.PWM_OUTPUT); // P1.2 LED Red
		ioe.setMode(Constants.LED_G_PIN, PinMode.PWM_OUTPUT); // P1.1 LED Green
		ioe.setMode(Constants.LED_B_PIN, PinMode.PWM_OUTPUT); // P1.0 LED Blue

		ioe.setMode(Constants.VREF_PIN, PinMode.ANALOG_INPUT); // P1.4 AIN5 - 2v8
		ioe.setMode(Constants.REDUCING_PIN, PinMode.ANALOG_INPUT); // P0.5 AIN4 - Reducing
		ioe.setMode(Constants.NH3_PIN, PinMode.ANALOG_INPUT); // P0.6 AIN3 - NH3
		ioe.setMode(Constants.OXIDISING_PIN, PinMode.ANALOG_INPUT); // P0.7 AIN2 - Oxidising

		ioe.setMode(Constants.HEATER_ENABLE_PIN, PinMode.DIGITAL_OUTPUT_PUSH_PULL); // P1.5 Heater Enable
		setHeater(true);
	}

	@Override
	public void close() {
		setLedBrightness(0);
		setLed(0, 0, 0);
		disableHeater();
		ioe.close();
	}

	/**
	 * Set the LED brightness.
	 * 
	 * @param ledBrightness: From 0.0 to 1.0
	 */
	public void setLedBrightness(float ledBrightness) {
		this.ledBrightnessPercentage = ledBrightness;
	}

	public void setHeater(boolean value) {
		ioe.output(Constants.HEATER_ENABLE_PIN, value ? IOConstants.LOW : IOConstants.HIGH);
	}

	public void disableHeater() {
		ioe.output(Constants.HEATER_ENABLE_PIN, IOConstants.HIGH);
		ioe.setMode(Constants.HEATER_ENABLE_PIN, PinMode.DIGITAL_INPUT);
	}

	public double getRawRef() {
		// Return raw reference ADC reading.
		return ioe.input(Constants.VREF_PIN);
	}

	public double getRawReducing() {
		// Return raw Reducing Gases reading.
		return ioe.input(Constants.REDUCING_PIN);
	}

	public double getRawNh3() {
		// Return raw NH3 ADC reading.
		return ioe.input(Constants.NH3_PIN);
	}

	public double getRawOxidising() {
		// Return raw Oxidising Gases ADC reading
		return ioe.input(Constants.OXIDISING_PIN);
	}

	/**
	 * Set onboard LED to RGB value.
	 * 
	 * @param r Amount of Red (0-255)
	 * @param g Amount of Green (0-255)
	 * @param b Amount of Blue (0-255)
	 */
	public void setLed(int r, int g, int b) {
		final int r_scaled = (int) (ioe.pwmPeriod - (r * ioe.pwmPeriod / 255.0 * ledBrightnessPercentage));
		final int g_scaled = (int) (ioe.pwmPeriod - (g * ioe.pwmPeriod / 255.0 * ledBrightnessPercentage));
		final int b_scaled = (int) (ioe.pwmPeriod - (b * ioe.pwmPeriod / 255.0 * ledBrightnessPercentage));

		ioe.output(Constants.LED_R_PIN, r_scaled);
		ioe.output(Constants.LED_G_PIN, g_scaled);
		ioe.output(Constants.LED_B_PIN, b_scaled);
	}

	public MICS6814Reading readAll() {
		// Return gas resistance for oxidising, reducing and NH3
		double raw_vref = getRawRef();
		double reducing = getRawReducing();
		double nh3 = getRawNh3();
		double oxdising = getRawOxidising();

		double vref = ioe.getAdcVref();

		reducing = (reducing * 56000) / (vref - reducing);
		if (Double.isInfinite(reducing)) {
			reducing = 0;
		}

		nh3 = (nh3 * 56000) / (vref - nh3);
		if (Double.isInfinite(nh3)) {
			nh3 = 0;
		}

		oxdising = (oxdising * 56000) / (vref - oxdising);
		if (Double.isInfinite(oxdising)) {
			oxdising = 0;
		}

		return new MICS6814Reading(oxdising, reducing, nh3, raw_vref);
	}

	/**
	 * Return gas resistance for oxidising gases. E.g. chlorine, nitrous oxide
	 * 
	 * @return Oxidising gases
	 */
	public double readOxidising() {
		return readAll().oxidising;
	}

	/**
	 * Return gas resistance for reducing gases, e.g. hydrogen, carbon monoxide
	 * 
	 * @return Reducing gases
	 */
	public double readReducing() {
		return readAll().reducing;
	}

	public double readNh3() {
		// Return gas resistance for nh3/ammonia
		return readAll().nh3;
	}

	public double readAdc() {
		// Return spare ADC channel value
		return readAll().adc;
	}

	private static abstract class Pin {
		private final int encChannel;
		protected final EnumSet<PinMode> type;
		private final int port;
		private final int pin;
		private Optional<PinMode> mode;
		private boolean invertOutput;

		public Pin(int encChannel, int port, int pin, EnumSet<PinMode> type) {
			this.encChannel = encChannel;
			this.port = port;
			this.pin = pin;
			if (type.isEmpty()) {
				this.type = EnumSet.of(PinMode.DIGITAL_INPUT_OUTPUT);
			} else {
				this.type = type;
			}
			this.mode = Optional.empty();
			this.invertOutput = false;
		}

		public boolean isInverted() {
			return invertOutput;
		}

		public void setInverted(boolean invertOutput) {
			this.invertOutput = invertOutput;
		}
	}

	private static class PwmPin extends Pin {
		protected final int regIoPwm;
		protected final int bitIoPwm;
		protected final int pwmModule;
		protected final int pwmChannel;
		protected boolean usingAlt;

		public PwmPin(int encChannel, int port, int pin, int regIoPwm, int bitIoPwm, int pwmModule, int pwmChannel) {
			super(encChannel, port, pin, EnumSet.of(PinMode.PWM_OUTPUT));

			this.regIoPwm = regIoPwm;
			this.bitIoPwm = bitIoPwm;
			this.pwmModule = pwmModule;
			this.pwmChannel = pwmChannel;

			usingAlt = false;
		}

		public boolean isUsingAlt() {
			return usingAlt;
		}

		public int getActiveModule() {
			return pwmModule;
		}

		public int getActiveChannel() {
			return pwmChannel;
		}
	}

	private static class DualPwmPin extends PwmPin {
		private final int regAuxr;
		private final int bitAuxr;
		private final int valAuxr;
		private final int pwmAltModule;
		private final int pwmAltChannel;

		public DualPwmPin(int encChannel, int port, int pin, int regIoPwm, int bitIoPwm, int pwmModule, int pwmChannel,
				int regAuxr, int bitAuxr, int valAuxr, int pwmAltModule, int pwmAltChannel) {
			super(encChannel, port, pin, regIoPwm, bitIoPwm, pwmModule, pwmChannel);

			this.regAuxr = regAuxr;
			this.bitAuxr = bitAuxr;
			this.valAuxr = valAuxr;
			this.pwmAltModule = pwmAltModule;
			this.pwmAltChannel = pwmAltChannel;
		}

		public void setUsingAlt(boolean usingAlt) {
			this.usingAlt = usingAlt;
		}

		@Override
		public int getActiveModule() {
			if (usingAlt) {
				return pwmAltModule;
			}
			return pwmModule;
		}

		@Override
		public int getActiveChannel() {
			if (usingAlt) {
				return pwmAltChannel;
			}
			return pwmChannel;
		}
	}

	private static interface AdcPinInterface {
		int adcChannel();
	}

	private static class AdcPin extends Pin implements AdcPinInterface {
		private final int adcChannel;

		public AdcPin(int encChannel, int port, int pin, int adcChannel) {
			super(encChannel, port, pin, EnumSet.of(PinMode.ANALOG_INPUT));

			this.adcChannel = adcChannel;
		}

		@Override
		public int adcChannel() {
			return adcChannel;
		}
	}

	private static class AdcOrPwmPin extends PwmPin implements AdcPinInterface {
		private final int adcChannel;

		public AdcOrPwmPin(int encChannel, int port, int pin, int regIoPwm, int pioconBit, int pwmModule,
				int pwmChannel, int adcChannel) {
			super(encChannel, port, pin, regIoPwm, pioconBit, pwmModule, pwmChannel);

			type.add(PinMode.ANALOG_INPUT);

			this.adcChannel = adcChannel;
		}

		@Override
		public int adcChannel() {
			return adcChannel;
		}
	}

	private static interface IOConstants {
		int HIGH = 1;
		int LOW = 0;

		int CLOCK_FREQ = 24000000;
		int MAX_PERIOD = (1 << 16) - 1;
		int MAX_DIVIDER = (1 << 7);
	}

	private static interface IORegs {
		int REG_CHIP_ID_L = 0xfa;
		int REG_CHIP_ID_H = 0xfb;
		int REG_VERSION = 0xfc;

		// Rotary encoder
		int REG_ENC_EN = 0x04;
		int BIT_ENC_EN_1 = 0;
		int BIT_ENC_MICROSTEP_1 = 1;
		int BIT_ENC_EN_2 = 2;
		int BIT_ENC_MICROSTEP_2 = 3;
		int BIT_ENC_EN_3 = 4;
		int BIT_ENC_MICROSTEP_3 = 5;
		int BIT_ENC_EN_4 = 6;
		int BIT_ENC_MICROSTEP_4 = 7;

		int REG_ENC_1_CFG = 0x05;
		int REG_ENC_1_COUNT = 0x06;
		int REG_ENC_2_CFG = 0x07;
		int REG_ENC_2_COUNT = 0x08;
		int REG_ENC_3_CFG = 0x09;
		int REG_ENC_3_COUNT = 0x0A;
		int REG_ENC_4_CFG = 0x0B;
		int REG_ENC_4_COUNT = 0x0C;

		// Cap touch
		int REG_CAPTOUCH_EN = 0x0D;
		int REG_CAPTOUCH_CFG = 0x0E;
		int REG_CAPTOUCH_0 = 0x0F; // First of 8 bytes from 15-22

		// Switch counters
		int REG_SWITCH_EN_P0 = 0x17;
		int REG_SWITCH_EN_P1 = 0x18;
		int REG_SWITCH_P00 = 0x19; // First of 8 bytes from 25-40
		int REG_SWITCH_P10 = 0x21; // First of 8 bytes from 33-49

		int REG_USER_FLASH = 0xD0;
		int REG_FLASH_PAGE = 0xF0;
		int REG_DEBUG = 0xF8;

		int REG_P0 = 0x40; // protect_bits 2 # Bit addressing
		int REG_SP = 0x41; // Read only
		int REG_DPL = 0x42; // Read only
		int REG_DPH = 0x43; // Read only
		int REG_RCTRIM0 = 0x44; // Read only
		int REG_RCTRIM1 = 0x45; // Read only
		int REG_RWK = 0x46;
		int REG_PCON = 0x47; // Read only
		int REG_TCON = 0x48;
		int REG_TMOD = 0x49;
		int REG_TL0 = 0x4a;
		int REG_TL1 = 0x4b;
		int REG_TH0 = 0x4c;
		int REG_TH1 = 0x4d;
		int REG_CKCON = 0x4e;
		int REG_WKCON = 0x4f; // Read only
		int REG_P1 = 0x50; // protect_bits 3 6 # Bit addressing
		int REG_SFRS = 0x51; // TA protected # Read only
		int REG_CAPCON0 = 0x52;
		int REG_CAPCON1 = 0x53;
		int REG_CAPCON2 = 0x54;
		int REG_CKDIV = 0x55;
		int REG_CKSWT = 0x56; // TA protected # Read only
		int REG_CKEN = 0x57; // TA protected # Read only
		int REG_SCON = 0x58;
		int REG_SBUF = 0x59;
		int REG_SBUF_1 = 0x5a;
		int REG_EIE = 0x5b; // Read only
		int REG_EIE1 = 0x5c; // Read only
		int REG_CHPCON = 0x5f; // TA protected # Read only
		int REG_P2 = 0x60; // Bit addressing
		int REG_AUXR1 = 0x62;
		int REG_BODCON0 = 0x63; // TA protected
		int REG_IAPTRG = 0x64; // TA protected # Read only
		int REG_IAPUEN = 0x65; // TA protected # Read only
		int REG_IAPAL = 0x66; // Read only
		int REG_IAPAH = 0x67; // Read only
		int REG_IE = 0x68; // Read only
		int REG_SADDR = 0x69;
		int REG_WDCON = 0x6a; // TA protected
		int REG_BODCON1 = 0x6b; // TA protected
		int REG_P3M1 = 0x6c;
		int REG_P3S = 0xc0; // Page 1 # Reassigned from 0x6c to avoid collision
		int REG_P3M2 = 0x6d;
		int REG_P3SR = 0xc1; // Page 1 # Reassigned from 0x6d to avoid collision
		int REG_IAPFD = 0x6e; // Read only
		int REG_IAPCN = 0x6f; // Read only
		int REG_P3 = 0x70; // Bit addressing
		int REG_P0M1 = 0x71; // protect_bits 2
		int REG_P0S = 0xc2; // Page 1 # Reassigned from 0x71 to avoid collision
		int REG_P0M2 = 0x72; // protect_bits 2
		int REG_P0SR = 0xc3; // Page 1 # Reassigned from 0x72 to avoid collision
		int REG_P1M1 = 0x73; // protect_bits 3 6
		int REG_P1S = 0xc4; // Page 1 # Reassigned from 0x73 to avoid collision
		int REG_P1M2 = 0x74; // protect_bits 3 6
		int REG_P1SR = 0xc5; // Page 1 # Reassigned from 0x74 to avoid collision
		int REG_P2S = 0x75;
		int REG_IPH = 0x77; // Read only
		int REG_PWMINTC = 0xc6; // Page 1 # Read only # Reassigned from 0x77 to avoid collision
		int REG_IP = 0x78; // Read only
		int REG_SADEN = 0x79;
		int REG_SADEN_1 = 0x7a;
		int REG_SADDR_1 = 0x7b;
		int REG_I2DAT = 0x7c; // Read only
		int REG_I2STAT = 0x7d; // Read only
		int REG_I2CLK = 0x7e; // Read only
		int REG_I2TOC = 0x7f; // Read only
		int REG_I2CON = 0x80; // Read only
		int REG_I2ADDR = 0x81; // Read only
		int REG_ADCRL = 0x82;
		int REG_ADCRH = 0x83;
		int REG_T3CON = 0x84;
		int REG_PWM4H = 0xc7; // Page 1 # Reassigned from 0x84 to avoid collision
		int REG_RL3 = 0x85;
		int REG_PWM5H = 0xc8; // Page 1 # Reassigned from 0x85 to avoid collision
		int REG_RH3 = 0x86;
		int REG_PIOCON1 = 0xc9; // Page 1 # Reassigned from 0x86 to avoid collision
		int REG_TA = 0x87; // Read only
		int REG_T2CON = 0x88;
		int REG_T2MOD = 0x89;
		int REG_RCMP2L = 0x8a;
		int REG_RCMP2H = 0x8b;
		int REG_TL2 = 0x8c;
		int REG_PWM4L = 0xca; // Page 1 # Reassigned from 0x8c to avoid collision
		int REG_TH2 = 0x8d;
		int REG_PWM5L = 0xcb; // Page 1 # Reassigned from 0x8d to avoid collision
		int REG_ADCMPL = 0x8e;
		int REG_ADCMPH = 0x8f;
		int REG_PSW = 0x90; // Read only
		int REG_PWMPH = 0x91;
		int REG_PWM0H = 0x92;
		int REG_PWM1H = 0x93;
		int REG_PWM2H = 0x94;
		int REG_PWM3H = 0x95;
		int REG_PNP = 0x96;
		int REG_FBD = 0x97;
		int REG_PWMCON0 = 0x98;
		int REG_PWMPL = 0x99;
		int REG_PWM0L = 0x9a;
		int REG_PWM1L = 0x9b;
		int REG_PWM2L = 0x9c;
		int REG_PWM3L = 0x9d;
		int REG_PIOCON0 = 0x9e;
		int REG_PWMCON1 = 0x9f;
		int REG_ACC = 0xa0; // Read only
		int REG_ADCCON1 = 0xa1;
		int REG_ADCCON2 = 0xa2;
		int REG_ADCDLY = 0xa3;
		int REG_C0L = 0xa4;
		int REG_C0H = 0xa5;
		int REG_C1L = 0xa6;
		int REG_C1H = 0xa7;
		int REG_ADCCON0 = 0xa8;
		int REG_PICON = 0xa9; // Read only
		int REG_PINEN = 0xaa; // Read only
		int REG_PIPEN = 0xab; // Read only
		int REG_PIF = 0xac; // Read only
		int REG_C2L = 0xad;
		int REG_C2H = 0xae;
		int REG_EIP = 0xaf; // Read only
		int REG_B = 0xb0; // Read only
		int REG_CAPCON3 = 0xb1;
		int REG_CAPCON4 = 0xb2;
		int REG_SPCR = 0xb3;
		int REG_SPCR2 = 0xcc; // Page 1 # Reassigned from 0xb3 to avoid collision
		int REG_SPSR = 0xb4;
		int REG_SPDR = 0xb5;
		int REG_AINDIDS0 = 0xb6;
		int REG_AINDIDS1 = -1;// Added to have common code with SuperIO
		int REG_EIPH = 0xb7; // Read only
		int REG_SCON_1 = 0xb8;
		int REG_PDTEN = 0xb9; // TA protected
		int REG_PDTCNT = 0xba; // TA protected
		int REG_PMEN = 0xbb;
		int REG_PMD = 0xbc;
		int REG_EIP1 = 0xbe; // Read only
		int REG_EIPH1 = 0xbf; // Read only

		int REG_INT = 0xf9;
		int MASK_INT_TRIG = 0x1;
		int MASK_INT_OUT = 0x2;
		int BIT_INT_TRIGD = 0;
		int BIT_INT_OUT_EN = 1;
		int BIT_INT_PIN_SWAP = 2; // 0 = P1.3, 1 = P0.0

		int REG_INT_MASK_P0 = 0x00;
		int REG_INT_MASK_P1 = 0x01;
		int REG_INT_MASK_P3 = 0x03;

		int REG_ADDR = 0xfd;

		int REG_CTRL = 0xfe; // 0 = Sleep, 1 = Reset, 2 = Read Flash, 3 = Write Flash, 4 = Addr Unlock
		int MASK_CTRL_SLEEP = 0x1;
		int MASK_CTRL_RESET = 0x2;
		int MASK_CTRL_FREAD = 0x4;
		int MASK_CTRL_FWRITE = 0x8;
		int MASK_CTRL_ADDRWR = 0x10;

		// Special mode registers, use a bit-addressing scheme to avoid
		// writing the *whole* port and smashing the i2c pins
		Set<Integer> BIT_ADDRESSED_REGS = Set.of(Integer.valueOf(REG_P0), Integer.valueOf(REG_P1),
				Integer.valueOf(REG_P2), Integer.valueOf(REG_P3));
	}

	/*-
	private static class PinRegs {
		private final int m1;
		private final int m2;
		private final int p;
		private final int ps;
		private final int intMaskP;
	
		public PinRegs(int m1, int m2, int p, int ps, int intMaskP) {
			// The PxM1 and PxM2 registers encode GPIO MODE
			// 0 0 = Quasi-bidirectional
			// 0 1 = Push-pull
			// 1 0 = Input-only (high-impedance)
			// 1 1 = Open-drain
			this.m1 = m1;
			this.m2 = m2;
			// The Px input register
			this.p = p;
			// The PxS Schmitt trigger register
			this.ps = ps;
			this.intMaskP = intMaskP;
		}
	}
	
	private static class PwmRegs {
		private final int piocon;
		private final int pwmcon0;
		private final int pwmcon1;
		private final int pwmL;
		private final int pwmH;
	
		public PwmRegs(int piocon, int pwmcon0, int pwmcon1, int pwmL, int pwmH) {
			this.piocon = piocon;
			this.pwmcon0 = pwmcon0;
			this.pwmcon1 = pwmcon1;
			this.pwmL = pwmL;
			this.pwmH = pwmH;
		}
	}
	*/

	/**
	 * Nuvoton MS51XB9AE microcontroller programmed as I/O expander
	 */
	private static class PimoroniIOExpander implements Closeable, IOConstants, IORegs {
		private final I2CDeviceInterface device;
		private final Map<Integer, Pin> pins = new HashMap<>();

		private final int[] regsM1 = { REG_P0M1, REG_P1M1, -1, REG_P3M1 };
		private final int[] regsM2 = { REG_P0M2, REG_P1M2, -1, REG_P3M2 };
		private final int[] regsP = { REG_P0, REG_P1, REG_P2, REG_P3 };
		private final int[] regsPs = { REG_P0S, REG_P1S, REG_P2S, REG_P3S };
		private final int[] regsIntMaskP = { REG_INT_MASK_P0, REG_INT_MASK_P1, -1, REG_INT_MASK_P3 };
		private final int[] regsPiocon = { REG_PIOCON0, REG_PIOCON1 };
		private final int[] regsAuxr = { -1, REG_AUXR1 };
		private final int[] regsPwmcon0 = { REG_PWMCON0 };
		private final int[] regsPwmcon1 = { REG_PWMCON1 };
		private final int[] regsPwmpl = { REG_PWMPL };
		private final int[] regsPwmph = { REG_PWMPH };
		private final int[][] regsPwml = { { REG_PWM0L, REG_PWM1L, REG_PWM2L, REG_PWM3L, REG_PWM4L, REG_PWM5L } };
		private final int[][] regsPwmh = { { REG_PWM0H, REG_PWM1H, REG_PWM2H, REG_PWM3H, REG_PWM4H, REG_PWM5H } };

		private long interruptTimeout = TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS); // 1,000ms = 1 second
		private long timeout = interruptTimeout;
		private final int[] encoderOffset = { 0, 0, 0, 0 };
		private final int[] encoderLast = { 0, 0, 0, 0 };
		private float vRef = 3.3f;
		private boolean adcEnabled;
		private int pwmPeriod;

		public PimoroniIOExpander(int controller, int address, boolean performReset) {
			this.device = I2CDevice.builder(address).setController(controller).setByteOrder(ByteOrder.LITTLE_ENDIAN)
					.build();

			// Reset the chip if requested, to put it into a known state
			if (performReset) {
				reset();
			}

			/*-
			pins = [                                                                                              # Pin |  ADC   |  PWM   |  ENC  |
			PWM_PIN(       enc_channel=1, port=1, pin=5, pwm_piocon=(1, 5), pwm_define=(0, 5)),                   # 1   |        | [CH 5] | CH 1  |
			PWM_PIN(       enc_channel=2, port=1, pin=0, pwm_piocon=(0, 2), pwm_define=(0, 2)),                   # 2   |        | [CH 2] | CH 2  |
			PWM_PIN(       enc_channel=3, port=1, pin=2, pwm_piocon=(0, 0), pwm_define=(0, 0)),                   # 3   |        | [CH 0] | CH 3  |
			PWM_PIN(       enc_channel=4, port=1, pin=4, pwm_piocon=(1, 1), pwm_define=(0, 1)),                   # 4   |        | [CH 1] | CH 4  |
			PWM_PIN(       enc_channel=5, port=0, pin=0, pwm_piocon=(0, 3), pwm_define=(0, 3)),                   # 5   |        | [CH 3] | CH 5  |
			PWM_PIN(       enc_channel=6, port=0, pin=1, pwm_piocon=(0, 4), pwm_define=(0, 4)),                   # 6   |        | [CH 4] | CH 6  |
			ADC_OR_PWM_PIN(enc_channel=7, port=1, pin=1, pwm_piocon=(0, 1), pwm_define=(0, 1), adc_channel=7),    # 7   | [CH 7] |  CH 1  | CH 7  |
			ADC_OR_PWM_PIN(enc_channel=8, port=0, pin=3, pwm_piocon=(0, 5), pwm_define=(0, 5), adc_channel=6),    # 8   | [CH 6] |  CH 5  | CH 8  |
			ADC_OR_PWM_PIN(enc_channel=9, port=0, pin=4, pwm_piocon=(1, 3), pwm_define=(0, 3), adc_channel=5),    # 9   | [CH 5] |  CH 3  | CH 9  |
			ADC_PIN(       enc_channel=10, port=3, pin=0, adc_channel=1),                                         # 10  | [CH 1] |        | CH 10 |
			ADC_PIN(       enc_channel=11, port=0, pin=6, adc_channel=3),                                         # 11  | [CH 3] |        | CH 11 |
			ADC_OR_PWM_PIN(enc_channel=12, port=0, pin=5, pwm_piocon=(1, 2), pwm_define=(0, 2), adc_channel=4),   # 12  | [CH 4] |  CH 2  | CH 12 |
			ADC_PIN(       enc_channel=13, port=0, pin=7, adc_channel=2),                                         # 13  | [CH 2] |        | CH 13 |
			ADC_PIN(       enc_channel=14, port=1, pin=7, adc_channel=0),                                         # 14  | [CH 0] |        | CH 14 |
			]                                                                                                     # [] = labelled pin functions
			new PwmRegs(regsPiocon[pin.regIoPwm], regsPwmcon0[pin.pwmModule], regsPwmcon1[pin.pwmModule],
					regsPwml[pin.pwmModule][pin.pwmChannel], regsPwmh[pin.pwmModule][pin.pwmChannel])
			*/
			addPin(new PwmPin(1, 1, 5, 1, 5, 0, 5));
			addPin(new PwmPin(2, 1, 0, 0, 2, 0, 2));
			addPin(new PwmPin(3, 1, 2, 0, 0, 0, 0));
			addPin(new PwmPin(4, 1, 4, 1, 1, 0, 1));
			addPin(new PwmPin(5, 0, 0, 0, 3, 0, 3));
			addPin(new PwmPin(6, 0, 1, 0, 4, 0, 4));
			addPin(new AdcOrPwmPin(7, 1, 1, 0, 1, 0, 1, 7));
			addPin(new AdcOrPwmPin(8, 0, 3, 0, 5, 0, 5, 6));
			addPin(new AdcOrPwmPin(9, 0, 4, 1, 3, 0, 3, 5));
			addPin(new AdcPin(10, 3, 0, 1));
			addPin(new AdcPin(11, 0, 6, 3));
			addPin(new AdcOrPwmPin(12, 0, 5, 1, 2, 0, 2, 4));
			addPin(new AdcPin(13, 0, 7, 2));
			addPin(new AdcPin(14, 1, 7, 0));
		}

		private void addPin(Pin pin) {
			pins.put(Integer.valueOf(pin.encChannel), pin);
		}

		@Override
		public void close() {
			device.close();
		}

		public int getChipId() {
			return device.readUShort(REG_CHIP_ID_L);
			// return read16(REG_CHIP_ID_L, REG_CHIP_ID_H);
		}

		public int getChipVersion() {
			// Get the IOE version
			return device.readUByte(REG_VERSION);
			// return (byte) read8(REG_VERSION);
		}

		public short checkReset() {
			return device.readUByte(REG_USER_FLASH);
			// return (byte) read8(REG_USER_FLASH);
		}

		public void reset() {
			long t_start = System.currentTimeMillis();
			setBits(REG_CTRL, MASK_CTRL_RESET);
			// Wait for a register to read its initialised value
			while (checkReset() != 0x78) {
				SleepUtil.sleepMillis(1);
				if (System.currentTimeMillis() - t_start >= timeout) {
					throw new RuntimeException("Timed out waiting for Reset!");
				}
			}
		}

		/*-
		private int read8(int register) {
			int value = device.readByteData(register) & 0xff;
			if (Logger.isTraceEnabled()) {
				System.out.format("i2c_read8(0x%02x) : 0x%02x%n", register, value);
			}
			return value;
		}
		*/

		/*-
		private void write8(int register, int value) {
			value = value & 0xff;
			if (Logger.isTraceEnabled()) {
				System.out.format("i2c_write8(0x%02x, 0x%02x)%n", register, value);
			}
			device.writeByteData(register, (byte) value);
		}
		*/

		private int read12(int regL, int regH) {
			// Read two (4+8bit) registers from the device, as a single read if they are consecutive
			if (regH == regL + 1) {
				final I2CMessage[] messages = { new I2CMessage(I2CMessage.I2C_M_WR, 1),
						new I2CMessage(I2CMessage.I2C_M_RD, 2) };
				final byte[] buffer = new byte[3];
				buffer[0] = (byte) regL;
				device.readWrite(messages, buffer);
				// msg_w = i2c_msg.write(self._i2c_addr, [reg_l])
				// msg_r = i2c_msg.read(self._i2c_addr, 2)
				// self._i2c_dev.i2c_rdwr(msg_w, msg_r)
				// return list(msg_r)[0] | (list(msg_r)[1] << 4)
				int value = (buffer[1] & 0xff) | ((buffer[2] & 0xff) << 4);
				if (Logger.isTraceEnabled()) {
					System.out.format("i2c_read12(0x%02x, 0x%02x) : 0x%04x%n", regL, regH, value);
				}
				return value;
			}

			final I2CMessage[] messages = { new I2CMessage(I2CMessage.I2C_M_WR, 1),
					new I2CMessage(I2CMessage.I2C_M_RD, 1), new I2CMessage(I2CMessage.I2C_M_WR, 1),
					new I2CMessage(I2CMessage.I2C_M_RD, 1) };
			final byte[] buffer = new byte[4];
			buffer[0] = (byte) regL;
			buffer[2] = (byte) regH;
			int value = (buffer[1] & 0xff) | ((buffer[3] & 0xff) << 4);
			if (Logger.isTraceEnabled()) {
				System.out.format("i2c_read12(0x%02x, 0x%02x) : 0x%04x%n", regL, regH, value);
			}
			return value;
		}

		private int read16(int regL, int regH) {
			// Read two (8+8bit) registers from the device, as a single read if they are consecutive.
			if (regH == regL + 1) {
				/*-
				msg_w = i2c_msg.write(self._i2c_addr, [reg_l])
				msg_r = i2c_msg.read(self._i2c_addr, 2)
				self._i2c_dev.i2c_rdwr(msg_w, msg_r)
				return list(msg_r)[0] | (list(msg_r)[1] << 8)
				*/
				int value = device.readUShort(regL);
				if (Logger.isTraceEnabled()) {
					System.out.format("i2c_read16(0x%02x, 0x%02x) : 0x%04x%n", regL, regH, value);
				}
				return value;
			}

			final I2CMessage[] messages = { new I2CMessage(I2CMessage.I2C_M_WR, 1),
					new I2CMessage(I2CMessage.I2C_M_RD, 1), new I2CMessage(I2CMessage.I2C_M_WR, 1),
					new I2CMessage(I2CMessage.I2C_M_RD, 1) };
			final byte[] buffer = new byte[4];
			buffer[0] = (byte) regL;
			buffer[2] = (byte) regH;
			device.readWrite(messages, buffer);
			int value = (buffer[1] & 0xff) | ((buffer[3] & 0xff) << 8);
			if (Logger.isTraceEnabled()) {
				System.out.format("i2c_read16(0x%02x, 0x%02x) : 0x%04x%n", regL, regH, value);
			}
			return value;
		}

		private void write16(int regL, int regH, int value) {
			if (Logger.isTraceEnabled()) {
				System.out.format("i2c_write16(0x%02x, 0x%02x, 0x%04x)%n", regL, regH, (short) value);
			}
			if (regH == regL + 1) {
				device.writeWordData(regL, (short) value);
			} else {
				byte val_l = (byte) (value & 0xff);
				byte val_h = (byte) ((value >> 8) & 0xff);
				final I2CMessage[] messages = { new I2CMessage(I2CMessage.I2C_M_WR, 2),
						new I2CMessage(I2CMessage.I2C_M_WR, 2) };

				device.readWrite(messages, new byte[] { (byte) regL, val_l, (byte) regH, val_h });
			}
		}

		public Pin getPin(int pin) {
			if (pin < 1 || pin > pins.size()) {
				throw new IllegalArgumentException(
						String.format("Pin should be in range 1-%d.", Integer.valueOf(pins.size())));
			}

			return pins.get(Integer.valueOf(pin));
		}

		private boolean getBit(int reg, int bit) {
			// Returns the specified bit (nth position from right) from a register.
			return (device.readByteData(reg) & (1 << bit)) != 0;
			// return (read8(reg) & (1 << bit)) != 0;
		}

		private void setBit(int reg, int bit) {
			// Set the specified bit (nth position from right) in a register.
			setBits(reg, (1 << bit));
		}

		private void setBits(int reg, int bits) {
			// Set the specified bits (using a mask) in a register.
			if (BIT_ADDRESSED_REGS.contains(Integer.valueOf(reg))) {
				for (int bit = 0; bit < 8; bit++) {
					if ((bits & (1 << bit)) != 0) {
						device.writeByteData(reg, 0b1000 | (bit & 0b111));
						// write8(reg, 0b1000 | (bit & 0b111));
					}
				}
			} else {
				byte value = device.readByteData(reg);
				// byte value = (byte) read8(reg);
				// time.sleep(0.001)
				device.writeByteData(reg, value | bits);
				// write8(reg, value | bits);
			}
		}

		private void clearBit(int reg, int bit) {
			// Clear the specified bit (nth position from right) in a register.
			clearBits(reg, (1 << bit));
		}

		private void clearBits(int reg, int bits) {
			// Clear the specified bits (using a mask) in a register.
			if (BIT_ADDRESSED_REGS.contains(Integer.valueOf(reg))) {
				for (int bit = 0; bit < 8; bit++) {
					if ((bits & (1 << bit)) != 0) {
						device.writeByteData(reg, 0b0000 | (bit & 0b111));
						// write8(reg, 0b0000 | (bit & 0b111));
					}
				}
			} else {
				byte value = device.readByteData(reg);
				// byte value = (byte) read8(reg);
				// time.sleep(0.001)
				device.writeByteData(reg, value & ~bits);
				// write8(reg, value & ~bits);
			}
		}

		private void changeBit(int reg, int bit, boolean state) {
			// Toggle one register bit on/off.
			if (state) {
				setBit(reg, bit);
			} else {
				clearBit(reg, bit);
			}
		}

		public float getAdcVref() {
			return vRef;
		}

		public void setAdcVref(float adcVref) {
			this.vRef = vRef;
		}

		public void enableAdc() {
			// Enable the analog to digital converter.
			setBit(REG_ADCCON1, 0);
			adcEnabled = true;
		}

		public void disableAdc() {
			// Disable the analog to digital converter.
			clearBit(REG_ADCCON1, 0);
			if (REG_AINDIDS1 != -1) {
				write16(REG_AINDIDS0, REG_AINDIDS1, 0);
			} else {
				device.writeByteData(REG_AINDIDS0, 0);
				// write8(REG_AINDIDS0, 0);
			}
			adcEnabled = false;
		}

		public int getPwmModule(int pin) {
			if (pin < 1 || pin > pins.size()) {
				throw new IllegalArgumentException("Pin should be in range 1-" + pins.size());
			}

			final Pin io_pin = pins.get(Integer.valueOf(pin));
			if (!io_pin.type.contains(PinMode.PWM_OUTPUT)) {
				int io_mode = (PinMode.PWM_OUTPUT.value() >> 2) & 0b11;
				throw new IllegalArgumentException(
						String.format("Pin %d does not support %s!", Integer.valueOf(pin), PinMode.PWM_OUTPUT));
			}

			final PwmPin pwm_pin = (PwmPin) io_pin;

			return pwm_pin.getActiveModule();
		}

		private void pwmLoad(int pwmModule) {
			pwmLoad(pwmModule, true);
		}

		private void pwmLoad(int pwmModule, boolean waitForLoad) {
			// Load new period and duty registers into buffer
			long t_start = System.currentTimeMillis();
			setBit(regsPwmcon0[pwmModule], 6); // Set the "LOAD" bit of PWMCON0
			if (waitForLoad) {
				while (pwmLoading(pwmModule)) {
					SleepUtil.sleepMillis(1); // Wait for "LOAD" to complete
					if (System.currentTimeMillis() - t_start >= timeout) {
						throw new RuntimeException("Timed out waiting for PWM load!");
					}
				}
			}
		}

		private boolean pwmLoading(int pwmModule) {
			return getBit(regsPwmcon0[pwmModule], 6);
		}

		public void pwmClear(int pwmModule) {
			pwmClear(pwmModule, true);
		}

		public void pwmClear(int pwm_module, boolean waitForClear) {
			// Clear the PWM counter
			long t_start = System.currentTimeMillis();
			setBit(regsPwmcon0[pwm_module], 4); // Set the "CLRPWM" bit of PWMCON0
			if (waitForClear) {
				while (pwmClearing(pwm_module)) {
					SleepUtil.sleepMillis(1); // Wait for "LOAD" to complete
					if (System.currentTimeMillis() - t_start >= timeout) {
						throw new RuntimeException("Timed out waiting for PWM clear!");
					}
				}
			}
		}

		public boolean pwmClearing(int pwmModule) {
			return getBit(regsPwmcon0[pwmModule], 4);
		}

		public void setPwmControl(ClockDivider divider) {
			setPwmControl(divider, 0);
		}

		/**
		 * Set PWM settings.
		 * 
		 * PWM is driven by the 24MHz FSYS clock by default.
		 * 
		 * @param divider   Clock divider, one of 1, 2, 4, 8, 16, 32, 64 or 128
		 * @param pwmModule The PWM module
		 */
		public void setPwmControl(ClockDivider divider, int pwmModule) {
			// TODO: This currently sets GP, PWMTYP and FBINEN to 0
			// It might be desirable to make these available to the user
			// GP - Group mode enable (changes first three pairs of pAM to PWM01H and PWM01L)
			// PWMTYP - PWM type select: 0 edge-aligned, 1 center-aligned
			// FBINEN - Fault-break input enable

			device.writeByteData(regsPwmcon1[pwmModule], divider.pwmdiv2);
			// write8(regsPwmcon1[pwmModule], divider.pwmdiv2);
		}

		public int getPwmPeriod() {
			return getPwmPeriod(0);
		}

		public int getPwmPeriod(int pwmModule) {
			int pwmpl = regsPwmpl[pwmModule];
			int pwmph = regsPwmph[pwmModule];
			return read16(pwmpl, pwmph);
		}

		/**
		 * Set the LED PWM period.
		 * 
		 * The period is the point at which the PWM counter is reset to zero.
		 * 
		 * The PWM clock runs at FSYS with a divider of 1/1.
		 * 
		 * Also specifies the maximum value that can be set in the PWM duty cycle.
		 * 
		 * @param pwmPeriod PWM period from 255 to 65535
		 */
		public void setPwmPeriod(int value) {
			setPwmPeriod(value, 0, true, true);
		}

		public void setPwmPeriod(int value, int pwmModule, boolean load, boolean waitForLoad) {
			this.pwmPeriod = value;

			int pwmpl = regsPwmpl[pwmModule];
			int pwmph = regsPwmph[pwmModule];
			write16(pwmpl, pwmph, value);

			// Commented out, as it gets set when the pin is configured
			// pwmcon0 = self._regs_pwmcon0[pwm_module]
			// self.set_bit(pwmcon0, 7) # Set PWMRUN bit

			if (load) {
				pwmLoad(pwmModule, waitForLoad);
			}
		}

		public void setPwmFrequency(int frequency) {
			setPwmFrequency(frequency, 0, true, true);
		}

		public int setPwmFrequency(int frequency, int pwmModule, boolean load, boolean waitForLoad) {
			int period = CLOCK_FREQ / frequency;
			if (period / 128 > MAX_PERIOD) {
				throw new IllegalArgumentException("The provided frequency is too low");
			}
			if (period < 2) {
				throw new IllegalArgumentException("The provided frequency is too high");
			}

			int divider = 1;

			while ((period > MAX_PERIOD) && (divider < MAX_DIVIDER)) {
				period = period >> 1;
				divider = divider << 1;
			}

			period = Math.min(period, MAX_PERIOD); // Should be unnecessary because of earlier raised errors, but kept
													// in case
			setPwmControl(ClockDivider.of(divider), pwmModule);
			setPwmPeriod(period - 1, pwmModule, load, waitForLoad);

			return period;
		}

		public Optional<PinMode> getMode(int pin) {
			// Get the current mode of a pin.
			return getPin(pin).mode;
		}

		public void setMode(int pin, PinMode mode) {
			setMode(pin, mode, false, false);
		}

		/**
		 * Set a pin output mode.
		 * 
		 * @param mode one of the supplied IN, OUT, PWM or ADC constants
		 */
		public void setMode(int pin, PinMode mode, boolean schmittTrigger, boolean invert) {
			final Pin io_pin = getPin(pin);
			if (io_pin.mode.isPresent() && io_pin.mode.get() == mode) {
				return;
			}

			byte mode_val = mode.value();
			int gpio_mode = mode_val & 0b11;
			int io_mode = (mode_val >> 2) & 0b11;
			int initial_state = mode_val >> 4;

			if (io_mode != PinMode.DIGITAL_INPUT_OUTPUT.value() && !io_pin.type.contains(mode)) {
				throw new IllegalArgumentException(String.format("Pin %d does not support %s!", pin, mode));
			}

			io_pin.mode = Optional.of(mode);

			switch (mode) {
			case PWM_OUTPUT:
				PwmPin pwm_pin = (PwmPin) io_pin;
				if (pwm_pin.isUsingAlt()) {
					// pwm_regs = getAltPwmRegs((DualPwmPin) pwm_pin);
					/*-
					new PwmRegs(regsPiocon[pin.regIoPwm], regsPwmcon0[pin.pwmAltModule], regsPwmcon1[pin.pwmAltModule],
						regsPwml[pin.pwmAltModule][pin.pwmAltChannel], regsPwmh[pin.pwmAltModule][pin.pwmAltChannel]);
					 */
					// pwmcon0_reg = regsPwmcon0[((DualPwmPin) pwm_pin).pwmAltModule];
				} else {
					// pwm_regs = getPwmRegs(pwm_pin);
					/*-
					new PwmRegs(regsPiocon[pin.regIoPwm], regsPwmcon0[pin.pwmModule], regsPwmcon1[pin.pwmModule],
						regsPwml[pin.pwmModule][pin.pwmChannel], regsPwmh[pin.pwmModule][pin.pwmChannel]);
					*/
					// pwmcon0_reg = regsPwmcon0[pwm_pin.pwmModule];
				}
				setBit(regsPiocon[pwm_pin.regIoPwm], pwm_pin.bitIoPwm);
				// if (pwm_pin.getActiveModule() == 0) { // Only module 0's outputs can be inverted
				if (pwm_pin.pwmModule == 0) { // Only module 0's outputs can be inverted
					changeBit(REG_PNP, pwm_pin.bitIoPwm, invert);
				}
				setBit(regsPwmcon0[pwm_pin.getActiveModule()], 7); // Set PWMRUN bit
				break;
			case ANALOG_INPUT:
				enableAdc();
				break;
			default:
				if (io_pin.type.contains(PinMode.PWM_OUTPUT)) {
					pwm_pin = (PwmPin) io_pin;
					clearBit(regsPiocon[pwm_pin.regIoPwm], pwm_pin.bitIoPwm);
				}
			}

			// final PinRegs pin_regs = getPinRegs(io_pin);

			byte pm1 = device.readByteData(regsM1[io_pin.port]);
			// byte pm1 = (byte) read8(pin_regs.m1);
			byte pm2 = device.readByteData(regsM2[io_pin.port]);
			// byte pm2 = (byte) read8(pin_regs.m2);

			// Clear the pm1 and pm2 bits
			pm1 &= 255 - (1 << io_pin.pin);
			pm2 &= 255 - (1 << io_pin.pin);

			// Set the new pm1 and pm2 bits according to our gpio_mode
			pm1 |= (gpio_mode >> 1) << io_pin.pin;
			pm2 |= (gpio_mode & 0b1) << io_pin.pin;

			device.writeByteData(regsM1[io_pin.port], pm1);
			// write8(pin_regs.m1, pm1);
			device.writeByteData(regsM2[io_pin.port], pm2);
			// write8(pin_regs.m2, pm2);

			// Set up Schmitt trigger mode on inputs
			if (mode == PinMode.DIGITAL_INPUT_PULLUP || mode == PinMode.DIGITAL_INPUT) {
				changeBit(regsPs[io_pin.port], io_pin.pin, schmittTrigger);
			}

			// If pin is a basic output, invert its initial state
			if (mode == PinMode.DIGITAL_OUTPUT_PUSH_PULL && invert) {
				initial_state = ~initial_state;
				io_pin.setInverted(true);
			} else {
				io_pin.setInverted(false);
			}

			// 5th bit of mode encodes default output pin state
			device.writeByteData(regsP[io_pin.port], (initial_state << 3) | io_pin.pin);
			// write8(pin_regs.p, (initial_state << 3) | io_pin.pin);
		}

		public double input(int pin) {
			return input(pin, TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS));
		}

		public double input(int pin, long adcTimeout) {
			/**
			 * Read the IO pin state.
			 * 
			 * Returns a 12-bit ADC reading if the pin is in ADC mode Returns True/False if the pin is
			 * in any other input mode Returns None if the pin is in PWM mode
			 * 
			 * @param adcTimeout Timeout (in seconds) for an ADC read (default 1.0)
			 * 
			 */
			final Pin io_pin = getPin(pin);

			if (io_pin.mode.isPresent() && io_pin.mode.get() == PinMode.ANALOG_INPUT) {
				final AdcPinInterface adc_pin = (AdcPinInterface) io_pin;
				if (adc_pin.adcChannel() > 8) {
					device.writeByteData(REG_AINDIDS1, 1 << (adc_pin.adcChannel() - 8));
					// write8(REG_AINDIDS1, 1 << (adc_pin.adcChannel() - 8));
				} else {
					device.writeByteData(REG_AINDIDS0, 1 << adc_pin.adcChannel());
					// write8(REG_AINDIDS0, 1 << adc_pin.adcChannel());
				}

				int con0value = device.readUByte(REG_ADCCON0);
				// int con0value = read8(REG_ADCCON0);
				con0value = con0value & ~0x0f;
				con0value = con0value | adc_pin.adcChannel();

				con0value = con0value & ~(1 << 7); // ADCF - Clear the conversion complete flag
				con0value = con0value | (1 << 6); // ADCS - Set the ADC conversion start flag
				device.writeByteData(REG_ADCCON0, con0value);
				// write8(REG_ADCCON0, con0value);

				if (adcTimeout != -1) {
					// Wait for the ADCF conversion complete flag to be set
					final long t_start = System.currentTimeMillis();
					while (!getBit(REG_ADCCON0, 7)) {
						SleepUtil.sleepMillis(1);
						if (System.currentTimeMillis() - t_start >= adcTimeout) {
							throw new RuntimeException("Timeout waiting for ADC conversion!");
						}
					}
				}

				int reading = read12(REG_ADCRL, REG_ADCRH);
				return (reading / 4095.0) * vRef;
			} else {
				boolean pv = getBit(regsP[io_pin.port], io_pin.pin);

				return pv ? HIGH : LOW;
			}
		}

		private void output(int channel, int value) {
			output(channel, value, true, true);
		}

		private void output(int channel, int value, boolean load, boolean waitForLoad) {
			final Pin pin = pins.get(Integer.valueOf(channel));
			if (pin.mode.isPresent() && pin.mode.get() == PinMode.PWM_OUTPUT) {
				final PwmPin pwm_pin = (PwmPin) pin;
				/*-
				if (pwm_pin.isUsingAlt()) {
					final DualPwmPin d_pwm_pin = (DualPwmPin) pwm_pin;
					final PwmRegs alt_regs = getAltPwmRegs(d_pwm_pin);
					write16(alt_regs.pwmL, alt_regs.pwmH, value);
					if (load) {
						pwmLoad(d_pwm_pin.pwmAltModule, waitForLoad);
					}
				} else {
					// final PwmRegs regs = getPwmRegs(pwm_pin);
					write16(regsPwml[pwm_pin.pwmModule][pwm_pin.pwmChannel],
							regsPwmh[pwm_pin.pwmModule][pwm_pin.pwmChannel], value);
					if (load) {
						pwmLoad(pwm_pin.pwmModule, waitForLoad);
					}
				}
				*/
				write16(regsPwml[pwm_pin.getActiveModule()][pwm_pin.getActiveChannel()],
						regsPwmh[pwm_pin.getActiveModule()][pwm_pin.getActiveChannel()], value);
				if (load) {
					pwmLoad(pwm_pin.getActiveModule(), waitForLoad);
				}
			} else {
				if (value == LOW) {
					changeBit(regsP[pin.port], pin.encChannel, pin.isInverted());
				} else {
					changeBit(regsP[pin.port], pin.encChannel, !pin.isInverted());
				}
			}
		}

		/*-
		public PwmRegs getPwmRegs(PwmPin pin) {
			return new PwmRegs(regsPiocon[pin.regIoPwm], regsPwmcon0[pin.pwmModule], regsPwmcon1[pin.pwmModule],
					regsPwml[pin.pwmModule][pin.pwmChannel], regsPwmh[pin.pwmModule][pin.pwmChannel]);
		}
		*/

		/*-
		public PwmRegs getAltPwmRegs(DualPwmPin pin) {
			return new PwmRegs(regsPiocon[pin.regIoPwm], regsPwmcon0[pin.pwmAltModule], regsPwmcon1[pin.pwmAltModule],
					regsPwml[pin.pwmAltModule][pin.pwmAltChannel], regsPwmh[pin.pwmAltModule][pin.pwmAltChannel]);
		}
		*/

		/*-
		public PinRegs getPinRegs(Pin pin) {
			return new PinRegs(regsM1[pin.port], regsM2[pin.port], regsP[pin.port], regsPs[pin.port],
					regsIntMaskP[pin.port]);
		}
		*/

		public void switchPwmToAlt(int pin) {
			if (pin < 1 || pin > pins.size()) {
				throw new IllegalArgumentException("Pin should be in range 1-" + pins.size());
			}

			final Pin io_pin = pins.get(Integer.valueOf(pin));

			if (!(io_pin instanceof DualPwmPin)) {
				throw new IllegalArgumentException("Pin does not have an alternate PWM.");
			}

			DualPwmPin d_pwm_pin = (DualPwmPin) io_pin;

			int auxr = device.readUByte(regsAuxr[d_pwm_pin.regAuxr]);
			// int auxr = read8(regsAuxr[d_pwm_pin.regAuxr]);
			auxr = auxr & ~(0b11 << d_pwm_pin.bitAuxr); // Clear the bits for the alt output
			auxr = auxr | (d_pwm_pin.valAuxr << d_pwm_pin.bitAuxr); // Set the bits for outputting the aux to the
																	// intended pin

			device.writeByteData(regsAuxr[d_pwm_pin.regAuxr], auxr);
			// write8(regsAuxr[d_pwm_pin.regAuxr], auxr);
			d_pwm_pin.setUsingAlt(true);
		}
	}
}
