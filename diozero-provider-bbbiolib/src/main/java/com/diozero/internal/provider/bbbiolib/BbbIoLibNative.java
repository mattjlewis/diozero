package com.diozero.internal.provider.bbbiolib;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - BBBioLib
 * Filename:     BbbIoLibNative.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 mattjlewis
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
