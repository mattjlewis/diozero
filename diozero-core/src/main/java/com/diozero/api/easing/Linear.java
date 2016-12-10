package com.diozero.api.easing;

public class Linear {
	public static float ease(float t, float b, float c, float d) {
		return c * t / d + b;
	}
}
