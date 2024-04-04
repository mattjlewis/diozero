package com.diozero.util;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     RangeUtil.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2024 diozero
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

public class RangeUtil {
	public static int constrain(int value, int min, int max) {
		return Math.min(max, Math.max(value, min));
	}

	public static float constrain(float value, float min, float max) {
		return Math.min(max, Math.max(value, min));
	}

	public static double constrain(double value, double min, double max) {
		return Math.min(max, Math.max(value, min));
	}

	/**
	 * Map a number from one range to another and constrain the resulting value to be within
	 * the target range. Assumes both ranges start at 0. Based on Arduino's map(). Example:
	 * <code>RangeUtil.map(500, 1000, 256);</code> returns 128.
	 *
	 * @param value    value to map
	 * @param fromHigh high end of originating range
	 * @param toHigh   high end of target range
	 * @return mapped value
	 */
	public static int map(int value, int fromHigh, int toHigh) {
		return map(value, 0, fromHigh, 0, toHigh, true);
	}

	/**
	 * Map a number from one range to another. Assumes both ranges start at 0. Based on
	 * Arduino's map(). Example: <code>RangeUtil.map(1500, 1000, 256, false);</code> returns
	 * 384 (128+256).
	 *
	 * @param value     value to map
	 * @param fromHigh  high end of originating range
	 * @param toHigh    high end of target range
	 * @param constrain whether to constrain the returned value to the specified range
	 * @return mapped value
	 */
	public static int map(int value, int fromHigh, int toHigh, boolean constrain) {
		return map(value, 0, fromHigh, 0, toHigh, constrain);
	}

	/**
	 * Map a number from one range to another and constrain the resulting value to be within
	 * the target range. Based on Arduino's map(). Example:
	 * <code>RangeUtil.map(0, -1000, 1000, 0, 256);</code> returns 128.
	 *
	 * @param value    value to map
	 * @param fromLow  low end of originating range
	 * @param fromHigh high end of originating range
	 * @param toLow    low end of target range
	 * @param toHigh   high end of target range
	 * @return mapped value
	 */
	public static int map(int value, int fromLow, int fromHigh, int toLow, int toHigh) {
		return map(value, fromLow, fromHigh, toLow, toHigh, true);
	}

	/**
	 * Map a number from one range to another and constrain the resulting value to be within
	 * the target range. Assumes both ranges start at 0. Based on Arduino's map(). Example:
	 * <code>RangeUtil.map(0.5f, 1f, 256);</code> returns 128.
	 *
	 * @param value    value to map
	 * @param fromHigh high end of originating range
	 * @param toHigh   high end of target range
	 * @return mapped value
	 */
	public static int map(float value, float fromHigh, int toHigh) {
		return map(value, 0, fromHigh, 0, toHigh, true);
	}

	/**
	 * Map a number from one range to another and constrain the resulting value to be within
	 * the target range. Based on Arduino's map(). Example:
	 * <code>RangeUtil.map(1.5f, 1f, 2f, 0, 256);</code> returns 128.
	 *
	 * @param value    value to map
	 * @param fromLow  low end of originating range
	 * @param fromHigh high end of originating range
	 * @param toLow    low end of target range
	 * @param toHigh   high end of target range
	 * @return mapped value
	 */
	public static int map(float value, float fromLow, float fromHigh, int toLow, int toHigh) {
		return map(value, fromLow, fromHigh, toLow, toHigh, true);
	}

	/**
	 * Map a number from one range to another. Based on Arduino's map(). Example:
	 * <code>RangeUtil.map(0.5f, 1f, 2f, 0, 256, false);</code> returns -128.
	 *
	 * @param value     value to map
	 * @param fromLow   low end of originating range
	 * @param fromHigh  high end of originating range
	 * @param toLow     low end of target range
	 * @param toHigh    high end of target range
	 * @param constrain whether to constrain the returned value to the specified range
	 * @return mapped value
	 */
	public static int map(float value, float fromLow, float fromHigh, int toLow, int toHigh, boolean constrain) {
		int result = Math.round((value - fromLow) * (toHigh - toLow) / (fromHigh - fromLow) + toLow);
		if (constrain) {
			result = constrain(result, toLow, toHigh);
		}
		return result;
	}

