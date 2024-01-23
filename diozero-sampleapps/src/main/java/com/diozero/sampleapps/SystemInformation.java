/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     SystemInformation.java
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

package com.diozero.sampleapps;

import static com.diozero.sampleapps.util.ConsoleUtil.getGpiodName;
import static com.diozero.sampleapps.util.ConsoleUtil.getNotDefined;
import static com.diozero.sampleapps.util.ConsoleUtil.getPinColour;
import static org.fusesource.jansi.Ansi.ansi;

import java.util.Map;
import java.util.stream.Collectors;

import org.fusesource.jansi.AnsiConsole;
import org.tinylog.Logger;

import com.diozero.api.PinInfo;
import com.diozero.internal.spi.NativeDeviceFactoryInterface;
import com.diozero.sbc.BoardInfo;
import com.diozero.sbc.DeviceFactoryHelper;
import com.diozero.sbc.LocalSystemInfo;
import com.diozero.util.Diozero;
import com.diozero.util.StringUtil;

public class SystemInformation {
	private static final int MIN_PIN_NAME_LENGTH = 8;

	public static void main(String[] args) {
		// Attempt to initialise Jansi
		try {
			AnsiConsole.systemInstall();
		} catch (Throwable t) {
			// Ignore
			Logger.trace(t, "Jansi native library not available on this platform: {}", t);
		}

		// System.out.println(Ansi.ansi().eraseScreen().render("@|red Hello|@ @|green
		// World|@") );

		LocalSystemInfo sys_info = LocalSystemInfo.getInstance();
		System.out.println(ansi().render("@|bold,underline Local System Info|@"));
		/*-
		// Can't do this until JAnsi supports ARMv6:
		AnsiConsole.out().format(render("@|bold diozero version|@: %s%n"), Diozero.getVersion());
		*/
		System.out.format(ansi().render("@|bold diozero version|@: %s%n").toString(), Diozero.getVersion());
		System.out.format(ansi().render("@|bold Operating System|@: %s %s - %s%n").toString(),
				sys_info.getOperatingSystemId(), sys_info.getOperatingSystemVersion(), sys_info.getOsArch());
		System.out.format(ansi().render("@|bold CPU Temperature|@: %.2f%n").toString(),
				Float.valueOf(sys_info.getCpuTemperature()));
		System.out.println();

		try (NativeDeviceFactoryInterface ndf = DeviceFactoryHelper.getNativeDeviceFactory()) {
			BoardInfo board_info = ndf.getBoardInfo();
			System.out.println(ansi().render("@|bold,underline Detected Board Info|@"));
			System.out.format(ansi().render("@|bold Device Factory|@: %s%n").toString(), ndf.getName());
			System.out.format(ansi().render("@|bold Board|@: %s (RAM: %,d bytes, O/S: %s %s)%n").toString(),
					board_info.getModel(), Integer.valueOf(board_info.getMemoryKb()), board_info.getOperatingSystemId(),
					board_info.getOperatingSystemVersion());
			System.out.format(ansi().render("@|bold I2C Bus Numbers|@: %s%n").toString(),
					ndf.getI2CBusNumbers().stream().map(Object::toString).collect(Collectors.joining(", ")));

			System.out.println();
			for (Map.Entry<String, Map<Integer, PinInfo>> header_pins_entry : board_info.getHeaders().entrySet()) {
				// Get the maximum pin name length
				int max_length = Math.max(MIN_PIN_NAME_LENGTH, header_pins_entry.getValue().values().stream()
						.mapToInt(pin_info -> pin_info.getName().length()).max().orElse(MIN_PIN_NAME_LENGTH));

				String name_dash = StringUtil.repeat('-', max_length);
				System.out.format(ansi().render("@|bold Header|@: %s%n").toString(), header_pins_entry.getKey());
				System.out.format("+-----+-%s-+--------+----------+--------+-%s-+-----+%n", name_dash, name_dash);
				System.out.format(ansi().render("+ @|bold GP#|@ + @|bold %" + max_length
						+ "s|@ +  @|bold gpiod|@ + @|bold Physical|@ + @|bold gpiod|@  + @|bold %-" + max_length
						+ "s|@ + @|bold GP#|@ +%n").toString(), "Name", "Name");
				System.out.format("+-----+-%s-+--------+----------+--------+-%s-+-----+%n", name_dash, name_dash);

				Map<Integer, PinInfo> pins = header_pins_entry.getValue();
				int index = 0;
				for (PinInfo pin_info : pins.values()) {
					if (index++ % 2 == 0) {
						System.out.format(
								ansi().render("| @|bold,FG_" + getPinColour(pin_info) + " %3s|@ | @|bold,FG_"
										+ getPinColour(pin_info) + " %" + max_length
										+ "s|@ | @|bold %6s|@ | @|bold %2s|@ |").toString(),
								getNotDefined(pin_info.getDeviceNumber()), pin_info.getName(),
								getGpiodName(pin_info.getChip(), pin_info.getLineOffset()),
								getNotDefined(pin_info.getPhysicalPin()));
					} else {
						System.out.format(ansi()
								.render("| @|bold %-2s|@ | @|bold %-6s|@ | @|bold,FG_" + getPinColour(pin_info) + " %-"
										+ max_length + "s|@ | @|bold,FG_" + getPinColour(pin_info) + " %-3s|@ |%n")
								.toString(), getNotDefined(pin_info.getPhysicalPin()),
								getGpiodName(pin_info.getChip(), pin_info.getLineOffset()), pin_info.getName(),
								getNotDefined(pin_info.getDeviceNumber()));
					}
				}
				if (index % 2 == 1) {
					System.out.format("|    |        | %-" + max_length + "s |     |%n", "");
				}

				System.out.format("+-----+-%s-+--------+----------+--------+-%s-+-----+%n", name_dash, name_dash);
				System.out.println();
			}
		} finally {
			Diozero.shutdown();
		}
	}
}

