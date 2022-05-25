package com.diozero.internal.board.raspberrypi;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     RaspberryPiBoardInfoProvider.java
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.PinInfo;
import com.diozero.internal.board.GenericLinuxArmBoardInfo;
import com.diozero.internal.spi.BoardInfoProvider;
import com.diozero.internal.spi.MmapGpioInterface;
import com.diozero.sbc.BoardInfo;
import com.diozero.sbc.LocalSystemInfo;

/**
 * See
 * <a href="https://github.com/AndrewFromMelbourne/raspberry_pi_revision">this c
 * library</a>. See also <a href="http://elinux.org/RPi_HardwareHistory">this
 * table of revisions</a>.
 */
public class RaspberryPiBoardInfoProvider implements BoardInfoProvider {
	public static final String MAKE = "Raspberry Pi";
	private static final String BCM_HARDWARE_PREFIX = "BCM";

	public static final String MODEL_A = "A";
	public static final String MODEL_B = "B";
	public static final String MODEL_A_PLUS = "A+";
	public static final String MODEL_B_PLUS = "B+";
	public static final String MODEL_2B = "2B";
	public static final String MODEL_ALPHA = "Alpha";
	public static final String COMPUTE_MODULE = "CM";
	public static final String MODEL_3B = "3B";
	public static final String MODEL_ZERO = "Zero";
	public static final String COMPUTE_MODULE_3 = "CM3";
	public static final String MODEL_ZERO_W = "ZeroW";
	public static final String MODEL_ZERO_W_2 = "ZeroW2";
	public static final String MODEL_3B_PLUS = "3B+";
	public static final String MODEL_3A_PLUS = "3A+";
	public static final String COMPUTE_MODULE_3_PLUS = "CM3+";
	public static final String MODEL_4B = "4B";
	public static final String MODEL_400 = "400";
	public static final String COMPUTE_MODULE_4 = "CM4";

	private static Map<Integer, String> MODELS;
	static {
		MODELS = new HashMap<>();
		MODELS.put(Integer.valueOf(0x0), MODEL_A);
		MODELS.put(Integer.valueOf(0x1), MODEL_B);
		MODELS.put(Integer.valueOf(0x2), MODEL_A_PLUS);
		MODELS.put(Integer.valueOf(0x3), MODEL_B_PLUS);
		MODELS.put(Integer.valueOf(0x4), MODEL_2B);
		MODELS.put(Integer.valueOf(0x5), MODEL_ALPHA);
		MODELS.put(Integer.valueOf(0x6), COMPUTE_MODULE);
		MODELS.put(Integer.valueOf(0x8), MODEL_3B);
		MODELS.put(Integer.valueOf(0x9), MODEL_ZERO);
		MODELS.put(Integer.valueOf(0xa), COMPUTE_MODULE_3);
		MODELS.put(Integer.valueOf(0xc), MODEL_ZERO_W);
		MODELS.put(Integer.valueOf(0xd), MODEL_3B_PLUS);
		MODELS.put(Integer.valueOf(0xe), MODEL_3A_PLUS);
		MODELS.put(Integer.valueOf(0x10), COMPUTE_MODULE_3_PLUS);
		MODELS.put(Integer.valueOf(0x11), MODEL_4B);
		MODELS.put(Integer.valueOf(0x12), MODEL_ZERO_W_2);
		MODELS.put(Integer.valueOf(0x13), MODEL_400);
		MODELS.put(Integer.valueOf(0x14), COMPUTE_MODULE_4);
	}

	private static final String PCB_REV_1_0 = "1.0";
	private static final String PCB_REV_1_1 = "1.1";
	private static final String PCB_REV_1_2 = "1.2";
	private static final String PCB_REV_2_0 = "2.0";

	private static Map<Integer, Integer> MEMORY;
	static {
		MEMORY = new HashMap<>();
		MEMORY.put(Integer.valueOf(0), Integer.valueOf(256_000));
		MEMORY.put(Integer.valueOf(1), Integer.valueOf(512_000));
		MEMORY.put(Integer.valueOf(2), Integer.valueOf(1_024_000));
		MEMORY.put(Integer.valueOf(3), Integer.valueOf(2_048_000));
		MEMORY.put(Integer.valueOf(4), Integer.valueOf(4_096_000));
		MEMORY.put(Integer.valueOf(5), Integer.valueOf(8_192_000));
	}

