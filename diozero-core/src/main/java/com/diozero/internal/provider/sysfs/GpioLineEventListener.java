package com.diozero.internal.provider.sysfs;

public interface GpioLineEventListener {
	void event(int gpioOffset, int eventDataId, long timestampNanos);
}
