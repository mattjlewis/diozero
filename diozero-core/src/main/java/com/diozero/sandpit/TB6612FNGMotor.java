package com.diozero.sandpit;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 diozero
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */


import java.io.IOException;

import org.pmw.tinylog.Logger;

import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.MotorInterface;
import com.diozero.api.PwmOutputDevice;

/**
 * Bi-directional motor controlled by a single PWM pin and separate forward / backward GPIO pins
 * Toshiba TB6612FNG Dual Motor Driver such as this one from Pololu: https://www.pololu.com/product/713
 * Turn forward, set pin 1 to HIGH, pin 2 to LOW, and PWM to >0
 * Turn backward, set pin 1 to LOW, pin 2 to HIGH, PWM to >0
 */
public class TB6612FNGMotor implements MotorInterface {
	private DigitalOutputDevice motorForwardControlPin;
	private DigitalOutputDevice motorBackwardControlPin;
	private PwmOutputDevice motorPwmControl;
	
	public TB6612FNGMotor(
			DigitalOutputDevice motorForwardControlPin, DigitalOutputDevice motorBackwardControlPin,
			PwmOutputDevice motorPwmControl) {
		this.motorForwardControlPin = motorForwardControlPin;
		this.motorBackwardControlPin = motorBackwardControlPin; 
		this.motorPwmControl = motorPwmControl; 
	}

	@Override
	public void close() {
		Logger.debug("close()");
		if (motorForwardControlPin != null) { try { motorForwardControlPin.close(); } catch (Exception e) { } }
		if (motorBackwardControlPin != null) { try { motorBackwardControlPin.close(); } catch (Exception e) { } }
		if (motorPwmControl != null) { try { motorPwmControl.close(); } catch (Exception e) { } }
	}

	/**
	 * @param speed
	 *            Range 0..1
	 * @throws IOException
	 */
	@Override
	public void forward(float speed) throws IOException {
		motorBackwardControlPin.off();
		motorForwardControlPin.on();
		motorPwmControl.setValue(speed);
	}
	
	/**
	 * @param speed
	 *            Range 0..1
	 * @throws IOException
	 */
	@Override
	public void backward(float speed) throws IOException {
		motorForwardControlPin.off();
		motorBackwardControlPin.on();
		motorPwmControl.setValue(speed);
	}
	
	@Override
	public void stop() throws IOException {
		motorForwardControlPin.off();
		motorBackwardControlPin.off();
		motorPwmControl.setValue(0);
	}

	/**
	 * Reverse direction of the motors
	 * @throws IOException
	 */
	@Override
	public void reverse() throws IOException {
		setValue(-getValue());
	}
	
	/**
	 * Represents the speed of the motor as a floating point value between -1
	 * (full speed backward) and 1 (full speed forward)
	 */
	@Override
	public float getValue() throws IOException {
		float speed = motorPwmControl.getValue();
		
		return motorForwardControlPin.isOn() ? speed : -speed;
	}

	/**
	 * Set the speed of the motor as a floating point value between -1 (full
	 * speed backward) and 1 (full speed forward)
	 * @param value Range -1 .. 1. Positive numbers for forward, Negative numbers for backward
	 * @throws IOException
	 */
	@Override
	public void setValue(float value) throws IOException {
		if (value < -1 || value > 1) {
			throw new IllegalArgumentException("Motor value must be between -1 and 1");
		}
		if (value > 0) {
			forward(value);
		} else if (value < 0) {
			backward(-value);
		} else {
			stop();
		}
	}

	@Override
	public boolean isActive() throws IOException {
		return motorPwmControl.isOn() && (motorForwardControlPin.isOn() || motorBackwardControlPin.isOn());
	}
}
