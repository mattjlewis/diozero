package com.diozero.sampleapps.sandpit;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     Hexapod.java
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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.tinylog.Logger;

import com.diozero.animation.Animation;
import com.diozero.animation.AnimationInstance;
import com.diozero.animation.easing.Quad;
import com.diozero.api.DeviceInterface;
import com.diozero.api.ServoDevice;
import com.diozero.api.ServoTrim;
import com.diozero.api.function.FloatConsumerCollection;
import com.diozero.devices.PCA9685;
import com.diozero.util.SleepUtil;

public class Hexapod implements AutoCloseable {
	private PCA9685 pca9685Left;
	private PCA9685 pca9685Right;

	private ServoTrim trim;

	private String easeIn = Quad.IN;
	private String easeOut = Quad.OUT;
	private String easeInOut = Quad.IN_OUT;

	// This object describes the "leg lift" used in walking
	private Lift lift = new Lift(30, -20);
	// This object contains the home positions of each servo in its forward, mid
	// and rear position for the three steps in walking
	private Positions home = new Positions( //
			new LegPosition( //
					new int[] { 56, 70, 91 }, //
					new int[] { 116, 120, 119 }, //
					new int[] { 97, 110, 116 }), //
			new LegPosition( //
					new int[] { 70, 88, 109 }, //
					new int[] { 116, 117, 119 }, //
					new int[] { 102, 106, 104 }), //
			new LegPosition( //
					new int[] { 56, 70, 91 }, //
					new int[] { 116, 120, 119 }, //
					new int[] { 97, 110, 116 }) //
	);
	// This object contains our end effector positions for the three steps in turns
	private Positions turns = new Positions( //
			new LegPosition( //
					new int[] { 56, 70, 85 }, //
					new int[] { 121, 120, 119 }, //
					new int[] { 117, 110, 105 }), //
			new LegPosition( //
					new int[] { 73, 88, 105 }, //
					new int[] { 118, 117, 118 }, //
					new int[] { 107, 106, 107 }), //
			new LegPosition( //
					new int[] { 56, 70, 85 }, //
					new int[] { 121, 120, 119 }, //
					new int[] { 117, 110, 105 }) //
	);
	// This object contains the home positions of each servo for the seven steps
	// in walk and crawl
	private Positions steps = new Positions( //
			new LegPosition( //
					new int[] { 56, 59, 65, 70, 76, 82, 91 }, //
					new int[] { 116, 117, 119, 120, 120, 119, 119 }, //
					new int[] { 97, 101, 106, 110, 112, 114, 116 }), //
			new LegPosition( //
					new int[] { 70, 76, 82, 88, 94, 100, 109 }, //
					new int[] { 116, 119, 118, 117, 118, 117, 116 }, //
					new int[] { 102, 105, 106, 106, 108, 106, 104 }), //
			new LegPosition( //
					new int[] { 91, 82, 76, 70, 65, 59, 56 }, //
					new int[] { 119, 119, 120, 120, 119, 117, 116 }, //
					new int[] { 116, 114, 112, 110, 106, 101, 97 }) //
	);
	// This object contains the sleep positions for our joints
	private LegPosition sleep = new LegPosition(new int[] { 90 }, new int[] { 165 }, new int[] { 150 });

	// Each leg has 3 servos; coxa (hip fowards / backwards), femur (hip up / down),
	// tibia (knee up / down)

	// Left front leg
	private ServoDevice l1c;
	private ServoDevice l1f;
	private ServoDevice l1t;
	private FloatConsumerCollection l1;

	// Right front leg
	private ServoDevice r1c;
	private ServoDevice r1f;
	private ServoDevice r1t;
	private FloatConsumerCollection r1;

	// Left middle leg
	private ServoDevice l2c;
	private ServoDevice l2f;
	private ServoDevice l2t;
	private FloatConsumerCollection l2;

	// Right middle leg
	private ServoDevice r2c;
	private ServoDevice r2f;
	private ServoDevice r2t;
	private FloatConsumerCollection r2;

