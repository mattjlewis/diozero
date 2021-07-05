/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     GpioReadAll.java
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

package com.diozero.sampleapps;

import static com.diozero.sampleapps.util.ConsoleUtil.getGpiodName;
import static com.diozero.sampleapps.util.ConsoleUtil.getModeColour;
import static com.diozero.sampleapps.util.ConsoleUtil.getModeString;
import static com.diozero.sampleapps.util.ConsoleUtil.getNotDefined;
import static com.diozero.sampleapps.util.ConsoleUtil.getPinColour;
import static com.diozero.sampleapps.util.ConsoleUtil.getValueColour;
import static com.diozero.sampleapps.util.ConsoleUtil.getValueString;
import static com.diozero.sampleapps.util.ConsoleUtil.gpioRead;
import static org.fusesource.jansi.Ansi.ansi;

import java.util.Map;
import java.util.Optional;

import org.fusesource.jansi.AnsiConsole;
import org.tinylog.Logger;

import com.diozero.api.PinInfo;
import com.diozero.internal.spi.NativeDeviceFactoryInterface;
import com.diozero.sbc.DeviceFactoryHelper;
import com.diozero.util.StringUtil;

public class GpioReadAll {
	public static void main(String[] args) {
		// Attempt to initialise Jansi
		try {
			AnsiConsole.systemInstall();
		} catch (Throwable t) {
			// Ignore
			Logger.trace(t, "Jansi native library not available on this platform: {}", t);
		}

		try (NativeDeviceFactoryInterface device_factory = DeviceFactoryHelper.getNativeDeviceFactory()) {
			device_factory.getBoardInfo().getHeaders().entrySet()
					.forEach(header_entry -> printPins(device_factory, header_entry.getKey(), header_entry.getValue()));
		}
	}

	private static void printPins(NativeDeviceFactoryInterface deviceFactory, String headerName,
			Map<Integer, PinInfo> pins) {
		if (pins == null) {
			Logger.error("Unable to resolve pins for header {}", headerName);
			return;
		}

		// Get the maximum pin name length
		int max_name_length = Math.max(8,
				pins.values().stream().mapToInt(pin_info -> pin_info.getName().length()).max().orElse(8));

		String name_dash = StringUtil.repeat('-', max_name_length);
		System.out.format(ansi().render("@|bold Header|@: %s%n").toString(), headerName);
		System.out.format("+-----+-%s-+------+---+--------+----------+--------+---+------+-%s-+-----+%n", name_dash,
				name_dash);
		System.out.format(ansi()
				.render("+ @|bold GP#|@ + @|bold %" + max_name_length
						+ "s|@ + @|bold Mode|@ + @|bold V|@ +  @|bold gpiod|@ + @|bold Physical|@ + @|bold gpiod|@  "
						+ "+ @|bold V|@ + @|bold Mode|@ + @|bold %-" + max_name_length + "s|@ + @|bold GP#|@ +%n")
				.toString(), "Name", "Name");
		System.out.format("+-----+-%s-+------+---+--------+----------+--------+---+------+-%s-+-----+%n", name_dash,
				name_dash);

		int index = 0;
		for (PinInfo pin_info : pins.values()) {
			int gpio = pin_info.getDeviceNumber();
			Optional<Boolean> value = gpioRead(deviceFactory, pin_info);
			if (index++ % 2 == 0) {
				System.out.format(
						ansi().render("| @|bold," + getPinColour(pin_info) + " %3s|@ | @|bold," + getPinColour(pin_info)
								+ " %" + max_name_length + "s|@ | @|bold," + getModeColour(deviceFactory, pin_info)
								+ " %4s|@ | @|bold," + getValueColour(value) + " %1s|@ | @|bold %6s|@ | @|bold %2s|@ |")
								.toString(),
						getNotDefined(gpio), pin_info.getName(), getModeString(deviceFactory, pin_info),
						getValueString(value), getGpiodName(pin_info.getChip(), pin_info.getLineOffset()),
						getNotDefined(pin_info.getPhysicalPin()));
			} else {
				System.out.format(
						ansi().render("| @|bold %-2s|@ | @|bold %-6s|@ | @|bold,FG_" + getValueColour(value)
								+ " %1s|@ | @|bold,FG_" + getModeColour(deviceFactory, pin_info)
								+ " %-4s|@ | @|bold,FG_" + getPinColour(pin_info) + " %-" + max_name_length
								+ "s|@ | @|bold,FG_" + getPinColour(pin_info) + " %-3s|@ |%n").toString(),
						getNotDefined(pin_info.getPhysicalPin()),
						getGpiodName(pin_info.getChip(), pin_info.getLineOffset()), getValueString(value),
						getModeString(deviceFactory, pin_info), pin_info.getName(), getNotDefined(gpio));
			}
		}

		if (index % 2 == 1) {
			System.out.format("|    |        |   |      | %-" + max_name_length + "s |     |%n", "");
		}

		System.out.format("+-----+-%s-+------+---+--------+----------+--------+---+------+-%s-+-----+%n", name_dash,
				name_dash);
	}
}
