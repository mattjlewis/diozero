package com.diozero.internal.board.beaglebone;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     BeagleBoneBoardInfoProvider.java
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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.tinylog.Logger;

import com.diozero.api.PinInfo;
import com.diozero.internal.board.GenericLinuxArmBoardInfo;
import com.diozero.internal.spi.BoardInfoProvider;
import com.diozero.sbc.BoardInfo;
import com.diozero.sbc.LocalSystemInfo;

/*-
 * https://github.com/derekmolloy/boneDeviceTree/raw/master/docs/BeagleboneBlackP8HeaderTable.pdf
 * https://github.com/derekmolloy/boneDeviceTree/raw/master/docs/BeagleboneBlackP9HeaderTable.pdf
 */
public class BeagleBoneBoardInfoProvider implements BoardInfoProvider {
	public static final String MAKE = "BeagleBone";

	static final class PwmModule {
		final int subsystem;
		final int address;
		final String suffix;

		PwmModule(int subsystem, int address, String suffix) {
			this.subsystem = subsystem;
			this.address = address;
			this.suffix = suffix;
		}
	}

	// private static final String BBB_HARDWARE_ID = "Generic AM33XX";
	static final Map<String, PwmModule> PWM_MODULES;
	static {
		// See https://github.com/julianduque/beaglebone-io/blob/master/lib/bbb-pins.js
		// and
		// https://github.com/julianduque/beaglebone-io/blob/master/lib/pwm-output.js
		/*-
		var pwmSubSystems = {
		subSystem0: { addr: 0x48300000 },
		subSystem1: { addr: 0x48302000 },
		subSystem2: { addr: 0x48304000 }
		};
		
		var pwmModules = {
		ecap0: { subSystem: pwmSubSystems.subSystem0, addr: 0x48300100, suffix: 'ecap' },
		ehrpwm0: { subSystem: pwmSubSystems.subSystem0, addr: 0x48300200, suffix: 'pwm' },
		ehrpwm1: { subSystem: pwmSubSystems.subSystem1, addr: 0x48302200, suffix: 'pwm' },
		ehrpwm2: { subSystem: pwmSubSystems.subSystem2, addr: 0x48304200, suffix: 'pwm' }
		};
		
		var pwmPins = {
		p8_13: { module: pwmModules.ehrpwm2, channel: 1 },
		p8_19: { module: pwmModules.ehrpwm2, channel: 0 },
		p9_14: { module: pwmModules.ehrpwm1, channel: 0 },
		p9_16: { module: pwmModules.ehrpwm1, channel: 1 },
		p9_21: { module: pwmModules.ehrpwm0, channel: 1 },
		p9_22: { module: pwmModules.ehrpwm0, channel: 0 },
		p9_42: { module: pwmModules.ecap0,   channel: 0 }
		};
		 */
		// Map "<header>_<pin_no>" to PWM Subsystem
		Map<String, PwmModule> map = new HashMap<>();
		int subsystem0 = 0x48300000;
		int subsystem1 = 0x48302000;
		int subsystem2 = 0x48304000;
		PwmModule ecap0 = new PwmModule(subsystem0, 0x48300100, "ecap");
		PwmModule ehrpwm0 = new PwmModule(subsystem0, 0x48300200, "pwm");
		PwmModule ehrpwm1 = new PwmModule(subsystem1, 0x48302200, "pwm");
		PwmModule ehrpwm2 = new PwmModule(subsystem2, 0x48304200, "pwm");
		map.put("P9_42", ecap0); // ECAP0PWMA
		map.put("P9_22", ehrpwm0); // EHRPWM0A
		map.put("P9_21", ehrpwm0); // EHRPWM0B
		map.put("P9_14", ehrpwm1); // EHRPWM1A
		map.put("P9_16", ehrpwm1); // EHRPWM1B
		map.put("P8_19", ehrpwm2); // EHRPWM21
		map.put("P8_13", ehrpwm2); // EHRPWM2B

		PWM_MODULES = Collections.unmodifiableMap(map);
	}

	@Override
	public BoardInfo lookup(LocalSystemInfo localSysInfo) {
		String model = localSysInfo.getModel();
		if (model != null && model.contains(MAKE)) {
			model = model.substring(model.lastIndexOf(' ') + 1);
			return new BeagleBoneBlackBoardInfo(localSysInfo, model);
		}
		return null;
	}

	/**
	 * Also works on the BeagleBone Green
	 */
	public static class BeagleBoneBlackBoardInfo extends GenericLinuxArmBoardInfo {
		public static final String P9_HEADER = "P9";
		public static final String P8_HEADER = "P8";

		private static final int MEMORY = 512_000;
		private static final float ADC_VREF = 1.8f;

		public BeagleBoneBlackBoardInfo(LocalSystemInfo localSysInfo, String model) {
			super(MAKE, model, MEMORY, ADC_VREF);
		}

