package com.diozero.sbc;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     LocalSystemInfo.java
 *
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2021 diozero
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.tinylog.Logger;

import com.diozero.util.StringUtil;

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
 * Board: Raspberry Pi Model B Rev 2
 * /proc/device-tree/compatible:    raspberrypi,model-b^@brcm,bcm2835^
 * /proc/device-tree/model:         ???
 * /proc/device-tree/serial-number: ???
 * /proc/cpuinfo:
 *   Hardware   : BCM2835
 *   Revision   : 000f
 *   Serial     : ???
 *   Model      : ???
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
 *   model name : ARMv7 Processor rev 3 (v7l)
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
 * /proc/device-tree/compatible:    raspberrypi,3-model-b^@brcm,bcm2837^@
 * /proc/device-tree/model:         Raspberry Pi 3 Model B Rev 1.2^@
 * /proc/device-tree/serial-number: 00000000c2b16ad3^@
 * /proc/cpuinfo:
 *   Hardware   : BCM2835
 *   Revision   : a02082
 *   Serial     : 00000000c2b16ad3
 *   Model      : Raspberry Pi 3 Model B Rev 1.2
 * os.name: Linux
 * os.arch: arm
 * sun.arch.data.model: 32
 *
 * Board: Raspberry Pi CM4
 * /etc/os-release PRETTY_NAME:     Raspbian GNU/Linux 10 (buster)
 * /proc/device-tree/compatible:    raspberrypi,4-compute-module^@brcm,bcm2711^@
 * /proc/device-tree/model:         Raspberry Pi Compute Module 4 Rev 1.0^@
 * /proc/device-tree/serial-number: 10000000b68ef68d^@
 * /proc/cpuinfo:
 *   Hardware   : BCM2835
 *   Revision   : a03140
 *   Serial     : 10000000b68ef68d
 *   Model      : Raspberry Pi Compute Module 4 Rev 1.0
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
 * /proc/device-tree/serial-number: 02c00081f71a3058^@
 * /proc/cpuinfo:
 *   Hardware   : Allwinner sun8i Family
 *   Revision   : 0000
 *   Serial     : 02c00081f71a3058
 * os.name: Linux
 * os.arch: arm
 * sun.arch.data.model: 32
 *
 * Board: BeagleBone Green / Black
 * /etc/os-release PRETTY_NAME: ???
 * /proc//device-tree/compatible:   ti,am335x-bone-green^@ti,am335x-bone-black^@ti,am335x-bone^@ti,am33xx^@
 * /proc/device-tree/model:         TI AM335x BeagleBone Green^@
 * /proc/device-tree/model:         TI AM335x BeagleBone Black^@
 * /proc/device-tree/serial-number: ??
 * /proc/cpuinfo:
 *   Hardware   : Generic AM33XX (Flattened Device Tree)
 *   Revision   : 0000
 *   Serial     : BBG217012434
 * os.name: Linux
 * os.arch: arm
 * sun.arch.data.model: 32
 */
/**
 * Utility class for accessing information about the local system. The majority
 * of information is specific to the Linux operating system.
 */
public class LocalSystemInfo {
	private static final String OS_NAME_SYSTEM_PROPERTY = "os.name";
	private static final String OS_ARCH_SYSTEM_PROPERTY = "os.arch";
	private static final String ARM_32_OS_ARCH = "arm";
	private static final String ARM_64_OS_ARCH = "aarch64";
	private static final String ARMV6_CPU_MODEL_NAME = "armv6";
	private static final String ARMV7_CPU_MODEL_NAME = "armv7";
	private static final String LINUX_OS_NAME = "Linux";
	private static final String WINDOWS_OS_NAME_PREFIX = "Windows";

