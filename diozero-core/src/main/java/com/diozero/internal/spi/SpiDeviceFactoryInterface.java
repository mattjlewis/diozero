package com.diozero.internal.spi;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     SpiDeviceFactoryInterface.java
 *
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2023 diozero
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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.tinylog.Logger;

import com.diozero.api.RuntimeIOException;
import com.diozero.api.SpiClockMode;

public interface SpiDeviceFactoryInterface extends DeviceFactoryInterface {
	/**
	 * Many distributions have a maximum SPI transfer of 4096 bytes. This can be changed in /boot/cmdline.txt by appending
	 *  spidev.bufsiz=32768
 	 */
	int DEFAULT_SPI_BUFFER_SIZE = 4096;
	String SPI_PREFIX = "-SPI-";

	default InternalSpiDeviceInterface provisionSpiDevice(int controller, int chipSelect,
			int frequency, SpiClockMode spiClockMode, boolean lsbFirst) throws RuntimeIOException {

		return registerDevice(()->createSpiKey(controller, chipSelect),
					   (k) ->createSpiDevice(k, controller, chipSelect, frequency, spiClockMode, lsbFirst));
	}

	InternalSpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect, int frequency,
			SpiClockMode spiClockMode, boolean lsbFirst) throws RuntimeIOException;

	default int getSpiBufferSize() {
		try(Stream<String> stream = Files.lines(Paths.get("/sys/module/spidev/parameters/bufsiz"))) {
			return stream.mapToInt(Integer::parseInt).findFirst().orElse(DEFAULT_SPI_BUFFER_SIZE);
		} catch (IOException e) {
			Logger.warn("Unable to read kernel SPI buffer size, using default: {}", e);
			return DEFAULT_SPI_BUFFER_SIZE;
		}
	}

	static String createSpiKey(String keyPrefix, int controller, int chipSelect) {
		return keyPrefix + SPI_PREFIX + controller + "-" + chipSelect;
	}
}
