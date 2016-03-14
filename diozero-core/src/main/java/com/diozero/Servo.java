package com.diozero;

import com.diozero.api.PwmOutputDevice;
import com.diozero.internal.DeviceFactoryHelper;
import com.diozero.internal.spi.PwmOutputDeviceFactoryInterface;
import com.diozero.util.RuntimeIOException;
import com.diozero.util.ServoUtil;

public class Servo extends PwmOutputDevice {
	private float range;
	private double pulseMsPerBit;
	
	public Servo(int pwmFrequency, int range, int pinNumber, float initialValue) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), pwmFrequency, range, pinNumber, initialValue);
	}
	
	public Servo(PwmOutputDeviceFactoryInterface pwmDeviceFactory, int pwmFrequency, int range,
			int pinNumber, float initialValue) throws RuntimeIOException {
		super(pwmDeviceFactory, pinNumber, initialValue);
		
		this.range = range;
		
		pulseMsPerBit = ServoUtil.calcPulseMsPerBit(pwmFrequency, range);
	}

	/**
	 * Set the servo pulse value in milliseconds
	 * @param pulseMs Servo pulse value
	 */
	public void setPulseMs(double pulseMs) {
		int pulse = ServoUtil.calcServoPulse(pulseMs, pulseMsPerBit);
		setValue(pulse / range);
	}
}
