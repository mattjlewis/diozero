package com.diozero.sandpit;

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
