package com.diozero.api;

@FunctionalInterface
public interface InputEventListener<T extends DeviceEvent> {
	void valueChanged(T event);
}
