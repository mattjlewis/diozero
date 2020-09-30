package com.diozero.sandpit.imu.invensense;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - IMU Sample App
 * Filename:     MPU9150DriverMqttPublisher.java  
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

import java.nio.ByteBuffer;

import org.apache.commons.math3.complex.Quaternion;
import org.apache.commons.math3.geometry.euclidean.threed.CardanEulerSingularityException;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.tinylog.Logger;

import com.diozero.api.I2CConstants;
import com.diozero.api.imu.ImuData;
import com.diozero.api.imu.MqttConstants;
import com.diozero.api.imu.OrientationEvent;
import com.diozero.api.imu.TapEvent;
import com.diozero.imu.drivers.invensense.AK8975Constants;
import com.diozero.imu.drivers.invensense.AccelFullScaleRange;
import com.diozero.imu.drivers.invensense.GyroFullScaleRange;
import com.diozero.imu.drivers.invensense.LowPassFilter;
import com.diozero.imu.drivers.invensense.MPU9150Constants;
import com.diozero.imu.drivers.invensense.MPU9150DMPConstants;
import com.diozero.imu.drivers.invensense.MPU9150DMPDriver;
import com.diozero.imu.drivers.invensense.MPU9150DataFactory;
import com.diozero.imu.drivers.invensense.MPU9150Driver;
import com.diozero.imu.drivers.invensense.MPU9150FIFOData;
import com.diozero.util.RuntimeIOException;
import com.diozero.util.SleepUtil;

/**
 * From https://github.com/richards-tech/RTIMULib/blob/master/RTIMULib/IMUDrivers/
 * 
 * MPU6050/MPU6500/MPU9150/MPU9250 over I2c for RaspberryPi using official
 * Invensense libraries (v5.1):
 * http://www.invensense.com/developers/index.php?_r=downloads
 */
public class MPU9150DriverMqttPublisher implements MqttConstants {
	private static final float RTIMU_FUZZY_ACCEL_ZERO = 0.05f;
	private static final float RTIMU_FUZZY_GYRO_ZERO = 0.20f;
	
	private static final int DEFAULT_FIFO_RATE = 20;
	
	// MPU driver variables
	private MPU9150DMPDriver dmp;
	private GyroFullScaleRange gyroFsr;
	private AccelFullScaleRange accelFsr;
	private int gyroAccelSampleRate;
	private int sampleRate;
	private int compassSampleRate;
	private LowPassFilter lpf;
	private int fifoRate;
	// Time to sleep between reading from the FIFO (= 1000 / fifoRate)
	private int fifoReadDelayMs;
	private long lastFifoRead;
	private AxisRotation axisRotation;
	
	// MQTT variables
	private String mqttServer;
	private MqttClient mqttClient;
	
	// RTIMULib variables
	private FusionAlgorithmType fusionType;
	private FusionInterface fusion;
	private Vector3D previousAccel;
	private int gyroSampleCount;
	private float gyroLearningAlpha;
	private float gyroContinuousAlpha;
	private Vector3D gyroBias;
	private boolean gyroBiasValid;

	public static void main(String[] args) {
		String mqtt_server = null;
		if (args.length > 0) {
			if (args[0].startsWith("--" + MQTT_SERVER_OPTION + "=")) {
				mqtt_server = args[0].substring(MQTT_SERVER_OPTION.length() + 3);
			}
		}
		new MPU9150DriverMqttPublisher(mqtt_server).run();
	}
	
	public MPU9150DriverMqttPublisher(String mqttServer) {
		// General defaults
		//axisRotation = AxisRotation.RTIMU_XNORTH_YEAST;
		axisRotation = AxisRotation.RTIMU_XEAST_YSOUTH;
		//axisRotation = AxisRotation.RTIMU_XNORTH_YWEST;
		//mpu9150AxisRotation = MPU9150AxisRotation.;
		
		
		// MPU9150 defaults
		gyroAccelSampleRate = 50;
		compassSampleRate = 25;
		lpf = LowPassFilter.INV_FILTER_20HZ;
		gyroFsr = GyroFullScaleRange.INV_FSR_1000DPS;
		accelFsr = AccelFullScaleRange.INV_FSR_8G;
		fifoRate = DEFAULT_FIFO_RATE;
		fifoReadDelayMs = 1000 / fifoRate;
		
		gyroBiasValid = false;
		gyroBias = Vector3D.ZERO;
		previousAccel = Vector3D.ZERO;
		sampleRate = gyroAccelSampleRate;
		
		fusionType = FusionAlgorithmType.RTFUSION_TYPE_RTQF;
		switch (fusionType) {
		case RTFUSION_TYPE_KALMANSTATE4:
			fusion = new RTFusionKalman4();
			break;
		case RTFUSION_TYPE_RTQF:
			fusion = new RTFusionRTQF();
			break;
		default:
			fusion = new RTFusion();
		}
		
		this.mqttServer = mqttServer;
	}
	
