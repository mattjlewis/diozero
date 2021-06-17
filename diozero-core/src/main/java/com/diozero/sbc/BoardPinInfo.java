package com.diozero.sbc;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     BoardPinInfo.java
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.diozero.api.DeviceMode;
import com.diozero.api.NoSuchDeviceException;
import com.diozero.api.PinInfo;

/*-
 * cat /sys/kernel/debug/gpio
 * https://github.com/google/periph/tree/master/host
 */
/**
 * Provide information about the GPIOs that are available on the connected
 * board.
 */
public class BoardPinInfo {
	public static final String GPIO_KEY_PREFIX = "GPIO";
	public static final String DEFAULT_GPIO_NAME_PREFIX = GPIO_KEY_PREFIX;
	public static final String ADC_KEY_PREFIX = "AIN";
	public static final String DEFAULT_ADC_NAME_PREFIX = ADC_KEY_PREFIX;
	public static final String DAC_KEY_PREFIX = "AOUT";
	public static final String DEFAULT_DAC_NAME_PREFIX = DAC_KEY_PREFIX;

	private Map<Integer, PinInfo> gpios;
	private Map<Integer, Integer> pwmNumToGpioMapping;
	private Map<Integer, PinInfo> adcs;
	private Map<Integer, PinInfo> dacs;
	private Map<String, PinInfo> pinsByName;
	private Map<String, Map<Integer, PinInfo>> headers;

	public BoardPinInfo() {
		gpios = new HashMap<>();
		pwmNumToGpioMapping = new HashMap<>();
		adcs = new HashMap<>();
		dacs = new HashMap<>();
		pinsByName = new HashMap<>();
		headers = new TreeMap<>();
	}

	private Map<Integer, PinInfo> getPinsForHeader(String header) {
		Map<Integer, PinInfo> pins_by_number = headers.get(header);
		if (pins_by_number == null) {
			pins_by_number = new TreeMap<>();
			headers.put(header, pins_by_number);
		}

		return pins_by_number;
	}

	public void addGeneralPinInfo(int physicalPin, String name) {
		addGeneralPinInfo(PinInfo.DEFAULT_HEADER, physicalPin, name, PinInfo.NOT_DEFINED, PinInfo.NOT_DEFINED);
	}

	public void addGeneralPinInfo(int physicalPin, String name, int chip, int line) {
		addGeneralPinInfo(PinInfo.DEFAULT_HEADER, physicalPin, name, chip, line);
	}

	public void addGeneralPinInfo(String header, int physicalPin, String name) {
		addGeneralPinInfo(new PinInfo("", header, PinInfo.NOT_DEFINED, physicalPin, name, Collections.emptySet()));
	}

	public void addGeneralPinInfo(String header, int physicalPin, String name, int chip, int line) {
		addGeneralPinInfo(new PinInfo("", header, PinInfo.NOT_DEFINED, physicalPin, name, Collections.emptySet(),
				PinInfo.NOT_DEFINED, chip, line));
	}

	public void addGeneralPinInfo(PinInfo pinInfo) {
		getPinsForHeader(pinInfo.getHeader()).put(Integer.valueOf(pinInfo.getPhysicalPin()), pinInfo);
	}

	public PinInfo addGpioPinInfo(int gpioNum, int physicalPin, Collection<DeviceMode> modes) {
		return addGpioPinInfo(PinInfo.DEFAULT_HEADER, gpioNum, DEFAULT_GPIO_NAME_PREFIX + gpioNum, physicalPin, modes);
	}

	public PinInfo addGpioPinInfo(int gpioNum, String name, int physicalPin, Collection<DeviceMode> modes) {
		return addGpioPinInfo(PinInfo.DEFAULT_HEADER, gpioNum, name, physicalPin, modes);
	}

	public PinInfo addGpioPinInfo(int gpioNum, String name, int physicalPin, Collection<DeviceMode> modes, int chip,
			int line) {
		return addGpioPinInfo(PinInfo.DEFAULT_HEADER, gpioNum, name, physicalPin, modes, chip, line);
	}

