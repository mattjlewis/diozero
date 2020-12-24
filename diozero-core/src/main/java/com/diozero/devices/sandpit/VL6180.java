package com.diozero.devices.sandpit;

import com.diozero.api.I2CDevice;
import com.diozero.api.RuntimeIOException;
import com.diozero.devices.DistanceSensorInterface;

/**
 * Datasheet: https://www.st.com/resource/en/datasheet/vl6180.pdf
 */
public class VL6180 implements DistanceSensorInterface {
	private I2CDevice device;

	@Override
	public void close() {
		device.close();
	}

	@Override
	public float getDistanceCm() throws RuntimeIOException {
		// TODO Auto-generated method stub
		return 0;
	}
}
