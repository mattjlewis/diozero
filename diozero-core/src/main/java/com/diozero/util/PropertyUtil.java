package com.diozero.util;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Core
 * Filename:     PropertyUtil.java  
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


public class PropertyUtil {
	public static boolean isPropertySet(String key) {
		return getProperty(key, null) != null;
	}
	
	public static int getIntProperty(String key, int defaultValue) {
		int result = defaultValue;
		
		String val = getProperty(key, null);
		if (val != null) {
			try {
				result = Integer.parseInt(val);
			} catch (NumberFormatException e) {
				// Ignore
			}
		}
		
		return result;
	}
	
	public static boolean getBooleanProperty(String key, boolean defaultValue) {
		boolean result = defaultValue;
		
		String val = getProperty(key, null);
		if (val != null) {
			result = Boolean.parseBoolean(val);
		}
		
		return result;
	}
	
	public static String getProperty(String key, String defaultValue) {
		// System properties (-D) take priority over environment variables
		return System.getProperties().getProperty(key, System.getenv().getOrDefault(key, defaultValue));
	}
}
