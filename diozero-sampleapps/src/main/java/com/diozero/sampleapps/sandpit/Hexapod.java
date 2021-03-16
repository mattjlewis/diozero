package com.diozero.sampleapps.sandpit;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Sample applications
 * Filename:     Hexapod.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2021 diozero
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
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.tinylog.Logger;

import com.diozero.animation.Animation;
import com.diozero.animation.AnimationInstance;
import com.diozero.animation.easing.Quad;
import com.diozero.api.OutputDeviceCollection;
import com.diozero.api.Servo;
import com.diozero.devices.PCA9685;
import com.diozero.util.SleepUtil;

public class Hexapod implements AutoCloseable {
	private PCA9685 pca9685Left;
	private PCA9685 pca9685Right;
	
	private Servo.Trim trim;
	
	private String easeIn = Quad.IN;
	private String easeOut = Quad.OUT;
	private String easeInOut = Quad.IN_OUT;
	
	// This object describes the "leg lift" used in walking
	private Lift lift = new Lift(30, -20);
	// This object contains the home positions of each servo in its forward, mid
	// and rear position for the three steps in walking
	private Positions home = new Positions(
			new LegPosition(
					new float[] { 56, 70, 91 },
					new float[] { 116, 120, 119 },
					new float[] { 97, 110, 116 } ),
			new LegPosition(
					new float[] { 70, 88, 109 },
					new float[] { 116, 117, 119 },
					new float[] { 102, 106, 104 } ),
			new LegPosition(
					new float[] { 56, 70, 91 },
					new float[] { 116, 120, 119 },
					new float[] { 97, 110, 116 } )
			);
	// This object contains our end effector positions for the three steps in turns
	private Positions turns = new Positions(
			new LegPosition(
					new float[] { 56, 70, 85 },
					new float[] { 121, 120, 119 },
					new float[] { 117, 110, 105 } ),
			new LegPosition(
					new float[] { 73, 88, 105 },
					new float[] { 118, 117, 118 },
					new float[] { 107, 106, 107 } ),
			new LegPosition(
					new float[] { 56, 70, 85 },
					new float[] { 121, 120, 119 },
					new float[] { 117, 110, 105 } )
			);
	// This object contains the home positions of each servo for the seven steps
	// in walk and crawl
	private Positions steps = new Positions(
			new LegPosition(
					new float[] { 56, 59, 65, 70, 76, 82, 91 },
					new float[] { 116, 117,119, 120, 120, 119, 119 },
					new float[] { 97, 101, 106, 110, 112, 114, 116 } ),
			new LegPosition(
					new float[] { 70, 76, 82, 88, 94, 100, 109 },
					new float[] { 116, 119, 118, 117, 118, 117, 116 },
					new float[] { 102, 105, 106, 106, 108, 106, 104 } ),
			new LegPosition(
					new float[] { 91, 82, 76, 70, 65, 59, 56 },
					new float[] { 119, 119,120, 120, 119, 117, 116 },
					new float[] { 116, 114, 112, 110, 106, 101, 97 } )
			);
	// This object contains the sleep positions for our joints
	private LegPosition sleep = new LegPosition(
			new float[] { 90 },
			new float[] { 165 },
			new float[] { 150 } );
	
	// Each leg has 3 servos; coxa (hip fowards / backwards), femur (hip up / down), tibia (knee up / down)
	
	// Left front leg
	private Servo l1c;
	private Servo l1f;
	private Servo l1t;
	private OutputDeviceCollection l1;
	
	// Right front leg
	private Servo r1c;
	private Servo r1f;
	private Servo r1t;
	private OutputDeviceCollection r1;
	
	// Left middle leg
	private Servo l2c;
	private Servo l2f;
	private Servo l2t;
	private OutputDeviceCollection l2;
	
	// Right middle leg
	private Servo r2c;
	private Servo r2f;
	private Servo r2t;
	private OutputDeviceCollection r2;
	
	// Left back leg
	private Servo l3c;
	private Servo l3f;
	private Servo l3t;
	private OutputDeviceCollection l3;
	
	// Right back leg
	private Servo r3c;
	private Servo r3f;
	private Servo r3t;
	private OutputDeviceCollection r3;

	private OutputDeviceCollection coxae;
	private OutputDeviceCollection femura;
	private OutputDeviceCollection tibiae;
	
	private OutputDeviceCollection innerCoxae;
	private OutputDeviceCollection outerCoxae;
	