	public void run() {
		try (MPU9150Driver mpu = new MPU9150Driver(I2CConstants.BUS_1, I2CConstants.ADDR_SIZE_7,
				MPU9150Constants.I2C_CLOCK_FREQUENCY_FAST)) {
			mpuInit(mpu);
			mqttInit();
			System.err.println("Ready.");

			do {
				ImuData imu_data = update(mpu);
				System.out.print("Got IMU data: compass=[" + imu_data.getCompass() +
						"], temp=" + imu_data.getTemperature() + ", gyro=[" + imu_data.getGyro() +
						"], accel=[" + imu_data.getAccel() + "], quat=[" + imu_data.getQuaternion().getQ0() +
						", " + imu_data.getQuaternion().getQ1() + ", " + imu_data.getQuaternion().getQ2() +
						", " + imu_data.getQuaternion().getQ3() + "], timestamp=" + imu_data.getTimestamp() + ", ");
				
				Quaternion q = imu_data.getQuaternion();
				//double[] ypr = q.toEuler();
				//double[] ypr = quat.getYawPitchRoll();
				Rotation r = new Rotation(q.getQ0(), q.getQ1(), q.getQ2(), q.getQ3(), true);
				double[] ypr = null;
				try {
					ypr = r.getAngles(RotationOrder.XYZ, RotationConvention.VECTOR_OPERATOR);
				} catch (CardanEulerSingularityException e) {
					ypr =  new double[] { 2*Math.atan2(q.getQ1(), q.getQ0()), Math.PI/2, 0};
					System.out.print("Singularity detected, ");
				}
				Logger.info("ypr=[{}, {}, {}]", Double.valueOf(ypr[0]), Double.valueOf(ypr[1]), Double.valueOf(ypr[2]));
				
				mqttPublish(imu_data, ypr);
			} while (true);

		} catch (RuntimeIOException ioe) {
			Logger.error(ioe, "Error: {}", ioe);
		} catch (MqttException me) {
			Logger.error(me, "Error: {}", me);
		} finally {
			try { mqttClient.disconnect(); } catch (Exception e) { }
		}
	}
	
	private void mqttPublish(ImuData imu_data, double[] ypr) throws MqttException {
		if (mqttClient != null) {
			MqttMessage message = new MqttMessage();
			message.setQos(MQTT_QOS_AT_MOST_ONCE);
			
			// 4 sets of 3 doubles, 1 set of 4 doubles
			byte[] bytes = new byte[4*3*8 + 1*4*8];
			ByteBuffer buffer = ByteBuffer.wrap(bytes);
			
			buffer.putDouble(imu_data.getCompass().getX());
			buffer.putDouble(imu_data.getCompass().getY());
			buffer.putDouble(imu_data.getCompass().getZ());

			buffer.putDouble(imu_data.getAccel().getX());
			buffer.putDouble(imu_data.getAccel().getY());
			buffer.putDouble(imu_data.getAccel().getZ());
			
			buffer.putDouble(imu_data.getGyro().getX());
			buffer.putDouble(imu_data.getGyro().getY());
			buffer.putDouble(imu_data.getGyro().getZ());
			
			buffer.putDouble(imu_data.getQuaternion().getQ0());
			buffer.putDouble(imu_data.getQuaternion().getQ1());
			buffer.putDouble(imu_data.getQuaternion().getQ2());
			buffer.putDouble(imu_data.getQuaternion().getQ3());
			
			buffer.putDouble(ypr[0]);
			buffer.putDouble(ypr[1]);
			buffer.putDouble(ypr[2]);
			
			buffer.flip();
			message.setPayload(bytes);
			mqttClient.publish(MQTT_TOPIC_IMU, message);
		}
	}

