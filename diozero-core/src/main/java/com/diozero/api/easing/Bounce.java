package com.diozero.api.easing;

public class Bounce {
	public static float easeIn(float t, float b, float c, float d) {
		return c - easeOut(d - t, 0, c, d) + b;
	}

	public static float easeOut(float t, float b, float c, float d) {
		if ((t /= d) < (1 / 2.75f)) {
			return c * (7.5625f * t * t) + b;
		}
		if (t < (2 / 2.75f)) {
			return c * (7.5625f * (t -= (1.5f / 2.75f)) * t + .75f) + b;
		}
		if (t < (2.5 / 2.75)) {
			return c * (7.5625f * (t -= (2.25f / 2.75f)) * t + .9375f) + b;
		}
		return c * (7.5625f * (t -= (2.625f / 2.75f)) * t + .984375f) + b;
	}

	public static float easeInOut(float t, float b, float c, float d) {
		if (t < d / 2) {
			return easeIn(t * 2, 0, c, d) * .5f + b;
		}
		return easeOut(t * 2 - d, 0, c, d) * .5f + c * .5f + b;
	}

}