package com.diozero.util;

public class StringUtil {
	public static String pad(String str, int length) {
		return String.format("%1$-" + length + "s", str).substring(0, length);
	}
}
