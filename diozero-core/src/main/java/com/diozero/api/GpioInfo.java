package com.diozero.api;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 - 2017 mattjlewis
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


import java.util.Arrays;
import java.util.List;

public class GpioInfo {
	public static final int NOT_DEFINED = -1;
	public static final List<DeviceMode> DIGITAL_IN_OUT = Arrays.asList(
			DeviceMode.DIGITAL_INPUT,
			DeviceMode.DIGITAL_OUTPUT,
			DeviceMode.SOFTWARE_PWM_OUTPUT);
	public static final List<DeviceMode> DIGITAL_IN_OUT_PWM = Arrays.asList(
			DeviceMode.DIGITAL_INPUT,
			DeviceMode.DIGITAL_OUTPUT,
			DeviceMode.SOFTWARE_PWM_OUTPUT,
			DeviceMode.PWM_OUTPUT);
	public static final List<DeviceMode> ANALOG_INPUT = Arrays.asList(
			DeviceMode.ANALOG_INPUT);
	public static final List<DeviceMode> ANALOG_OUTPUT = Arrays.asList(
			DeviceMode.ANALOG_OUTPUT);
	public static final List<DeviceMode> PWM_OUTPUT = Arrays.asList(
			DeviceMode.PWM_OUTPUT);
	public static final List<DeviceMode> DIGITAL_PWM_OUTPUT = Arrays.asList(
			DeviceMode.DIGITAL_OUTPUT, DeviceMode.PWM_OUTPUT);
	public static final List<DeviceMode> DIGITAL_ANALOG_INPUT = Arrays.asList(
			DeviceMode.DIGITAL_INPUT, DeviceMode.ANALOG_INPUT);
	
	private static final String DEFAULT_GPIO_NAME_PREFIX = "GPIO";
	private static final String DEFAULT_HEADER = "DEFAULT";
	
	private String header;
	private int gpioNum;
	private String name;
	private int pin;
	private int pwmNum;
	private List<DeviceMode> modes;
	
	public GpioInfo(int gpioNum, int pin, List<DeviceMode> modes) {
		this(DEFAULT_HEADER, gpioNum, DEFAULT_GPIO_NAME_PREFIX + gpioNum, pin, NOT_DEFINED, modes);
	}
	
	public GpioInfo(int gpioNum, String name, int pin, List<DeviceMode> modes) {
		this(DEFAULT_HEADER, gpioNum, name, pin, NOT_DEFINED, modes);
	}
	
	public GpioInfo(int gpioNum, String name, int pin, int pwmNum, List<DeviceMode> modes) {
		this(DEFAULT_HEADER, gpioNum, name, pin, pwmNum, modes);
	}
	
	public GpioInfo(String header, int gpioNum, int pin, List<DeviceMode> modes) {
		this(header, gpioNum, DEFAULT_GPIO_NAME_PREFIX + gpioNum, pin, NOT_DEFINED, modes);
	}
	
	public GpioInfo(String header, int gpioNum, String name, int pin, List<DeviceMode> modes) {
		this(header, gpioNum, name, pin, NOT_DEFINED, modes);
	}
	
	public GpioInfo(String header, int gpioNum, String name, int pin, int pwmNum, List<DeviceMode> modes) {
		this.header = header;
		this.gpioNum = gpioNum;
		this.name = name;
		this.pin = pin;
		this.pwmNum = pwmNum;
		this.modes = modes;
	}
	
	public String getHeader() {
		return header;
	}
	
	public int getGpioNum() {
		return gpioNum;
	}

	public String getName() {
		return name;
	}
	
	public int getPin() {
		return pin;
	}
	
	public int getPwmNum() {
		return pwmNum;
	}

	public List<DeviceMode> getModes() {
		return modes;
	}

	@Override
	public String toString() {
		return "GpioInfo [header=" + header + ", gpioNum=" + gpioNum + ", name=" + name + ", pin=" + pin + ", pwmNum="
				+ pwmNum + ", modes=" + modes + "]";
	}
}
