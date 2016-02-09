package com.diozero.api;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 diozero
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
import java.util.logging.LogManager;

import org.pmw.tinylog.Logger;

import com.diozero.internal.spi.GpioDeviceFactoryInterface;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;
import com.diozero.util.SleepUtil;

public class DigitalOutputDevice extends GpioDevice {
	public static final int INFINITE_ITERATIONS = -1;
	
	private boolean activeHigh;
	private boolean running;
	private Thread backgroundThread;
	private GpioDigitalOutputDeviceInterface device;
	
	public DigitalOutputDevice(int pinNumber) throws IOException {
		this(pinNumber, true, false);
	}
	
	public DigitalOutputDevice(int pinNumber, boolean activeHigh, boolean initialValue) throws IOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), pinNumber, activeHigh, initialValue);
	}
	
	public DigitalOutputDevice(GpioDeviceFactoryInterface deviceFactory, int pinNumber, boolean activeHigh, boolean initialValue) throws IOException {
		this(deviceFactory.provisionDigitalOutputPin(pinNumber, activeHigh & initialValue), activeHigh);
	}

	public DigitalOutputDevice(GpioDigitalOutputDeviceInterface device, boolean activeHigh) {
		super(device.getPin());
		this.device = device;
		this.activeHigh = activeHigh;
	}

	@Override
	public void close() {
		Logger.debug("close()");
		stopOnOffLoop();
		try {
			device.setValue(!activeHigh);
			device.close();
		} catch (IOException e) {
			Logger.error(e, "Error closing device: {}", e);
		}
	}
	
	protected void onOffLoop(float onTime, float offTime, int n, boolean background) throws IOException {
		stopOnOffLoop();
		if (background) {
			//CompletableFuture future = CompletableFuture.supplyAsync;
			backgroundThread = new Thread("DIO-Zero Digital Output On-Off Loop pin: " + pinNumber) {
				@Override
				public void run() {
					try {
						DigitalOutputDevice.this.onOffLoop(onTime, offTime, n);
					} catch (IOException e) {
						// TODO Auto-generated catch block - what to do?!
						e.printStackTrace();
					}
				}
			};
			backgroundThread.setDaemon(true);
			backgroundThread.start();
		} else {
			onOffLoop(onTime, offTime, n);
		}
	}
	
	private void onOffLoop(float onTime, float offTime, int n) throws IOException {
		running = true;
		if (n > 0) {
			for (int i=0; i<n && running; i++) {
				onOff(onTime, offTime);
			}
		} else if (n == INFINITE_ITERATIONS) {
			while (running) {
				onOff(onTime, offTime);
			}
		}
	}

	private void onOff(float onTime, float offTime) throws IOException {
		setValueInternal(activeHigh);
		SleepUtil.sleepSeconds(onTime);
		if (running) {
			setValueInternal(!activeHigh);
			SleepUtil.sleepSeconds(offTime);
		}
	}

	private void stopOnOffLoop() {
		running = false;
	}
	
	// Exposed operations
	public void on() throws IOException {
		stopOnOffLoop();
		setValueInternal(activeHigh);
	}
	
	public void off() throws IOException {
		stopOnOffLoop();
		setValueInternal(!activeHigh);
	}
	
	public void toggle() throws IOException {
		stopOnOffLoop();
		setValueInternal(!device.getValue());
	}

	// Exposed properties
	public boolean isOn() throws IOException {
		return activeHigh == device.getValue();
	}
	
	public void setValue(boolean value) throws IOException {
		stopOnOffLoop();
		setValueInternal(value);
	}
	
	private void setValueInternal(boolean value) throws IOException {
		synchronized (device) {
			device.setValue(value);
		}
	}
}
