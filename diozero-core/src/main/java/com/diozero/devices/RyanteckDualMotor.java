package com.diozero.devices;

import com.diozero.devices.motor.DualMotor;
import com.diozero.devices.motor.PwmMotor;
import com.diozero.util.RuntimeIOException;

/**
 * RTK MCB Robot. Generic robot controller with pre-configured GPIO connections.
 */
@SuppressWarnings("resource")
public class RyanteckDualMotor extends DualMotor {
	public RyanteckDualMotor() throws RuntimeIOException {
		super(new PwmMotor(17, 18), new PwmMotor(22, 23));
	}
}
