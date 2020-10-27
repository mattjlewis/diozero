package com.diozero.internal.provider.sysfs;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.tinylog.Logger;

import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.util.DioZeroScheduler;
import com.diozero.util.LibraryLoader;
import com.diozero.util.RuntimeIOException;

public class NativeGpioChip extends GpioChipInfo implements Closeable, GpioLineEventListener {
	static {
		LibraryLoader.loadSystemUtils();
	}
	
	public static native ArrayList<GpioChipInfo> getChips();
	/**
	 * Open the specified GPIO chip
	 * @param filename File path to the chip, e.g. /dev/gpiochip0
	 * @return The NativeGpioChip
	 */
	public static native NativeGpioChip openChip(String filename);
	public static NativeGpioChip openChip(int chipNum) {
		return openChip("/dev/" + GPIO_CHIP_FILENAME_PREFIX + "/" + chipNum);
	}

	private static native int provisionGpioInputDevice(int chipFd, int offset, int handleFlags, int eventFlags);
	private static native int provisionGpioOutputDevice(int chipFd, int offset, int initialValue);
	private static native int getValue(int lineFd);
	private static native int setValue(int lineFd, int value);
	private static native int epollCreate();
	private static native int epollAddFileDescriptor(int epollFd, int lineFd);
	private static native int epollRemoveFileDescriptor(int epollFd, int lineFd);
	/*-
	 * The timeout argument specifies the number of milliseconds that epoll_wait() will block
	 * Specifying a timeout of -1 causes epoll_wait() to block indefinitely, while specifying a timeout
	 * equal to zero cause epoll_wait() to return immediately, even if no events are available
	 */
	private static native int eventLoop(int epollFd, int timeoutMillis, GpioLineEventListener listener);
	/**
	 * Close a file descriptor
	 * @param fd The file descriptor to close
	 */
	static native void close(int fd);

	private static final int EPOLL_FD_NOT_CREATED = -1;

	private static final String GPIO_CHIP_FILENAME_PREFIX = "gpiochip";
	private static final String GPIO_LINE_NAME_PREFIX = "GPIO";
	
	// Linerequest flags
	// https://elixir.bootlin.com/linux/v4.9.127/source/include/uapi/linux/gpio.h#L58
	private static final int GPIOHANDLE_REQUEST_INPUT = (1 << 0);
	private static final int GPIOHANDLE_REQUEST_OUTPUT = (1 << 1);
	private static final int GPIOHANDLE_REQUEST_ACTIVE_LOW = (1 << 2);
	private static final int GPIOHANDLE_REQUEST_OPEN_DRAIN = (1 << 3);
	private static final int GPIOHANDLE_REQUEST_OPEN_SOURCE = (1 << 4);
	
	// Eventrequest flags
	// https://elixir.bootlin.com/linux/v4.9.127/source/include/uapi/linux/gpio.h#L109
	private static final int GPIOEVENT_REQUEST_RISING_EDGE = (1 << 0);
	private static final int GPIOEVENT_REQUEST_FALLING_EDGE = (1 << 1);
	private static final int GPIOEVENT_REQUEST_BOTH_EDGES = ((1 << 0) | (1 << 1));
	
	// GPIO event types
	// https://elixir.bootlin.com/linux/v4.9.127/source/include/uapi/linux/gpio.h#L136
	static final int GPIOEVENT_EVENT_RISING_EDGE = 0x01;
	static final int GPIOEVENT_EVENT_FALLING_EDGE = 0x02;
	
	private int chipId;
	private int fd;
	private GpioLine[] lines;
	private Map<String, GpioLine> linesByName;
	private int epollFd;
	private Map<Integer, GpioLineEventListener> fdToListener;
	private AtomicBoolean running;
	private Queue<NativeGpioEvent> eventQueue;
	private Lock lock;
	private Condition condition;

	private Future<?> processEventsFuture;
	private Future<?> eventLoopFuture;

	public NativeGpioChip(String name, String label, int fd, GpioLine[] lines) {
		super(name, label, lines.length);

		chipId = Integer.parseInt(name.substring(GPIO_CHIP_FILENAME_PREFIX.length()));
		this.fd = fd;
		this.lines = lines;
		linesByName = new HashMap<>();
		for (GpioLine line : lines) {
			linesByName.put(line.getName(), line);
		}
		
		epollFd = EPOLL_FD_NOT_CREATED;
		
		fdToListener = new HashMap<>();
		
		running = new AtomicBoolean(false);
		eventQueue = new ConcurrentLinkedQueue<>();
		lock = new ReentrantLock();
		condition = lock.newCondition();
	}
	
	public int getChipId() {
		return chipId;
	}

	public GpioLine[] getLines() {
		return lines;
	}

	public GpioLine getLineByName(String name) {
		if (linesByName == null) {
			return null;
		}
		return linesByName.get(name);
	}

	GpioLine provisionGpioInputDevice(int offset, GpioPullUpDown pud, GpioEventTrigger trigger) {
		if (offset < 0 || offset >= lines.length) {
			throw new IllegalArgumentException("Invalid GPIO offset " + offset + " must 0.." + (lines.length - 1));
		}
		// Pull-up / pull-down config available in Kernel 5.5 via gpio_v2_line_flag GPIO_V2_LINE_FLAG_BIAS_*
		// https://microhobby.com.br/blog/2020/02/02/new-linux-kernel-5-5-new-interfaces-in-gpiolib/
		// As on 19/10/2020 Raspberry is on Kernel 5.4.51
		int handle_flags = 0;
		int event_flags;
		switch (trigger) {
		case RISING:
			event_flags = GPIOEVENT_REQUEST_RISING_EDGE;
			break;
		case FALLING:
			event_flags = GPIOEVENT_REQUEST_FALLING_EDGE;
			break;
		case BOTH:
			event_flags = GPIOEVENT_REQUEST_BOTH_EDGES;
			break;
		default:
			event_flags = 0;
		}
		int line_fd = provisionGpioInputDevice(fd, offset, handle_flags, event_flags);
		if (line_fd < 0) {
			throw new RuntimeIOException("Error in provisionGpioInputDevice: " + line_fd);
		}
		lines[offset].setFd(line_fd);

		return lines[offset];
	}

