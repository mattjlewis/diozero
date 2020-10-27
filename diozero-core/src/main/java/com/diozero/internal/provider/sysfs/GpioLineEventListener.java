package com.diozero.internal.provider.sysfs;

public interface GpioLineEventListener {
	void event(int lineFd, int eventDataId, long timestampNanos);
}
