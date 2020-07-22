package com.diozero.imu.drivers.invensense;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - IMU device classes
 * Filename:     MPU9150Constants.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 mattjlewis
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


// MPU9150 (or MPU6050 w/ AK8975 on the auxiliary bus)
public interface MPU9150Constants {
	// 16bit signed value (-32,768 to 32,767)
	static final int HARDWARE_UNIT = ((int)Math.pow(2, 16)) / 2; // ((2^16)/2 = 32,768)
	// TODO Validate the scale of the Quaternion data
	static final double QUATERNION_SCALE = 1.0 / (HARDWARE_UNIT/2);
	// From https://github.com/pocketmoon/MPU-6050-Arduino-Micro-Head-Tracker/blob/master/MPUReset/MPU6050Reset.ino
	//private static final double QUATERNION_SCALE = 1.0 / 16384;

	// Communication with all registers of the device is performed using I2C at 400 kHz
	static final int I2C_CLOCK_FREQUENCY_FAST		= 400_000;
	static final int I2C_CLOCK_FREQUENCY_STANDARD	= 100_000;
	
	static final byte BIT_I2C_MST_VDDIO			= (byte)0x80;
	static final byte BIT_FIFO_EN				= 0x40;
	static final byte BIT_DMP_EN				= (byte)0x80;
	static final byte BIT_FIFO_RST				= 0x04;
	static final byte BIT_DMP_RST				= 0x08;
	static final byte BIT_FIFO_OVERFLOW			= 0x10;
	static final byte BIT_DATA_RDY_EN			= 0x01;
	static final byte BIT_DMP_INT_EN			= 0x02;
	static final byte BIT_MOT_INT_EN			= 0x40;
	static final byte BITS_FSR            		= 0x18;
	static final byte BITS_LPF            		= 0x07;
	static final byte BITS_HPF            		= 0x07;
	static final byte BITS_CLK            		= 0x07;
	static final byte BIT_FIFO_SIZE_1024  		= 0x40;
	static final byte BIT_FIFO_SIZE_2048  		= (byte)0x80;
	static final byte BIT_FIFO_SIZE_4096  		= (byte)0xC0;
	static final byte BIT_RESET           		= (byte)0x80;
	static final byte BIT_SLEEP           		= 0x40;
	static final byte BIT_S0_DELAY_EN     		= 0x01;
	static final byte BIT_S2_DELAY_EN     		= 0x04;
	static final byte BITS_SLAVE_LENGTH   		= 0x0F;
	static final byte BIT_SLAVE_BYTE_SW   		= 0x40;
	static final byte BIT_SLAVE_GROUP     		= 0x10;
	static final byte BIT_SLAVE_EN        		= (byte)0x80;
	static final byte BIT_I2C_READ        		= (byte)0x80;
	static final byte BITS_I2C_MASTER_DLY 		= 0x1F;
	static final byte BIT_AUX_IF_EN       		= 0x20;
	static final byte BIT_ACTL            		= (byte)0x80;
	static final byte BIT_LATCH_EN        		= 0x20;
	static final byte BIT_ANY_RD_CLR      		= 0x10;
	static final byte BIT_BYPASS_EN       		= 0x02;
	static final byte BITS_WOM_EN         		= (byte)0xC0;
	static final byte BIT_LPA_CYCLE       		= 0x20;
	static final byte BIT_STBY_XA         		= 0x20;
	static final byte BIT_STBY_YA         		= 0x10;
	static final byte BIT_STBY_ZA         		= 0x08;
	static final byte BIT_STBY_XG         		= 0x04;
	static final byte BIT_STBY_YG         		= 0x02;
	static final byte BIT_STBY_ZG         		= 0x01;
	static final byte BIT_STBY_XYZA       		= (byte)(BIT_STBY_XA | BIT_STBY_YA | BIT_STBY_ZA);
	static final byte BIT_STBY_XYZG       		= (byte)(BIT_STBY_XG | BIT_STBY_YG | BIT_STBY_ZG);


