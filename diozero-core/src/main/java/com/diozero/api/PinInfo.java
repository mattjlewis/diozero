package com.diozero.api;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     PinInfo.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2023 diozero
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

import java.util.Collection;
import java.util.EnumSet;

/**
 * <p>
 * Describe the various attributes of an individual General-Purpose Input/Output
 * (GPIO) pin. GPIO pin functions include Digital Input / Output, PWM Output and
 * Analog Input / Output.
 * </p>
 *
 * <p>
 * <strong>Always access instances of this class via the
 * {@link com.diozero.sbc.BoardPinInfo BoardPinInfo} accessor methods rather
 * than constructing instances yourself.</strong> For example,
 * {@link com.diozero.sbc.BoardPinInfo#getByGpioNumber(int)
 * BoardPinInfo.getByGpioNumber(gpio)} and
 * {@link com.diozero.sbc.BoardPinInfo#getByChipAndLineOffset(int, int)
 * BoardPinInfo.getByChipAndLineOffset(chip, lineOffset)}.
 * </p>
 *
 * <p>
 * A board has a number of headers, each header has a number of physical pins
 * connected to it. For example, the Raspberry Pi model B has the main 40-pin J8
 * header as well as the 8-pin P5 header (that doesn't have header pins soldered
 * by default). Non-GPIO pins such as Vcc and GND are also included for
 * information purposes only.
 * </p>
 *
 * <p>
 * A pin has the following attributes:
 * </p>
 * <dl>
 * <dt>keyPrefix</dt>
 * <dd>internal only attribute used by
 * {@link com.diozero.internal.spi.AbstractDeviceFactory AbstractDeviceFactory}
 * when provisioning GPIO devices</dd>
 * <dt>header</dt>
 * <dd>the name of the board header to which this pin is attached</dd>
 * <dt>physicalPin</dt>
 * <dd>the physical header pin number</dd>
 * <dt>deviceNumber</dt>
 * <dd>the logical device GPIO number</dd>
 * <dt>sysFsNumber</dt>
 * <dd>typically the same as the GPIO / device number; can be different for PWM
 * pins that are controlled via Linux sysfs</dd>
 * <dt>chip</dt>
 * <dd>the Linux GPIO chip number for this GPIO as defined by the Linux GPIO
 * character device; see /sys/gpiochip&lt;n&gt;, run <code>gpiodetect</code> to
 * list</dd>
 * <dt>lineOffset</dt>
 * <dd>the line number offset for this GPIO on the GPIO chip - Linux GPIO
 * character device; <code>run gpioinfo &lt;n&gt;</code> to list</dd>
 * <dt>name</dt>
 * <dd>the name of this pin</dd>
 * <dt>modes</dt>
 * <dd>the set of valid {@link DeviceMode modes} for this pin</dd>
 * </dl>
 */
public class PinInfo {
	public static final EnumSet<DeviceMode> DIGITAL_IN = EnumSet.of(DeviceMode.DIGITAL_INPUT);
	public static final EnumSet<DeviceMode> DIGITAL_OUT = EnumSet.of(DeviceMode.DIGITAL_OUTPUT);
	public static final EnumSet<DeviceMode> DIGITAL_IN_OUT = EnumSet.of(DeviceMode.DIGITAL_INPUT,
			DeviceMode.DIGITAL_OUTPUT);
	public static final EnumSet<DeviceMode> DIGITAL_IN_OUT_PWM = EnumSet.of(DeviceMode.DIGITAL_INPUT,
			DeviceMode.DIGITAL_OUTPUT, DeviceMode.PWM_OUTPUT);
	public static final EnumSet<DeviceMode> PWM_OUTPUT = EnumSet.of(DeviceMode.PWM_OUTPUT);
	public static final EnumSet<DeviceMode> DIGITAL_PWM_OUTPUT = EnumSet.of(DeviceMode.DIGITAL_OUTPUT,
			DeviceMode.PWM_OUTPUT);
	public static final EnumSet<DeviceMode> DIGITAL_ANALOG_INPUT = EnumSet.of(DeviceMode.DIGITAL_INPUT,
			DeviceMode.ANALOG_INPUT);
	public static final EnumSet<DeviceMode> ANALOG_INPUT = EnumSet.of(DeviceMode.ANALOG_INPUT);
	public static final EnumSet<DeviceMode> ANALOG_OUTPUT = EnumSet.of(DeviceMode.ANALOG_OUTPUT);

	public static final int NOT_DEFINED = -1;
	public static final String DEFAULT_HEADER = "DEFAULT";

	public static final String GROUND = "GND";
	public static final String VCC_5V = "5v";
	public static final String VCC_3V3 = "3v3";

	private String keyPrefix;
	private String header;
	private int physicalPin;
	/** e.g. gpioNumber */
	private int deviceNumber;
	private int sysFsNumber;
	private int chip;
	private int lineOffset;
	private String name;
	private Collection<DeviceMode> modes;
	private int pwmChip;
	private int pwmNum;

	public PinInfo(String keyPrefix, String header, int deviceNumber, int physicalPin, String name,
			Collection<DeviceMode> modes) {
		this(keyPrefix, header, deviceNumber, physicalPin, NOT_DEFINED, NOT_DEFINED, name, modes, deviceNumber,
				NOT_DEFINED, NOT_DEFINED);
	}