	public PinInfo addGpioPinInfo(String header, int gpioNum, int physicalPin, Collection<DeviceMode> modes) {
		return addGpioPinInfo(header, gpioNum, DEFAULT_GPIO_NAME_PREFIX + gpioNum, physicalPin, modes);
	}

	public PinInfo addGpioPinInfo(String header, int gpioNum, String name, int physicalPin,
			Collection<DeviceMode> modes) {
		return addGpioPinInfo(header, gpioNum, name, physicalPin, modes, PinInfo.NOT_DEFINED, PinInfo.NOT_DEFINED);
	}

	public PinInfo addGpioPinInfo(String header, int gpioNum, String name, int physicalPin,
			Collection<DeviceMode> modes, int chip, int line) {
		PinInfo pin_info = new PinInfo(GPIO_KEY_PREFIX, header, gpioNum, physicalPin, name, modes,
				mapToSysFsGpioNumber(gpioNum), chip, line);
		addGpioPinInfo(pin_info);
		return pin_info;
	}

	public void addGpioPinInfo(PinInfo pinInfo) {
		pinsByName.put(pinInfo.getName(), pinInfo);

		if (pinInfo.getDeviceNumber() != PinInfo.NOT_DEFINED) {
			gpios.put(Integer.valueOf(pinInfo.getDeviceNumber()), pinInfo);
		}

		if (pinInfo.getPhysicalPin() != PinInfo.NOT_DEFINED) {
			getPinsForHeader(pinInfo.getHeader()).put(Integer.valueOf(pinInfo.getPhysicalPin()), pinInfo);
		}
	}

	public PinInfo addPwmPinInfo(int gpioNum, int physicalPin, int pwmChip, int pwmNum, Collection<DeviceMode> modes) {
		return addPwmPinInfo(PinInfo.DEFAULT_HEADER, gpioNum, DEFAULT_GPIO_NAME_PREFIX + gpioNum, physicalPin, pwmChip,
				pwmNum, modes);
	}

	public PinInfo addPwmPinInfo(int gpioNum, String name, int physicalPin, int pwmChip, int pwmNum,
			Collection<DeviceMode> modes) {
		return addPwmPinInfo(PinInfo.DEFAULT_HEADER, gpioNum, name, physicalPin, pwmChip, pwmNum, modes);
	}

	public PinInfo addPwmPinInfo(int gpioNum, String name, int physicalPin, int pwmChip, int pwmNum,
			Collection<DeviceMode> modes, int chip, int line) {
		return addPwmPinInfo(PinInfo.DEFAULT_HEADER, gpioNum, name, physicalPin, pwmChip, pwmNum, modes, chip, line);
	}

	public PinInfo addPwmPinInfo(String header, int gpioNumber, String name, int physicalPin, int pwmChip, int pwmNum,
			Collection<DeviceMode> modes) {
		return addPwmPinInfo(header, gpioNumber, name, physicalPin, pwmChip, pwmNum, modes, PinInfo.NOT_DEFINED,
				PinInfo.NOT_DEFINED);
	}

	public PinInfo addPwmPinInfo(String header, int gpioNumber, String name, int physicalPin, int pwmChip, int pwmNum,
			Collection<DeviceMode> modes, int chip, int line) {
		PinInfo pin_info = new PinInfo(GPIO_KEY_PREFIX, header, gpioNumber, physicalPin, pwmChip, pwmNum, name, modes,
				mapToSysFsGpioNumber(gpioNumber), chip, line);
		addGpioPinInfo(pin_info);
		pwmNumToGpioMapping.put(Integer.valueOf(pwmNum), Integer.valueOf(gpioNumber));
		return pin_info;
	}

	public PinInfo addAdcPinInfo(int adcNumber, int physicalPin) {
		return addAdcPinInfo(PinInfo.DEFAULT_HEADER, adcNumber, DEFAULT_ADC_NAME_PREFIX + adcNumber, physicalPin);
	}