	static final byte INV_TEMP					= (byte)0x80;
	static final byte INV_X_GYRO				= 0x40;
	static final byte INV_Y_GYRO				= 0x20;
	static final byte INV_Z_GYRO				= 0x10;
	static final byte INV_XYZ_GYRO				= INV_X_GYRO | INV_Y_GYRO | INV_Z_GYRO;
	static final byte INV_XYZ_ACCEL				= 0x08;
	static final byte INV_XYZ_COMPASS			= 0x01;
	static final int MAX_COMPASS_SAMPLE_RATE	= 100;

	// MUP-9150 main I2C address
	static final int MPU9150_ADDRESS_AD0_LOW     = 0x68; // address pin low (GND), default for InvenSense evaluation board
	static final int MPU9150_ADDRESS_AD0_HIGH    = 0x69; // address pin high (VCC)
	static final int MPU9150_DEFAULT_ADDRESS     = MPU9150_ADDRESS_AD0_LOW;
	// Accelerometer and gyroscope registers
	static final int MPU9150_RA_SMPL_RATE_DIV    = 0x19;
	static final int MPU9150_RA_CONFIG           = 0x1A;
	static final int MPU9150_RA_GYRO_CONFIG      = 0x1B;
	static final int MPU9150_RA_ACCEL_CONFIG     = 0x1C;
	static final int MPU9150_RA_FF_THR           = 0x1D;
	static final int MPU9150_RA_FF_DUR           = 0x1E;
	static final int MPU9150_RA_MOT_THR          = 0x1F;
	static final int MPU9150_RA_MOT_DUR          = 0x20;
	static final int MPU9150_RA_ZRMOT_THR        = 0x21;
	static final int MPU9150_RA_ZRMOT_DUR        = 0x22;
	static final int MPU9150_RA_FIFO_EN          = 0x23;
	static final int MPU9150_RA_I2C_MST_CTRL     = 0x24;
	static final int MPU9150_RA_I2C_SLV0_ADDR    = 0x25;
	static final int MPU9150_RA_I2C_SLV0_REG     = 0x26;
	static final int MPU9150_RA_I2C_SLV0_CTRL    = 0x27;
	static final int MPU9150_RA_I2C_SLV1_ADDR    = 0x28;
	static final int MPU9150_RA_I2C_SLV1_REG     = 0x29;
	static final int MPU9150_RA_I2C_SLV1_CTRL    = 0x2A;
	static final int MPU9150_RA_I2C_SLV2_ADDR    = 0x2B;
	static final int MPU9150_RA_I2C_SLV2_REG     = 0x2C;
	static final int MPU9150_RA_I2C_SLV2_CTRL    = 0x2D;
	static final int MPU9150_RA_I2C_SLV3_ADDR    = 0x2E;
	static final int MPU9150_RA_I2C_SLV3_REG     = 0x2F;
	static final int MPU9150_RA_I2C_SLV3_CTRL    = 0x30;
	static final int MPU9150_RA_I2C_SLV4_ADDR    = 0x31;
	static final int MPU9150_RA_I2C_SLV4_REG     = 0x32;
	static final int MPU9150_RA_I2C_SLV4_DO      = 0x33;
	static final int MPU9150_RA_I2C_SLV4_CTRL    = 0x34;
	static final int MPU9150_RA_I2C_SLV4_DI      = 0x35;
	static final int MPU9150_RA_I2C_MST_STATUS   = 0x36;
	static final int MPU9150_RA_INT_PIN_CFG      = 0x37;
	static final int MPU9150_RA_INT_ENABLE       = 0x38;
	static final int MPU9150_RA_DMP_INT_STATUS   = 0x39;
	static final int MPU9150_RA_INT_STATUS       = 0x3A;
	static final int MPU9150_RA_ACCEL_XOUT_H     = 0x3B;
	static final int MPU9150_RA_ACCEL_XOUT_L     = 0x3C;
	static final int MPU9150_RA_ACCEL_YOUT_H     = 0x3D;
	static final int MPU9150_RA_ACCEL_YOUT_L     = 0x3E;
	static final int MPU9150_RA_ACCEL_ZOUT_H     = 0x3F;
	static final int MPU9150_RA_ACCEL_ZOUT_L     = 0x40;
	static final int MPU9150_RA_TEMP_OUT_H       = 0x41;
	static final int MPU9150_RA_TEMP_OUT_L       = 0x42;
	static final int MPU9150_RA_GYRO_XOUT_H      = 0x43;
	static final int MPU9150_RA_GYRO_XOUT_L      = 0x44;
	static final int MPU9150_RA_GYRO_YOUT_H      = 0x45;
	static final int MPU9150_RA_GYRO_YOUT_L      = 0x46;
	static final int MPU9150_RA_GYRO_ZOUT_H      = 0x47;
	static final int MPU9150_RA_GYRO_ZOUT_L      = 0x48;
	static final int MPU9150_RA_EXT_SENS_DATA_00 = 0x49;
	static final int MPU9150_RA_EXT_SENS_DATA_01 = 0x4A;
	static final int MPU9150_RA_EXT_SENS_DATA_02 = 0x4B;
	static final int MPU9150_RA_EXT_SENS_DATA_03 = 0x4C;
	static final int MPU9150_RA_EXT_SENS_DATA_04 = 0x4D;
	static final int MPU9150_RA_EXT_SENS_DATA_05 = 0x4E;
	static final int MPU9150_RA_EXT_SENS_DATA_06 = 0x4F;
	static final int MPU9150_RA_EXT_SENS_DATA_07 = 0x50;
	static final int MPU9150_RA_EXT_SENS_DATA_08 = 0x51;
	static final int MPU9150_RA_EXT_SENS_DATA_09 = 0x52;
	static final int MPU9150_RA_EXT_SENS_DATA_10 = 0x53;
	static final int MPU9150_RA_EXT_SENS_DATA_11 = 0x54;
	static final int MPU9150_RA_EXT_SENS_DATA_12 = 0x55;
	static final int MPU9150_RA_EXT_SENS_DATA_13 = 0x56;
	static final int MPU9150_RA_EXT_SENS_DATA_14 = 0x57;
	static final int MPU9150_RA_EXT_SENS_DATA_15 = 0x58;
	static final int MPU9150_RA_EXT_SENS_DATA_16 = 0x59;
	static final int MPU9150_RA_EXT_SENS_DATA_17 = 0x5A;
	static final int MPU9150_RA_EXT_SENS_DATA_18 = 0x5B;
	static final int MPU9150_RA_EXT_SENS_DATA_19 = 0x5C;
	static final int MPU9150_RA_EXT_SENS_DATA_20 = 0x5D;
	static final int MPU9150_RA_EXT_SENS_DATA_21 = 0x5E;
	static final int MPU9150_RA_EXT_SENS_DATA_22 = 0x5F;
	static final int MPU9150_RA_EXT_SENS_DATA_23 = 0x60;
	static final int MPU9150_RA_MOT_DETECT_STATUS= 0x61;
	static final int MPU9150_RA_I2C_SLV0_DO      = 0x63;
	static final int MPU9150_RA_I2C_SLV1_DO      = 0x64;
	static final int MPU9150_RA_I2C_SLV2_DO      = 0x65;
	static final int MPU9150_RA_I2C_SLV3_DO      = 0x66;
	static final int MPU9150_RA_I2C_MST_DELAY_CTRL= 0x67;
	static final int MPU9150_RA_SIGNAL_PATH_RESET= 0x68;
	static final int MPU9150_RA_MOT_DETECT_CTRL  = 0x69;
	static final int MPU9150_RA_USER_CTRL        = 0x6A;
	static final int MPU9150_RA_PWR_MGMT_1       = 0x6B;
	static final int MPU9150_RA_PWR_MGMT_2       = 0x6C;
	static final int MPU9150_RA_BANK_SEL         = 0x6D;
	static final int MPU9150_RA_MEM_START_ADDR   = 0x6E;
	static final int MPU9150_RA_MEM_R_W          = 0x6F;
	static final int MPU9150_RA_DMP_CFG_1        = 0x70;
	static final int MPU9150_RA_DMP_CFG_2        = 0x71;
	static final int MPU9150_RA_FIFO_COUNTH      = 0x72;
	static final int MPU9150_RA_FIFO_COUNTL      = 0x73;
	static final int MPU9150_RA_FIFO_R_W         = 0x74;
	static final int MPU9150_RA_WHO_AM_I         = 0x75;
	
