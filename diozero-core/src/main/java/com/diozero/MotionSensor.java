package com.diozero;

import java.io.IOException;

import com.diozero.api.GpioPullUpDown;
import com.diozero.api.SmoothedInputDevice;

/**
 * A PIR (Passive Infra-Red) motion sensor.
 * 
 * A typical PIR device has a small circuit board with three pins: VCC, OUT, and
 * GND. VCC should be connected to the Pi's +5V pin, GND to one of the Pi's
 * ground pins, and finally OUT to the GPIO specified as the value of the 'pin'
 * parameter in the constructor.
 * 
 * This class defaults 'queue_len' to 1, effectively removing the averaging of
 * the internal queue. If your PIR sensor has a short fall time and is
 * particularly "jittery" you may wish to set this to a higher value (e.g. 5) to
 * mitigate this.
 */
public class MotionSensor extends SmoothedInputDevice {
	public MotionSensor(int pinNumber) throws IOException {
		this(pinNumber, 0.5f, 1, 10, false);
	}
	
	public MotionSensor(int pinNumber, float threshold, int queueLen, int sampleRate, boolean partial)
			throws IOException {
		super(pinNumber, GpioPullUpDown.NONE, threshold, queueLen, 1f / sampleRate, partial);
	}

	// TODO Implementation
}
