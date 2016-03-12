package com.diozero.api.imu;

import java.util.concurrent.TimeUnit;

import com.diozero.util.DioZeroScheduler;

public abstract class ImuBase implements ImuInterface {

	@Override
	public void startRead() {
		DioZeroScheduler.getDaemonInstance().scheduleAtFixedRate(this::processData, 20, 20, TimeUnit.MILLISECONDS);
	}
	
	@Override
	public void stopRead() {
		
	}
	
	protected void processData() {
		ImuData imu_data = getImuData();
		// TODO Now what?
	}
}
