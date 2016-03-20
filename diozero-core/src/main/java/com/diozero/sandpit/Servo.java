package com.diozero.sandpit;

import java.io.IOException;

import org.pmw.tinylog.Logger;

import com.diozero.api.GpioDevice;
import com.diozero.internal.DeviceFactoryHelper;
import com.diozero.internal.spi.PwmOutputDeviceFactoryInterface;
import com.diozero.internal.spi.PwmOutputDeviceInterface;
import com.diozero.util.RuntimeIOException;

public class Servo extends GpioDevice {
	private static final float MIN_PULSE_WIDTH_MS = 0.5f;
	private static final float MAX_PULSE_WIDTH_MS = 2.6f;
	
	private int pwmFrequency;
	private PwmOutputDeviceInterface device;
	
	public Servo(int pinNumber, int pwmFrequency, float initialPulseWidthMs) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), pinNumber, pwmFrequency, initialPulseWidthMs);
	}
	
	public Servo(PwmOutputDeviceFactoryInterface pwmDeviceFactory, int pinNumber,
			int pwmFrequency, float initialPulseWidthMs) throws RuntimeIOException {
		super(pinNumber);
		
		this.pwmFrequency = pwmFrequency;
		pwmDeviceFactory.setPwmFrequency(pinNumber, pwmFrequency);
		this.device = pwmDeviceFactory.provisionPwmOutputPin(pinNumber, calcValue(initialPulseWidthMs));
	}
	
	@Override
	public void close() {
		Logger.debug("close()");
		try {
			device.close();
		} catch (IOException e) {
			// Log and ignore
			Logger.warn(e, "Error closing device: {}", e);
		}
		Logger.debug("device closed");
	}
	
	public int getPwmFrequency() {
		return pwmFrequency;
	}
	
	public float getValue() {
		return device.getValue();
	}
	
	public void setValue(float value) {
		if (value < 0 || value > 1) {
			throw new IllegalArgumentException("Value must be 0..1, you requested " + value);
		}
		device.setValue(value);
	}
	
	/**
	 * Get the current servo pulse width in milliseconds
	 * @return The servo pulse width (milliseconds)
	 */
	public float getPulseWidthMs() {
		return getValue() * 1000f / pwmFrequency;
	}

	/**
	 * Set the servo pulse width in milliseconds
	 * @param pulseMs Servo pulse width (milliseconds)
	 */
	public void setPulseWidthMs(float pulseWidthMs) {
		if (pulseWidthMs < MIN_PULSE_WIDTH_MS || pulseWidthMs > MAX_PULSE_WIDTH_MS) {
			throw new IllegalArgumentException("Invalid pulse width (" + pulseWidthMs + "), must be " +
					MIN_PULSE_WIDTH_MS + ".." + MAX_PULSE_WIDTH_MS);
		}
		setValue(calcValue(pulseWidthMs));
	}

	private float calcValue(float pulseWidthMs) {
		return pulseWidthMs * pwmFrequency / 1000f;
	}
}
