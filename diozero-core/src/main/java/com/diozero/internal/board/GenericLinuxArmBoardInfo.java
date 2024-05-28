package com.diozero.internal.board;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     GenericLinuxArmBoardInfo.java
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
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import org.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.internal.spi.MmapGpioInterface;
import com.diozero.sbc.BoardInfo;
import com.diozero.sbc.LocalSystemInfo;
import com.diozero.util.StringUtil;
import com.diozero.util.Version;

public class GenericLinuxArmBoardInfo extends BoardInfo {
	private static final String SOC_MAPPING_FILE = "/soc_mapping.properties";

	private String soc;
	private List<String> compatibility;
	private Properties mmapGpioClasses;
	private Optional<Map<String, Integer>> chipMapping;
	private boolean boardDefLoaded = false;

	public GenericLinuxArmBoardInfo(LocalSystemInfo systemInfo) {
		this(systemInfo, systemInfo.getMake());
	}

	public GenericLinuxArmBoardInfo(LocalSystemInfo systemInfo, String make) {
		this(make, systemInfo.getModel(), systemInfo.getSoc(),
				systemInfo.getMemoryKb() == null ? -1 : systemInfo.getMemoryKb().intValue(),
				systemInfo.getLinuxBoardCompatibility());
	}

	public GenericLinuxArmBoardInfo(String make, String model, String soc, int memoryKb, List<String> compatibility) {
		super(make, model, memoryKb, LocalSystemInfo.getInstance().getOperatingSystemId(),
				LocalSystemInfo.getInstance().getOperatingSystemVersion());

		this.soc = soc;
		this.compatibility = compatibility;

		mmapGpioClasses = new Properties();
		try {
			mmapGpioClasses.load(getClass().getResourceAsStream(SOC_MAPPING_FILE));
		} catch (IOException e) {
			Logger.error(e, "Error loading SoC to MMAP GPIO implementation class file '{}': {}", SOC_MAPPING_FILE,
					e.getMessage());
		}

		chipMapping = Optional.empty();
	}

	@Override
	public void populateBoardPinInfo() {
		// Try to load the board definition file from the classpath using the device
		// tree compatibility values
		/*- Examples:
		 * ["asus,rk3288-tinker", "rockchip,rk3288"]
		 * ["raspberrypi,4-model-b", "brcm,bcm2711"]
		 * ["raspberrypi,3-model-b", "brcm,bcm2837"]
		 * ["raspberrypi,model-zero", "brcm,bcm2835"]
		 * ["hardkernel,odroid-c2", "amlogic,meson-gxbb"]
		 */
		for (String compat : compatibility) {
			boardDefLoaded = loadBoardPinInfoDefinition(compat.split(","));
			if (boardDefLoaded) {
				break;
			}
		}

		// Note that if this fails the GPIO chardev implementation in the device
		// factory will attempt to auto-populate (if enabled)
	}

	@Override
	public boolean isRecognised() {
		return boardDefLoaded;
	}

	protected boolean loadBoardPinInfoDefinition(String... compatibilityParts) {
		for (int i = 0; i < compatibilityParts.length; i++) {
			compatibilityParts[i] = compatibilityParts[i].trim();
		}

		boolean loaded = false;
		for (int i = 0; i < compatibilityParts.length && !loaded; i++) {
			loaded = loadBoardPinInfoDefinition(
					StringUtil.join("_", "/boarddefs/", ".txt", compatibilityParts.length - i, compatibilityParts));
		}

		return loaded;
	}

	protected boolean loadBoardPinInfoDefinition(String boardDefFile) {
		boolean loaded = false;
		Logger.trace("Looking for board def file '{}'", boardDefFile);
		try (InputStream is = getClass().getResourceAsStream(boardDefFile)) {
			if (is != null) {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
					Logger.debug("Reading board defs file {}", boardDefFile);
					while (true) {
						String line = reader.readLine();
						if (line == null) {
							break;
						}

						// Strip comments
						int index = line.indexOf("#");
						if (index != -1) {
							line = line.substring(0, index);
						}

						// Ignore empty lines
						if (line.isEmpty()) {
							continue;
						}

						String[] parts = line.split(",");
						try {
							switch (parts[0].trim().toUpperCase()) {
							case "CHIP":
								loadChipMapping(parts);
								break;
							case "GENERAL":
								loadGeneralPinInfo(parts);
								break;
							case "GPIO":
								loadGpioPinInfo(parts);
								break;
							case "PWM":
								loadPwmPinInfo(parts);
								break;
							case "ADC":
								loadAdcPinInfo(parts);
								break;
							case "DAC":
								loadDacPinInfo(parts);
								break;
							default:
								Logger.warn("Unexpected entry '{}' in line '{}'", parts[0].trim(), line);
							}
						} catch (IllegalArgumentException e) {
							Logger.warn("Illegal argument: {} - line: '{}'", e.getMessage(), line);
						}
					}

					loaded = true;
				}
			}
		} catch (IOException e) {
			// Ignore
		}

