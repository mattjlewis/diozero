package com.diozero.sampleapps;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Sample applications
 * Filename:     EpollTest.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
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


import java.util.Date;

import org.tinylog.Logger;

import com.diozero.util.EpollNative;
import com.diozero.util.SleepUtil;

/*
 * Use mkfifo <file> to test in Linux, epoll doesn't work with regular files
 */
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
		try (EpollNative epoll = new EpollNative()) {
			int delay = 10;
			
			Logger.info("Enabling events");
			epoll.enableEvents();
			
			for (String filename : filenames) {
				Logger.info("Calling register('{}')", filename);
				epoll.register(filename, EpollTest::notify);
			}
			
			Logger.info("Waiting for {} seconds", Integer.valueOf(delay));
			SleepUtil.sleepSeconds(delay);
			
			for (String filename : filenames) {
				Logger.info("Calling deregister('{}')", filename);
				epoll.deregister(filename);
			}
	
			Logger.info("Waiting for {} seconds", Integer.valueOf(delay));
			SleepUtil.sleepSeconds(delay);
			
			for (String filename : filenames) {
				Logger.info("Calling register('{}')", filename);
				epoll.register(filename, EpollTest::notify);
			}
			
			Logger.info("Waiting for {} seconds", Integer.valueOf(delay));
			SleepUtil.sleepSeconds(delay);
		}
	}
	
	private static void notify(long epochTime, long nanoTime, char value) {
		Logger.info("notify({}, {}, {})", new Date(epochTime), Long.valueOf(nanoTime), Character.valueOf(value));
	}
}