	private void mqttInit() throws MqttException {
		if (mqttServer == null) {
			return;
		}
		
		mqttClient = new MqttClient(mqttServer, "MPU9150", new MemoryPersistence());
		MqttConnectOptions con_opts = new MqttConnectOptions();
		con_opts.setCleanSession(true);
		mqttClient.connect(con_opts);
		Logger.debug("Connected to MQTT server '{}'", mqttServer);
	}

	private void mpuInit(MPU9150Driver mpu) throws RuntimeIOException {
		// initialise device
		Logger.debug("Initialising MPU...");
		mpu.mpu_init();
		dmp = new MPU9150DMPDriver(mpu);

		Logger.debug("Setting MPU sensors...");
		// Can be bitwise combination of INV_X_GYRO, INV_Y_GYRO, INV_Z_GYRO,
		// INV_XYZ_GYRO, INV_XYZ_ACCEL, INV_XYZ_COMPASS
		mpu.mpu_set_sensors((byte) (MPU9150Constants.INV_XYZ_GYRO | MPU9150Constants.INV_XYZ_ACCEL |
				MPU9150Constants.INV_XYZ_COMPASS));
		
		//Logger.debug("Setting LPF...");
		//mpu.mpu_set_lpf(lpf);

		Logger.debug("Setting GYRO sensitivity...");
		mpu.mpu_set_gyro_fsr(gyroFsr);

		Logger.debug("Setting ACCEL sensitivity...");
		mpu.mpu_set_accel_fsr(accelFsr);

		// verify connection
		Logger.debug("Powering up MPU...");
		boolean dev_status = mpu.mpu_get_power_state();
		Logger.debug(dev_status ? "MPU9150 connection successful" : "MPU9150 connection failed");

		// fifo config
		Logger.debug("Setting MPU fifo...");
		// Note compass data doesn't go into the FIFO, no need trying to set INV_XYZ_COMPASS
		mpu.mpu_configure_fifo((byte) (MPU9150Constants.INV_XYZ_GYRO | MPU9150Constants.INV_XYZ_ACCEL));

		// load and configure the DMP
		Logger.debug("Loading DMP firmware...");
		dmp.dmp_load_motion_driver_firmware();
		
		// Configure the orientation
		dmp.dmp_set_orientation(MPU9150DMPDriver.inv_orientation_matrix_to_scalar(
				axisRotation.getOrientationMatrix()));

		Logger.debug("Activating DMP...");
		mpu.mpu_set_dmp_state(true);

		Logger.debug("Configuring DMP...");
		dmp.dmp_register_tap_cb(MPU9150DriverMqttPublisher::tapCallback);
		dmp.dmp_register_android_orient_cb(MPU9150DriverMqttPublisher::androidOrientCallback);
		//int hal_dmp_features = MPU9150DMPConstants.DMP_FEATURE_6X_LP_QUAT |
		//		MPU9150DMPConstants.DMP_FEATURE_SEND_RAW_ACCEL|
		//		MPU9150DMPConstants.DMP_FEATURE_SEND_CAL_GYRO |
		//		MPU9150DMPConstants.DMP_FEATURE_GYRO_CAL;
		int hal_dmp_features =
				MPU9150DMPConstants.DMP_FEATURE_TAP |
				MPU9150DMPConstants.DMP_FEATURE_ANDROID_ORIENT |
				MPU9150DMPConstants.DMP_FEATURE_PEDOMETER |
				MPU9150DMPConstants.DMP_FEATURE_6X_LP_QUAT |
				MPU9150DMPConstants.DMP_FEATURE_SEND_RAW_ACCEL |
				MPU9150DMPConstants.DMP_FEATURE_SEND_CAL_GYRO |
				MPU9150DMPConstants.DMP_FEATURE_GYRO_CAL;
		dmp.dmp_enable_feature(hal_dmp_features);

		Logger.debug("Setting DMP fifo rate...");
		// MPU sample rate is ignored if DMP is enabled
		//mpu.mpu_set_sample_rate(rate);
		//mpu.mpu_set_compass_sample_rate(compassSampleRate);
		dmp.dmp_set_fifo_rate(fifoRate);

		/* When the DMP is used, the hardware sampling rate is fixed at
		 * 200Hz, and the DMP is configured to downsample the FIFO output
		 * using the function dmp_set_fifo_rate. However, when the DMP is
		 * turned off, the sampling rate remains at 200Hz. This could be
		 * handled in inv_mpu.c, but it would need to know that
		 * inv_mpu_dmp_motion_driver.c exists. To avoid this, we'll just
		 * put the extra logic in the application layer.
		 */
		//int dmp_rate = dmp.dmp_get_fifo_rate();
		//mpu.mpu_set_sample_rate(dmp_rate);
		
		
		Logger.debug("Resetting fifo queue...");
		mpu.mpu_reset_fifo();
		
		gyroBiasInit();

		Logger.debug("Sleep time={}", Integer.valueOf(fifoReadDelayMs));
		Logger.debug("Waiting for first FIFO data item... ");
		MPU9150FIFOData fifo_data = null;
		do {
			SleepUtil.sleepMillis(fifoReadDelayMs);
			fifo_data = dmp.dmp_read_fifo();
		} while (fifo_data == null);
		lastFifoRead = fifo_data.getTimestamp();
		Logger.debug("Done.");
	}