	// Undocumented registers?
	static final int MPU9150_RA_XG_OFFS_TC       = 0x00; //[7] PWR_MODE, [6:1] XG_OFFS_TC, [0] OTP_BNK_VLD
	static final int MPU9150_RA_YG_OFFS_TC       = 0x01; //[7] PWR_MODE, [6:1] YG_OFFS_TC, [0] OTP_BNK_VLD
	static final int MPU9150_RA_ZG_OFFS_TC       = 0x02; //[7] PWR_MODE, [6:1] ZG_OFFS_TC, [0] OTP_BNK_VLD
	static final int MPU9150_RA_X_FINE_GAIN      = 0x03; //[7:0] X_FINE_GAIN
	static final int MPU9150_RA_Y_FINE_GAIN      = 0x04; //[7:0] Y_FINE_GAIN
	static final int MPU9150_RA_Z_FINE_GAIN      = 0x05; //[7:0] Z_FINE_GAIN
	static final int MPU9150_RA_XA_OFFS_H        = 0x06; //[15:0] XA_OFFS
	static final int MPU9150_RA_XA_OFFS_L_TC     = 0x07;
	static final int MPU9150_RA_YA_OFFS_H        = 0x08; //[15:0] YA_OFFS
	static final int MPU9150_RA_YA_OFFS_L_TC     = 0x09;
	static final int MPU9150_RA_ZA_OFFS_H        = 0x0A; //[15:0] ZA_OFFS
	static final int MPU9150_RA_ZA_OFFS_L_TC     = 0x0B;
	static final int MPU9150_RA_XG_OFFS_USRH     = 0x13; //[15:0] XG_OFFS_USR
	static final int MPU9150_RA_XG_OFFS_USRL     = 0x14;
	static final int MPU9150_RA_YG_OFFS_USRH     = 0x15; //[15:0] YG_OFFS_USR
	static final int MPU9150_RA_YG_OFFS_USRL     = 0x16;
	static final int MPU9150_RA_ZG_OFFS_USRH     = 0x17; //[15:0] ZG_OFFS_USR
	static final int MPU9150_RA_ZG_OFFS_USRL     = 0x18;

