package com.diozero.sandpit;

import com.diozero.api.AnalogueInputDevice;
import com.diozero.internal.spi.AnalogueInputDeviceFactoryInterface;

public class Potentiometer extends AnalogueInputDevice {
	private float vRef;
	private float r1;
	
	public Potentiometer(AnalogueInputDeviceFactoryInterface deviceFactory, int pinNumber, float vRef, float r1) {
		super(deviceFactory, pinNumber, vRef);
		
		this.vRef = vRef;
		this.r1 = r1;
	}
	
	public double getResistance() {
		double v_pot = getUnscaledValue();
		double r_pot = r1 / (vRef / v_pot - 1);
		
		return r_pot;
	}
}
