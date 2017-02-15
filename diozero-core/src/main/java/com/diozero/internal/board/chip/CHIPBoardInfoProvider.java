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

import com.diozero.api.PinInfo;
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
		public static final String U13_HEADER = "U13";
		public static final String U14_HEADER = "U14";
		
		private static final int MEMORY = 512;
		
		private int xioGpioOffset = 0;
		
		public CHIPBoardInfo() {
			super(MAKE, "CHIP", MEMORY, MAKE.toLowerCase());
			
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

			// Not all gpio pins support interrupts. Whether a pin supports
			// interrupts can be seen by the presence of an "edge" file (e.g.
			// /sys/class/gpio/<pin>/edge) after exporting the pin. The
			// following pins support interrupts: XIO-P0 thru XIO-P7, AP-EINT1,
			// AP-EINT3, CSIPCK, CSICK.
			// The XIO pin interrupts are for state changes only and may miss
			// edges. The PCF8574 I/O extender only provides an interrupt on pin
			// change and the driver then compares the current state of the
			// inputs with the last seen state to determine which pin changed.
			// This means that it can miss short pulses. In addition, the
			// current driver does not respect the direction of change and
			// instead delivers a signal on every change, meaning that setting
			// the edge detection to "rising" or "falling" acts as if it were
			// set to "both".
			
			// Map the XIO pins. Note these are connected via a PCF8574 GPIO
			// expansion board that does not handle interrupts reliably.
			int gpio = 0;
			int pin = 13;
			for (gpio=0; gpio<8; gpio++) {
				addGpioPinInfo(U14_HEADER, gpio, pin++, PinInfo.DIGITAL_IN_OUT);
			}
			
			// PWM0
			// To enable:
			//sudo fdtput -t s /boot/sun5i-r8-chip.dtb "/soc@01c00000/pwm@01c20e00" "status" "okay"
			//sudo fdtput -t s /boot/sun5i-r8-chip.dtb "/soc@01c00000/pwm@01c20e00" "pinctrl-names" "default"
			//sudo fdtput -t x /boot/sun5i-r8-chip.dtb "/soc@01c00000/pwm@01c20e00" "pinctrl-0" "0x67" # 0x63 for older v4.4
			addPwmPinInfo(U13_HEADER, 34, "PWM0", 18, 0, PinInfo.DIGITAL_IN_OUT_PWM);
			
			// LCD-D2-D7
			gpio = 98;
			addGpioPinInfo(U13_HEADER, gpio++, "LCD-D2", 17, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(U13_HEADER, gpio++, "LCD-D3", 20, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(U13_HEADER, gpio++, "LCD-D4", 19, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(U13_HEADER, gpio++, "LCD-D5", 22, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(U13_HEADER, gpio++, "LCD-D6", 21, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(U13_HEADER, gpio++, "LCD-D7", 24, PinInfo.DIGITAL_IN_OUT);
			
			// LCD-D10-D15
			gpio = 106;
			addGpioPinInfo(U13_HEADER, gpio++, "LCD-D10", 23, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(U13_HEADER, gpio++, "LCD-D11", 26, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(U13_HEADER, gpio++, "LCD-D12", 25, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(U13_HEADER, gpio++, "LCD-D13", 28, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(U13_HEADER, gpio++, "LCD-D14", 27, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(U13_HEADER, gpio++, "LCD-D15", 30, PinInfo.DIGITAL_IN_OUT);
			
			// LCD-D18-D23
			gpio = 114;
			addGpioPinInfo(U13_HEADER, gpio++, "LCD-D18", 29, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(U13_HEADER, gpio++, "LCD-D19", 32, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(U13_HEADER, gpio++, "LCD-D20", 31, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(U13_HEADER, gpio++, "LCD-D21", 34, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(U13_HEADER, gpio++, "LCD-D22", 33, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(U13_HEADER, gpio++, "LCD-D23", 36, PinInfo.DIGITAL_IN_OUT);
			
			// LCD-CLK, LCD-VSYNC, LCD-HSYNC
			pin = 36;
			addGpioPinInfo(U13_HEADER, 120, "LCD-CLK", pin++, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(U13_HEADER, 122, "LCD-VSYNC", pin++, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(U13_HEADER, 123, "LCD-HSYNC", pin++, PinInfo.DIGITAL_IN_OUT);
			
			// UART1
			addGpioPinInfo(U14_HEADER, 195, "UART1-TX", 3, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(U14_HEADER, 196, "UART1-RX", 5, PinInfo.DIGITAL_IN_OUT);
			
			// AP-EINT
			pin = 23;
			addGpioPinInfo(U14_HEADER, 193, "AP-EINT1", pin++, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(U14_HEADER, 35, "AP-EINT3", pin++, PinInfo.DIGITAL_IN_OUT);
			
			// I2C / SPI
			pin = 25;
			addGpioPinInfo(U14_HEADER, 50, "TWI2-SDA", pin++, PinInfo.DIGITAL_IN_OUT);	// I2C2-SDA
			addGpioPinInfo(U14_HEADER, 49, "TWI2-SCK", pin++, PinInfo.DIGITAL_IN_OUT);	// I2C2-SCL
			addGpioPinInfo(U14_HEADER, 128, "CSIPCK", pin++, PinInfo.DIGITAL_IN_OUT);	// SPI-CS0
			addGpioPinInfo(U14_HEADER, 129, "CSICK", pin++, PinInfo.DIGITAL_IN_OUT);	// SPI-CLK
			addGpioPinInfo(U14_HEADER, 130, "CSIHSYNC", pin++, PinInfo.DIGITAL_IN_OUT);	// SPI-MOSI
			addGpioPinInfo(U14_HEADER, 131, "CSIVSYNC", pin++, PinInfo.DIGITAL_IN_OUT);	// SPI-MISO
			
			// CSID0-7
			gpio = 132;
			for (int csid=0; csid<8; csid++) {
				addGpioPinInfo(U14_HEADER, gpio+csid, "CSID"+csid, 31+csid, PinInfo.DIGITAL_IN_OUT);
			}
			
			// LRADC
			addAdcPinInfo(U14_HEADER, 0, "LRADC", 11);
		}
		
		@Override
		public int mapToSysFsGpioNumber(int gpio) {
			return gpio < 8 ? xioGpioOffset + gpio : gpio;
		}

		@Override
		public int getPwmChip(int pwmNum) {
			return 0;
		}
	}
}
