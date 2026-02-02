package com.diozero.sbc;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     LocalSystemInfo.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2024 diozero
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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.tinylog.Logger;

import com.diozero.internal.provider.builtin.i2c.NativeI2C;
import com.diozero.util.FileNative;
import com.diozero.util.StringUtil;
import com.diozero.util.Version;

/**
 * Utility class for accessing information about the local system. The majority of
 * information is specific to the Linux operating system.
 */
public class LocalSystemInfo {
	private static final String LSCPU_ARCHITECTURE = "Architecture";
	private static final String LSCPU_MODEL_NAME = "Model name";

	private static final String OS_NAME_SYSTEM_PROPERTY = "os.name";
	private static final String OS_ARCH_SYSTEM_PROPERTY = "os.arch";
	private static final String ARM_32_OS_ARCH = "arm";
	private static final String ARM_64_OS_ARCH = "aarch64";
	private static final String ARMV6_CPU_ARCHITECTURE = "armv6";
	private static final String ARMV7_CPU_ARCHITECTURE = "armv7";
	private static final String LINUX_OS_NAME = "Linux";
	private static final String MAC_OS_NAME = "Mac OS X";
	private static final String WINDOWS_OS_NAME_PREFIX = "Windows";

	// Linux system files used for discovery
	private static final String LINUX_OS_RELEASE_FILE = "/etc/os-release";
	private static final String LINUX_CPUINFO_FILE = "/proc/cpuinfo";
	private static final String LINUX_MEMINFO_FILE = "/proc/meminfo";
	private static final String LINUX_VERSION_FILE = "/proc/version";
	private static final String LINUX_DEVICE_TREE_COMPATIBLE_FILE = "/proc/device-tree/compatible";
	private static final String LINUX_DEVICE_TREE_MODEL_FILE = "/proc/device-tree/model";
	private static final String LINUX_DEVICE_TREE_SERIAL_NUMBER_FILE = "/proc/device-tree/serial-number";
	private static final String TEMPERATURE_FILE = "/sys/class/thermal/thermal_zone0/temp";

	private static LocalSystemInfo instance;

	private String osName;
	private String osArch;
	private String libFileExtension;
	private String osId;
	private String osVersion;
	private String make;
	private String model;
	private String soc;
	private String revision;
	private String serial;
	private String cpuArchitecture;
	private String cpuModelName;
	private Integer memoryKb;
	private Version kernelVersion;
	private List<String> compatible;

	public synchronized static LocalSystemInfo getInstance() {
		if (instance == null) {
			instance = new LocalSystemInfo();
		}
		return instance;
	}

	// For unit testing purposes only
	LocalSystemInfo(String osName, String osArch, String make, String model, String soc, String revision, int memoryKb,
			List<String> compatible) {
		this.osName = osName;
		this.osArch = osArch;
		this.make = make;
		this.model = model;
		this.soc = soc;
		this.revision = revision;
		this.memoryKb = Integer.valueOf(memoryKb);
		this.compatible = compatible;
	}

	private LocalSystemInfo() {
		osName = System.getProperty(OS_NAME_SYSTEM_PROPERTY);
		osArch = System.getProperty(OS_ARCH_SYSTEM_PROPERTY);
		libFileExtension = isWindows() ? "dll" : isMacOS() ? "dylib" : "so";
		compatible = Collections.emptyList();

		if (isLinux()) {
			populateFromOsRelease();
			populateFromLsCpu();
			populateFromDeviceTreeModel();
			populateFromDeviceTreeCompatible();
			populateFromDeviceTreeSerialNumber();
			populateFromMemInfo();
			populateFromVersion();
			// Note aarch64 doesn't have Board Model / Hardware / Revision info in
			// /proc/cpuinfo
			populateFromCpuInfo();
		} else if (isMacOS()) {
			try (InputStream is = Runtime.getRuntime().exec("sw_vers").getInputStream()) {
				Properties props = new Properties();
				props.load(is);
				osId = props.getProperty("ProductName");
				osVersion = props.getProperty("ProductVersion") + "-" + props.getProperty("BuildVersion");
			} catch (IOException e) {
				// Ignore
				Logger.warn(e, "Error getting versions: {}", e);
			}
		}
	}

	private void populateFromDeviceTreeSerialNumber() {
		try {
			serial = new String(Files.readAllBytes(Paths.get(LINUX_DEVICE_TREE_SERIAL_NUMBER_FILE))).trim();
		} catch (IOException e) {
			// Ignore
		}
	}

	private void populateFromOsRelease() {
		try (Reader reader = new FileReader(LINUX_OS_RELEASE_FILE)) {
			Properties props = new Properties();
			props.load(reader);
			osId = props.getProperty("ID");
			osVersion = StringUtil.unquote(props.getProperty("VERSION"));
		} catch (IOException e) {
			Logger.warn("Error loading properties file '{}': {}", LINUX_OS_RELEASE_FILE, e);
		}
	}

