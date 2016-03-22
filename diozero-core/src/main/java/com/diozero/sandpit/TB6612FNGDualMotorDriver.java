package com.diozero.sandpit;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 mattjlewis
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

import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.PwmOutputDevice;
import com.diozero.api.motor.DualMotor;
import com.diozero.internal.spi.PwmOutputDeviceFactoryInterface;
import com.diozero.util.RuntimeIOException;

/**
 * Dual bi-directional motor controlled by a single PWM pin and separate forward / backward GPIO pins
 * Toshiba TB6612FNG Dual Motor Driver such as this one from Pololu: https://www.pololu.com/product/713
 */
@SuppressWarnings("resource")
public class TB6612FNGDualMotorDriver extends DualMotor {
	public TB6612FNGDualMotorDriver(int leftMotorClockwiseControlPinNumber, int leftMotorCounterClockwiseControlPinNumber,
			int leftMotorPwmPinNumber,
			int rightMotorClockwiseControlPinNumber,int rightMotorCounterClockwiseControlPinNumber,
			int rightMotorPwmPinNumber) throws RuntimeIOException {
		this(new DigitalOutputDevice(leftMotorClockwiseControlPinNumber),
				new DigitalOutputDevice(leftMotorCounterClockwiseControlPinNumber),
				new PwmOutputDevice(leftMotorPwmPinNumber),
				new DigitalOutputDevice(rightMotorClockwiseControlPinNumber),
				new DigitalOutputDevice(rightMotorCounterClockwiseControlPinNumber),
				new PwmOutputDevice(rightMotorPwmPinNumber));
	}
	
	public TB6612FNGDualMotorDriver(PwmOutputDeviceFactoryInterface pwmDeviceFactory,
			int leftMotorClockwiseControlPinNumber, int leftMotorCounterClockwiseControlPinNumber,
			int leftMotorPwmPinNumber,
			int rightMotorClockwiseControlPinNumber,int rightMotorCounterClockwiseControlPinNumber,
			int rightMotorPwmPinNumber) throws RuntimeIOException {
		this(new DigitalOutputDevice(leftMotorClockwiseControlPinNumber),
				new DigitalOutputDevice(leftMotorCounterClockwiseControlPinNumber),
				new PwmOutputDevice(pwmDeviceFactory, leftMotorPwmPinNumber, 0),
				new DigitalOutputDevice(rightMotorClockwiseControlPinNumber),
				new DigitalOutputDevice(rightMotorCounterClockwiseControlPinNumber),
				new PwmOutputDevice(pwmDeviceFactory, rightMotorPwmPinNumber, 0));
	}
	
	public TB6612FNGDualMotorDriver(
			DigitalOutputDevice leftMotorClockwiseControlPin, DigitalOutputDevice leftMotorCounterClockwiseControlPin,
			PwmOutputDevice leftMotorPwmControl,
			DigitalOutputDevice rightMotorClockwiseControlPin, DigitalOutputDevice rightMotorCounterClockwiseControlPin,
			PwmOutputDevice rightMotorPwmControl) {
		super(
			new TB6612FNGMotor(leftMotorClockwiseControlPin,
				leftMotorCounterClockwiseControlPin, leftMotorPwmControl),
			new TB6612FNGMotor(rightMotorClockwiseControlPin,
				rightMotorCounterClockwiseControlPin, rightMotorPwmControl));
	}
}
