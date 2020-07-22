package com.diozero.internal.board.chip;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Core
 * Filename:     CHIPBoardInfoProvider.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 mattjlewis
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


import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.pmw.tinylog.Logger;

import com.diozero.api.PinInfo;
import com.diozero.internal.provider.MmapGpioInterface;
import com.diozero.util.BoardInfo;
import com.diozero.util.BoardInfoProvider;

public class CHIPBoardInfoProvider implements BoardInfoProvider {
	public static final String MAKE = "Next Thing Company";
	public static final String MODEL_CHIP = "CHIP";
	public static final String MODEL_CHIP_PRO = "CHIP Pro";
	private static final String CHIP_LIBRARY_PATH = MODEL_CHIP.toLowerCase();
	
	public static final CHIPBoardInfo CHIP_BOARD_INFO = new CHIPBoardInfo();
	public static final CHIPProBoardInfo CHIP_PRO_BOARD_INFO = new CHIPProBoardInfo();
	
	private static final float ADC_VREF = 1.750f;

	@Override
	public BoardInfo lookup(String hardware, String revision, Integer memoryKb) {
		if (hardware != null && hardware.startsWith("Allwinner sun4i/sun5i")) {
			if (memoryKb != null && memoryKb.intValue() < 500_000) {
				return CHIP_PRO_BOARD_INFO;
			}
			return CHIP_BOARD_INFO;
		}
		return null;
	}

	public static final class CHIPBoardInfo extends BoardInfo {
		public static final String U13_HEADER = "U13";
		public static final String U14_HEADER = "U14";
		
		private static final int MEMORY = 512;
		
		private int xioGpioOffset = 0;
		private boolean xioGpioOffsetLoaded;
		
		public CHIPBoardInfo() {
			super(MAKE, MODEL_CHIP, MEMORY, CHIP_LIBRARY_PATH, ADC_VREF);
		}
		
		@Override
		public void initialisePins() {
			// http://www.chip-community.org/index.php/GPIO_Info#Interrupts
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
			
			// Add other pins
			pin = 1;
			addGeneralPinInfo(U13_HEADER, pin++, PinInfo.GROUND);
			addGeneralPinInfo(U13_HEADER, pin++, "CHG-IN 5V");
			addGeneralPinInfo(U13_HEADER, pin++, PinInfo.VCC_5V);
			addGeneralPinInfo(U13_HEADER, pin++, PinInfo.GROUND);
			addGeneralPinInfo(U13_HEADER, pin++, PinInfo.VCC_3V3);
			addGeneralPinInfo(U13_HEADER, pin++, "TS");
			addGeneralPinInfo(U13_HEADER, pin++, "VCC-1V8");
			addGeneralPinInfo(U13_HEADER, pin++, "Bat");
			pin++;
			addGeneralPinInfo(U13_HEADER, pin++, "PWROn");
			pin++;
			addGeneralPinInfo(U13_HEADER, pin++, PinInfo.GROUND);
			addGeneralPinInfo(U13_HEADER, pin++, "X1");
			addGeneralPinInfo(U13_HEADER, pin++, "X2");
			addGeneralPinInfo(U13_HEADER, pin++, "Y1");
			addGeneralPinInfo(U13_HEADER, pin++, "Y2");
			addGeneralPinInfo(U13_HEADER, 39, PinInfo.GROUND);
			pin = 1;
			addGeneralPinInfo(U14_HEADER, pin++, PinInfo.GROUND);
			addGeneralPinInfo(U14_HEADER, pin++, PinInfo.VCC_5V);
			pin++;
			addGeneralPinInfo(U14_HEADER, pin++, "HPL");
			pin++;
			addGeneralPinInfo(U14_HEADER, pin++, "HPCOM");
			addGeneralPinInfo(U14_HEADER, pin++, "FEL");
			addGeneralPinInfo(U14_HEADER, pin++, "HPR");
			addGeneralPinInfo(U14_HEADER, pin++, PinInfo.VCC_3V3);
			addGeneralPinInfo(U14_HEADER, pin++, "MICM");
			pin++;
			addGeneralPinInfo(U14_HEADER, pin++, "MICINT");
			pin = 21;
			addGeneralPinInfo(U14_HEADER, pin++, PinInfo.GROUND);
			addGeneralPinInfo(U14_HEADER, pin++, PinInfo.GROUND);
			pin = 39;
			addGeneralPinInfo(U14_HEADER, pin++, PinInfo.GROUND);
			addGeneralPinInfo(U14_HEADER, pin++, PinInfo.GROUND);
		}
		
		@Override
		public int mapToSysFsGpioNumber(int gpio) {
			loadXioGpioOffset();
			return gpio < 8 ? xioGpioOffset + gpio : gpio;
		}

		@Override
		public int getPwmChip(int pwmNum) {
			return 0;
		}
		
		@Override
		public MmapGpioInterface createMmapGpio() {
			return new ChipMmapGpio();
		}
		
