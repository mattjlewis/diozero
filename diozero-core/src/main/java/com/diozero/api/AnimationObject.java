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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.diozero.api.easing.EasingFunction;
import com.diozero.api.easing.EasingFunctions;

public class AnimationObject {
	private int duration;
	private float[] cuePoints;
	private List<KeyFrame[]> keyFrames;
	
	public int getDuration() {
		return duration;
	}
	
	public float[] getCuePoints() {
		return cuePoints;
	}
	
	public List<KeyFrame[]> getKeyFrames() {
		return keyFrames;
	}

	@Override
	public String toString() {
		return "AnimationObject [duration=" + duration + ", cuePoints=" + Arrays.toString(cuePoints) + ", keyFrames="
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
}