	static final int MPU9150_TC_PWR_MODE_BIT     = 7;
	static final int MPU9150_TC_OFFSET_BIT       = 6;
	static final int MPU9150_TC_OFFSET_LENGTH    = 6;
	static final int MPU9150_TC_OTP_BNK_VLD_BIT  = 0;

	static final int MPU9150_VDDIO_LEVEL_VLOGIC  = 0;
	static final int MPU9150_VDDIO_LEVEL_VDD     = 1;

	static final int MPU9150_CFG_EXT_SYNC_SET_BIT    = 5;
	static final int MPU9150_CFG_EXT_SYNC_SET_LENGTH = 3;
	static final int MPU9150_CFG_DLPF_CFG_BIT    = 2;
	static final int MPU9150_CFG_DLPF_CFG_LENGTH = 3;

	static final int MPU9150_EXT_SYNC_DISABLED       = 0x0;
	static final int MPU9150_EXT_SYNC_TEMP_OUT_L     = 0x1;
	static final int MPU9150_EXT_SYNC_GYRO_XOUT_L    = 0x2;
	static final int MPU9150_EXT_SYNC_GYRO_YOUT_L    = 0x3;
	static final int MPU9150_EXT_SYNC_GYRO_ZOUT_L    = 0x4;
	static final int MPU9150_EXT_SYNC_ACCEL_XOUT_L   = 0x5;
	static final int MPU9150_EXT_SYNC_ACCEL_YOUT_L   = 0x6;
	static final int MPU9150_EXT_SYNC_ACCEL_ZOUT_L   = 0x7;

