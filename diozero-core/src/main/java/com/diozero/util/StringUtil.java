package com.diozero.util;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     StringUtil.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2023 diozero
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

import java.util.Arrays;
import java.util.Collections;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StringUtil {
	public static void main(String[] args) {
		final String compatibility = "a,b,c,d";
		final String[] parts = compatibility.split(",");

		for (int i = 0; i < parts.length; i++) {
			System.out.println(join("_", parts.length - i, parts));
			System.out.println(join("_", "/boarddefs/", ".txt", parts.length - i, parts));
		}

		IntStream.range(0, parts.length).mapToObj(i -> join("_", parts.length - i, parts)).forEach(System.out::println);
	}

	public static boolean isNullOrBlank(String s) {
		return s == null || s.isBlank();
	}

	public static boolean isNotBlank(String s) {
		return s != null && !s.isBlank();
	}

	public static String pad(String str, int length) {
		return String.format("%1$-" + length + "s", str).substring(0, length);
	}

	public static String repeat(char ch, int n) {
		return repeat(String.valueOf(ch), n);
	}

	public static String repeat(CharSequence string, int n) {
		return String.join("", Collections.nCopies(n, string));
	}

	public static String join(String delimiter, int limit, String[] elements) {
		return join(delimiter, "", "", limit, elements);
	}

	public static String join(String delimiter, String prefix, String suffix, int limit, String[] elements) {
		return Arrays.stream(elements).limit(limit).collect(Collectors.joining(delimiter, prefix, suffix));
	}

	public static String unquote(String value) {
		if (value == null) {
			return null;
		}
		if (value.startsWith("\"") && value.endsWith("\"")) {
			return value.substring(1, value.length() - 1);
		}
		return value;
	}
}
