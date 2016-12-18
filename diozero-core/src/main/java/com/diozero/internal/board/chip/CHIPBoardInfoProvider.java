package com.diozero.internal.board.chip;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

import com.diozero.internal.spi.GpioDeviceInterface;
import com.diozero.internal.spi.GpioDeviceInterface.Mode;
import com.diozero.util.BoardInfo;
import com.diozero.util.BoardInfoProvider;
import com.diozero.util.RuntimeIOException;

public class CHIPBoardInfoProvider implements BoardInfoProvider {
	public static final String MAKE = "CHIP";
	
	public static final CHIPBoardInfo CHIP_BOARD_INFO = new CHIPBoardInfo();
	
	public static void main(String[] args) {
		int gpio = 0;
		System.out.println("mapped gpio(" + gpio + ") =" + CHIP_BOARD_INFO.mapGpio(gpio));
	}

	@Override
	public BoardInfo lookup(String hardware, String revision) {
		if (hardware != null && hardware.startsWith("Allwinner sun4i/sun5i")) {
			return CHIP_BOARD_INFO;
		}
		return null;
	}

	public static final class CHIPBoardInfo extends BoardInfo {
		private static Map<Integer, List<Mode>> CHIP_PINS;
		static {
			List<Mode> digital_in_out_with_pwm = Arrays.asList(
					GpioDeviceInterface.Mode.DIGITAL_INPUT,
					GpioDeviceInterface.Mode.DIGITAL_OUTPUT,
					GpioDeviceInterface.Mode.SOFTWARE_PWM_OUTPUT,
					GpioDeviceInterface.Mode.PWM_OUTPUT);
			List<Mode> digital_in_out = Arrays.asList(
					GpioDeviceInterface.Mode.DIGITAL_INPUT,
					GpioDeviceInterface.Mode.DIGITAL_OUTPUT,
					GpioDeviceInterface.Mode.SOFTWARE_PWM_OUTPUT);

			CHIP_PINS = new HashMap<>();
			CHIP_PINS.put(Integer.valueOf(0), digital_in_out_with_pwm);
			for (int i=1; i<8; i++) {
				CHIP_PINS.put(Integer.valueOf(i), digital_in_out);
			}
		}
		
		private int gpioOffset = 0;
		
		public CHIPBoardInfo() {
			super(MAKE, "CHIP", 512, CHIP_PINS, MAKE.toLowerCase());
			
			// Determine the GPIO base
			Path gpio_sysfs_dir = FileSystems.getDefault().getPath("/sys/class/gpio");
			try (DirectoryStream<Path> dirs = Files.newDirectoryStream(gpio_sysfs_dir, "gpiochip*")) {
				for (Path p : dirs) {
					try (BufferedReader reader = new BufferedReader(new FileReader(p.resolve("label").toFile()))) {
						if (reader.readLine().equals("pcf8574a")) {
							String dir_name = p.getFileName().toString();
							gpioOffset = Integer.parseInt(dir_name.replace("gpiochip", ""));
							break;
						}
					}
				}
			} catch (IOException e) {
				throw new RuntimeIOException("Error determining GPIO base: " + e, e);
			}
		}
		
		@Override
		public int mapGpio(int gpio) {
			return gpio < 8 ? gpioOffset + gpio : gpio;
		}
	}
}
