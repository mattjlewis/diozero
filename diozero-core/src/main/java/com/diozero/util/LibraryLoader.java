package com.diozero.util;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Core
 * Filename:     LibraryLoader.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2017 mattjlewis
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

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import org.pmw.tinylog.Logger;

public class LibraryLoader {
	private static final Map<String, Boolean> LOADED_LIBRARIES = new HashMap<>();
	private static final String LIBRARY_EXTENSION = System.getProperty("os.name").startsWith("Windows") ? ".dll"
			: ".so";

	public static void loadLibrary(Class<?> clz, String libName) throws UnsatisfiedLinkError {
		loadLibrary(clz, libName, true);
	}

	public static void loadLibrary(Class<?> clz, String libName, boolean boardSpecific) throws UnsatisfiedLinkError {
		synchronized (LOADED_LIBRARIES) {
			if (LOADED_LIBRARIES.get(libName) == null) {
				boolean loaded = false;

				// First try load the library from within the JAR file
				String lib_file;
				if (boardSpecific) {
					lib_file = String.format("/lib/%s/lib%s%s",
							DeviceFactoryHelper.getNativeDeviceFactory().getBoardInfo().getLibraryPath(), libName,
							LIBRARY_EXTENSION);
				} else {
					lib_file = String.format("/lib/lib%s%s", libName, LIBRARY_EXTENSION);
				}
				Logger.debug("Looking for lib '" + lib_file + "' on classpath");
				try (InputStream is = clz.getResourceAsStream(lib_file)) {
					Path path = Files.createTempFile("lib" + libName, LIBRARY_EXTENSION);
					path.toFile().deleteOnExit();
					Files.copy(is, path, StandardCopyOption.REPLACE_EXISTING);
					Runtime.getRuntime().load(path.toString());
					loaded = true;
					Logger.debug("Loaded library '{}' from classpath", libName);
				} catch (Throwable t) {
					Logger.warn("Error loading library '{}' from classpath, trying System.loadLibrary: {}", libName, t);
				}
				if (!loaded) {
					// Try load from the Java system library path (-Djava.library.path)
					Logger.debug("Looking for lib '" + libName + "' on library path");
					try {
						System.loadLibrary(libName);
						loaded = true;
						Logger.info("Loaded library '{}' from system library path", libName);
					} catch (Throwable t) {
						Logger.error("Error loading library '{}' from system library path: {}", libName, t);
					}
				}
				LOADED_LIBRARIES.put(libName, Boolean.valueOf(loaded));
			}
		}
	}
}