	// Linux system files used for discovery
	private static final String LINUX_OS_RELEASE_FILE = "/etc/os-release";
	private static final String LINUX_CPUINFO_FILE = "/proc/cpuinfo";
	private static final String LINUX_MEMINFO_FILE = "/proc/meminfo";
	private static final String LINUX_DEVICE_TREE_COMPATIBLE_FILE = "/proc/device-tree/compatible";
	private static final String LINUX_DEVICE_TREE_MODEL_FILE = "/proc/device-tree/model";
	private static final String LINUX_DEVICE_TREE_SERIAL_NUMBER_FILE = "/proc/device-tree/serial-number";
	private static final String TEMPERATURE_FILE = "/sys/class/thermal/thermal_zone0/temp";

	private static LocalSystemInfo instance;

	private String osName;
	private String osArch;
	private String libFileExtension;
	private Properties linuxOsReleaseProperties;
	private String hardware;
	private String revision;
	private String model;
	private String cpuModelName;
	private Integer memoryKb;

	public synchronized static LocalSystemInfo getInstance() {
		if (instance == null) {
			instance = new LocalSystemInfo();
		}
		return instance;
	}

	// For unit testing purposes only
	LocalSystemInfo(String hardware, String revision, String model) {
		this();

		this.hardware = hardware;
		this.revision = revision;
		this.model = model;
	}

