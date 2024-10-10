package com.diozero.api;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     ServoTrim.java
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

import com.diozero.util.RangeUtil;

/**
 * Arduino defaults to a range of 544 to 2400.
 * <p>
 * 1 to 2 ms is the minimum pulse range used in the R/C radio control industry.
 * The maximum pulse range is a servo specific value that you must determine by
 * either manufacture's datasheet information or by testing of the specific
 * servo you are using.
 */
public class ServoTrim {
	public static final int MID_ANGLE = 90;

	public static final int DEFAULT_MID_US = 1_500;
	public static final int DEFAULT_90_DELTA_US = 900;
	/** Default to 180 degree range, from 0.6ms to 2.4ms with 1.5ms centre */
	public static final ServoTrim DEFAULT = new ServoTrim(DEFAULT_MID_US, DEFAULT_90_DELTA_US);

	// A TowerPro SG90 is right around 180 degrees (0.5-2.4)
	private static final int TOWERPRO_SG90_90_US = 950; // (2400 - 500) / 2
	private static final int TOWERPRO_SG90_CENTRE_US = 1450; // 500 + 950
	public static final ServoTrim TOWERPRO_SG90 = new ServoTrim(TOWERPRO_SG90_CENTRE_US, TOWERPRO_SG90_90_US);

	// A TowerPro SG5010 can have a range slightly greater than 180 degrees
	private static final int TOWERPRO_SG5010_RANGE_US = 2_000;
	public static final ServoTrim TOWERPRO_SG5010 = new ServoTrim(DEFAULT_MID_US, DEFAULT_90_DELTA_US,
			TOWERPRO_SG5010_RANGE_US);

	private static final int MG996R_RANGE_US = 2_000;
	public static final ServoTrim MG996R = new ServoTrim(DEFAULT_MID_US, DEFAULT_90_DELTA_US, MG996R_RANGE_US);

	// Metal-gear micro servo ("size-compatible" with an SG90) but with an extended range
	public static final ServoTrim MG90S = new ServoTrim(400,2600,200L);

	private final int midPulseWidthUs;
	private final int ninetyDegPulseWidthUs;
	private final int minPulseWidthUs;
	private final int maxPulseWidthUs;
	private final int minAngle;
	private final int maxAngle;

	/**
	 * Assumes 180 degree range of movement
	 *
	 * @param midPulseWidthUs       Pulse width in microseconds corresponding to the
	 *                              centre position (90 degrees)
	 * @param ninetyDegPulseWidthUs Pulse width in microseconds corresponding to a
	 *                              90 degree movement in either direction
	 */
	public ServoTrim(int midPulseWidthUs, int ninetyDegPulseWidthUs) {
		this(midPulseWidthUs, ninetyDegPulseWidthUs, 2 * ninetyDegPulseWidthUs);
	}

	/**
	 * Assumes 180 degree range of movement
	 *
	 * @param midPulseWidthUs       Pulse width in microseconds corresponding to the
	 *                              centre position (90 degrees)
	 * @param ninetyDegPulseWidthUs Pulse width in microseconds corresponding to a
	 *                              90 degree movement in either direction
	 * @param rangePulseWidthUs     Pulse width in microseconds corresponding to the range
	 *                              of motion in either direction (this is subtracted from
	 *                              the 90 degree value
	 */
	public ServoTrim(int midPulseWidthUs, int ninetyDegPulseWidthUs, int rangePulseWidthUs) {
		this(midPulseWidthUs, ninetyDegPulseWidthUs, midPulseWidthUs - rangePulseWidthUs / 2,
				midPulseWidthUs + rangePulseWidthUs / 2);
	}

    /**
     * Assumes 180 degree range of movement.
     * <p>
     * Note: if the min/max pulse widths exceed the
     * mid-point +/- the 90 degree mark, value for min/max angles <b>may</b> be out of the
     * 0-180 range.
     * </p>
     *
     * @param midPulseWidthUs       Pulse width in microseconds corresponding to the
     *                              centre position (90 degrees)
     * @param ninetyDegPulseWidthUs Pulse width in microseconds corresponding to a
     *                              90 degree movement in either direction
     * @param minPulseWidthUs       Pulse width in microseconds corresponding to the minimum
     *                              rotation location (see description)
     * @param maxPulseWidthUs       Pulse width in microseconds corresponding to the maximum
     *                              rotation location (see description)
	 * @see ServoTrim#MG996R
     */
	public ServoTrim(int midPulseWidthUs, int ninetyDegPulseWidthUs, int minPulseWidthUs, int maxPulseWidthUs) {
		this.midPulseWidthUs = midPulseWidthUs;
		this.ninetyDegPulseWidthUs = ninetyDegPulseWidthUs;
		this.minPulseWidthUs = minPulseWidthUs;
		this.maxPulseWidthUs = maxPulseWidthUs;
		minAngle = RangeUtil.map(minPulseWidthUs, midPulseWidthUs - ninetyDegPulseWidthUs,
				midPulseWidthUs + ninetyDegPulseWidthUs, 0, 180, false);
		maxAngle = RangeUtil.map(maxPulseWidthUs, midPulseWidthUs - ninetyDegPulseWidthUs,
				midPulseWidthUs + ninetyDegPulseWidthUs, 0, 180, false);
	}