	private static final String SONY = "Sony";
	private static final String EGOMAN = "Egoman";
	private static final String EMBEST = "Embest";
	private static final String SONY_JAPAN = "Sony Japan";
	private static final String EMBEST_2 = "Embest-2";
	private static final String STADIUM = "Stadium";
	private static final String QISDA = "Qisda";
	private static Map<Integer, String> MANUFACTURERS;
	static {
		MANUFACTURERS = new HashMap<>();
		MANUFACTURERS.put(Integer.valueOf(0), SONY);
		MANUFACTURERS.put(Integer.valueOf(1), EGOMAN);
		MANUFACTURERS.put(Integer.valueOf(2), EMBEST);
		MANUFACTURERS.put(Integer.valueOf(3), SONY_JAPAN);
		MANUFACTURERS.put(Integer.valueOf(4), EMBEST_2);
		MANUFACTURERS.put(Integer.valueOf(5), STADIUM);
		MANUFACTURERS.put(Integer.valueOf(99), QISDA);
	}

	private static final String BCM2835 = "BCM2835";
	private static final String BCM2836 = "BCM2836";
	private static final String BCM2837 = "BCM2837";
	private static final String BCM2711 = "BCM2711";
	private static Map<Integer, String> PROCESSORS;
	static {
		PROCESSORS = new HashMap<>();
		PROCESSORS.put(Integer.valueOf(0), BCM2835);
		PROCESSORS.put(Integer.valueOf(1), BCM2836);
		PROCESSORS.put(Integer.valueOf(2), BCM2837);
		PROCESSORS.put(Integer.valueOf(3), BCM2711);
	}

	@Override
	public BoardInfo lookup(LocalSystemInfo systemInfo) {
		String hardware = systemInfo.getHardware();
		String revision = systemInfo.getRevision();
		if (systemInfo.getHardware() == null || revision == null) {
			return null;
		}
		if (!hardware.startsWith(BCM_HARDWARE_PREFIX) || revision.length() < 4) {
			return null;
		}

		return lookupByRevision(revision);
	}

	public static Map<Integer, Integer> extractPwmGpioNumbers(String line) {
		Map<String, Integer> pwm_config = new HashMap<>();
		// One PWM channel: dtoverlay=pwm,pin=12,func=4
		// Two PWM channels: dtoverlay=pwm-2chan,pin=12,func=4,pin2=13,func2=4
		String[] line_parts = line.split(",");
		int index = 1;
		try {
			do {
				String[] name_and_value = line_parts[index++].split("=");
				// FIXME Could map "pin" to "0" and "pin2" to "1", "pin<n>" to "n-1", etc
				pwm_config.put(name_and_value[0], Integer.valueOf(name_and_value[1]));
				name_and_value = line_parts[index++].split("=");
				pwm_config.put(name_and_value[0], Integer.valueOf(name_and_value[1]));
			} while (index < line_parts.length);
		} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
			Logger.warn(e, "Error parsing dtoverlay line '" + line + "': {}", e);
		}

		Map<Integer, Integer> pwm_gpio_numbers = new HashMap<>();
		Integer pwm_0_gpio = pwm_config.get("pin");
		if (pwm_0_gpio != null) {
			pwm_gpio_numbers.put(pwm_0_gpio, Integer.valueOf(0));
			Integer pwm_1_gpio = pwm_config.get("pin2");
			if (pwm_1_gpio != null) {
				pwm_gpio_numbers.put(pwm_1_gpio, Integer.valueOf(1));
			}
		}

