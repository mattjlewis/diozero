package com.diozero.devices;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     LPS25H.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
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


import java.io.Closeable;
import java.nio.ByteOrder;

import org.tinylog.Logger;

import com.diozero.api.I2CConstants;
import com.diozero.api.I2CDevice;

/**
 * STMicroelectronics LPS25H "ultra compact absolute piezoresistive pressure sensor". Datasheet:
 * http://www2.st.com/content/ccc/resource/technical/document/datasheet/58/d2/33/a4/42/89/42/0b/DM00066332.pdf/files/DM00066332.pdf/jcr:content/translations/en.DM00066332.pdf
 * Example implementation:
 * https://github.com/richards-tech/RTIMULib/blob/master/RTIMULib/IMUDrivers/RTPressureLPS25H.cpp
 * Eclipse Kura implementation: https://github.com/eclipse/kura/tree/develop/kura/examples/org.eclipse.kura.raspberrypi.sensehat/src/main/java/org/eclipse/kura
 */
@SuppressWarnings("unused")
public class LPS25H implements ThermometerInterface, BarometerInterface, Closeable {
	private static final double PRESSURE_SCALE = 4096;
	
	//  LPS25H I2C Slave Addresses
	private static final int DEFAULT_DEVICE_ADDRESS0 = 0x5c;
	private static final int DEFAULT_DEVICE_ADDRESS1 = 0x5d;
	private static final int REG_ID = 0x0f;
	private static final int ID = 0xbd;
	/** Set to 1 to read, 0 to write. */
	private static final int READ = 0x80;
	
	//	Register map
	/** Reference pressure, 2s complement, 3-byte signed integer. */
	private static final int REF_P_XL = 0x08;
	private static final int REF_P_L = 0x09;
	private static final int REF_P_H = 0x0a;
	/** Pressure and temperature resolution configuration. */
	private static final int RES_CONF = 0x10;
	/** Control Register 1. */
	private static final int CTRL_REG1 = 0x20;
	/** Control Register 2. */
	private static final int CTRL_REG2 = 0x21;
	/** Control Register 3. */
	private static final int CTRL_REG3 = 0x22;
	/** Control Register 4. */
	private static final int CTRL_REG4 = 0x23;
	/** Interrupt configuration. */
	private static final int INTERRUPT_CFG = 0x24;
	/** Interrupt Source. */
	private static final int INT_SOURCE = 0x25;
	/** Status register. This register is updated every ODR cycle, regardless of BDU value in CTRL_REG1. */
	private static final int STATUS_REG = 0x27;
	/** Pressure value, 2s complement, 3-byte signed integer. */
	private static final int PRESS_OUT_XL = 0x28;
	private static final int PRESS_OUT_L = 0x29;
	private static final int PRESS_OUT_H = 0x2a;
	/** Temperature value, 2s complement, 2-byte signed integer. */
	private static final int TEMP_OUT_L = 0x2b;
	private static final int TEMP_OUT_H = 0x2c;
	/** FIFO Control. */
	private static final int FIFO_CTRL = 0x2e;
	/** FIFI Status. */
	private static final int FIFO_STATUS = 0x2f;
	/** Threshold Pressure, unsigned 2-byte integer. */
	private static final int THS_P_L = 0x30;
	private static final int THS_P_H = 0x31;
	/** Pressure offset, 2s-complement signed 2-byte integer. */
	private static final int RPDS_L = 0x39;
	private static final int RPDS_H = 0x3a;
	
	/*
	 * Flags for pressure and temperature resolution configuration.
	 * Bit:   3     2     1     0
	 * Use: AVGP1 AVGP0 AVGT1 AVGT0
	 * AVGP1 AVGP0 Num. Internal Avg
	 *   0     0            8
	 *   0     1           32
	 *   1     0          128
	 *   1     1          512
	 * AVGT1 AVGT0 Num. Internal Avg
	 *   0     0            8
	 *   0     1           16
	 *   1     0           32
	 *   1     1           64
	 */
	private static final byte RC_PRESSURE_8_SAMPLES = 0b00 << 2;
	private static final byte RC_PRESSURE_32_SAMPLES = 0b01 << 2;
	private static final byte RC_PRESSURE_128_SAMPLES = 0b10 << 2;
	private static final byte RC_PRESSURE_512_SAMPLES = 0b11 << 2;
	private static final byte RC_TEMP_8_SAMPLES = 0b00;
	private static final byte RC_TEMP_16_SAMPLES = 0b01;
	private static final byte RC_TEMP_32_SAMPLES = 0b10;
	private static final byte RC_TEMP_64_SAMPLES = 0b11;
	
