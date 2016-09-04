package com.diozero.internal.provider.sysfs;

public class NativeSysFsI2C {
	public static native int open(int controller, int address);
	public static native int close(int fd);
	public static native int readBlockData(int fd, int register, byte[] buffer, int toRead);
	public static native int writeBlockData(int fd, int register, byte[] buffer, int toWrite);
	public static native int readDevice(int fd, byte[] buffer, int toRead);
	public static native int writeDevice(int fd, byte[] buffer, int toWrite);
}
