package com.diozero.sandpit;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Core
 * Filename:     TB6612FNGMotor.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2017 mattjlewis
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

import org.pmw.tinylog.Logger;

import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.PwmOutputDevice;
import com.diozero.api.motor.MotorBase;
import com.diozero.util.RuntimeIOException;

/**
 * Bi-directional motor controlled by a single PWM pin and separate forward / backward GPIO pins
 * Toshiba TB6612FNG Dual Motor Driver such as @see <a href="https://www.pololu.com/product/713">this one from Pololu</a>
 * Turn forward, set pin 1 to HIGH, pin 2 to LOW, and PWM to &gt;0
 * Turn backward, set pin 1 to LOW, pin 2 to HIGH, PWM to &gt;0
 */
public class TB6612FNGMotor extends MotorBase {
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
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	@Override
	public void forward(float speed) throws RuntimeIOException {
		motorBackwardControlPin.off();
		motorForwardControlPin.on();
		motorPwmControl.setValue(speed);
		valueChanged(speed);
	}
	
	/**
	 * @param speed
	 *            Range 0..1
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	@Override
	public void backward(float speed) throws RuntimeIOException {
		motorForwardControlPin.off();
		motorBackwardControlPin.on();
		motorPwmControl.setValue(speed);
		valueChanged(-speed);
	}
	
	@Override
	public void stop() throws RuntimeIOException {
		motorForwardControlPin.off();
		motorBackwardControlPin.off();
		motorPwmControl.setValue(0);
		valueChanged(0);
	}
	
	/**
	 * Represents the speed of the motor as a floating point value between -1
	 * (full speed backward) and 1 (full speed forward)
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	@Override
	public float getValue() throws RuntimeIOException {
		float speed = motorPwmControl.getValue();
		
		return motorForwardControlPin.isOn() ? speed : -speed;
	}

	@Override
	public boolean isActive() throws RuntimeIOException {
		return motorPwmControl.isOn() && (motorForwardControlPin.isOn() || motorBackwardControlPin.isOn());
	}
}
