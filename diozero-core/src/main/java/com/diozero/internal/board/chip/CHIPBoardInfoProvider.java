package com.diozero.internal.board.chip;

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


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import com.diozero.api.DeviceMode;
import com.diozero.util.BoardInfo;
import com.diozero.util.BoardInfoProvider;
import com.diozero.util.RuntimeIOException;

public class CHIPBoardInfoProvider implements BoardInfoProvider {
	public static final String MAKE = "CHIP";
	
	public static final CHIPBoardInfo CHIP_BOARD_INFO = new CHIPBoardInfo();

	@Override
	public BoardInfo lookup(String hardware, String revision) {
		if (hardware != null && hardware.startsWith("Allwinner sun4i/sun5i")) {
			return CHIP_BOARD_INFO;
		}
		return null;
	}

	public static final class CHIPBoardInfo extends BoardInfo {
		private static Map<Integer, List<DeviceMode>> CHIP_GPIOS;
		static {
			List<DeviceMode> digital_in_out_with_pwm = Arrays.asList(
					DeviceMode.DIGITAL_INPUT,
					DeviceMode.DIGITAL_OUTPUT,
					DeviceMode.SOFTWARE_PWM_OUTPUT,
					DeviceMode.PWM_OUTPUT);
			List<DeviceMode> digital_in_out = Arrays.asList(
					DeviceMode.DIGITAL_INPUT,
					DeviceMode.DIGITAL_OUTPUT,
					DeviceMode.SOFTWARE_PWM_OUTPUT);

			CHIP_GPIOS = new HashMap<>();
			
			// Map the XIO pins. Note these are connected via a PCF8574 GPIO
			// expansion board that does not handle interrupts reliably.
			// Special rule for GPIO0 - maps to PWM0 (U13-18) for hardware PWM
			// or GPIO0 (U14-13, XIO-P0) for digital in/out.
			CHIP_GPIOS.put(Integer.valueOf(0), digital_in_out_with_pwm);
			// Add the rest of the GPIOs (XIO-P1-7)
			for (int i=1; i<8; i++) {
				CHIP_GPIOS.put(Integer.valueOf(i), digital_in_out);
			}
			
			// LCD-D2-D7
			for (int i=98; i<104; i++) {
				CHIP_GPIOS.put(Integer.valueOf(i), digital_in_out);
			}
			
			// LCD-D10-D15
			for (int i=106; i<112; i++) {
				CHIP_GPIOS.put(Integer.valueOf(i), digital_in_out);
			}
			
			// LCD-D18-D23
			for (int i=114; i<120; i++) {
				CHIP_GPIOS.put(Integer.valueOf(i), digital_in_out);
			}
			
			// LCD-CLK, LCD-VSYNC, LCD-HSYNC
			CHIP_GPIOS.put(Integer.valueOf(120), digital_in_out);
			CHIP_GPIOS.put(Integer.valueOf(122), digital_in_out);
			CHIP_GPIOS.put(Integer.valueOf(123), digital_in_out);
			
			// CSID0-7
			for (int i=132; i<140; i++) {
				CHIP_GPIOS.put(Integer.valueOf(i), digital_in_out);
			}
		}
		
		private int xioGpioOffset = 0;
		
		public CHIPBoardInfo() {
			super(MAKE, "CHIP", 512, CHIP_GPIOS, MAKE.toLowerCase());
			
			// Determine the XIO GPIO base
			Path gpio_sysfs_dir = FileSystems.getDefault().getPath("/sys/class/gpio");
			try (DirectoryStream<Path> dirs = Files.newDirectoryStream(gpio_sysfs_dir, "gpiochip*")) {
				for (Path p : dirs) {
					try (BufferedReader reader = new BufferedReader(new FileReader(p.resolve("label").toFile()))) {
						if (reader.readLine().equals("pcf8574a")) {
							String dir_name = p.getFileName().toString();
							xioGpioOffset = Integer.parseInt(dir_name.replace("gpiochip", ""));
							break;
						}
					}
				}
			} catch (IOException e) {
				throw new RuntimeIOException("Error determining XIO GPIO base: " + e, e);
			}
		}
		
		@Override
		public int mapGpio(int gpio) {
			return gpio < 8 ? xioGpioOffset + gpio : gpio;
		}
	}
}
