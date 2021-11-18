package com.diozero.sampleapps.sandpit;

import com.diozero.api.I2CException;
import com.diozero.api.ServoDevice;
import com.diozero.api.ServoTrim;
import com.diozero.devices.GarminLidarLiteV4;
import com.diozero.devices.PiconZero;

public class RoomScan {
	// Degrees
	private static final float ANGLE_STEP = 2f;

	public static void main(String[] args) {
		ServoTrim trim = new ServoTrim(ServoTrim.DEFAULT_MID_US, ServoTrim.DEFAULT_90_DELTA_US,
				ServoTrim.DEFAULT_90_DELTA_US - 200);
		System.out.println(trim.getMinAngle() + ", " + trim.getMidAngle() + ", " + trim.getMaxAngle());

		try (PiconZero pz = new PiconZero();
				ServoDevice servo = ServoDevice.newBuilder(1).setDeviceFactory(pz).setTrim(trim).build();
				GarminLidarLiteV4 lidar = new GarminLidarLiteV4()) {
			lidar.configure(GarminLidarLiteV4.Preset.BALANCED);
			lidar.setPowerMode(GarminLidarLiteV4.PowerMode.ALWAYS_ON);
			lidar.setHighAccuracyMode(0x20);

			// Mid to max angle (degrees)
			for (float angle = trim.getMidAngle(); angle <= trim.getMaxAngle(); angle += ANGLE_STEP) {
				servo.setAngle(angle);
				int distance = lidar.getSingleReading();
				System.out.format("%.2f, %d%n", angle, distance);
				// SleepUtil.sleepMillis(5);
			}

			long start_ms = System.currentTimeMillis();
			// Max to min angle (degrees)
			for (float angle = trim.getMaxAngle(); angle >= trim.getMinAngle(); angle -= ANGLE_STEP) {
				servo.setAngle(angle);
				int distance = lidar.getSingleReading();
				System.out.format("%.2f, %d%n", angle, distance);
			}
			long sweep_duration_ms = System.currentTimeMillis() - start_ms;
			System.out.println("Sweep duration: " + sweep_duration_ms);

			for (int i = 0; i < 10; i++) {
				// Min to max angle (degrees)
				for (float angle = trim.getMinAngle(); angle <= trim.getMaxAngle(); angle += ANGLE_STEP) {
					servo.setAngle(angle);
					try {
						int distance = lidar.getSingleReading();
						System.out.format("%.2f, %d%n", angle, distance);
					} catch (I2CException e) {
						if (e.getError() != -121) {
							// Rethrow
							throw e;
						}
					}
					// SleepUtil.sleepMillis(5);
				}
				// Max to min angle (degrees)
				for (float angle = trim.getMaxAngle(); angle >= trim.getMinAngle(); angle -= ANGLE_STEP) {
					servo.setAngle(angle);
					try {
						int distance = lidar.getSingleReading();
						System.out.format("%.2f, %d%n", angle, distance);
					} catch (I2CException e) {
						if (e.getError() != -121) {
							// Rethrow
							throw e;
						}
					}
					// SleepUtil.sleepMillis(5);
				}
			}

			// Min to mid angle (degrees)
			for (float angle = trim.getMinAngle(); angle <= trim.getMidAngle(); angle += ANGLE_STEP) {
				servo.setAngle(angle);
				int distance = lidar.getSingleReading();
				System.out.format("%.2f, %d%n", angle, distance);
				// SleepUtil.sleepMillis(5);
			}
		}
	}
}