	private OutputDeviceCollection frontCoxae;
	private OutputDeviceCollection frontFemura;
	private OutputDeviceCollection frontTibiae;
	private OutputDeviceCollection midCoxae;
	private OutputDeviceCollection midFemura;
	private OutputDeviceCollection midTibiae;
	private OutputDeviceCollection rearCoxae;
	private OutputDeviceCollection rearFemura;
	private OutputDeviceCollection rearTibiae;
	
	private OutputDeviceCollection leftOuterCoxae;
	private OutputDeviceCollection rightOuterCoxae;
	private OutputDeviceCollection leftOuterFemura;
	private OutputDeviceCollection rightOuterFemura;
	private OutputDeviceCollection leftOuterTibiae;
	private OutputDeviceCollection rightOuterTibiae;
	
	private OutputDeviceCollection jointPairs;
	private OutputDeviceCollection joints;
	private OutputDeviceCollection altJoints;
	private OutputDeviceCollection triJoints;
	
	private Servo.Array legs;
	
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
		
		//trim = Servo.Trim.MG996R;
		// Constrain to reduced range
		trim = new Servo.Trim(Servo.Trim.DEFAULT_MID, Servo.Trim.DEFAULT_90_DELTA, Servo.Trim.DEFAULT_90_DELTA*(2/3f));
		
		l1c = new Servo(pca9685Left, 0, trim.getMidPulseWidthMs(), pwmFrequency, trim);
		l1c.setOutputDeviceUnit(Servo.OutputDeviceUnit.DEGREES);
		l1f = new Servo(pca9685Left, 1, trim.getMidPulseWidthMs(), pwmFrequency, trim);
		l1f.setOutputDeviceUnit(Servo.OutputDeviceUnit.DEGREES).setInverted(true);
		l1t = new Servo(pca9685Left, 2, trim.getMidPulseWidthMs(), pwmFrequency, trim);
		l1t.setOutputDeviceUnit(Servo.OutputDeviceUnit.DEGREES).setInverted(true);
		l1 = new OutputDeviceCollection(l1c, l1f, l1t);
		
		r1c = new Servo(pca9685Right, 14, trim.getMidPulseWidthMs(), pwmFrequency, trim);
		r1c.setOutputDeviceUnit(Servo.OutputDeviceUnit.DEGREES).setInverted(true);
		r1f = new Servo(pca9685Right, 15, trim.getMidPulseWidthMs(), pwmFrequency, trim);
		r1f.setOutputDeviceUnit(Servo.OutputDeviceUnit.DEGREES);
		r1t = new Servo(pca9685Right, 13, trim.getMidPulseWidthMs(), pwmFrequency, trim);
		r1t.setOutputDeviceUnit(Servo.OutputDeviceUnit.DEGREES);
		r1 = new OutputDeviceCollection(r1c, r1f, r1t);

		l2c = new Servo(pca9685Left, 8, trim.getMidPulseWidthMs(), pwmFrequency, trim);
		l2c.setOutputDeviceUnit(Servo.OutputDeviceUnit.DEGREES);
		l2f = new Servo(pca9685Left, 9, trim.getMidPulseWidthMs(), pwmFrequency, trim);
		l2f.setOutputDeviceUnit(Servo.OutputDeviceUnit.DEGREES).setInverted(true);
		l2t = new Servo(pca9685Left, 10, trim.getMidPulseWidthMs(), pwmFrequency, trim);
		l2t.setOutputDeviceUnit(Servo.OutputDeviceUnit.DEGREES).setInverted(true);
		l2 = new OutputDeviceCollection(l2c, l2f, l2t);
		
		r2c = new Servo(pca9685Right, 6, trim.getMidPulseWidthMs(), pwmFrequency, trim);
		r2c.setOutputDeviceUnit(Servo.OutputDeviceUnit.DEGREES).setInverted(true);
		r2f = new Servo(pca9685Right, 7, trim.getMidPulseWidthMs(), pwmFrequency, trim);
		r2f.setOutputDeviceUnit(Servo.OutputDeviceUnit.DEGREES);
		r2t = new Servo(pca9685Right, 5, trim.getMidPulseWidthMs(), pwmFrequency, trim);
		r2t.setOutputDeviceUnit(Servo.OutputDeviceUnit.DEGREES);
		r2 = new OutputDeviceCollection(r2c, r2f, r2t);
		
