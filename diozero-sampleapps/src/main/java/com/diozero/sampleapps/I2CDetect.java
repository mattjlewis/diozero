package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     I2CDetect.java
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import com.diozero.api.DeviceBusyException;
import com.diozero.api.I2CDevice;
import com.diozero.api.I2CDeviceInterface;
import com.diozero.sbc.DeviceFactoryHelper;

import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "I2CDetect", mixinStandardHelpOptions = true, //
		description = "A reimplementation of i2cdetect in Java - scan an I2C bus to detect attached devices.")
public class I2CDetect implements Runnable {
	private static final int LAST_ADDRESS = 0x77;

	private static final int FIRST_ADDRESS = 0x03;

	public static void main(String[] args) {
		I2CDetect app = new I2CDetect();
		new CommandLine(app).execute(args);
	}

	@Spec
	private CommandSpec spec;

	@ArgGroup(order = 0, exclusive = true, multiplicity = "1")
	private ListOrDetail listOrDetail;

	private static class ListOrDetail {
		@Option(names = "-l", required = true, description = "Output a list of installed buses.")
		Boolean listBuses;
		@ArgGroup(order = 0, exclusive = false, multiplicity = "1")
		Detail detail;
	}

	private static class Detail {
		@Option(names = "-y", required = false, description = "Disable interactive mode. By default, i2cdetect will "
				+ "wait for a confirmation from the user before messing with the I2C bus. When this flag is used, it "
				+ "will perform the operation directly. This is mainly meant to be used in scripts.", defaultValue = "false")
		boolean nonInteractive;
		@Option(names = "-q", required = false, description = "Use SMBus \"quick write\" command for probing.  "
				+ "Not recommended. This is known to corrupt the Atmel AT24RF08 EEPROM found on many IBM Thinkpad laptops.")
		Boolean quickWriteMode;
		@Option(names = "-r", required = false, description = "Use SMBus \"receive byte\" command for probing.  "
				+ "Not recommended. This is known to lock SMBus on various write-only chips (most notably clock chips at "
				+ "address 0x69).")
		Boolean receiveByteMode;
		@Option(names = "-F", required = false, description = "Display the list of functionalities implemented by the "
				+ "adapter and exit.")
		Boolean listFunctionalities;

		// FIXME Ugly hack. picocli does not handle optional positional parameters
		// within ArgGroups
		@Parameters(index = "0..2", arity = "1..3", hideParamSyntax = true, paramLabel = "i2cbus [first last]", //
				description = "I2C bus number and optional scan address range.")
		String[] params;
	}

	private I2CDevice.ProbeMode mode;
	private int firstAddress;
	private int lastAddress;

	@Override
	public void run() {
		if (listOrDetail.listBuses != null && listOrDetail.listBuses.booleanValue()) {
			for (Integer bus_num : DeviceFactoryHelper.getNativeDeviceFactory().getI2CBusNumbers()) {
				System.out.format("i2c-%d%n", bus_num);
			}
			return;
		}

		mode = I2CDevice.ProbeMode.AUTO;
		if (listOrDetail.detail.quickWriteMode != null && listOrDetail.detail.quickWriteMode.booleanValue()) {
			mode = I2CDevice.ProbeMode.QUICK;
		} else if (listOrDetail.detail.receiveByteMode != null && listOrDetail.detail.receiveByteMode.booleanValue()) {
			mode = I2CDevice.ProbeMode.READ;
		}

		String bus_num = listOrDetail.detail.params[0].trim();
		int i2cbus;
		try {
			i2cbus = Integer.parseInt(bus_num);
		} catch (NumberFormatException e) {
			throw new ParameterException(spec.commandLine(), "Invalid I2C bus number '" + bus_num + "'");
		}

		firstAddress = FIRST_ADDRESS;
		lastAddress = LAST_ADDRESS;
		String first = null;
		String last = null;
		if (listOrDetail.detail.params.length > 1) {
			first = listOrDetail.detail.params[1];
			if (listOrDetail.detail.params.length > 2) {
				last = listOrDetail.detail.params[2];
			}
		}
		if (first != null) {
			firstAddress = Integer.decode(first).intValue();
		}
		if (last != null) {
			lastAddress = Integer.decode(last).intValue();
		}

		if (firstAddress >= lastAddress) {
			throw new ParameterException(spec.commandLine(), "first must be < last");
		}
		if (firstAddress < FIRST_ADDRESS) {
			throw new ParameterException(spec.commandLine(),
					String.format("Error: FIRST (0x%02x) argument out of range (0x%02x-0x%02x)!%n",
							Integer.valueOf(firstAddress), Integer.valueOf(FIRST_ADDRESS),
							Integer.valueOf(LAST_ADDRESS)));
		}
		if (lastAddress > LAST_ADDRESS) {
			throw new ParameterException(spec.commandLine(),
					String.format("Error: LAST argument (0x%02x) out of range (0x%02x-0x%02x)!%n",
							Integer.valueOf(lastAddress), Integer.valueOf(FIRST_ADDRESS),
							Integer.valueOf(LAST_ADDRESS)));
		}

		if (listOrDetail.detail.listFunctionalities != null && listOrDetail.detail.listFunctionalities.booleanValue()) {
			scanFunctionalities(i2cbus);
			return;
		}

		boolean run = listOrDetail.detail.nonInteractive;
		if (!listOrDetail.detail.nonInteractive) {
			System.out.println("WARNING! This program can confuse your I2C bus, cause data loss and worse!");
			System.out.format("I will probe file /dev/i2c-%d.%n", Integer.valueOf(i2cbus));
			System.out.format("I will probe address range 0x%02x-0x%02x.%n", Integer.valueOf(firstAddress),
					Integer.valueOf(lastAddress));
			System.out.print("Continue? [Y/n] ");
			System.out.flush();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
				String line = reader.readLine();
				run = line.trim().toLowerCase().startsWith("y") || line.trim().isEmpty();
			} catch (IOException e) {
				// Ignore
			}
		}

