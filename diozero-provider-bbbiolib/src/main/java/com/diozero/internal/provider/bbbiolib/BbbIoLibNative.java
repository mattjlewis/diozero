package com.diozero.internal.provider.bbbiolib;

import com.diozero.util.LibraryLoader;

public class BbbIoLibNative {
	static {
		LibraryLoader.loadLibrary(BbbIoLibNative.class, "diozero_bbbiolib", false);
	}
	
	static final byte BBBIO_DIR_IN = 0;
	static final byte BBBIO_DIR_OUT = 1;

	static native int init();
	static native void shutdown();
	static native int setDir(byte port, byte pin, byte dir);
	static native int getValue(byte port, byte pin);
	static native void setValue(byte port, byte pin, boolean value);
}
