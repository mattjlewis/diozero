package com.diozero.internal.provider.builtin.gpio;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     SysFsGpioUtil.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2022 diozero
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

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.diozero.api.DeviceMode;
import com.diozero.api.RuntimeIOException;

public class SysFsGpioUtil {
	private static final String GPIO_ROOT_DIR = "/sys/class/gpio";
	private static final String EXPORT_FILE = "export";
	private static final String UNEXPORT_FILE = "unexport";
	private static final String GPIO_DIR_PREFIX = "gpio";
	private static final String DIRECTION_FILE = "direction";
	
	private static Path rootPath = Paths.get(GPIO_ROOT_DIR);
	
	public static void export(int gpio, DeviceMode mode) {
		if (!isExported(gpio)) {
			try (Writer export_writer = new FileWriter(rootPath.resolve(EXPORT_FILE).toFile())) {
				export_writer.write(String.valueOf(gpio));
			} catch (IOException e) {
				throw new RuntimeIOException(e);
			}
		}

		Path direction_file = getGpioDirectoryPath(gpio).resolve(DIRECTION_FILE);
		// TODO Is this polling actually required?
		// Wait up to 500ms for the gpioxxx/direction file to exist
		int delay = 500;
		long start = System.currentTimeMillis();
		while (!Files.isWritable(direction_file)) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException ie) {
				// Ignore
			}
			if (System.currentTimeMillis() - start > delay) {
				unexport(gpio);
				throw new RuntimeIOException(
						"Waited for over " + delay + " ms for the GPIO pin to be created, aborting");
			}
		}

		// Defaults to "in" on all boards I have encountered
		try (FileWriter writer = new FileWriter(direction_file.toFile(), true)) {
			writer.write(mode == DeviceMode.DIGITAL_OUTPUT ? "out" : "in");
		} catch (IOException e) {
			unexport(gpio);
			throw new RuntimeIOException("Error setting direction for GPIO " + gpio, e);
		}
	}

	public static Path getGpioDirectoryPath(int gpio) {
		return rootPath.resolve(GPIO_DIR_PREFIX + gpio);
	}

	public static void unexport(int gpio) {
		if (isExported(gpio)) {
			try (Writer unexport_writer = new FileWriter(rootPath.resolve(UNEXPORT_FILE).toFile())) {
				unexport_writer.write(String.valueOf(gpio));
			} catch (IOException e) {
				// Issue #27, BBB throws an IOException: Invalid argument when closing GPIOs if
				// you have the universal cape enabled, https://github.com/fivdi/onoff/issues/50
				if (!e.getMessage().equalsIgnoreCase("Invalid argument")) {
					throw new RuntimeIOException(e);
				}
			}
		}
	}

	/**
	 * Check if this pin is exported by checking the existance of
	 * /sys/class/gpio/gpioxxx/
	 * 
	 * @param gpio GPIO pin
	 * @return Returns true if this pin is currently exported
	 */
	public static boolean isExported(int gpio) {
		return Files.isDirectory(getGpioDirectoryPath(gpio));
	}
}