	/*
	 * Flags for Control Register 1
	 *   [7] PD: Power down control (0: power down mode, 1: active mode)
	 * [6:4] ODR2, ODR1, ODR0: output data rate selection
	 *   [3] DIFF_EN: Interrupt circuit enable (0: interrupt generation disabled; 1: interrupt circuit enabled)
	 *   [2] BDU: block data update (0: continuous update; 1: output registers not updated until MSB and LSB reading)
	 *   [1] RESET_AZ: Reset AutoZero function. Reset REF_P reg, set pressure to default value in RPDS register (@0x39/A)
	 *       (1: Reset. 0: disable)
	 *   [0] SIM: SPI Serial Interface Mode selection. (0: 4-wire interface; 1: 3-wire interface)
	 */
	private static final byte CR1_PD_CONTROL_BIT = 7;
	/** The PD bit is used to turn on the device. The device is in power-down mode when
	 * PD = ?0? (default value after boot). The device is active when PD is set to ?1?. */
	private static final byte CR1_PD_CONTROL = (byte) (1 << CR1_PD_CONTROL_BIT);
	private static final byte CR1_ODR_ONE_SHOT = 0b000 << 4;
	private static final byte CR1_ODR_1HZ      = 0b001 << 4;
	private static final byte CR1_ODR_7HZ      = 0b010 << 4;
	private static final byte CR1_ODR_12_5HZ   = 0b011 << 4;
	private static final byte CR1_ODR_25HZ     = 0b100 << 4;
	private static final byte CR1_DIFF_EN_BIT = 3;
	/** Used to enable the circuitry for the computing of differential pressure output.
	 * In default mode (DIFF_EN=?0?) the circuitry is turned off. It is suggested to turn
	 * on the circuitry only after the configuration of REF_P_x and THS_P_x. */
	private static final byte CR1_DIFF_EN = 1 << CR1_DIFF_EN_BIT;
	private static final byte CR1_BDU_BIT = 2;
	/** (0: continuous update; 1: output registers not updated until MSB and LSB reading) */
	private static final byte CR1_BDU = 1 << CR1_BDU_BIT;
	private static final byte CR1_RESET_AZ_BIT = 1;
	/** Used to Reset AutoZero function. Reset REF_P reg (@0x08..0A) set pressure
	 * reference to default value RPDS reg(0x39/3A). RESET_AZ is self cleared. */
	private static final byte CR1_RESET_AZ = 1 << CR1_RESET_AZ_BIT;
	private static final byte CR1_SIM_BIT = 1;
	/** Selects the SPI serial interface mode.
	 * 0: (default value) 4-wire SPI interface mode selected.
	 * 1: 3-wire SPI interface mode selected. */
	private static final byte CR1_SIM = 1 << CR1_SIM_BIT;
	
