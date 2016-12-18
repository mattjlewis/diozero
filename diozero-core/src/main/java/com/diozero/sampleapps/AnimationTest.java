package com.diozero.sampleapps;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.pmw.tinylog.Logger;

import com.diozero.PwmLed;
import com.diozero.api.OutputDeviceInterface;
import com.diozero.api.easing.Linear;
import com.diozero.sandpit.Animation;
import com.diozero.util.DioZeroScheduler;

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
			List<OutputDeviceInterface> targets = Arrays.asList(led1, led2);
			
			Animation anim = new Animation(targets, 100, Linear::ease, 1);
			anim.setLoop(true);
			
			// How long the animation is
			int duration = 5000;
			// Relative time points (0..1)
			float[] cue_points = { 0, 0.2f, 0.8f, 1 };
			// Value for each target at the corresponding cue points
			//float[][] key_frames = { { 1, 5, 10 }, { 2, 4, 9 }, { 3, 6, 8 } };
			float[][] key_frames = { { 0, 1 }, { 0.2f, 0.8f }, { 0.8f, 0.2f }, { 0, 1 } };
			anim.enqueue(duration, cue_points, key_frames);
			
			Logger.info("Starting animation...");
			Future<?> future = anim.play();
			try {
				Logger.info("Waiting");
				future.get();
				Logger.info("Finished");
			} catch (CancellationException | ExecutionException | InterruptedException e) {
				Logger.info("Finished {}", e);
			}
			
			DioZeroScheduler.shutdownAll();
		}
	}
}
