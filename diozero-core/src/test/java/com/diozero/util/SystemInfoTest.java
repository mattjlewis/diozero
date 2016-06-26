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


import com.diozero.util.SystemInfo;

public class SystemInfoTest {
	public static void main(String[] args) {
		//System.out.println(osReleaseProperties);
		//System.out.format("O/S Id='%s', Version='%s', Version Id='%s'%n",
		//		getOperatingSystemId(), getOperatingSystemVersion(), getOperatingSystemVersionId());
		String line = "Revision        : a02082\n";
		String revision_string = line.split(":")[1].trim();
		System.out.println(revision_string + ": " + SystemInfo.lookupBoardInfo(revision_string));
		revision_string = "a01040";
		System.out.println(revision_string + ": " + SystemInfo.lookupBoardInfo(revision_string));
		revision_string = "a01041";
		System.out.println(revision_string + ": " + SystemInfo.lookupBoardInfo(revision_string));
		revision_string = "a21041";
		System.out.println(revision_string + ": " + SystemInfo.lookupBoardInfo(revision_string));
		revision_string = "900092";
		System.out.println(revision_string + ": " + SystemInfo.lookupBoardInfo(revision_string));
		revision_string = "a00092";
		System.out.println(revision_string + ": " + SystemInfo.lookupBoardInfo(revision_string));
		revision_string = "a00093";
		System.out.println(revision_string + ": " + SystemInfo.lookupBoardInfo(revision_string));
		revision_string = "a02082";
		System.out.println(revision_string + ": " + SystemInfo.lookupBoardInfo(revision_string));
		revision_string = "020b";
		System.out.println(revision_string + ": " + SystemInfo.lookupBoardInfo(revision_string));
	}
}
