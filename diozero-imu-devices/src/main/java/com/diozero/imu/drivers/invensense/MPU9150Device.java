package com.diozero.imu.drivers.invensense;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - IMU device classes
 * Filename:     MPU9150Device.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2021 diozero
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

import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import com.diozero.api.I2CConstants;
import com.diozero.api.RuntimeIOException;
import com.diozero.devices.imu.ImuData;
import com.diozero.devices.imu.ImuDataFactory;
import com.diozero.devices.imu.ImuInterface;
import com.diozero.devices.imu.OrientationListener;
import com.diozero.devices.imu.TapListener;

public class MPU9150Device implements ImuInterface {
	private static final int DEFAULT_FIFO_RATE = 20;
	private static final String DEVICE_NAME = "MPU-9150";
	
	private MPU9150Driver mpu;
	private MPU9150DMPDriver dmp;
	private Collection<TapListener> tapListeners;
	private Collection<OrientationListener> orientationListeners;
	
	public MPU9150Device() throws RuntimeIOException {
		this(I2CConstants.CONTROLLER_1, DEFAULT_FIFO_RATE);
	}
	
	public MPU9150Device(int controller, int fifoRate) throws RuntimeIOException {
		orientationListeners = new ArrayList<>();
		tapListeners = new ArrayList<>();
		
		mpu = new MPU9150Driver(controller);
		mpu.mpu_init();
		dmp = new MPU9150DMPDriver(mpu);
		
		mpu.mpu_set_sensors((byte) (MPU9150Constants.INV_XYZ_GYRO | MPU9150Constants.INV_XYZ_ACCEL |
				MPU9150Constants.INV_XYZ_COMPASS));
		dmp.dmp_load_motion_driver_firmware();
		mpu.mpu_set_dmp_state(true);
		
		dmp.dmp_register_tap_cb(event -> tapListeners.forEach(listener -> listener.accept(event)));
		dmp.dmp_register_android_orient_cb(event -> orientationListeners.forEach(listener -> listener.accept(event)));
		
		int hal_dmp_features =
				MPU9150DMPConstants.DMP_FEATURE_TAP |
				MPU9150DMPConstants.DMP_FEATURE_ANDROID_ORIENT |
				MPU9150DMPConstants.DMP_FEATURE_PEDOMETER |
				MPU9150DMPConstants.DMP_FEATURE_6X_LP_QUAT |
				MPU9150DMPConstants.DMP_FEATURE_SEND_RAW_ACCEL |
				MPU9150DMPConstants.DMP_FEATURE_SEND_CAL_GYRO |
				MPU9150DMPConstants.DMP_FEATURE_GYRO_CAL;
		dmp.dmp_enable_feature(hal_dmp_features);
		
		dmp.dmp_set_fifo_rate(fifoRate);
		mpu.mpu_reset_fifo();
	}
	
	@Override
	public boolean hasGyro() {
		return true;
	}

	@Override
	public boolean hasAccelerometer() {
		return true;
	}
	
	@Override
	public boolean hasCompass() {
		return true;
	}
	
	@Override
	public ImuData getImuData() throws RuntimeIOException {
		return ImuDataFactory.newInstance(mpu.mpu_get_gyro_reg(), mpu.mpu_get_accel_reg(),
				mpu.mpu_get_compass_reg(), mpu.mpu_get_temperature(),
				mpu.mpu_get_gyro_fsr().getScale(), mpu.mpu_get_accel_fsr().getScale(), AK8975Constants.COMPASS_SCALE);
	}
	
	@Override
	public Vector3D getGyroData() throws RuntimeIOException {
		return ImuDataFactory.createVector(mpu.mpu_get_gyro_reg(), mpu.mpu_get_gyro_fsr().getScale());
	}
	
	@Override
	public Vector3D getAccelerometerData() throws RuntimeIOException {
		return ImuDataFactory.createVector(mpu.mpu_get_accel_reg(), mpu.mpu_get_accel_fsr().getScale());
	}
	
	@Override
	public Vector3D getCompassData() throws RuntimeIOException {
		return ImuDataFactory.createVector(mpu.mpu_get_compass_reg(), AK8975Constants.COMPASS_SCALE);
	}
	
	@Override
	public void addTapListener(TapListener listener) {
		tapListeners.add(listener);
	}
	
	@Override
	public void addOrientationListener(OrientationListener listener) {
		orientationListeners.add(listener);
	}
	
	@Override
	public void close() throws RuntimeIOException {
		mpu.close();
	}

	@Override
	public String getImuName() {
		return DEVICE_NAME;
	}

	@Override
	public int getPollInterval() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void startRead() {
		// TODO Auto-generated method stub
	}

	@Override
	public void stopRead() {
		// TODO Auto-generated method stub
	}
}
