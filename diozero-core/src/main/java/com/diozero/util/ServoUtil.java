package com.diozero.util;

public class ServoUtil {
	public static double calcPulseMsPerBit(int pwmFrequency, int range) {
		return 1000.0/pwmFrequency/range;
	}

	public static int calcServoPulse(double pulseWidthMs, double pulseMsPerBit) {
		return (int)Math.round(pulseWidthMs / pulseMsPerBit);
	}

	public static int calcServoPulse(double pulseWidthMs, int pwmFrequency, int range) {
		return (int)Math.round(pulseWidthMs / calcPulseMsPerBit(pwmFrequency, range));
	}
}