	public PinInfo(String keyPrefix, String header, int deviceNumber, int physicalPin, String name,
			Collection<DeviceMode> modes, int sysFsNumber, int chip, int line) {
		this(keyPrefix, header, deviceNumber, physicalPin, NOT_DEFINED, NOT_DEFINED, name, modes, sysFsNumber, chip,
				line);
	}

	public PinInfo(String keyPrefix, String header, int deviceNumber, int physicalPin, int pwmChip, int pwmNum,
			String name, Collection<DeviceMode> modes, int sysFsNumber, int chip, int line) {
		this.keyPrefix = keyPrefix;
		this.header = header;
		this.physicalPin = physicalPin;
		this.deviceNumber = deviceNumber;
		this.pwmChip = pwmChip;
		this.pwmNum = pwmNum;
		this.name = name;
		this.modes = modes;
		this.sysFsNumber = sysFsNumber;
		this.chip = chip;
		this.lineOffset = line;
	}

	/**
	 * Internal only attribute used by
	 * {@link com.diozero.internal.spi.AbstractDeviceFactory AbstractDeviceFactory}
	 * when provisioning GPIO devices
	 *
	 * @return the key prefix for this pin
	 */
	public String getKeyPrefix() {
		return keyPrefix;
	}

	/**
	 * Get the name of the board header to which this pin is attached
	 *
	 * @return the header name to which this pin is attached
	 */
	public String getHeader() {
		return header;
	}

	/**
	 * Get the physical header pin number
	 *
	 * @return the physical header pin number
	 */
	public int getPhysicalPin() {
		return physicalPin;
	}

	/**
	 * Get the logical device GPIO number
	 *
	 * @return the logical device GPIO number
	 */
	public int getDeviceNumber() {
		return deviceNumber;
	}

	/**
	 * Get sysfs number for this pin. This is typically the same as the GPIO /
	 * device number; can be different for PWM pins that are controlled via Linux
	 * sysfs
	 *
	 * @return the sysfs number for this pin
	 */
	public int getSysFsNumber() {
		return sysFsNumber;
	}

	/**
	 * the Linux GPIO chip number for this GPIO as defined by the Linux GPIO
	 * character device; see /sys/gpiochip&lt;n&gt;, run <code>gpiodetect</code> to
	 * list
	 *
	 * @return the GPIO chip number for this pin
	 */
	public int getChip() {
		return chip;
	}

	/**
	 * <strong>Internal method - do not call</strong>
	 *
	 * @param chip the GPIO chip number for this pin
	 */
	public void setChip(int chip) {
		this.chip = chip;
	}

	/**
	 * Get the line number offset for this GPIO on the GPIO chip - Linux GPIO
	 * character device; <code>run gpioinfo &lt;n&gt;</code> to list
	 *
	 * @return the GPIO line offset
	 */
	public int getLineOffset() {
		return lineOffset;
	}

	/**
	 * <strong>Internal method - do not call</strong>
	 *
	 * @param lineOffset the GPIO chip line offset number for this pin
	 */
	public void setLineOffset(int lineOffset) {
		this.lineOffset = lineOffset;
	}

	/**
	 * Get the name of this pin
	 *
	 * @return the name of this pin
	 */
	public String getName() {
		return name;
	}

	/**
	 * Get the set of valid {@link DeviceMode modes} for this pin
	 *
	 * @return the set of valid {@link DeviceMode modes} for this pin
	 */
	public Collection<DeviceMode> getModes() {
		return modes;
	}

	/**
	 * Check if the specified {@link DeviceMode mode} is supported by this pin
	 *
	 * @param mode the {@link DeviceMode device mode} to check
	 * @return true if the device mode is supported
	 */
	public boolean isSupported(DeviceMode mode) {
		return modes.contains(mode);
	}

	public boolean isDigitalInputSupported() {
		return modes.contains(DeviceMode.DIGITAL_INPUT);
	}

	public boolean isDigitalOutputSupported() {
		return modes.contains(DeviceMode.DIGITAL_OUTPUT);
	}

	public boolean isPwmOutputSupported() {
		return modes.contains(DeviceMode.PWM_OUTPUT);
	}

	public boolean isServoSupported() {
		return modes.contains(DeviceMode.SERVO);
	}

	public boolean isAnalogInputSupported() {
		return modes.contains(DeviceMode.ANALOG_INPUT);
	}

	public boolean isAnalogOutputSupported() {
		return modes.contains(DeviceMode.ANALOG_OUTPUT);
	}

	public int getPwmChip() {
		return pwmChip;
	}

	public int getPwmNum() {
		return pwmNum;
	}

	@Override
	public String toString() {
		return "PinInfo [keyPrefix=" + keyPrefix + ", header=" + header + ", deviceNumber=" + deviceNumber
				+ ", physicalPin=" + physicalPin + ", name=" + name + ", chip=" + chip + ", lineOffset=" + lineOffset
				+ ", modes=" + modes + "]";
	}

	public void updateGpioChipId(int oldChipId, int newChipId) {
		if (chip == oldChipId) {
			chip = newChipId;
		}
	}
}