	public PinInfo addAdcPinInfo(int adcNumber, String name, int physicalPin) {
		return addAdcPinInfo(PinInfo.DEFAULT_HEADER, adcNumber, name, physicalPin);
	}

	public PinInfo addAdcPinInfo(String header, int adcNumber, String name, int physicalPin) {
		PinInfo pin_info = new PinInfo(ADC_KEY_PREFIX, header, adcNumber, physicalPin, name, PinInfo.ANALOG_INPUT);
		addAdcPinInfo(pin_info);
		return pin_info;
	}

	public void addAdcPinInfo(PinInfo pinInfo) {
		adcs.put(Integer.valueOf(pinInfo.getDeviceNumber()), pinInfo);
		pinsByName.put(pinInfo.getName(), pinInfo);

		if (pinInfo.getPhysicalPin() != PinInfo.NOT_DEFINED) {
			getPinsForHeader(pinInfo.getHeader()).put(Integer.valueOf(pinInfo.getPhysicalPin()), pinInfo);
		}
	}

	public PinInfo addDacPinInfo(int dacNumber, int pin) {
		return addDacPinInfo(PinInfo.DEFAULT_HEADER, dacNumber, DEFAULT_DAC_NAME_PREFIX + dacNumber, pin);
	}

	public PinInfo addDacPinInfo(int dacNumber, String name, int pin) {
		return addDacPinInfo(PinInfo.DEFAULT_HEADER, dacNumber, name, pin);
	}

	public PinInfo addDacPinInfo(String header, int dacNumber, int pin) {
		return addDacPinInfo(PinInfo.DEFAULT_HEADER, dacNumber, DEFAULT_DAC_NAME_PREFIX + dacNumber, pin);
	}

	public PinInfo addDacPinInfo(String header, int dacNumber, String name, int pin) {
		PinInfo pin_info = new PinInfo(DAC_KEY_PREFIX, header, dacNumber, pin, name, PinInfo.ANALOG_OUTPUT);
		addDacPinInfo(pin_info);
		return pin_info;
	}

	public void addDacPinInfo(PinInfo pinInfo) {
		dacs.put(Integer.valueOf(pinInfo.getDeviceNumber()), pinInfo);
		pinsByName.put(pinInfo.getName(), pinInfo);
		if (pinInfo.getPhysicalPin() != PinInfo.NOT_DEFINED) {
			getPinsForHeader(pinInfo.getHeader()).put(Integer.valueOf(pinInfo.getPhysicalPin()), pinInfo);
		}
	}

	public Optional<PinInfo> getByGpioNumber(int gpio) {
		return Optional.ofNullable(gpios.get(Integer.valueOf(gpio)));
	}

	public PinInfo getByGpioNumberOrThrow(int gpio) throws NoSuchDeviceException {
		return getByGpioNumber(gpio).orElseThrow(() -> new NoSuchDeviceException("No such GPIO #" + gpio));
	}

	public Optional<PinInfo> getByPhysicalPin(String headerName, int physicalPin) {
		Map<Integer, PinInfo> header = headers.get(headerName);
		if (header == null) {
			return Optional.empty();
		}

		return Optional.ofNullable(header.get(Integer.valueOf(physicalPin)));
	}

	public PinInfo getByPhysicalPinOrThrow(String headerName, int physicalPin) throws NoSuchDeviceException {
		return getByPhysicalPin(headerName, physicalPin)
				.orElseThrow(() -> new NoSuchDeviceException("No such device #" + headerName + ":" + physicalPin));
	}

	public Optional<PinInfo> getByChipAndLineOffset(int chipId, int lineOffset) {
		if (chipId == PinInfo.NOT_DEFINED || lineOffset == PinInfo.NOT_DEFINED) {
			return Optional.empty();
		}
		for (Map<Integer, PinInfo> header : headers.values()) {
			for (PinInfo pin : header.values()) {
				if (pin.getChip() == chipId && pin.getLineOffset() == lineOffset) {
					return Optional.of(pin);
				}
			}
		}
		return Optional.empty();
	}

