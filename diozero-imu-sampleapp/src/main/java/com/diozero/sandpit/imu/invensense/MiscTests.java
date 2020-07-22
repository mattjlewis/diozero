package com.diozero.sandpit.imu.invensense;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - IMU Sample App
 * Filename:     MiscTests.java  
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

import org.pmw.tinylog.Logger;

import com.diozero.imu.drivers.invensense.AccelFullScaleRange;
import com.diozero.imu.drivers.invensense.MPU9150DMPConstants;

public class MiscTests {
	public static void main(String[] args) {
		dmp_set_tap_thresh(1, 2, AccelFullScaleRange.INV_FSR_2G);
		dmp_set_tap_thresh(1, 2, AccelFullScaleRange.INV_FSR_4G);
		dmp_set_tap_thresh(1, 2, AccelFullScaleRange.INV_FSR_8G);
		dmp_set_tap_thresh(1, 2, AccelFullScaleRange.INV_FSR_16G);
	}
	
	public static boolean dmp_set_tap_thresh(int axis, int thresh, AccelFullScaleRange accel_fsr) {
		float scaled_thresh = (float)thresh / MPU9150DMPConstants.DMP_SAMPLE_RATE;
		
		int dmp_thresh = Math.round(scaled_thresh * accel_fsr.getSensitivityScaleFactor());
		int dmp_thresh_2 = Math.round(scaled_thresh * accel_fsr.getSensitivityScaleFactor() * 0.75f);
		Logger.info(dmp_thresh + ", " + dmp_thresh_2);

		switch (accel_fsr) {
		case INV_FSR_2G:
			dmp_thresh = Math.round(scaled_thresh * 16384);
			/* dmp_thresh * 0.75 */
			dmp_thresh_2 = Math.round(scaled_thresh * 12288);
			break;
		case INV_FSR_4G:
			dmp_thresh = Math.round(scaled_thresh * 8192);
			/* dmp_thresh * 0.75 */
			dmp_thresh_2 = Math.round(scaled_thresh * 6144);
			break;
		case INV_FSR_8G:
	        dmp_thresh = Math.round(scaled_thresh * 4096);
	        /* dmp_thresh * 0.75 */
	        dmp_thresh_2 = Math.round(scaled_thresh * 3072);
	        break;
	    case INV_FSR_16G:
	        dmp_thresh = Math.round(scaled_thresh * 2048);
	        /* dmp_thresh * 0.75 */
	        dmp_thresh_2 = Math.round(scaled_thresh * 1536);
	        break;
	    default:
	        return false;
	    }
		Logger.info(dmp_thresh + ", " + dmp_thresh_2);
		
		byte[] tmp1 = new byte[2];
	    tmp1[0] = (byte)(dmp_thresh >> 8);
	    tmp1[1] = (byte)(dmp_thresh & 0xFF);
		byte[] tmp2 = new byte[2];
	    tmp2[0] = (byte)(dmp_thresh_2 >> 8);
	    tmp2[1] = (byte)(dmp_thresh_2 & 0xFF);

	    return true;
	}

}