	private ImuData update(MPU9150Driver mpu) throws RuntimeIOException {
		// Wait for FIFO data to be available
		// FIXME Use a fixed period scheduler instead
		long delay = fifoReadDelayMs - (System.currentTimeMillis() - lastFifoRead);
		
		if (delay > 0) {
			Logger.debug("Sleeping for {}ms", Long.valueOf(delay));
			SleepUtil.sleepMillis(delay);
		} else {
			Logger.debug("Not sleeping, delay={}", Long.valueOf(delay));
		}
		
		//gyro and accel can be null because of being disabled in the features
		// FIXME move this logic to a scheduler within MPU-9150 driver?
		MPU9150FIFOData fifo_data;
		// dmp_read_fifo(g, a, _q, &sensors, &fifoCount)
		do {
			fifo_data = dmp.dmp_read_fifo();
		} while (fifo_data == null);
		Logger.debug("Time between FIFO reads = {}ms", Long.valueOf(System.currentTimeMillis() - lastFifoRead));
		lastFifoRead = fifo_data.getTimestamp();
		
		ImuData imu_data = MPU9150DataFactory.newInstance(fifo_data, mpu.mpu_get_compass_reg(),
				gyroFsr.getScale(), accelFsr.getScale(), AK8975Constants.COMPASS_SCALE,
				MPU9150Constants.QUATERNION_SCALE, mpu.mpu_get_temperature());
		
		// Now do standard processing
		//handleGyroBias(imu_data);
		//calibrateAverageCompass();
		//calibrateAccel();
		
		// now update the filter
		//updateFusion(imu_data);
		
		return imu_data;
	}
	
	private void updateFusion(ImuData imuData) {
		fusion.newIMUData(imuData);
	}
	
	private void gyroBiasInit() {
		gyroLearningAlpha = 2.0f / sampleRate;
		gyroContinuousAlpha = 0.01f / sampleRate;
		gyroSampleCount = 0;
	}
	
