package com.diozero.api;

import java.io.IOException;

/**
 * Represents a generic input device with typical on/off behaviour.
 * 
 * This class extends 'WaitableInputDevice' with machinery to fire the active
 * and inactive events for devices that operate in a typical digital manner:
 * straight forward on / off states with (reasonably) clean transitions between
 * the two.
 */
public class DebouncedDigitalInputDevice extends WaitableDigitalInputDevice {
	public DebouncedDigitalInputDevice(int pinNumber) throws IOException {
		this(pinNumber, GpioPullUpDown.NONE, 0, GpioEventTrigger.BOTH);
	}

	/**
	 * 
	 * @param pinNumber
	 * @param pullUp
	 * @param bounceTime
	 *            Specifies the length of time (in seconds) that the component
	 *            will ignore changes in state after an initial change. This
	 *            defaults to 0 which indicates that no bounce compensation will
	 *            be performed.
	 * @throws IOException
	 */
	public DebouncedDigitalInputDevice(int pinNumber, GpioPullUpDown pud, float debounceTime) throws IOException {
		this(pinNumber, pud, debounceTime, GpioEventTrigger.BOTH);
	}
	
	public DebouncedDigitalInputDevice(int pinNumber, GpioPullUpDown pud, float debounceTime, GpioEventTrigger trigger) throws IOException {
		super(pinNumber, pud, trigger);

		if (debounceTime > 0) {
			device.setDebounceTimeMillis((int) (debounceTime * 1000));
		}
	}

	// Exposed operations
	public void setDebounceTime(float debounceTime) {
		device.setDebounceTimeMillis((int) (debounceTime * 1000));
	}
}