	// Left back leg
	private ServoDevice l3c;
	private ServoDevice l3f;
	private ServoDevice l3t;
	private FloatConsumerCollection l3;

	// Right back leg
	private ServoDevice r3c;
	private ServoDevice r3f;
	private ServoDevice r3t;
	private FloatConsumerCollection r3;

	private FloatConsumerCollection coxae;
	private FloatConsumerCollection femura;
	private FloatConsumerCollection tibiae;

	private FloatConsumerCollection innerCoxae;
	private FloatConsumerCollection outerCoxae;

	private FloatConsumerCollection frontCoxae;
	private FloatConsumerCollection frontFemura;
	private FloatConsumerCollection frontTibiae;
	private FloatConsumerCollection midCoxae;
	private FloatConsumerCollection midFemura;
	private FloatConsumerCollection midTibiae;
	private FloatConsumerCollection rearCoxae;
	private FloatConsumerCollection rearFemura;
	private FloatConsumerCollection rearTibiae;

	private FloatConsumerCollection leftOuterCoxae;
	private FloatConsumerCollection rightOuterCoxae;
	private FloatConsumerCollection leftOuterFemura;
	private FloatConsumerCollection rightOuterFemura;
	private FloatConsumerCollection leftOuterTibiae;
	private FloatConsumerCollection rightOuterTibiae;

	private FloatConsumerCollection jointPairs;
	private FloatConsumerCollection joints;
	private FloatConsumerCollection altJoints;
	private FloatConsumerCollection triJoints;

	private Collection<? extends DeviceInterface> legs;

	public static void main(String[] args) {
		int i2c_controller = 1;
		int pwm_frequency = 50;

		try (Hexapod hexapod = new Hexapod(i2c_controller, pwm_frequency)) {
			hexapod.run();
		}
	}

