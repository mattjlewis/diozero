package com.diozero.sbc;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     SystemInfoConstants.java  
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
