package com.diozero.api;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     SmoothedInputDevice.java
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

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import com.diozero.internal.spi.GpioDeviceFactoryInterface;
import com.diozero.sbc.DeviceFactoryHelper;
import com.diozero.util.DiozeroScheduler;

/**
 * <p>
 * Represents a generic input device which takes its value from the number of
 * active events over a specific time period.
 * </p>
 * 
 * <p>
 * This class extends {@link com.diozero.api.DigitalInputDevice
 * DigitalInputDevice} with a queue which is added to whenever the input device
 * is active. The number of the active events in the queue is compared to a
 * threshold which is used to determine the state of the 'active' property.
 * </p>
 * 
 * <p>
 * Any active events over the specified eventAge are removed by a background
 * thread.
 * </p>
 * 
 * <p>
 * This class is intended for use with devices which exhibit "twitchy" behaviour
 * (such as certain motion sensors). It can enable basic debounce functionality
 * by setting threshold to 1, eventAge = -1 and eventDetectPeriod = debounceTime
 * (ms).
 * </p>
 */
public class SmoothedInputDevice extends DigitalInputDevice implements Runnable {
	private static final int DEFAULT_THRESHOLD = 1;
	private static final int DEFAULT_EVENT_DETECT_PERIOD_MS = 10;
	private static final int DEFAULT_EVENT_AGE_MS = 2 * DEFAULT_EVENT_DETECT_PERIOD_MS;

	/**
	 * Smoothed input device builder. Default values:
	 * <dl>
	 * <dt>pud:</dt>
	 * <dd>{@link GpioPullUpDown#NONE}</dd>
	 * <dt>activeHigh:</dt>
	 * <dd>set to false if pud == {@link GpioPullUpDown#PULL_UP}, otherwise true
	 * (assumes normally open wiring configuration)</dd>
	 * <dt>deviceFactory:</dt>
	 * <dd>{@link DeviceFactoryHelper#getNativeDeviceFactory}</dd>
	 * <dt>threshold:</dt>
	 * <dd>1</dd>
	 * <dt>eventAge:</dt>
	 * <dd>20</dd>
	 * <dt>eventDetectPeriodMs:</dt>
	 * <dd>10</dd>
	 * </dl>
	 * 
	 * Either a GPIO number or a {@link PinInfo} instance must be specified. Using a
	 * PinInfo instance allows input devices to be identified by either physical pin
	 * number or GPIO chip and line offset.
	 * 
	 * The optional activeHigh parameter defaults assume a normally open wiring
	 * configuration, however, it can be overridden for normally closed
	 * configurations as well as scenarios where pud is {@link GpioPullUpDown#NONE}
	 * and an external pull up/down resistor is used.
	 */
	public static class Builder {
		public static Builder builder(int gpio) {
			return new Builder(gpio);
		}

		public static Builder builder(PinInfo pinInfo) {
			return new Builder(pinInfo);
		}

		private Integer gpio;
		private PinInfo pinInfo;
		private GpioPullUpDown pud = GpioPullUpDown.NONE;
		private Boolean activeHigh;
		private GpioDeviceFactoryInterface deviceFactory;
		private int threshold = DEFAULT_THRESHOLD;
		private int eventAgeMs = DEFAULT_EVENT_AGE_MS;
		private int eventDetectPeriodMs = DEFAULT_EVENT_DETECT_PERIOD_MS;

		public Builder(int gpio) {
			this.gpio = Integer.valueOf(gpio);
		}

		public Builder(PinInfo pinInfo) {
			this.pinInfo = pinInfo;
		}

		public Builder setPullUpDown(GpioPullUpDown pud) {
			this.pud = pud;
			return this;
		}

		public Builder setActiveHigh(boolean activeHigh) {
			this.activeHigh = Boolean.valueOf(activeHigh);
			return this;
		}

		public Builder setThreshold(int threshold) {
			this.threshold = threshold;
			return this;
		}

		public Builder setEventAgeMs(int eventAgeMs) {
			this.eventAgeMs = eventAgeMs;
			return this;
		}

		public Builder setEventDetectPeriodMs(int eventDetectPeriodMs) {
			this.eventDetectPeriodMs = eventDetectPeriodMs;
			return this;
		}

		public Builder setGpioDeviceFactoryInterface(GpioDeviceFactoryInterface deviceFactory) {
			this.deviceFactory = deviceFactory;
			return this;
		}

		public SmoothedInputDevice build() {
			// Determine activeHigh from pud if not explicitly set
			if (activeHigh == null) {
				activeHigh = Boolean.valueOf(pud != GpioPullUpDown.PULL_UP);
			}

			// Default to the native device factory if not set
			if (deviceFactory == null) {
				deviceFactory = DeviceFactoryHelper.getNativeDeviceFactory();
			}

			if (pinInfo == null) {
				pinInfo = deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(gpio.intValue());
			}

			return new SmoothedInputDevice(deviceFactory, pinInfo, pud, activeHigh.booleanValue(), threshold,
					eventAgeMs, eventDetectPeriodMs);
		}
	}

	private int threshold;
	private int eventAge;
	private int eventDetectPeriod;
	private final Queue<Long> queue;
	// For the event smoothing logic
	private Predicate<Long> removePredicate;
	private long eventCheckTimeMs;
	private boolean currentlyActive;