	public Hexapod(int i2cController, int pwmFrequency) {
		pca9685Left = new PCA9685(i2cController, 0x40, pwmFrequency);
		pca9685Right = new PCA9685(i2cController, 0x42, pwmFrequency);

		// trim = Servo.Trim.MG996R;
		// Constrain to reduced range
		trim = new ServoTrim(ServoTrim.DEFAULT_MID_US, ServoTrim.DEFAULT_90_DELTA_US,
				(int) (ServoTrim.DEFAULT_90_DELTA_US * (2 / 3f)));

		l1c = ServoDevice.newBuilder(0).setDeviceFactory(pca9685Left).setTrim(trim).build();
		l1f = ServoDevice.newBuilder(1).setDeviceFactory(pca9685Left).setTrim(trim).setInverted(true).build();
		l1t = ServoDevice.newBuilder(2).setDeviceFactory(pca9685Left).setTrim(trim).setInverted(true).build();
		l1 = new FloatConsumerCollection(Arrays.asList(l1c::setAngle, l1f::setAngle, l1t::setAngle));

		r1c = ServoDevice.newBuilder(14).setDeviceFactory(pca9685Left).setTrim(trim).setInverted(true).build();
		r1f = ServoDevice.newBuilder(15).setDeviceFactory(pca9685Left).setTrim(trim).build();
		r1t = ServoDevice.newBuilder(13).setDeviceFactory(pca9685Left).setTrim(trim).build();
		r1 = new FloatConsumerCollection(Arrays.asList(r1c::setAngle, r1f::setAngle, r1t::setAngle));

		l2c = ServoDevice.newBuilder(8).setDeviceFactory(pca9685Left).setTrim(trim).build();
		l2f = ServoDevice.newBuilder(9).setDeviceFactory(pca9685Left).setTrim(trim).setInverted(true).build();
		l2t = ServoDevice.newBuilder(10).setDeviceFactory(pca9685Left).setTrim(trim).setInverted(true).build();
		l2 = new FloatConsumerCollection(Arrays.asList(l2c::setAngle, l2f::setAngle, l2t::setAngle));

		r2c = ServoDevice.newBuilder(6).setDeviceFactory(pca9685Left).setTrim(trim).setInverted(true).build();
		r2f = ServoDevice.newBuilder(7).setDeviceFactory(pca9685Left).setTrim(trim).build();
		r2t = ServoDevice.newBuilder(5).setDeviceFactory(pca9685Left).setTrim(trim).build();
		r2 = new FloatConsumerCollection(Arrays.asList(r2c::setAngle, r2f::setAngle, r2t::setAngle));

		l3c = ServoDevice.newBuilder(12).setDeviceFactory(pca9685Left).setTrim(trim).setInverted(true).build();
		l3f = ServoDevice.newBuilder(13).setDeviceFactory(pca9685Left).setTrim(trim).setInverted(true).build();
		l3t = ServoDevice.newBuilder(14).setDeviceFactory(pca9685Left).setTrim(trim).setInverted(true).build();
		l3 = new FloatConsumerCollection(Arrays.asList(l3c::setAngle, l3f::setAngle, l3t::setAngle));

		r3c = ServoDevice.newBuilder(2).setDeviceFactory(pca9685Left).setTrim(trim).build();
		r3f = ServoDevice.newBuilder(3).setDeviceFactory(pca9685Left).setTrim(trim).build();
		r3t = ServoDevice.newBuilder(1).setDeviceFactory(pca9685Left).setTrim(trim).build();
		r3 = new FloatConsumerCollection(Arrays.asList(r3c::setAngle, r3f::setAngle, r3t::setAngle));

		coxae = new FloatConsumerCollection(Arrays.asList(l1c::setAngle, r1c::setAngle, l2c::setAngle, r2c::setAngle,
				l3c::setAngle, r3c::setAngle));
		femura = new FloatConsumerCollection(Arrays.asList(l1f::setAngle, r1f::setAngle, l2f::setAngle, r2f::setAngle,
				l3f::setAngle, r3f::setAngle));
		tibiae = new FloatConsumerCollection(Arrays.asList(l1t::setAngle, r1t::setAngle, l2t::setAngle, r2t::setAngle,
				l3t::setAngle, r3t::setAngle));
		innerCoxae = new FloatConsumerCollection(Arrays.asList(l2c::setAngle, r2c::setAngle));
		outerCoxae = new FloatConsumerCollection(
				Arrays.asList(l1c::setAngle, r1c::setAngle, l3c::setAngle, r3c::setAngle));
		frontCoxae = new FloatConsumerCollection(Arrays.asList(l1c::setAngle, r1c::setAngle));
		frontFemura = new FloatConsumerCollection(Arrays.asList(l1f::setAngle, r1f::setAngle));
		frontTibiae = new FloatConsumerCollection(Arrays.asList(l1t::setAngle, r1t::setAngle));
		midCoxae = new FloatConsumerCollection(Arrays.asList(l2c::setAngle, r2c::setAngle));
		midFemura = new FloatConsumerCollection(Arrays.asList(l2f::setAngle, r2f::setAngle));
		midTibiae = new FloatConsumerCollection(Arrays.asList(l2t::setAngle, r2t::setAngle));
		rearCoxae = new FloatConsumerCollection(Arrays.asList(l3c::setAngle, r3c::setAngle));
		rearFemura = new FloatConsumerCollection(Arrays.asList(l3f::setAngle, r3f::setAngle));
		rearTibiae = new FloatConsumerCollection(Arrays.asList(l3t::setAngle, r3t::setAngle));
		leftOuterCoxae = new FloatConsumerCollection(Arrays.asList(l1c::setAngle, l3c::setAngle));
		rightOuterCoxae = new FloatConsumerCollection(Arrays.asList(r1c::setAngle, r3c::setAngle));
		leftOuterFemura = new FloatConsumerCollection(Arrays.asList(l1f::setAngle, l3f::setAngle));
		rightOuterFemura = new FloatConsumerCollection(Arrays.asList(r1f::setAngle, r3f::setAngle));
		leftOuterTibiae = new FloatConsumerCollection(Arrays.asList(l1t::setAngle, l3t::setAngle));
		rightOuterTibiae = new FloatConsumerCollection(Arrays.asList(r1t::setAngle, r3t::setAngle));

		jointPairs = new FloatConsumerCollection(Arrays.asList(frontCoxae, frontFemura, frontTibiae, midCoxae,
				midFemura, midTibiae, rearCoxae, rearFemura, rearTibiae));

		joints = new FloatConsumerCollection(Arrays.asList(coxae, femura, tibiae));
		altJoints = new FloatConsumerCollection(Arrays.asList(innerCoxae, outerCoxae, femura, tibiae));
		triJoints = new FloatConsumerCollection(Arrays.asList(leftOuterCoxae, r2c::setAngle, leftOuterFemura,
				r2f::setAngle, leftOuterTibiae, r2t::setAngle, rightOuterCoxae, l2c::setAngle, rightOuterFemura,
				l2f::setAngle, rightOuterTibiae, l2t::setAngle));

		legs = Arrays.asList(l1c, l1f, l1t, r1c, r1f, r1t, l2c, l2f, l2t, r2c, r2f, r2t, l3c, l3f, l3t, r3c, r3f, r3t);
	}

