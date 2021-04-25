package com.diozero.util;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     FileDescriptorUtil.java
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

import java.io.FileDescriptor;
import java.lang.reflect.Field;

import org.tinylog.Logger;

import com.diozero.api.RuntimeIOException;

public class FileDescriptorUtil {
	private static boolean initialised;
	private static Field fdField;
	// private static Constructor<FileDescriptor> fdConstructor;

	private static synchronized void initialise() {
		if (!initialised) {
			try {
				fdField = FileDescriptor.class.getDeclaredField("fd");
				fdField.setAccessible(true);

				/*-
				fdConstructor = FileDescriptor.class.getDeclaredConstructor(int.class);
				fdConstructor.setAccessible(true);
				*/

				initialised = true;
			} catch (NoSuchFieldException | SecurityException e) {
				Logger.error(e, "Error: {}", e);
				throw new RuntimeIOException("Error getting native file descriptor declared field / constructor: " + e,
						e);
			}
		}
	}

	public static synchronized int getNativeFileDescriptor(FileDescriptor fd) {
		initialise();

		try {
			return ((Integer) fdField.get(fd)).intValue();
		} catch (IllegalArgumentException | IllegalAccessException e) {
			Logger.error(e, "Error: {}", e);
			throw new RuntimeIOException("Error accessing private fd attribute: " + e, e);
		}
	}

	/*-
	public static FileDescriptor createFileDescriptor(int fd) {
		initialise();
	
		try {
			return fdConstructor.newInstance(Integer.valueOf(fd));
		} catch (IllegalArgumentException | IllegalAccessException | InstantiationException
				| InvocationTargetException e) {
			Logger.error(e, "Error: {}", e);
			throw new RuntimeIOException("Error accessing private fd attribute: " + e, e);
		}
	}
	*/
}
