package com.diozero.internal.board;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import org.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.util.SystemInfoConstants;

public class GenericLinuxArmBoardInfo extends UnknownBoardInfo {
	public GenericLinuxArmBoardInfo(String make, String model, Integer memoryKb, String osName, String osArch) {
		super(make, model, memoryKb, osName, osArch);
	}

	public GenericLinuxArmBoardInfo(String make, String model, Integer memoryKb, String libraryPath) {
		super(make, model, memoryKb, libraryPath);
	}

	@Override
	public void initialisePins() {
		// Try to load the board definition file from the classpath using the device
		// tree compatibility values
		try {
			// This file has multiple strings separated by \0
			byte[] bytes = Files.readAllBytes(Paths.get(SystemInfoConstants.LINUX_DEVICE_TREE_COMPATIBLE_FILE));
			List<String> compatible = new ArrayList<>();
			int string_start = 0;
			for (int i = 0; i < bytes.length; i++) {
				if (bytes[i] == 0 || i == bytes.length - 1) {
					compatible.add(new String(bytes, string_start, i - string_start));
					string_start = i + 1;
				}
			}

			/*- Examples:
			 * [asus,rk3288-tinker, rockchip,rk3288]
			 * [raspberrypi,4-model-b,brcm,bcm2711]
			 * [raspberrypi,3-model-b,brcm,bcm2837]
			 * [hardkernel,odroid-c2, amlogic,meson-gxbb]
			 */
			for (String compatibility : compatible) {
				String[] values = compatibility.split(",");
				// First look for classpath:/boarddefs/values[0]-values[1].txt
				boolean loaded = loadBoardPinInfoDefinition(values);
				if (!loaded) {
					// If not found, look for classpath:/boarddefs/values[0].txt
					loaded = loadBoardPinInfoDefinition(values[0]);
				}
				if (loaded) {
					break;
				}
			}
		} catch (IOException e) {
			// This file should exist on a Linux system
			Logger.warn(e, "Unable to read file {}: {}", SystemInfoConstants.LINUX_DEVICE_TREE_COMPATIBLE_FILE,
					e.getMessage());
		}

		// Note that if this fails the GPIO character implementation in the device
		// factory will auto-populate (if activated)
	}

	private boolean loadBoardPinInfoDefinition(String... paths) {
		boolean loaded = false;
		String file = "/boarddefs/" + String.join("-", paths) + ".txt";
		try (InputStream is = getClass().getResourceAsStream(file)) {
			if (is != null) {
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
					Logger.debug("Reading board defs file {}", file);
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
		addGpioPinInfo(parts[1].trim().toUpperCase(), Integer.parseInt(parts[2].trim()), parts[3].trim(),
				Integer.parseInt(parts[4].trim()), parseModes(parts[5].trim()), Integer.parseInt(parts[6].trim()),
				Integer.parseInt(parts[7].trim()));
	}

	private void loadPwmPinInfo(String[] parts) {
		if (parts.length != 9) {
			Logger.warn("Invalid PWM def line '{}'", String.join(",", parts));
			return;
		}
		addPwmPinInfo(parts[1].trim().toUpperCase(), Integer.parseInt(parts[2].trim()), parts[3].trim(),
				Integer.parseInt(parts[4].trim()), Integer.parseInt(parts[5].trim()), parseModes(parts[6].trim()),
				Integer.parseInt(parts[7].trim()), Integer.parseInt(parts[8].trim()));
	}

	private void loadAdcPinInfo(String[] parts) {
		if (parts.length != 5) {
			Logger.warn("Invalid ADC def line '{}'", String.join(",", parts));
			return;
		}
		addAdcPinInfo(parts[1].trim().toUpperCase(), Integer.parseInt(parts[2].trim()), parts[3].trim(),
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
