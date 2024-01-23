package com.diozero.devices.imu.invensense;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - IMU device classes
 * Filename:     AK8975Constants.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2024 diozero
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


public interface AK8975Constants {
	// TODO Validate scale of the compass data?!
	// 0.3f is used here: https://github.com/richards-tech/RTIMULib/blob/master/RTIMULib/IMUDrivers/RTIMUMPU9150.cpp
	public static final double COMPASS_SCALE = 0.3;
	
	// I2C address for the AKM magnetometer
	static final int AK8975_MAG_ADDRESS			= 0x0C;
	// Magnetometer registers
	static final int AK8975_RA_MAG_DEVICE_ID	= 0x00;
	static final int AKM_REG_WHOAMI				= 0x00;
	static final int AK8975_RA_MAG_INFO			= 0x01;
	static final int AKM_REG_ST1				= 0x02;
	static final int AK8975_RA_MAG_STATUS_1		= 0x02;
	// Measurement data is stored in two's complement and Little Endian format.
	// 13bit signed value?
	// Measurement range of each axis is from -4096 to +4095 in decimal
	static final int HARDWARE_UNIT				= 4096;
	static final int AK8975_RA_MAG_XOUT_L		= 0x03;
	static final int AKM_REG_HXL				= 0x03;
	static final int AK8975_RA_MAG_XOUT_H		= 0x04;
	static final int AK8975_RA_MAG_YOUT_L		= 0x05;
	static final int AK8975_RA_MAG_YOUT_H		= 0x06;
	static final int AK8975_RA_MAG_ZOUT_L		= 0x07;
	static final int AK8975_RA_MAG_ZOUT_H		= 0x08;
	static final int AK8975_RA_MAG_STATUS_2		= 0x09;
	static final int AKM_REG_ST2				= 0x09;
	static final int AK8975_RA_MAG_CONTROL		= 0x0A;
	static final int AKM_REG_CNTL				= 0x0A;
	static final int AK8975_RA_MAG_RESERVED		= 0x0B;
	static final int AK8975_RA_MAG_SELF_TST_CTL	= 0x0C;
	static final int AKM_REG_ASTC				= 0x0C;
	static final int AK8975_RA_MAG_TEST_1		= 0x0D;
	static final int AK8975_RA_MAG_TEST_2		= 0x0E;
	static final int AK8975_RA_I2C_DISABLE		= 0x0F;
	static final int AK8975_RA_SENS_ADJ_X		= 0x10;
	static final int AKM_REG_ASAX				= 0x10;
	static final int AK8975_RA_SENS_ADJ_Y		= 0x11;
	static final int AKM_REG_ASAY				= 0x11;
	static final int AK8975_RA_SENS_ADJ_Z		= 0x12;
	static final int AKM_REG_ASAZ				= 0x12;
	static final int SUPPORTS_AK89xx_HIGH_SENS	= 0x00;

	static final byte AKM_DATA_READY      		= 0x01;
	static final byte AKM_DATA_OVERRUN    		= 0x02;
	static final byte AKM_OVERFLOW        		= (byte)0x80;
	static final byte AKM_DATA_ERROR      		= 0x40;

	static final byte AKM_BIT_SELF_TEST   		= 0x40;

	static final byte AKM_POWER_DOWN         	= (0x00 | SUPPORTS_AK89xx_HIGH_SENS);
	static final byte AKM_SINGLE_MEASUREMENT 	= (0x01 | SUPPORTS_AK89xx_HIGH_SENS);
	static final byte AKM_FUSE_ROM_ACCESS    	= (0x0F | SUPPORTS_AK89xx_HIGH_SENS);
	static final byte AKM_MODE_SELF_TEST     	= (0x08 | SUPPORTS_AK89xx_HIGH_SENS);

	static final byte AKM_WHOAMI      			= 0x48;

	static final int AK8975_FSR					= 9830;
	static final int AK8963_FSR					= 4915;
	
	// +/-1200
	static final int MAG_FSR = 2*1200;
	static final int WORLD_LENGTH = 13;
	static final double SENSITIVITY_FACTOR = MAG_FSR / Math.pow(2, 13);
}
