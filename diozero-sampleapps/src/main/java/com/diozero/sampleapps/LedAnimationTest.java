package com.diozero.sampleapps;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.pmw.tinylog.Logger;

import com.diozero.api.Animation;
import com.diozero.api.AnimationObject;
import com.diozero.api.OutputDeviceInterface;
import com.diozero.api.easing.EasingFunction;
import com.diozero.api.easing.EasingFunctions;
import com.diozero.util.DioZeroScheduler;

public class LedAnimationTest {
	public static void main(String[] args) {
		if (args.length < 7) {
			Logger.error("Usage: {} <led1,led2,...> <fps> <easing-function> <speed> <duration> <cue-point1,cue-point2,...> <cp1-led1-val,cp1-led2-val,...;cp2-led1-val,cp2-led2-val,...;...>", LedAnimationTest.class.getName());
			System.exit(1);
		}
		
		int arg = 0;
		
		List<OutputDeviceInterface> leds = new ArrayList<>();
		for (String led : args[arg++].split(",")) {
			Logger.debug("led=" + led);
			//leds.add(new PwmLed(Integer.parseInt(led)));
			leds.add(value -> Logger.debug(led + ": " + Float.valueOf(value)));
		}
		
		int fps = Integer.parseInt(args[arg++]);
		Logger.debug("fps=" + fps);
		EasingFunction easing = EasingFunctions.forName(args[arg++]);
		float speed = Float.parseFloat(args[arg++]);
		Logger.debug("speed=" + speed);
		
		int duration = Integer.parseInt(args[arg++]);
		Logger.debug("duration=" + duration);
		
		List<Float> cue_points_list = new ArrayList<>();
		for (String cue_point : args[arg++].split(",")) {
			cue_points_list.add(Float.valueOf(cue_point));
		}
		float[] cue_points = new float[cue_points_list.size()];
		int i = 0;
		for (Float cue_point : cue_points_list) {
			cue_points[i] = cue_point.floatValue();
			Logger.debug("cue_points[" + i + "]=" + cue_points[i]);
			i++;
		}
		
		List<AnimationObject.KeyFrame[]> key_frames = new ArrayList<>();
		for (String key_frame_val : args[arg++].split(";")) {
			Logger.debug("key_frame_val=" + key_frame_val);
			List<Float> key_frame_list = new ArrayList<>();
			for (String key_frame : key_frame_val.split(",")) {
				Logger.debug("key_frame=" + key_frame);
				key_frame_list.add(Float.valueOf(key_frame));
			}
			AnimationObject.KeyFrame[] key_frame_values = new AnimationObject.KeyFrame[key_frame_list.size()];
			key_frames.add(key_frame_values);
			i = 0;
			for (Float key_frame : key_frame_list) {
				key_frame_values[i++] = new AnimationObject.KeyFrame(key_frame.floatValue());
			}
		}
		
		Animation anim = new Animation(leds, fps, easing, speed);
		anim.enqueue(duration, cue_points, key_frames);
		
		Future<?> future = anim.play();
		try {
			Logger.debug("Waiting");
			future.get();
			Logger.debug("Finished");
		} catch (CancellationException | ExecutionException | InterruptedException e) {
			Logger.debug("Finished {}", e);
		}
		
		DioZeroScheduler.shutdownAll();
	}
}
