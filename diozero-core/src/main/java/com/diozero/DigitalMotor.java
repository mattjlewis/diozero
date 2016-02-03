package com.diozero;

import java.io.Closeable;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.api.DigitalOutputDevice;

public class DigitalMotor implements Closeable {
	private static final Logger logger = LogManager.getLogger(DigitalMotor.class);
	
	private DigitalOutputDevice forward;
	private DigitalOutputDevice backward;
	
	public DigitalMotor(int forwardPin, int backwardPin) throws IOException {
		forward = new DigitalOutputDevice(forwardPin);
		backward = new DigitalOutputDevice(forwardPin);
	}

	@Override
	public void close() {
		logger.debug("close()");
		forward.close();
		backward.close();
	}
	
	// Exposed operations
	public void forward() throws IOException {
		forward.on();
		backward.off();
	}
	
	public void backward() throws IOException {
		forward.off();
		backward.on();
	}
	
	public void stop() throws IOException {
		forward.off();
		backward.off();
	}
	
	public void reverse() throws IOException {
		if (!isActive()) {
			return;
		}
		forward.toggle();
		backward.toggle();
	}
	
	public boolean isActive() throws IOException {
		return forward.isOn() || backward.isOn();
	}
	
	/**
	 * Represents the speed of the motor as a floating point value between -1
	 *   (full speed backward) and 1 (full speed forward)
	 * @throws IOException 
	 */
	public float getValue() throws IOException {
		if (forward.isOn()) {
			return 1;
		} else if (backward.isOn()) {
			return -1;
		}
		
		return 0;
	}
}