	/*
	 * Flags for Control Register 2
	 * [7] BOOT: Reboot memory content. Default value: 0
	 *      (0: normal mode; 1: reboot memory content) Self-clearing upon completion)
	 * [6]  FIFO_EN: FIFO Enable. Default value: 0
	 *      (0: disable; 1: enable)
	 * [5]  WTM_EN: Enable FIFO Watermark level use. Default value 0
	 *      (0: disable; 1: enable)
	 * [4]  FIFO_MEAN_DEC: Enable 1Hz ODR decimation
	 *      (0: disable; 1 enable)
	 * [3]  I2C enable
	 *      (0: I2C enable; 1: SPI disable)
	 * [2]  Software reset. Default value: 0
	 *      (0: normal mode; 1: software reset) Self-clearing upon completion)
	 * [1]  Autozero enable. Default value: 0
	 *      (0: normal mode; 1: autozero enable)
	 * [0]  One shot enable. Default value: 0
	 *      (0: waiting for start of conversion; 1: start for a new dataset)
	 */
	private static final byte CR2_BOOT_BIT = 7;
	private static final byte CR2_BOOT = (byte) (1 << CR2_BOOT_BIT);
	private static final byte CR2_FIFO_EN_BIT = 6;
	private static final byte CR2_FIFO_EN = (byte) (1 << CR2_FIFO_EN_BIT);
	private static final byte CR2_WTM_EN_BIT = 5;
	private static final byte CR2_WTM_EN = (byte) (1 << CR2_WTM_EN_BIT);
	private static final byte CR2_FIFO_MEAN_DEC_BIT = 4;
	private static final byte CR2_FIFO_MEAN_DEC_EN = (byte) (1 << CR2_FIFO_MEAN_DEC_BIT);
	private static final byte CR2_I2C_EN_BIT = 3;
	private static final byte CR2_I2C_EN = (byte) (1 << CR2_I2C_EN_BIT);
	private static final byte CR2_SOFTWARE_RESET_BIT = 2;
	private static final byte CR2_SOFTWARE_RESET = (byte) (1 << CR2_SOFTWARE_RESET_BIT);
	private static final byte CR2_AUTOZERO_EN_BIT = 1;
	private static final byte CR2_AUTOZERO_EN = (byte) (1 << CR2_AUTOZERO_EN_BIT);
	private static final byte CR2_ONESHOT_EN_BIT = 0;
	private static final byte CR2_ONESHOT_EN = (byte) (1 << CR2_ONESHOT_EN_BIT);
	
	/*
	 * Flags for Control Register 3
	 *   [7]  INT_H_L: Interrupt active high, low. Default value: 0
	 *        (0: active high; 1: active low)
	 *   [6]  PP_OD: Push-pull/open drain selection on interrupt pads. Default value: 0
	 *        (0: push-pull; 1: open drain)
	 * [5:2]  Reserved
	 * [1:0]  INT1_S2, INT1_S1: Pressure interrupt, data signal on INT1 pad control bits. Default value: 00
	 * INT_S2 INT_S1 INT1
	 *   0      0    Data signal (see CTRL_REG4)
	 *   0      1    Pressure high (P_high)
	 *   1      0    Pressure low (P_low)
	 *   1      1    Pressure low OR high 
	 */
	
	/*
	 * Flags for Control Register 4
	 * [7:4]  Reserved: keep these bits at 0
	 *   [3]  P1_EMPTY: Empty signal on INT1 pin
	 *   [2]  P1_WTM: Watermark signal on INT1 pin
	 *   [1]  P1_OVERRUN: Overrun signal on INT1 pin
	 *   [0]  P1_DRDY: Data ready signal on INT1 pin
	 */
	
	/*
	 * Flags for Interrupt Configuration
	 * [7:3]  RESERVED
	 *   [2]  LIR: Latch Interrupt request into INT_SOURCE register. Default value: 0.
	 *        (0: interrupt request not latched; 1: interrupt request latched)
	 *   [1]  PL_E: Enable interrupt generation on differential pressure low event. Default value: 0.
	 *        (0: disable interrupt request;
	 *         1: enable interrupt request on measured differential pressure value lower than preset threshold)
	 *   [0]  PH_E: Enable interrupt generation on differential pressure high event. Default value: 0
	 *        (0: disable interrupt request;
	 *         1:enable interrupt request on measured differential pressure value higher than preset threshold)
	 */
	
	/*
	 * Flags for Interrupt Source
	 * [7:3]  Reserved: keep these bits at 0 
	 *   [2]  IA: Interrupt Active.
	 *        (0: no interrupt has been generated; 1: one or more interrupt events have been generated).
	 *   [1]  PL: Differential pressure Low.
	 *        (0: no interrupt has been generated; 1: Low differential pressure event has occurred).
	 *   [0]  PH: Differential pressure High.
	 *        (0: no interrupt has been generated; 1: High differential pressure event has occurred)
	 */
	
	/*
	 * Flags for Status Register
	 *   [5]  P_OR: Pressure data overrun. Default value: 0
	 *        (0: no overrun has occurred;
	 *         1: new data for pressure has overwritten the previous one)
	 *   [4]  T_OR: Temperature data overrun. Default value: 0
	 *        (0: no overrun has occurred;
	 *         1: a new data for temperature has overwritten the previous one)
	 * [3:2]  Reserved
	 *   [1]  P_DA: Pressure data available. Default value: 0
	 *        (0: new data for pressure is not yet available;
	 *         1: new data for pressure is available)
	 *   [0]  T_DA: Temperature data available. Default value: 0
	 *        (0: new data for temperature is not yet available;
	 *         1: new data for temperature is available)
	 */
	private static final byte SR_P_DA_BIT = 1;
	private static final byte SR_P_DA = 1 << SR_P_DA_BIT;
	private static final byte SR_T_DA_BIT = 0;
	private static final byte SR_T_DA = 1 << SR_T_DA_BIT;
	
