package com.diozero.sandpit.imu.invensense;

import com.diozero.api.imu.ImuData;

public interface FusionInterface {

	void newIMUData(ImuData imuData);

}