		public void oldPopulateBoardPinInfo() {
			// Kept for info only - now externalised to a file
			addGpioPinInfo(P9_HEADER, 60, 12, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P9_HEADER, 48, 15, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P9_HEADER, 49, 23, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P9_HEADER, 117, 25, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P9_HEADER, 115, 27, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P9_HEADER, 112, 30, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P9_HEADER, 20, 41, PinInfo.DIGITAL_IN_OUT);

			addGpioPinInfo(P8_HEADER, 66, 7, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P8_HEADER, 67, 8, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P8_HEADER, 69, 9, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P8_HEADER, 68, 10, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P8_HEADER, 45, 11, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P8_HEADER, 44, 12, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P8_HEADER, 26, 14, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P8_HEADER, 47, 15, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P8_HEADER, 46, 16, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P8_HEADER, 27, 17, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P8_HEADER, 65, 18, PinInfo.DIGITAL_IN_OUT);
			addGpioPinInfo(P8_HEADER, 61, 26, PinInfo.DIGITAL_IN_OUT);

			/*- To enable: sudo sh -c "echo 'BB-ADC' > /sys/devices/platform/bone_capemgr/slots" */
			addAdcPinInfo(P9_HEADER, 0, "AIN0", 39);
			addAdcPinInfo(P9_HEADER, 1, "AIN1", 40);
			addAdcPinInfo(P9_HEADER, 2, "AIN2", 37);
			addAdcPinInfo(P9_HEADER, 3, "AIN3", 38);
			addAdcPinInfo(P9_HEADER, 4, "AIN4", 33);
			addAdcPinInfo(P9_HEADER, 5, "AIN5", 36);
			addAdcPinInfo(P9_HEADER, 6, "AIN6", 35);

			// BB-PWM0,BB-PWM1,BB-PWM2
			addPwmPinInfo(P9_HEADER, PinInfo.NOT_DEFINED, "EHRPWM1A", 14, 0, 0, PinInfo.DIGITAL_IN_OUT_PWM);
			addPwmPinInfo(P9_HEADER, PinInfo.NOT_DEFINED, "EHRPWM1B", 16, 0, 1, PinInfo.DIGITAL_IN_OUT_PWM);
			addPwmPinInfo(P8_HEADER, PinInfo.NOT_DEFINED, "EHRPWM2A", 13, 0, 3, PinInfo.DIGITAL_IN_OUT_PWM);
			addPwmPinInfo(P8_HEADER, PinInfo.NOT_DEFINED, "EHRPWM2B", 19, 0, 4, PinInfo.DIGITAL_IN_OUT_PWM);
		}

		/*-
		 * Alternatively override addPwmPinInfo:
		@Override
		public PinInfo addPwmPinInfo(String header, int gpioNumber, String name, int physicalPin, int pwmChip,
				int pwmNum, Collection<DeviceMode> modes, int chip, int line) {
		}
		*/

		@Override
		public int getPwmChipNumberOverride(PinInfo pinInfo) {
			// PWM chip number for a GPIO can change between boots

			/*-
			ecap0:   /sys/devices/platform/ocp/0x48300000.epwmss/0x48300100.ecap/pwm
			ehrpwm0: /sys/devices/platform/ocp/0x48300000.epwmss/0x48300200.pwm/pwm
			ehrpwm1: /sys/devices/platform/ocp/0x48302000.epwmss/0x48302200.pwm/pwm
			ehrpwm2: /sys/devices/platform/ocp/0x48304000.epwmss/0x48304200.pwm/pwm
			*/
			String lookup = pinInfo.getHeader() + "_" + pinInfo.getPhysicalPin();
			PwmModule pwm_module = PWM_MODULES.get(lookup);
			if (pwm_module == null) {
				Logger.warn("PWM module not found for '" + lookup + "'");
				return -1;
			}

			Path chip_path = Paths.get(String.format("/sys/devices/platform/ocp/%d.epwmss/%d.%s/pwm",
					Integer.valueOf(pwm_module.subsystem), Integer.valueOf(pwm_module.address), pwm_module.suffix));
			int pwm_chip = -1;
			// FIXME Treat as a stream?
			try (DirectoryStream<Path> dirs = Files.newDirectoryStream(chip_path, "pwm*")) {
				for (Path p : dirs) {
					String dir = p.getFileName().toString();
					Logger.info("Got {}", dir);
					pwm_chip = Integer.parseInt(dir.substring(dir.length() - 1));
					Logger.info("Found pwmChip {}", Integer.valueOf(pwm_chip));
				}
			} catch (IOException e) {
				Logger.error(e, "Error: " + e);
			}

			return pwm_chip;
		}

		/*-
		@Override
		public MmapGpioInterface createMmapGpio() {
			return new BeagleBoneBlackMmapGpio();
		}
		*/
	}
}
