package com.diozero.sampleapps;

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
