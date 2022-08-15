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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import org.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.sbc.BoardInfo;
import com.diozero.sbc.LocalSystemInfo;
import com.diozero.util.StringUtil;

public class GenericLinuxArmBoardInfo extends BoardInfo {
	public GenericLinuxArmBoardInfo(LocalSystemInfo systemInfo) {
		this(systemInfo, BoardInfo.UNKNOWN);
	}

	public GenericLinuxArmBoardInfo(LocalSystemInfo systemInfo, String make) {
		this(make, systemInfo.getModel(), systemInfo.getMemoryKb() == null ? -1 : systemInfo.getMemoryKb().intValue(),
				BoardInfo.UNKNOWN_ADC_VREF, systemInfo.getDefaultLibraryPath());
	}

	public GenericLinuxArmBoardInfo(String make, String model, int memoryKb) {
		this(make, model, memoryKb, BoardInfo.UNKNOWN_ADC_VREF, LocalSystemInfo.getInstance().getDefaultLibraryPath());
	}

	public GenericLinuxArmBoardInfo(String make, String model, int memoryKb, float adcVRef) {
		this(make, model, memoryKb, adcVRef, LocalSystemInfo.getInstance().getDefaultLibraryPath());
	}

	public GenericLinuxArmBoardInfo(String make, String model, int memoryKb, String libraryPath) {
		this(make, model, memoryKb, BoardInfo.UNKNOWN_ADC_VREF,
				libraryPath + "-" + LocalSystemInfo.getInstance().getOsArch());
	}

	public GenericLinuxArmBoardInfo(String make, String model, int memoryKb, float adcVRef, String libraryPath) {
		super(make, model, memoryKb, adcVRef, libraryPath, LocalSystemInfo.getInstance().getOperatingSystemId(),
				LocalSystemInfo.getInstance().getOperatingSystemVersion());
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
		for (String compatibility : getBoardCompatibility()) {
			boolean loaded = loadBoardPinInfoDefinition(compatibility.split(","));

			if (loaded) {
				break;
			}
		}

		// Note that if this fails the GPIO character implementation in the device
		// factory will auto-populate (if enabled)
	}

	@SuppressWarnings("static-method")
	protected List<String> getBoardCompatibility() {
		return LocalSystemInfo.getInstance().loadLinuxBoardCompatibility();
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

						line = line.trim();

						if (line.startsWith("#") || line.isEmpty()) {
							continue;
						}

						int index = line.indexOf("#");
						if (index != -1) {
							line = line.substring(0, index);
						}

						String[] parts = line.split(",");
						try {
							switch (parts[0].trim().toUpperCase()) {
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
								// Ignore
							}
						} catch (IllegalArgumentException e) {
							Logger.warn("Illegal argument: {} - line: ", e.getMessage(), line);
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
				Integer.parseInt(parts[4].trim()), parseModes(parts[5].trim()), Integer.parseInt(parts[6].trim()),
				Integer.parseInt(parts[7].trim()));
	}

	private void loadPwmPinInfo(String[] parts) {
		if (parts.length != 10) {
			Logger.warn("Invalid PWM def line '{}'", String.join(",", parts));
			return;
		}
		addPwmPinInfo(parts[1].trim(), Integer.parseInt(parts[2].trim()), parts[3].trim(),
				Integer.parseInt(parts[4].trim()), Integer.parseInt(parts[5].trim()), Integer.parseInt(parts[6].trim()),
				parseModes(parts[7].trim()), Integer.parseInt(parts[8].trim()), Integer.parseInt(parts[9].trim()));
	}

	private void loadAdcPinInfo(String[] parts) {
		if (parts.length != 5) {
			Logger.warn("Invalid ADC def line '{}'", String.join(",", parts));
			return;
		}
		addAdcPinInfo(parts[1].trim(), Integer.parseInt(parts[2].trim()), parts[3].trim(),
				Integer.parseInt(parts[4].trim()));
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
}
