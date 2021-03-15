package com.diozero.sampleapps;

import org.tinylog.Logger;

import com.diozero.api.DigitalInputDevice;
import com.diozero.api.RuntimeIOException;
import com.diozero.devices.HCSR04;
import com.diozero.devices.LED;
import com.diozero.devices.motor.DualMotor;
import com.diozero.util.SleepUtil;

public abstract class RobotTest implements AutoCloseable {
	private static final int LED_FRONT_LEFT_PIN = 18;
	private static final int LED_FRONT_RIGHT_PIN = 17;
	private static final int LED_REAR_LEFT_PIN = 22;
	private static final int LED_REAR_RIGHT_PIN = 27;

	private static final int IR_SENSOR_LEFT_PIN = 24;
	private static final int IR_SENSOR_CENTRE_PIN = 23;
	private static final int IR_SENSOR_RIGHT_PIN = 25;
	
	private static final int HCSR04_TRIGGER_PIN = 26;
	private static final int HCSR04_ECHO_PIN = 4;
	
	private LED frontLeftLed;
	private LED frontRightLed;
	private LED rearLeftLed;
	private LED rearRightLed;
	private DigitalInputDevice leftIrSensor;
	private DigitalInputDevice centreIrSensor;
	private DigitalInputDevice rightIrSensor;
	private HCSR04 hcsr04;
	private DualMotor dualMotor;

	public RobotTest(DualMotor dualMotor) {
		frontLeftLed = new LED(LED_FRONT_LEFT_PIN, true);
		frontRightLed = new LED(LED_FRONT_RIGHT_PIN, true);
		rearLeftLed = new LED(LED_REAR_LEFT_PIN, true);
		rearRightLed = new LED(LED_REAR_RIGHT_PIN, true);
	
		leftIrSensor = new DigitalInputDevice(IR_SENSOR_LEFT_PIN);
		centreIrSensor = new DigitalInputDevice(IR_SENSOR_CENTRE_PIN);
		rightIrSensor = new DigitalInputDevice(IR_SENSOR_RIGHT_PIN);
		
		hcsr04 = new HCSR04(HCSR04_TRIGGER_PIN, HCSR04_ECHO_PIN);
			
		this.dualMotor = dualMotor;
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
