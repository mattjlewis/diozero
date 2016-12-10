package com.diozero.sandpit;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.pmw.tinylog.Logger;

import com.diozero.api.OutputDeviceInterface;
import com.diozero.api.easing.Elastic;
import com.diozero.util.DioZeroScheduler;

public class AnimationTest {
	public static void main(String[] args) {
		OutputDeviceInterface con1 = (f) -> System.out.println(f);
		//AnimationTest test = new AnimationTest();
		List<OutputDeviceInterface> targets = Arrays.asList(
				con1/*, test::setMyValue, (f) -> Logger.info("con2 {}", Float.valueOf(f))*/);
		
		Animation anim = new Animation(targets, 100, Elastic::easeOut, 1);
		anim.setLoop(true);
		
		// How long the animation is
		int duration = 2000;
		// Relative time points (0..1)
		float[] cue_points = { 0, 0.2f, 0.8f, 1 };
		// Value for each target at the corresponding cue points
		//float[][] key_frames = { { 1, 5, 10 }, { 2, 4, 9 }, { 3, 6, 8 } };
		float[][] key_frames = { { 1 }, { 2 }, { 3 }, { 1 } };
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
		}
		
		DioZeroScheduler.shutdownAll();
	}
	
	public void setMyValue(float f) {
		Logger.info("setMyValue({})", Float.valueOf(f));
	}
}
