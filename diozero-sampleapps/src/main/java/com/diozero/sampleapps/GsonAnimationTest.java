package com.diozero.sampleapps;

/*
 * #%L
 * Device I/O Zero - Sample applications
 * %%
 * Copyright (C) 2016 - 2017 mattjlewis
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

import org.pmw.tinylog.Logger;

import com.diozero.api.Animation;
import com.diozero.api.AnimationObject;
import com.diozero.api.OutputDeviceInterface;
import com.diozero.api.easing.Cubic;
import com.diozero.api.easing.Elastic;
import com.google.gson.Gson;

public class GsonAnimationTest {
	public static void main(String[] args) {
		
		Gson gson = new Gson();
		
		try (Reader reader = new InputStreamReader(GsonAnimationTest.class.getResourceAsStream("/animation1.json"))) {
			AnimationObject anim_obj = gson.fromJson(reader, AnimationObject.class);
			System.out.println(anim_obj);
			
			int fps = 100;
			float speed = 1;
			Collection<OutputDeviceInterface> targets = Arrays.asList(value -> System.out.println(value));
			Animation anim = new Animation(targets, fps, Elastic::easeOut, speed);
			anim.enqueue(anim_obj);
			
			Logger.info("Starting animation...");
			Future<?> future = anim.play();
			try {
				Logger.info("Waiting");
				future.get();
				Logger.info("Finished");
			} catch (CancellationException | ExecutionException | InterruptedException e) {
				Logger.info("Finished {}", e);
			}
		} catch (IOException e) {
			Logger.error(e, "Error: {}", e);
		}
		
		try (Reader reader = new InputStreamReader(GsonAnimationTest.class.getResourceAsStream("/animation2.json"))) {
			AnimationObject anim_obj = gson.fromJson(reader, AnimationObject.class);
			System.out.println(anim_obj);
			
			int fps = 100;
			float speed = 1;
			Collection<OutputDeviceInterface> targets = Arrays.asList(
					value -> System.out.println("1: " + value), value -> System.out.println("2: " + value));
			Animation anim = new Animation(targets, fps, Cubic::easeIn, speed);
			anim.enqueue(anim_obj);
			
			Logger.info("Starting animation...");
			Future<?> future = anim.play();
			try {
				Logger.info("Waiting");
				future.get();
				Logger.info("Finished");
			} catch (CancellationException | ExecutionException | InterruptedException e) {
				Logger.info("Finished {}", e);
			}
		} catch (IOException e) {
			Logger.error(e, "Error: {}", e);
		}
	}
}
