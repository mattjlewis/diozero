package com.diozero.api;

import java.io.IOException;

/**
 * Represents a generic input device with distinct waitable states.
 * 
 * This class extends 'InputDevice' with methods for waiting on the device's
 * status ('wait_for_active' and 'wait_for_inactive'), and properties that hold
 * functions to be called when the device changes state ('when_activated' and
 * 'when_deactivated'). These are aliased appropriately in various subclasses.
 * 
 * Note that this class provides no means of actually firing its events; it's
 * effectively an abstract base class.
 */
public class WaitableDigitalInputDevice extends DigitalInputDevice {
	protected DigitalPinEvent lastPinEvent;

	public WaitableDigitalInputDevice(int pinNumber, GpioPullUpDown pud, GpioEventTrigger trigger) throws IOException {
		super(pinNumber, pud, trigger);
	}

	public void waitForActive() {
		waitForActive(0);
	}

	public void waitForActive(float timeoutSec) {
		waitForValue(activeHigh, timeoutSec);
	}

	public void waitForInactive() {
		waitForInactive(0);
	}

	public void waitForInactive(float timeoutSec) {
		waitForValue(!activeHigh, timeoutSec);
	}

	protected void waitForValue(boolean value, float timeoutSec) {
		long start = System.currentTimeMillis();
		long wait_for_millis = (long) (timeoutSec * 1000);
		synchronized (this) {
			do {
				try {
					if (timeoutSec > 0) {
						wait(wait_for_millis);
					} else {
						wait();
					}
				} catch (InterruptedException ie) {
				}
				if (lastPinEvent.getValue() == value) {
					break;
				}
			} while ((System.currentTimeMillis() - start) < wait_for_millis);
		}
	}

	@Override
	public void valueChanged(DigitalPinEvent event) {
		synchronized (this) {
			lastPinEvent = event;
			notify();
		}
		
		super.valueChanged(event);
	}
}
