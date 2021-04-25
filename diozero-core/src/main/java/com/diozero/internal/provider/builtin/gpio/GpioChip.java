package com.diozero.internal.provider.builtin.gpio;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     GpioChip.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

import org.tinylog.Logger;

import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.RuntimeIOException;
import com.diozero.util.DiozeroScheduler;
import com.diozero.util.LibraryLoader;

public class GpioChip extends GpioChipInfo implements AutoCloseable, GpioLineEventListener {
	static {
		LibraryLoader.loadSystemUtils();
	}

	public static Map<Integer, GpioChip> openAllChips() throws IOException {
		Map<Integer, GpioChip> chips = Files.list(Paths.get("/dev"))
				.filter(p -> p.getFileName().toString().startsWith("gpiochip"))
				.map(p -> NativeGpioDevice.openChip(p.toString())) //
				.filter(p -> p != null) // openChip will return null if it is unable to open the chip
				.collect(Collectors.toMap(GpioChip::getChipId, chip -> chip));

		if (chips.isEmpty()) {
			Logger.error("Unable to open any gpiochip files in /dev");
		}

		// Calculate the line offset for the chips
		// This allows GPIOs to be auto-detected as the GPIO number is chip offset +
		// line offset
		AtomicInteger offset = new AtomicInteger(0);
		chips.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
			entry.getValue().setLineOffset(offset.getAndAdd(entry.getValue().getNumLines()));
		});

		return chips;
	}

	public static List<GpioChipInfo> getChips() {
		return NativeGpioDevice.getChips();
	}

	public static GpioChip openChip(int chipNum) {
		return openChip("/dev/" + GPIO_CHIP_FILENAME_PREFIX + "/" + chipNum);
	}

	public static GpioChip openChip(String chipDeviceFile) {
		return NativeGpioDevice.openChip(chipDeviceFile);
	}

	private static final int EPOLL_FD_NOT_CREATED = -1;
	private static final String GPIO_CHIP_FILENAME_PREFIX = "gpiochip";

	// Linerequest flags
	// https://elixir.bootlin.com/linux/v4.9.127/source/include/uapi/linux/gpio.h#L58
	private static final int GPIOHANDLE_REQUEST_INPUT = 1 << 0;
	private static final int GPIOHANDLE_REQUEST_OUTPUT = 1 << 1;
	private static final int GPIOHANDLE_REQUEST_ACTIVE_LOW = 1 << 2;
	private static final int GPIOHANDLE_REQUEST_OPEN_DRAIN = 1 << 3;
	private static final int GPIOHANDLE_REQUEST_OPEN_SOURCE = 1 << 4;

	// Eventrequest flags
	// https://elixir.bootlin.com/linux/v4.9.127/source/include/uapi/linux/gpio.h#L109
	private static final int GPIOEVENT_REQUEST_RISING_EDGE = 1 << 0;
	private static final int GPIOEVENT_REQUEST_FALLING_EDGE = 1 << 1;
	private static final int GPIOEVENT_REQUEST_BOTH_EDGES = (1 << 0) | (1 << 1);

	// GPIO event types
	// https://elixir.bootlin.com/linux/v4.9.127/source/include/uapi/linux/gpio.h#L136
	public static final int GPIOEVENT_EVENT_RISING_EDGE = 0x01;
	public static final int GPIOEVENT_EVENT_FALLING_EDGE = 0x02;

	private final int chipId;
	private final int chipFd;
	private int lineOffset;
	private GpioLine[] lines;
	private final Map<String, GpioLine> linesByName;
	private int epollFd;
	private final Map<Integer, GpioLineEventListener> fdToListener;
	private final AtomicBoolean running;
	private final Queue<NativeGpioEvent> eventQueue;
	private final Lock lock;
	private final Condition condition;

	private Future<?> processEventsFuture;
	private Future<?> eventLoopFuture;

	private GpioChip(String name, String label, int chipFd, GpioLine... lines) {
		super(name, label, lines.length);

		chipId = Integer.parseInt(name.substring(GPIO_CHIP_FILENAME_PREFIX.length()));
		this.chipFd = chipFd;
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

	public int getLineOffset() {
		return lineOffset;
	}

	private void setLineOffset(int lineOffset) {
		this.lineOffset = lineOffset;
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

	public GpioLine provisionGpioInputDevice(int offset, GpioPullUpDown pud, GpioEventTrigger trigger) {
		if (offset < 0 || offset >= lines.length) {
			throw new IllegalArgumentException("Invalid GPIO offset " + offset + " must 0.." + (lines.length - 1));
		}
		// Pull-up / pull-down config available in Kernel 5.5 via gpio_v2_line_flag
		// GPIO_V2_LINE_FLAG_BIAS_*
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
		int line_fd = NativeGpioDevice.provisionGpioInputDevice(chipFd, offset, handle_flags, event_flags);
		if (line_fd < 0) {
			throw new RuntimeIOException("Error in provisionGpioInputDevice: " + line_fd);
		}
		lines[offset].setFd(line_fd);

		return lines[offset];
	}

	public GpioLine provisionGpioOutputDevice(int offset, int initialValue) {
		if (offset < 0 || offset >= lines.length) {
			throw new IllegalArgumentException("Invalid GPIO offset " + offset + " must 0.." + (lines.length - 1));
		}
		int line_fd = NativeGpioDevice.provisionGpioOutputDevice(chipFd, offset, initialValue);
		if (line_fd < 0) {
			throw new RuntimeIOException("Error in provisionGpioInputDevice: " + line_fd);
		}
		lines[offset].setFd(line_fd);

		return lines[offset];
	}

	@Override
	public synchronized void close() {
		Logger.trace("close()");

		stopEventProcessing();

		// Close all of the lines
		if (lines != null) {
			for (GpioLine line : lines) {
				line.close();
			}
			lines = null;
		}
		linesByName.clear();

		// Finally close the GPIO chip itself
		NativeGpioDevice.close(chipFd);
	}

	public void register(int fd, GpioLineEventListener listener) {
		startEventProcessing();

		int rc = NativeGpioDevice.epollAddFileDescriptor(epollFd, fd);
		if (rc < 0) {
			throw new RuntimeIOException("Error adding file descriptor '" + fd + "' to epoll");
		}

		fdToListener.put(Integer.valueOf(fd), listener);
	}

	public void deregister(int fd) {
		if (epollFd == EPOLL_FD_NOT_CREATED) {
			throw new IllegalStateException("Attempt to register an epoll fd without epoll being initiated");
		}

		int rc = NativeGpioDevice.epollRemoveFileDescriptor(epollFd, fd);
		fdToListener.remove(Integer.valueOf(fd));
		if (fdToListener.isEmpty()) {
			stopEventProcessing();
		}
		if (rc < 0) {
			throw new RuntimeIOException("Error removing file descriptor '" + fd + "' from epoll");
		}
	}

	@Override
	public void event(int fd, int eventDataId, long epochTimeMs, long timestampNanos) {
		// Acquire the lock
		lock.lock();
		// Add the event to the tail of the queue
		eventQueue.offer(new NativeGpioEvent(fd, eventDataId, epochTimeMs, timestampNanos));
		try {
			// Notify the event processor
			condition.signal();
		} finally {
			lock.unlock();
		}
	}

	private void eventLoop() {
		if (epollFd == EPOLL_FD_NOT_CREATED) {
			Logger.error("Cannot start event loop as epoll not initialised");
			return;
		}

		Logger.trace("Starting event loop for chip {}", Integer.valueOf(chipId));
		NativeGpioDevice.eventLoop(epollFd, -1, this);
		Logger.info("Event loop finished");
	}

	private void startEventProcessing() {
		// TODO Investigate use of SelectorProvider instead
		if (epollFd == EPOLL_FD_NOT_CREATED) {
			int rc = NativeGpioDevice.epollCreate();
			if (rc < 0) {
				throw new RuntimeIOException("Error in epollCreate: " + rc);
			}
			epollFd = rc;

			running.getAndSet(true);
			processEventsFuture = DiozeroScheduler.getNonDaemonInstance().submit(this::processEvents);
			eventLoopFuture = DiozeroScheduler.getNonDaemonInstance().submit(this::eventLoop);
		}
	}

	private void stopEventProcessing() {
		running.set(false);

		// Stop the epoll event loop
		if (epollFd != EPOLL_FD_NOT_CREATED) {
			Logger.trace("Stopping the epoll_wait event loop");
			NativeGpioDevice.stopEventLoop(epollFd);
			epollFd = EPOLL_FD_NOT_CREATED;
		}

		if (eventLoopFuture != null) {
			eventLoopFuture.cancel(true);
			try {
				eventLoopFuture.get();
			} catch (Exception e) {
				// Ignore
			}
			eventLoopFuture = null;
		}

		// Wake up the event processor
		lock.lock();
		try {
			condition.signal();
		} finally {
			lock.unlock();
		}

		// Finally make sure that the event processing thread is stopped
		if (processEventsFuture != null) {
			processEventsFuture.cancel(true);
			try {
				processEventsFuture.get();
			} catch (Exception e) {
				// Ignore
			}
			processEventsFuture = null;
		}
	}

	private void processEvents() {
		Thread.currentThread().setName("diozero-GpioChip-processEvents-" + hashCode());

		while (running.get()) {
			// Acquire the lock
			lock.lock();
			try {
				// Only wait if the queue is empty
				if (eventQueue.isEmpty()) {
					// Wait for an event on the queue
					condition.await();
				}
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
					Integer event_fd = Integer.valueOf(event.fd);
					GpioLineEventListener listener = fdToListener.get(event_fd);
					if (listener == null) {
						Logger.warn("No listener for fd {}, event data: '{}'", event_fd,
								Integer.valueOf(event.eventDataId));
					} else {
						listener.event(event.fd, event.eventDataId, event.epochTimeMs, event.timestampNanos);
					}
				}
			} while (!eventQueue.isEmpty());
		}

		Logger.debug("Finished");
	}

	public void eventNotUsed(int fd, int eventDataId, long epochTimeMs, long timestampNanos) {
		System.out.println("event, delta time: " + (System.nanoTime() - timestampNanos));
		synchronized (eventQueue) {
			// Add the event to the tail of the queue
			eventQueue.offer(new NativeGpioEvent(fd, eventDataId, epochTimeMs, timestampNanos));
			// Notify the event processor
			eventQueue.notify();
		}
	}

	private void processEventsNotUsed() {
		Thread.currentThread().setName("diozero-GpioChip-processEvents-" + hashCode());

		while (running.get()) {
			synchronized (eventQueue) {
				// Only wait if the queue is empty
				if (eventQueue.isEmpty()) {
					// Wait for an event on the queue
					try {
						eventQueue.wait();
					} catch (InterruptedException e) {
						Logger.debug("Interrupted!");
						break;
					}
				}
			}

			// Process all of the events on the queue
			// Note the event queue is a concurrent linked queue hence thread safe
			do {
				NativeGpioEvent event = eventQueue.poll();

				if (event == null) {
					Logger.debug("No event returned");
				} else {
					Integer event_fd = Integer.valueOf(event.fd);
					GpioLineEventListener listener = fdToListener.get(event_fd);
					if (listener == null) {
						Logger.warn("No listener for fd {}, event data: '{}'", event_fd,
								Integer.valueOf(event.eventDataId));
					} else {
						listener.event(event.fd, event.eventDataId, event.epochTimeMs, event.timestampNanos);
					}
				}
			} while (!eventQueue.isEmpty());
		}

		Logger.debug("Finished");
	}

	private static class NativeGpioEvent {
		int fd;
		int eventDataId;
		long epochTimeMs;
		long timestampNanos;

		public NativeGpioEvent(int fd, int eventDataId, long epochTimeMs, long timestampNanos) {
			this.fd = fd;
			this.eventDataId = eventDataId;
			this.epochTimeMs = epochTimeMs;
			this.timestampNanos = timestampNanos;
		}
	}
}
