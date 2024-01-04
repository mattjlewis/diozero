package com.diozero.internal.soc.rockchip;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     RockchipRK3399MmapGpio.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2023 diozero
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

import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.diozero.api.DeviceMode;
import com.diozero.api.GpioPullUpDown;
import com.diozero.internal.spi.MmapGpioInterface;
import com.diozero.util.MmapIntBuffer;

/*-
 * Pinout: http://www.orangepi.org/html/hardWare/computerAndMicrocontrollers/details/Orange-Pi-5.html
 * Datasheet: ??
 * Technical Reference Manual: https://github.com/Hao-boyan/rk3588-TRM-and-Datasheet
 * WiringOP: https://github.com/orangepi-xunlong/wiringOP/blob/next/wiringPi/wiringPi.h#L83
 *
 * Rockchip RK3588 GPIO has 5 banks, GPIO0 to GPIO4, each bank has 32pins.
 *
 * The GPIO number can be calculated as below, take GPIO4_D5 (PIN22 on 40PIN GPIO) as an example:
 * GPIO4_D5 = 4*32 + 3*8 + 5 = 157
 * (A=0, B=1, C=2, D=3)
 * 
 * gpio0@fd8a0000
 * gpio1@fec20000
 * gpio2@fec30000
 * gpio3@fec40000
 * gpio4@fec50000
 * 
 */
public class RK3588MmapGpio implements MmapGpioInterface {
	private static final String MEM_DEVICE = "/dev/mem";

	private static final int BLOCK_SIZE = 4 * 1024;

	static final int GRF_GPIO2A_IOMUX = 0x0e000;

	// GPIO0 (64K), GPIO1 (64K), GPIO2 (32K), GPIO3 (32K), GPIO4 (32K)
	static final long[] GPIO_BASE = { 0xfd8a_0000L, 0xfec2_0000L, 0xfec3_0000L, 0xfec4_0000L, 0xfec5_0000L };
	// CRU - Clock & Rest Unit (64K)
	static final long CRU_BASE = 0xfd7c_0000L;
	// PMU1 CRU - Clock & Rest Unit (64K)
	static final long PMU1CRU_BASE = 0xfd7f_0000L;
	// GPIO iomux (IOC = I/O Control?, PMU = Performance Monitoring Unit?)
	static final long PMU1_IOC_BASE = 0xfd5f_0000L;
	static final long PMU2_IOC_BASE = 0xfd5f_4000L;
	static final long BUS_IOC_BASE = 0xfd5f_8000L;
	// GPIO pull up/down
	static final long VCCIO1_4_IOC_BASE = 0xfd5f9000L;
	static final long RK3588_VCCIO3_5_IOC_BASE = 0xfd5fa000L;
	private static final long RK3588_VCCIO6_IOC_BASE = 0xfd5fc000L;

	// Data register
	static final int GPIO_SWPORT_DR = 0x0000 / 4;
	// Data direction register
	static final int GPIO_SWPORT_DDR = 0x0008 / 4;
	// External value register
	static final int GPIO_EXT_PORT = 0x0070 / 4;
	// GPIO Mode
	static final int IOMUX_INT_OFFSET = 0x00000 / 4;

	private boolean initialised;
	private MmapIntBuffer[] gpioBanks;
	private MmapIntBuffer pmu1IocMmapIntBuffer;
	private MmapIntBuffer pmu2IocMmapIntBuffer;
	private MmapIntBuffer busIocMmapIntBuffer;
	private MmapIntBuffer cruMmapIntBuffer;
	private MmapIntBuffer pmu1CruMmapIntBuffer;
	private MmapIntBuffer vccIo14IocMmapIntBuffer;
	private MmapIntBuffer vccIo35IocMmapIntBuffer;
	private MmapIntBuffer vccIo6IocMmapIntBuffer;
	private Map<String, DeviceMode> gpioModes;
	private Map<String, Integer> gpioModeValues;

