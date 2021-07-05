package com.diozero.util;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     PropertyUtil.java
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

import java.util.Optional;
import java.util.HashMap;
import java.util.Map;

import org.tinylog.Logger;

/**
 * Access properties that are set either via command line "-D" parameters or as
 * environment variables. Command line parameters take precedence over
 * environment variables.
 */
public class PropertyUtil {
	private static final Map<String, String> envProps;
	static {
		envProps = new HashMap<>();
		System.getenv().entrySet().forEach(entry -> {
			envProps.put(entry.getKey(), entry.getValue());
			// Additionally convert '_' to '.' as the shell export command doesn't allow '.'
			if (entry.getKey().contains("_")) {
				envProps.put(entry.getKey().replace('_', '.'), entry.getValue());
			}
		});
	}

	public static boolean isPropertySet(String key) {
		return getProperty(key).isPresent();
	}

	public static long getLongProperty(String key, long defaultValue) {
		return getLongProperty(key).orElse(Long.valueOf(defaultValue)).longValue();
	}

	public static Optional<Long> getLongProperty(String key) {
		Optional<Long> result = Optional.empty();

		String val = getProperty(key, null);
		if (val != null) {
			try {
				result = Optional.of(Long.valueOf(val));
			} catch (NumberFormatException e) {
				// Ignore and leave as empty
				Logger.warn(e, "Error parsing '" + val + "' as Long", e);
			}
		}

		return result;
	}

	public static int getIntProperty(String key, int defaultValue) {
		return getIntProperty(key).orElse(Integer.valueOf(defaultValue)).intValue();
	}

	public static Optional<Integer> getIntProperty(String key) {
		Optional<Integer> result = Optional.empty();

		String val = getProperty(key, null);
		if (val != null) {
			try {
				result = Optional.of(Integer.valueOf(val));
			} catch (NumberFormatException e) {
				// Ignore and leave as empty
				Logger.warn(e, "Error parsing '" + val + "' as Integer", e);
			}
		}

		return result;
	}

	public static Optional<Boolean> getBooleanProperty(String key) {
		Optional<Boolean> result = Optional.empty();

		String val = getProperty(key, null);
		if (val != null) {
			result = Optional.of(Boolean.valueOf(val));
		}

		return result;
	}

	public static float getFloatProperty(String key, float defaultValue) {
		return getFloatProperty(key).orElse(Float.valueOf(defaultValue)).floatValue();
	}

	public static Optional<Float> getFloatProperty(String key) {
		Optional<Float> result = Optional.empty();

		String val = getProperty(key, null);
		if (val != null) {
			try {
				result = Optional.of(Float.valueOf(val));
			} catch (NumberFormatException e) {
				// Ignore and leave as empty
				Logger.warn(e, "Error parsing '" + val + "' as Float", e);
			}
		}

		return result;
	}

	public static double getDoubleProperty(String key, double defaultValue) {
		return getDoubleProperty(key).orElse(Double.valueOf(defaultValue)).doubleValue();
	}

	public static Optional<Double> getDoubleProperty(String key) {
		Optional<Double> result = Optional.empty();

		String val = getProperty(key, null);
		if (val != null) {
			try {
				result = Optional.of(Double.valueOf(val));
			} catch (NumberFormatException e) {
				// Ignore and leave as empty
				Logger.warn(e, "Error parsing '" + val + "' as Double", e);
			}
		}

		return result;
	}

	public static boolean getBooleanProperty(String key, boolean defaultValue) {
		return getBooleanProperty(key).orElse(Boolean.valueOf(defaultValue)).booleanValue();
	}

	public static Optional<String> getProperty(String key) {
		// System properties (-D) take priority over environment variables
		// Java 9 has Optional.or():
		/*-
		return Optional.ofNullable(System.getProperties().getProperty(key, envProps.get(key)))
				.or(Optional::empty);
		*/
		String value = System.getProperties().getProperty(key, envProps.get(key));
		return value == null ? Optional.empty() : Optional.of(value);
	}

	public static String getProperty(String key, String defaultValue) {
		return getProperty(key).orElse(defaultValue);
	}
}
