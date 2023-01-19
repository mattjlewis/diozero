package com.diozero.devices.sandpit;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     WaveshareEink75b.java
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

import com.diozero.api.DigitalInputDevice;
import com.diozero.api.DigitalOutputDevice;
import com.diozero.util.SleepUtil;

/**
 * 7.5" Datasheet:
 * https://www.waveshare.com/wiki/File:7.5inch-e-paper-specification.pdf 7.5" V2
 * Datasheet:
 * https://www.waveshare.com/w/upload/6/60/7.5inch_e-Paper_V2_Specification.pdf
 * https://www.waveshare.com/wiki/7.5inch_e-Paper_HAT
 *
 * Code: https://github.com/waveshare/e-Paper
 *
 * OTP: One Time Programmable memory, not programmed into registers by the
 * driver SW for this controller. LUT: Waveform Look Up Table
 */
public class WaveshareEink75b extends WaveshareEink {
	private static final byte PANEL_SETTING_REG = 0x00;
	private static final byte POWER_SETTING_REG = 0x01;
	private static final byte POWER_OFF_REG = 0x02;
	private static final byte POWER_OFF_SEQ_REG = 0x03;
	private static final byte POWER_ON_REG = 0x04;
	private static final byte POWER_ON_MEAS_REG = 0x05;
	private static final byte BOOSTER_SOFT_START_REG = 0x06;
	private static final byte DEEP_SLEEP_REG = 0x07;
	// K/W or OLD pixel data
	private static final byte DISP_START_TRANS_REG = 0x10;
	private static final byte DATA_STOP_REG = 0x11;
	private static final byte DISPLAY_REFRESH_REG = 0x12;
	// Red or NEW pixel data
	private static final byte DISP2_START_TRANS_REG = 0x13;
	private static final byte DUAL_SPI_REG = 0x15;
	private static final byte AUTO_SEQ_REG = 0x17;
	private static final byte VCOM_AND_DATA_INT_REG = 0x50;
	private static final byte TCON_REG = 0x60;
	// Resolution
	private static final byte TRES_REG = 0x61;
	private static final byte REVISION_REG = 0x70;
	private static final byte I2C_STATUS_FLAG_REG = 0x71;

	public WaveshareEink75b(int controller, int chipSelect, DigitalOutputDevice reset,
			DigitalOutputDevice dataOrCommand, DigitalInputDevice busy) {
		super(Model._7x5V2, controller, chipSelect, reset, dataOrCommand, busy);
	}