	private void populateFromLsCpu() {
		try {
			final List<ProcessBuilder> pbs = Arrays.asList(new ProcessBuilder("lscpu"),
					new ProcessBuilder("egrep", "-i", "(?:" + LSCPU_ARCHITECTURE + "|" + LSCPU_MODEL_NAME + "):"));
			final List<Process> processes = ProcessBuilder.startPipeline(pbs);
			final Process proc = processes.get(processes.size() - 1);
			try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
				br.lines().map(line -> line.split(":")).forEach(this::processLsCpuEntry);
			}
		} catch (Exception e) {
			Logger.warn("Error with lscpu command: {}", e.getMessage());
			populateCpuArchitectureFromUname();
		}
	}

	private void processLsCpuEntry(String[] parts) {
		if (parts.length != 2) {
			// XXX Assume CPU Architecture and Model name never contain the ':' character
			Logger.warn("Invalid lscpu entry: '" + Arrays.toString(parts) + "'");
			return;
		}

		if (parts[0].equalsIgnoreCase(LSCPU_ARCHITECTURE)) {
			cpuArchitecture = parts[1].trim();
		} else if (parts[0].equalsIgnoreCase(LSCPU_MODEL_NAME)) {
			cpuModelName = parts[1].trim();
		}
	}

	private void populateCpuArchitectureFromUname() {
		try {
			final Process proc = new ProcessBuilder("uname", "-m").start();
			try (BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
				cpuArchitecture = br.readLine();
			}
		} catch (Exception e) {
			Logger.warn("Error with 'uname -m' command: {}", e.getMessage());
		}
	}

	private void populateFromDeviceTreeModel() {
		try {
			model = Files.lines(Paths.get(LINUX_DEVICE_TREE_MODEL_FILE)).findFirst().map(s -> s.trim()).orElse(null);
		} catch (Exception e) {
			Logger.warn("Unable to process file '{}': {}", LINUX_DEVICE_TREE_MODEL_FILE, e);
		}
	}

	private void populateFromDeviceTreeCompatible() {
		try {
			// This file has multiple strings separated by \0
			compatible = Files.lines(Paths.get(LINUX_DEVICE_TREE_COMPATIBLE_FILE)).findFirst()
					.map(line -> Arrays.asList(line.split("\0"))).orElse(Collections.emptyList());
			// The first part is the make and model, e.g. "raspberrypi,model-b",
			// "xunlong,orangepi-one-plus"
			make = compatible.get(0).split(",")[0];
			// The second part is the SoC, e.g. "allwinner,sun50i-h6", "rockchip,rk3399"
			soc = compatible.get(compatible.size() - 1);
		} catch (Exception e) {
			Logger.warn(e, "Unable to process file '{}': {}", LINUX_DEVICE_TREE_COMPATIBLE_FILE, e.getMessage());
		}
	}

	private void populateFromCpuInfo() {
		try {
			Files.lines(Paths.get(LINUX_CPUINFO_FILE)).forEach(line -> {
				if (revision == null && line.startsWith("Revision")) {
					revision = line.split(":")[1].trim();
				} else if (serial == null && line.startsWith("Serial")) {
					serial = line.split(":")[1].trim();
				} else if (model == null && line.matches("Model\\w*:")) {
					model = line.split(":")[1].trim();
				} else if (cpuArchitecture == null && line.startsWith("model name")) {
					// FIXME Ugly and possibly unsafe code!
					// Only populated on certain O/S / SBC combinations
					try {
						// model name : ARMv7 Processor rev 3 (v7l)
						// model name : ARMv6-compatible processor rev 7 (v6l)
						cpuArchitecture = line.split(":")[1].trim().split("[- ]")[0].trim().toLowerCase();
					} catch (Exception e) {
						Logger.debug(e, "Error processing model name line '{}': {}", line, e);
					}
				}
			});
		} catch (Exception e) {
			Logger.warn("Unable to process file '{}': {}", LINUX_CPUINFO_FILE, e.getMessage());
		}
	}

	private void populateFromMemInfo() {
		memoryKb = null;
		try {
			memoryKb = Files.lines(Paths.get(LINUX_MEMINFO_FILE)).filter(line -> line.startsWith("MemTotal:"))
					.findFirst().map(line -> Integer.valueOf(line.split("\\s+")[1].trim())).orElse(null);
		} catch (IOException | NullPointerException | IndexOutOfBoundsException e) {
			Logger.warn("Unable to process file '{}': {}", LINUX_MEMINFO_FILE, e.getMessage());
		}
	}

	private void populateFromVersion() {
		try {
			kernelVersion = Files.lines(Paths.get(LINUX_VERSION_FILE)).findFirst()
					.map(line -> line.replaceAll("^Linux version (\\d+\\.\\d+\\.\\d+).*$", "$1")).map(Version::new)
					.orElse(null);
		} catch (IOException | NullPointerException | IndexOutOfBoundsException e) {
			Logger.warn("Unable to process file '{}': {}", LINUX_VERSION_FILE, e.getMessage());
		}
	}

	public String getOsName() {
		return osName;
	}

	public boolean isLinux() {
		return osName.equals(LINUX_OS_NAME);
	}

	public boolean isMacOS() {
		return osName.equals(MAC_OS_NAME);
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

	public boolean isArm() {
		return isArm32() || isArm64();
	}

	public boolean isArmv6() {
		return cpuArchitecture != null && cpuArchitecture.startsWith(ARMV6_CPU_ARCHITECTURE);
	}

	public boolean isArmv7() {
		return cpuArchitecture != null && cpuArchitecture.startsWith(ARMV7_CPU_ARCHITECTURE);
	}

	public String getLibFileExtension() {
		return libFileExtension;
	}

	public String getRevision() {
		return revision;
	}

	public String getMake() {
		return make;
	}

	public String getModel() {
		return model;
	}

	public String getSoc() {
		return soc;
	}

	public String getCpuArchitecture() {
		return cpuArchitecture;
	}

	public String getCpuModelName() {
		return cpuModelName;
	}

	public Integer getMemoryKb() {
		return memoryKb;
	}

	public Version getKernelVersion() {
		return kernelVersion;
	}

	public String getDefaultLibraryPath() {
		String lib_path;
		if (isArm32() && !StringUtil.isNullOrBlank(cpuArchitecture)) {
			lib_path = osName.toLowerCase().replace(" ", "") + "-" + cpuArchitecture.toLowerCase();
		} else {
			lib_path = osName.toLowerCase().replace(" ", "") + "-" + osArch.toLowerCase();
		}
		return lib_path;
	}

	public List<String> getLinuxBoardCompatibility() {
		return compatible;
	}

	/**
	 * Get the local operating system id. For Linux this is as defined by the ID property in
	 * <code>/etc/os-release</code>. For macOS it is the "ProductName" as returned by sw_vers.
	 *
	 * @return value of the ID property
	 */
	public String getOperatingSystemId() {
		return osId;
	}

	/**
	 * Get the local operating system version as defined by the VERSION property in
	 * <code>/etc/os-release</code>
	 *
	 * @return value of the VERSION property
	 */
	public String getOperatingSystemVersion() {
		return osVersion;
	}

	public static List<Integer> getI2CBusNumbers() {
		try {
			return StreamSupport.stream(Files.newDirectoryStream(Paths.get("/dev"), "i2c-*").spliterator(), false)
					.map(path -> Integer.valueOf(path.toString().split("-")[1])).sorted().collect(Collectors.toList());
		} catch (IOException e) {
			Logger.info(e, "Error enumerating local I2C buses: {}", e);
			return Collections.emptyList();
		}
	}

	public static int getI2CFunctionalities(int controller) {
		int fd = FileNative.open("/dev/i2c-" + controller, FileNative.O_RDWR);
		if (fd < 0) {
			return -1;
		}

		int funcs = NativeI2C.getFuncs(fd);
		FileNative.close(fd);

		return funcs;
	}

	/**
	 * Utility method to get the CPU temperate of the attached board
	 *
	 * @return the CPU temperature
	 */
	public float getCpuTemperature() {
		if (isLinux()) {
			try {
				return Integer.parseInt(Files.lines(Paths.get(TEMPERATURE_FILE)).findFirst().orElse("0")) / 1000f;
			} catch (Throwable t) {
				Logger.debug("Error reading {}: {}", TEMPERATURE_FILE, t);
				return -1;
			}
		} else if (isMacOS()) {
			// Assumes osx-cpu-temp has been installed (brew install osx-cpu-temp)
			try (BufferedReader br = new BufferedReader(
					new InputStreamReader(Runtime.getRuntime().exec("/usr/local/bin/osx-cpu-temp").getInputStream()))) {
				// Note that the output includes the non-ASCII degree symbol
				// Remove any non-numeric / decimal point characters
				return Float.parseFloat(br.readLine().replaceAll("[^0-9\\.]", ""));
			} catch (Exception e) {
				// Ignore
				Logger.debug(e, "Error getting macOS CPU temp: {}", e);
			}
		}
		return -1;
	}

	@Override
	public String toString() {
		return "LocalSystemInfo [osName=" + osName + ", osArch=" + osArch + ", libFileExtension=" + libFileExtension
				+ ", make=" + make + ", model=" + model + ", soc=" + soc + ", revision=" + revision + ", serial="
				+ serial + ", cpuArchitecture=" + cpuArchitecture + ", cpuModelName=" + cpuModelName + ", memoryKb="
				+ memoryKb + "]";
	}

	public static void main(String[] args) {
		System.out.println("System properties:");
		System.getProperties().forEach((key, value) -> System.out.println(key + ": " + value));

		LocalSystemInfo lsi = new LocalSystemInfo();
		System.out.println("macOS? " + lsi.isMacOS() + ", Linux? " + lsi.isLinux() + ", Windows? " + lsi.isWindows());
		System.out.println(lsi.getOperatingSystemId() + " version " + lsi.getOperatingSystemVersion());
		System.out.println("CPU Temperature: " + lsi.getCpuTemperature());

		System.out.println(lsi);
	}
}
