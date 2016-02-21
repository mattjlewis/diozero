package com.diozero.sandpit;

import com.diozero.api.AnalogInputDevice;
import com.diozero.internal.spi.AnalogInputDeviceFactoryInterface;

public class Potentiometer extends AnalogInputDevice {
	private float vRef;
	private float r1;
	
	public Potentiometer(AnalogInputDeviceFactoryInterface deviceFactory, int pinNumber, float vRef, float r1) {
		super(deviceFactory, pinNumber, vRef);
		
		this.vRef = vRef;
		this.r1 = r1;
	}
	
	@Override
	public float getScaledValue() {
		return getResistance();
	}
	
	public float getResistance() {
		float v_pot = super.getScaledValue();
		float r_pot = r1 / (vRef / v_pot - 1);
		
		return r_pot;
	}
}
