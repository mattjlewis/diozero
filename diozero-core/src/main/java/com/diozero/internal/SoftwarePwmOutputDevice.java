package com.diozero.internal;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.tinylog.Logger;

import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;
import com.diozero.internal.spi.InternalPwmOutputDeviceInterface;
import com.diozero.util.DiozeroScheduler;
import com.diozero.util.SleepUtil;

/**
 * Generate a very poor approximation of a PWM signal - use at your own risk!
 * All timing is in milliseconds hence it is strongly recommend to use a
 * frequency of 50Hz to minimise integer rounding errors.
 */
public class SoftwarePwmOutputDevice extends AbstractDevice implements InternalPwmOutputDeviceInterface {
	private final GpioDigitalOutputDeviceInterface digitalOutputDevice;
	private final AtomicBoolean running = new AtomicBoolean(false);

	private final AtomicInteger periodNs = new AtomicInteger();
	private final AtomicInteger dutyNs = new AtomicInteger();
	private Future<?> future;

	public SoftwarePwmOutputDevice(String key, DeviceFactoryInterface deviceFactory,
			GpioDigitalOutputDeviceInterface digitalOutputDevice, int frequencyHz, float initialValue) {
		super(key, deviceFactory);

		Logger.warn("Hardware PWM not available for device {}, reverting to software", key);

		this.digitalOutputDevice = digitalOutputDevice;
		digitalOutputDevice.setChild(true);

		periodNs.set(Math.round(1_000_000_000f / frequencyHz));
		setValue(initialValue);
		start();
	}

	public void start() {
		if (!running.getAndSet(true)) {
			future = DiozeroScheduler.getNonDaemonInstance().submit(this::dutyLoop);
		}
	}

	public void stop() {
		if (running.getAndSet(false)) {
			if (future == null) {
				Logger.warn("Unexpected condition - future was null when stopping PWM output");
			} else {
				// Wait for the runnable to complete
				try {
					// Give it the period plus an additional 10ms grace to stop
					future.get(periodNs.get() + 10_000_000, TimeUnit.NANOSECONDS);
				} catch (InterruptedException | ExecutionException | TimeoutException e) {
					Logger.debug(e, "Error waiting for future to complete: {}", e);
					// Cancel the future if it doesn't complete normally by setting running to false
					future.cancel(true);
				}
				future = null;
			}
		}
	}

	private void dutyLoop() {
		long start_ns;

		while (running.get()) {
			start_ns = System.nanoTime();

			// So the value doesn't change mid-iteration
			int dutyCycle = dutyNs.get();
			int perdiodCycle = periodNs.get();

			if (dutyCycle == 0) {
				// Fully off
				digitalOutputDevice.setValue(false);
			}
			else if (dutyCycle == perdiodCycle) {
				// Fully on
				digitalOutputDevice.setValue(true);
			}
			else {
				digitalOutputDevice.setValue(true);
				 SleepUtil.busySleep(dutyCycle);
				digitalOutputDevice.setValue(false);
			}

			// Minimum sleep time is 0.1ms
			SleepUtil.busySleep(Math.max(100_000, perdiodCycle - (System.nanoTime() - start_ns)));
		}
	}

	@Override
	protected void closeDevice() {
		Logger.trace("closeDevice() {}", getKey());
		stop();
		// The diozero shutdown handler closes devices in an arbitrary order
		if (digitalOutputDevice.isOpen()) {
			digitalOutputDevice.close();
		}
	}

	@Override
	public int getGpio() {
		return digitalOutputDevice.getGpio();
	}

	@Override
	public int getPwmNum() {
		return digitalOutputDevice.getGpio();
	}

	@Override
	public float getValue() {
		return dutyNs.get() / (float) periodNs.get();
	}

	@Override
	public void setValue(float value) {
		dutyNs.set((int) Math.floor(value * periodNs.get()));
	}

	@Override
	public int getPwmFrequency() {
		return 1_000_000_000 / periodNs.get();
	}

	@Override
	public void setPwmFrequency(int frequencyHz) {
		// Save the current value
		float current_value = getValue();
		periodNs.set(Math.round(1_000_000_000f / frequencyHz));
		// Restore the equivalent value
		dutyNs.set(Math.round(current_value * periodNs.get()));
	}
}
