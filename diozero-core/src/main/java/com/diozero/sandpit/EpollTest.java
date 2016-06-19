package com.diozero.sandpit;

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

import com.diozero.util.EpollEvent;
import com.diozero.util.EpollNative;

public class EpollTest {
	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Usage: " + EpollTest.class.getName() + " <filename>");
			return;
		}
		
		String filename = args[0];
		
		test(filename);
	}
	
	private static void test(String filename) {
		Logger.info("Calling epollCreate()");
		int epoll_fd = EpollNative.epollCreate();
		if (epoll_fd == -1) {
			Logger.error("Error in epollCreate()");
			return;
		}
		Logger.info("Calling addFile('" + filename + "')");
		int file_fd = EpollNative.addFile(epoll_fd, filename);
		if (file_fd == -1) {
			Logger.error("Error in addFile()");
			//EpollNative.close();
			return;
		}
		Logger.info("file_fd = " + file_fd);
		int event_num = 0;
		while (true) {
			Logger.info("Waiting for events");
			for (EpollEvent event : EpollNative.waitForEvents(epoll_fd)) {
				System.out.println("Got epoll event : " + event);
				if (event.getFd() == file_fd) {
					event_num++;
					System.out.println("Event was for my file descriptor, event_num=" + event_num);
					if (event_num == 10) {
						Logger.info("Count reached " + event_num + ", stopping listener");
						EpollNative.removeFile(epoll_fd, file_fd);
					}
				}
			}
		}
	}
}
