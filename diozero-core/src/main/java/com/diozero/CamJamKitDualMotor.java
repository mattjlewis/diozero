package com.diozero;

import java.io.IOException;

/**
 * CamJam EduKit 3 Robot. Generic robot controller with pre-configured pin numbers.
 */
@SuppressWarnings("resource")
public class CamJamKitDualMotor extends DualMotor {
	public CamJamKitDualMotor() throws IOException {
		super(new Motor(9, 10), new Motor(7, 8));
	}
}