	static final int MPU9150_DLPF_BW_256         = 0x00;
	static final int MPU9150_DLPF_BW_188         = 0x01;
	static final int MPU9150_DLPF_BW_98          = 0x02;
	static final int MPU9150_DLPF_BW_42          = 0x03;
	static final int MPU9150_DLPF_BW_20          = 0x04;
	static final int MPU9150_DLPF_BW_10          = 0x05;
	static final int MPU9150_DLPF_BW_5           = 0x06;

	static final int MPU9150_GCONFIG_FS_SEL_BIT      = 4;
	static final int MPU9150_GCONFIG_FS_SEL_LENGTH   = 2;

	static final int MPU9150_GYRO_FS_250         = 0x00;
	static final int MPU9150_GYRO_FS_500         = 0x01;
	static final int MPU9150_GYRO_FS_1000        = 0x02;
	static final int MPU9150_GYRO_FS_2000        = 0x03;

	static final int MPU9150_ACONFIG_XA_ST_BIT           = 7;
	static final int MPU9150_ACONFIG_YA_ST_BIT           = 6;
	static final int MPU9150_ACONFIG_ZA_ST_BIT           = 5;
	static final int MPU9150_ACONFIG_AFS_SEL_BIT         = 4;
	static final int MPU9150_ACONFIG_AFS_SEL_LENGTH      = 2;
	static final int MPU9150_ACONFIG_ACCEL_HPF_BIT       = 2;
	static final int MPU9150_ACONFIG_ACCEL_HPF_LENGTH    = 3;

	static final int MPU9150_ACCEL_FS_2          = 0x00;
	static final int MPU9150_ACCEL_FS_4          = 0x01;
	static final int MPU9150_ACCEL_FS_8          = 0x02;
	static final int MPU9150_ACCEL_FS_16         = 0x03;

	static final int MPU9150_DHPF_RESET          = 0x00;
	static final int MPU9150_DHPF_5              = 0x01;
	static final int MPU9150_DHPF_2P5            = 0x02;
	static final int MPU9150_DHPF_1P25           = 0x03;
	static final int MPU9150_DHPF_0P63           = 0x04;
	static final int MPU9150_DHPF_HOLD           = 0x07;

	static final int MPU9150_TEMP_FIFO_EN_BIT    = 7;
	static final int MPU9150_XG_FIFO_EN_BIT      = 6;
	static final int MPU9150_YG_FIFO_EN_BIT      = 5;
	static final int MPU9150_ZG_FIFO_EN_BIT      = 4;
	static final int MPU9150_ACCEL_FIFO_EN_BIT   = 3;
	static final int MPU9150_SLV2_FIFO_EN_BIT    = 2;
	static final int MPU9150_SLV1_FIFO_EN_BIT    = 1;
	static final int MPU9150_SLV0_FIFO_EN_BIT    = 0;

	static final int MPU9150_MULT_MST_EN_BIT     = 7;
	static final int MPU9150_WAIT_FOR_ES_BIT     = 6;
	static final int MPU9150_SLV_3_FIFO_EN_BIT   = 5;
	static final int MPU9150_I2C_MST_P_NSR_BIT   = 4;
	static final int MPU9150_I2C_MST_CLK_BIT     = 3;
	static final int MPU9150_I2C_MST_CLK_LENGTH  = 4;

