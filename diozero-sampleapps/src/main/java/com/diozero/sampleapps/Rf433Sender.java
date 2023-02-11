package com.diozero.sampleapps;

import java.util.concurrent.locks.LockSupport;

import com.diozero.internal.spi.MmapGpioInterface;
import com.diozero.sbc.DeviceFactoryHelper;

public class Rf433Sender {
	public static void main(String[] args) {
		int gpio = 26;
		int iterations = 10;
		int delay_ns = 300_000;
		try (MmapGpioInterface mmap_gpio = DeviceFactoryHelper.getNativeDeviceFactory().getBoardInfo()
				.createMmapGpio()) {
			for (int i = 0; i < iterations; i++) {
				mmap_gpio.gpioWrite(gpio, false);
				LockSupport.parkNanos(delay_ns);
				mmap_gpio.gpioWrite(gpio, true);
				LockSupport.parkNanos(delay_ns);
			}
		}
	}
}
