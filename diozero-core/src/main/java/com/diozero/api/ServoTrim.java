package com.diozero.api;

import com.diozero.util.RangeUtil;

/**
 * Arduino defaults to a range of 544 to 2400.
 *
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

	// From my testing TowerPro SG90 has a range slightly greater than 180 degree
	// (0.5-2.4)
	private static final int TOWERPRO_SG90_RANGE_US = 1_800;
	public static final ServoTrim TOWERPRO_SG90 = new ServoTrim(DEFAULT_MID_US, DEFAULT_90_DELTA_US,
			TOWERPRO_SG90_RANGE_US);

	// From my testing TowerPro SG5010 has a range slightly greater than 180 degree
	private static final int TOWERPRO_SG5010_RANGE_US = 2_000;
	public static final ServoTrim TOWERPRO_SG5010 = new ServoTrim(DEFAULT_MID_US, DEFAULT_90_DELTA_US,
			TOWERPRO_SG5010_RANGE_US);

	private static final int MG996R_RANGE_US = 2_000;
	public static final ServoTrim MG996R = new ServoTrim(DEFAULT_MID_US, DEFAULT_90_DELTA_US, MG996R_RANGE_US);

	private int midPulseWidthUs;
	private int ninetyDegPulseWidthUs;
	private int minPulseWidthUs;
	private int maxPulseWidthUs;
	private int minAngle;
	private int maxAngle;

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

	public ServoTrim(int midPulseWidthUs, int ninetyDegPulseWidthUs, int rangePulseWidthUs) {
		this(midPulseWidthUs, ninetyDegPulseWidthUs, midPulseWidthUs - rangePulseWidthUs / 2,
				midPulseWidthUs + rangePulseWidthUs / 2);
	}

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

	public int getMinAngle() {
		return minAngle;
	}

	public int getMaxAngle() {
		return maxAngle;
	}

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
}