	public void run() {
		setTo(home, LegPosition.FORWARD);
		SleepUtil.sleepMillis(500);
		setTo(home, LegPosition.MID);
		SleepUtil.sleepMillis(500);
		setTo(home, LegPosition.REAR);
		SleepUtil.sleepMillis(500);
		setTo(home, LegPosition.MID);
		SleepUtil.sleepMillis(500);

		testRange();
		SleepUtil.sleepMillis(500);

		testAnimation();
	}

	private void setTo(Positions position, int forwardMidRear) {
		Logger.info("Setting positions to " + forwardMidRear);
		frontCoxae.accept(position.front.coxae[forwardMidRear]);
		frontFemura.accept(position.front.femura[forwardMidRear]);
		frontTibiae.accept(position.front.tibiae[forwardMidRear]);
		midCoxae.accept(position.mid.coxae[forwardMidRear]);
		midFemura.accept(position.mid.femura[forwardMidRear]);
		midTibiae.accept(position.mid.tibiae[forwardMidRear]);
		rearCoxae.accept(position.rear.coxae[forwardMidRear]);
		rearFemura.accept(position.rear.femura[forwardMidRear]);
		rearTibiae.accept(position.rear.tibiae[forwardMidRear]);
	}

	private void testRange() {
		Logger.info("Testing servo ranges for all servos");
		int delay = 10;
		float delta = 5;
		for (FloatConsumerCollection servos : Arrays.asList(l2, r2, l3, coxae, femura, tibiae)) {
			for (float angle = trim.getMidAngle(); angle < trim.getMaxAngle(); angle += delta) {
				servos.accept(angle);
				SleepUtil.sleepMillis(delay);
			}
			for (float angle = trim.getMaxAngle(); angle > trim.getMinAngle(); angle -= delta) {
				servos.accept(angle);
				SleepUtil.sleepMillis(delay);
			}
			for (float angle = trim.getMinAngle(); angle < trim.getMidAngle(); angle += delta) {
				servos.accept(angle);
				SleepUtil.sleepMillis(delay);
			}
		}
	}

