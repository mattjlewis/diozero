package com.diozero.internal.provider.sysfs;

/*
 * #%L
 * Device I/O Zero - Java Sysfs provider
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


import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;

import org.pmw.tinylog.Logger;

import com.diozero.api.DigitalInputEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.internal.spi.AbstractInputDevice;
import com.diozero.internal.spi.GpioDigitalInputDeviceInterface;
import com.diozero.util.*;

/**
 * Pure Java implementation using <a href="https://www.kernel.org/doc/Documentation/gpio/sysfs.txt">the sysfs (/sys/class/gpio)</a> kernel module.
 */
public class SysFsDigitalInputDevice extends AbstractInputDevice<DigitalInputEvent>
implements GpioDigitalInputDeviceInterface, PollEventListener {
	private static final String EDGE_FILE = "edge";
	private static final String VALUE_FILE = "value";
	//private static final byte LOW_VALUE = '0';
	private static final byte HIGH_VALUE = '1';
	
	// Note java.nio.file.WatchService doesn't work with /sys, /proc and network file-systems
	// http://stackoverflow.com/questions/30190730/nio-watchservice-for-unix-sys-classes-gpio-files
	private EpollNative epollNative;
	
	private SysFsDeviceFactory deviceFactory;
	private int gpio;
	private Path valuePath;
	private RandomAccessFile valueFile;

	public SysFsDigitalInputDevice(SysFsDeviceFactory deviceFactory, Path gpioDir,
			String key, int gpio, GpioEventTrigger trigger) {
		super(key, deviceFactory);
		
		this.deviceFactory = deviceFactory;
		this.gpio = gpio;
		epollNative = new EpollNative();

		try (FileWriter writer = new FileWriter(gpioDir.resolve(EDGE_FILE).toFile())) {
			writer.write(trigger.name().toLowerCase());
		} catch (IOException e) {
			Logger.warn(e, "Error writing to edge file for GPIO {}: {}", Integer.valueOf(gpio), e);
		}
		
		// Note: Not possible to set pull-up/down resistor configuration via /sys/class/gpio
		
		valuePath = gpioDir.resolve(VALUE_FILE);
		try {
			valueFile = new RandomAccessFile(valuePath.toFile(), "r");
		} catch (IOException e) {
			throw new RuntimeIOException("Error opening value file for GPIO " + gpio, e);
		}
	}

	@Override
	public int getGpio() {
		return gpio;
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		try {
			valueFile.seek(0);
			return valueFile.readByte() == HIGH_VALUE;
		} catch (IOException e) {
			throw new RuntimeIOException("Error reading value", e);
		}
	}

	@Override
	public void setDebounceTimeMillis(int debounceTime) {
		throw new UnsupportedOperationException("Debounce not supported");
	}

	@Override
	public void enableListener() {
		epollNative.register(valuePath.toString(), Integer.valueOf(gpio), this);
		DioZeroScheduler.getDaemonInstance().execute(epollNative::processEvents);
	}

	@Override
	public void disableListener() {
		epollNative.deregister(valuePath.toString());
		epollNative.stop();
	}

	@Override
	public void closeDevice() throws RuntimeIOException {
		Logger.debug("closeDevice()");
		disableListener();
		epollNative.close();
		try {
			valueFile.close();
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
		deviceFactory.unexport(gpio);
	}

	@Override
	public void notify(Object ref, long epochTime, char value) {
		valueChanged(new DigitalInputEvent(gpio, epochTime, System.nanoTime(), value == HIGH_VALUE));
	}
}