	GpioLine provisionGpioOutputDevice(int offset, int initialValue) {
		if (offset < 0 || offset >= lines.length) {
			throw new IllegalArgumentException("Invalid GPIO offset " + offset + " must 0.." + (lines.length - 1));
		}
		int line_fd = provisionGpioOutputDevice(fd, offset, initialValue);
		if (line_fd < 0) {
			throw new RuntimeIOException("Error in provisionGpioInputDevice: " + line_fd);
		}
		lines[offset].setFd(line_fd);

		return lines[offset];
	}
	
	int getValue(GpioLine line) {
		int rc = getValue(line.getFd());
		if (rc < 0) {
			throw new RuntimeIOException("Error in getValue(" + line.getOffset() + "): " + rc);
		}
		return rc;
	}
	
	void setValue(GpioLine line, int value) {
		int rc = setValue(line.getFd(), value);
		if (rc < 0) {
			throw new RuntimeIOException("Error in setValue(" + line.getOffset() + ", " + value + "): " + rc);
		}
	}

	int getLineValue(int offset) {
		if (offset < 0 || offset >= lines.length) {
			throw new IllegalArgumentException("Invalid GPIO offset " + offset + " must 0.." + (lines.length - 1));
		}
		return getValue(lines[offset]);
	}

	void setLineValue(int offset, int value) {
		if (offset < 0 || offset >= lines.length) {
			throw new IllegalArgumentException("Invalid GPIO offset " + offset + " must 0.." + (lines.length - 1));
		}
		setValue(lines[offset], value);
	}

	@Override
	public void close() {
		if (running.get()) {
			running.getAndSet(false);
	
			lock.lock();
			try {
				// Attempt to interrupt the condition
				condition.signal();
			} finally {
				lock.unlock();
			}
			
			// Stop the eventLoop and processEvents threads
			if (eventLoopFuture != null) {
				eventLoopFuture.cancel(true);
			}
			if (processEventsFuture != null) {
				processEventsFuture.cancel(true);
			}
		}
		
		// Close all of the lines
		for (GpioLine line : lines) {
			close(line.getFd());
		}
		lines = null;
		linesByName.clear();
		
		// Then close the chip
		close(fd);
	}

	public void register(int fd, GpioLineEventListener listener) {
		// FIXME Use SelectorProvider instead?
		if (epollFd == EPOLL_FD_NOT_CREATED) {
			int rc = NativeGpioChip.epollCreate();
			if (rc < 0) {
				throw new RuntimeIOException("Error in epollCreate: " + rc);
			}
			epollFd = rc;
			
			running.getAndSet(true);
			processEventsFuture = DioZeroScheduler.getDaemonInstance().submit(this::processEvents);
			eventLoopFuture = DioZeroScheduler.getDaemonInstance().submit(this::eventLoop);
		}
		
		int rc = epollAddFileDescriptor(epollFd, fd);
		if (rc < 0) {
			throw new RuntimeIOException("Error adding file descriptor '" + fd + "' to epoll");
		}

		fdToListener.put(Integer.valueOf(fd), listener);
	}

	public void deregister(int fd) {
		if (epollFd == EPOLL_FD_NOT_CREATED) {
			throw new IllegalStateException("Attempt to register an epoll fd without epoll being initiated");
		}
		
		int rc = epollRemoveFileDescriptor(epollFd, fd);
		fdToListener.remove(Integer.valueOf(fd));
		if (rc < 0) {
			throw new RuntimeIOException("Error removing file descriptor '" + fd + "' from epoll");
		}
	}
	
	@Override
	public void event(int fd, int eventDataId, long timestampNanos) {
		lock.lock();
		eventQueue.add(new NativeGpioEvent(fd, eventDataId, timestampNanos));
		try {
			condition.signal();
		} finally {
			lock.unlock();
		}
	}
	
	private void eventLoop() {
		int rc = NativeGpioChip.eventLoop(epollFd, -1, this);
		if (rc < 0) {
			Logger.error("Event loop exited with {}", Integer.valueOf(rc));
		}
		Logger.info("Poll loop finished");
	}
	
	private void processEvents() {
		Thread.currentThread().setName("diozero-NativeGpioChip-processEvents-" + hashCode());

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
				NativeGpioEvent event = eventQueue.poll();

				if (event == null) {
					Logger.debug("No event returned");
				} else {
					Integer fd = Integer.valueOf(event.fd);
					GpioLineEventListener listener = fdToListener.get(fd);
					if (listener == null) {
						Logger.warn("No listener for fd {}, event data: '{}'", fd, Integer.valueOf(event.eventDataId));
					} else {
						listener.event(event.fd, event.eventDataId, event.timestampNanos);
					}
				}
			} while (! eventQueue.isEmpty());
		}
		
		Logger.debug("Finished");
	}
	
	private static class NativeGpioEvent {
		int fd;
		int eventDataId;
		long timestampNanos;
		
		public NativeGpioEvent(int fd, int eventDataId, long timestampNanos) {
			this.fd = fd;
			this.eventDataId = eventDataId;
			this.timestampNanos = timestampNanos;
		}
	}
}