	static final int MPU9150_CLOCK_DIV_348       = 0x0;
	static final int MPU9150_CLOCK_DIV_333       = 0x1;
	static final int MPU9150_CLOCK_DIV_320       = 0x2;
	static final int MPU9150_CLOCK_DIV_308       = 0x3;
	static final int MPU9150_CLOCK_DIV_296       = 0x4;
	static final int MPU9150_CLOCK_DIV_286       = 0x5;
	static final int MPU9150_CLOCK_DIV_276       = 0x6;
	static final int MPU9150_CLOCK_DIV_267       = 0x7;
	static final int MPU9150_CLOCK_DIV_258       = 0x8;
	static final int MPU9150_CLOCK_DIV_500       = 0x9;
	static final int MPU9150_CLOCK_DIV_471       = 0xA;
	static final int MPU9150_CLOCK_DIV_444       = 0xB;
	static final int MPU9150_CLOCK_DIV_421       = 0xC;
	static final int MPU9150_CLOCK_DIV_400       = 0xD;
	static final int MPU9150_CLOCK_DIV_381       = 0xE;
	static final int MPU9150_CLOCK_DIV_364       = 0xF;

	static final int MPU9150_I2C_SLV_RW_BIT      = 7;
	static final int MPU9150_I2C_SLV_ADDR_BIT    = 6;
	static final int MPU9150_I2C_SLV_ADDR_LENGTH = 7;
	static final int MPU9150_I2C_SLV_EN_BIT      = 7;
	static final int MPU9150_I2C_SLV_BYTE_SW_BIT = 6;
	static final int MPU9150_I2C_SLV_REG_DIS_BIT = 5;
	static final int MPU9150_I2C_SLV_GRP_BIT     = 4;
	static final int MPU9150_I2C_SLV_LEN_BIT     = 3;
	static final int MPU9150_I2C_SLV_LEN_LENGTH  = 4;

	static final int MPU9150_I2C_SLV4_RW_BIT         = 7;
	static final int MPU9150_I2C_SLV4_ADDR_BIT       = 6;
	static final int MPU9150_I2C_SLV4_ADDR_LENGTH    = 7;
	static final int MPU9150_I2C_SLV4_EN_BIT         = 7;
	static final int MPU9150_I2C_SLV4_INT_EN_BIT     = 6;
	static final int MPU9150_I2C_SLV4_REG_DIS_BIT    = 5;
	static final int MPU9150_I2C_SLV4_MST_DLY_BIT    = 4;
	static final int MPU9150_I2C_SLV4_MST_DLY_LENGTH = 5;

	static final int MPU9150_MST_PASS_THROUGH_BIT    = 7;
	static final int MPU9150_MST_I2C_SLV4_DONE_BIT   = 6;
	static final int MPU9150_MST_I2C_LOST_ARB_BIT    = 5;
	static final int MPU9150_MST_I2C_SLV4_NACK_BIT   = 4;
	static final int MPU9150_MST_I2C_SLV3_NACK_BIT   = 3;
	static final int MPU9150_MST_I2C_SLV2_NACK_BIT   = 2;
	static final int MPU9150_MST_I2C_SLV1_NACK_BIT   = 1;
	static final int MPU9150_MST_I2C_SLV0_NACK_BIT   = 0;

	static final int MPU9150_INTCFG_INT_LEVEL_BIT        = 7;
	static final int MPU9150_INTCFG_INT_OPEN_BIT         = 6;
	static final int MPU9150_INTCFG_LATCH_INT_EN_BIT     = 5;
	static final int MPU9150_INTCFG_INT_RD_CLEAR_BIT     = 4;
	static final int MPU9150_INTCFG_FSYNC_INT_LEVEL_BIT  = 3;
	static final int MPU9150_INTCFG_FSYNC_INT_EN_BIT     = 2;
	static final int MPU9150_INTCFG_I2C_BYPASS_EN_BIT    = 1;
	static final int MPU9150_INTCFG_CLKOUT_EN_BIT        = 0;

