package com.diozero;

import java.io.IOException;

/**
 * RTK MCB Robot. Generic robot controller with pre-configured pin numbers.
 */
@SuppressWarnings("resource")
public class RyanteckDualMotor extends DualMotor {
	public RyanteckDualMotor() throws IOException {
		super(new Motor(17, 18), new Motor(22, 23));
	}
}