	/**
	 * Assumes minimum value is assigned to 0 degrees and the maximum value is associated with the full arc.
	 * <p>
	 * Note: the arc parameter is defined as a <b>long</b> to avoid conflicts with the other constructor(s).
	 * </p>
	 *
	 * @param minPulseWidthUs Pulse width in microseconds corresponding to the minimum
	 *                        rotation location, or 0 degrees
	 * @param maxPulseWidthUs Pulse width in microseconds corresponding to the maximum
	 *                        rotation location, aka {@code arcDegrees}
	 * @param arcDegrees      How far the servo rotates when {@code maxPulseWidthUs} is applied.
	 */
	public ServoTrim(int minPulseWidthUs, int maxPulseWidthUs, long arcDegrees) {
		minAngle = 0;
		maxAngle = (int)arcDegrees;
		this.minPulseWidthUs = minPulseWidthUs;
		this.maxPulseWidthUs = maxPulseWidthUs;

		// calculate everything else
		int pulseRange = maxPulseWidthUs - minPulseWidthUs;
		this.midPulseWidthUs = Math.round(pulseRange / 2f) + minPulseWidthUs;

		float ninetyLocation = 90f / arcDegrees;
		this.ninetyDegPulseWidthUs = Math.round(ninetyLocation * pulseRange) + minPulseWidthUs;
	}

	public int getMidPulseWidthUs() {
		return midPulseWidthUs;
	}

	public float getMidPulseWidthMs() {
		return midPulseWidthUs / 1_000f;
	}

	public int getNinetyDegPulseWidthUs() {
		return ninetyDegPulseWidthUs;
	}

	public float getNinetyDegPulseWidthMs() {
		return ninetyDegPulseWidthUs / 1_000f;
	}

	public int getMinPulseWidthUs() {
		return minPulseWidthUs;
	}

	public float getMinPulseWidthMs() {
		return minPulseWidthUs / 1_000f;
	}

	public int getMaxPulseWidthUs() {
		return maxPulseWidthUs;
	}

	public float getMaxPulseWidthMs() {
		return maxPulseWidthUs / 1_000f;
	}

	/**
	 * Get the servo minimum angle in degrees where 90 degrees is the middle angle
	 *
	 * @return the servo minimum angle (90 degrees is central)
	 */
	public int getMinAngle() {
		return minAngle;
	}

	/**
	 * Get the servo maximum angle in degrees where 90 degrees is the middle angle
	 *
	 * @return the servo maximum angle (90 degrees is central)
	 */
	public int getMaxAngle() {
		return maxAngle;
	}

	/**
	 * Get the servo middle angle in degrees (constant - 90 degrees)
	 *
	 * @return the servo middle angle (a constant of 90 degrees)
	 */
	@SuppressWarnings("static-method")
	public int getMidAngle() {
		return MID_ANGLE;
	}

	public int convertPulseWidthUsToAngle(int pulseWidthUs) {
		return RangeUtil.map(pulseWidthUs, minPulseWidthUs, maxPulseWidthUs, minAngle, maxAngle);
	}

	public int convertAngleToPulseWidthUs(int angle) {
		return RangeUtil.map(angle, minAngle, maxAngle, minPulseWidthUs, maxPulseWidthUs);
	}

	public int convertPulseWidthMsToAngle(float pulseWidthMs) {
		return convertPulseWidthUsToAngle((int) (pulseWidthMs * 1_000));
	}

	public float convertAngleToPulseWidthMs(int angle) {
		return convertAngleToPulseWidthUs(angle) / 1_000f;
	}

	public int convertValueToPulseWidthUs(float value) {
		return RangeUtil.map(value, -1f, 1f, minPulseWidthUs, maxPulseWidthUs);
	}

	public float convertPulseWidthUsToValue(int pulseWidthUs) {
		return RangeUtil.map(pulseWidthUs, minPulseWidthUs, maxPulseWidthUs, -1f, 1f);
	}
}
