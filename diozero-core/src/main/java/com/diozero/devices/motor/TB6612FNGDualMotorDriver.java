package com.diozero.devices.motor;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     TB6612FNGDualMotorDriver.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2022 diozero
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
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.PwmOutputDeviceFactoryInterface;

/**
 * Toshiba TB6612FNG Dual Motor Driver. Dual bi-directional motors, each
 * controlled by a PWM pin to control relative speed and forward / backward
 * control pins to control motor direction (both on or both off == motor off).
 *
 * Such as <a href="https://www.pololu.com/product/713">this one from
 * Pololu</a>.
 */
public class TB6612FNGDualMotorDriver extends DualMotor {
	public TB6612FNGDualMotorDriver(int motorAClockwiseControlGpio, int motorACounterClockwiseControlGpio,
			int motorAPwmGpio, int motorBClockwiseControlGpio, int motorBCounterClockwiseControlGpio, int motorBPwmGpio)
			throws RuntimeIOException {
		this(new DigitalOutputDevice(motorAClockwiseControlGpio),
				new DigitalOutputDevice(motorACounterClockwiseControlGpio), new PwmOutputDevice(motorAPwmGpio),
				new DigitalOutputDevice(motorBClockwiseControlGpio),
				new DigitalOutputDevice(motorBCounterClockwiseControlGpio), new PwmOutputDevice(motorBPwmGpio));
	}

	public TB6612FNGDualMotorDriver(PwmOutputDeviceFactoryInterface pwmDeviceFactory, int motorAClockwiseControlGpio,
			int motorACounterClockwiseControlGpio, int motorAPwmGpio, int motorBClockwiseControlGpio,
			int motorBCounterClockwiseControlGpio, int motorBPwmGpio) throws RuntimeIOException {
		this(new DigitalOutputDevice(motorAClockwiseControlGpio),
				new DigitalOutputDevice(motorACounterClockwiseControlGpio),
				new PwmOutputDevice(pwmDeviceFactory, motorAPwmGpio, 0),
				new DigitalOutputDevice(motorBClockwiseControlGpio),
				new DigitalOutputDevice(motorBCounterClockwiseControlGpio),
				new PwmOutputDevice(pwmDeviceFactory, motorBPwmGpio, 0));
	}

	public TB6612FNGDualMotorDriver(DigitalOutputDevice motorAClockwiseControlPin,
			DigitalOutputDevice motorACounterClockwiseControlPin, PwmOutputDevice motorAPwmControl,
			DigitalOutputDevice motorBClockwiseControlPin, DigitalOutputDevice motorBCounterClockwiseControlPin,
			PwmOutputDevice motorBPwmControl) {
		super(new TB6612FNGMotor(motorAClockwiseControlPin, motorACounterClockwiseControlPin, motorAPwmControl),
				new TB6612FNGMotor(motorBClockwiseControlPin, motorBCounterClockwiseControlPin, motorBPwmControl));
	}
}