		l3c = new Servo(pca9685Left, 12, trim.getMidPulseWidthMs(), pwmFrequency, trim);
		l3c.setOutputDeviceUnit(Servo.OutputDeviceUnit.DEGREES).setInverted(true);
		l3f = new Servo(pca9685Left, 13, trim.getMidPulseWidthMs(), pwmFrequency, trim);
		l3f.setOutputDeviceUnit(Servo.OutputDeviceUnit.DEGREES).setInverted(true);
		l3t = new Servo(pca9685Left, 14, trim.getMidPulseWidthMs(), pwmFrequency, trim);
		l3t.setOutputDeviceUnit(Servo.OutputDeviceUnit.DEGREES).setInverted(true);
		l3 = new OutputDeviceCollection(l3c, l3f, l3t);
		
		r3c = new Servo(pca9685Right, 2, trim.getMidPulseWidthMs(), pwmFrequency, trim);
		r3c.setOutputDeviceUnit(Servo.OutputDeviceUnit.DEGREES);
		r3f = new Servo(pca9685Right, 3, trim.getMidPulseWidthMs(), pwmFrequency, trim);
		r3f.setOutputDeviceUnit(Servo.OutputDeviceUnit.DEGREES);
		r3t = new Servo(pca9685Right, 1, trim.getMidPulseWidthMs(), pwmFrequency, trim);
		r3t.setOutputDeviceUnit(Servo.OutputDeviceUnit.DEGREES);
		r3 = new OutputDeviceCollection(r3c, r3f, r3t);
		
		coxae = new OutputDeviceCollection(l1c, r1c, l2c, r2c, l3c, r3c);
		femura = new OutputDeviceCollection(l1f, r1f, l2f, r2f, l3f, r3f);
		tibiae = new OutputDeviceCollection(l1t, r1t, l2t, r2t, l3t, r3t);
		innerCoxae = new OutputDeviceCollection(l2c, r2c);
		outerCoxae = new OutputDeviceCollection(l1c, r1c, l3c, r3c);
		frontCoxae = new OutputDeviceCollection(l1c, r1c);
		frontFemura = new OutputDeviceCollection(l1f, r1f);
		frontTibiae = new OutputDeviceCollection(l1t, r1t);
		midCoxae = new OutputDeviceCollection(l2c, r2c);
		midFemura = new OutputDeviceCollection(l2f, r2f);
		midTibiae = new OutputDeviceCollection(l2t, r2t);
		rearCoxae = new OutputDeviceCollection(l3c, r3c);
		rearFemura = new OutputDeviceCollection(l3f, r3f);
		rearTibiae = new OutputDeviceCollection(l3t, r3t);
		leftOuterCoxae = new OutputDeviceCollection(l1c, l3c);
		rightOuterCoxae = new OutputDeviceCollection(r1c, r3c);
		leftOuterFemura = new OutputDeviceCollection(l1f, l3f);
		rightOuterFemura = new OutputDeviceCollection(r1f, r3f);
		leftOuterTibiae = new OutputDeviceCollection(l1t, l3t);
		rightOuterTibiae = new OutputDeviceCollection(r1t, r3t);
		
		jointPairs = new OutputDeviceCollection(frontCoxae, frontFemura, frontTibiae, midCoxae, midFemura, midTibiae,
				rearCoxae, rearFemura, rearTibiae);
		
		joints = new OutputDeviceCollection(coxae, femura, tibiae);
		altJoints = new OutputDeviceCollection(innerCoxae, outerCoxae, femura, tibiae);
		triJoints = new OutputDeviceCollection(leftOuterCoxae, r2c, leftOuterFemura, r2f, leftOuterTibiae, r2t,
				rightOuterCoxae, l2c, rightOuterFemura, l2f, rightOuterTibiae, l2t);
		
