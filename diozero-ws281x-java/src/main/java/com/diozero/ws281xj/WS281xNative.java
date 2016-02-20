package com.diozero.ws281xj;

import java.nio.ByteBuffer;

public class WS281xNative {
	public static native ByteBuffer initialise(int frequency, int dmaNum, int gpioNum, int brightness, int numPixels);
	public static native void terminate();
	public static native int render();
}
