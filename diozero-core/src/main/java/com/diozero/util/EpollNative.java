package com.diozero.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.tinylog.Logger;

import com.diozero.api.RuntimeIOException;

public class EpollNative implements EpollNativeCallback, AutoCloseable {
	static {
		LibraryLoader.loadSystemUtils();
	}

	private static native int epollCreate();
	private static native int addFile(int epollFd, String filename);
	private static native int removeFile(int epollFd, int fileFd);
	private static native EpollEvent[] waitForEvents(int epollFd);
	private static native int eventLoop(int epollFd, EpollNativeCallback callback);
	private static native void stopWait(int epollFd);
	private static native void shutdown(int epollFd);

	private int epollFd;
	private Map<Integer, PollEventListener> fdToListener;
	private Map<String, Integer> filenameToFd;
	private AtomicBoolean running;
	private Queue<EpollEvent> eventQueue;
	private Lock lock;
	private Condition condition;

	public EpollNative() {
		fdToListener = new HashMap<>();
		filenameToFd = new HashMap<>();
		running = new AtomicBoolean(false);
		eventQueue = new ConcurrentLinkedQueue<>();
		lock = new ReentrantLock();
		condition = lock.newCondition();

		int rc = epollCreate();
		if (rc < 0) {
			throw new RuntimeIOException("Error in epollCreate: " + rc);
		}

		epollFd = rc;
	}

	private void waitForEvents() {
		Thread.currentThread().setName("diozero-EpollNative-waitForEvents-" + hashCode());
		
		if (eventLoop(epollFd, this) < 0) {
			throw new RuntimeIOException("Error starting native epoll event loop");
		}

		/*
		while (running.get()) {
			EpollEvent[] events = waitForEvents(epollFd);
			if (events == null) {
				Logger.debug("Got no events");
				running.getAndSet(false);
			} else {
				for (EpollEvent event : events) {
					eventQueue.add(event);
				}
				// Notify the process events thread that there are new events on the queue
				lock.lock();
				try {
					condition.signal();
				} finally {
					lock.unlock();
				}
			}
		}
		*/
		
		Logger.trace("Finished");
	}
	
	@Override
	public void callback(int fd, int eventMask, long epochTime, long nanoTime, byte value) {
		// Notify the process events thread that there are new events on the queue
		lock.lock();
		eventQueue.add(new EpollEvent(fd, eventMask, epochTime, nanoTime, value));
		try {
			condition.signal();
		} finally {
			lock.unlock();
		}
	}

	private void processEvents() {
		Thread.currentThread().setName("diozero-EpollNative-processEvents-" + hashCode());

		while (running.get()) {
			// Wait for an event on the queue
			lock.lock();
			try {
				condition.await();
			} catch (InterruptedException e) {
				Logger.debug("Interrupted!");
				break;
			} finally {
				lock.unlock();
			}

			// Process all of the events on the queue
			// Note the event queue is a concurrent linked queue hence thread safe
			do {
				EpollEvent event = eventQueue.poll();

				if (event == null) {
					Logger.debug("No event returned");
				} else {
					Integer fd = Integer.valueOf(event.getFd());
					PollEventListener listener = fdToListener.get(fd);
					if (listener == null) {
						Logger.warn("No listener for fd {}, event value: '{}'", fd,
								Character.valueOf(event.getValue()));
					} else {
						listener.notify(event.getEpochTime(), event.getNanoTime(), event.getValue());
					}
				}
			} while (! eventQueue.isEmpty());
		}
		
		Logger.debug("Finished");
	}

	public void register(String filename, PollEventListener listener) {
		int file_fd = addFile(epollFd, filename);
		if (file_fd == -1) {
			throw new RuntimeIOException("Error registering file '" + filename + "' with epoll");
		}

		fdToListener.put(Integer.valueOf(file_fd), listener);
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

		fdToListener.remove(file_fd);
		filenameToFd.remove(filename);
	}

	public void enableEvents() {
		running.getAndSet(true);
		DiozeroScheduler.getDaemonInstance().execute(this::processEvents);
		DiozeroScheduler.getDaemonInstance().execute(this::waitForEvents);
	}

	public void disableEvents() {
		running.getAndSet(false);
		// Stop the epoll thread
		stopWait(epollFd);
		// Wake up the process events thread so that it can exit
		lock.lock();
		try {
			condition.signal();
		} finally {
			lock.unlock();
		}
	}

	@Override
	public void close() {
		disableEvents();
		shutdown(epollFd);
		fdToListener.clear();
		filenameToFd.clear();
	}
}
