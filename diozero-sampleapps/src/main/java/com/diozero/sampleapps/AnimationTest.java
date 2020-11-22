package com.diozero.sampleapps;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Sample applications
 * Filename:     AnimationTest.java  
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
import com.diozero.api.OutputDeviceInterface;
import com.diozero.devices.PwmLed;
import com.diozero.sbc.DeviceFactoryHelper;

public class AnimationTest {
	public static void main(String[] args) {
		if (args.length < 2) {
			Logger.error("Usage: {} <gpio1> <gpio2>", AnimationTest.class.getName());
			System.exit(1);
		}
		test(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
	}
	
	public static void test(int pin1, int pin2) {
		try (PwmLed led1 = new PwmLed(pin1); PwmLed led2 = new PwmLed(pin2)) { 
			Collection<OutputDeviceInterface> targets = Arrays.asList(led1, led2);
			
			Animation anim = new Animation(targets, 100, Quad::easeIn, 1);
			anim.setLoop(true);
			
			// How long the animation is
			int duration = 5000;
			// Relative time points (0..1)
			float[] cue_points = { 0, 0.6f, 0.8f, 1 };
			// Value for each target at the corresponding cue points
			//float[][] key_frames = { { 1, 5, 10 }, { 2, 4, 9 }, { 3, 6, 8 } };
			List<AnimationInstance.KeyFrame[]> key_frames = AnimationInstance.KeyFrame.fromValues(
				new float[][] { {0, 1}, {0.2f, 0.8f}, {0.8f, 0.2f}, {1, 0} } );
			anim.enqueue(duration, cue_points, key_frames);
			
			Logger.info("Starting animation...");
			Future<?> future = anim.play();
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
	}
}
