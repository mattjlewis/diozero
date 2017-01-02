package com.diozero.util;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.pmw.tinylog.Logger;

import com.diozero.internal.spi.*;

public class SoftwarePwmOutputDevice extends AbstractDevice implements PwmOutputDeviceInterface, Runnable {
	static {
		LibraryLoader.loadLibrary(SleepUtil.class, "diozero-system-utils");
	}
	
	private GpioDigitalOutputDeviceInterface digitalOutputDevice;
	private ScheduledFuture<?> future;
	private int periodMs;
	private int dutyNs;
	
	public SoftwarePwmOutputDevice(String key, DeviceFactoryInterface deviceFactory,
			GpioDigitalOutputDeviceInterface digitalOutputDevice, int frequency, float initialValue) {
		super(key, deviceFactory);
		
		this.digitalOutputDevice = digitalOutputDevice;
		
		periodMs = 1_000 / frequency;
		setValue(initialValue);
	}
	
	public void start() {
		future = DioZeroScheduler.getNonDaemonInstance().scheduleAtFixedRate(this, periodMs, periodMs, TimeUnit.MILLISECONDS);
	}
	
	public void stop() {
		if (future != null) {
			future.cancel(true);
		}
	}

	@Override
	public void run() {
		digitalOutputDevice.setValue(true);
		SleepUtil.sleepNanos(0, dutyNs);
		digitalOutputDevice.setValue(false);
	}

	@Override
	public void closeDevice() {
		stop();
		Logger.debug("closeDevice() {}", getKey());
		stop();
		if (digitalOutputDevice != null) {
			digitalOutputDevice.close();
			digitalOutputDevice = null;
		}
	}
	
	@Override
	public float getValue() {
		return dutyNs / (float) TimeUnit.MILLISECONDS.toNanos(periodMs);
	}
	
	@Override
	public void setValue(float value) {
		if (value < 0) {
			value = 0;
		}
		if (value > 1) {
			value = 1;
		}
		dutyNs = (int) (value * TimeUnit.MILLISECONDS.toNanos(periodMs));
	}

	@Override
	public int getGpio() {
		return digitalOutputDevice.getGpio();
	}

	@Override
	public int getPwmNum() {
		return digitalOutputDevice.getGpio();
	}
}
