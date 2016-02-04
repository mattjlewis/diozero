package com.diozero.sandpit.imu.invensense;

import com.diozero.imu.IMUData;

public interface FusionInterface {

	void newIMUData(IMUData imuData);

}