/*-
SBC Name: Odroid C2 (RAM: 2048 bytes)
Pins for header DEFAULT:
+-----+-----------------+--------+----------+--------+-----------------+-----+
+ GP# +            Name +  gpiod + Physical + gpiod  + Name            + GP# +
+-----+-----------------+--------+----------+--------+-----------------+-----+
|     |            3.3V |   :    |  1 || 2  |   :    | 5V              |     |
| 447 |       I2C A SDA |  1:69  |  3 || 4  |   :    | 5V              |     |
| 448 |       I2C A SCK |  1:70  |  5 || 6  |   :    | GND             |     |
| 491 |  J2 Header Pin7 |  1:113 |  7 || 8  |  1:104 | J2 Header Pin8  | 482 |
|     |             GND |   :    |  9 || 10 |  1:105 | J2 Header Pin10 | 483 |
| 489 | J2 Header Pin11 |  1:111 | 11 || 12 |  1:102 | J2 Header Pin12 | 480 |
| 481 | J2 Header Pin13 |  1:103 | 13 || 14 |   :    | GND             |     |
| 479 | J2 Header Pin15 |  1:101 | 15 || 16 |  1:100 | J2 Header Pin16 | 478 |
|     |            3.3V |   :    | 17 || 18 |  1:97  | J2 Header Pin18 | 475 |
| 477 | J2 Header Pin19 |  1:99  | 19 || 20 |   :    | GND             |     |
| 474 | J2 Header Pin21 |  1:96  | 21 || 22 |  1:95  | J2 Header Pin22 | 473 |
| 472 | J2 Header Pin23 |  1:94  | 23 || 24 |  1:93  | J2 Header Pin24 | 471 |
|     |             GND |   :    | 25 || 26 |  1:89  | J2 Header Pin26 | 467 |
| 449 |       I2C B SDA |  1:71  | 27 || 28 |  1:72  | I2C B SCK       | 450 |
| 470 | J2 Header Pin29 |  1:92  | 29 || 30 |   :    | GND             |     |
| 461 | J2 Header Pin31 |  1:83  | 31 || 32 |  1:88  | J2 Header Pin32 | 466 |
| 476 | J2 Header Pin33 |  1:98  | 33 || 34 |   :    | GND             |     |
| 456 | J2 Header Pin35 |  1:78  | 35 || 36 |  1:82  | J2 Header Pin36 | 460 |
|   1 |            AIN1 |   :    | 37 || 38 |   :    | 1.8V            |     |
|     |             GND |   :    | 39 || 40 |   :    | AIN0            | 0   |
+-----+-----------------+--------+----------+--------+-----------------+-----+

I2C buses: [2, 1, 0]
CPU Temperature: 30.00
 */