	private void testAnimation() {
		Logger.info("Testing servo animation");
		// From sleep to stand
		List<AnimationInstance.KeyFrame[]> stand_key_frames = Arrays.asList( //
				new AnimationInstance.KeyFrame[] { //
						new AnimationInstance.KeyFrame(false), //
						new AnimationInstance.KeyFrame(false), //
						new AnimationInstance.KeyFrame(home.mid.coxae[1]), //
						new AnimationInstance.KeyFrame(false), //
						new AnimationInstance.KeyFrame(false) //
				}, //
				new AnimationInstance.KeyFrame[] { //
						new AnimationInstance.KeyFrame(false), //
						new AnimationInstance.KeyFrame(false), //
						new AnimationInstance.KeyFrame(home.front.coxae[1]), //
						new AnimationInstance.KeyFrame(false), //
						new AnimationInstance.KeyFrame(false) //
				}, //
				new AnimationInstance.KeyFrame[] { //
						new AnimationInstance.KeyFrame(false), //
						new AnimationInstance.KeyFrame(false), //
						new AnimationInstance.KeyFrame(false), //
						new AnimationInstance.KeyFrame(home.front.femura[1] + 20).setEasing(easeOut), //
						new AnimationInstance.KeyFrame(home.front.femura[1]).setEasing(easeIn) //
				}, //
				new AnimationInstance.KeyFrame[] { //
						new AnimationInstance.KeyFrame(false), //
						new AnimationInstance.KeyFrame(home.front.tibiae[1]), //
						new AnimationInstance.KeyFrame(false), //
						new AnimationInstance.KeyFrame(home.front.tibiae[1]), //
						new AnimationInstance.KeyFrame(false) //
				} //
		);
		Animation stand = new Animation(altJoints.getDevices(), 100, Quad::easeOut, 1);
		stand.enqueue(500, new float[] { 0, 0.1f, 0.3f, 0.7f, 1.0f }, stand_key_frames);
		Future<?> future = stand.play();
		try {
			future.get();
			Logger.info("Waiting");
			future.get();
			Logger.info("Finished");
		} catch (CancellationException | ExecutionException | InterruptedException e) {
			Logger.info("Finished {}", e);
		}

		// From stand to sleep
		List<AnimationInstance.KeyFrame[]> sleep_key_frames = Arrays.asList( //
				new AnimationInstance.KeyFrame[] { //
						new AnimationInstance.KeyFrame(false), //
						new AnimationInstance.KeyFrame(false), //
						new AnimationInstance.KeyFrame(false), //
						new AnimationInstance.KeyFrame(sleep.coxae[0]).setEasing(easeOut), //
				}, //
				new AnimationInstance.KeyFrame[] { //
						new AnimationInstance.KeyFrame(false), //
						new AnimationInstance.KeyFrame(false), //
						new AnimationInstance.KeyFrame(false), //
						new AnimationInstance.KeyFrame(sleep.coxae[0]).setEasing(easeOut), //
				}, //
				new AnimationInstance.KeyFrame[] { //
						new AnimationInstance.KeyFrame(false), //
						new AnimationInstance.KeyFrame(home.front.femura[1] + 20).setEasing(easeOut), //
						new AnimationInstance.KeyFrame(sleep.femura[0]).setEasing(easeInOut), //
						new AnimationInstance.KeyFrame(false) //
				}, //
				new AnimationInstance.KeyFrame[] { //
						new AnimationInstance.KeyFrame(false), //
						new AnimationInstance.KeyFrame(false), //
						new AnimationInstance.KeyFrame(sleep.tibiae[0]).setEasing(easeInOut), //
						new AnimationInstance.KeyFrame(false) //
				} //
		);
		Animation sleep = new Animation(altJoints.getDevices(), 100, Quad::easeOut, 1);
		sleep.enqueue(500, new float[] { 0, 0.1f, 0.3f, 0.7f, 1.0f }, sleep_key_frames);
		future = sleep.play();
		try {
			future.get();
			Logger.info("Waiting");
			future.get();
			Logger.info("Finished");
		} catch (CancellationException | ExecutionException | InterruptedException e) {
			Logger.info("Finished {}", e);
		}
	}

	@Override
	public void close() {
		legs.forEach(DeviceInterface::close);
		pca9685Left.close();
		pca9685Right.close();
	}

	static class Lift {
		public float femur;
		public float tibia;

		public Lift(float femur, float tibia) {
			this.femur = femur;
			this.tibia = tibia;
		}
	}

	static class LegPosition {
		public static final int FORWARD = 0;
		public static final int MID = 1;
		public static final int REAR = 2;

		public int[] coxae;
		public int[] femura;
		public int[] tibiae;

		public LegPosition(int[] coxae, int[] femura, int[] tibiae) {
			this.coxae = coxae;
			this.femura = femura;
			this.tibiae = tibiae;
		}
	}

	static class Positions {
		public LegPosition front;
		public LegPosition mid;
		public LegPosition rear;

		public Positions(LegPosition front, LegPosition mid, LegPosition rear) {
			this.front = front;
			this.mid = mid;
			this.rear = rear;
		}
	}
}