	/*
	 * Flags for FIFO Control
	 * [7:5]  F_MODE2-0: FIFO mode selection.
	 * [4:0]  WTM_POINT4-0 : FIFO threshold watermark level setting.
	 */
	/** BYPASS MODE. */
	private static final byte FC_BYPASS_MODE = 0b000 << 5;
	/** FIFO MODE. Stops collecting data when full. */
	private static final byte FC_FIFO_MODE = 0b001 << 5;
	/**  STREAM MODE: Keep the newest measurements in the FIFO. */
	private static final byte FC_STREAM_MODE = 0b010 << 5;
	/** STREAM MODE until trigger deasserted, then change to FIFO MODE. */
	private static final byte FC_STREAM_THEN_FIFO_MODE = 0b011 << 5;
	/** BYPASS MODE until trigger deasserted, then change to STREAM MODE. */
	private static final byte FC_BYPASS_THEN_STREAM_MODE = (byte) (0b100 << 5);
	/**  FIFO_MEAN MODE: FIFO is used to generate a running average filtered pressure. */
	private static final byte FC_FIFO_MEAN_MODE = (byte) (0b110 << 5);
	/** BYPASS mode until trigger deasserted, then change to FIFO MODE. */
	private static final byte FC_BYPASS_THEN_FIFO_MODE = (byte) (0b111 << 5);
	/** 2 samples moving average. */
	private static final byte FC_WTM_2_SAMPLES  = 0b00001;
	/** 4 samples moving average. */
	private static final byte FC_WTM_4_SAMPLES  = 0b00011;
	/** 8 samples moving average. */
	private static final byte FC_WTM_8_SAMPLES  = 0b00111;
	/** 16 samples moving average. */
	private static final byte FC_WTM_16_SAMPLES = 0b01111;
	/** 32 samples moving average. */
	private static final byte FC_WTM_32_SAMPLES = 0b11111;
	
	private I2CDevice device;

	public LPS25H() {
		this(I2CConstants.BUS_1, DEFAULT_DEVICE_ADDRESS0);
	}

	public LPS25H(int controller, int address) {
		device = I2CDevice.builder(address).setController(controller).setByteOrder(ByteOrder.LITTLE_ENDIAN).build();

		// Power on, 25Hz output data rate, output registers not updated until both MSB & LSB read
		device.writeByteData(CTRL_REG1, CR1_PD_CONTROL | CR1_ODR_25HZ | CR1_BDU);
		// Configure the number of pressure and temperature samples
		device.writeByteData(RES_CONF, RC_PRESSURE_32_SAMPLES | RC_TEMP_16_SAMPLES);
		// Configure the FIFO (mean mode)
		// TODO Configure number of WTM samples?!
		device.writeByteData(FIFO_CTRL, FC_FIFO_MEAN_MODE);
		// Enable the FIFO
		device.writeByteData(CTRL_REG2, CR2_FIFO_EN);
	}
	
	@Override
	public float getPressure() {
		byte status = device.readByteData(STATUS_REG);
		if ((status & SR_P_DA) == 0) {
			Logger.warn("Pressure data not available");
			return -1;
		}
		
		byte[] raw_data = new byte[3];
		device.readI2CBlockData(PRESS_OUT_XL | READ, raw_data);
		
		int raw_pressure = raw_data[2] << 16 | (raw_data[1] & 0xff) << 8 | (raw_data[0] & 0xff);
		
		return (float) (raw_pressure / PRESSURE_SCALE);
	}
	
	@Override
	public float getTemperature() {
		byte status = device.readByteData(STATUS_REG);
		if ((status & SR_T_DA) == 0) {
			Logger.warn("Temperature data not available");
			return -1;
		}
		
		short raw_temp = device.readShort(TEMP_OUT_L | READ);
		
		return (float) (raw_temp / 480.0 + 42.5);
	}

	@Override
	public void close() {
		device.close();
	}
}
