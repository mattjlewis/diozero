package com.diozero.internal.provider.builtin.gpio;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     GpioLine.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2021 diozero
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

import com.diozero.api.RuntimeIOException;

// struct gpiod_line
public class GpioLine implements AutoCloseable {
	public static enum Direction {
		INPUT, OUTPUT;
	}

	// Line informational flags
	// https://elixir.bootlin.com/linux/v4.9.127/source/include/uapi/linux/gpio.h#L29
	private static final int GPIOLINE_FLAG_KERNEL = 1 << 0;
	private static final int GPIOLINE_FLAG_IS_OUT = 1 << 1;
	private static final int GPIOLINE_FLAG_ACTIVE_LOW = 1 << 2;
	private static final int GPIOLINE_FLAG_OPEN_DRAIN = 1 << 3;
	private static final int GPIOLINE_FLAG_OPEN_SOURCE = 1 << 4;

	private final int offset;
	private final boolean reserved;
	private final Direction direction;
	private final boolean activeLow;
	private final boolean openDrain;
	private final boolean openSource;
	private final String name;
	private final String consumer;
	private int fd;

	public GpioLine(int offset, int flags, String name, String consumer) {
		this.offset = offset;
		this.reserved = ((flags & GPIOLINE_FLAG_KERNEL) == 0) ? false : true;
		this.direction = ((flags & GPIOLINE_FLAG_IS_OUT) == 0) ? Direction.INPUT : Direction.OUTPUT;
		this.activeLow = ((flags & GPIOLINE_FLAG_ACTIVE_LOW) == 0) ? false : true;
		this.openDrain = ((flags & GPIOLINE_FLAG_OPEN_DRAIN) == 0) ? false : true;
		this.openSource = ((flags & GPIOLINE_FLAG_OPEN_SOURCE) == 0) ? false : true;
		this.name = name;
		this.consumer = consumer;
	}

	public int getOffset() {
		return offset;
	}

	public boolean isReserved() {
		return reserved;
	}

	public Direction getDirection() {
		return direction;
	}

	public boolean isActiveLow() {
		return activeLow;
	}

	public boolean isOpenDrain() {
		return openDrain;
	}

	public boolean isOpenSource() {
		return openSource;
	}

	public String getName() {
		return name;
	}

	public String getConsumer() {
		return consumer;
	}

	public int getFd() {
		return fd;
	}

	void setFd(int fd) {
		this.fd = fd;
	}

	public int getValue() {
		int rc = NativeGpioDevice.getValue(fd);
		if (rc < 0) {
			throw new RuntimeIOException("Error in getValue() for line " + offset + ": " + rc);
		}
		return rc;
	}

	public void setValue(int value) {
		int rc = NativeGpioDevice.setValue(fd, value);
		if (rc < 0) {
			throw new RuntimeIOException("Error in setValue(" + value + ") for line " + offset + ": " + rc);
		}
	}

	@Override
	public void close() {
		NativeGpioDevice.close(fd);
	}
}
