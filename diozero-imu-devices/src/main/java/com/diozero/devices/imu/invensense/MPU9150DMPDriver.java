package com.diozero.devices.imu.invensense;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - IMU device classes
 * Filename:     MPU9150DMPDriver.java
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

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.tinylog.Logger;

import com.diozero.api.RuntimeIOException;
import com.diozero.devices.imu.OrientationEvent;
import com.diozero.devices.imu.OrientationListener;
import com.diozero.devices.imu.TapEvent;
import com.diozero.devices.imu.TapListener;
import com.diozero.devices.imu.OrientationEvent.OrientationType;
import com.diozero.devices.imu.TapEvent.TapType;
import com.diozero.util.Hex;

/**
 * Sensor Driver Layer
 * Hardware drivers to communicate with sensors via I2C.
 *
 * DMP image and interface functions.
 * All functions are preceded by the dmp_ prefix to differentiate among
 *          MPL and general driver function calls.
 */
public class MPU9150DMPDriver implements MPU9150DMPConstants {
	// Not sure how this flag is set
	static final boolean EMPL_NO_64BIT = false;

	private static final boolean FIFO_CORRUPTION_CHECK = true;

	private static final boolean DUMP_FIFO_DATA = false;

	private MPU9150Driver mpu;
	private TapListener tap_cb;
	private OrientationListener android_orient_cb;
	private int orient;
	private int feature_mask;
	private int fifo_rate;
	private int packet_length;

	public MPU9150DMPDriver(MPU9150Driver mpu) {
		this.mpu = mpu;
	}

	/**
	 * Load the DMP with this image.
	 */
	public void dmp_load_motion_driver_firmware() throws RuntimeIOException {
		mpu.mpu_load_firmware(DMP612.DMP_CODE_SIZE, DMP612.dmp_memory, DMP612.sStartAddress, DMP_SAMPLE_RATE);
	}

	/**
	 * Push gyro and accel orientation to the DMP. The orientation is
	 *        represented here as the output of
	 * @param orient Gyro and accel orientation in body frame.
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void dmp_set_orientation(int orient) throws RuntimeIOException {
		byte[] gyro_axes = { DINA4C, DINACD, DINA6C };
		byte[] accel_axes = { DINA0C, DINAC9, DINA2C };
		byte[] gyro_sign = { DINA36, DINA56, DINA76 };
		byte[] accel_sign = { DINA26, DINA46, DINA66 };

		byte[] gyro_regs = new byte[3];
		gyro_regs[0] = gyro_axes[orient & 3];
		gyro_regs[1] = gyro_axes[(orient >> 3) & 3];
		gyro_regs[2] = gyro_axes[(orient >> 6) & 3];

		byte[] accel_regs = new byte[3];
		accel_regs[0] = accel_axes[orient & 3];
		accel_regs[1] = accel_axes[(orient >> 3) & 3];
		accel_regs[2] = accel_axes[(orient >> 6) & 3];

		/* Chip-to-body, axes only. */
		mpu.mpu_write_mem(DMP612.FCFG_1, gyro_regs);
		mpu.mpu_write_mem(DMP612.FCFG_2, accel_regs);

		// memcpy(dest, src, length);
		// memcpy(gyro_regs, gyro_sign, 3);
		System.arraycopy(gyro_sign, 0, gyro_regs, 0, 3);
		// memcpy(accel_regs, accel_sign, 3);
		System.arraycopy(accel_sign, 0, accel_regs, 0, 3);
		if ((orient & 4) != 0) {
			gyro_regs[0] |= 1;
			accel_regs[0] |= 1;
		}
		if ((orient & 0x20) != 0) {
			gyro_regs[1] |= 1;
			accel_regs[1] |= 1;
		}
		if ((orient & 0x100) != 0) {
			gyro_regs[2] |= 1;
			accel_regs[2] |= 1;
		}

		/* Chip-to-body, sign only. */
		mpu.mpu_write_mem(DMP612.FCFG_3, gyro_regs);
		mpu.mpu_write_mem(DMP612.FCFG_7, accel_regs);

