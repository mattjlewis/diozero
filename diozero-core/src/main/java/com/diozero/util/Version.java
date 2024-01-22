package com.diozero.util;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     Version.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2024 diozero
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

public class Version {
	private int major;
	private int minor;
	private int point;

	public Version(int major, int minor, int point) {
		this.major = major;
		this.minor = minor;
		this.point = point;
	}

	public Version(String s) {
		final String[] parts = s.split("\\.");
		major = Integer.parseInt(parts[0]);
		minor = Integer.parseInt(parts[1]);
		point = Integer.parseInt(parts[2].split("-")[0]);
	}

	public int getMajor() {
		return major;
	}

	public int getMinor() {
		return minor;
	}

	public int getPoint() {
		return point;
	}

	@Override
	public String toString() {
		return "Version [" + major + "." + minor + "." + point + "]";
	}

	public static void main(String[] args) {
		// Self test
		System.out.println(new Version("2.6.18-92.el5"));

		String version = "Linux version 6.1.21-v7l+ (dom@buildbot) (arm-linux-gnueabihf-gcc-8 (Ubuntu/Linaro 8.4.0-3ubuntu1) 8.4.0, GNU ld (GNU Binutils for Ubuntu) 2.34) #1642 SMP Mon Apr  3 17:22:30 BST 2023";
		String version_string = version.replaceAll("^Linux version (\\d+\\.\\d+\\.\\d+(?:-.+?)?) .*$", "$1");
		System.out.println(new Version(version_string));
	}
}