		return pwm_gpio_numbers;
	}

	public static BoardInfo lookupByRevision(String revision) {
		try {
			int rev_int = Integer.parseInt(revision, 16);
			// With the release of the Raspberry Pi 2, there is a new encoding of the
			// Revision field in /proc/cpuinfo
			if ((rev_int & (1 << 23)) != 0) {
				String pcb_revision;
				int pcb_rev = rev_int & 0x0F;
				switch (pcb_rev) {
				case 0:
					pcb_revision = PCB_REV_1_0;
					break;
				case 1:
					pcb_revision = PCB_REV_1_1;
					break;
				case 2:
					pcb_revision = PCB_REV_2_0;
					break;
				default:
					pcb_revision = "1." + pcb_rev;
				}
				int model = (rev_int & (0xFF << 4)) >> 4;
				int proc = (rev_int & (0x0F << 12)) >> 12;
				int mfr = (rev_int & (0x0F << 16)) >> 16;
				int mem = (rev_int & (0x07 << 20)) >> 20;
				// boolean warranty_void = (revision & (0x03 << 24)) != 0;

				String model_val = MODELS.get(Integer.valueOf(model));
				if (model_val == null) {
					Logger.warn("Unknown Pi model: " + model);
					model_val = "UNKNOWN-" + model;
				}

				String proc_val = PROCESSORS.get(Integer.valueOf(proc));
				if (proc_val == null) {
					Logger.warn("Unknown Pi processor: " + proc);
					proc_val = "UNKNOWN-" + proc;
				}

				String mfr_val = MANUFACTURERS.get(Integer.valueOf(mfr));
				if (mfr_val == null) {
					Logger.warn("Unknown Pi manufacturer: " + mfr);
					mfr_val = "UNKNOWN-" + mfr;
				}

				Integer mem_val = MEMORY.get(Integer.valueOf(mem));
				if (mem_val == null) {
					Logger.warn("Unknown Pi memory value: " + mem);
					mem_val = Integer.valueOf(0);
				}

				return new PiABPlusBoardInfo(revision, model_val, pcb_revision, mem_val.intValue(), mfr_val, proc_val);
			}
		} catch (NumberFormatException nfe) {
			// Ignore
		}

		return legacyBoardLookup(revision.substring(revision.length() - 4));
	}

	private static BoardInfo legacyBoardLookup(String revision) {
		switch (revision) {
		case "0002":
			return new PiBRev1BoardInfo(revision, PCB_REV_1_0, 256, EGOMAN);
		case "0003":
			return new PiBRev1BoardInfo(revision, PCB_REV_1_1, 256, EGOMAN);
		case "0004":
			return new PiABRev2BoardInfo(revision, MODEL_B, 256, SONY);
		case "0005":
			return new PiABRev2BoardInfo(revision, MODEL_B, 256, QISDA);
		case "0006":
			return new PiABRev2BoardInfo(revision, MODEL_B, 256, EGOMAN);
		case "0007":
			return new PiABRev2BoardInfo(revision, MODEL_A, 256, EGOMAN);
		case "0008":
			return new PiABRev2BoardInfo(revision, MODEL_A, 256, SONY);
		case "0009":
			return new PiABRev2BoardInfo(revision, MODEL_A, 256, QISDA);
		case "000d":
			return new PiABRev2BoardInfo(revision, MODEL_B, 512, EGOMAN);
		case "000e":
			return new PiABRev2BoardInfo(revision, MODEL_B, 512, SONY);
		case "000f":
			return new PiABRev2BoardInfo(revision, MODEL_B, 512, QISDA);
		case "0010":
			return new PiABPlusBoardInfo(revision, MODEL_B_PLUS, PCB_REV_1_2, 512, SONY, BCM2835);
		case "0011":
			return new PiComputeModuleBoardInfo(revision, 512, SONY, BCM2835);
		case "0012":
			return new PiABPlusBoardInfo(revision, MODEL_A_PLUS, PCB_REV_1_2, 256, SONY, BCM2835);
		case "0013":
			return new PiABPlusBoardInfo(revision, MODEL_B_PLUS, PCB_REV_1_2, 512, EGOMAN, BCM2835);
		case "0014":
			return new PiComputeModuleBoardInfo(revision, 512, EMBEST, BCM2835);
		// Unknown as to whether this has 256MB or 512MB RAM
		case "0015":
			return new PiABPlusBoardInfo(revision, MODEL_A_PLUS, PCB_REV_1_1, 256, EMBEST, BCM2835);
		default:
			return null;
		}
	}

	/**
	 * <p>
	 * See <a href="https://pinout.xyz/">https://pinout.xyz/</a>
	 * </p>
	 *
	 * <pre>
	 *  +-----+-----+---------+------+---+---Pi 2---+---+------+---------+-----+-----+
	 *  | BCM | wPi |   Name  | Mode | V | Physical | V | Mode | Name    | wPi | BCM |
	 *  +-----+-----+---------+------+---+----++----+---+------+---------+-----+-----+
	 *  |     |     |    3.3v |      |   |  1 || 2  |   |      | 5v      |     |     |
	 *  |   2 |   8 |   SDA.1 | ALT0 | 1 |  3 || 4  |   |      | 5V      |     |     |
	 *  |   3 |   9 |   SCL.1 | ALT0 | 1 |  5 || 6  |   |      | 0v      |     |     |
	 *  |   4 |   7 | GPIO. 7 |   IN | 1 |  7 || 8  | 1 | ALT0 | TxD     | 15  | 14  |
	 *  |     |     |      0v |      |   |  9 || 10 | 1 | ALT0 | RxD     | 16  | 15  |
	 *  |  17 |   0 | GPIO. 0 |   IN | 0 | 11 || 12 | 1 | IN   | GPIO. 1 | 1   | 18  |
	 *  |  27 |   2 | GPIO. 2 |   IN | 0 | 13 || 14 |   |      | 0v      |     |     |
	 *  |  22 |   3 | GPIO. 3 |   IN | 0 | 15 || 16 | 0 | IN   | GPIO. 4 | 4   | 23  |
	 *  |     |     |    3.3v |      |   | 17 || 18 | 0 | IN   | GPIO. 5 | 5   | 24  |
	 *  |  10 |  12 |    MOSI | ALT0 | 0 | 19 || 20 |   |      | 0v      |     |     |
	 *  |   9 |  13 |    MISO | ALT0 | 0 | 21 || 22 | 0 | IN   | GPIO. 6 | 6   | 25  |
	 *  |  11 |  14 |    SCLK | ALT0 | 0 | 23 || 24 | 1 | OUT  | CE0     | 10  | 8   |
	 *  |     |     |      0v |      |   | 25 || 26 | 1 | OUT  | CE1     | 11  | 7   |
	 *  |   0 |  30 |   SDA.0 |   IN | 1 | 27 || 28 | 1 | IN   | SCL.0   | 31  | 1   |
	 *  |   5 |  21 | GPIO.21 |   IN | 1 | 29 || 30 |   |      | 0v      |     |     |
	 *  |   6 |  22 | GPIO.22 |   IN | 1 | 31 || 32 | 0 | IN   | GPIO.26 | 26  | 12  |
	 *  |  13 |  23 | GPIO.23 |   IN | 0 | 33 || 34 |   |      | 0v      |     |     |
	 *  |  19 |  24 | GPIO.24 |   IN | 0 | 35 || 36 | 0 | IN   | GPIO.27 | 27  | 16  |
	 *  |  26 |  25 | GPIO.25 |   IN | 0 | 37 || 38 | 0 | IN   | GPIO.28 | 28  | 20  |
	 *  |     |     |      0v |      |   | 39 || 40 | 0 | IN   | GPIO.29 | 29  | 21  |
	 *  +-----+-----+---------+------+---+----++----+---+------+---------+-----+-----+
	 *  | BCM | wPi |   Name  | Mode | V | Physical | V | Mode | Name    | wPi | BCM |
	 *  +-----+-----+---------+------+---+---Pi 2---+---+------+---------+-----+-----+
	 * </pre>
	 */
	static abstract class PiBoardInfo extends GenericLinuxArmBoardInfo {
		private String code;
		private String pcbRevision;
		private String manufacturer;
		private String processor;
		// Map from GPIO number to PWM number
		private Map<Integer, Integer> gpioToPwmNumberMapping;

		public PiBoardInfo(String code, String model, String pcbRevision, int memory, String manufacturer,
				String processor) {
			super(MAKE, model, memory);

			this.code = code;
			this.pcbRevision = pcbRevision;
			this.manufacturer = manufacturer;
			this.processor = processor;

			// Read /boot/config.txt to see if the PWM device tree overlay is loaded
			// Can be max one of "dtoverlay=pwm," or "dtoverlay=pwm-2chan,"
			try {
				gpioToPwmNumberMapping = Files.lines(Paths.get("/boot/config.txt"))
						.filter(line -> line.startsWith("dtoverlay=pwm")).findFirst()
						.map(RaspberryPiBoardInfoProvider::extractPwmGpioNumbers).orElse(Collections.emptyMap());
			} catch (IOException e) {
				gpioToPwmNumberMapping = new HashMap<>();
				// Ignore
			}
		}

		public String getCode() {
			return code;
		}

		public String getRevision() {
			return pcbRevision;
		}

		public String getManufacturer() {
			return manufacturer;
		}

		public String getProcessor() {
			return processor;
		}

		@Override
		public MmapGpioInterface createMmapGpio() {
			return new RaspberryPiMmapGpio(processor.equals(BCM2711));
		}

		@Override
		public void populateBoardPinInfo() {
			String[] compatibility = new String[] { MAKE.replace(" ", "").toLowerCase(),
					getModel().replace(" ", "").replace("+", "plus").toLowerCase() };
			// Override for pigpioj remote use cases
			if (!loadBoardPinInfoDefinition(compatibility[0], compatibility[1])) {
				loadBoardPinInfoDefinition(compatibility[0]);
			}
		}

		@Override
		public PinInfo addPwmPinInfo(String header, int gpioNumber, String name, int physicalPin, int pwmChip,
				int pwmNum, Collection<DeviceMode> modes, int chip, int line) {
			if (pwmChip != PinInfo.NOT_DEFINED) {
				// Validate that this GPIO is actually configured for PWM
				Integer pwm_num = gpioToPwmNumberMapping.get(Integer.valueOf(gpioNumber));
				if (pwm_num != null) {
					return super.addPwmPinInfo(header, gpioNumber, name, physicalPin, pwmChip, pwm_num.intValue(),
							modes, chip, line);
				}
			}

			return super.addGpioPinInfo(header, gpioNumber, name, physicalPin, modes, chip, line);
		}

		@Override
		public String getLongName() {
			return getMake() + " Model " + getModel() + " V" + pcbRevision;
		}
	}

	public static class PiBRev1BoardInfo extends PiBoardInfo {
		public PiBRev1BoardInfo(String code, String pcbRevision, int memory, String manufacturer) {
			super(code, MODEL_B, pcbRevision, memory, manufacturer, BCM2835);
		}

		@Override
		public void populateBoardPinInfo() {
			int pin = 1;
			addGeneralPinInfo(pin++, PinInfo.VCC_3V3);
			addGeneralPinInfo(pin++, PinInfo.VCC_5V);
			addGpioPinInfo(0, "SDA1", pin++, PinInfo.DIGITAL_IN_OUT, 0, 0); // I2C SDA
			addGeneralPinInfo(pin++, PinInfo.VCC_5V);
			addGpioPinInfo(1, "SCL1", pin++, PinInfo.DIGITAL_IN_OUT, 0, 1); // I2C SCL
			addGeneralPinInfo(pin++, PinInfo.GROUND);
			addGpioPinInfo(4, "GPIO_GCLK", pin++, PinInfo.DIGITAL_IN_OUT, 0, 4);
			addGpioPinInfo(14, "TXD1", pin++, PinInfo.DIGITAL_IN_OUT, 0, 14); // UART TXD
			addGeneralPinInfo(pin++, PinInfo.GROUND);
			addGpioPinInfo(15, "RXD1", pin++, PinInfo.DIGITAL_IN_OUT, 0, 15); // UART RXD
			addGpioPinInfo(17, "GPIO17", pin++, PinInfo.DIGITAL_IN_OUT, 0, 17);
			// TODO Try enabling sysfs PWM, see
			// http://www.jumpnowtek.com/rpi/Using-the-Raspberry-Pi-Hardware-PWM-timers.html
			// addPwmPinInfo(18, pin++, 0, PinInfo.DIGITAL_IN_OUT_PWM);
			addGpioPinInfo(18, "GPIO18", pin++, PinInfo.DIGITAL_IN_OUT, 0, 18);
			addGpioPinInfo(21, "GPIO21", pin++, PinInfo.DIGITAL_IN_OUT, 0, 21);
			addGeneralPinInfo(pin++, PinInfo.GROUND);
			addGpioPinInfo(22, "GPIO22", pin++, PinInfo.DIGITAL_IN_OUT, 0, 22);
			addGpioPinInfo(23, "GPIO23", pin++, PinInfo.DIGITAL_IN_OUT, 0, 23);
			addGeneralPinInfo(pin++, PinInfo.VCC_3V3);
			addGpioPinInfo(24, "GPIO24", pin++, PinInfo.DIGITAL_IN_OUT, 0, 24);
			addGpioPinInfo(10, "SPI_MOSI", pin++, PinInfo.DIGITAL_IN_OUT, 0, 10); // SPI MOSI
			addGeneralPinInfo(pin++, PinInfo.GROUND);
			addGpioPinInfo(9, "SPI_MISO", pin++, PinInfo.DIGITAL_IN_OUT, 0, 9); // SPI MISO
			addGpioPinInfo(25, "GPIO25", pin++, PinInfo.DIGITAL_IN_OUT, 0, 25);
			addGpioPinInfo(11, "SPI_SCLK", pin++, PinInfo.DIGITAL_IN_OUT, 0, 11); // SPI CLK
			addGpioPinInfo(8, "SPI_CE0_N", pin++, PinInfo.DIGITAL_IN_OUT, 0, 8); // SPI CE0
			addGeneralPinInfo(pin++, PinInfo.GROUND);
			addGpioPinInfo(7, "SPI_CE1_N", pin++, PinInfo.DIGITAL_IN_OUT, 0, 7); // SPI CE1
		}
	}

	public static class PiABRev2BoardInfo extends PiBoardInfo {
		public PiABRev2BoardInfo(String code, String model, int memory, String manufacturer) {
			super(code, model, PCB_REV_2_0, memory, manufacturer, BCM2835);
		}

		@Override
		public void populateBoardPinInfo() {
			int pin = 1;
			addGeneralPinInfo(pin++, PinInfo.VCC_3V3);
			addGeneralPinInfo(pin++, PinInfo.VCC_5V);
			addGpioPinInfo(2, "SDA1", pin++, PinInfo.DIGITAL_IN_OUT, 0, 2); // I2C SDA
			addGeneralPinInfo(pin++, PinInfo.VCC_5V);
			addGpioPinInfo(3, "SCL", pin++, PinInfo.DIGITAL_IN_OUT, 0, 3); // I2C SCL
			addGeneralPinInfo(pin++, PinInfo.GROUND);
			addGpioPinInfo(4, "GPIO_GCLK", pin++, PinInfo.DIGITAL_IN_OUT, 0, 4);
			addGpioPinInfo(14, "TXD1", pin++, PinInfo.DIGITAL_IN_OUT, 0, 14); // UART TXD
			addGeneralPinInfo(pin++, PinInfo.GROUND);
			addGpioPinInfo(15, "RXD1", pin++, PinInfo.DIGITAL_IN_OUT, 0, 15); // UART RXD
			addGpioPinInfo(17, "GPIO17", pin++, PinInfo.DIGITAL_IN_OUT, 0, 17);
			// TODO Try enabling sysfs PWM, see
			// http://www.jumpnowtek.com/rpi/Using-the-Raspberry-Pi-Hardware-PWM-timers.html
			// addPwmPinInfo(18, pin++, 0, PinInfo.DIGITAL_IN_OUT_PWM);
			addGpioPinInfo(18, "GPIO18", pin++, PinInfo.DIGITAL_IN_OUT, 0, 18);
			addGpioPinInfo(27, "GPIO27", pin++, PinInfo.DIGITAL_IN_OUT, 0, 27);
			addGeneralPinInfo(pin++, PinInfo.GROUND);
			addGpioPinInfo(22, "GPIO22", pin++, PinInfo.DIGITAL_IN_OUT, 0, 22);
			addGpioPinInfo(23, "GPIO23", pin++, PinInfo.DIGITAL_IN_OUT, 0, 23);
			addGeneralPinInfo(pin++, PinInfo.VCC_3V3);
			addGpioPinInfo(24, "GPIO24", pin++, PinInfo.DIGITAL_IN_OUT, 0, 24);
			addGpioPinInfo(10, "SPI_MOSI", pin++, PinInfo.DIGITAL_IN_OUT, 0, 10); // SPI MOSI
			addGeneralPinInfo(pin++, PinInfo.GROUND);
			addGpioPinInfo(9, "SPI_MISO", pin++, PinInfo.DIGITAL_IN_OUT, 0, 9); // SPI MISO
			addGpioPinInfo(25, "GPIO25", pin++, PinInfo.DIGITAL_IN_OUT, 0, 25);
			addGpioPinInfo(11, "SPI_SCLK", pin++, PinInfo.DIGITAL_IN_OUT, 0, 11); // SPI CLK
			addGpioPinInfo(8, "SPI_CE0_N", pin++, PinInfo.DIGITAL_IN_OUT, 0, 8); // SPI CE0
			addGeneralPinInfo(pin++, PinInfo.GROUND);
			addGpioPinInfo(7, "SPI_CE1_N", pin++, PinInfo.DIGITAL_IN_OUT, 0, 7); // SPI CE1
		}
	}

	public static class PiABPlusBoardInfo extends PiBoardInfo {
		public static final String P5_HEADER = "P5";

		public PiABPlusBoardInfo(String code, String model, String pcbRevision, int memory, String manufacturer,
				String processor) {
			super(code, model, pcbRevision, memory, manufacturer, processor);
		}

		public void initialisePinsOld() {
			int chip = 0;
			addGeneralPinInfo(1, PinInfo.VCC_3V3);
			addGeneralPinInfo(2, PinInfo.VCC_5V);
			addGpioPinInfo(2, "SDA1", 3, PinInfo.DIGITAL_IN_OUT, chip, 2); // I2C-SDA
			addGeneralPinInfo(4, PinInfo.VCC_5V);
			addGpioPinInfo(3, "SCL1", 5, PinInfo.DIGITAL_IN_OUT, chip, 3); // I2C-SCL
			addGeneralPinInfo(6, PinInfo.GROUND);
			addGpioPinInfo(4, "GPIO_GCLK", 7, PinInfo.DIGITAL_IN_OUT, chip, 4); // GPCLK0
			addGpioPinInfo(14, 8, PinInfo.DIGITAL_IN_OUT); // UART-TXD
			addGeneralPinInfo(9, PinInfo.GROUND);
			addGpioPinInfo(15, 10, PinInfo.DIGITAL_IN_OUT); // UART-RXD
			addGpioPinInfo(17, "GPIO17", 11, PinInfo.DIGITAL_IN_OUT, chip, 17); // Alt4 = SPI1-CE1
			// TODO Try enabling sysfs PWM, see
			// http://www.jumpnowtek.com/rpi/Using-the-Raspberry-Pi-Hardware-PWM-timers.html
			// addPwmPinInfo(18, 12, 0, PinInfo.DIGITAL_IN_OUT_PWM);
			addGpioPinInfo(18, "GPIO18", 12, PinInfo.DIGITAL_IN_OUT, chip, 18); // Alt0 = PCM-CLK, Alt4 = SPI1-CE0, Alt5
																				// = PWM0
			addGpioPinInfo(27, "GPIO27", 13, PinInfo.DIGITAL_IN_OUT, chip, 27);
			addGeneralPinInfo(14, PinInfo.GROUND);
			addGpioPinInfo(22, "GPIO22", 15, PinInfo.DIGITAL_IN_OUT, chip, 22);
			addGpioPinInfo(23, "GPIO23", 16, PinInfo.DIGITAL_IN_OUT, chip, 23);
			addGeneralPinInfo(17, PinInfo.VCC_3V3);
			addGpioPinInfo(24, "GPIO24", 18, PinInfo.DIGITAL_IN_OUT, chip, 24);
			addGpioPinInfo(10, "SPI_MOSI", 19, PinInfo.DIGITAL_IN_OUT, chip, 10);// SPI0-MOSI
			addGeneralPinInfo(20, PinInfo.GROUND);
			addGpioPinInfo(9, "SPI_MISO", 21, PinInfo.DIGITAL_IN_OUT, chip, 9); // SPI0-MISO
			addGpioPinInfo(25, "GPIO25", 22, PinInfo.DIGITAL_IN_OUT, chip, 25);
			addGpioPinInfo(11, "SPI_SCLK", 23, PinInfo.DIGITAL_IN_OUT, chip, 11);// SPI0-CLK
			addGpioPinInfo(8, "SPI_CE0_N", 24, PinInfo.DIGITAL_IN_OUT, chip, 8); // SPI0-CE0
			addGeneralPinInfo(25, PinInfo.GROUND);
			addGpioPinInfo(7, "SPI_CE1_N", 26, PinInfo.DIGITAL_IN_OUT, chip, 7); // SPI0-CE1
			addGeneralPinInfo(27, "ID_SDA", chip, 0);
			addGeneralPinInfo(28, "ID_SCL", chip, 1);
			addGpioPinInfo(5, "GPIO5", 29, PinInfo.DIGITAL_IN_OUT, chip, 5);
			addGeneralPinInfo(30, PinInfo.GROUND);
			addGpioPinInfo(6, "GPIO6", 31, PinInfo.DIGITAL_IN_OUT, chip, 6);
			// TODO Try enabling sysfs PWM, see
			// http://www.jumpnowtek.com/rpi/Using-the-Raspberry-Pi-Hardware-PWM-timers.html
			// addPwmPinInfo(12, 32, 0, PinInfo.DIGITAL_IN_OUT_PWM);
			addGpioPinInfo(12, "GPIO12", 32, PinInfo.DIGITAL_IN_OUT, chip, 12); // Alt0 = PWM0
			// TODO Try enabling sysfs PWM, see
			// http://www.jumpnowtek.com/rpi/Using-the-Raspberry-Pi-Hardware-PWM-timers.html
			// addPwmPinInfo(13, 33, 1, PinInfo.DIGITAL_IN_OUT_PWM);
			addGpioPinInfo(13, "GPIO13", 33, PinInfo.DIGITAL_IN_OUT, chip, 13); // Alt0 = PWM1
			addGeneralPinInfo(34, PinInfo.GROUND);
			// TODO Try enabling sysfs PWM, see
			// http://www.jumpnowtek.com/rpi/Using-the-Raspberry-Pi-Hardware-PWM-timers.html
			// addPwmPinInfo(19, 35, 1, PinInfo.DIGITAL_IN_OUT_PWM);
			addGpioPinInfo(19, "GPIO19", 35, PinInfo.DIGITAL_IN_OUT, chip, 19); // Alt4 = SPI1-MISO, Alt5 = PWM1
			addGpioPinInfo(16, "GPIO16", 36, PinInfo.DIGITAL_IN_OUT, chip, 16); // Alt4 = SPI1-CE2
			addGpioPinInfo(26, "GPIO26", 37, PinInfo.DIGITAL_IN_OUT, chip, 26);
			addGpioPinInfo(20, "GPIO20", 38, PinInfo.DIGITAL_IN_OUT, chip, 20); // Alt4 = SPI1-MOSI
			addGeneralPinInfo(39, PinInfo.GROUND);
			addGpioPinInfo(21, "GPIO21", 40, PinInfo.DIGITAL_IN_OUT, chip, 21); // Alt4 = SPI1-SCLK

			// P5 Header
			addGpioPinInfo(P5_HEADER, 28, 1, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P5_HEADER, 29, 2, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P5_HEADER, 30, 3, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P5_HEADER, 31, 4, PinInfo.DIGITAL_IN_OUT);
		}
	}

	public static class PiComputeModuleBoardInfo extends PiBoardInfo {
		public PiComputeModuleBoardInfo(String code, int memory, String manufacturer, String processor) {
			super(code, COMPUTE_MODULE, PCB_REV_1_2, memory, manufacturer, processor);
		}

		@Override
		public void populateBoardPinInfo() {
			// See
			// https://www.raspberrypi.org/documentation/hardware/computemodule/RPI-CM-DATASHEET-V1_0.pdf
			addGpioPinInfo(0, 3, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(1, 5, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(2, 9, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(3, 11, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(4, 15, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(5, 17, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(6, 21, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(7, 23, PinInfo.DIGITAL_IN_OUT); // SPI0-CE1
			addGpioPinInfo(8, 27, PinInfo.DIGITAL_IN_OUT); // SPI0-CE0
			addGpioPinInfo(9, 29, PinInfo.DIGITAL_IN_OUT); // SPI0-MISO
			addGpioPinInfo(10, 33, PinInfo.DIGITAL_IN_OUT); // SPI0-MOSI
			addGpioPinInfo(11, 35, PinInfo.DIGITAL_IN_OUT); // SPI0-SCLK
			addPwmPinInfo(12, 45, 0, 0, PinInfo.DIGITAL_IN_OUT_PWM); // PWM0
			addPwmPinInfo(13, 47, 0, 1, PinInfo.DIGITAL_IN_OUT_PWM); // PWM1
			// TODO Complete this (up to GPIO45)
		}
	}
}
