package com.diozero.imu.drivers.invensense;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - IMU device classes
 * Filename:     MPU9150DMPConstants.java  
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


public interface MPU9150DMPConstants {
	static final byte TAP_X               = 0x01;
	static final byte TAP_Y               = 0x02;
	static final byte TAP_Z               = 0x04;
	static final byte TAP_XYZ             = 0x07;

	static final byte TAP_X_UP            = 0x01;
	static final byte TAP_X_DOWN          = 0x02;
	static final byte TAP_Y_UP            = 0x03;
	static final byte TAP_Y_DOWN          = 0x04;
	static final byte TAP_Z_UP            = 0x05;
	static final byte TAP_Z_DOWN          = 0x06;

	static final byte ANDROID_ORIENT_PORTRAIT             = 0x00;
	static final byte ANDROID_ORIENT_LANDSCAPE            = 0x01;
	static final byte ANDROID_ORIENT_REVERSE_PORTRAIT     = 0x02;
	static final byte ANDROID_ORIENT_REVERSE_LANDSCAPE    = 0x03;

	static final byte DMP_INT_GESTURE     = 0x01;
	static final byte DMP_INT_CONTINUOUS  = 0x02;

	static final int DMP_FEATURE_TAP             = 0x001;
	static final int DMP_FEATURE_ANDROID_ORIENT  = 0x002;
	static final int DMP_FEATURE_LP_QUAT         = 0x004;
	static final int DMP_FEATURE_PEDOMETER       = 0x008;
	static final int DMP_FEATURE_6X_LP_QUAT      = 0x010;
	static final int DMP_FEATURE_GYRO_CAL        = 0x020;
	static final int DMP_FEATURE_SEND_RAW_ACCEL  = 0x040;
	static final int DMP_FEATURE_SEND_RAW_GYRO   = 0x080;
	static final int DMP_FEATURE_SEND_CAL_GYRO   = 0x100;

	static final byte DMP_FEATURE_SEND_ANY_GYRO = (byte)(DMP_FEATURE_SEND_RAW_GYRO |  DMP_FEATURE_SEND_CAL_GYRO);

	static final int INV_WXYZ_QUAT       = 0x100;
	
	static final byte INT_SRC_TAP = 0x01;
	static final byte INT_SRC_ANDROID_ORIENT = 0x08;

	static final int MAX_PACKET_LENGTH = 32;

	static final int DMP_SAMPLE_RATE = 200;
	static final long GYRO_SF = 46850825L * 200 / DMP_SAMPLE_RATE;
	
	static final int QUAT_ERROR_THRESH = 1<<24;
	static final int QUAT_MAG_SQ_NORMALIZED = 1<<28;
	static final int QUAT_MAG_SQ_MIN = QUAT_MAG_SQ_NORMALIZED - QUAT_ERROR_THRESH;
	static final int QUAT_MAG_SQ_MAX = QUAT_MAG_SQ_NORMALIZED + QUAT_ERROR_THRESH;

	static final int D_EXT_GYRO_BIAS_X = 61 * 16;
	static final int D_EXT_GYRO_BIAS_Y = (61 * 16) + 4;
	static final int D_EXT_GYRO_BIAS_Z = (61 * 16) + 8;

	static final byte DINA0A = (byte)0x0a;
	static final byte DINA22 = (byte)0x22;
	static final byte DINA42 = (byte)0x42;
	static final byte DINA5A = (byte)0x5a;

	static final byte DINA06 = (byte)0x06;
	static final byte DINA0E = (byte)0x0e;
	static final byte DINA16 = (byte)0x16;
	static final byte DINA1E = (byte)0x1e;
	static final byte DINA26 = (byte)0x26;
	static final byte DINA2E = (byte)0x2e;
	static final byte DINA36 = (byte)0x36;
	static final byte DINA3E = (byte)0x3e;
	static final byte DINA46 = (byte)0x46;
	static final byte DINA4E = (byte)0x4e;
	static final byte DINA56 = (byte)0x56;
	static final byte DINA5E = (byte)0x5e;
	static final byte DINA66 = (byte)0x66;
	static final byte DINA6E = (byte)0x6e;
	static final byte DINA76 = (byte)0x76;
	static final byte DINA7E = (byte)0x7e;

