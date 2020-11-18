package com.diozero.sandpit.imu.invensense;

import com.diozero.devices.imu.ImuData;

public interface FusionInterface {

	void newIMUData(ImuData imuData);

}