	@Override
	public synchronized void initialise() {
		if (!initialised) {
			gpioBanks = new MmapIntBuffer[GPIO_BASE.length];
			for (int i = 0; i < GPIO_BASE.length; i++) {
				gpioBanks[i] = new MmapIntBuffer(MEM_DEVICE, GPIO_BASE[i], BLOCK_SIZE, ByteOrder.LITTLE_ENDIAN);
			}

			pmu1IocMmapIntBuffer = new MmapIntBuffer(MEM_DEVICE, PMU1_IOC_BASE, BLOCK_SIZE,
					ByteOrder.LITTLE_ENDIAN);
			pmu2IocMmapIntBuffer = new MmapIntBuffer(MEM_DEVICE, PMU2_IOC_BASE, BLOCK_SIZE,
					ByteOrder.LITTLE_ENDIAN);
			busIocMmapIntBuffer = new MmapIntBuffer(MEM_DEVICE, BUS_IOC_BASE, BLOCK_SIZE, ByteOrder.LITTLE_ENDIAN);
			cruMmapIntBuffer = new MmapIntBuffer(MEM_DEVICE, CRU_BASE, BLOCK_SIZE, ByteOrder.LITTLE_ENDIAN);
			pmu1CruMmapIntBuffer = new MmapIntBuffer(MEM_DEVICE, PMU1CRU_BASE, BLOCK_SIZE, ByteOrder.LITTLE_ENDIAN);
			vccIo14IocMmapIntBuffer = new MmapIntBuffer(MEM_DEVICE, VCCIO1_4_IOC_BASE, BLOCK_SIZE,
					ByteOrder.LITTLE_ENDIAN);
			vccIo35IocMmapIntBuffer = new MmapIntBuffer(MEM_DEVICE, RK3588_VCCIO3_5_IOC_BASE, BLOCK_SIZE,
					ByteOrder.LITTLE_ENDIAN);
			vccIo6IocMmapIntBuffer = new MmapIntBuffer(MEM_DEVICE, RK3588_VCCIO6_IOC_BASE, BLOCK_SIZE,
					ByteOrder.LITTLE_ENDIAN);

			gpioModes = new HashMap<>();
			gpioModeValues = new HashMap<>();
			// Only map the exposed GPIOs
			// TODO Mapping
		}
	}

	@Override
	public void close() {
		if (gpioBanks != null) {
			for (int i = 0; i < gpioBanks.length; i++) {
				gpioBanks[i].close();
			}
			gpioBanks = null;
		}
		if (pmu1IocMmapIntBuffer != null) {
			pmu1IocMmapIntBuffer.close();
			pmu1IocMmapIntBuffer = null;
		}
		if (pmu2IocMmapIntBuffer != null) {
			pmu2IocMmapIntBuffer.close();
			pmu2IocMmapIntBuffer = null;
		}
		if (busIocMmapIntBuffer != null) {
			busIocMmapIntBuffer.close();
			busIocMmapIntBuffer = null;
		}
		if (cruMmapIntBuffer != null) {
			cruMmapIntBuffer.close();
			cruMmapIntBuffer = null;
		}
		if (pmu1CruMmapIntBuffer != null) {
			pmu1CruMmapIntBuffer.close();
			pmu1CruMmapIntBuffer = null;
		}
		if (vccIo14IocMmapIntBuffer != null) {
			vccIo14IocMmapIntBuffer.close();
			vccIo14IocMmapIntBuffer = null;
		}
		if (vccIo35IocMmapIntBuffer != null) {
			vccIo35IocMmapIntBuffer.close();
			vccIo35IocMmapIntBuffer = null;
		}
		if (vccIo6IocMmapIntBuffer != null) {
			vccIo6IocMmapIntBuffer.close();
			vccIo6IocMmapIntBuffer = null;
		}
	}

	@Override
	public DeviceMode getMode(int gpio) {
		final int bank = gpio >> 5;
		final int bank_offset = gpio % 32;
		final int bus_ioc_shift = (gpio % 4) << 2;
		final int bus_ioc_int_offset = (bank << 3) + (bank_offset >> 2);

		int mode = (busIocMmapIntBuffer.get(bus_ioc_int_offset) >> bus_ioc_shift) & 0x0f;

		if (mode == 0) {
			// GPIO - determine if input or output
			final int ddr_int_offset = RK3588MmapGpio.GPIO_SWPORT_DDR + (bank_offset >> 4);
			final int ddr_shift = bank_offset % 16;
			return (gpioBanks[bank].get(ddr_int_offset) & (1 << ddr_shift)) == 0 ? DeviceMode.DIGITAL_INPUT
					: DeviceMode.DIGITAL_OUTPUT;
		}

		return gpioModes.getOrDefault(gpio + "-" + mode, DeviceMode.UNKNOWN);
	}

	@Override
	public void setMode(int gpio, DeviceMode deviceMode) {
		int mode_mask = 0;
		if (deviceMode != DeviceMode.DIGITAL_INPUT & deviceMode != DeviceMode.DIGITAL_OUTPUT) {
			Integer i = gpioModeValues.get(gpio + "-" + deviceMode);
			if (i == null) {
				throw new IllegalArgumentException("Invalid mode " + deviceMode + " for GPIO " + gpio);
			}
			mode_mask = i.intValue();
		}

		setModeUnchecked(gpio, mode_mask);

		if (mode_mask == 0) {

		}
	}

	@Override
	public void setModeUnchecked(int gpio, int mode) {
	}

	@Override
	public Optional<GpioPullUpDown> getPullUpDown(int gpio) {
		return Optional.empty();
	}

	@Override
	public void setPullUpDown(int gpio, GpioPullUpDown pud) {
	}

	@Override
	public boolean gpioRead(int gpio) {
		return false;
	}

	@Override
	public void gpioWrite(int gpio, boolean value) {
	}

	public static void main(String[] args) {
	}
}