		if (run) {
			scanI2CBus(i2cbus, mode, firstAddress, lastAddress);
		} else {
			System.out.println("Aborting on user request.");
		}
	}

	private static void scanFunctionalities(int controller) {
		System.out.format("Functionalities implemented by /dev/i2c-%d:%n", Integer.valueOf(controller));
		int funcs = DeviceFactoryHelper.getNativeDeviceFactory().getI2CFunctionalities(controller);
		System.out.println("I2C                        " + yesNo(funcs, I2CDeviceInterface.I2C_FUNC_I2C));
		System.out.println("SMBus Quick Command        " + yesNo(funcs, I2CDeviceInterface.I2C_FUNC_SMBUS_QUICK));
		System.out.println("SMBus Send Byte            " + yesNo(funcs, I2CDeviceInterface.I2C_FUNC_SMBUS_WRITE_BYTE));
		System.out.println("SMBus Receive Byte         " + yesNo(funcs, I2CDeviceInterface.I2C_FUNC_SMBUS_READ_BYTE));
		System.out.println("SMBus Write Byte           " + //
				yesNo(funcs, I2CDeviceInterface.I2C_FUNC_SMBUS_WRITE_BYTE_DATA));
		System.out.println("SMBus Read Byte            " + //
				yesNo(funcs, I2CDeviceInterface.I2C_FUNC_SMBUS_READ_BYTE_DATA));
		System.out.println("SMBus Write Word           " + //
				yesNo(funcs, I2CDeviceInterface.I2C_FUNC_SMBUS_WRITE_WORD_DATA));
		System.out.println("SMBus Read Word            " + //
				yesNo(funcs, I2CDeviceInterface.I2C_FUNC_SMBUS_READ_WORD_DATA));
		System.out.println("SMBus Process Call         " + yesNo(funcs, I2CDeviceInterface.I2C_FUNC_SMBUS_PROC_CALL));
		System.out.println("SMBus Block Write          " + //
				yesNo(funcs, I2CDeviceInterface.I2C_FUNC_SMBUS_WRITE_BLOCK_DATA));
		System.out.println("SMBus Block Read           " + //
				yesNo(funcs, I2CDeviceInterface.I2C_FUNC_SMBUS_READ_BLOCK_DATA));
		System.out.println("SMBus Block Process Call   " + //
				yesNo(funcs, I2CDeviceInterface.I2C_FUNC_SMBUS_BLOCK_PROC_CALL));
		System.out.println("SMBus PEC                  " + yesNo(funcs, I2CDeviceInterface.I2C_FUNC_SMBUS_PEC));
		System.out.println("I2C Block Write            " + //
				yesNo(funcs, I2CDeviceInterface.I2C_FUNC_SMBUS_WRITE_I2C_BLOCK));
		System.out.println("I2C Block Read             " + //
				yesNo(funcs, I2CDeviceInterface.I2C_FUNC_SMBUS_READ_I2C_BLOCK));
	}

	private static String yesNo(int i, int mask) {
		return yesNo((i & mask) != 0);
	}

	private static String yesNo(boolean b) {
		return b ? "yes" : "no";
	}

	private static void scanI2CBus(int controller, I2CDevice.ProbeMode mode, int first, int last) {
		System.out.println("     0  1  2  3  4  5  6  7  8  9  a  b  c  d  e  f");
		for (int device_address = 0; device_address < 128; device_address++) {
			if ((device_address % 16) == 0) {
				System.out.print(String.format("%02x: ", Integer.valueOf(device_address)));
			}
			if (device_address < first || device_address > last) {
				System.out.print("   ");
			} else {
				try (I2CDevice device = I2CDevice.builder(device_address).setController(controller).build()) {
					if (device.probe(mode)) {
						System.out.print(String.format("%02x ", Integer.valueOf(device_address)));
					} else {
						System.out.print("-- ");
					}
				} catch (DeviceBusyException e) {
					System.out.print("UU ");
				}
			}
			if ((device_address % 16) == 15) {
				System.out.println();
			}
		}
	}
}