		return loaded;
	}

	private void loadChipMapping(String[] parts) {
		if (parts.length != 3) {
			Logger.warn("Invalid Chip Mapping def line '{}'", String.join(",", parts));
			return;
		}
		addChipMapping(Integer.parseInt(parts[1].trim()), parts[2].trim());
	}

	private void loadGeneralPinInfo(String[] parts) {
		if (parts.length != 4) {
			Logger.warn("Invalid General Pin def line '{}'", String.join(",", parts));
			return;
		}
		addGeneralPinInfo(parts[1].trim(), Integer.parseInt(parts[2].trim()), parts[3].trim());
	}

	private void loadGpioPinInfo(String[] parts) {
		if (parts.length != 8) {
			Logger.warn("Invalid GPIO def line '{}'", String.join(",", parts));
			return;
		}

		addGpioPinInfo(parts[1].trim(), Integer.parseInt(parts[2].trim()), parts[3].trim(),
				Integer.parseInt(parts[4].trim()), parseModes(parts[7].trim()), Integer.parseInt(parts[5].trim()),
				Integer.parseInt(parts[6].trim()));
	}

	private void loadPwmPinInfo(String[] parts) {
		if (parts.length != 10) {
			Logger.warn("Invalid PWM def line '{}'", String.join(",", parts));
			return;
		}

		// CSV: Header, GPIO#, Name, Physical Pin, Chip, Line, PWM Chip, PWM Num, Modes
		addPwmPinInfo(parts[1].trim(), Integer.parseInt(parts[2].trim()), parts[3].trim(),
				Integer.parseInt(parts[4].trim()), Integer.parseInt(parts[7].trim()), Integer.parseInt(parts[8].trim()),
				parseModes(parts[9].trim()), Integer.parseInt(parts[5].trim()), Integer.parseInt(parts[6].trim()));
	}

	private void loadAdcPinInfo(String[] parts) {
		if (parts.length != 6) {
			Logger.warn("Invalid ADC def line '{}'", String.join(",", parts));
			return;
		}
		addAdcPinInfo(parts[1].trim(), Integer.parseInt(parts[2].trim()), parts[3].trim(),
				Integer.parseInt(parts[4].trim()), Float.parseFloat(parts[5].trim()));
	}

	private void loadDacPinInfo(String[] parts) {
		if (parts.length != 5) {
			Logger.warn("Invalid DAC def line '{}'", String.join(",", parts));
			return;
		}
		addDacPinInfo(parts[1].trim(), Integer.parseInt(parts[2].trim()), parts[3].trim(),
				Integer.parseInt(parts[4].trim()));
	}

	private static Collection<DeviceMode> parseModes(String modeValues) {
		if (modeValues.isEmpty()) {
			return EnumSet.noneOf(DeviceMode.class);
		}
		return Arrays.stream(modeValues.split("\\|")).map(mode -> DeviceMode.valueOf(mode.trim().toUpperCase()))
				.collect(Collectors.toSet());
	}

	@Override
	public Optional<Map<String, Integer>> getChipMapping() {
		return chipMapping;
	}

	private void addChipMapping(int chipId, String label) {
		if (chipMapping.isEmpty()) {
			chipMapping = Optional.of(new HashMap<>());
		}
		chipMapping.get().put(label, Integer.valueOf(chipId));
	}

	@Override
	public MmapGpioInterface createMmapGpio() {
		final String clz_name = mmapGpioClasses.getProperty(soc);
		if (clz_name == null) {
			return null;
		}
		try {
			return (MmapGpioInterface) Class.forName(clz_name).getDeclaredConstructor().newInstance();
		} catch (Throwable t) {
			Logger.error(t, "Error resolving MMAP GPIO instance '{}' for SoC '{}'", clz_name, soc);
			return null;
		}
	}

	@Override
	public boolean isBiasControlSupported() {
		final Version kernel_version = LocalSystemInfo.getInstance().getKernelVersion();
		return kernel_version != null && (kernel_version.getMajor() >= 6
				|| (kernel_version.getMajor() == 5 && kernel_version.getMinor() >= 5));
	}
}
