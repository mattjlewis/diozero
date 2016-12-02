package com.diozero.sandpit;

import java.io.IOException;

import com.diozero.api.motor.MotorBase;
import com.diozero.util.RuntimeIOException;

public class PiconZeroMotor extends MotorBase {
	private static final int MAX_FORWARD_SPEED = 127;
	private static final int MAX_BACKWARD_SPEED = -128;
	private PiconZero piconZero;
	private int motor;
	
	public PiconZeroMotor(PiconZero piconZero, int motor) {
		this.piconZero = piconZero;
		this.motor = motor;
	}

	@Override
	public void forward(float speed) throws RuntimeIOException {
		piconZero.setMotor(motor, (int) (Math.abs(speed) * MAX_FORWARD_SPEED));
	}

	@Override
	public void backward(float speed) throws RuntimeIOException {
		piconZero.setMotor(motor, (int) (Math.abs(speed) * MAX_BACKWARD_SPEED));
	}

	@Override
	public void stop() throws RuntimeIOException {
		piconZero.setMotor(motor, 0);
	}

	/**
	 * Get the relative output value for the motor
	 * @return -1 for full reverse, 1 for full forward, 0 for stop
	 */
	@Override
	public float getValue() throws RuntimeIOException {
		return piconZero.getMotor(motor) / (float) Math.abs(MAX_BACKWARD_SPEED);
	}

	@Override
	public boolean isActive() throws RuntimeIOException {
		return piconZero.getMotor(motor) != 0;
	}

	@Override
	public void close() throws IOException {
		stop();
	}
}