	/**
	 * @param gpio              GPIO to which the device is connected.
	 * @param pud               Pull up/down configuration, values: NONE, PULL_UP,
	 *                          PULL_DOWN.
	 * @param threshold         The value above which the device will be considered
	 *                          "on".
	 * @param eventAge          The time in milliseconds to keep active events in
	 *                          the queue.
	 * @param eventDetectPeriod How frequently to check for events.
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public SmoothedInputDevice(int gpio, GpioPullUpDown pud, int threshold, int eventAge, int eventDetectPeriod)
			throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), gpio, pud, threshold, eventAge, eventDetectPeriod);
	}

	/**
	 * @param deviceFactory     Device factory to use to provision this device.
	 * @param gpio              GPIO to which the device is connected.
	 * @param pud               Pull up/down configuration, values: NONE, PULL_UP,
	 *                          PULL_DOWN.
	 * @param threshold         The value above which the device will be considered
	 *                          "on".
	 * @param eventAge          The time in milliseconds to keep active events in
	 *                          the queue.
	 * @param eventDetectPeriod How frequently to check for events.
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public SmoothedInputDevice(GpioDeviceFactoryInterface deviceFactory, int gpio, GpioPullUpDown pud, int threshold,
			int eventAge, int eventDetectPeriod) throws RuntimeIOException {
		this(deviceFactory, deviceFactory.getBoardPinInfo().getByGpioNumberOrThrow(gpio), pud,
				pud != GpioPullUpDown.PULL_UP, threshold, eventAge, eventDetectPeriod);
	}

	/**
	 * @param deviceFactory     Device factory to use to provision this device
	 * @param pinInfo           PinInfo instance to which the device is connected
	 * @param pud               Pull up/down configuration, values: NONE, PULL_UP,
	 *                          PULL_DOWN
	 * @param activeHigh        Set to true if digital 1 is to be treated as active
	 * @param threshold         The value above which the device will be considered
	 *                          "on"
	 * @param eventAge          The time in milliseconds to keep active events in
	 *                          the queue
	 * @param eventDetectPeriod How frequently to check for events
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public SmoothedInputDevice(GpioDeviceFactoryInterface deviceFactory, PinInfo pinInfo, GpioPullUpDown pud,
			boolean activeHigh, int threshold, int eventAge, int eventDetectPeriod) throws RuntimeIOException {
		super(deviceFactory, pinInfo, pud, GpioEventTrigger.BOTH, activeHigh);

		this.threshold = threshold;
		this.eventAge = eventAge;
		this.eventDetectPeriod = eventDetectPeriod;

		queue = new LinkedList<>();
		removePredicate = time -> time.longValue() < (eventCheckTimeMs - eventAge);
		DiozeroScheduler.getNonDaemonInstance().scheduleAtFixedRate(this, eventDetectPeriod, eventDetectPeriod,
				TimeUnit.MILLISECONDS);
	}

	@Override
	public void accept(DigitalInputEvent event) {
		event.setActiveHigh(activeHigh);
		// Only add active events to the queue
		if (event.isActive()) {
			synchronized (queue) {
				queue.add(Long.valueOf(event.getEpochTime()));
			}
		}
	}

	@Override
	public void run() {
		long nano_time = System.nanoTime();
		eventCheckTimeMs = System.currentTimeMillis();
		synchronized (queue) {
			// Purge any old events
			queue.removeIf(removePredicate);

			// Check if the number of events exceeds the threshold
			if (currentlyActive) {
				super.accept(new DigitalInputEvent(getGpio(), eventCheckTimeMs, nano_time, !activeHigh));
				currentlyActive = false;
				// If an event is fired then clear the queue of all events
				queue.clear();
			} else if (queue.size() >= threshold) {
				if (!currentlyActive) {
					super.accept(new DigitalInputEvent(getGpio(), eventCheckTimeMs, nano_time, activeHigh));
					currentlyActive = true;

					// If an event is fired then clear the queue of all events
					queue.clear();
				}
			}
		}
	}

	/**
	 * If the number of on events younger than eventAge exceeds this amount, then
	 * 'isActive' will return 'True'.
	 * 
	 * @return event threshold
	 */
	public int getThreshold() {
		return threshold;
	}

	/**
	 * Set the threshold value in terms of number of on events within the specified
	 * time period that will trigger an on event to any listeners.
	 * 
	 * @param threshold New threshold value.
	 */
	public void setThreshold(int threshold) {
		this.threshold = threshold;
	}

	/**
	 * The time in milliseconds to keep items in the queue.
	 * 
	 * @return The event age (milliseconds).
	 */
	public int getEventAge() {
		return eventAge;
	}

	/**
	 * Set the event age (milliseconds).
	 * 
	 * @param eventAge New event age value (milliseconds).
	 */
	public void setEventAge(int eventAge) {
		this.eventAge = eventAge;
	}

	/**
	 * How frequently (in milliseconds) to check the state of the queue.
	 * 
	 * @return The event detection period (milliseconds)
	 */
	public int getEventDetectPeriod() {
		return eventDetectPeriod;
	}
}
