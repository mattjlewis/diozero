package com.diozero.animation;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     AnimationTest.java  
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
import com.diozero.animation.easing.EasingFunctions;
import com.diozero.animation.easing.Elastic;
import com.diozero.api.OutputDeviceInterface;
import com.diozero.sbc.DeviceFactoryHelper;

public class AnimationTest {
	public static void main(String[] args) {
		OutputDeviceInterface con1 = (f) -> System.out.println(f);
		//AnimationTest test = new AnimationTest();
		List<OutputDeviceInterface> targets = Arrays.asList(
				con1, AnimationTest::setMyValue/*, (f) -> Logger.info("con2 {}", Float.valueOf(f))*/);
		
		Animation anim = new Animation(targets, 100, EasingFunctions.forName(Elastic.OUT), 1);
		//Animation anim = new Animation(targets, 100, Elastic::easeOut, 1);
		//anim.setLoop(true);
		
		// How long the animation is
		int duration = 2000;
		// Relative time points (0..1)
		float[] cue_points = { 0, 0.2f, 0.8f, 1 };
		// Value for each target at the corresponding cue points
		//List<AnimationInstance.KeyFrame[]> key_frames = AnimationInstance.KeyFrame.from(new float[][] { { 1, 5, 10 }, { 2, 4, 9 }, { 3, 6, 8 } };
		//List<AnimationInstance.KeyFrame[]> key_frames = AnimationInstance.KeyFrame.from(new float[][] { { 1 }, { 2 }, { 3 }, { 1 } };
		//List<AnimationInstance.KeyFrame[]> key_frames = AnimationInstance.KeyFrame.from(new float[][] { { 1, 3 }, { 2, 2 }, { 3, 1 }, { 1, 3 } });
		List<AnimationInstance.KeyFrame[]> key_frames = AnimationInstance.KeyFrame.fromValues(
				new float[][] { {0, 1}, {0.2f, 0.8f}, {0.8f, 0.2f}, {1, 0} } );
		//List<AnimationInstance.KeyFrame[]> key_frames = Arrays.asList(
		//		new KeyFrame[] { new KeyFrame(1) }, new KeyFrame[] { new KeyFrame(0.2f) },
		//		new KeyFrame[] { new KeyFrame(0.5f) }, new KeyFrame[] { new KeyFrame(1) } );
		anim.enqueue(duration, cue_points, key_frames);
		
		Future<?> future = anim.play();
		
		//Thread.sleep(3000);
		//anim.stop();
		try {
			Logger.info("Waiting");
			future.get();
			Logger.info("Finished");
		} catch (CancellationException | ExecutionException | InterruptedException e) {
			Logger.info("Finished {}", e);
		} finally {
			// Required if there are non-daemon threads that will prevent the
			// built-in clean-up routines from running
			DeviceFactoryHelper.getNativeDeviceFactory().close();
		}
	}
	
	public static void setMyValue(float f) {
		System.out.println("2: " + f);
	}
}