		this.orient = orient;
	}

	/**
	 * Push gyro biases to the DMP. Because the gyro integration is
	 *        handled in the DMP, any gyro biases calculated by the MPL should
	 *        be pushed down to DMP memory to remove 3-axis quaternion drift. 
	 *        NOTE: If the DMP-based gyro calibration is enabled, the DMP will
	 *        overwrite the biases written to this location once a new one is
	 *        computed.
	 * @param bias Gyro biases in q16.
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void dmp_set_gyro_bias(long[] bias) throws RuntimeIOException {
		long[] gyro_bias_body = new long[3];
		byte[] regs = new byte[4];

		gyro_bias_body[0] = bias[orient & 3];
		if ((orient & 4) != 0) {
			gyro_bias_body[0] *= -1;
		}
		gyro_bias_body[1] = bias[(orient >> 3) & 3];
		if ((orient & 0x20) != 0) {
			gyro_bias_body[1] *= -1;
		}
		gyro_bias_body[2] = bias[(orient >> 6) & 3];
		if ((orient & 0x100) != 0) {
			gyro_bias_body[2] *= -1;
		}

		if (EMPL_NO_64BIT) {
			gyro_bias_body[0] = (long) ((gyro_bias_body[0] * GYRO_SF) / 1073741824.f);
			gyro_bias_body[1] = (long) ((gyro_bias_body[1] * GYRO_SF) / 1073741824.f);
			gyro_bias_body[2] = (long) ((gyro_bias_body[2] * GYRO_SF) / 1073741824.f);
		} else {
			gyro_bias_body[0] = ((gyro_bias_body[0] * GYRO_SF) >> 30);
			gyro_bias_body[1] = ((gyro_bias_body[1] * GYRO_SF) >> 30);
			gyro_bias_body[2] = ((gyro_bias_body[2] * GYRO_SF) >> 30);
		}

		regs[0] = (byte) ((gyro_bias_body[0] >> 24) & 0xFF);
		regs[1] = (byte) ((gyro_bias_body[0] >> 16) & 0xFF);
		regs[2] = (byte) ((gyro_bias_body[0] >> 8) & 0xFF);
		regs[3] = (byte) (gyro_bias_body[0] & 0xFF);
		mpu.mpu_write_mem(D_EXT_GYRO_BIAS_X, regs);

		regs[0] = (byte) ((gyro_bias_body[1] >> 24) & 0xFF);
		regs[1] = (byte) ((gyro_bias_body[1] >> 16) & 0xFF);
		regs[2] = (byte) ((gyro_bias_body[1] >> 8) & 0xFF);
		regs[3] = (byte) (gyro_bias_body[1] & 0xFF);
		mpu.mpu_write_mem(D_EXT_GYRO_BIAS_Y, regs);

		regs[0] = (byte) ((gyro_bias_body[2] >> 24) & 0xFF);
		regs[1] = (byte) ((gyro_bias_body[2] >> 16) & 0xFF);
		regs[2] = (byte) ((gyro_bias_body[2] >> 8) & 0xFF);
		regs[3] = (byte) (gyro_bias_body[2] & 0xFF);
		mpu.mpu_write_mem(D_EXT_GYRO_BIAS_Z, regs);
	}

	/**
	 * Push accel biases to the DMP. These biases will be removed from
	 *        the DMP 6-axis quaternion.
	 * @param bias Accel biases in q16.
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void dmp_set_accel_bias(int[] bias) throws RuntimeIOException {
		int accel_sens = mpu.mpu_get_accel_sens();
		long accel_sf = (long) accel_sens << 15;

		int[] accel_bias_body = new int[3];
		accel_bias_body[0] = bias[orient & 3];
		if ((orient & 4) != 0) {
			accel_bias_body[0] *= -1;
		}
		accel_bias_body[1] = bias[(orient >> 3) & 3];
		if ((orient & 0x20) != 0) {
			accel_bias_body[1] *= -1;
		}
		accel_bias_body[2] = bias[(orient >> 6) & 3];
		if ((orient & 0x100) != 0) {
			accel_bias_body[2] *= -1;
		}

		accel_bias_body[0] = Math.round((accel_bias_body[0] * accel_sf) >> 30);
		accel_bias_body[1] = Math.round((accel_bias_body[1] * accel_sf) >> 30);
		accel_bias_body[2] = Math.round((accel_bias_body[2] * accel_sf) >> 30);

		byte[] regs = new byte[12];
		regs[0] = (byte) ((accel_bias_body[0] >> 24) & 0xFF);
		regs[1] = (byte) ((accel_bias_body[0] >> 16) & 0xFF);
		regs[2] = (byte) ((accel_bias_body[0] >> 8) & 0xFF);
		regs[3] = (byte) (accel_bias_body[0] & 0xFF);
		regs[4] = (byte) ((accel_bias_body[1] >> 24) & 0xFF);
		regs[5] = (byte) ((accel_bias_body[1] >> 16) & 0xFF);
		regs[6] = (byte) ((accel_bias_body[1] >> 8) & 0xFF);
		regs[7] = (byte) (accel_bias_body[1] & 0xFF);
		regs[8] = (byte) ((accel_bias_body[2] >> 24) & 0xFF);
		regs[9] = (byte) ((accel_bias_body[2] >> 16) & 0xFF);
		regs[10] = (byte) ((accel_bias_body[2] >> 8) & 0xFF);
		regs[11] = (byte) (accel_bias_body[2] & 0xFF);
		mpu.mpu_write_mem(DMP612.D_ACCEL_BIAS, regs);
	}

	/**
	 * Set DMP output rate. Only used when DMP is on.
	 * @param rate Desired fifo rate (Hz).
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void dmp_set_fifo_rate(int rate) throws RuntimeIOException {
		Logger.debug("dmp_set_fifo_rate({})", rate);

		if (rate > DMP_SAMPLE_RATE) {
			Logger.warn(
					"Requested rate ({}) was greater than DMP_SAMPLE_RATE ({})", rate, DMP_SAMPLE_RATE);
			return;
		}

		int div = DMP_SAMPLE_RATE / rate - 1;
		Logger.debug("dmp_set_fifo_rate(), div={}", div);
		byte[] tmp = new byte[2];
		tmp[0] = (byte) ((div >> 8) & 0xFF);
		tmp[1] = (byte) (div & 0xFF);
		mpu.mpu_write_mem(DMP612.D_0_22, tmp);

		byte[] regs_end = { DINAFE, DINAF2, DINAAB, (byte) 0xc4, DINAAA, DINAF1, DINADF, DINADF, (byte) 0xBB,
				(byte) 0xAF, DINADF, DINADF };
		mpu.mpu_write_mem(DMP612.CFG_6, regs_end);

		fifo_rate = rate;
	}

	/**
	 * Get DMP output rate. return rate Current fifo rate (Hz).
	 * @return dmp FIFO rate
	 */
	public int dmp_get_fifo_rate() {
		return fifo_rate;
	}

	/**
	 * Set tap threshold for a specific axis.
	 * @param axis 1, 2, and 4 for XYZ accel, respectively.
	 * @param thresh Tap threshold, in mg/ms.
	 * @return success status
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public boolean dmp_set_tap_thresh(short axis, int thresh) throws RuntimeIOException {
		if ((axis & TAP_XYZ) == 0 || thresh > 1600) {
			return false;
		}

		float scaled_thresh = (float)thresh / DMP_SAMPLE_RATE;
		
		AccelFullScaleRange accel_fsr = mpu.mpu_get_accel_fsr();
		int dmp_thresh = Math.round(scaled_thresh * accel_fsr.getSensitivityScaleFactor());
		int dmp_thresh_2 = Math.round(scaled_thresh * accel_fsr.getSensitivityScaleFactor() * 0.75f);

		/*
		switch (accel_fsr) {
		case INV_FSR_2G:
			dmp_thresh = Math.round(scaled_thresh * 16384);
			// dmp_thresh * 0.75
			dmp_thresh_2 = Math.round(scaled_thresh * 12288);
			break;
		case INV_FSR_4G:
			dmp_thresh = Math.round(scaled_thresh * 8192);
			// dmp_thresh * 0.75
			dmp_thresh_2 = Math.round(scaled_thresh * 6144);
			break;
		case INV_FSR_8G:
	        dmp_thresh = Math.round(scaled_thresh * 4096);
	        // dmp_thresh * 0.75
	        dmp_thresh_2 = Math.round(scaled_thresh * 3072);
	        break;
	    case INV_FSR_16G:
	        dmp_thresh = Math.round(scaled_thresh * 2048);
	        // dmp_thresh * 0.75
	        dmp_thresh_2 = Math.round(scaled_thresh * 1536);
	        break;
	    default:
	        return false;
	    }
		*/
		
		byte[] tmp1 = new byte[2];
	    tmp1[0] = (byte)(dmp_thresh >> 8);
	    tmp1[1] = (byte)(dmp_thresh & 0xFF);
		byte[] tmp2 = new byte[2];
	    tmp2[0] = (byte)(dmp_thresh_2 >> 8);
	    tmp2[1] = (byte)(dmp_thresh_2 & 0xFF);

	    if ((axis & TAP_X) != 0) {
	        mpu.mpu_write_mem(DMPMap.DMP_TAP_THX, tmp1);
	        mpu.mpu_write_mem(DMP612.D_1_36, tmp2);
	    }
	    if ((axis & TAP_Y) != 0) {
	        mpu.mpu_write_mem(DMPMap.DMP_TAP_THY, tmp1);
	        mpu.mpu_write_mem(DMP612.D_1_40, tmp2);
	    }
	    if ((axis & TAP_Z) != 0) {
	        mpu.mpu_write_mem(DMPMap.DMP_TAP_THZ, tmp1);
	        mpu.mpu_write_mem(DMP612.D_1_44, tmp2);
	    }
	    return true;
	}

	/**
	 * Set which axes will register a tap.
	 * @param axis 1, 2, and 4 for XYZ, respectively.
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void dmp_set_tap_axes(short axis) throws RuntimeIOException {
		byte tmp = 0;

	    if ((axis & TAP_X) != 0) {
	        tmp |= 0x30;
	    }
	    if ((axis & TAP_Y) != 0) {
	        tmp |= 0x0C;
	    }
	    if ((axis & TAP_Z) != 0) {
	        tmp |= 0x03;
	    }
	    mpu.mpu_write_mem(DMP612.D_1_72, new byte[] { tmp });
	}

	/**
	 * Set minimum number of taps needed for an interrupt.
	 * @param min_taps Minimum consecutive taps (1-4).
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void dmp_set_tap_count(byte min_taps) throws RuntimeIOException {
		byte tmp;

		if (min_taps < 1) {
			min_taps = 1;
		} else if (min_taps > 4) {
			min_taps = 4;
		}

		tmp = (byte)(min_taps - 1);
		mpu.mpu_write_mem(DMP612.D_1_79, new byte[] { tmp });
	}

	/**
	 * Set length between valid taps.
	 * @param time Milliseconds between taps.
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void dmp_set_tap_time(int time) throws RuntimeIOException {

	    int dmp_time = time / (1000 / DMP_SAMPLE_RATE);
	    byte[] tmp = new byte[2];
	    tmp[0] = (byte)(dmp_time >> 8);
	    tmp[1] = (byte)(dmp_time & 0xFF);
	    mpu.mpu_write_mem(DMPMap.DMP_TAPW_MIN, tmp);
	}

	/**
	 * Set max time between taps to register as a multi-tap.
	 * @param time Max milliseconds between taps.
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void dmp_set_tap_time_multi(int time) throws RuntimeIOException {
		int dmp_time = time / (1000 / DMP_SAMPLE_RATE);
		byte[] tmp = new byte[2];
		tmp[0] = (byte)(dmp_time >> 8);
		tmp[1] = (byte)(dmp_time & 0xFF);
		mpu.mpu_write_mem(DMP612.D_1_218, tmp);
	}

	/**
	 * Set shake rejection threshold. If the DMP detects a gyro sample
	 *        larger than  thresh, taps are rejected.
	 * @param sf Gyro scale factor.
	 * @param thresh Gyro threshold in dps.
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void dmp_set_shake_reject_thresh(long sf, int thresh) throws RuntimeIOException {
	    long thresh_scaled = sf / 1000 * thresh;
	    byte[] tmp = new byte[4];
	    tmp[0] = (byte)((thresh_scaled >> 24) & 0xFF);
	    tmp[1] = (byte)((thresh_scaled >> 16) & 0xFF);
	    tmp[2] = (byte)((thresh_scaled >> 8) & 0xFF);
	    tmp[3] = (byte)(thresh_scaled & 0xFF);
	    mpu.mpu_write_mem(DMP612.D_1_92, tmp);
	}

	/**
	 * Set shake rejection time. Sets the length of time that the gyro
	 *        must be outside of the threshold set by 
	 *        gyro_set_shake_reject_thresh before taps are rejected. A mandatory
	 *        60 ms is added to this parameter.
	 * @param time Time in milliseconds.
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void dmp_set_shake_reject_time(int time) throws RuntimeIOException {
	    time /= (1000 / DMP_SAMPLE_RATE);
	    byte[] tmp = new byte[2];
	    tmp[0] = (byte)(time >> 8);
	    tmp[1] = (byte)(time & 0xFF);
	    mpu.mpu_write_mem(DMP612.D_1_90, tmp);
	}

	/**
	 * Set shake rejection timeout. Sets the length of time after a shake
	 *        rejection that the gyro must stay inside of the threshold before
	 *        taps can be detected again. A mandatory 60 ms is added to this
	 *        parameter.
	 * @param time Time in milliseconds.
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void dmp_set_shake_reject_timeout(int time) throws RuntimeIOException {
		time /= (1000 / DMP_SAMPLE_RATE);
		byte[] tmp = new byte[2];
		tmp[0] = (byte)(time >> 8);
		tmp[1] = (byte)(time & 0xFF);
	    mpu.mpu_write_mem(DMP612.D_1_88, tmp);
	}

	/**
	 * Get current step count. return count Number of steps detected.
	 * @return Step count
	 */
	public long dmp_get_pedometer_step_count() throws RuntimeIOException {
		byte[] tmp = mpu.mpu_read_mem(DMP612.D_PEDSTD_STEPCTR, 4);

		long count = ((long)(tmp[0]&0xff) << 24) | ((long)(tmp[1]&0xff) << 16) |
				((long)(tmp[2]&0xff) << 8) | (tmp[3]&0xff);
		
		return count;
	}

	/**
	 * Overwrite current step count. WARNING: This function writes to DMP
	 *        memory and could potentially encounter a race condition if called
	 *        while the pedometer is enabled.
	 * @param count New step count.
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void dmp_set_pedometer_step_count(long count) throws RuntimeIOException {
		byte[] tmp = new byte[4];
		tmp[0] = (byte)((count >> 24) & 0xFF);
		tmp[1] = (byte)((count >> 16) & 0xFF);
		tmp[2] = (byte)((count >> 8) & 0xFF);
		tmp[3] = (byte)(count & 0xFF);
		mpu.mpu_write_mem(DMP612.D_PEDSTD_STEPCTR, tmp);
	}

	/**
	 * Get duration of walking time.
	 * @return time Walk time in milliseconds.
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public long dmp_get_pedometer_walk_time() throws RuntimeIOException {
		byte[] tmp =  mpu.mpu_read_mem(DMP612.D_PEDSTD_TIMECTR, 4);

		return (((long)tmp[0] << 24) | ((long)(tmp[1] & 0xff) << 16) |
				((long)(tmp[2] & 0xff) << 8) | (tmp[3] & 0xff)) * 20;
	}

	/**
	 * Overwrite current walk time. WARNING: This function writes to DMP
	 *        memory and could potentially encounter a race condition if called
	 *        while the pedometer is enabled.
	 * @param time New walk time in milliseconds.
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void dmp_set_pedometer_walk_time(long time) throws RuntimeIOException {
	    time /= 20;

	    byte[] tmp = new byte[4];
	    tmp[0] = (byte)((time >> 24) & 0xFF);
	    tmp[1] = (byte)((time >> 16) & 0xFF);
	    tmp[2] = (byte)((time >> 8) & 0xFF);
	    tmp[3] = (byte)(time & 0xFF);
	    mpu.mpu_write_mem(DMP612.D_PEDSTD_TIMECTR, tmp);
	}

	/**
	 * Enable DMP features.
	 * The following #define's are used in the input mask: 
	 *        DMP_FEATURE_TAP
	 *        DMP_FEATURE_ANDROID_ORIENT
	 *        DMP_FEATURE_LP_QUAT
	 *        DMP_FEATURE_6X_LP_QUAT
	 *        DMP_FEATURE_GYRO_CAL
	 *        DMP_FEATURE_SEND_RAW_ACCEL
	 *        DMP_FEATURE_SEND_RAW_GYRO
	 *  NOTE: DMP_FEATURE_LP_QUAT and DMP_FEATURE_6X_LP_QUAT are mutually exclusive.
	 *  NOTE: DMP_FEATURE_SEND_RAW_GYRO and DMP_FEATURE_SEND_CAL_GYRO are also mutually exclusive.
	 * @param mask Mask of features to enable.
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void dmp_enable_feature(int mask) throws RuntimeIOException {

		/*
		 * TODO: All of these settings can probably be integrated into the default DMP image.
		 */
		/* Set integration scale factor. */
		byte[] tmp = new byte[4];
		tmp[0] = (byte) ((GYRO_SF >> 24) & 0xFF);
		tmp[1] = (byte) ((GYRO_SF >> 16) & 0xFF);
		tmp[2] = (byte) ((GYRO_SF >> 8) & 0xFF);
		tmp[3] = (byte) (GYRO_SF & 0xFF);
		mpu.mpu_write_mem(DMP612.D_0_104, tmp);

		/* Send sensor data to the FIFO. */
		tmp = new byte[10];
		tmp[0] = (byte) 0xA3;
		if ((mask & DMP_FEATURE_SEND_RAW_ACCEL) != 0) {
			tmp[1] = (byte) 0xC0;
			tmp[2] = (byte) 0xC8;
			tmp[3] = (byte) 0xC2;
		} else {
			tmp[1] = (byte) 0xA3;
			tmp[2] = (byte) 0xA3;
			tmp[3] = (byte) 0xA3;
		}
		if ((mask & DMP_FEATURE_SEND_ANY_GYRO) != 0) {
			tmp[4] = (byte) 0xC4;
			tmp[5] = (byte) 0xCC;
			tmp[6] = (byte) 0xC6;
		} else {
			tmp[4] = (byte) 0xA3;
			tmp[5] = (byte) 0xA3;
			tmp[6] = (byte) 0xA3;
		}
		tmp[7] = (byte) 0xA3;
		tmp[8] = (byte) 0xA3;
		tmp[9] = (byte) 0xA3;
		mpu.mpu_write_mem(DMP612.CFG_15, tmp);

		/* Send gesture data to the FIFO. */
		tmp = new byte[1];
		if ((mask & (DMP_FEATURE_TAP | DMP_FEATURE_ANDROID_ORIENT)) != 0) {
			tmp[0] = DINA20;
		} else {
			tmp[0] = (byte) 0xD8;
		}
		mpu.mpu_write_mem(DMP612.CFG_27, tmp);

		if ((mask & DMP_FEATURE_GYRO_CAL) != 0) {
			dmp_enable_gyro_cal(true);
		} else {
			dmp_enable_gyro_cal(false);
		}

		if ((mask & DMP_FEATURE_SEND_ANY_GYRO) != 0) {
			tmp = new byte[4];
			if ((mask & DMP_FEATURE_SEND_CAL_GYRO) != 0) {
				tmp[0] = (byte) 0xB2;
				tmp[1] = (byte) 0x8B;
				tmp[2] = (byte) 0xB6;
				tmp[3] = (byte) 0x9B;
			} else {
				tmp[0] = DINAC0;
				tmp[1] = DINA80;
				tmp[2] = DINAC2;
				tmp[3] = DINA90;
			}
			mpu.mpu_write_mem(DMP612.CFG_GYRO_RAW_DATA, tmp);
		}

		tmp = new byte[1];
		if ((mask & DMP_FEATURE_TAP) != 0) {
			/* Enable tap. */
			tmp[0] = (byte) 0xF8;
			mpu.mpu_write_mem(DMP612.CFG_20, tmp);
			dmp_set_tap_thresh(TAP_XYZ, 250);
			dmp_set_tap_axes(TAP_XYZ);
			dmp_set_tap_count((byte)1);
			dmp_set_tap_time(100);
			dmp_set_tap_time_multi(500);

			dmp_set_shake_reject_thresh(GYRO_SF, 200);
			dmp_set_shake_reject_time(40);
			dmp_set_shake_reject_timeout(10);
		} else {
			tmp[0] = (byte) 0xD8;
			mpu.mpu_write_mem(DMP612.CFG_20, tmp);
		}

		tmp = new byte[1];
		if ((mask & DMP_FEATURE_ANDROID_ORIENT) != 0) {
			tmp[0] = (byte) 0xD9;
		} else {
			tmp[0] = (byte) 0xD8;
		}
		mpu.mpu_write_mem(DMP612.CFG_ANDROID_ORIENT_INT, tmp);

		if ((mask & DMP_FEATURE_LP_QUAT) != 0) {
			dmp_enable_lp_quat(true);
		} else {
			dmp_enable_lp_quat(false);
		}

		if ((mask & DMP_FEATURE_6X_LP_QUAT) != 0) {
			dmp_enable_6x_lp_quat(true);
		} else {
			dmp_enable_6x_lp_quat(false);
		}

		/* Pedometer is always enabled. */
		feature_mask = mask | DMP_FEATURE_PEDOMETER;
		mpu.mpu_reset_fifo();

		packet_length = 0;
		if ((mask & DMP_FEATURE_SEND_RAW_ACCEL) != 0) {
			packet_length += 6;
		}
		if ((mask & DMP_FEATURE_SEND_ANY_GYRO) != 0) {
			packet_length += 6;
		}
		if ((mask & (DMP_FEATURE_LP_QUAT | DMP_FEATURE_6X_LP_QUAT)) != 0) {
			packet_length += 16;
		}
		if ((mask & (DMP_FEATURE_TAP | DMP_FEATURE_ANDROID_ORIENT)) != 0) {
			packet_length += 4;
		}
	}

	/**
	 * Get list of currently enabled DMP features.
	 * @return Mask of enabled features.
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public int dmp_get_enabled_features() {
		return feature_mask;
	}

	/**
	 * Calibrate the gyro data in the DMP. After eight seconds of no
	 *        motion, the DMP will compute gyro biases and subtract them from
	 *        the quaternion output. If dmp_enable_feature is called with 
	 *        DMP_FEATURE_SEND_CAL_GYRO, the biases will also be subtracted from
	 *        the gyro output.
	 * @param enable 1 to enable gyro calibration.
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void dmp_enable_gyro_cal(boolean enable) throws RuntimeIOException {
		if (enable) {
			byte[] regs = new byte[] { (byte) 0xb8, (byte) 0xaa, (byte) 0xb3, (byte) 0x8d, (byte) 0xb4, (byte) 0x98,
					(byte) 0x0d, (byte) 0x35, (byte) 0x5d };
			mpu.mpu_write_mem(DMP612.CFG_MOTION_BIAS, regs);
		} else {
			byte[] regs = new byte[] { (byte) 0xb8, (byte) 0xaa, (byte) 0xaa, (byte) 0xaa, (byte) 0xb0, (byte) 0x88,
					(byte) 0xc3, (byte) 0xc5, (byte) 0xc7 };
			mpu.mpu_write_mem(DMP612.CFG_MOTION_BIAS, regs);
		}
	}

	/**
	 * Generate 3-axis quaternions from the DMP. In this driver, the
	 *        3-axis and 6-axis DMP quaternion features are mutually exclusive.
	 * @param enable 1 to enable 3-axis quaternion.
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void dmp_enable_lp_quat(boolean enable) throws RuntimeIOException {
		byte[] regs = new byte[4];
		if (enable) {
			regs[0] = DINBC0;
			regs[1] = DINBC2;
			regs[2] = DINBC4;
			regs[3] = DINBC6;
		} else {
			// memset(void *s, int c, size_t n)
			// The memset() function fills the first n bytes of the memory area
			// pointed to by s with the constant byte c
			// memset(regs, 0x8B, 4);
			regs[0] = (byte) 0x8B;
			regs[1] = (byte) 0x8B;
			regs[2] = (byte) 0x8B;
			regs[3] = (byte) 0x8B;
		}

		mpu.mpu_write_mem(DMP612.CFG_LP_QUAT, regs);

		mpu.mpu_reset_fifo();
	}

	/**
	 * Generate 6-axis quaternions from the DMP. In this driver, the
	 *        3-axis and 6-axis DMP quaternion features are mutually exclusive.
	 * @param enable 1 to enable 6-axis quaternion.
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void dmp_enable_6x_lp_quat(boolean enable) throws RuntimeIOException {
		byte[] regs = new byte[4];
		if (enable) {
			regs[0] = DINA20;
			regs[1] = DINA28;
			regs[2] = DINA30;
			regs[3] = DINA38;
		} else {
			// memset(regs, 0xA3, 4);
			regs[0] = (byte) 0xA3;
			regs[1] = (byte) 0xA3;
			regs[2] = (byte) 0xA3;
			regs[3] = (byte) 0xA3;
		}

		mpu.mpu_write_mem(DMP612.CFG_8, regs);

		mpu.mpu_reset_fifo();
	}

	/**
	 * Decode the four-byte gesture data and execute any callbacks.
	 * @param gesture Gesture data from DMP packet.
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void decode_gesture(byte[] gesture) {
		Logger.debug("decode_gesture()", Hex.encodeHexString(gesture));
		if ((gesture[1] & INT_SRC_TAP) != 0) {
			Logger.debug("decode_gesture() got Tap!");
			byte tap = (byte) (0x3F & gesture[3]);
			byte direction = (byte) (tap >> 3);
			byte count = (byte) ((tap % 8) + 1);
			if (tap_cb != null) {
				tap_cb.accept(new TapEvent(createTapType(direction), count));
			}
		}

		if ((gesture[1] & INT_SRC_ANDROID_ORIENT) != 0) {
			Logger.debug("decode_gesture() got Orient!");
			byte android_orient = (byte) (gesture[3] & 0xC0);
			if (android_orient_cb != null) {
				android_orient_cb.accept(new OrientationEvent(getOrientationType((short) (android_orient >> 6))));
			}
		}
	}

	private static TapType createTapType(byte direction) {
		TapType type;
		switch (direction) {
		case 1:
			type = TapType.TAP_X_UP;
			break;
		case 2:
			type = TapType.TAP_X_DOWN;
			break;
		case 3:
			type = TapType.TAP_Y_UP;
			break;
		case 4:
			type = TapType.TAP_Y_DOWN;
			break;
		case 5:
			type = TapType.TAP_Z_UP;
			break;
		case 6:
			type = TapType.TAP_Z_DOWN;
			break;
		default:
			type = TapType.UNKNOWN;
		}
		
		return type;
	}

	private static OrientationType getOrientationType(short s) {
		OrientationType orientation;
		switch (s) {
		case 0:
			orientation = OrientationEvent.OrientationType.PORTRAIT;
			break;
		case 1:
			orientation = OrientationEvent.OrientationType.LANDSCAPE;
			break;
		case 2:
			orientation = OrientationEvent.OrientationType.REVERSE_PORTRAIT;
			break;
		case 3:
			orientation = OrientationEvent.OrientationType.REVERSE_LANDSCAPE;
			break;
		default:
			orientation = OrientationEvent.OrientationType.UNKOWN;
		}
		
		return orientation;
	}

	/**
	 * Specify when a DMP interrupt should occur. A DMP interrupt can be
	 *        configured to trigger on either of the two conditions below:
	 *        a. One FIFO period has elapsed (set by  mpu_set_sample_rate).
	 *        b. A tap event has been detected.
	 * @param mode DMP_INT_GESTURE or DMP_INT_CONTINUOUS.
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void dmp_set_interrupt_mode(short mode) throws RuntimeIOException {
		byte[] regs_continuous = new byte[] { (byte) 0xd8, (byte) 0xb1, (byte) 0xb9, (byte) 0xf3, (byte) 0x8b,
				(byte) 0xa3, (byte) 0x91, (byte) 0xb6, (byte) 0x09, (byte) 0xb4, (byte) 0xd9 };
		byte[] regs_gesture = new byte[] { (byte) 0xda, (byte) 0xb1, (byte) 0xb9, (byte) 0xf3, (byte) 0x8b, (byte) 0xa3,
				(byte) 0x91, (byte) 0xb6, (byte) 0xda, (byte) 0xb4, (byte) 0xda };

		switch (mode) {
		case DMP_INT_CONTINUOUS:
			mpu.mpu_write_mem(DMP612.CFG_FIFO_ON_EVENT, regs_continuous);
			break;
		case DMP_INT_GESTURE:
			mpu.mpu_write_mem(DMP612.CFG_FIFO_ON_EVENT, regs_gesture);
			break;
		}
	}

	/**
	 * Get one packet from the FIFO. If  sensors does not contain a
	 *        particular sensor, disregard the data returned to that pointer.
	 *         sensors can contain a combination of the following flags:
	 *        INV_X_GYRO, INV_Y_GYRO, INV_Z_GYRO
	 *        INV_XYZ_GYRO
	 *        INV_XYZ_ACCEL
	 *        INV_WXYZ_QUAT
	 *        If the FIFO has no new data, sensors will be zero.
	 *        If the FIFO is disabled,  sensors will be zero and this function will return a non-zero error code.
	 * @return FIFOData: gyro Gyro data in hardware units. accel Accel data in
	 *         hardware units. quat 3-axis quaternion data in hardware units.
	 *         timestamp Timestamp in milliseconds. sensors Mask of sensors read
	 *         from FIFO. more Number of remaining packets.
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public MPU9150FIFOData dmp_read_fifo() throws RuntimeIOException {

		/*
		 * TODO: sensors[0] only changes when dmp_enable_feature is called. We
		 * can cache this value and save some cycles.
		 */
		byte sensors = 0;

		/* Get a packet. */
		FIFOStream fifo_stream = mpu.mpu_read_fifo_stream(packet_length);
		if (fifo_stream == null) {
			//Logger.warn("FIFO stream was null");
			return null;
		}
		// unsigned char fifo_data[MAX_PACKET_LENGTH];
		byte[] fifo_data = fifo_stream.getData();
		if (DUMP_FIFO_DATA) {
			Logger.trace("FIFO Data: {}", Hex.encodeHexString(fifo_data));
		}
		ByteBuffer fifo_data_buffer = ByteBuffer.wrap(fifo_data);

		// DMP driver has this as long, which is int in Java
		int[] quat = new int[4];
		byte ii = 0;

		/* Parse DMP packet. */
		if ((feature_mask & (DMP_FEATURE_LP_QUAT | DMP_FEATURE_6X_LP_QUAT)) != 0) {
			int[] quat_q14 = new int[4];
			int quat_mag_sq;
			// quat[0] = ((long)fifo_data[0] << 24) | ((long)fifo_data[1] << 16) |
			//	((long)fifo_data[2] << 8) | fifo_data[3];
			// quat[1] = ((long)fifo_data[4] << 24) | ((long)fifo_data[5] << 16) |
			//	((long)fifo_data[6] << 8) | fifo_data[7];
			// quat[2] = ((long)fifo_data[8] << 24) | ((long)fifo_data[9] << 16) |
			//	((long)fifo_data[10] << 8) | fifo_data[11];
			// quat[3] = ((long)fifo_data[12] << 24) | ((long)fifo_data[13] << 16) |
			//	((long)fifo_data[14] << 8) | fifo_data[15];
			quat[0] = fifo_data_buffer.getInt();
			quat[1] = fifo_data_buffer.getInt();
			quat[2] = fifo_data_buffer.getInt();
			quat[3] = fifo_data_buffer.getInt();
			ii += 16;
			if (FIFO_CORRUPTION_CHECK) {
				/*
				 * We can detect a corrupted FIFO by monitoring the quaternion
				 * data and ensuring that the magnitude is always normalised to
				 * one. This shouldn't happen in normal operation, but if an I2C
				 * error occurs, the FIFO reads might become misaligned.
				 *
				 * Let's start by scaling down the quaternion data to avoid long
				 * long math.
				 */
				quat_q14[0] = quat[0] >> 16;
				quat_q14[1] = quat[1] >> 16;
				quat_q14[2] = quat[2] >> 16;
				quat_q14[3] = quat[3] >> 16;
				quat_mag_sq = quat_q14[0] * quat_q14[0] + quat_q14[1] * quat_q14[1] + quat_q14[2] * quat_q14[2]
						+ quat_q14[3] * quat_q14[3];
				if ((quat_mag_sq < QUAT_MAG_SQ_MIN) || (quat_mag_sq > QUAT_MAG_SQ_MAX)) {
					/* Quaternion is outside of the acceptable threshold. */
					Logger.error("Quaternion is outside of the acceptable threshold.");
					mpu.mpu_reset_fifo();
					sensors = 0;
					return null;
				}
			}
			sensors |= INV_WXYZ_QUAT;
		}

		short[] accel = new short[3];
		if ((feature_mask & DMP_FEATURE_SEND_RAW_ACCEL) != 0) {
			// accel[0] = (short)((fifo_data[ii+0] << 8) | (fifo_data[ii+1] & 0xff));
			// accel[1] = (short)((fifo_data[ii+2] << 8) | (fifo_data[ii+3] & 0xff));
			// accel[2] = (short)((fifo_data[ii+4] << 8) | (fifo_data[ii+5] & 0xff));
			accel[0] = fifo_data_buffer.getShort();
			accel[1] = fifo_data_buffer.getShort();
			accel[2] = fifo_data_buffer.getShort();
			ii += 6;
			sensors |= MPU9150Constants.INV_XYZ_ACCEL;
		}

		short[] gyro = new short[3];
		if ((feature_mask & DMP_FEATURE_SEND_ANY_GYRO) != 0) {
			// gyro[0] = (short)((fifo_data[ii+0] << 8) | (fifo_data[ii+1] & 0xff));
			// gyro[1] = (short)((fifo_data[ii+2] << 8) | (fifo_data[ii+3] & 0xff));
			// gyro[2] = (short)((fifo_data[ii+4] << 8) | (fifo_data[ii+5] & 0xff));
			gyro[0] = fifo_data_buffer.getShort();
			gyro[1] = fifo_data_buffer.getShort();
			gyro[2] = fifo_data_buffer.getShort();
			ii += 6;
			sensors |= MPU9150Constants.INV_XYZ_GYRO;
		}

		/*
		 * Gesture data is at the end of the DMP packet. Parse it and call the
		 * gesture callbacks (if registered).
		 */
		if ((feature_mask & (DMP_FEATURE_TAP | DMP_FEATURE_ANDROID_ORIENT)) != 0) {
			decode_gesture(Arrays.copyOfRange(fifo_data, ii, ii + 4));
		}

		long timestamp = System.currentTimeMillis();

		return new MPU9150FIFOData(gyro, accel, quat, timestamp, sensors, fifo_stream.getMore());
	}

	/**
	 * Register a function to be executed on a tap event. The tap
	 *        direction is represented by one of the following:
	 *        TAP_X_UP
	 *        TAP_X_DOWN
	 *        TAP_Y_UP
	 *        TAP_Y_DOWN
	 *        TAP_Z_UP
	 *        TAP_Z_DOWN
	 * @param func Callback function.
	 */
	// public void dmp_register_tap_cb(void (*func)(unsigned char, unsigned
	// char)) throws RuntimeIOException {
	public void dmp_register_tap_cb(TapListener func) {
		tap_cb = func;
	}

	/**
	 * Register a function to be executed on a android orientation event.
	 * @param func Callback function.
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void dmp_register_android_orient_cb(OrientationListener func) {
		android_orient_cb = func;
	}
	
	/* These next two functions convert the orientation matrix (see
	 * gyro_orientation) to a scalar representation for use by the DMP.
	 * NOTE: These functions are borrowed from InvenSense's MPL.
	 */
	private static int inv_row_2_scale(byte[] row) {
	    int b;

	    if (row[0] > 0) {
	        b = 0;
	    } else if (row[0] < 0) {
	        b = 4;
	    } else if (row[1] > 0) {
	        b = 1;
	    } else if (row[1] < 0) {
	        b = 5;
	    } else if (row[2] > 0) {
	        b = 2;
	    } else if (row[2] < 0) {
	        b = 6;
	    } else {
	        b = 7;      // error
	    }
	    
	    return b;
	}
	
	public static int inv_orientation_matrix_to_scalar(byte[][] matrix) {
	    int scalar;
	    /*
	       XYZ  010_001_000 Identity Matrix
	       XZY  001_010_000
	       YXZ  010_000_001
	       YZX  000_010_001
	       ZXY  001_000_010
	       ZYX  000_001_010
	     */
	    scalar = inv_row_2_scale(matrix[0]);
	    scalar |= inv_row_2_scale(matrix[1]) << 3;
	    scalar |= inv_row_2_scale(matrix[2]) << 6;
	    
	    return scalar;		
	}
}