	static final int MPU9150_INTMODE_ACTIVEHIGH  = 0x00;
	static final int MPU9150_INTMODE_ACTIVELOW   = 0x01;

	static final int MPU9150_INTDRV_PUSHPULL     = 0x00;
	static final int MPU9150_INTDRV_OPENDRAIN    = 0x01;

	static final int MPU9150_INTLATCH_50USPULSE  = 0x00;
	static final int MPU9150_INTLATCH_WAITCLEAR  = 0x01;

	static final int MPU9150_INTCLEAR_STATUSREAD = 0x00;
	static final int MPU9150_INTCLEAR_ANYREAD    = 0x01;

	static final int MPU9150_INTERRUPT_FF_BIT            = 7;
	static final int MPU9150_INTERRUPT_MOT_BIT           = 6;
	static final int MPU9150_INTERRUPT_ZMOT_BIT          = 5;
	static final int MPU9150_INTERRUPT_FIFO_OFLOW_BIT    = 4;
	static final int MPU9150_INTERRUPT_I2C_MST_INT_BIT   = 3;
	static final int MPU9150_INTERRUPT_PLL_RDY_INT_BIT   = 2;
	static final int MPU9150_INTERRUPT_DMP_INT_BIT       = 1;
	static final int MPU9150_INTERRUPT_DATA_RDY_BIT      = 0;

	// TODO: figure out what these actually do
	// UMPL source code is not very obvious
	static final int MPU9150_DMPINT_5_BIT            = 5;
	static final int MPU9150_DMPINT_4_BIT            = 4;
	static final int MPU9150_DMPINT_3_BIT            = 3;
	static final int MPU9150_DMPINT_2_BIT            = 2;
	static final int MPU9150_DMPINT_1_BIT            = 1;
	static final int MPU9150_DMPINT_0_BIT            = 0;

	static final int MPU9150_MOTION_MOT_XNEG_BIT     = 7;
	static final int MPU9150_MOTION_MOT_XPOS_BIT     = 6;
	static final int MPU9150_MOTION_MOT_YNEG_BIT     = 5;
	static final int MPU9150_MOTION_MOT_YPOS_BIT     = 4;
	static final int MPU9150_MOTION_MOT_ZNEG_BIT     = 3;
	static final int MPU9150_MOTION_MOT_ZPOS_BIT     = 2;
	static final int MPU9150_MOTION_MOT_ZRMOT_BIT    = 0;

	static final int MPU9150_DELAYCTRL_DELAY_ES_SHADOW_BIT   = 7;
	static final int MPU9150_DELAYCTRL_I2C_SLV4_DLY_EN_BIT   = 4;
	static final int MPU9150_DELAYCTRL_I2C_SLV3_DLY_EN_BIT   = 3;
	static final int MPU9150_DELAYCTRL_I2C_SLV2_DLY_EN_BIT   = 2;
	static final int MPU9150_DELAYCTRL_I2C_SLV1_DLY_EN_BIT   = 1;
	static final int MPU9150_DELAYCTRL_I2C_SLV0_DLY_EN_BIT   = 0;

	static final int MPU9150_PATHRESET_GYRO_RESET_BIT    = 2;
	static final int MPU9150_PATHRESET_ACCEL_RESET_BIT   = 1;
	static final int MPU9150_PATHRESET_TEMP_RESET_BIT    = 0;

	static final int MPU9150_DETECT_ACCEL_ON_DELAY_BIT       = 5;
	static final int MPU9150_DETECT_ACCEL_ON_DELAY_LENGTH    = 2;
	static final int MPU9150_DETECT_FF_COUNT_BIT             = 3;
	static final int MPU9150_DETECT_FF_COUNT_LENGTH          = 2;
	static final int MPU9150_DETECT_MOT_COUNT_BIT            = 1;
	static final int MPU9150_DETECT_MOT_COUNT_LENGTH         = 2;