	private void handleGyroBias(ImuData imuData) {
		/*
		 * Shouldn't need to do any of this if we call dmp_set_orientation...
		// do axis rotation
		if (axisRotation != null) {
			// need to do an axis rotation
			byte[][] matrix = axisRotation.getOrientationMatrix();
			IMUData tempIMU = imuData;

			// do new x value
			if (matrix[0][0] != 0) {
				imuData.gyro.setX(tempIMU.gyro.x() * matrix[0][0]);
				imuData.accel.setX(tempIMU.accel.x() * matrix[0][0]);
				imuData.compass.setX(tempIMU.compass.x() * matrix[0][0]);
			} else if (matrix[0][1] != 0) {
				imuData.gyro.setX(tempIMU.gyro.y() * matrix[0][1]);
				imuData.accel.setX(tempIMU.accel.y() * matrix[0][1]);
				imuData.compass.setX(tempIMU.compass.y() * matrix[0][1]);
			} else if (matrix[0][2] != 0) {
				imuData.gyro.setX(tempIMU.gyro.z() * matrix[0][2]);
				imuData.accel.setX(tempIMU.accel.z() * matrix[0][2]);
				imuData.compass.setX(tempIMU.compass.z() * matrix[0][2]);
			}

			// do new y value
			if (matrix[1][0] != 0) {
				imuData.gyro.setY(tempIMU.gyro.x() * matrix[1][0]);
				imuData.accel.setY(tempIMU.accel.x() * matrix[1][0]);
				imuData.compass.setY(tempIMU.compass.x() * matrix[1][0]);
			} else if (matrix[1][1] != 0) {
				imuData.gyro.setY(tempIMU.gyro.y() * matrix[1][1]);
				imuData.accel.setY(tempIMU.accel.y() * matrix[1][1]);
				imuData.compass.setY(tempIMU.compass.y() * matrix[1][1]);
			} else if (matrix[1][2] != 0) {
				imuData.gyro.setY(tempIMU.gyro.z() * matrix[1][2]);
				imuData.accel.setY(tempIMU.accel.z() * matrix[1][2]);
				imuData.compass.setY(tempIMU.compass.z() * matrix[1][2]);
			}

			// do new z value
			if (matrix[2][0] != 0) {
				imuData.gyro.setZ(tempIMU.gyro.x() * matrix[2][0]);
				imuData.accel.setZ(tempIMU.accel.x() * matrix[2][0]);
				imuData.compass.setZ(tempIMU.compass.x() * matrix[2][0]);
			} else if (matrix[2][1] != 0) {
				imuData.gyro.setZ(tempIMU.gyro.y() * matrix[2][1]);
				imuData.accel.setZ(tempIMU.accel.y() * matrix[2][1]);
				imuData.compass.setZ(tempIMU.compass.y() * matrix[2][1]);
			} else if (matrix[2][2] != 0) {
				imuData.gyro.setZ(tempIMU.gyro.z() * matrix[2][2]);
				imuData.accel.setZ(tempIMU.accel.z() * matrix[2][2]);
				imuData.compass.setZ(tempIMU.compass.z() * matrix[2][2]);
			}
		}
		*/

		Vector3D deltaAccel = previousAccel.subtract(imuData.getAccel());   // compute difference
		previousAccel = imuData.getAccel();

		if ((deltaAccel.getNorm() < RTIMU_FUZZY_ACCEL_ZERO) && (imuData.getGyro().getNorm() < RTIMU_FUZZY_GYRO_ZERO)) {
			// What we are seeing on the gyros should be bias only so learn from this
			if (gyroSampleCount < (5 * sampleRate)) {
				gyroBias = new Vector3D((1.0 - gyroLearningAlpha) * gyroBias.getX() + gyroLearningAlpha * imuData.getGyro().getX(),
						(1.0 - gyroLearningAlpha) * gyroBias.getY() + gyroLearningAlpha * imuData.getGyro().getY(),
						(1.0 - gyroLearningAlpha) * gyroBias.getZ() + gyroLearningAlpha * imuData.getGyro().getZ());

				gyroSampleCount++;

				if (gyroSampleCount == (5 * sampleRate)) {
					// this could have been true already of course
					gyroBiasValid = true;
					//saveSettings();
				}
			} else {
				gyroBias = new Vector3D((1.0 - gyroContinuousAlpha) * gyroBias.getX() + gyroContinuousAlpha * imuData.getGyro().getX(),
						(1.0 - gyroContinuousAlpha) * gyroBias.getY() + gyroContinuousAlpha * imuData.getGyro().getY(),
						(1.0 - gyroContinuousAlpha) * gyroBias.getZ() + gyroContinuousAlpha * imuData.getGyro().getZ());
			}
		}

		imuData.setGyro(imuData.getGyro().subtract(gyroBias));
	}

	public static void tapCallback(TapEvent event) {
		Logger.debug("tapCallback({})", event);
	}

	public static void androidOrientCallback(OrientationEvent event) {
		Logger.debug("androidOrientCallback({})", event);
	}
	
