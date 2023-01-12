package com.diozero.internal.board.soc.rockchip;

import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import org.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.GpioPullUpDown;
import com.diozero.internal.spi.MmapGpioInterface;
import com.diozero.util.MmapIntBuffer;
import com.diozero.util.SleepUtil;

/*-
 * Wiki: https://wiki.radxa.com/Rockpi4/hardware/gpio
 * Datasheet: https://dl.radxa.com/rockpi4/docs/hw/datasheets/Rockchip%20RK3399-T%20Datasheet%20V1.0-20210818.pdf
 * WiringX: https://github.com/wiringX/wiringX/blob/master/src/soc/rockchip/rk3399.c
 *
 * Rockchip RK3399 GPIO has 5 banks, GPIO0 to GPIO4, each bank has 32pins.
 *
 * For Rockchip 4.4 kernel, the GPIO number can be calculated as below, take GPIO4_D5 (PIN22 on 40PIN GPIO) as an example:
 * GPIO4_D5 = 4*32 + 3*8 + 5 = 157
 * (A=0, B=1, C=2, D=3)
 * 
 * gpio0@ff720000
 * gpio1@ff730000
 * gpio2@ff780000
 * gpio3@ff788000
 * gpio4@ff790000
 * 
 * +-----+----------+------+---+--------+----------+--------+---+------+----------+-----+
 * + GP# +     Name + Mode + V +  gpiod + Physical + gpiod  + V + Mode + Name     + GP# +
 * +-----+----------+------+---+--------+----------+--------+---+------+----------+-----+
 * |     |      3v3 |      |   |        |  1 || 2  |        |   |      | 5v       |     |
 * |  71 | GPIO2_A7 |   In | 0 |  2:7   |  3 || 4  |        |   |      | 5v       |     |
 * |  72 | GPIO2_B0 |   In | 0 |  2:8   |  5 || 6  |        |   |      | GND      |     |
 * |  75 | GPIO2_B3 |   In | 0 |  2:11  |  7 || 8  |  4:20  | 1 | UART | TXD2     | 148 |
 * |     |      GND |      |   |        |  9 || 10 |  4:19  | 1 | UART | RXD2     | 147 |
 * | 146 |     PWM0 |   In | 0 |  4:18  | 11 || 12 |  4:3   | 1 | I2S  | GPIO4_A3 | 131 |
 * | 150 |     PWM1 |   In | 0 |  4:22  | 13 || 14 |        |   |      | GND      |     |
 * | 149 | GPIO4_C5 |   In | 0 |  4:21  | 15 || 16 |  4:26  | 1 | In   | GPIO4_D2 | 154 |
 * |     |      3v3 |      |   |        | 17 || 18 |  4:28  | 0 | In   | GPIO4_D4 | 156 |
 * |  40 |  SPI1_TX |  SPI | 0 |  1:8   | 19 || 20 |        |   |      | GND      |     |
 * |  39 |  SPI1_RX |  SPI | 1 |  1:7   | 21 || 22 |  4:29  | 0 | In   | GPIO4_D5 | 157 |
 * |  41 | SPI1_CLK |  SPI | 1 |  1:9   | 23 || 24 |  1:10  | 1 | SPI  | SPI1_CS0 | 42  |
 * |     |      GND |      |   |        | 25 || 26 |        |   | In   | ADC_IN0  | 0   |
 * |  64 | GPIO2_A0 |   In | 0 |  2:0   | 27 || 28 |  2:1   | 0 | In   | GPIO2_A1 | 65  |
 * |  74 | GPIO2_B2 |   In | 0 |  2:10  | 29 || 30 |        |   |      | GND      |     |
 * |  73 | GPIO2_B1 |   In | 0 |  2:9   | 31 || 32 |  3:0   | 0 | In   | GPIO3_C0 | 112 |
 * |  76 | GPIO2_B4 |   In | 0 |  2:12  | 33 || 34 |        |   |      | GND      |     |
 * | 133 | GPIO4_A5 |  I2S | 0 |  4:5   | 35 || 36 |  4:4   | 0 | I2S  | GPIO4_A4 | 132 |
 * | 158 | GPIO4_D6 |   In | 0 |  4:30  | 37 || 38 |  4:6   | 1 | I2S  | GPIO4_A6 | 134 |
 * |     |      GND |      |   |        | 39 || 40 |  4:7   | 0 | I2S  | GPIO4_A7 | 135 |
 * +-----+----------+------+---+--------+----------+--------+---+------+----------+-----+
 * 
 * Note: to use GPIO 112, the status LED has to be disabled (i.e. LED’s trigger set to none).
 * Otherwise, the operation will have no effect and no error will be returned.
 * This workaround is required on vendor 4.4 kernel.
 */
