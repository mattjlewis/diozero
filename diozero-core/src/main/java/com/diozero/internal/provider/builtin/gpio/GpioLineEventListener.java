package com.diozero.internal.provider.builtin.gpio;

public interface GpioLineEventListener {
	void event(int lineFd, int eventDataId, long timestampNanos);
}
