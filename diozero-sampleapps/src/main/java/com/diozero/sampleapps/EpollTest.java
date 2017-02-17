package com.diozero.sampleapps;

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


import org.pmw.tinylog.Logger;

import com.diozero.util.EpollNative;

public class EpollTest {
	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Usage: " + EpollTest.class.getName() + " <filename> <filename> <filename>");
			return;
		}
		
		test(args);
	}
	
	private static void test(String[] filenames) {
		Logger.info("Calling epollCreate()");
		EpollNative epoll = new EpollNative();
		for (String filename : filenames) {
			Logger.info("Calling addFile('{}')", filename);
			epoll.register(filename, filename, (ref, epochTime, value) -> Logger.info("notify({}, {}, {})",
					filename, Long.valueOf(epochTime), Character.valueOf(value)));
		}
		epoll.processEvents();
	}
}