/*-
SBC Name: RaspberryPi 4B (RAM: 2048 bytes)
Pins for header DEFAULT:
+-----+-----------+--------+----------+--------+-----------+-----+
+ GP# +      Name +  gpiod + Physical + gpiod  + Name      + GP# +
+-----+-----------+--------+----------+--------+-----------+-----+
|     |      3.3V |   :    |  1 || 2  |   :    | 5V        |     |
|   2 |      SDA1 |  0:2   |  3 || 4  |   :    | 5V        |     |
|   3 |      SCL1 |  0:3   |  5 || 6  |   :    | GND       |     |
|   4 | GPIO_GCLK |  0:4   |  7 || 8  |   :    | GPIO14    | 14  |
|     |       GND |   :    |  9 || 10 |   :    | GPIO15    | 15  |
|  17 |    GPIO17 |  0:17  | 11 || 12 |  0:18  | GPIO18    | 18  |
|  27 |    GPIO27 |  0:27  | 13 || 14 |   :    | GND       |     |
|  22 |    GPIO22 |  0:22  | 15 || 16 |  0:23  | GPIO23    | 23  |
|     |      3.3V |   :    | 17 || 18 |  0:24  | GPIO24    | 24  |
|  10 |  SPI_MOSI |  0:10  | 19 || 20 |   :    | GND       |     |
|   9 |  SPI_MISO |  0:9   | 21 || 22 |  0:25  | GPIO25    | 25  |
|  11 |  SPI_SCLK |  0:11  | 23 || 24 |  0:8   | SPI_CE0_N | 8   |
|     |       GND |   :    | 25 || 26 |  0:7   | SPI_CE1_N | 7   |
|     |    ID_SDA |  0:0   | 27 || 28 |  0:1   | ID_SCL    |     |
|   5 |     GPIO5 |  0:5   | 29 || 30 |   :    | GND       |     |
|   6 |     GPIO6 |  0:6   | 31 || 32 |  0:12  | GPIO12    | 12  |
|  13 |    GPIO13 |  0:13  | 33 || 34 |   :    | GND       |     |
|  19 |    GPIO19 |  0:19  | 35 || 36 |  0:16  | GPIO16    | 16  |
|  26 |    GPIO26 |  0:26  | 37 || 38 |  0:20  | GPIO20    | 20  |
|     |       GND |   :    | 39 || 40 |  0:21  | GPIO21    | 21  |
+-----+-----------+--------+----------+--------+-----------+-----+
Pins for header P5:
+-----+----------+--------+----------+--------+----------+-----+
+ GP# +     Name +  gpiod + Physical + gpiod  + Name     + GP# +
+-----+----------+--------+----------+--------+----------+-----+
|  28 |   GPIO28 |   :    |  1 || 2  |   :    | GPIO29   | 29  |
|  30 |   GPIO30 |   :    |  3 || 4  |   :    | GPIO31   | 31  |
+-----+----------+--------+----------+--------+----------+-----+

I2C buses: [1]
CPU Temperature: 45.76
 */
