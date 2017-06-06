package com.diozero.util;

public class Hex {
	private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
	private static final char BUNDLE_SEP = ' ';

	public static String encodeHexString(byte[] bytes) {
		return encodeHexString(bytes, 0);
	}
	
	public static String encodeHexString(byte[] bytes, int bundleSize) {
		char[] hexChars = new char[(bytes.length * 2) + (bundleSize > 0 ? (bytes.length / bundleSize) : 0)];
		for (int j = 0, k = 1; j < bytes.length; j++, k++) {
			int v = bytes[j] & 0xFF;
			int start = (j * 2) + (bundleSize > 0 ? j / bundleSize : 0);

			hexChars[start] = HEX_ARRAY[v >>> 4];
			hexChars[start + 1] = HEX_ARRAY[v & 0x0F];

			if (bundleSize > 0 && (k % bundleSize) == 0) {
				hexChars[start + 2] = BUNDLE_SEP;
			}
		}
		return new String(hexChars).trim();
	}
	
	public static byte[] decodeHex(CharSequence s) {
		int len = s.length();
		if ((len & 0x01) != 0) {
			throw new IllegalArgumentException("Odd number of characters.");
		}

		byte[] out = new byte[len >> 1];
		for (int i = 0, j = 0; j < len; i++) {
			int f = (toDigit(s.charAt(j++)) << 4) | toDigit(s.charAt(j++));
			out[i] = (byte) (f & 0xFF);
		}
		
		return out;
	}
	
	private static int toDigit(char ch) {
		int digit = Character.digit(ch,  16);
		if (digit == -1) {
			throw new IllegalArgumentException("Invalid hex character '" + ch + "'");
		}
		return digit;
	}
	
	public static void main(String[] args) {
		byte[] bytes = { (byte) 0xde, (byte) 0xad, (byte) 0xbe, (byte) 0xef };
		String s = encodeHexString(bytes);
		System.out.println(s);
		System.out.println(encodeHexString(bytes, 2));
		System.out.println(encodeHexString(decodeHex(s)));
	}
}
