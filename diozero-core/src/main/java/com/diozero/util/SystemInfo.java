package com.diozero.util;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     SystemInfo.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
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


import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;

import org.tinylog.Logger;

import com.diozero.api.PinInfo;

/**
 * <p>Utility class for accessing information for the system the application is
 * executing on.</p>
 * <p>Note some boards are accessed remotely (e.g. Firmata protocol and pigpio sockets)
 * hence this information may differ to the actual device you are controlling.</p>
 */
public class SystemInfo {
	private static final String OS_RELEASE_FILE = "/etc/os-release";
	private static final String CPUINFO_FILE = "/proc/cpuinfo";
	private static final String MEMINFO_FILE = "/proc/meminfo";
	
	private static boolean initialised;
	private static Properties osReleaseProperties;
	private static String hardware;
	private static String revision;
	private static Integer memoryKb;
	
	private static synchronized void initialise() throws RuntimeIOException {
		if (! initialised) {
			osReleaseProperties = new Properties();
			try (Reader reader = new FileReader(OS_RELEASE_FILE)) {
				osReleaseProperties.load(reader);
			} catch (IOException e) {
				Logger.warn("Error loading properties file '{}': {}", OS_RELEASE_FILE, e);
			}
			
			try {
				Files.lines(Paths.get(CPUINFO_FILE)).forEach(line -> {
					if (line.startsWith("Hardware")) {
						hardware = line.split(":")[1].trim();
					} else if (line.startsWith("Revision")) {
						revision = line.split(":")[1].trim();
					}
				});
			} catch (IOException | NullPointerException | IndexOutOfBoundsException e) {
				Logger.warn("Error reading '{}': {}", CPUINFO_FILE, e.getMessage());
			}

			memoryKb = null;
			try {
				Files.lines(Paths.get(MEMINFO_FILE)).forEach(line -> {
					if (line.startsWith("MemTotal:")) {
						memoryKb = Integer.valueOf(line.split("\\s+")[1].trim());
					}
				});
			} catch (IOException | NullPointerException | IndexOutOfBoundsException e) {
				Logger.warn("Error reading '{}': {}", MEMINFO_FILE, e.getMessage());
			}

			initialised = true;
		}
	}
	
	/**
	 * Returns information for the local device only. Note some providers work
	 * over a remote connection - if you want information for the device you are
	 * controlling please use:<br>
	 * {@code DeviceFactoryHelper.getNativeDeviceFactory().getBoardInfo()}
	 * 
	 * @return BoardInfo instance describing the local device.
	 */
	public static BoardInfo lookupLocalBoardInfo() {
		initialise();
		return lookupLocalBoardInfo(hardware, revision, memoryKb);
	}
	
	static BoardInfo lookupLocalBoardInfo(String hardware, String revision, Integer memoryKb) {
		BoardInfo bi = BoardInfoProvider.loadInstances().map(bip -> bip.lookup(hardware, revision, memoryKb))
				.filter(Objects::nonNull).findFirst()
				.orElseGet(() -> UnknownBoardInfo.get(hardware, revision, memoryKb));
		bi.initialisePins();
		
		return bi;
	}

	public static String getOsReleaseProperty(String property) {
		initialise();
		
		return osReleaseProperties.getProperty(property);
	}

	public static String getOperatingSystemId() {
		initialise();
		
		return osReleaseProperties.getProperty("ID");
	}

	public static String getOperatingSystemVersion() {
		initialise();
		
		return osReleaseProperties.getProperty("VERSION");
	}

	public static String getOperatingSystemVersionId() {
		initialise();
		
		return osReleaseProperties.getProperty("VERSION_ID");
	}
	
	public static void main(String[] args) {
		initialise();
		Logger.info(osReleaseProperties);
		Logger.info(lookupLocalBoardInfo());
	}
	
	public static final class UnknownBoardInfo extends BoardInfo {
		private static final String UNKNOWN = "unknown";
		
		static BoardInfo get(String hardware, String revision, Integer memoryKb) {
			Logger.warn("Failed to resolve board info for hardware '{}' and revision '{}'. Local O/S: {}",
					hardware, revision, System.getProperty("os.name"));
			return new UnknownBoardInfo();
		}
		
		public UnknownBoardInfo() {
			super(UNKNOWN, UNKNOWN, -1, System.getProperty("os.name").replace(" ", "").toLowerCase());
		}
		
		@Override
		public void initialisePins() {
		}
		
		@Override
		public PinInfo getByGpioNumber(int gpio) {
			PinInfo pin_info = super.getByGpioNumber(gpio);
			if (pin_info == null) {
				pin_info = addGpioPinInfo(gpio, gpio, PinInfo.DIGITAL_IN_OUT);
			}
			return pin_info;
		}
		
		@Override
		public PinInfo getByAdcNumber(int adcNumber) {
			PinInfo pin_info = super.getByAdcNumber(adcNumber);
			if (pin_info == null) {
				pin_info = addAdcPinInfo(adcNumber, adcNumber);
			}
			return pin_info;
		}
		
		@Override
		public PinInfo getByDacNumber(int dacNumber) {
			PinInfo pin_info = super.getByDacNumber(dacNumber);
			if (pin_info == null) {
				pin_info = addDacPinInfo(dacNumber, dacNumber);
			}
			return pin_info;
		}
	}
}
