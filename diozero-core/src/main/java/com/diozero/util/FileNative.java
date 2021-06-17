package com.diozero.util;

public class FileNative {
	public static final int O_RDWR = 0x0002;
	public static final int O_SYNC = 0x0080;

	public static native int open(String filename, int flags);

	public static native int close(int fd);
}
