package com.diozero.sandpit;

import java.io.IOException;

import com.diozero.DualMotor;
import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.PwmOutputDevice;
import com.diozero.internal.spi.PwmOutputDeviceFactoryInterface;

/**
 * Dual bi-directional motor controlled by a single PWM pin and separate forward / backward GPIO pins
 * Toshiba TB6612FNG Dual Motor Driver such as this one from Pololu: https://www.pololu.com/product/713
 */
@SuppressWarnings("resource")
public class TB6612FNGDualMotorDriver extends DualMotor {
	public TB6612FNGDualMotorDriver(int leftMotorClockwiseControlPinNumber, int leftMotorCounterClockwiseControlPinNumber,
			int leftMotorPwmPinNumber,
			int rightMotorClockwiseControlPinNumber,int rightMotorCounterClockwiseControlPinNumber,
			int rightMotorPwmPinNumber) throws IOException {
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
			int rightMotorPwmPinNumber) throws IOException {
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
