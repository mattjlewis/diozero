package com.diozero.internal.provider.test;

import java.io.IOException;

import org.pmw.tinylog.Logger;

import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;
import com.diozero.util.DioZeroScheduler;
import com.diozero.util.RuntimeIOException;
import com.diozero.util.SleepUtil;

public class HCSR04TriggerPin extends AbstractDevice
implements GpioDigitalOutputDeviceInterface, Runnable {
	private int pinNumber;
	private boolean value;
	private long start;
	
	public HCSR04TriggerPin(String key, DeviceFactoryInterface deviceFactory,
			int pinNumber, boolean initialValue) {
		super(key, deviceFactory);
		
		this.pinNumber = pinNumber;
		this.value = initialValue;
	}
	
	@Override
	public int getPin() {
		return pinNumber;
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		return value;
	}
	
	@Override
	public void setValue(boolean value) throws RuntimeIOException {
		boolean old_value = this.value;
		this.value = value;

		// Start the signal echo process if the trigger pin goes high then low
		if (old_value && ! value) {
			start = System.currentTimeMillis();
			DioZeroScheduler.getDaemonInstance().execute(this);
		}
	}
	
	@Override
	public void run() {
		Logger.debug("run()");
		SleepUtil.sleepMillis(50);
		HCSR04EchoPin.getInstance().doEcho(start);
	}

	@Override
	protected void closeDevice() throws IOException {
	}
}
