package com.diozero.sampleapps;

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

import java.io.Closeable;
import java.io.IOException;

import org.pmw.tinylog.Logger;

import com.diozero.CamJamKitDualMotor;
import com.diozero.LED;
import com.diozero.api.DigitalInputDevice;
import com.diozero.util.SleepUtil;;

public class CamJamMotorTest implements Closeable {
	private static final int PIN_REAR_LEFT_LED = 22;
	private static final int PIN_REAR_RIGHT_LED = 27;
	private static final int PIN_FRONT_LEFT_LED = 18;
	private static final int PIN_FRONT_RIGHT_LED = 17;

	private static final int PIN_LEFT_IR_SENSOR = 24;
	private static final int PIN_CENTRE_IR_SENSOR = 23;
	private static final int PIN_RIGHT_IR_SENSOR = 25;

	private static final int GO_FORWARDS = 1;
	private static final int GO_BACKWARDS = 2;
	private static final int GO_LEFT = 3;
	private static final int GO_RIGHT = 4;
	private static final int GO_LEFT_BACKWARDS = 5;
	private static final int GO_RIGHT_BACKWARDS = 6;
	private static final int GO_SHARP_LEFT = 7;
	private static final int GO_SHARP_RIGHT = 8;

	public static void main(String[] args) {
		float DELAY = 0.65f;
		float speed = 0.9f;
		
		try (CamJamMotorTest robot = new CamJamMotorTest()) {
			robot.go(GO_RIGHT, DELAY, speed);
			robot.go(GO_FORWARDS, DELAY, speed);
			robot.go(GO_LEFT, DELAY, speed);
			robot.stop(DELAY);
			robot.go(GO_LEFT_BACKWARDS, DELAY, speed);
			robot.go(GO_BACKWARDS, DELAY, speed);
			robot.go(GO_RIGHT_BACKWARDS, DELAY, speed);
			robot.stop(DELAY);
			robot.go(GO_SHARP_LEFT, DELAY, speed);
			robot.go(GO_SHARP_RIGHT, DELAY, speed);
			robot.stop(DELAY);
			robot.go(GO_FORWARDS, DELAY, speed);
			robot.go(GO_BACKWARDS, DELAY, speed);
			robot.stop(DELAY);
		} catch (IOException e) {
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
	private CamJamKitDualMotor dualMotor;

	public CamJamMotorTest() {
		frontLeftLed = new LED(PIN_FRONT_LEFT_LED, true);
		frontRightLed = new LED(PIN_FRONT_RIGHT_LED, true);
		rearLeftLed = new LED(PIN_REAR_LEFT_LED, true);
		rearRightLed = new LED(PIN_REAR_RIGHT_LED, true);
	
		leftIrSensor = new DigitalInputDevice(PIN_LEFT_IR_SENSOR);
		centreIrSensor = new DigitalInputDevice(PIN_CENTRE_IR_SENSOR);
		rightIrSensor = new DigitalInputDevice(PIN_RIGHT_IR_SENSOR);
		
		leftIrSensor.whenActivated(() -> System.out.println("Left activated"));
		leftIrSensor.whenDeactivated(() -> System.out.println("Left deactivated"));
		centreIrSensor.whenActivated(() -> System.out.println("Centre activated"));
		centreIrSensor.whenDeactivated(() -> System.out.println("Centre deactivated"));
		rightIrSensor.whenActivated(() -> System.out.println("Right activated"));
		rightIrSensor.whenDeactivated(() -> System.out.println("Right deactivated"));
			
		dualMotor = new CamJamKitDualMotor();
		
		frontLeftLed.blink(0.25f, 0.25f, 1, false);
		frontRightLed.blink(0.25f, 0.25f, 1, false);
		rearRightLed.blink(0.25f, 0.25f, 1, false);
		rearLeftLed.blink(0.25f, 0.25f, 1, false);
		SleepUtil.sleepSeconds(1);
	}

	public void go(int direction, float duration, float speed) {
		if (direction == GO_FORWARDS) {
			frontLeftLed.on();
			frontRightLed.on();
			rearLeftLed.off();
			rearRightLed.off();
			dualMotor.forward(speed);
		} else if (direction == GO_BACKWARDS) {
			frontLeftLed.off();
	    	frontRightLed.off();
	    	rearLeftLed.on();
	    	rearRightLed.on();
	    	dualMotor.backward(speed);
		} else if (direction == GO_SHARP_LEFT) {
			frontLeftLed.off();
			frontRightLed.on();
			rearLeftLed.off();
			rearRightLed.on();
			dualMotor.rotateLeft(speed);
		} else if (direction == GO_SHARP_RIGHT) {
			frontLeftLed.on();
			frontRightLed.off();
			rearLeftLed.on();
			rearRightLed.off();
			dualMotor.rotateRight(speed);
		} else if (direction == GO_LEFT) {
			frontLeftLed.off();
			frontRightLed.on();
			rearLeftLed.off();
			rearRightLed.off();
			dualMotor.forwardLeft(speed);
		} else if (direction == GO_RIGHT) {
			frontLeftLed.on();
			frontRightLed.off();
			rearLeftLed.off();
			rearRightLed.off();
			dualMotor.forwardRight(speed);
		} else if (direction == GO_LEFT_BACKWARDS) {
			frontLeftLed.off();
			frontRightLed.off();
			rearLeftLed.off();
			rearRightLed.on();
			dualMotor.backwardLeft(speed);
		} else if (direction == GO_RIGHT_BACKWARDS) {
			frontLeftLed.off();
			frontRightLed.off();
			rearLeftLed.on();
			rearRightLed.off();
			dualMotor.backwardRight(speed);
		}
		
		if (duration > 0) {
			SleepUtil.sleepSeconds(duration);
		}
	}

	public void stop(float duration) {
		frontLeftLed.off();
		frontRightLed.off();
		rearLeftLed.off();
		rearRightLed.off();
		dualMotor.stop();
		if (duration > 0) {
			SleepUtil.sleepSeconds(duration);
		}
	}

	@Override
	public void close() throws IOException {
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
