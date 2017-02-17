package com.diozero.api.easing;

import java.util.HashMap;
import java.util.Map;

public class EasingFunctions {
	private static Map<String, EasingFunction> EASING_FUNCTIONS;
	static {
		EASING_FUNCTIONS = new HashMap<>();
		
		EASING_FUNCTIONS.put(Linear.LINEAR, Linear::ease);
		EASING_FUNCTIONS.put(Quad.IN, Quad::easeIn);
		EASING_FUNCTIONS.put(Quad.OUT, Quad::easeOut);
		EASING_FUNCTIONS.put(Quad.IN_OUT, Quad::easeInOut);
		EASING_FUNCTIONS.put(Cubic.IN, Cubic::easeIn);
		EASING_FUNCTIONS.put(Cubic.OUT, Cubic::easeOut);
		EASING_FUNCTIONS.put(Cubic.IN_OUT, Cubic::easeInOut);
		EASING_FUNCTIONS.put(Quart.IN, Quart::easeIn);
		EASING_FUNCTIONS.put(Quart.OUT, Quart::easeOut);
		EASING_FUNCTIONS.put(Quart.IN_OUT, Quart::easeInOut);
		EASING_FUNCTIONS.put(Quintic.IN, Quintic::easeIn);
		EASING_FUNCTIONS.put(Quintic.OUT, Quintic::easeOut);
		EASING_FUNCTIONS.put(Quintic.IN_OUT, Quintic::easeInOut);
		EASING_FUNCTIONS.put(Sine.IN, Sine::easeIn);
		EASING_FUNCTIONS.put(Sine.OUT, Sine::easeOut);
		EASING_FUNCTIONS.put(Sine.IN_OUT, Sine::easeInOut);
		EASING_FUNCTIONS.put(Exponential.IN, Exponential::easeIn);
		EASING_FUNCTIONS.put(Exponential.OUT, Exponential::easeOut);
		EASING_FUNCTIONS.put(Exponential.IN_OUT, Exponential::easeInOut);
		EASING_FUNCTIONS.put(Circular.IN, Circular::easeIn);
		EASING_FUNCTIONS.put(Circular.OUT, Circular::easeOut);
		EASING_FUNCTIONS.put(Circular.IN_OUT, Circular::easeInOut);
		EASING_FUNCTIONS.put(Back.IN, Back::easeIn);
		EASING_FUNCTIONS.put(Back.OUT, Back::easeOut);
		EASING_FUNCTIONS.put(Back.IN_OUT, Back::easeInOut);
		EASING_FUNCTIONS.put(Bounce.IN, Bounce::easeIn);
		EASING_FUNCTIONS.put(Bounce.OUT, Bounce::easeOut);
		EASING_FUNCTIONS.put(Bounce.IN_OUT, Bounce::easeInOut);
		EASING_FUNCTIONS.put(Elastic.IN, Elastic::easeIn);
		EASING_FUNCTIONS.put(Elastic.OUT, Elastic::easeOut);
		EASING_FUNCTIONS.put(Elastic.IN_OUT, Elastic::easeInOut);
	}
	
	public static EasingFunction forName(String name) {
		return EASING_FUNCTIONS.get(name);
	}
}