	private LocalSystemInfo() {
		osName = System.getProperty(OS_NAME_SYSTEM_PROPERTY);
		osArch = System.getProperty(OS_ARCH_SYSTEM_PROPERTY);
		libFileExtension = isWindows() ? ".dll" : ".so";

		linuxOsReleaseProperties = new Properties();
		if (isLinux()) {
			try (Reader reader = new FileReader(LINUX_OS_RELEASE_FILE)) {
				linuxOsReleaseProperties.load(reader);
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
					} else if (line.startsWith("model name")) {
						// FIXME Ugly and possibly unsafe code!
						try {
							// model name : ARMv7 Processor rev 3 (v7l)
							cpuModelName = line.split(":")[1].trim().split(" ")[0].trim().toLowerCase();
						} catch (Exception e) {
							Logger.debug(e, "Error processing model name line '{}': {}", line, e);
						}
					}
				});
			} catch (IOException | NullPointerException | IndexOutOfBoundsException e) {
				Logger.warn("Error reading '{}': {}", LINUX_CPUINFO_FILE, e.getMessage());
			}

			if (model == null) {
				// aarch64 doesn't have Revision info in /proc/cpuinfo
				// Load from /proc/device-tree/model instead
				try {
					model = Files.lines(Paths.get(LINUX_DEVICE_TREE_MODEL_FILE)).findFirst().map(s -> s.trim())
							.orElse(null);
				} catch (IOException e) {
					// Ignore
				}
			}

			if (hardware == null) {
				// aarch64 doesn't have Hardware info in /proc/cpuinfo
				// Load from /proc/device-tree/model instead
				hardware = model;
			}

			if (revision == null) {
				// aarch64 doesn't have Revision info in /proc/cpuinfo
				// Load from /proc/device-tree/serial-number instead
				try {
					revision = new String(Files.readAllBytes(Paths.get(LINUX_DEVICE_TREE_SERIAL_NUMBER_FILE))).trim();
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
	}

	public String getOsName() {
		return osName;
	}

	public boolean isLinux() {
		return osName.equals(LINUX_OS_NAME);
	}

	public boolean isWindows() {
		return osName.startsWith(WINDOWS_OS_NAME_PREFIX);
	}

	public String getOsArch() {
		return osArch;
	}

	public boolean isArm32() {
		return osArch.equals(ARM_32_OS_ARCH);
	}

	public boolean isArm64() {
		return osArch.equals(ARM_64_OS_ARCH);
	}

	public boolean isArmv6() {
		return cpuModelName != null && cpuModelName.equals(ARMV6_CPU_MODEL_NAME);
	}

	public boolean isArmv7() {
		return cpuModelName != null && cpuModelName.equals(ARMV7_CPU_MODEL_NAME);
	}

	public boolean isArm() {
		return isArm32() || isArm64();
	}

	public String getLibFileExtension() {
		return libFileExtension;
	}

	public String getHardware() {
		return hardware;
	}

	public String getRevision() {
		return revision;
	}

	public String getModel() {
		return model;
	}

	public String getCpuModelName() {
		return cpuModelName;
	}

	public Integer getMemoryKb() {
		return memoryKb;
	}

	public String getDefaultLibraryPath() {
		String lib_path;
		if (isArm32() && !StringUtil.isNullOrBlank(cpuModelName)) {
			lib_path = osName.toLowerCase().replace(" ", "") + "-" + cpuModelName.toLowerCase();
		} else {
			lib_path = osName.toLowerCase().replace(" ", "") + "-" + osArch.toLowerCase();
		}
		return lib_path;
	}

	public List<String> loadLinuxBoardCompatibility() {
		List<String> compatible = new ArrayList<>();
		try {
			// This file has multiple strings separated by \0
			byte[] bytes = Files.readAllBytes(Paths.get(LINUX_DEVICE_TREE_COMPATIBLE_FILE));
			int string_start = 0;
			for (int i = 0; i < bytes.length; i++) {
				if (bytes[i] == 0 || i == bytes.length - 1) {
					compatible.add(new String(bytes, string_start, i - string_start));
					string_start = i + 1;
				}
			}
		} catch (IOException e) {
			if (isLinux() && isArm()) {
				// This file should exist on a Linux ARM system
				Logger.warn(e, "Unable to read file {}: {}", LINUX_DEVICE_TREE_COMPATIBLE_FILE, e.getMessage());
			}
		}
		return compatible;
	}

	/**
	 * Get a property from the operating system release file
	 * <code>/etc/os-release</code>
	 *
	 * @param property the property to get
	 * @return property value
	 */
	public String getLinuxOsReleaseProperty(String property) {
		return linuxOsReleaseProperties.getProperty(property);
	}

	/**
	 * Get the local operating system id as defined by the ID property in
	 * <code>/etc/os-release</code>
	 *
	 * @return value of the ID property
	 */
	public String getLinuxOperatingSystemId() {
		return linuxOsReleaseProperties.getProperty("ID");
	}

	/**
	 * Get the local operating system version as defined by the VERSION property in
	 * <code>/etc/os-release</code>
	 *
	 * @return value of the VERSION property
	 */
	public String getLinuxOperatingSystemVersion() {
		return linuxOsReleaseProperties.getProperty("VERSION");
	}

	/**
	 * Get the local operating system version id as defined by the VERSION_ID
	 * property in <code>/etc/os-release</code>
	 *
	 * @return value of the VERSION_ID property
	 */
	public String getLinuxOperatingSystemVersionId() {
		return linuxOsReleaseProperties.getProperty("VERSION_ID");
	}

	public static Collection<Integer> getI2CBusNumbers() {
		try {
			List<Integer> i2c_buses = new ArrayList<>();
			Files.newDirectoryStream(Paths.get("/dev"), "i2c-*")
					.forEach(path -> i2c_buses.add(Integer.valueOf(path.toString().split("-")[1])));
			return i2c_buses;
		} catch (IOException e) {
			Logger.error(e, "Error: {}", e);
			return null;
		}
	}

	/**
	 * Utility method to get the CPU temperate of the attached board
	 *
	 * @return the CPU temperature
	 */
	public static float getCpuTemperature() {
		try {
			return Integer.parseInt(Files.lines(Paths.get(TEMPERATURE_FILE)).findFirst().orElse("0")) / 1000f;
		} catch (IOException e) {
			Logger.warn("Error reading {}: {}", TEMPERATURE_FILE, e);
			return -1;
		}
	}

	public static void main(String[] args) {
		System.out.println("System properties:");
		System.getProperties().forEach((key, value) -> System.out.println(key + ": " + value));
	}
}