	static final int MPU9150_DETECT_DECREMENT_RESET  = 0x0;
	static final int MPU9150_DETECT_DECREMENT_1      = 0x1;
	static final int MPU9150_DETECT_DECREMENT_2      = 0x2;
	static final int MPU9150_DETECT_DECREMENT_4      = 0x3;

	static final int MPU9150_USERCTRL_DMP_EN_BIT             = 7;
	static final int MPU9150_USERCTRL_FIFO_EN_BIT            = 6;
	static final int MPU9150_USERCTRL_I2C_MST_EN_BIT         = 5;
	static final int MPU9150_USERCTRL_I2C_IF_DIS_BIT         = 4;
	static final int MPU9150_USERCTRL_DMP_RESET_BIT          = 3;
	static final int MPU9150_USERCTRL_FIFO_RESET_BIT         = 2;
	static final int MPU9150_USERCTRL_I2C_MST_RESET_BIT      = 1;
	static final int MPU9150_USERCTRL_SIG_COND_RESET_BIT     = 0;

	static final int MPU9150_PWR1_DEVICE_RESET_BIT   = 7;
	static final int MPU9150_PWR1_SLEEP_BIT          = 6;
	static final int MPU9150_PWR1_CYCLE_BIT          = 5;
	static final int MPU9150_PWR1_TEMP_DIS_BIT       = 3;
	static final int MPU9150_PWR1_CLKSEL_BIT         = 2;
	static final int MPU9150_PWR1_CLKSEL_LENGTH      = 3;

	static final int MPU9150_CLOCK_INTERNAL          = 0x00;
	static final int MPU9150_CLOCK_PLL_XGYRO         = 0x01;
	static final int MPU9150_CLOCK_PLL_YGYRO         = 0x02;
	static final int MPU9150_CLOCK_PLL_ZGYRO         = 0x03;
	static final int MPU9150_CLOCK_PLL_EXT32K        = 0x04;
	static final int MPU9150_CLOCK_PLL_EXT19M        = 0x05;
	static final int MPU9150_CLOCK_KEEP_RESET        = 0x07;

	static final int MPU9150_PWR2_LP_WAKE_CTRL_BIT       = 7;
	static final int MPU9150_PWR2_LP_WAKE_CTRL_LENGTH    = 2;
	static final int MPU9150_PWR2_STBY_XA_BIT            = 5;
	static final int MPU9150_PWR2_STBY_YA_BIT            = 4;
	static final int MPU9150_PWR2_STBY_ZA_BIT            = 3;
	static final int MPU9150_PWR2_STBY_XG_BIT            = 2;
	static final int MPU9150_PWR2_STBY_YG_BIT            = 1;
	static final int MPU9150_PWR2_STBY_ZG_BIT            = 0;

	static final int MPU9150_WAKE_FREQ_1P25      = 0x0;
	static final int MPU9150_WAKE_FREQ_2P5       = 0x1;
	static final int MPU9150_WAKE_FREQ_5         = 0x2;
	static final int MPU9150_WAKE_FREQ_10        = 0x3;

	static final int MPU9150_BANKSEL_PRFTCH_EN_BIT       = 6;
	static final int MPU9150_BANKSEL_CFG_USER_BANK_BIT   = 5;
	static final int MPU9150_BANKSEL_MEM_SEL_BIT         = 4;
	static final int MPU9150_BANKSEL_MEM_SEL_LENGTH      = 5;

	static final int MPU9150_WHO_AM_I_BIT        = 6;
	static final int MPU9150_WHO_AM_I_LENGTH     = 6;

	static final int MPU9150_DMP_MEMORY_BANKS        = 8;
	static final int bank_size = 256;
	static final int MPU9150_DMP_MEMORY_BANK_SIZE    = 256;
	static final int MPU9150_DMP_MEMORY_CHUNK_SIZE   = 16;
}
