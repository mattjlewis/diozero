package com.diozero.util;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     EpollEvent.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
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


public class EpollEvent {
	private int fd;
	private int eventMask;
	private long epochTime;
	private long nanoTime;
	private char value;

	public EpollEvent(int fd, int eventMask, long epochTime, long nanoTime, byte value) {
		this.fd = fd;
		this.eventMask = eventMask;
		this.epochTime = epochTime;
		this.nanoTime = nanoTime;
		this.value = (char) value;
	}

	public int getFd() {
		return fd;
	}

	public int getEventMask() {
		return eventMask;
	}

	public long getEpochTime() {
		return epochTime;
	}

	public long getNanoTime() {
		return nanoTime;
	}

	public char getValue() {
		return value;
	}

	@Override
	public String toString() {
		return "EpollEvent [fd=" + fd + ", eventMask=" + eventMask + ", epochTime=" + epochTime + ", nanoTime="
				+ nanoTime + ", value=" + value + "]";
	}
}
