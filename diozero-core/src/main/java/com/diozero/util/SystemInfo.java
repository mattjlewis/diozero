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

import com.diozero.internal.board.UnknownBoardInfo;

/**
 * <p>
 * Utility class for accessing information for the system the application is
 * executing on.
 * </p>
 * <p>
 * Note some boards are accessed remotely (e.g. Firmata protocol and pigpio
 * sockets) hence this information may differ to the actual device you are
 * controlling.
 * </p>
 */
public class SystemInfo implements SystemInfoConstants {
	private static boolean initialised;
	private static Properties osReleaseProperties;
	private static String hardware;
	private static String revision;
	private static String model;
	private static Integer memoryKb;
	private static BoardInfo localBoardInfo;

	/*-
	 * Notes...
	 * Board: TinkerBoard
	 * /etc/os-release PRETTY_NAME:     Armbian 20.08.17 Focal
	 * /proc/device-tree/compatible:    asus,rk3288-tinker^@rockchip,rk3288^@
	 * /proc/device-tree/model:         Rockchip RK3288 Asus Tinker Board^@
	 * /proc/device-tree/serial-number: <No such file>
	 * /proc/cpuinfo:
	 *   Hardware : Rockchip (Device Tree)
	 *   Revision : 0000
	 *   Serial   : 0000000000000000
	 * os.name: Linux
	 * os.arch: arm
	 * sun.arch.data.model: 32
	 *
	 * Board: Raspberry Pi 4 Model B
	 * /etc/os-release PRETTY_NAME:     Raspbian GNU/Linux 10 (buster)
	 * /proc/device-tree/compatible:    raspberrypi,4-model-b^@brcm,bcm2711^@
	 * /proc/device-tree/model:         Raspberry Pi 4 Model B Rev 1.1^@
	 * /proc/device-tree/serial-number: 100000002914db7e^@
	 * /proc/cpuinfo:
	 *   Hardware   : BCM2711
	 *   Revision   : b03111
	 *   Serial     : 100000002914db7e
	 *   Model      : Raspberry Pi 4 Model B Rev 1.1
	 * os.name: Linux
	 * os.arch: arm
	 * sun.arch.data.model: 32
	 * 
	 * Board: Raspberry Pi 3 Model B
	 * /etc/os-release PRETTY_NAME:     Raspbian GNU/Linux 10 (buster)
	 * /proc/device-tree/compatible:	raspberrypi,3-model-b^@brcm,bcm2837^@
	 * /proc/device-tree/model:         Raspberry Pi 3 Model B Rev 1.2^@
	 * /proc/device-tree/serial-number: 00000000c2b16ad3^@
	 * /proc/cpuinfo:
	 *   Hardware	: BCM2835
	 *   Revision	: a02082
	 *   Serial		: 00000000c2b16ad3
	 *   Model		: Raspberry Pi 3 Model B Rev 1.2
	 * os.name: Linux
	 * os.arch: arm
	 * sun.arch.data.model: 32
	 * 
	 * Board: Odroid C2
	 * /etc/os-release PRETTY_NAME:     Armbian 20.08.17 Buster
	 * /proc/device-tree/compatible:    hardkernel,odroid-c2^@amlogic,meson-gxbb^@
	 * /proc/device-tree/model:         Hardkernel ODROID-C2^@
	 * /proc/device-tree/serial-number: HKC213254DFD1F85M-^H^@
	 * /proc/cpuinfo:
	 *   Hardware : <<Not present>>
	 *   Revision : <<Not present>>
	 *   Serial   : <<Not present>>
	 * os.name: Linux
	 * os.arch: aarch64
	 * sun.arch.data.model: 64
	 * 
	 * Board: NanoPi Duo2
	 * /etc/os-release PRETTY_NAME:     Armbian 20.08.17 Buster
	 * /proc/device-tree/compatible:    friendlyarm,nanopi-duo2^@allwinner,sun8i-h3^@
	 * /proc/device-tree/model:         FriendlyElec NanoPi-Duo2^@
	 * /proc/device-tree/serial-number:	02c00081f71a3058^@
	 * /proc/cpuinfo:
	 *   Hardware	: Allwinner sun8i Family
	 *   Revision	: 0000
	 *   Serial		: 02c00081f71a3058
	 * os.name: Linux
	 * os.arch: arm
	 * sun.arch.data.model: 32
	 */
	private static synchronized void initialise() throws RuntimeIOException {
		if (!initialised) {
			String os_name = System.getProperty(OS_NAME_SYSTEM_PROPERTY);
			if (!os_name.startsWith("Windows")) {
				osReleaseProperties = new Properties();
				try (Reader reader = new FileReader(LINUX_OS_RELEASE_FILE)) {
					osReleaseProperties.load(reader);
				} catch (IOException e) {
					Logger.warn("Error loading properties file '{}': {}", LINUX_OS_RELEASE_FILE, e);
				}

				try {
					Files.lines(Paths.get(LINUX_CPUINFO_FILE)).forEach(line -> {
						if (line.startsWith("Hardware")) {
							hardware = line.split(":")[1].trim();
						} else if (line.startsWith("Revision")) {
							revision = line.split(":")[1].trim();
						} else if (line.startsWith("Model")) {
							model = line.split(":")[1].trim();
						}
					});
				} catch (IOException | NullPointerException | IndexOutOfBoundsException e) {
					Logger.warn("Error reading '{}': {}", LINUX_CPUINFO_FILE, e.getMessage());
				}
				if (hardware == null) {
					// arm64 doesn't have Hardware info in /proc/cpuinfo
					try {
						hardware = Files.lines(Paths.get(LINUX_DEVICE_TREE_MODEL_FILE)).findFirst().map(s -> s.trim())
								.orElse(null);
					} catch (IOException e) {
						// Ignore
					}
				}
				if (revision == null) {
					// arm64 doesn't have Revision info in /proc/cpuinfo
					try {
						revision = new String(Files.readAllBytes(Paths.get(LINUX_DEVICE_TREE_SERIAL_NUMBER_FILE))).trim();
					} catch (IOException e) {
						// Ignore
					}
				}
				if (model == null) {
					// arm64 doesn't have Revision info in /proc/cpuinfo
					try {
						model = new String(Files.readAllBytes(Paths.get(LINUX_DEVICE_TREE_MODEL_FILE))).trim();
					} catch (IOException e) {
						// Ignore
					}
				}

				memoryKb = null;
				try {
					Files.lines(Paths.get(LINUX_MEMINFO_FILE)).forEach(line -> {
						if (line.startsWith("MemTotal:")) {
							memoryKb = Integer.valueOf(line.split("\\s+")[1].trim());
						}
					});
				} catch (IOException | NullPointerException | IndexOutOfBoundsException e) {
					Logger.warn("Error reading '{}': {}", LINUX_MEMINFO_FILE, e.getMessage());
				}
			}
			
			localBoardInfo = lookupLocalBoardInfo(model, hardware, revision, memoryKb);
			
			initialised = true;
		}
	}

	/**
	 * Returns information for the local device only. Note some providers work over
	 * a remote connection - if you want information for the device you are
	 * controlling please use:<br>
	 * {@code DeviceFactoryHelper.getNativeDeviceFactory().getBoardInfo()}
	 * 
	 * @return BoardInfo instance describing the local device.
	 */
	public static BoardInfo lookupLocalBoardInfo() {
		initialise();
		
		return localBoardInfo;
	}

	// Package private purely for unit tests
	static BoardInfo lookupLocalBoardInfo(String model, String hardware, String revision, Integer memoryKb) {
		BoardInfo bi = BoardInfoProvider.loadInstances().map(bip -> bip.lookup(hardware, revision, memoryKb))
				.filter(Objects::nonNull).findFirst()
				.orElseGet(() -> UnknownBoardInfo.get(model, hardware, revision, memoryKb));
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
}
