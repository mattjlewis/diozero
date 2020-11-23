package com.diozero.api;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     PinInfo.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
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
	public static final String VCC_5V = "5V";
	public static final String VCC_3V3 = "3.3V";

	private String keyPrefix;
	private String header;
	private int deviceNumber;
	private int physicalPin;
	private String name;
	private Collection<DeviceMode> modes;
	private int sysFsNumber;
	private int chip;
	private int lineOffset;

	public PinInfo(String keyPrefix, String header, int deviceNumber, int physicalPin, String name,
			Collection<DeviceMode> modes) {
		this(keyPrefix, header, deviceNumber, physicalPin, name, modes, deviceNumber, NOT_DEFINED, NOT_DEFINED);
	}

	public PinInfo(String keyPrefix, String header, int deviceNumber, int physicalPin, String name,
			Collection<DeviceMode> modes, int sysFsNumber) {
		this(keyPrefix, header, deviceNumber, physicalPin, name, modes, sysFsNumber, NOT_DEFINED, NOT_DEFINED);
	}

	public PinInfo(String keyPrefix, String header, int deviceNumber, int physicalPin, String name,
			Collection<DeviceMode> modes, int sysFsNumber, int chip, int line) {
		this.keyPrefix = keyPrefix;
		this.header = header;
		this.deviceNumber = deviceNumber;
		this.physicalPin = physicalPin;
		this.name = name;
		this.modes = modes;
		this.sysFsNumber = sysFsNumber;
		this.chip = chip;
		this.lineOffset = line;
	}

	public String getKeyPrefix() {
		return keyPrefix;
	}

	public String getHeader() {
		return header;
	}

	public int getDeviceNumber() {
		return deviceNumber;
	}

	public int getPhysicalPin() {
		return physicalPin;
	}

	public String getName() {
		return name;
	}

	public Collection<DeviceMode> getModes() {
		return modes;
	}

	public boolean isSupported(DeviceMode mode) {
		return modes.contains(mode);
	}

	public boolean isDigitalInputSupported() {
		return modes.contains(DeviceMode.DIGITAL_OUTPUT);
	}

	public boolean isDigitalOutputSupported() {
		return modes.contains(DeviceMode.DIGITAL_OUTPUT);
	}

	public boolean isPwmOutputSupported() {
		return modes.contains(DeviceMode.PWM_OUTPUT);
	}

	public boolean isAnalogInputSupported() {
		return modes.contains(DeviceMode.ANALOG_INPUT);
	}

	public boolean isAnalogOutputSupported() {
		return modes.contains(DeviceMode.ANALOG_OUTPUT);
	}

	public int getSysFsNumber() {
		return sysFsNumber;
	}

	public int getChip() {
		return chip;
	}

	public void setChip(int chip) {
		this.chip = chip;
	}

	public int getLineOffset() {
		return lineOffset;
	}

	public void setLineOffset(int lineOffset) {
		this.lineOffset = lineOffset;
	}

	@Override
	public String toString() {
		return "PinInfo [keyPrefix=" + keyPrefix + ", header=" + header + ", deviceNumber=" + deviceNumber
				+ ", physicalPin=" + physicalPin + ", name=" + name + ", chip=" + chip + ", lineOffset=" + lineOffset
				+ ", modes=" + modes + "]";
	}
}