		private synchronized void loadXioGpioOffset() {
			if (! xioGpioOffsetLoaded) {
				// Determine the XIO GPIO base
				Path gpio_sysfs_dir = Paths.get("/sys/class/gpio");
				// FIXME Treat as a stream
				try (DirectoryStream<Path> dirs = Files.newDirectoryStream(gpio_sysfs_dir, "gpiochip*")) {
					for (Path p : dirs) {
						if (Files.lines(p.resolve("label")).filter(line -> line.equals("pcf8574a")).count() > 0) {
							String dir_name = p.getFileName().toString();
							xioGpioOffset = Integer.parseInt(dir_name.replace("gpiochip", ""));
							break;
						}
						/*
						try (BufferedReader reader = new BufferedReader(new FileReader(p.resolve("label").toFile()))) {
							if (reader.readLine().equals("pcf8574a")) {
								String dir_name = p.getFileName().toString();
								xioGpioOffset = Integer.parseInt(dir_name.replace("gpiochip", ""));
								break;
							}
						}
						*/
					}
					Logger.debug("xioGpioOffset: {}", Integer.valueOf(xioGpioOffset));
				} catch (IOException e) {
					Logger.error(e, "Error determining XIO GPIO base: {}", e);
				}
				
				xioGpioOffsetLoaded = true;
			}
		}
	}

	public static final class CHIPProBoardInfo extends BoardInfo {
		private static final int MEMORY = 256;
		
		public CHIPProBoardInfo() {
			super(MAKE, MODEL_CHIP_PRO, MEMORY, CHIP_LIBRARY_PATH, ADC_VREF);
		}
		
		@Override
		public void initialisePins() {
			// https://docs.getchip.com/chip_pro.html#pin-descriptions

			// Look at the letter that follows the "P", in this case it's "E".
			// Starting with A = 0, count up in the alphabet until you arrive at
			// "E" and that is the letter index. For example, E=4. Multiply the
			// letter index by 32, then add the number that follows "PE":
			// (4*32)+4 = 132
			// A=0, B=1, C=2, D=3, E=4, F=5, G=6
			// PE0(128)/PE1(129)/PE2(130)/PG0(192)/PG1(193)/PG2(194) are for input only.

			// PWM 0 & 1
			int pin = 9;
			addPwmPinInfo(34, "PWM0", pin++, 0, PinInfo.DIGITAL_IN_OUT_PWM);	// PB2
			addPwmPinInfo(205, "PWM1", pin++, 1, PinInfo.DIGITAL_IN_OUT_PWM);	// PG13
			
			// TWI1, UART2
			int gpio = 47;
			addGpioPinInfo(gpio++, "TWI1-SCK", pin++, PinInfo.DIGITAL_IN_OUT);	// PB15
			addGpioPinInfo(gpio++, "TWI1-SDA", pin++, PinInfo.DIGITAL_IN_OUT);	// PB16
			gpio = 98;
			addGpioPinInfo(gpio++, "UART2-TX", pin++, PinInfo.DIGITAL_IN_OUT);	// PD2
			addGpioPinInfo(gpio++, "UART2-RX", pin++, PinInfo.DIGITAL_IN_OUT);	// PD3
			addGpioPinInfo(gpio++, "UART2-CTS", pin++, PinInfo.DIGITAL_IN_OUT);	// PD4
			addGpioPinInfo(gpio++, "UART2-RTS", pin++, PinInfo.DIGITAL_IN_OUT);	// PD5
			
			// I2S
			gpio = 37;
			pin = 21;
			addGpioPinInfo(gpio++, "I2S-MCLK", pin++, PinInfo.DIGITAL_IN_OUT);	// PB5
			addGpioPinInfo(gpio++, "I2S-BCLK", pin++, PinInfo.DIGITAL_IN_OUT);	// PB6
			addGpioPinInfo(gpio++, "I2S-LCLK", pin++, PinInfo.DIGITAL_IN_OUT);	// PB7
			addGpioPinInfo(gpio++, "I2S-DO", pin++, PinInfo.DIGITAL_IN_OUT);	// PB8
			addGpioPinInfo(gpio++, "I2S-DI", pin++, PinInfo.DIGITAL_IN_OUT);	// PB9
			
			// UART1
			gpio = 195;
			pin = 44;
			addGpioPinInfo(gpio++, "UART1-TX", pin--, PinInfo.DIGITAL_IN_OUT);	// PG3
			addGpioPinInfo(gpio++, "UART1-RX", pin--, PinInfo.DIGITAL_IN_OUT);	// PG4
			
			// LRADC
			addAdcPinInfo(0, "LRADC0", pin--);
			
			// SPI2
			gpio = 128;
			// PE0/PE1/PE2 are for input only
			addGpioPinInfo(gpio++, "CSIPCK", pin--, PinInfo.DIGITAL_IN);		// PE0
			addGpioPinInfo(gpio++, "CSIMCLK", pin--, PinInfo.DIGITAL_IN);		// PE1
			addGpioPinInfo(gpio++, "CSIHSYNC", pin--, PinInfo.DIGITAL_IN);		// PE2
			addGpioPinInfo(gpio++, "CSIVSYNC", pin--, PinInfo.DIGITAL_IN_OUT);	// PE3
			
			// CSID0-7
			gpio = 132;
			for (int i=0; i<8; i++) {
				addGpioPinInfo(gpio+i, "CSID" + i, pin--, PinInfo.DIGITAL_IN_OUT);	// PE4-11
			}
		}
	}
}