	static final byte DINA00 = (byte)0x00;
	static final byte DINA08 = (byte)0x08;
	static final byte DINA10 = (byte)0x10;
	static final byte DINA18 = (byte)0x18;
	static final byte DINA20 = (byte)0x20;
	static final byte DINA28 = (byte)0x28;
	static final byte DINA30 = (byte)0x30;
	static final byte DINA38 = (byte)0x38;
	static final byte DINA40 = (byte)0x40;
	static final byte DINA48 = (byte)0x48;
	static final byte DINA50 = (byte)0x50;
	static final byte DINA58 = (byte)0x58;
	static final byte DINA60 = (byte)0x60;
	static final byte DINA68 = (byte)0x68;
	static final byte DINA70 = (byte)0x70;
	static final byte DINA78 = (byte)0x78;

	static final byte DINA04 = (byte)0x04;
	static final byte DINA0C = (byte)0x0c;
	static final byte DINA14 = (byte)0x14;
	static final byte DINA1C = (byte)0x1C;
	static final byte DINA24 = (byte)0x24;
	static final byte DINA2C = (byte)0x2c;
	static final byte DINA34 = (byte)0x34;
	static final byte DINA3C = (byte)0x3c;
	static final byte DINA44 = (byte)0x44;
	static final byte DINA4C = (byte)0x4c;
	static final byte DINA54 = (byte)0x54;
	static final byte DINA5C = (byte)0x5c;
	static final byte DINA64 = (byte)0x64;
	static final byte DINA6C = (byte)0x6c;
	static final byte DINA74 = (byte)0x74;
	static final byte DINA7C = (byte)0x7c;

	static final byte DINA01 = (byte)0x01;
	static final byte DINA09 = (byte)0x09;
	static final byte DINA11 = (byte)0x11;
	static final byte DINA19 = (byte)0x19;
	static final byte DINA21 = (byte)0x21;
	static final byte DINA29 = (byte)0x29;
	static final byte DINA31 = (byte)0x31;
	static final byte DINA39 = (byte)0x39;
	static final byte DINA41 = (byte)0x41;
	static final byte DINA49 = (byte)0x49;
	static final byte DINA51 = (byte)0x51;
	static final byte DINA59 = (byte)0x59;
	static final byte DINA61 = (byte)0x61;
	static final byte DINA69 = (byte)0x69;
	static final byte DINA71 = (byte)0x71;
	static final byte DINA79 = (byte)0x79;

	static final byte DINA25 = (byte)0x25;
	static final byte DINA2D = (byte)0x2d;
	static final byte DINA35 = (byte)0x35;
	static final byte DINA3D = (byte)0x3d;
	static final byte DINA4D = (byte)0x4d;
	static final byte DINA55 = (byte)0x55;
	static final byte DINA5D = (byte)0x5D;
	static final byte DINA6D = (byte)0x6d;
	static final byte DINA75 = (byte)0x75;
	static final byte DINA7D = (byte)0x7d;

	static final byte DINADC = (byte)0xdc;
	static final byte DINAF2 = (byte)0xf2;
	static final byte DINAAB = (byte)0xab;
	static final byte DINAAA = (byte)0xaa;
	static final byte DINAF1 = (byte)0xf1;
	static final byte DINADF = (byte)0xdf;
	static final byte DINADA = (byte)0xda;
	static final byte DINAB1 = (byte)0xb1;
	static final byte DINAB9 = (byte)0xb9;
	static final byte DINAF3 = (byte)0xf3;
	static final byte DINA8B = (byte)0x8b;
	static final byte DINAA3 = (byte)0xa3;
	static final byte DINA91 = (byte)0x91;
	static final byte DINAB6 = (byte)0xb6;
	static final byte DINAB4 = (byte)0xb4;


	static final byte DINC00 = (byte)0x00;
	static final byte DINC01 = (byte)0x01;
	static final byte DINC02 = (byte)0x02;
	static final byte DINC03 = (byte)0x03;
	static final byte DINC08 = (byte)0x08;
	static final byte DINC09 = (byte)0x09;
	static final byte DINC0A = (byte)0x0a;
	static final byte DINC0B = (byte)0x0b;
	static final byte DINC10 = (byte)0x10;
	static final byte DINC11 = (byte)0x11;
	static final byte DINC12 = (byte)0x12;
	static final byte DINC13 = (byte)0x13;
	static final byte DINC18 = (byte)0x18;
	static final byte DINC19 = (byte)0x19;
	static final byte DINC1A = (byte)0x1a;
	static final byte DINC1B = (byte)0x1b;