public class RockchipRK3399MmapGpio implements MmapGpioInterface {
	private static final String GPIOMEM_DEVICE = "/dev/mem";
	private static final int GRF_GPIO2A_IOMUX = 0x0e000;
	// GRF - General Register Files (64K)
	private static final long GRF_MEM_OFFSET = 0xff77_0000L + GRF_GPIO2A_IOMUX;
	// PMU - Power Management Unit (64K)
	private static final long PMUGRF_MEM_OFFSET = 0xff32_0000L;
	// PMU CRU - Clock & Rest Unit (64K)
	private static final long PMUCRU_MEM_OFFSET = 0xff75_0000L;
	// CRU - Clock & Rest Unit (64K)
	private static final long CRU_MEM_OFFSET = 0xff76_0000L;
	// GPIO0 (64K), GPIO1 (64K), GPIO2 (32K), GPIO3 (32K), GPIO4 (32K)
	private static final long[] GPIOMEM_OFFSETS = { 0xff72_0000L, 0xff73_0000L, 0xff78_0000L, 0xff78_8000L,
			0xff79_0000L };

	private static final int MEM_INFO = 1024;
	private static final int BLOCK_SIZE = 4 * MEM_INFO;

	// Data register
	private static final int GPIO_SWPORTA_DR = 0x0000 / 4;
	// Data direction register
	private static final int GPIO_SWPORTA_DDR = 0x0004 / 4;
	// External value register
	private static final int GPIO_EXT_PORTA = 0x0050 / 4;
	// GPIO Mode
	private static final int IOMUX_INT_OFFSET = 0x00000 / 4;
	// GPIO PU/PD
	private static final int PUD_INT_OFFSET = 0x00040 / 4;

	// Internal clock gating register for GPIO banks 0-1 [bits 3-4]
	private static final int PMUCRU_CLKGATE_CON1_INT_OFFSET = 0x0104 / 4;
	// Internal clock gating register for GPIO banks 2-4 [bits 3-5]
	private static final int CRU_CLKGATE_CON31_INT_OFFSET = 0x037c / 4;

	private boolean initialised;
	private MmapIntBuffer grfMmapIntBuffer;
	private MmapIntBuffer pmuGrfMmapIntBuffer;
	private MmapIntBuffer pmuCruMmapIntBuffer;
	private MmapIntBuffer cruMmapIntBuffer;
	private MmapIntBuffer[] gpioBanks;
	private Map<String, DeviceMode> gpioModes;
	private Map<String, Integer> gpioModeValues;

