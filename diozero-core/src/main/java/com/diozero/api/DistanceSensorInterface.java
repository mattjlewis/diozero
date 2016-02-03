package com.diozero.api;

import java.io.IOException;

public interface DistanceSensorInterface {
	/**
	 * @return distance in cm
	 */
	public double getDistanceCm() throws IOException;
}
