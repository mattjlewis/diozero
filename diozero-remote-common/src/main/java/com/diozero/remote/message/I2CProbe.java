package com.diozero.remote.message;

import com.diozero.api.I2CDevice;

public class I2CProbe extends I2CBase {
	private static final long serialVersionUID = 1448406173117611971L;

	private I2CDevice.ProbeMode probeMode;
	
	public I2CProbe(int controller, int address, I2CDevice.ProbeMode probeMode, String correlationId) {
		super(controller, address, correlationId);
		
		this.probeMode = probeMode;
	}

	public I2CDevice.ProbeMode getProbeMode() {
		return probeMode;
	}
}