	@Override
	public synchronized void initialise() {
		if (!initialised) {
			grfMmapIntBuffer = new MmapIntBuffer(GPIOMEM_DEVICE, GRF_MEM_OFFSET, BLOCK_SIZE, ByteOrder.LITTLE_ENDIAN);
			pmuGrfMmapIntBuffer = new MmapIntBuffer(GPIOMEM_DEVICE, PMUGRF_MEM_OFFSET, BLOCK_SIZE,
					ByteOrder.LITTLE_ENDIAN);
			pmuCruMmapIntBuffer = new MmapIntBuffer(GPIOMEM_DEVICE, PMUCRU_MEM_OFFSET, BLOCK_SIZE,
					ByteOrder.LITTLE_ENDIAN);
			cruMmapIntBuffer = new MmapIntBuffer(GPIOMEM_DEVICE, CRU_MEM_OFFSET, BLOCK_SIZE, ByteOrder.LITTLE_ENDIAN);

			gpioBanks = new MmapIntBuffer[GPIOMEM_OFFSETS.length];
			for (int i = 0; i < GPIOMEM_OFFSETS.length; i++) {
				gpioBanks[i] = new MmapIntBuffer(GPIOMEM_DEVICE, GPIOMEM_OFFSETS[i], BLOCK_SIZE,
						ByteOrder.LITTLE_ENDIAN);
			}

			gpioModes = new HashMap<>();
			gpioModeValues = new HashMap<>();
			// Only map the exposed GPIOs
			// Note conflicts between RK3399 datasheet and Wiki page
			// GPIO1_A7 (39)
			gpioModes.put("39-1", DeviceMode.SERIAL);
			gpioModeValues.put("39-" + DeviceMode.SERIAL, Integer.valueOf(1));
			gpioModes.put("39-2", DeviceMode.SPI);
			gpioModeValues.put("39-" + DeviceMode.SPI, Integer.valueOf(2));
			// GPIO1_B0 (40)
			gpioModes.put("40-1", DeviceMode.SERIAL);
			gpioModeValues.put("40-" + DeviceMode.SERIAL, Integer.valueOf(1));
			gpioModes.put("40-2", DeviceMode.SPI);
			gpioModeValues.put("40-" + DeviceMode.SPI, Integer.valueOf(2));
			// GPIO1_B1 (41)
			gpioModes.put("41-2", DeviceMode.SPI);
			gpioModeValues.put("41-" + DeviceMode.SPI, Integer.valueOf(2));
			// GPIO1_B2 (42)
			gpioModes.put("42-2", DeviceMode.SPI);
			gpioModeValues.put("42-" + DeviceMode.SPI, Integer.valueOf(2));
			// GPIO2_A0 (64)
			gpioModes.put("64-2", DeviceMode.I2C);
			gpioModeValues.put("64-" + DeviceMode.I2C, Integer.valueOf(2));
			// GPIO2_A1 (65)
			gpioModes.put("65-2", DeviceMode.I2C);
			gpioModeValues.put("65-" + DeviceMode.I2C, Integer.valueOf(2));
			// GPIO2_A7 (71)
			gpioModes.put("71-2", DeviceMode.I2C);
			gpioModeValues.put("71-" + DeviceMode.I2C, Integer.valueOf(2));
			// GPIO2_B0 (72)
			gpioModes.put("72-2", DeviceMode.I2C);
			gpioModeValues.put("72-" + DeviceMode.I2C, Integer.valueOf(2));
			// GPIO2_B1 (73)
			gpioModes.put("73-1", DeviceMode.SPI);
			gpioModeValues.put("73-" + DeviceMode.SPI, Integer.valueOf(1));
			gpioModes.put("73-2", DeviceMode.I2C);
			gpioModeValues.put("73-" + DeviceMode.I2C, Integer.valueOf(2));
			// GPIO2_B2 (74)
			gpioModes.put("74-1", DeviceMode.SPI);
			gpioModeValues.put("74-" + DeviceMode.SPI, Integer.valueOf(1));
			gpioModes.put("74-2", DeviceMode.I2C);
			gpioModeValues.put("74-" + DeviceMode.I2C, Integer.valueOf(2));
			// GPIO2_B3 (75)
			gpioModes.put("75-1", DeviceMode.SPI);
			gpioModeValues.put("75-" + DeviceMode.SPI, Integer.valueOf(1));
			// GPIO2_B4 (76)
			gpioModes.put("76-1", DeviceMode.SPI);
			gpioModeValues.put("76-" + DeviceMode.SPI, Integer.valueOf(1));
			// GPIO3_C0 (112)
			gpioModes.put("112-2", DeviceMode.SERIAL);
			gpioModeValues.put("112-" + DeviceMode.SERIAL, Integer.valueOf(2));
			// GPIO4_A3 (131)
			gpioModes.put("131-1", DeviceMode.I2S);
			gpioModeValues.put("131-" + DeviceMode.I2S, Integer.valueOf(1));
			// GPIO4_A4 (132)
			gpioModes.put("132-1", DeviceMode.I2S);
			gpioModeValues.put("132-" + DeviceMode.I2S, Integer.valueOf(1));
			// GPIO4_A5 (133)
			gpioModes.put("133-1", DeviceMode.I2S);
			gpioModeValues.put("133-" + DeviceMode.I2S, Integer.valueOf(1));
			// GPIO4_A6 (134)
			gpioModes.put("134-1", DeviceMode.I2S);
			gpioModeValues.put("134-" + DeviceMode.I2S, Integer.valueOf(1));
			// GPIO4_A7 (135)
			gpioModes.put("135-1", DeviceMode.I2S);
			gpioModeValues.put("135-" + DeviceMode.I2S, Integer.valueOf(1));
			// GPIO4_C2 (146)
			gpioModes.put("146-1", DeviceMode.PWM_OUTPUT);
			gpioModeValues.put("146-" + DeviceMode.PWM_OUTPUT, Integer.valueOf(1));
			// GPIO4_C3 (147)
			gpioModes.put("147-1", DeviceMode.SERIAL);
			gpioModeValues.put("147-" + DeviceMode.SERIAL, Integer.valueOf(1));
			gpioModes.put("147-2", DeviceMode.SERIAL);
			gpioModeValues.put("147-" + DeviceMode.SERIAL, Integer.valueOf(2));
			// GPIO4_C4 (148)
			gpioModes.put("148-1", DeviceMode.SERIAL);
			gpioModeValues.put("148-" + DeviceMode.SERIAL, Integer.valueOf(1));
			gpioModes.put("148-2", DeviceMode.SERIAL);
			gpioModeValues.put("148-" + DeviceMode.SERIAL, Integer.valueOf(2));
			// GPIO4_C5 (149) - spdif_tx on func 1
			// GPIO4_C6 (150)
			gpioModes.put("150-1", DeviceMode.PWM_OUTPUT);
			gpioModeValues.put("150-" + DeviceMode.PWM_OUTPUT, Integer.valueOf(1));
			// GPIO4_D2 (154) - GPIO only (no iomux - bits 15:4 reserved)
			// GPIO4_D4 (156) - GPIO only (no iomux - bits 15:4 reserved)
			// GPIO4_D5 (157) - GPIO only (no iomux - bits 15:4 reserved)
			// GPIO4_D6 (158) - GPIO only (no iomux - bits 15:4 reserved)
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
		if (pmuGrfMmapIntBuffer != null) {
			pmuGrfMmapIntBuffer.close();
			pmuGrfMmapIntBuffer = null;
		}
		if (grfMmapIntBuffer != null) {
			grfMmapIntBuffer.close();
			grfMmapIntBuffer = null;
		}
		if (cruMmapIntBuffer != null) {
			cruMmapIntBuffer.close();
			cruMmapIntBuffer = null;
		}
		if (pmuCruMmapIntBuffer != null) {
			pmuCruMmapIntBuffer.close();
			pmuCruMmapIntBuffer = null;
		}
	}

	@Override
	public DeviceMode getMode(int gpio) {
		/*-
		 * iomux control
		 * Name                Offset (16) Offset (10) GPIOs    Int Offset (16)
		 * PMUGRF_GPIO0A_IOMUX 0x00000     0           0..7     0x00000
		 * PMUGRF_GPIO0B_IOMUX 0x00004     4           8..15    0x00001
		 * PMUGRF_GPIO0C_IOMUX 0x00008     8           16..23   0x00002 - UNDEFINED
		 * PMUGRF_GPIO0D_IOMUX 0x0000c     12          24..31   0x00003 - UNDEFINED
		 * PMUGRF_GPIO1A_IOMUX 0x00010     16          32..39   0x00004
		 * PMUGRF_GPIO1B_IOMUX 0x00014     20          40..47   0x00005
		 * PMUGRF_GPIO1C_IOMUX 0x00018     24          48..55   0x00006
		 * PMUGRF_GPIO1D_IOMUX 0x0001c     28          56..63   0x00007
		 * GRF_GPIO2A_IOMUX    0x0e000     57344       64..71   0x03800
		 * GRF_GPIO2B_IOMUX    0x0e004     57348       72..79   0x03801
		 * GRF_GPIO2C_IOMUX    0x0e008     57352       80..87   0x03802
		 * GRF_GPIO2D_IOMUX    0x0e00c     57356       88..95   0x03803
		 * GRF_GPIO3A_IOMUX    0x0e010     57360       96..103  0x03804
		 * GRF_GPIO3B_IOMUX    0x0e014     57364       104..111 0x03805
		 * GRF_GPIO3C_IOMUX    0x0e018     57368       112..119 0x03806
		 * GRF_GPIO3D_IOMUX    0x0e01c     57372       120..127 0x03807
		 * GRF_GPIO4A_IOMUX    0x0e020     57376       128..135 0x03808
		 * GRF_GPIO4B_IOMUX    0x0e024     57380       136..143 0x03809
		 * GRF_GPIO4C_IOMUX    0x0e028     57384       144..151 0x0380a
		 * GRF_GPIO4D_IOMUX    0x0e02c     57388       152..159 0x0380b
		 */
		final int bank = gpio >> 5;
		final int shift = (gpio % 8) << 1;
		final int bank_offset = gpio % 32;
		int mode = 0;
		// GPIOs 154 onwards are GPIO in/out only
		if (gpio < 154) {
			int mode_int_offset;
			MmapIntBuffer int_buffer;
			if (bank < 2) {
				mode_int_offset = IOMUX_INT_OFFSET + bank * 4 + bank_offset / 8;
				int_buffer = pmuGrfMmapIntBuffer;
			} else {
				mode_int_offset = IOMUX_INT_OFFSET + (bank - 2) * 4 + bank_offset / 8;
				int_buffer = grfMmapIntBuffer;
			}
			mode = (int_buffer.get(mode_int_offset) >> shift) & 0b11;
		}

		if (mode == 0) {
			// GPIO - determine if input or output
			return (gpioBanks[bank].get(GPIO_SWPORTA_DDR) & (1 << bank_offset)) == 0 ? DeviceMode.DIGITAL_INPUT
					: DeviceMode.DIGITAL_OUTPUT;
		}

		return gpioModes.getOrDefault(gpio + "-" + mode, DeviceMode.UNKNOWN);
	}

	@Override
	public void setMode(int gpio, DeviceMode deviceMode) {
		int mode_mask = 0;
		// GPIOs 154 onwards are GPIO in/out only
		if (gpio < 154) {
			if (deviceMode != DeviceMode.DIGITAL_INPUT & deviceMode != DeviceMode.DIGITAL_OUTPUT) {
				Integer i = gpioModeValues.get(gpio + "-" + deviceMode);
				if (i == null) {
					throw new IllegalArgumentException("Invalid mode " + deviceMode + " for GPIO " + gpio);
				}
				mode_mask = i.intValue();
			}

			setModeUnchecked(gpio, mode_mask);
		}

		// GPIO Input or Output mode
		if (mode_mask == 0) {
			final int bank = gpio >> 5;
			final int bank_offset = gpio % 32;

			/*-
			 * cru_reg = (volatile unsigned int *)(cru_register_virtual_address + pin->cru.offset);
			 * HIGH = disable / LOW = enable the clock for the entire GPIO bank
			 * *cru_reg = REGISTER_CLEAR_BITS(cru_reg, pin->cru.bit, 1);
			 */
			int int_offset;
			MmapIntBuffer int_buffer;
			int shift;
			if (bank < 2) {
				int_offset = PMUCRU_CLKGATE_CON1_INT_OFFSET;
				int_buffer = pmuCruMmapIntBuffer;
				shift = bank + 3;
			} else {
				int_offset = CRU_CLKGATE_CON31_INT_OFFSET;
				int_buffer = cruMmapIntBuffer;
				shift = bank + 1;
			}
			int cru_reg = int_buffer.get(int_offset);
			// Set to low to enable the clock for GPIO bank - unclear why we need to do this
			cru_reg &= ~(0b1 << shift);
			// Set the write enable bit
			cru_reg |= 0b1 << (shift + 16);
			int_buffer.put(int_offset, cru_reg);

			int reg_val = gpioBanks[bank].get(GPIO_SWPORTA_DDR);
			if (deviceMode == DeviceMode.DIGITAL_INPUT) {
				reg_val &= ~(0b1 << bank_offset);
			} else {
				reg_val |= 0b1 << bank_offset;
			}
			gpioBanks[bank].put(GPIO_SWPORTA_DDR, reg_val);
		}
	}

	@Override
	public void setModeUnchecked(int gpio, int mode) {
		final int bank = gpio >> 5;
		final int shift = (gpio % 8) << 1;
		final int bank_offset = gpio % 32;

		int mode_int_offset;
		MmapIntBuffer int_buffer;
		if (bank < 2) {
			mode_int_offset = IOMUX_INT_OFFSET + bank * 4 + bank_offset / 8;
			int_buffer = pmuGrfMmapIntBuffer;
		} else {
			mode_int_offset = IOMUX_INT_OFFSET + (bank - 2) * 4 + bank_offset / 8;
			int_buffer = grfMmapIntBuffer;
		}

		int reg_val = int_buffer.get(mode_int_offset);
		reg_val &= ~(0b11 << shift);
		// Set the write enable bits
		reg_val |= ((mode & 0b11) << shift) | (0b11 << (shift + 16));
		int_buffer.put(mode_int_offset, reg_val);
	}

	public GpioPullUpDown getPullUpDown(int gpio) {
		final int bank = gpio >> 5;
		final int shift = (gpio % 8) << 1;
		final int bank_offset = gpio % 32;

		if (bank <= 2) {
			// PU/PD control only available for GPIO2A onwards
			return GpioPullUpDown.NONE;
		}

		int pud_int_offset;
		MmapIntBuffer int_buffer;
		if (bank < 2) {
			pud_int_offset = PUD_INT_OFFSET + bank * 4 + bank_offset / 8;
			int_buffer = pmuGrfMmapIntBuffer;
		} else {
			pud_int_offset = PUD_INT_OFFSET + (bank - 2) * 4 + bank_offset / 8;
			int_buffer = grfMmapIntBuffer;
		}

		int pud_val = (int_buffer.get(pud_int_offset) >> shift) & 0b11;
		GpioPullUpDown pud;
		if (gpio < 32 || (gpio >= 80 && gpio < 96)) {
			switch (pud_val) {
			case 0b01:
				pud = GpioPullUpDown.PULL_DOWN;
				break;
			case 0b11:
				pud = GpioPullUpDown.PULL_UP;
				break;
			default:
				pud = GpioPullUpDown.NONE;
			}
		} else {
			switch (pud_val) {
			case 0b01:
				pud = GpioPullUpDown.PULL_UP;
				break;
			case 0b10:
				pud = GpioPullUpDown.PULL_DOWN;
				break;
			default:
				pud = GpioPullUpDown.NONE;
			}
		}

		return pud;
	}

	@Override
	public void setPullUpDown(int gpio, GpioPullUpDown pud) {
		/*-
		 * PU/PD control
		 * Name                Offset (16) Offset (10) Int Offset (16) GPIOs
		 * PMUGRF_GPIO0A_P     0x00040     64          0x00010         0..7
		 * PMUGRF_GPIO0B_P     0x00044     68          0x00011         8..15
		 * PMUGRF_GPIO0C_P     0x00048     72          0x00012         16..23
		 * PMUGRF_GPIO0D_P     0x0004c     76          0x00013         24..31
		 * PMUGRF_GPIO1A_P     0x00050     80          0x00014         32..39
		 * PMUGRF_GPIO1B_P     0x00054     84          0x00015         40..47
		 * PMUGRF_GPIO1C_P     0x00058     88          0x00016         48..55
		 * PMUGRF_GPIO1D_P     0x0005c     92          0x00017         56..63
		 * GRF_GPIO2A_P        0x0e040     96          0x03810         64..71
		 * GRF_GPIO2B_P        0x0e044     100         0x03811         72..79
		 * GRF_GPIO2C_P        0x0e048     104         0x03812         80..87
		 * GRF_GPIO2D_P        0x0e04c     108         0x03813         88..95
		 * GRF_GPIO3A_P        0x0e050     112         0x03814         96..103
		 * GRF_GPIO3B_P        0x0e054     116         0x03815         104..111
		 * GRF_GPIO3C_P        0x0e058     120         0x03816         112..119
		 * GRF_GPIO3D_P        0x0e05c     124         0x03817         120..127
		 * GRF_GPIO4A_P        0x0e060     128         0x03818         128..135
		 * GRF_GPIO4B_P        0x0e064     132         0x03819         136..143
		 * GRF_GPIO4C_P        0x0e068     136         0x0381a         144..151
		 * GRF_GPIO4D_P        0x0e06c     140         0x0381b         152..159
		 */

		final int bank = gpio >> 5;
		final int shift = (gpio % 8) << 1;
		final int bank_offset = gpio % 32;

		int pud_int_offset;
		MmapIntBuffer int_buffer;
		if (bank < 2) {
			pud_int_offset = PUD_INT_OFFSET + bank * 4 + bank_offset / 8;
			int_buffer = pmuGrfMmapIntBuffer;
		} else {
			pud_int_offset = PUD_INT_OFFSET + (bank - 2) * 4 + bank_offset / 8;
			int_buffer = grfMmapIntBuffer;
		}

		int pud_val;
		if (gpio < 32 || (gpio >= 80 && gpio < 96)) {
			/*-
			 * 0a, 0b, 2c, 2d (0-4) PE/PS control:
			 * 2'b00: Z(Normal operation);
			 * 2'b01: weak 0(pull-down);
			 * 2'b10: Z(Normal operation);
			 * 2'b11: weak 1(pull-up);
			 */
			switch (pud) {
			case PULL_DOWN:
				pud_val = 0b01; // weak 0(pull-down)
				break;
			case PULL_UP:
				pud_val = 0b11; // weak 1(pull-up)
				break;
			default:
				pud_val = 0b00; // Z(Normal operation)
			}
		} else {
			/*-
			 * 1a, 1b, 1c, 1d, 2a, 2b (0-4), 3a, 3b, 3c (0-1), 3d, 4a, 4b (0-5), 4c, 4d (0-6) PU/PD control:
			 * 2'b00: Z(Normal operation);
			 * 2'b01: weak 1(pull-up);
			 * 2'b10: weak 0(pull-down);
			 * 2'b11: Reserved;
			 */
			switch (pud) {
			case PULL_UP:
				pud_val = 0b01; // weak 1(pull-up)
				break;
			case PULL_DOWN:
				pud_val = 0b10; // weak 0(pull-down)
				break;
			default:
				pud_val = 0b00; // Z(Normal operation)
			}
		}

		int reg_val = int_buffer.get(pud_int_offset);
		reg_val &= ~(0b11 << shift);
		// Set the write enable bits
		reg_val |= (pud_val << shift) | (0b11 << (shift + 16));
		int_buffer.put(pud_int_offset, reg_val);
	}

	@Override
	public boolean gpioRead(int gpio) {
		final int bank = gpio >> 5;
		int shift = gpio % 32;

		return (gpioBanks[bank].get(GPIO_EXT_PORTA) & (1 << shift)) != 0;
	}

	@Override
	public void gpioWrite(int gpio, boolean value) {
		final int bank = gpio >> 5;
		int shift = gpio % 32;

		int reg_val = gpioBanks[bank].get(GPIO_SWPORTA_DR);
		if (value) {
			reg_val |= (1 << shift);
		} else {
			reg_val &= ~(1 << shift);
		}
		gpioBanks[bank].put(GPIO_SWPORTA_DR, reg_val);
	}

	@SuppressWarnings("boxing")
	public static void main(String[] args) {
		int gpio = 0;
		int bank = gpio >> 5;
		int bank_index = gpio % 32;
		int letter = bank_index >> 3;
		int index = bank_index % 8;
		int iomux_int_offset;
		if (bank < 2) {
			iomux_int_offset = IOMUX_INT_OFFSET + bank * 4 + (gpio % 32) / 8;
		} else {
			iomux_int_offset = GRF_GPIO2A_IOMUX + (bank - 2) * 4 + (gpio % 32) / 8;
		}
		System.out.format(
				"GPIO: %d, bank: %d, bank_index: %d, letter: %d, index: %d - GPIO%d_%s%d, iomux int offset: 0x%05x%n",
				gpio, bank, bank_index, letter, index, bank, Character.toString('A' + letter), index, iomux_int_offset);

		gpio = 39;
		bank = gpio >> 5;
		bank_index = gpio % 32;
		letter = bank_index >> 3;
		index = bank_index % 8;
		if (bank < 2) {
			iomux_int_offset = IOMUX_INT_OFFSET + bank * 4 + (gpio % 32) / 8;
		} else {
			iomux_int_offset = GRF_GPIO2A_IOMUX + (bank - 2) * 4 + (gpio % 32) / 8;
		}
		System.out.format(
				"GPIO: %d, bank: %d, bank_index: %d, letter: %d, index: %d - GPIO%d_%s%d, iomux int offset: 0x%05x%n",
				gpio, bank, bank_index, letter, index, bank, Character.toString('A' + letter), index, iomux_int_offset);

		gpio = 40;
		bank = gpio >> 5;
		bank_index = gpio % 32;
		letter = bank_index >> 3;
		index = bank_index % 8;
		if (bank < 2) {
			iomux_int_offset = IOMUX_INT_OFFSET + bank * 4 + (gpio % 32) / 8;
		} else {
			iomux_int_offset = GRF_GPIO2A_IOMUX + (bank - 2) * 4 + (gpio % 32) / 8;
		}
		System.out.format(
				"GPIO: %d, bank: %d, bank_index: %d, letter: %d, index: %d - GPIO%d_%s%d, iomux int offset: 0x%05x%n",
				gpio, bank, bank_index, letter, index, bank, Character.toString('A' + letter), index, iomux_int_offset);

		gpio = 47;
		bank = gpio >> 5;
		bank_index = gpio % 32;
		letter = bank_index >> 3;
		index = bank_index % 8;
		if (bank < 2) {
			iomux_int_offset = IOMUX_INT_OFFSET + bank * 4 + (gpio % 32) / 8;
		} else {
			iomux_int_offset = GRF_GPIO2A_IOMUX + (bank - 2) * 4 + (gpio % 32) / 8;
		}
		System.out.format(
				"GPIO: %d, bank: %d, bank_index: %d, letter: %d, index: %d - GPIO%d_%s%d, iomux int offset: 0x%05x%n",
				gpio, bank, bank_index, letter, index, bank, Character.toString('A' + letter), index, iomux_int_offset);

		gpio = 124;
		bank = gpio >> 5;
		bank_index = gpio % 32;
		letter = bank_index >> 3;
		index = bank_index % 8;
		if (bank < 2) {
			iomux_int_offset = IOMUX_INT_OFFSET + bank * 4 + (gpio % 32) / 8;
		} else {
			iomux_int_offset = GRF_GPIO2A_IOMUX + (bank - 2) * 4 + (gpio % 32) / 8;
		}
		System.out.format(
				"GPIO: %d, bank: %d, bank_index: %d, letter: %d, index: %d - GPIO%d_%s%d, iomux int offset: 0x%05x%n",
				gpio, bank, bank_index, letter, index, bank, Character.toString('A' + letter), index, iomux_int_offset);

		/*-
		 * GPIO4_D5 = 4*32 + 3*8 + 5 = 157
		 * (A=0, B=1, C=2, D=3)
		 */
		gpio = 157;
		bank = gpio >> 5;
		bank_index = gpio % 32;
		letter = bank_index >> 3;
		index = bank_index % 8;
		if (bank < 2) {
			iomux_int_offset = IOMUX_INT_OFFSET + bank * 4 + (gpio % 32) / 8;
		} else {
			iomux_int_offset = GRF_GPIO2A_IOMUX + (bank - 2) * 4 + (gpio % 32) / 8;
		}
		System.out.format(
				"GPIO: %d, bank: %d, bank_index: %d, letter: %d, index: %d - GPIO%d_%s%d, iomux int offset: 0x%05x%n",
				gpio, bank, bank_index, letter, index, bank, Character.toString('A' + letter), index, iomux_int_offset);

		if (args.length == 0) {
			return;
		}

		gpio = Integer.parseInt(args[0]);
		bank = gpio >> 5;
		bank_index = gpio % 32;
		letter = bank_index >> 3;
		index = bank_index % 8;
		System.out.println(
				"Testing with GPIO: " + gpio + ", bank: " + bank + ", bank_index: " + bank_index + ", letter: " + letter
						+ ", index: " + index + " - GPIO" + bank + "_" + Character.toString('A' + letter) + index);

		try (RockchipRK3399MmapGpio mmap_gpio = new RockchipRK3399MmapGpio()) {
			mmap_gpio.initialise();
			boolean value = false;
			for (int i = 0; i < 20; i++) {
				DeviceMode mode = mmap_gpio.getMode(gpio);
				System.out.println(i + " mode: " + mode);
				if (mode == DeviceMode.DIGITAL_OUTPUT) {
					System.out.println(i + " pre write: " + mmap_gpio.gpioRead(gpio));
					mmap_gpio.gpioWrite(gpio, value);
					System.out.println(i + " post write: " + mmap_gpio.gpioRead(gpio));
					value = !value;
				} else if (mode == DeviceMode.DIGITAL_INPUT) {
					GpioPullUpDown pud = mmap_gpio.getPullUpDown(gpio);
					System.out.println(i + " value pre PD change: " + mmap_gpio.gpioRead(gpio) + ", pud: " + pud);
					mmap_gpio.setPullUpDown(gpio, GpioPullUpDown.PULL_DOWN);
					pud = mmap_gpio.getPullUpDown(gpio);
					System.out.println(i + " value post PD change: " + mmap_gpio.gpioRead(gpio) + ", pud: " + pud);
					mmap_gpio.setPullUpDown(gpio, GpioPullUpDown.PULL_UP);
					pud = mmap_gpio.getPullUpDown(gpio);
					System.out.println(i + " value post PU change: " + mmap_gpio.gpioRead(gpio) + ", pud: " + pud);
					mmap_gpio.setPullUpDown(gpio, GpioPullUpDown.NONE);
					pud = mmap_gpio.getPullUpDown(gpio);
					System.out.println(i + " value post NONE change: " + mmap_gpio.gpioRead(gpio) + ", pud: " + pud);
					mmap_gpio.setPullUpDown(gpio, pud);
					pud = mmap_gpio.getPullUpDown(gpio);
					System.out.println(i + " value post restoring pud: " + mmap_gpio.gpioRead(gpio) + ", pud: " + pud);
				}
				SleepUtil.sleepSeconds(1);
			}
		} catch (Throwable t) {
			Logger.error(t, "Error: {}", t);
		}
	}
}
