package com.diozero.animation.easing;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     Back.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2022 diozero
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


public class Back {
	private static final float DEFAULT_SPEED = 1.70158f;
	
	public static final String IN = "inBack";
	public static float easeIn(float t, float b, float c, float d) {
		return easeIn(t, b, c, d, DEFAULT_SPEED);
	}

	public static float easeIn(float t, float b, float c, float d, float s) {
		return c * (t /= d) * t * ((s + 1) * t - s) + b;
	}

	public static final String OUT = "outBack";
	public static float easeOut(float t, float b, float c, float d) {
		return easeOut(t, b, c, d, DEFAULT_SPEED);
	}

	public static float easeOut(float t, float b, float c, float d, float s) {
		return c * ((t = t / d - 1) * t * ((s + 1) * t + s) + 1) + b;
	}

	public static final String IN_OUT = "inOutBack";
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