	static final byte DINC20 = (byte)0x20;
	static final byte DINC21 = (byte)0x21;
	static final byte DINC22 = (byte)0x22;
	static final byte DINC23 = (byte)0x23;
	static final byte DINC28 = (byte)0x28;
	static final byte DINC29 = (byte)0x29;
	static final byte DINC2A = (byte)0x2a;
	static final byte DINC2B = (byte)0x2b;
	static final byte DINC30 = (byte)0x30;
	static final byte DINC31 = (byte)0x31;
	static final byte DINC32 = (byte)0x32;
	static final byte DINC33 = (byte)0x33;
	static final byte DINC38 = (byte)0x38;
	static final byte DINC39 = (byte)0x39;
	static final byte DINC3A = (byte)0x3a;
	static final byte DINC3B = (byte)0x3b;

	static final byte DINC40 = (byte)0x40;
	static final byte DINC41 = (byte)0x41;
	static final byte DINC42 = (byte)0x42;
	static final byte DINC43 = (byte)0x43;
	static final byte DINC48 = (byte)0x48;
	static final byte DINC49 = (byte)0x49;
	static final byte DINC4A = (byte)0x4a;
	static final byte DINC4B = (byte)0x4b;
	static final byte DINC50 = (byte)0x50;
	static final byte DINC51 = (byte)0x51;
	static final byte DINC52 = (byte)0x52;
	static final byte DINC53 = (byte)0x53;
	static final byte DINC58 = (byte)0x58;
	static final byte DINC59 = (byte)0x59;
	static final byte DINC5A = (byte)0x5a;
	static final byte DINC5B = (byte)0x5b;

	static final byte DINC60 = (byte)0x60;
	static final byte DINC61 = (byte)0x61;
	static final byte DINC62 = (byte)0x62;
	static final byte DINC63 = (byte)0x63;
	static final byte DINC68 = (byte)0x68;
	static final byte DINC69 = (byte)0x69;
	static final byte DINC6A = (byte)0x6a;
	static final byte DINC6B = (byte)0x6b;
	static final byte DINC70 = (byte)0x70;
	static final byte DINC71 = (byte)0x71;
	static final byte DINC72 = (byte)0x72;
	static final byte DINC73 = (byte)0x73;
	static final byte DINC78 = (byte)0x78;
	static final byte DINC79 = (byte)0x79;
	static final byte DINC7A = (byte)0x7a;
	static final byte DINC7B = (byte)0x7b;

	static final byte DIND40 = (byte)0x40;


	static final byte DINA80 = (byte)0x80;
	static final byte DINA90 = (byte)0x90;
	static final byte DINAA0 = (byte)0xa0;
	static final byte DINAC9 = (byte)0xc9;
	static final byte DINACB = (byte)0xcb;
	static final byte DINACD = (byte)0xcd;
	static final byte DINACF = (byte)0xcf;
	static final byte DINAC8 = (byte)0xc8;
	static final byte DINACA = (byte)0xca;
	static final byte DINACC = (byte)0xcc;
	static final byte DINACE = (byte)0xce;
	static final byte DINAD8 = (byte)0xd8;
	static final byte DINADD = (byte)0xdd;
	static final byte DINAF8 = (byte)0xf0;
	static final byte DINAFE = (byte)0xfe;

	static final byte DINBF8 = (byte)0xf8;
	static final byte DINAC0 = (byte)0xb0;
	static final byte DINAC1 = (byte)0xb1;
	static final byte DINAC2 = (byte)0xb4;
	static final byte DINAC3 = (byte)0xb5;
	static final byte DINAC4 = (byte)0xb8;
	static final byte DINAC5 = (byte)0xb9;
	static final byte DINBC0 = (byte)0xc0;
	static final byte DINBC2 = (byte)0xc2;
	static final byte DINBC4 = (byte)0xc4;
	static final byte DINBC6 = (byte)0xc6;

	
	static final int NUM_BANKS = 12;	
}