	/**
	 * Map a number from one range to another and constrain the resulting value to be within
	 * the target range. Assumes both ranges start at zero. Based on Arduino's map(). Example:
	 * <code>RangeUtil.map(0.5f, 1f, 256f);</code> returns 128f.
	 *
	 * @param value    value to map
	 * @param fromHigh high end of originating range
	 * @param toHigh   high end of target range
	 * @return mapped value
	 */
	public static float map(float value, float fromHigh, float toHigh) {
		return map(value, 0, fromHigh, 0, toHigh, true);
	}

	/**
	 * Map a number from one range to another and constrain the resulting value to be within
	 * the target range. Based on Arduino's map(). Example:
	 * <code>RangeUtil.map(1.5f, 1f, 2f, 0f, 256f);</code> returns 128f.
	 *
	 * @param value    value to map
	 * @param fromLow  low end of originating range
	 * @param fromHigh high end of originating range
	 * @param toLow    low end of target range
	 * @param toHigh   high end of target range
	 * @return mapped value
	 */
	public static float map(float value, float fromLow, float fromHigh, float toLow, float toHigh) {
		return map(value, fromLow, fromHigh, toLow, toHigh, true);
	}

	/**
	 * Map a number from one range to another. Based on Arduino's map(). Example:
	 * <code>RangeUtil.map(500, 0, 1000, 0, 256);</code> returns 128.
	 *
	 * @param value     value to map
	 * @param fromLow   low end of originating range
	 * @param fromHigh  high end of originating range
	 * @param toLow     low end of target range
	 * @param toHigh    high end of target range
	 * @param constrain whether to constrain the returned value to the specified range
	 * @return mapped value
	 */
	public static float map(float value, float fromLow, float fromHigh, float toLow, float toHigh, boolean constrain) {
		float result = (value - fromLow) * (toHigh - toLow) / (fromHigh - fromLow) + toLow;
		if (constrain) {
			result = constrain(result, toLow, toHigh);
		}
		return result;
	}

	/**
	 * Map a number from one range to another and constrain the resulting value to be within
	 * the target range. Assumes both ranges start at zero. Based on Arduino's map(). Example:
	 * <code>RangeUtil.map(500, 1000, 256);</code> returns 128
	 *
	 * @param value    value to map
	 * @param fromHigh high end of originating range
	 * @param toHigh   high end of target range
	 * @return mapped value
	 */
	public static double map(double value, double fromHigh, double toHigh) {
		return map(value, 0, fromHigh, 0, toHigh, true);
	}

	/**
	 * Map a number from one range to another and constrain the resulting value to be within
	 * the target range. Based on Arduino's map(). Example:
	 * <code>RangeUtil.map(500, 0, 1000, 0, 256);</code> returns 128.
	 *
	 * @param value    value to map
	 * @param fromLow  low end of originating range
	 * @param fromHigh high end of originating range
	 * @param toLow    low end of target range
	 * @param toHigh   high end of target range
	 * @return mapped value
	 */
	public static double map(double value, double fromLow, double fromHigh, double toLow, double toHigh) {
		return map(value, fromLow, fromHigh, toLow, toHigh, true);
	}

	/**
	 * Map a number from one range to another. Based on Arduino's map(). Example:
	 * <code>RangeUtil.map(500, 0, 1000, 0, 256);</code> returns 128.
	 *
	 * @param value     value to map
	 * @param fromLow   low end of originating range
	 * @param fromHigh  high end of originating range
	 * @param toLow     low end of target range
	 * @param toHigh    high end of target range
	 * @param constrain whether to constrain the returned value to the specified range
	 * @return mapped value
	 */
	public static double map(double value, double fromLow, double fromHigh, double toLow, double toHigh,
			boolean constrain) {
		double result = (value - fromLow) * (toHigh - toLow) / (fromHigh - fromLow) + toLow;
		if (constrain) {
			result = constrain(result, toLow, toHigh);
		}
		return result;
	}

	public static int resolutionEndInclusive(int numBits) {
		return 1 << numBits;
	}

	public static int resolutionEndExclusive(int numBits) {
		return resolutionEndInclusive(numBits) - 1;
	}
}
