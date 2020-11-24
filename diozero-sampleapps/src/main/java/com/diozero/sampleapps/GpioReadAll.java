package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Sample applications
 * Filename:     GpioReadAll.java  
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

import java.util.Collections;
import java.util.Map;

import org.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.PinInfo;
import com.diozero.internal.spi.MmapGpioInterface;
import com.diozero.sbc.BoardInfo;
import com.diozero.sbc.DeviceFactoryHelper;

public class GpioReadAll {
	public static void main(String[] args) {
		BoardInfo board_info = DeviceFactoryHelper.getNativeDeviceFactory().getBoardInfo();
		MmapGpioInterface mmap_gpio = board_info.createMmapGpio();
		if (mmap_gpio == null) {
			Logger.error("Unable to load MMAP GPIO interface");
			return;
		}
		mmap_gpio.initialise();

		board_info.getHeaders().entrySet()
				.forEach(header_entry -> printPins(mmap_gpio, header_entry.getKey(), header_entry.getValue()));
	}

	private static void printPins(MmapGpioInterface mmapGpio, String headerName, Map<Integer, PinInfo> pins) {
		if (pins == null) {
			Logger.error("Unable to resolve pins for header {}", headerName);
			return;
		}

		// Get the maximum pin name length
		int max_name_length = Math.max(8,
				pins.values().stream().mapToInt(pin_info -> pin_info.getName().length()).max().orElse(8));

		String name_dash = String.join("", Collections.nCopies(max_name_length, "-"));
		System.out.format("Pins for header %s:%n", headerName);
		System.out.format("+-----+-%s-+------+---+--------+----------+--------+---+------+-%s-+-----+%n", name_dash,
				name_dash);
		System.out.format("+ GP# + %" + max_name_length + "s + Mode + V +  gpiod + Physical + gpiod  + V + Mode + %-"
				+ max_name_length + "s + GP# +%n", "Name", "Name");
		System.out.format("+-----+-%s-+------+---+--------+----------+--------+---+------+-%s-+-----+%n", name_dash,
				name_dash);

		int index = 0;
		for (PinInfo pin_info : pins.values()) {
			int gpio = pin_info.getDeviceNumber();
			if (index++ % 2 == 0) {
				System.out.format("| %3s | %" + max_name_length + "s | %4s | %1s | %6s | %2s |", getNotDefined(gpio), //
						pin_info.getName(), //
						getModeString(mmapGpio, pin_info), //
						gpioRead(mmapGpio, pin_info), //
						getGpiodName(pin_info.getChip(), pin_info.getLineOffset()), //
						getNotDefined(pin_info.getPhysicalPin()));
			} else {
				System.out.format("| %-2s | %-6s | %1s | %-4s | %-" + max_name_length + "s | %-3s |%n",
						getNotDefined(pin_info.getPhysicalPin()), //
						getGpiodName(pin_info.getChip(), pin_info.getLineOffset()), //
						gpioRead(mmapGpio, pin_info), //
						getModeString(mmapGpio, pin_info), //
						pin_info.getName(), //
						getNotDefined(gpio));
			}
		}
		System.out.format("+-----+-%s-+------+---+--------+----------+--------+---+------+-%s-+-----+%n", name_dash,
				name_dash);
	}

	private static String gpioRead(MmapGpioInterface mmapGpio, PinInfo pinInfo) {
		int gpio = pinInfo.getDeviceNumber();
		if (gpio == PinInfo.NOT_DEFINED) {
			return " ";
		}

		if (pinInfo.getModes().contains(DeviceMode.DIGITAL_INPUT)
				|| pinInfo.getModes().contains(DeviceMode.DIGITAL_OUTPUT)) {
			return mmapGpio.gpioRead(gpio) ? "1" : "0";
		}

		return " ";
	}

	private static String getGpiodName(int chip, int lineOffset) {
		if (chip == PinInfo.NOT_DEFINED || lineOffset == PinInfo.NOT_DEFINED) {
			return "";
		}
		return String.format("%2s:%-3s", Integer.valueOf(chip), Integer.valueOf(lineOffset));
	}

	private static String getModeString(MmapGpioInterface mmapGpio, PinInfo pinInfo) {
		int gpio = pinInfo.getDeviceNumber();
		if (gpio == PinInfo.NOT_DEFINED) {
			return "";
		}

		if (pinInfo.getModes().contains(DeviceMode.DIGITAL_INPUT)
				|| pinInfo.getModes().contains(DeviceMode.DIGITAL_OUTPUT)) {
			switch (mmapGpio.getMode(gpio)) {
			case DIGITAL_OUTPUT:
				return "Out";
			case DIGITAL_INPUT:
				return "In";
			default:
				return "Unkn";
			}
		}

		return "Unkn";
	}

	public static String getNotDefined(int value) {
		return value == PinInfo.NOT_DEFINED ? "" : Integer.toString(value);
	}
}