	/**  Axis rotation defs
	 * These allow the IMU to be virtually repositioned if it is in a non-standard configuration
	 * Standard configuration is X pointing at north, Y pointing east and Z pointing down
	 * with the IMU horizontal. There are 24 different possible orientations as defined
	 * below. Setting the axis rotation code to non-zero values performs the repositioning.
	 *
	 * See Orientation Matrix Transformation chart.pdf
	 * XYZ  010_001_000 Identity Matrix
	 * XZY  001_010_000
	 * YXZ  010_000_001
	 * YZX  000_010_001
	 * ZXY  001_000_010
	 * ZYX  000_001_010
	 */
	public static enum AxisRotation {
		RTIMU_XNORTH_YEAST(new byte[][]	{{1, 0, 0},		{0, 1, 0},	{0, 0, 1}}), // this is the default identity matrix
		RTIMU_XEAST_YSOUTH(new byte[][]	{{0, -1, 0},	{1, 0, 0},	{0, 0, 1}}),
		RTIMU_XSOUTH_YWEST(new byte[][]	{{-1, 0, 0},	{0, -1, 0},	{0, 0, 1}}),
		RTIMU_XWEST_YNORTH(new byte[][]	{{0, 1, 0},		{-1, 0, 0},	{0, 0, 1}}),
		RTIMU_XNORTH_YWEST(new byte[][]	{{1, 0, 0},		{0, -1, 0},	{0, 0, -1}}),
		RTIMU_XEAST_YNORTH(new byte[][]	{{0, 1, 0},		{1, 0, 0},	{0, 0, -1}}),
		RTIMU_XSOUTH_YEAST(new byte[][]	{{-1, 0, 0},	{0, 1, 0},	{0, 0, -1}}),
		RTIMU_XWEST_YSOUTH(new byte[][]	{{0, -1, 0},	{-1, 0, 0}, {0, 0, -1}}),
		RTIMU_XUP_YNORTH(new byte[][]	{{0, 1, 0},		{0, 0, -1},	{-1, 0, 0}}),
		RTIMU_XUP_YEAST(new byte[][]	{{0, 0, 1},		{0, 1, 0},	{-1, 0, 0}}),
		RTIMU_XUP_YSOUTH(new byte[][]	{{0, -1, 0},	{0, 0, 1},	{-1, 0, 0}}),
		RTIMU_XUP_YWEST(new byte[][]	{{0, 0, -1},	{0, -1, 0},	{-1, 0, 0}}),
		RTIMU_XDOWN_YNORTH(new byte[][]	{{0, 1, 0},		{0, 0, 1},	{1, 0, 0}}),
		RTIMU_XDOWN_YEAST(new byte[][]	{{0, 0, -1},	{0, 1, 0},	{1, 0, 0}}),
		RTIMU_XDOWN_YSOUTH(new byte[][]	{{0, -1, 0},	{0, 0, -1},	{1, 0, 0}}),
		RTIMU_XDOWN_YWEST(new byte[][]	{{0, 0, 1},		{0, -1, 0},	{1, 0, 0}}),
		RTIMU_XNORTH_YUP(new byte[][]	{{1, 0, 0},		{0, 0, 1},	{0, -1, 0}}),
		RTIMU_XEAST_YUP(new byte[][]	{{0, 0, -1},	{1, 0, 0},	{0, -1, 0}}),
		RTIMU_XSOUTH_YUP(new byte[][]	{{-1, 0, 0},	{0, 0, -1}, {0, -1, 0}}),
		RTIMU_XWEST_YUP(new byte[][]	{{0, 0, 1},		{-1, 0, 0}, {0, -1, 0}}),
		RTIMU_XNORTH_YDOWN(new byte[][]	{{1, 0, 0},		{0, 0, -1}, {0, 1, 0}}),
		RTIMU_XEAST_YDOWN(new byte[][]	{{0, 0, 1},		{1, 0, 0},	{0, 1, 0}}),
		RTIMU_XSOUTH_YDOWN(new byte[][]	{{-1, 0, 0},	{0, 0, 1},	{0, 1, 0}}),
		RTIMU_XWEST_YDOWN(new byte[][]	{{0, 0, -1},	{-1, 0, 0},	{0, 1, 0}});
		
		private byte[][] orientationMatrix;
		
		private AxisRotation(byte[][] orientationMatrix) {
			this.orientationMatrix = orientationMatrix;
		}

		public byte[][] getOrientationMatrix() {
			return orientationMatrix;
		}
	}

	public static enum FusionAlgorithmType {
		RTFUSION_TYPE_KALMANSTATE4,	// kalman state is the quaternion pose
		RTFUSION_TYPE_RTQF;			// RT quaternion fusion
	}
}
