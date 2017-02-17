package com.diozero.sandpit;

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
