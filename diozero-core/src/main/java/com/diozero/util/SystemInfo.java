package com.diozero.util;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 mattjlewis
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


import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

public class SystemInfo {
	private static final String OS_RELEASE_FILE = "/etc/os-release";
	
	private static Properties properties;
	private static boolean initialised;
	
	private static synchronized void initialise() throws RuntimeIOException {
		if (! initialised) {
			properties = new Properties();
			try (Reader reader = new FileReader(OS_RELEASE_FILE)) {
				properties.load(reader);
				
				initialised = true;
			} catch (IOException e) {
				throw new RuntimeIOException("Error loading properties file '" + OS_RELEASE_FILE, e);
			}
		}
	}

	public static String getOperatingSystem() {
		initialise();
		
		return properties.getProperty("ID");
	}

	public static void main(String[] args) {
		System.out.println(properties);
		System.out.println("OS='" + getOperatingSystem() + "'");
	}
}
