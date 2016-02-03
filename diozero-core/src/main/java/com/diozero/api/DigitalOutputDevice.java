package com.diozero.api;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.internal.spi.GpioDeviceFactoryInterface;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;
import com.diozero.util.SleepUtil;

public class DigitalOutputDevice extends GpioDevice {
	private static final Logger logger = LogManager.getLogger(DigitalOutputDevice.class);
	
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
		logger.debug("close()");
		stopOnOffLoop();
		try {
			device.setValue(!activeHigh);
			device.close();
		} catch (IOException e) {
			logger.error("Error closing device: " + e, e);
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
