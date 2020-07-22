package com.diozero.devices.sandpit;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     TB6612FNGDualMotorDriver.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
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
import com.diozero.internal.provider.PwmOutputDeviceFactoryInterface;
import com.diozero.util.RuntimeIOException;

/**
 * Dual bi-directional motor controlled by a single PWM pin and separate forward / backward GPIO pins
 * Toshiba TB6612FNG Dual Motor Driver such as this one from Pololu: https://www.pololu.com/product/713
 */
@SuppressWarnings("resource")
public class TB6612FNGDualMotorDriver extends DualMotor {
	public TB6612FNGDualMotorDriver(int leftMotorClockwiseControlGpio, int leftMotorCounterClockwiseControlGpio,
			int leftMotorPwmGpio,
			int rightMotorClockwiseControlGpio,int rightMotorCounterClockwiseControlGpio,
			int rightMotorPwmGpio) throws RuntimeIOException {
		this(new DigitalOutputDevice(leftMotorClockwiseControlGpio),
				new DigitalOutputDevice(leftMotorCounterClockwiseControlGpio),
				new PwmOutputDevice(leftMotorPwmGpio),
				new DigitalOutputDevice(rightMotorClockwiseControlGpio),
				new DigitalOutputDevice(rightMotorCounterClockwiseControlGpio),
				new PwmOutputDevice(rightMotorPwmGpio));
	}
	
	public TB6612FNGDualMotorDriver(PwmOutputDeviceFactoryInterface pwmDeviceFactory,
			int leftMotorClockwiseControlGpio, int leftMotorCounterClockwiseControlGpio,
			int leftMotorPwmGpio,
			int rightMotorClockwiseControlGpio,int rightMotorCounterClockwiseControlGpio,
			int rightMotorPwmGpio) throws RuntimeIOException {
		this(new DigitalOutputDevice(leftMotorClockwiseControlGpio),
				new DigitalOutputDevice(leftMotorCounterClockwiseControlGpio),
				new PwmOutputDevice(pwmDeviceFactory, leftMotorPwmGpio, 0),
				new DigitalOutputDevice(rightMotorClockwiseControlGpio),
				new DigitalOutputDevice(rightMotorCounterClockwiseControlGpio),
				new PwmOutputDevice(pwmDeviceFactory, rightMotorPwmGpio, 0));
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
