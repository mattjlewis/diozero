package com.diozero.sandpit.imu.invensense;

import org.apache.commons.math3.filter.KalmanFilter;

import com.diozero.imu.IMUData;

public class RTFusionKalman4 implements FusionInterface {
	private KalmanFilter filter;
	
	public RTFusionKalman4() {
		//ProcessModel pm = new DefaultProcessModel(A, B, Q, x, P0);
		//MeasurementModel mm = new DefaultMeasurementModel(H, R);
		//filter = new KalmanFilter(pm, mm);
	}

	@Override
	public void newIMUData(IMUData imuData) {
		// TODO Auto-generated method stub
		
	}
}
