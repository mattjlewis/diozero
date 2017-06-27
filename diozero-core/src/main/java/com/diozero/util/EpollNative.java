package com.diozero.util;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Core
 * Filename:     EpollNative.java  
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


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.pmw.tinylog.Logger;

public class EpollNative {
	static {
		LibraryLoader.loadLibrary(EpollNative.class, "diozero-system-utils");
	}
	
	private static native int epollCreate();
	private static native int addFile(int epollFd, String filename);
	private static native int removeFile(int epollFd, int fileFd);
	private static native EpollEvent[] waitForEvents(int epollFd);
	private static native void stopWait(int epollFd);
	private static native void shutdown(int epollFd);
	
	private int epollFd;
	private Map<Integer, String> fdToFilename;
	private Map<Integer, PollEventListener> fdToListener;
	private Map<Integer, Object> fdToRef;
	private Map<String, Integer> filenameToFd;
	private AtomicBoolean running;
	
	public EpollNative() {
		running = new AtomicBoolean(false);
		fdToFilename = new HashMap<>();
		fdToListener = new HashMap<>();
		fdToRef = new HashMap<>();
		filenameToFd = new HashMap<>();
		
		int rc = epollCreate();
		if (rc < 0) {
			throw new RuntimeIOException("Error in epollCreate: " + rc);
		}
		
		epollFd = rc;
	}
	
	public void register(String filename, Object ref, PollEventListener listener) {
		int file_fd = addFile(epollFd, filename);
		if (file_fd == -1) {
			throw new RuntimeIOException("Error registering file '" + filename + "' with epoll");
		}
		
		fdToFilename.put(Integer.valueOf(file_fd), filename);
		fdToListener.put(Integer.valueOf(file_fd), listener);
		fdToRef.put(Integer.valueOf(file_fd), ref);
		filenameToFd.put(filename, Integer.valueOf(file_fd));
	}
	
	public void deregister(String filename) {
		Integer file_fd = filenameToFd.get(filename);
		if (file_fd == null) {
			Logger.warn("No file descriptor for '{}'", filename);
			return;
		}
		
		int rc = removeFile(epollFd, file_fd.intValue());
		if (rc < 0) {
			Logger.warn("Error in epoll removeFile for file fd {}: {}", file_fd, Integer.valueOf(rc));
		}
		fdToFilename.remove(file_fd);
		fdToListener.remove(file_fd);
		fdToRef.remove(file_fd);
		filenameToFd.remove(filename);
	}
	
	public void processEvents() {
		running.getAndSet(true);
		while (running.get()) {
			EpollEvent[] events = waitForEvents(epollFd);
			if (events == null) {
				running.getAndSet(false);
			} else {
				for (EpollEvent event : events) {
					PollEventListener listener = fdToListener.get(Integer.valueOf(event.getFd()));
					if (listener != null) {
						listener.notify(fdToRef.get(Integer.valueOf(event.getFd())), event.getEpochTime(), event.getValue());
					}
				}
			}
		}
	}
	
	public void stop() {
		running.getAndSet(false);
		stopWait(epollFd);
	}
	
	public void close() {
		shutdown(epollFd);
	}
}
