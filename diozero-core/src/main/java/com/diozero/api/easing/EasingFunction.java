package com.diozero.api.easing;

/**
 * <p>
 * See <a href="http://easings.net/">easings.net</a> for examples. See also
 * <a href=
 * "http://upshots.org/actionscript/jsas-understanding-easing">Understanding
 * Easing (Explaining Penner’s equations)</a>.
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
	 *            can be seconds or frames, steps, seconds, ms, whatever – as
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
