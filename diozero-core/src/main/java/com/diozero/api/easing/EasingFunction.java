package com.diozero.api.easing;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Core
 * Filename:     EasingFunction.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
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


/**
 * <p>
 * See <a href="http://easings.net/">easings.net</a> for examples. See also
 * <a href=
 * "http://upshots.org/actionscript/jsas-understanding-easing">Understanding
 * Easing (Explaining Penner?s equations)</a>.
 * </p>
 * <p>
 * Implemented as a functional interface so that implementations can use method
 * references rather than inheritance, makes it easier to refer to ease in, ease
 * out and ease in-out variants.
 * </p>
 */
@FunctionalInterface
public interface EasingFunction {
	/**
	 * @param t
	 *            (time) is the current time (or position) of the tween. This
	 *            can be seconds or frames, steps, seconds, ms, whatever ? as
	 *            long as the unit is the same as is used for the total time
	 *            [3].
	 * @param b
	 *            (begin) is the beginning value of the property.
	 * @param c
	 *            (change) is the change between the beginning and destination
	 *            value of the property.
	 * @param d
	 *            (duration) is the total time of the tween.
	 * @return
	 *            Next value
	 */
	public float ease(float t, float b, float c, float d);
}
