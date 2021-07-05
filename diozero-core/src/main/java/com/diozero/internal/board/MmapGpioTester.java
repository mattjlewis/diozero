package com.diozero.internal.board;

import org.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.GpioPullUpDown;
import com.diozero.internal.spi.MmapGpioInterface;
import com.diozero.internal.spi.NativeDeviceFactoryInterface;
import com.diozero.sbc.DeviceFactoryHelper;

public class MmapGpioTester {
	public static void main(String[] args) {
		if (args.length < 1) {
			Logger.error("Usage: {} <gpio>", MmapGpioTester.class.getName());
			return;
		}

		int gpio = Integer.parseInt(args[0]);

		try (NativeDeviceFactoryInterface ndf = DeviceFactoryHelper.getNativeDeviceFactory();
				MmapGpioInterface mmap_gpio = ndf.getBoardInfo().createMmapGpio()) {
			if (mmap_gpio == null) {
				Logger.error("Memory mapped GPIO not available");
				return;
			}
			mmap_gpio.initialise();

			DeviceMode start_mode = mmap_gpio.getMode(gpio);
			boolean start_value = mmap_gpio.gpioRead(gpio);
			Logger.info("Initial mode for GPIO {}: {}, value: ", Integer.valueOf(gpio), start_mode,
					Boolean.valueOf(start_value));

			DeviceMode new_mode = DeviceMode.DIGITAL_OUTPUT;
			mmap_gpio.setMode(gpio, new_mode);
			DeviceMode mode = mmap_gpio.getMode(gpio);
			if (mode != new_mode) {
				Logger.error("Failed to set GPIO {} mode to {}, detected mode: {}", Integer.valueOf(gpio), new_mode,
						mode);
				return;
			}

			boolean new_value = true;
			mmap_gpio.gpioWrite(gpio, new_value);
			boolean value = mmap_gpio.gpioRead(gpio);
			if (value != new_value) {
				Logger.error("Failed to set GPIO {} value to {}, detected value: {}", Integer.valueOf(gpio),
						Boolean.valueOf(new_value), Boolean.valueOf(value));
				return;
			}

			new_value = false;
			mmap_gpio.gpioWrite(gpio, new_value);
			value = mmap_gpio.gpioRead(gpio);
			if (value != new_value) {
				Logger.error("Failed to set GPIO {} value to {}, detected value: {}", Integer.valueOf(gpio),
						Boolean.valueOf(new_value), Boolean.valueOf(value));
				return;
			}

			new_mode = DeviceMode.DIGITAL_INPUT;
			mmap_gpio.setMode(gpio, new_mode);
			mode = mmap_gpio.getMode(gpio);
			if (mode != new_mode) {
				Logger.error("Failed to set GPIO {} mode to {}, detected mode: {}", Integer.valueOf(gpio), new_mode,
						mode);
				return;
			}

			GpioPullUpDown new_pud = GpioPullUpDown.PULL_DOWN;
			mmap_gpio.setPullUpDown(gpio, new_pud);
			value = mmap_gpio.gpioRead(gpio);
			// Should be false assuming nothing is connected
			if (value) {
				Logger.error("Failed to set GPIO {} pud to {}", Integer.valueOf(gpio), new_pud);
				return;
			}

			new_pud = GpioPullUpDown.PULL_UP;
			mmap_gpio.setPullUpDown(gpio, new_pud);
			value = mmap_gpio.gpioRead(gpio);
			// Should be true assuming nothing is connected
			if (!value) {
				Logger.error("Failed to set GPIO {} pud to {}", Integer.valueOf(gpio), new_pud);
				return;
			}

			Logger.info("Done - all tests past.");

			mmap_gpio.setMode(gpio, start_mode);
			if (start_mode == DeviceMode.DIGITAL_OUTPUT) {
				mmap_gpio.gpioWrite(gpio, start_value);
			}
		}
	}
}
