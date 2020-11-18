package com.diozero.sampleapps;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Sample applications
 * Filename:     CamJamLineFollower.java  
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

import java.io.Closeable;

import org.tinylog.Logger;

import com.diozero.api.DigitalInputDevice;
import com.diozero.devices.CamJamKitDualMotor;
import com.diozero.devices.HCSR04;
import com.diozero.devices.LED;
import com.diozero.devices.motor.DualMotor;
import com.diozero.util.RuntimeIOException;
import com.diozero.util.SleepUtil;

/**
 * Line follower test application with CamJam EduKit motor controller, 3 IR sensors, an HC-SR04 distance sensor and 4 LEDs. To run:
 * <ul>
 * <li>sysfs:<br>
 *  {@code java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar com.diozero.sampleapps.CamJamLineFollower}</li>
 * <li>Pi4j:<br>
 *  {@code sudo java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-pi4j-$DIOZERO_VERSION.jar:pi4j-core-1.2.jar com.diozero.sampleapps.CamJamLineFollower}</li>
 * <li>wiringPi:<br>
 *  {@code sudo java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-wiringpi-$DIOZERO_VERSION.jar:pi4j-core-1.2.jar com.diozero.sampleapps.CamJamLineFollower}</li>
 * <li>pigpgioJ:<br>
 *  {@code sudo java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-pigpio-$DIOZERO_VERSION.jar:pigpioj-java-2.4.jar com.diozero.sampleapps.CamJamLineFollower}</li>
 * </ul>
 */
public class CamJamLineFollower implements Closeable {
	private static final int LED_FRONT_LEFT_PIN = 18;
	private static final int LED_FRONT_RIGHT_PIN = 17;
	private static final int LED_REAR_LEFT_PIN = 22;
	private static final int LED_REAR_RIGHT_PIN = 27;

	private static final int IR_SENSOR_LEFT_PIN = 24;
	private static final int IR_SENSOR_CENTRE_PIN = 23;
	private static final int IR_SENSOR_RIGHT_PIN = 25;
	
	private static final int HCSR04_TRIGGER_PIN = 26;
	private static final int HCSR04_ECHO_PIN = 4;

	public static void main(String[] args) {
		float speed = 0.9f;
		float DELAY = 0.65f;
		
		try (CamJamLineFollower robot = new CamJamLineFollower()) {
			robot.testMovements(speed, DELAY);
			
			robot.go();
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
		}
	}
	
	private LED frontLeftLed;
	private LED frontRightLed;
	private LED rearLeftLed;
	private LED rearRightLed;
	private DigitalInputDevice leftIrSensor;
	private DigitalInputDevice centreIrSensor;
	private DigitalInputDevice rightIrSensor;
	private HCSR04 hcsr04;
	private DualMotor dualMotor;

	public CamJamLineFollower() {
		frontLeftLed = new LED(LED_FRONT_LEFT_PIN, true);
		frontRightLed = new LED(LED_FRONT_RIGHT_PIN, true);
		rearLeftLed = new LED(LED_REAR_LEFT_PIN, true);
		rearRightLed = new LED(LED_REAR_RIGHT_PIN, true);
	
		leftIrSensor = new DigitalInputDevice(IR_SENSOR_LEFT_PIN);
		centreIrSensor = new DigitalInputDevice(IR_SENSOR_CENTRE_PIN);
		rightIrSensor = new DigitalInputDevice(IR_SENSOR_RIGHT_PIN);
		
		hcsr04 = new HCSR04(HCSR04_TRIGGER_PIN, HCSR04_ECHO_PIN);
			
		dualMotor = new CamJamKitDualMotor();
		dualMotor.getLeftMotor().addListener((event) -> {
			float value = event.getValue();
			if (value > 0) {
				frontLeftLed.on(); rearLeftLed.off();
			} else if (value < 0) {
				frontLeftLed.off(); rearLeftLed.on();
			} else {
				frontLeftLed.off(); rearLeftLed.off();
			}
		});
		dualMotor.getRightMotor().addListener((event) -> {
			float value = event.getValue();
			if (value > 0) {
				frontRightLed.on(); rearRightLed.off();
			} else if (value < 0) {
				frontRightLed.off(); rearRightLed.on();
			} else {
				frontRightLed.off(); rearRightLed.off();
			}
		});
		
		frontLeftLed.blink(0.25f, 0.25f, 1, false);
		frontRightLed.blink(0.25f, 0.25f, 1, false);
		rearRightLed.blink(0.25f, 0.25f, 1, false);
		rearLeftLed.blink(0.25f, 0.25f, 1, false);
		
		Logger.info("Ready");
	}
	
