package com.diozero.sampleapps;

import org.pmw.tinylog.Logger;

import com.diozero.PCA9685;
import com.diozero.Servo;
import com.diozero.util.ServoUtil;
import com.diozero.util.SleepUtil;

/**
 * 
 * To run:
 * JDK Device I/O 1.0:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.4-SNAPSHOT.jar:diozero-provider-jdkdio10-0.4-SNAPSHOT.jar:dio-1.0.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.PCA9685ServoTest 60 15
 * JDK Device I/O 1.1:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.4-SNAPSHOT.jar:diozero-provider-jdkdio11-0.4-SNAPSHOT.jar:dio-1.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.PCA9685ServoTest 60 15
 * Pi4j:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.4-SNAPSHOT.jar:diozero-provider-pi4j-0.4-SNAPSHOT.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.PCA9685ServoTest 60 15
 * wiringPi:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.4-SNAPSHOT.jar:diozero-provider-wiringpi-0.4-SNAPSHOT.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.PCA9685ServoTest 60 15
 * pigpgioJ:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.4-SNAPSHOT.jar:diozero-provider-pigpio-0.4-SNAPSHOT.jar:pigpioj-java-1.0.0.jar com.diozero.sampleapps.PCA9685ServoTest 60 15
 */
public class PCA9685ServoTest {
	private static final double TOWERPRO_SG90_MIN_MS = 0.7;
	private static final double TOWERPRO_SG90_MAX_MS = 2.6;
	private static final double TOWERPRO_SG90_MID_MS = (TOWERPRO_SG90_MIN_MS + TOWERPRO_SG90_MAX_MS) / 2;
	
	public static void main(String[] args) {
		int freq = 60;
		int pin_number = 15;
		
		test(freq, pin_number);
	}
	
	public static void test(int frequency, int pinNumber) {
		int min_pulse = ServoUtil.calcServoPulse(TOWERPRO_SG90_MIN_MS, frequency, PCA9685.RANGE);
		int mid_pulse = ServoUtil.calcServoPulse(TOWERPRO_SG90_MID_MS, frequency, PCA9685.RANGE);
		int max_pulse = ServoUtil.calcServoPulse(TOWERPRO_SG90_MAX_MS, frequency, PCA9685.RANGE);
		Logger.info("Min={}, Mid={}, Max={}, Range={}", Integer.valueOf(min_pulse),
				Integer.valueOf(mid_pulse), Integer.valueOf(max_pulse), Integer.valueOf(PCA9685.RANGE));
		
		float mid = mid_pulse / (float)PCA9685.RANGE;
		try (PCA9685 pca9685 = new PCA9685(frequency);
				Servo servo = new Servo(pca9685, frequency, PCA9685.RANGE, pinNumber, mid)) {
			Logger.info("Mid");
			pca9685.setServoPulse(pinNumber, TOWERPRO_SG90_MID_MS);
			SleepUtil.sleepMillis(1000);
			Logger.info("Max");
			pca9685.setServoPulse(pinNumber, TOWERPRO_SG90_MAX_MS);
			SleepUtil.sleepMillis(1000);
			Logger.info("Mid");
			pca9685.setServoPulse(pinNumber, TOWERPRO_SG90_MID_MS);
			SleepUtil.sleepMillis(1000);
			Logger.info("Min");
			pca9685.setServoPulse(pinNumber, TOWERPRO_SG90_MIN_MS);
			SleepUtil.sleepMillis(1000);
			Logger.info("Mid");
			pca9685.setServoPulse(pinNumber, TOWERPRO_SG90_MID_MS);
			SleepUtil.sleepMillis(1000);
			
			Logger.info("Mid");
			servo.setValue(mid_pulse / (float)PCA9685.RANGE);
			SleepUtil.sleepMillis(1000);
			
			for (int i=mid_pulse; i>min_pulse; i--) {
				servo.setValue(i / (float)PCA9685.RANGE);
				SleepUtil.sleepMillis(10);
			}
			for (int i=min_pulse; i<max_pulse; i++) {
				servo.setValue(i / (float)PCA9685.RANGE);
				SleepUtil.sleepMillis(10);
			}
			for (int i=max_pulse; i>mid_pulse; i--) {
				servo.setValue(i / (float)PCA9685.RANGE);
				SleepUtil.sleepMillis(10);
			}
			
			for (double pulse_ms=TOWERPRO_SG90_MID_MS; pulse_ms<TOWERPRO_SG90_MAX_MS; pulse_ms+=0.01) {
				servo.setPulseMs(pulse_ms);
				SleepUtil.sleepMillis(10);
			}
			for (double pulse_ms=TOWERPRO_SG90_MAX_MS; pulse_ms>TOWERPRO_SG90_MIN_MS; pulse_ms-=0.01) {
				servo.setPulseMs(pulse_ms);
				SleepUtil.sleepMillis(10);
			}
			for (double pulse_ms=TOWERPRO_SG90_MIN_MS; pulse_ms<TOWERPRO_SG90_MID_MS; pulse_ms+=0.01) {
				servo.setPulseMs(pulse_ms);
				SleepUtil.sleepMillis(10);
			}
		}
	}
}