	public PinInfo getByChipAndLineOffsetOrThrow(int chipId, int lineOffset) throws NoSuchDeviceException {
		return getByChipAndLineOffset(chipId, lineOffset)
				.orElseThrow(() -> new NoSuchDeviceException("No such device #" + chipId + ":" + lineOffset));
	}

	public Optional<PinInfo> getByPwmNumber(int pwmNum) {
		Integer gpio = pwmNumToGpioMapping.get(Integer.valueOf(pwmNum));
		if (gpio == null) {
			return Optional.empty();
		}
		return Optional.ofNullable(gpios.get(gpio));
	}

	public PinInfo getByPwmNumberOrThrow(int pwmNum) {
		return getByPwmNumber(pwmNum).orElseThrow(() -> new NoSuchDeviceException("No such PWM #" + pwmNum));
	}

	public Optional<PinInfo> getByPwmOrGpioNumber(int pwmOrGpioNum) {
		// Lookup by PWM number first, if not found or doesn't support
		// PWM_OUTPUT, lookup by GPIO number
		Optional<PinInfo> pin_info = getByPwmNumber(pwmOrGpioNum);
		if (!pin_info.isPresent() || !pin_info.get().isSupported(DeviceMode.PWM_OUTPUT)) {
			pin_info = getByGpioNumber(pwmOrGpioNum);
		}
		return pin_info;
	}

	public PinInfo getByPwmOrGpioNumberOrThrow(int pwmOrGpioNum) {
		return getByPwmOrGpioNumber(pwmOrGpioNum)
				.orElseThrow(() -> new NoSuchDeviceException("No such PWM #" + pwmOrGpioNum));
	}

	public Optional<PinInfo> getByAdcNumber(int adcNumber) {
		return Optional.ofNullable(adcs.get(Integer.valueOf(adcNumber)));
	}

	public PinInfo getByAdcNumberOrThrow(int adcNumber) {
		return getByAdcNumber(adcNumber).orElseThrow(() -> new NoSuchDeviceException("No such ADC #" + adcNumber));
	}

	public Optional<PinInfo> getByDacNumber(int dacNumber) {
		return Optional.ofNullable(dacs.get(Integer.valueOf(dacNumber)));
	}

	public PinInfo getByDacNumberOrThrow(int dacNumber) {
		return getByDacNumber(dacNumber).orElseThrow(() -> new NoSuchDeviceException("No such DAC #" + dacNumber));
	}

	public PinInfo getByName(String name) {
		return pinsByName.get(name);
	}

	public Map<String, Map<Integer, PinInfo>> getHeaders() {
		return headers;
	}

	public Collection<String> getHeaderNames() {
		return headers.keySet();
	}

	public Collection<Map<Integer, PinInfo>> getHeaderValues() {
		return headers.values();
	}

	public Map<Integer, PinInfo> getGpios() {
		return gpios;
	}

	public Collection<PinInfo> getGpioPins() {
		return gpios.values();
	}

	public Collection<PinInfo> getAdcPins() {
		return adcs.values();
	}

	public Collection<PinInfo> getDacPins() {
		return dacs.values();
	}

	@SuppressWarnings("static-method")
	public int mapToSysFsGpioNumber(int gpio) {
		return gpio;
	}

	/**
	 * Get the PWM chip for the specified pin. Only relevant for sysfs hardware PWM
	 * control, in particular on the BeagleBone Black where the PWM chip number can
	 * change between reboots.
	 *
	 * @param pinInfo object describing this pin
	 * @return The PWM chip number for the requested PWM channel number, return -1
	 *         if not supported
	 */
	@SuppressWarnings("static-method")
	public int getPwmChipNumberOverride(PinInfo pinInfo) {
		return pinInfo.getPwmChip();
	}
}