	@Override
	protected void init() {
		reset();

		commandAndData(BOOSTER_SOFT_START_REG, new byte[] { 0x17, 0x17, 0x27, 0x17 });

		// datasheet p20:
		// commandAndData(POWER_SETTING_REG, new byte[] { 0x07, 0x17, 0x3f, 0x3f });
		// data[0] - example: 0x07 (board disabled and internal power)
		// bit[4]: Border. 0: border disabled (default), 1: border enabled
		// bit[2:0] Power source. 1: internal power, 0: external power source
		// data[1] - example: 0x07 or data sheet 0x17 (fast VCOM slew)
		// bit[7]: OTP power selection (0: external, 1: internal)
		// bit[4]: VCOM_SLEW. 0: slow, 1: fast
		// bit[2:0]: VG_LVL. Voltage level 9,10,11,12,17,18,19,20v
		// data[2] - example 0x3f (15v)
		// bit[5:0] VDH_LVL. Voltage increments of 0.2v from 2.4v to 15v
		// data[3] VDL_LVL[5:0] 0x3f (-15v)
		// data[4] VDHR_LVL[5:0] - not set
		// Power setting: VGH=20V, VGL=-20V; VDH=15V, VDL=-15V
		commandAndData(POWER_SETTING_REG, new byte[] { 0x07, 0x07, 0x3f, 0x3f });

		// Power on
		command(POWER_ON_REG);
		SleepUtil.sleepMillis(100);
		readBusy();

		// [5] REG: LUT selection. 0: LUT from OTP (default), 1: LUT from register
		// [4] KW/R: Black / White / Red.
		// 0: Pixel with Black/White/Red, KWR mode. (Default)
		// 1: Pixel with Black/White, KW mode.
		// [3] UD: Gate scan direction. 0: Scan down, 1: Scan up (default)
		// [2] SHL: Source Shift Direction. 0: Shift left, 1: Shift right (default)
		// [1] SHD_N: Booster Switch. 0: Booster OFF, 1: Booster ON (default)
		// [0] RST_N: Soft Rest. 0: Reset, 1: No effect (default)
		// Example 0x1f -> 0b00011111 (LUT: OTP, KW, Scan up/right, boost on)
		// LUT Reg/B&W: 0x3f, LUT Reg/BW&R: 0x2f, LUT OTP/B&W: 0x1f, LUT OTP/BW&R: 0x0f
		commandAndData(PANEL_SETTING_REG, (byte) 0x1f);

		// Resolution: Source 800 (0x03, 0x20), Gate 480 (0x01, 0xe0)
		// HRES[9:3]: Horizontal resolution / 8. (value range 1..100)
		// VRES[9:0]: Vertical resolution. (value range 1..600)
		// Last active source = HRES[9:3] * 8 - 1
		// Last active gate = VRES[9:0] - 1
		// 0x320 == HRES[9:3] 0b1100100000 == HRES[6:0] 0b1100100 == 100
		// data[0] HRES[9:8]
		// data[1] HRES[7:3]
		// 0x1e0 == VRES[9:0] 0b111100000 == 480
		// data[2] VRES[9:8]
		// data[3] VRES[7:0]
		int hres = model.getHeight();
		int vres = model.getWidth();
		// commandAndData(TRES_REG, new byte[] { 0x03, 0x20, 0x01, (byte) 0xe0 });
		commandAndData(TRES_REG, new byte[] { (byte) ((hres >> 8) & 0x03), (byte) (hres & 0xf8),
				(byte) ((vres >> 8) & 0x03), (byte) (vres & 0xff) });

		// data[0] Dual SPI mode
		// [5] MM_EN: MM input pin definition enable. 0: disable, 1: enable
		// [4] DUSPI_EN: Dual SPI mode enable. 0: disable, 1: enable
		commandAndData(DUAL_SPI_REG, (byte) 0x00);

		// data[0] Non-overlap period of Gate and Source
		// [7:4] S2G[3:0]: Source to Gate.
		// [3:0] G2S[3:0]: Gate to Source.
		// 0..15 increments of 4 period units (4..64)
		// 1 period unit = 667 nS. Default for both is 0b0010 (12 == 8 uS)
		commandAndData(TCON_REG, (byte) 0x22);

		// VCOM and Data interval Setting - Set Interval between VCOM and Data
		// data[0] example 0x10
		// [7] BDZ: Border Hi-Z control. 0: disabled (default), 1: enabled
		// [5:4] BDV[1:0]: Border LUT selection.
		// [3] N2OCP Copy frame data from NEW data to OLD data enable control after
		// display refresh with NEW/OLD in KW mode. 0: disabled, 1: enabled
		// [1:0] DDX[1:0]: Data polarity.
		// data[1] example 0x07 (default)
		// CDI[3:0]: VCOM and data interval. 0 (17) to 15 (2)
		commandAndData(VCOM_AND_DATA_INT_REG, new byte[] { 0x10, 0x07 });
	}

	@Override
	protected void sleep() {
		command(POWER_OFF_REG);
		readBusy();
		commandAndData(DEEP_SLEEP_REG, (byte) 0xa5);

		SleepUtil.sleepMillis(2_000);
	}

	@Override
	protected void clear() {
		byte[] buf = new byte[model.getWidth() * model.getHeight()];
		commandAndData(DISP_START_TRANS_REG, buf);
		commandAndData(DISP2_START_TRANS_REG, buf);
		command(DISPLAY_REFRESH_REG);
		SleepUtil.sleepMillis(100);
		readBusy();
	}

	@Override
	protected void readBusy() {
		do {
			command((byte) 0x71);
			SleepUtil.sleepMillis(1);
		} while (!busy.getValue());
		SleepUtil.sleepMillis(200);
	}

	@Override
	protected void getRevision() {
		command(REVISION_REG);
		dataOrCommand.on();
		// PROD_REV[23:0], LUT_REV[23:0], CHIP_REV[7:0]
		byte[] buffer = new byte[3 + 3 + 1];
		buffer = device.writeAndRead(buffer);
		prodRev = (buffer[0] & 0xff) << 16 | (buffer[1] & 0xff) << 8 | (buffer[2] & 0xff);
		lutRev = (buffer[3] & 0xff) << 16 | (buffer[4] & 0xff) << 8 | (buffer[5] & 0xff);
		// Should be fixed at 0b00001100
		chipRev = buffer[6] & 0xff;
	}
}
