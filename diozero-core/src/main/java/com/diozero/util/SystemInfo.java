package com.diozero.util;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 mattjlewis
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
import java.io.*;
import java.util.Properties;
import java.util.ServiceLoader;

import org.pmw.tinylog.Logger;

import com.diozero.api.PinInfo;

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
				Logger.warn(e, "Error loading properties file '" + OS_RELEASE_FILE + "': " + e);
			}
			
			ProcessBuilder pb = new ProcessBuilder("cat", CPUINFO_FILE);
			try {
				Process proc = pb.start();
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
					String line;
					// Fully read the process output
					do {
						line = reader.readLine();
						if (line != null && line.startsWith("Hardware")) {
							hardware = line.split(":")[1].trim();
						}
						if (line != null && line.startsWith("Revision")) {
							revision = line.split(":")[1].trim();
						}
					} while (line != null);
				}
			} catch (IOException | NullPointerException | IndexOutOfBoundsException e) {
				Logger.error(e, "Error reading {}: ", CPUINFO_FILE, e.getMessage());
			}

			pb = new ProcessBuilder("cat", MEMINFO_FILE);
			memoryKb = null;
			try {
				Process proc = pb.start();
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
					String line;
					// Fully read the process output
					do {
						line = reader.readLine();
						if (line != null && line.startsWith("MemTotal")) {
							memoryKb = new Integer(line.split("\\s+")[1].trim());
						}
					} while (line != null);
				}
			} catch (IOException | NullPointerException | IndexOutOfBoundsException e) {
				Logger.error(e, "Error reading {}: {}", MEMINFO_FILE, e.getMessage());
			}
			
			initialised = true;
		}
	}
	
	public static BoardInfo lookupBoardInfo() {
		return lookupBoardInfo(hardware, revision, memoryKb);
	}
	
	public static BoardInfo lookupBoardInfo(String hardware, String revision, Integer memoryKb) {
		BoardInfo board_info = null;
		ServiceLoader<BoardInfoProvider> service_loader = ServiceLoader.load(BoardInfoProvider.class);
		for (BoardInfoProvider board_info_provider : service_loader) {
			board_info = board_info_provider.lookup(hardware, revision, memoryKb);
			if (board_info != null) {
				break;
			}
		}
		if (board_info == null) {
			Logger.warn("Failed to resolve board info for hardware '{}' and revision '{}' {}", hardware, revision, System.getProperty("os.name"));
			board_info = new UnknownBoardInfo();
		}
		return board_info;
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
		Logger.info(lookupBoardInfo());
	}
	
	public static final class UnknownBoardInfo extends BoardInfo {
		private static final String UNKNOWN = "unknown";
		
		public UnknownBoardInfo() {
			super(UNKNOWN, UNKNOWN, -1, UNKNOWN);
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
