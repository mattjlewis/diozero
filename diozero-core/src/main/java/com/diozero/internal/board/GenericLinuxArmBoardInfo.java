package com.diozero.internal.board;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.tinylog.Logger;

import com.diozero.util.SystemInfoConstants;

public class GenericLinuxArmBoardInfo extends UnknownBoardInfo {
	public GenericLinuxArmBoardInfo(String model, String revision, Integer memoryKb, String osName, String osArch) {
		super(model, memoryKb, osName, osArch);
	}

	@Override
	public void initialisePins() {
		// TODO Attempt to discover automatically?

		try {
			// FIXME I believe this file has multiple strings separated by \0
			byte[] bytes = Files.readAllBytes(Paths.get(SystemInfoConstants.LINUX_DEVICE_TREE_COMPATIBLE_FILE));
			List<String> compatible = new ArrayList<>();
			int string_start = 0;
			for (int i = 0; i < bytes.length; i++) {
				if (bytes[i] == 0 || i == bytes.length - 1) {
					compatible.add(new String(bytes, string_start, i - string_start));
					string_start = i + 1;
				}
			}
			System.out.println("*** compatible: " + compatible);

			// TODO Load a custom file from the classpath that defines GPIOs, names and gpio
			// chip and offset?
		} catch (IOException e) {
			// This file should exist on a Linux system
			Logger.warn(e, "Unable to read file {}: {}", SystemInfoConstants.LINUX_DEVICE_TREE_COMPATIBLE_FILE,
					e.getMessage());
		}
	}
}
