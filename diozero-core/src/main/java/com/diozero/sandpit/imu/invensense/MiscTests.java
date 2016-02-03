package com.diozero.sandpit.imu.invensense;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.imu.drivers.invensense.AccelFullScaleRange;
import com.diozero.imu.drivers.invensense.MPU9150DMPConstants;

public class MiscTests {
	private static final Logger logger = LogManager.getLogger(MiscTests.class);
	
	public static void main(String[] args) {
		dmp_set_tap_thresh(1, 2, AccelFullScaleRange.INV_FSR_2G);
		dmp_set_tap_thresh(1, 2, AccelFullScaleRange.INV_FSR_4G);
		dmp_set_tap_thresh(1, 2, AccelFullScaleRange.INV_FSR_8G);
		dmp_set_tap_thresh(1, 2, AccelFullScaleRange.INV_FSR_16G);
	}
	
	public static boolean dmp_set_tap_thresh(int axis, int thresh, AccelFullScaleRange accel_fsr) {
		float scaled_thresh = (float)thresh / MPU9150DMPConstants.DMP_SAMPLE_RATE;
		
		int dmp_thresh = (int)(scaled_thresh * accel_fsr.getSensitivityScaleFactor());
		int dmp_thresh_2 = (int)(scaled_thresh * accel_fsr.getSensitivityScaleFactor()*0.75);
		logger.info(dmp_thresh + ", " + dmp_thresh_2);

		switch (accel_fsr) {
		case INV_FSR_2G:
			dmp_thresh = (int)(scaled_thresh * 16384);
			/* dmp_thresh * 0.75 */
			dmp_thresh_2 = (int)(scaled_thresh * 12288);
			break;
		case INV_FSR_4G:
			dmp_thresh = (int)(scaled_thresh * 8192);
			/* dmp_thresh * 0.75 */
			dmp_thresh_2 = (int)(scaled_thresh * 6144);
			break;
		case INV_FSR_8G:
	        dmp_thresh = (int)(scaled_thresh * 4096);
	        /* dmp_thresh * 0.75 */
	        dmp_thresh_2 = (int)(scaled_thresh * 3072);
	        break;
	    case INV_FSR_16G:
	        dmp_thresh = (int)(scaled_thresh * 2048);
	        /* dmp_thresh * 0.75 */
	        dmp_thresh_2 = (int)(scaled_thresh * 1536);
	        break;
	    default:
	        return false;
	    }
		logger.info(dmp_thresh + ", " + dmp_thresh_2);
		
		byte[] tmp1 = new byte[2];
	    tmp1[0] = (byte)(dmp_thresh >> 8);
	    tmp1[1] = (byte)(dmp_thresh & 0xFF);
		byte[] tmp2 = new byte[2];
	    tmp2[0] = (byte)(dmp_thresh_2 >> 8);
	    tmp2[1] = (byte)(dmp_thresh_2 & 0xFF);

	    return true;
	}

}
