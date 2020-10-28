package com.diozero.internal.provider.builtin.gpio;

import java.util.ArrayList;

public class NativeGpioDevice {
	static native ArrayList<GpioChipInfo> getChips();

	/**
	 * Open the specified GPIO chip
	 * 
	 * @param filename File path to the chip, e.g. /dev/gpiochip0
	 * @return The NativeGpioChip
	 */
	static native GpioChip openChip(String filename);


	static native int provisionGpioInputDevice(int chipFd, int offset, int handleFlags, int eventFlags);
	static native int provisionGpioOutputDevice(int chipFd, int offset, int initialValue);
	static native int getValue(int lineFd);
	static native int setValue(int lineFd, int value);
	static native int epollCreate();
	static native int epollAddFileDescriptor(int epollFd, int lineFd);
	static native int epollRemoveFileDescriptor(int epollFd, int lineFd);

	/*-
	 * The timeout argument specifies the number of milliseconds that epoll_wait() will block
	 * Specifying a timeout of -1 causes epoll_wait() to block indefinitely, while specifying a timeout
	 * equal to zero cause epoll_wait() to return immediately, even if no events are available
	 */
	static native void eventLoop(int epollFd, int timeoutMillis, GpioLineEventListener listener);

	/**
	 * Close a file descriptor
	 * 
	 * @param fd The file descriptor to close
	 */
	static native void close(int fd);
}