	public void testMovements(float speed, float delay) {
		dualMotor.forwardRight(speed);
		SleepUtil.sleepSeconds(delay);
		dualMotor.forward(speed);
		SleepUtil.sleepSeconds(delay);
		dualMotor.forwardLeft(speed);
		SleepUtil.sleepSeconds(delay);
		dualMotor.stop();
		SleepUtil.sleepSeconds(delay);
		dualMotor.backwardLeft(speed);
		SleepUtil.sleepSeconds(delay);
		dualMotor.backward(speed);
		SleepUtil.sleepSeconds(delay);
		dualMotor.backwardRight(speed);
		SleepUtil.sleepSeconds(delay);
		dualMotor.stop();
		SleepUtil.sleepSeconds(delay);
		dualMotor.rotateLeft(speed);
		SleepUtil.sleepSeconds(delay);
		dualMotor.rotateRight(speed);
		SleepUtil.sleepSeconds(delay);
		dualMotor.stop();
		SleepUtil.sleepSeconds(delay);
		dualMotor.forward(speed);
		SleepUtil.sleepSeconds(delay);
		dualMotor.backward(speed);
		SleepUtil.sleepSeconds(delay);
		dualMotor.stop();
		SleepUtil.sleepSeconds(delay);
	}
	
	public void go() {
		float speed = 0.8f;
		float turn_rate = 0.4f;
		int delay_ms = 20;
		
		while (true) {
			SleepUtil.sleepMillis(delay_ms);
			
			float distance;
			int count = 0;
			do {
				distance = hcsr04.getDistanceCm();
			} while (distance == -1 && ++count < 10);
			
			if (distance != -1) {
				if (distance < 40) {
					Logger.info("Object detected");
				} else if (distance < 5) {
					Logger.info("Object detected - stopping");
					dualMotor.stop();
					continue;
				}
			}
			
			// Is the centre IR sensor detecting the line?
			if (centreIrSensor.isActive()) {
				if (leftIrSensor.isActive() && !rightIrSensor.isActive()) {
					Logger.info("Drifting left");
					dualMotor.circleLeft(speed, 0.2f);
				} else if (rightIrSensor.isActive() && !leftIrSensor.isActive()) {
					Logger.info("Drifting right");
					dualMotor.circleRight(speed, 0.2f);
				} else {
					Logger.info("Straight-on");
					dualMotor.forward(speed);
				}
			} else {
				Logger.info("Centre IR sensor has lost the line");
				if (leftIrSensor.isActive()) {
					Logger.info("Detected line to the left, turning left");
					dualMotor.circleLeft(speed, turn_rate);
				} else if (rightIrSensor.isActive()) {
					Logger.info("Detected line to the right, turning right");
					dualMotor.circleRight(speed, turn_rate);
				} else {
					Logger.info("Lost the line...");
					// TODO Is there anything that can be done? Retrace steps to find the line again?
					dualMotor.stop();
					break;
				}
			}
		}
	}

	@Override
	public void close() throws RuntimeIOException {
		frontLeftLed.close();
		frontRightLed.close();
		rearLeftLed.close();
		rearRightLed.close();
		leftIrSensor.close();
		centreIrSensor.close();
		rightIrSensor.close();
		dualMotor.close();
	}
}
