package com.diozero.sampleapps;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.pmw.tinylog.Logger;

import com.diozero.PCA9685;
import com.diozero.api.Animation;
import com.diozero.api.AnimationInstance;
import com.diozero.api.easing.Sine;
import com.diozero.sandpit.Servo;

/**
 * PCA9685 sample application. To run:
 * <ul>
 * <li>sysfs:<br>
 *  {@code java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar com.diozero.sampleapps.PCA9685ServoTest 60 15}</li>
 * <li>JDK Device I/O 1.0:<br>
 *  {@code sudo java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-jdkdio10-$DIOZERO_VERSION.jar:dio-1.0.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.PCA9685ServoTest 60 15}</li>
 * <li>JDK Device I/O 1.1:<br>
 *  {@code sudo java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-jdkdio11-$DIOZERO_VERSION.jar:dio-1.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.PCA9685ServoTest 60 15}</li>
 * <li>Pi4j:<br>
 *  {@code sudo java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-pi4j-$DIOZERO_VERSION.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.PCA9685ServoTest 60 15}</li>
 * <li>wiringPi:<br>
 *  {@code sudo java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-wiringpi-$DIOZERO_VERSION.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.PCA9685ServoTest 60 15}</li>
 * <li>pigpgioJ:<br>
 *  {@code sudo java -cp tinylog-1.2.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-pigpio-$DIOZERO_VERSION.jar:pigpioj-java-1.0.1.jar com.diozero.sampleapps.PCA9685ServoTest 60 15}</li>
 * </ul>
 */
public class PCA9685ServoAnimation {
	private static final float TOWERPRO_SG90_MIN_MS = 0.6f;
	private static final float TOWERPRO_SG90_MAX_MS = 2.4f;
	private static final float TOWERPRO_SG90_MINUS90_MS = 1f;
	private static final float TOWERPRO_SG90_PLUS90_MS = 2f;
	private static final float TOWERPRO_SG90_MID_MS = (TOWERPRO_SG90_MIN_MS + TOWERPRO_SG90_MAX_MS) / 2;
	
	public static void main(String[] args) {
		int pwm_freq = 50;
		test(pwm_freq, 0, 1, 12, 13, 14, 15);
	}
	
	public static void test(int pwmFrequency, int gpio1, int gpio2, int gpio3, int gpio4, int gpio5, int gpio6) {
		try (PCA9685 pca9685 = new PCA9685(pwmFrequency);
				Servo servo1 = new Servo(pca9685, gpio1, pwmFrequency, TOWERPRO_SG90_MIN_MS, TOWERPRO_SG90_MAX_MS,
						TOWERPRO_SG90_MINUS90_MS, TOWERPRO_SG90_PLUS90_MS, TOWERPRO_SG90_MID_MS);
				Servo servo2 = new Servo(pca9685, gpio2, pwmFrequency, TOWERPRO_SG90_MIN_MS, TOWERPRO_SG90_MAX_MS,
						TOWERPRO_SG90_MINUS90_MS, TOWERPRO_SG90_PLUS90_MS, TOWERPRO_SG90_MID_MS);
				Servo servo3 = new Servo(pca9685, gpio3, pwmFrequency, TOWERPRO_SG90_MIN_MS, TOWERPRO_SG90_MAX_MS,
						TOWERPRO_SG90_MINUS90_MS, TOWERPRO_SG90_PLUS90_MS, TOWERPRO_SG90_MID_MS);
				Servo servo4 = new Servo(pca9685, gpio4, pwmFrequency, TOWERPRO_SG90_MIN_MS, TOWERPRO_SG90_MAX_MS,
						TOWERPRO_SG90_MINUS90_MS, TOWERPRO_SG90_PLUS90_MS, TOWERPRO_SG90_MID_MS);
				Servo servo5 = new Servo(pca9685, gpio5, pwmFrequency, TOWERPRO_SG90_MIN_MS, TOWERPRO_SG90_MAX_MS,
						TOWERPRO_SG90_MINUS90_MS, TOWERPRO_SG90_PLUS90_MS, TOWERPRO_SG90_MID_MS);
				Servo servo6 = new Servo(pca9685, gpio6, pwmFrequency, TOWERPRO_SG90_MIN_MS, TOWERPRO_SG90_MAX_MS,
					TOWERPRO_SG90_MINUS90_MS, TOWERPRO_SG90_PLUS90_MS, TOWERPRO_SG90_MID_MS)) {
			Animation animation = new Animation(Arrays.asList(servo1, servo2, servo3, servo4, servo5, servo6), 100, Sine::easeIn, 1f);
			animation.setLoop(true);
			float[] cue_points = { 0, 0.2f, 0.5f, 1 };
			List<AnimationInstance.KeyFrame[]> key_frames = AnimationInstance.KeyFrame.from(
					new float[][] {
						{TOWERPRO_SG90_MIN_MS, TOWERPRO_SG90_MIN_MS, TOWERPRO_SG90_MIN_MS, TOWERPRO_SG90_MIN_MS, TOWERPRO_SG90_MIN_MS, TOWERPRO_SG90_MIN_MS},
						{TOWERPRO_SG90_MID_MS, TOWERPRO_SG90_MID_MS, TOWERPRO_SG90_MID_MS, TOWERPRO_SG90_MID_MS, TOWERPRO_SG90_MID_MS, TOWERPRO_SG90_MID_MS},
						{TOWERPRO_SG90_MAX_MS, TOWERPRO_SG90_MAX_MS, TOWERPRO_SG90_MAX_MS, TOWERPRO_SG90_MAX_MS, TOWERPRO_SG90_MAX_MS, TOWERPRO_SG90_MAX_MS},
						{TOWERPRO_SG90_MIN_MS, TOWERPRO_SG90_MIN_MS, TOWERPRO_SG90_MIN_MS, TOWERPRO_SG90_MIN_MS, TOWERPRO_SG90_MIN_MS, TOWERPRO_SG90_MIN_MS} } );
			AnimationInstance ai = new AnimationInstance(5000, cue_points, key_frames);
			animation.enqueue(ai);
			Future<?> future = animation.play();
			future.get();
		} catch (CancellationException e) {
			Logger.info("Cancelled");
		} catch (InterruptedException | ExecutionException e) {
			Logger.error(e, "Error: {}", e);
		}
	}
}
