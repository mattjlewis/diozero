/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Sample applications
 * Filename:     GpioReadAll.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
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

import java.util.Map;
import java.util.Optional;

import org.fusesource.jansi.Ansi;
import org.fusesource.jansi.AnsiConsole;
import org.tinylog.Logger;

import com.diozero.api.PinInfo;
import com.diozero.internal.spi.MmapGpioInterface;
import com.diozero.sbc.BoardInfo;
import com.diozero.sbc.DeviceFactoryHelper;
import com.diozero.util.StringUtil;

public class GpioReadAll {
	public static void main(String[] args) {
		BoardInfo board_info = DeviceFactoryHelper.getNativeDeviceFactory().getBoardInfo();
		MmapGpioInterface mmap_gpio = board_info.createMmapGpio();
		if (mmap_gpio == null) {
			Logger.error("Unable to load MMAP GPIO interface");
			return;
		}
		mmap_gpio.initialise();

		// Attempt to initialise Jansi
		try {
			AnsiConsole.systemInstall();
		} catch (Throwable t) {
			// Ignore
			Logger.trace(t, "Jansi native library not available on this platform: {}", t);
		}

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

		String name_dash = StringUtil.repeat('-', max_name_length);
		System.out.println(Ansi.ansi().bold().a("Header").boldOff().a(": ").a(headerName));
		System.out.format("+-----+-%s-+------+---+--------+----------+--------+---+------+-%s-+-----+%n", name_dash,
				name_dash);
		System.out.format("+ GP# + %" + max_name_length + "s + Mode + V +  gpiod + Physical + gpiod  + V + Mode + %-"
				+ max_name_length + "s + GP# +%n", "Name", "Name");
		System.out.format("+-----+-%s-+------+---+--------+----------+--------+---+------+-%s-+-----+%n", name_dash,
				name_dash);

		int index = 0;
		for (PinInfo pin_info : pins.values()) {
			int gpio = pin_info.getDeviceNumber();
			Optional<Boolean> value = gpioRead(mmapGpio, pin_info);
			if (index++ % 2 == 0) {
				System.out.print(Ansi.ansi().a("| ") //
						.bold().fg(getPinColour(pin_info)).format("%3s", getNotDefined(gpio)).fgDefault().boldOff()
						.a(" | ") //
						.bold().fg(getPinColour(pin_info)).format("%" + max_name_length + "s", pin_info.getName())
						.fgDefault().boldOff().a(" | ") //
						.bold().fg(getModeColour(mmapGpio, pin_info)).format("%4s", getModeString(mmapGpio, pin_info))
						.fgDefault().boldOff().a(" | ") //
						.bold().fg(getValueColour(value)).format("%1s", getValueString(value)).fgDefault().boldOff()
						.a(" | ") //
						.bold().format("%6s", getGpiodName(pin_info.getChip(), pin_info.getLineOffset())).boldOff()
						.a(" | ") //
						.bold().format("%2s", getNotDefined(pin_info.getPhysicalPin())).boldOff().a(" |"));
				/*-
				System.out.format("| %3s | %" + max_name_length + "s | %4s | %1s | %6s | %2s |", getNotDefined(gpio), //
						pin_info.getName(), //
						getModeString(mmapGpio, pin_info), //
						gpioRead(mmapGpio, pin_info), //
						getGpiodName(pin_info.getChip(), pin_info.getLineOffset()), //
						getNotDefined(pin_info.getPhysicalPin()));
				*/
			} else {
				System.out.println(Ansi.ansi().a("| ") //
						.bold().format("%-2s", getNotDefined(pin_info.getPhysicalPin())).boldOff().a(" | ") //
						.bold().format("%-6s", getGpiodName(pin_info.getChip(), pin_info.getLineOffset())).boldOff()
						.a(" | ") //
						.bold().fg(getValueColour(value)).format("%1s", getValueString(value)).fgDefault().boldOff()
						.a(" | ") //
						.bold().fg(getModeColour(mmapGpio, pin_info)).format("%-4s", getModeString(mmapGpio, pin_info))
						.fgDefault().boldOff().a(" | ") //
						.bold().fg(getPinColour(pin_info)).format("%-" + max_name_length + "s", pin_info.getName())
						.fgDefault().boldOff().a(" | ") //
						.bold().fg(getPinColour(pin_info)).format("%-3s", getNotDefined(gpio)).fgDefault().boldOff()
						.a(" |"));
			}
		}
		System.out.format("+-----+-%s-+------+---+--------+----------+--------+---+------+-%s-+-----+%n", name_dash,
				name_dash);
	}
}
