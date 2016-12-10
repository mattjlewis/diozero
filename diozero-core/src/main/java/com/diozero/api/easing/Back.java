package com.diozero.api.easing;

public class Back {
	private static final float DEFAULT_SPEED = 1.70158f;
	
	public static float easeIn(float t, float b, float c, float d) {
		return easeIn(t, b, c, d, DEFAULT_SPEED);
	}

	public static float easeIn(float t, float b, float c, float d, float s) {
		return c * (t /= d) * t * ((s + 1) * t - s) + b;
	}

	public static float easeOut(float t, float b, float c, float d) {
		return easeOut(t, b, c, d, DEFAULT_SPEED);
	}

	public static float easeOut(float t, float b, float c, float d, float s) {
		return c * ((t = t / d - 1) * t * ((s + 1) * t + s) + 1) + b;
	}

	public static float easeInOut(float t, float b, float c, float d) {
		return easeInOut(t, b, c, d, DEFAULT_SPEED);
	}

	public static float easeInOut(float t, float b, float c, float d, float s) {
		if ((t /= d / 2) < 1) {
			return c / 2 * (t * t * (((s *= (1.525f)) + 1) * t - s)) + b;
		}
		return c / 2 * ((t -= 2) * t * (((s *= (1.525f)) + 1) * t + s) + 2) + b;
	}
}
