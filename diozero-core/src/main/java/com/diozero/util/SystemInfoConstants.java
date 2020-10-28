package com.diozero.util;

public interface SystemInfoConstants {
	static final String OS_NAME_SYSTEM_PROPERTY = "os.name";
	static final String OS_ARCH_SYSTEM_PROPERTY = "os.arch";
	static final String ARM_32_OS_ARCH = "arm";
	static final String ARM_64_OS_ARCH = "aarch64";
	static final String LINUX_OS_NAME = "Linux";

	// Linux system files used for discovery
	static final String LINUX_OS_RELEASE_FILE = "/etc/os-release";
	static final String LINUX_CPUINFO_FILE = "/proc/cpuinfo";
	static final String LINUX_MEMINFO_FILE = "/proc/meminfo";
	static final String LINUX_DEVICE_TREE_COMPATIBLE_FILE = "/proc/device-tree/compatible";
	static final String LINUX_DEVICE_TREE_MODEL_FILE = "/proc/device-tree/model";
	static final String LINUX_DEVICE_TREE_SERIAL_NUMBER_FILE = "/proc/device-tree/serial-number";
}
