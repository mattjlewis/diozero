package com.diozero.animation;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     AnimationInstance.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2023 diozero
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.tinylog.Logger;

import com.diozero.animation.easing.EasingFunction;
import com.diozero.animation.easing.EasingFunctions;
import com.diozero.api.function.FloatConsumer;

public class AnimationInstance {
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
	private List<AnimationInstance.KeyFrame[]> keyFrames;
	private List<List<float[]>> segmentValues;

	public AnimationInstance() {
	}

	public AnimationInstance(int durationMillis, float[] cuePoints, List<KeyFrame[]> keyFrames) {
		this.durationMillis = durationMillis;
		this.cuePoints = cuePoints;
		this.keyFrames = keyFrames;
	}

	void prepare(Animation animation) {
		// Prepare the key frame values using delta if set
		KeyFrame[] prev_kf_array = null;
		for (KeyFrame[] key_frame_array : keyFrames) {
			if (prev_kf_array != null) {
				for (int tgt = 0; tgt < key_frame_array.length; tgt++) {
					Float delta = key_frame_array[tgt].getDelta();
					if (delta != null) {
						key_frame_array[tgt].setValue(prev_kf_array[tgt].getValue() + delta.floatValue());
					}
				}
			}
			prev_kf_array = key_frame_array;
		}

		// Find our timeline endpoints and refresh rate
		float scaled_duration = durationMillis / Math.abs(animation.getSpeed());

		if (cuePoints.length == 0 || cuePoints.length != keyFrames.size()) {
			throw new IllegalArgumentException("cuePoints length (" + cuePoints.length
					+ ") must equal keyFrames length (" + keyFrames.size() + ")");
		}
		Collection<FloatConsumer> targets = animation.getTargets();
		if (keyFrames.size() == 0 || keyFrames.get(0).length != targets.size()) {
			throw new IllegalArgumentException("keyFrames length (" + keyFrames.get(0).length
					+ ") must equal number of targets (" + targets.size() + ")");
		}

		cuePointsMillis = new int[cuePoints.length];
		int segment = 0;
		for (float cue_point : cuePoints) {
			cuePointsMillis[segment++] = Math.round(cue_point * scaled_duration);
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
					EasingFunction kf_ef = keyFrames.get(segment - 1)[tgt].getEasingFunction();
					EasingFunction easing_func = kf_ef == null ? animation.getEasingFunction() : kf_ef;

					AnimationInstance.KeyFrame begin = keyFrames.get(segment - 1)[tgt];
					float delta = keyFrames.get(segment)[tgt].getValue() - begin.getValue();
					tgt_values[tgt] = easing_func.ease(time, begin.getValue(), delta, seg_duration);
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

	public int getDurationMillis() {
		return durationMillis;
	}

	public float[] getCuePoints() {
		return cuePoints;
	}

	public List<KeyFrame[]> getKeyFrames() {
		return keyFrames;
	}

	public List<List<float[]>> getSegmentValues() {
		return segmentValues;
	}

	@Override
	public String toString() {
		return "AnimationInstance [duration=" + durationMillis + ", cuePoints=" + Arrays.toString(cuePoints)
				+ ", keyFrames=" + keyFrames + "]";
	}

	public static class KeyFrame {
		private float value;
		private Float delta;
		private boolean change;
		private String easing;

		public KeyFrame() {
		}

		public KeyFrame(float value) {
			setValue(value);
		}

		public KeyFrame(boolean change) {
			setChange(change);
		}

		public float getValue() {
			return value;
		}

		public float getValue(float previousValue) {
			if (!change) {
				return previousValue;
			}
			if (delta == null) {
				return value;
			}
			return value + delta.floatValue();
		}

		public KeyFrame setValue(float value) {
			change = true;
			this.value = value;

			return this;
		}

		public Float getDelta() {
			return delta;
		}

		public KeyFrame setDelta(float delta) {
			change = true;
			this.delta = Float.valueOf(delta);

			return this;
		}

		public boolean isChange() {
			return change;
		}

		public KeyFrame setChange(boolean change) {
			this.change = change;

			return this;
		}

		public String getEasing() {
			return easing;
		}

		public KeyFrame setEasing(String easing) {
			this.easing = easing;

			return this;
		}

		public EasingFunction getEasingFunction() {
			return EasingFunctions.forName(easing);
		}

		@Override
		public String toString() {
			return "KeyFrame [value=" + value + ", delta=" + delta + ", change=" + change + ", easing=" + easing + "]";
		}

		public static List<KeyFrame[]> fromValues(float[][] values) {
			List<KeyFrame[]> key_frames = new ArrayList<>();
			for (float[] kf_values : values) {
				KeyFrame[] kf_array = new KeyFrame[kf_values.length];
				for (int i = 0; i < kf_values.length; i++) {
					kf_array[i] = new KeyFrame();
					kf_array[i].setValue(kf_values[i]);
				}
				key_frames.add(kf_array);
			}
			return key_frames;
		}
	}
}
