package com.diozero.sampleapps;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Sample applications
 * Filename:     GsonAnimationTest.java  
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

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.tinylog.Logger;

import com.diozero.animation.Animation;
import com.diozero.animation.AnimationInstance;
import com.diozero.animation.easing.Cubic;
import com.diozero.animation.easing.EasingFunction;
import com.diozero.animation.easing.Elastic;
import com.diozero.api.OutputDeviceInterface;
import com.diozero.util.DeviceFactoryHelper;
import com.google.gson.Gson;

public class GsonAnimationTest {
	public static void main(String[] args) {
		Collection<OutputDeviceInterface> one_target = Arrays.asList(value -> System.out.println(value));
		Collection<OutputDeviceInterface> two_targets = Arrays.asList(
				value -> System.out.println("1: " + value), value -> System.out.println("2: " + value));

		try {
			animate(one_target, 10, Elastic::easeOut, 1, "/animation1.json", "/animation3.json");
			animate(one_target, 10, Elastic::easeOut, 1, "/animation3.json");
			animate(two_targets, 100, Cubic::easeIn, 1, "/animation2.json");
		} catch (IOException e) {
			Logger.error(e, "Error: {}", e);
		} finally {
			// Required if there are non-daemon threads that will prevent the
			// built-in clean-up routines from running
			DeviceFactoryHelper.getNativeDeviceFactory().close();
		}
	}

	private static void animate(Collection<OutputDeviceInterface> targets, int fps, EasingFunction easing, float speed,
			String... files) throws IOException {
		Animation anim = new Animation(targets, fps, easing, speed);

		Gson gson = new Gson();
		for (String file : files) {
			try (Reader reader = new InputStreamReader(GsonAnimationTest.class.getResourceAsStream(file))) {
				AnimationInstance anim_obj = gson.fromJson(reader, AnimationInstance.class);
	
				anim.enqueue(anim_obj);
			}
		}
		
		Logger.info("Starting animation...");
		Future<?> future = anim.play();
		try {
			Logger.info("Waiting");
			future.get();
			Logger.info("Finished");
		} catch (CancellationException | ExecutionException | InterruptedException e) {
			Logger.info("Finished {}", e);
		}
	}
}
