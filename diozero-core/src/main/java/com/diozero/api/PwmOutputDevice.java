package com.diozero.api;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.internal.spi.PwmOutputDeviceFactoryInterface;
import com.diozero.internal.spi.PwmOutputDeviceInterface;
import com.diozero.util.SleepUtil;

/**
 * Represent a generic PWM output GPIO.
 * Note the following BCM GPIO pins provide hardware PWM support:
 * 	12 (phys 32, wPi 26), 13 (phys 33, wPi 23), 18 (phys 12, wPi 1), 19 (phys 35, wPi 24)
 * Any other pin will revert to software controlled PWM (not very good)
 */
public class PwmOutputDevice extends GpioDevice {
	private static final Logger logger = LogManager.getLogger(PwmOutputDevice.class);
	
	public static final int INFINITE_ITERATIONS = -1;
	
	private PwmOutputDeviceInterface device;
	private boolean running;
	private Thread backgroundThread;
	private int pinNumber;

	public PwmOutputDevice(int pinNumber) throws IOException {
		this(pinNumber, 0);
	}
	
	public PwmOutputDevice(int pinNumber, float initialValue) throws IOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), pinNumber, initialValue);
	}
	
	public PwmOutputDevice(PwmOutputDeviceFactoryInterface pwmDeviceFactory, int pinNumber,
			float initialValue) throws IOException {
		this(pwmDeviceFactory.provisionPwmOutputPin(pinNumber, initialValue));
	}
	
	public PwmOutputDevice(PwmOutputDeviceInterface device) {
		super(device.getPin());
		this.device = device;
	}

	@Override
	public void close() {
		logger.debug("close()");
		stopLoops();
		if (device != null) {
			device.close();
		}
	}
	
	protected void onOffLoop(float onTime, float offTime, int n, boolean background) throws IOException {
		stopLoops();
		if (background) {
			backgroundThread = new Thread("DIO-Zero PWM Output On-Off Loop pin: " + pinNumber) {
				@Override
				public void run() {
					try {
						PwmOutputDevice.this.onOffLoop(onTime, offTime, n);
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
		if (n > 0) {
			running = true;
			for (int i=0; i<n && running; i++) {
				onOff(onTime, offTime);
			}
			running = false;
		} else if (n == INFINITE_ITERATIONS) {
			running = true;
			while (running) {
				onOff(onTime, offTime);
			}
		}
	}
	
	protected void fadeInOutLoop(float fadeTime, int steps, int iterations, boolean background) throws IOException {
		stopLoops();
		if (background) {
			backgroundThread = new Thread("DIO-Zero PWM Output Fade In-Out Loop pin: " + pinNumber) {
				@Override
				public void run() {
					try {
						PwmOutputDevice.this.fadeInOutLoop(fadeTime, steps, iterations);
					} catch (IOException e) {
						// TODO Auto-generated catch block - what to do?!
						e.printStackTrace();
					}
				}
			};
			backgroundThread.setDaemon(true);
			backgroundThread.start();
		} else {
			fadeInOutLoop(fadeTime, steps, iterations);
		}
	}

	private void fadeInOutLoop(float fadeTime, int steps, int iterations) throws IOException {
		float sleep_time = fadeTime / steps;
		float delta = 1f / steps;
		if (iterations > 0) {
			running = true;
			for (int i=0; i<iterations && running; i++) {
				fadeInOut(sleep_time, delta);
			}
			running = false;
		} else if (iterations == INFINITE_ITERATIONS) {
			running = true;
			while (running) {
				fadeInOut(sleep_time, delta);
			}
		}
	}
	
	private void fadeInOut(float sleepTime, float delta) throws IOException {
		float value = 0;
		while (value <= 1) {
			setValueInternal(value);
			SleepUtil.sleepSeconds(sleepTime);
			value += delta;
		}
		value = 1;
		while (value >= 0) {
			setValueInternal(value);
			SleepUtil.sleepSeconds(sleepTime);
			value -= delta;
		}
	}
	
	protected void stopLoops() {
		// TODO Check if this is called in a separate thread to that which started non-background loops?
		//Thread t = Thread.currentThread();
		running = false;
		if (backgroundThread != null && backgroundThread.isAlive()) {
			backgroundThread.interrupt();
		}
	}

	private void onOff(float onTime, float offTime) throws IOException {
		setValueInternal(1);
		SleepUtil.sleepSeconds(onTime);
		setValueInternal(0);
		SleepUtil.sleepSeconds(offTime);
	}

	private void setValueInternal(float value) throws IOException {
		device.setValue(value);
	}
	
	// Exposed operations
	public void on() throws IOException {
		stopLoops();
		setValueInternal(1);
	}
	
	public void off() throws IOException {
		stopLoops();
		setValueInternal(0);
	}
	
	public void toggle() throws IOException {
		stopLoops();
		setValueInternal(1 - device.getValue());
	}
	
	public boolean isOn() throws IOException {
		return device.getValue() > 0;
	}
	
	public float getValue() throws IOException {
		return device.getValue();
	}

	public void setValue(float value) throws IOException {
		stopLoops();
		setValueInternal(value);
	}
}
