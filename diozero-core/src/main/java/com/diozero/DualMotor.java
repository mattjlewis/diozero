package com.diozero;

import java.io.Closeable;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.api.MotorInterface;

/**
 * Generic dual bi-directional motor driver
 */
public class DualMotor implements Closeable {
	private static final Logger logger = LogManager.getLogger(DualMotor.class);
	
	private MotorInterface leftMotor;
	private MotorInterface rightMotor;
	
	public DualMotor(MotorInterface leftMotor, MotorInterface rightMotor) {
		this.leftMotor = leftMotor;
		this.rightMotor = rightMotor;
	}

	@Override
	public void close() throws IOException {
		logger.debug("close()");
		if (leftMotor != null) { leftMotor.close(); }
		if (rightMotor != null) { rightMotor.close(); }
	}
	
	public float[] getValues() throws IOException {
		return new float[] { leftMotor.getValue(), rightMotor.getValue() };
	}
	
	/**
	 * Set the speed and direction for both motors (clockwise / counter-clockwise)
	 * @param speed Range -1 .. 1. Positive numbers for clockwise, Negative numbers for counter clockwise
	 * @throws IOException
	 */
	public void setValues(float leftValue, float rightValue) throws IOException {
		leftMotor.setValue(leftValue);
		rightMotor.setValue(rightValue);
	}
	
	public void forward(float speed) throws IOException {
		leftMotor.forward(speed);
		rightMotor.forward(speed);
	}
	
	public void backward(float speed) throws IOException {
		leftMotor.backward(speed);
		rightMotor.backward(speed);
	}
	
	public void rotateLeft(float speed) throws IOException {
		rightMotor.forward(speed);
		leftMotor.backward(speed);
	}
	
	public void rotateRight(float speed) throws IOException {
		leftMotor.forward(speed);
		rightMotor.backward(speed);
	}
	
	public void reverse() throws IOException {
		leftMotor.reverse();
		rightMotor.reverse();
	}
	
	public void stop() throws IOException {
		leftMotor.stop();
		rightMotor.stop();
	}
}
