package com.diozero.api;

/*
 * #%L
 * Device I/O Zero - Core
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
import java.util.*;

import org.pmw.tinylog.Logger;

import com.diozero.api.easing.EasingFunction;
import com.diozero.api.easing.EasingFunctions;

public class AnimationObject {
	private int durationMillis;
	/**
	 * Array of values from 0.0 to 1.0 representing the beginning and end of the
	 * animation respectively (default [0, 1]);
	 */
	private float[] cuePoints;
	private int[] cuePointsMillis;
	/**
	 * A 1 or 2 dimensional array of device positions over time. See more on
	 * keyFrames below. (required)
	 */
	private List<AnimationObject.KeyFrame[]> keyFrames;
	private List<List<float[]>> segmentValues;
	
	public AnimationObject(int durationMillis, float[] cuePoints, List<KeyFrame[]> keyFrames) {
		this.durationMillis = durationMillis;
		this.cuePoints = cuePoints;
		this.keyFrames = keyFrames;
	}

	void prepare(Animation animation) {
		// Find our timeline endpoints and refresh rate
		float scaled_duration = durationMillis / Math.abs(animation.getSpeed());

		if (cuePoints.length == 0 || cuePoints.length != keyFrames.size()) {
			throw new IllegalArgumentException("cuePoints length (" + cuePoints.length + ") must equal keyFrames length (" + keyFrames.size() + ")");
		}
		Collection<OutputDeviceInterface> targets = animation.getTargets();
		if (keyFrames.size() == 0 || keyFrames.get(0).length != targets.size()) {
			throw new IllegalArgumentException("keyFrames length (" + keyFrames.get(0).length
					+ ") must equal number of targets (" + targets.size() + ")");
		}

		cuePointsMillis = new int[cuePoints.length];
		int segment = 0;
		for (float cue_point : cuePoints) {
			cuePointsMillis[segment++] = (int) (cue_point * scaled_duration);
		}

		// List of segments, each segment is a list of steps with a value for
		// each target
		// Java generics make it very difficult to have arrays of Collections
		segmentValues = new ArrayList<>(cuePoints.length - 1);

		// Setup first step from key frame start values
		List<float[]> step_values = new ArrayList<>();
		segmentValues.add(step_values);
		float[] tgt_values = new float[targets.size()];
		step_values.add(tgt_values);
		for (int tgt = 0; tgt < targets.size(); tgt++) {
			tgt_values[tgt] = keyFrames.get(0)[tgt].getValue();
		}

		for (segment = 1; segment < cuePoints.length; segment++) {
			Logger.info("Segment={}", Integer.valueOf(segment));
			long time = 0;
			time += animation.getPeriodMs();
			long seg_duration = cuePointsMillis[segment] - cuePointsMillis[segment - 1];
			while (time < seg_duration) {
				// For each target
				tgt_values = new float[targets.size()];
				step_values.add(tgt_values);
				for (int tgt = 0; tgt < targets.size(); tgt++) {
					EasingFunction kf_ef = keyFrames.get(segment-1)[tgt].getEasingFunction();
					EasingFunction easing_func = kf_ef == null ? animation.getEasingFunction() : kf_ef;
					
					AnimationObject.KeyFrame begin = keyFrames.get(segment - 1)[tgt];
					float change = keyFrames.get(segment)[tgt].getValue() - keyFrames.get(segment - 1)[tgt].getValue();
					tgt_values[tgt] = easing_func.ease(time, begin.getValue(), change, seg_duration);
				}
				time += animation.getPeriodMs();
			}

			// Add the segment finish state
			tgt_values = new float[targets.size()];
			step_values.add(tgt_values);
			for (int tgt = 0; tgt < targets.size(); tgt++) {
				tgt_values[tgt] = keyFrames.get(segment)[tgt].getValue();
			}

			if (segment < cuePoints.length - 1) {
				step_values = new ArrayList<>();
				segmentValues.add(step_values);
				Logger.info("Added to segmentValues, size {}", Integer.valueOf(segmentValues.size()));
			}
		}
	}
	
	public int getDuration() {
		return durationMillis;
	}
	
	public float[] getCuePoints() {
		return cuePoints;
	}
	
	public List<KeyFrame[]> getKeyFrames() {
		return keyFrames;
	}

	@Override
	public String toString() {
		return "AnimationObject [duration=" + durationMillis + ", cuePoints=" + Arrays.toString(cuePoints) + ", keyFrames="
				+ keyFrames + "]";
	}

	public static class KeyFrame {
		private float value;
		private float delta;
		private String easing;
		
		public KeyFrame() {
		}
		
		public KeyFrame(float value) {
			this.value = value;
		}
		
		public float getValue() {
			return value;
		}
		
		public float getDelta() {
			return delta;
		}
		
		public String getEasing() {
			return easing;
		}
		
		public EasingFunction getEasingFunction() {
			return EasingFunctions.forName(easing);
		}

		@Override
		public String toString() {
			return "KeyFrame [value=" + value + ", delta=" + delta + "]";
		}
		
		public static List<KeyFrame[]> from(float[][] values) {
			List<KeyFrame[]> key_frames = new ArrayList<>();
			for (float[] kf_values : values) {
				KeyFrame[] kf_array = new KeyFrame[kf_values.length];
				for (int i=0; i<kf_values.length; i++) {
					kf_array[i] = new KeyFrame(kf_values[i]);
				}
				key_frames.add(kf_array);
			}
			return key_frames;
		}
	}

	public List<List<float[]>> getSegmentValues() {
		return segmentValues;
	}
}
