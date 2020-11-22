package com.diozero.devices;

import com.diozero.devices.motor.DualMotor;
import com.diozero.devices.motor.PwmMotor;
import com.diozero.util.RuntimeIOException;

/**
 * CamJam EduKit 3 Robot. Generic robot controller with pre-configured GPIO connections.
 */
public class CamJamKitDualMotor extends DualMotor {
	public CamJamKitDualMotor() throws RuntimeIOException {
		super(new PwmMotor(9, 10), new PwmMotor(7, 8));
	}
}