		legs = new Servo.Array(l1c, l1f, l1t, r1c, r1f, r1t, l2c, l2f, l2t, r2c, r2f, r2t, l3c, l3f, l3t, r3c, r3f, r3t);
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
		frontCoxae.setValue(position.front.coxae[forwardMidRear]);
		frontFemura.setValue(position.front.femura[forwardMidRear]);
		frontTibiae.setValue(position.front.tibiae[forwardMidRear]);
		midCoxae.setValue(position.mid.coxae[forwardMidRear]);
		midFemura.setValue(position.mid.femura[forwardMidRear]);
		midTibiae.setValue(position.mid.tibiae[forwardMidRear]);
		rearCoxae.setValue(position.rear.coxae[forwardMidRear]);
		rearFemura.setValue(position.rear.femura[forwardMidRear]);
		rearTibiae.setValue(position.rear.tibiae[forwardMidRear]);
	}
	
	private void testRange() {
		Logger.info("Testing servo ranges for all servos");
		int delay = 10;
		float delta = 5;
		for (OutputDeviceCollection servos : Arrays.asList(l2, r2, l3, coxae, femura, tibiae, legs)) {
			for (float angle=trim.getMidAngle(); angle<trim.getMaxAngle(); angle+=delta) {
				servos.setValue(angle);
				SleepUtil.sleepMillis(delay);
			}
			for (float angle=trim.getMaxAngle(); angle>trim.getMinAngle(); angle-=delta) {
				servos.setValue(angle);
				SleepUtil.sleepMillis(delay);
			}
			for (float angle=trim.getMinAngle(); angle<trim.getMidAngle(); angle+=delta) {
				servos.setValue(angle);
				SleepUtil.sleepMillis(delay);
			}
		}
	}
	
	private void testAnimation() {
		Logger.info("Testing servo animation");
		// From sleep to stand
		List<AnimationInstance.KeyFrame[]> stand_key_frames = Arrays.asList(
			new AnimationInstance.KeyFrame[] {
					new AnimationInstance.KeyFrame(false),
					new AnimationInstance.KeyFrame(false),
					new AnimationInstance.KeyFrame(home.mid.coxae[1]),
					new AnimationInstance.KeyFrame(false),
					new AnimationInstance.KeyFrame(false)
				},
			new AnimationInstance.KeyFrame[] {
					new AnimationInstance.KeyFrame(false),
					new AnimationInstance.KeyFrame(false),
					new AnimationInstance.KeyFrame(home.front.coxae[1]),
					new AnimationInstance.KeyFrame(false),
					new AnimationInstance.KeyFrame(false)
				},
			new AnimationInstance.KeyFrame[] {
					new AnimationInstance.KeyFrame(false),
					new AnimationInstance.KeyFrame(false),
					new AnimationInstance.KeyFrame(false),
					new AnimationInstance.KeyFrame(home.front.femura[1] + 20).setEasing(easeOut),
					new AnimationInstance.KeyFrame(home.front.femura[1]).setEasing(easeIn)
				},
			new AnimationInstance.KeyFrame[] {
					new AnimationInstance.KeyFrame(false),
					new AnimationInstance.KeyFrame(home.front.tibiae[1]),
					new AnimationInstance.KeyFrame(false),
					new AnimationInstance.KeyFrame(home.front.tibiae[1]),
					new AnimationInstance.KeyFrame(false)
				}
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
		List<AnimationInstance.KeyFrame[]> sleep_key_frames = Arrays.asList(
			new AnimationInstance.KeyFrame[] {
					new AnimationInstance.KeyFrame(false),
					new AnimationInstance.KeyFrame(false),
					new AnimationInstance.KeyFrame(false),
					new AnimationInstance.KeyFrame(sleep.coxae[0]).setEasing(easeOut),
				},
			new AnimationInstance.KeyFrame[] {
					new AnimationInstance.KeyFrame(false),
					new AnimationInstance.KeyFrame(false),
					new AnimationInstance.KeyFrame(false),
					new AnimationInstance.KeyFrame(sleep.coxae[0]).setEasing(easeOut),
				},
			new AnimationInstance.KeyFrame[] {
					new AnimationInstance.KeyFrame(false),
					new AnimationInstance.KeyFrame(home.front.femura[1] + 20).setEasing(easeOut),
					new AnimationInstance.KeyFrame(sleep.femura[0]).setEasing(easeInOut),
					new AnimationInstance.KeyFrame(false)
				},
			new AnimationInstance.KeyFrame[] {
					new AnimationInstance.KeyFrame(false),
					new AnimationInstance.KeyFrame(false),
					new AnimationInstance.KeyFrame(sleep.tibiae[0]).setEasing(easeInOut),
					new AnimationInstance.KeyFrame(false)
				}
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
		legs.close();
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
		
		public float[] coxae;
		public float[] femura;
		public float[] tibiae;
		
		public LegPosition(float[] coxae, float[] femura, float[] tibiae) {
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
