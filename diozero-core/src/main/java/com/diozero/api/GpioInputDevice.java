package com.diozero.api;

import java.util.ArrayList;
import java.util.Collection;

public abstract class GpioInputDevice<T extends DeviceEvent> extends GpioDevice implements InputEventListener<T> {
	protected Collection<InputEventListener<T>> listeners;
	
	public GpioInputDevice(int pinNumber) {
		super(pinNumber);
		listeners = new ArrayList<>();
	}
	
	public void addListener(InputEventListener<T> listener) {
		if (! listeners.contains(listener)) {
			listeners.add(listener);
		}
	}
	
	public void removeListener(InputEventListener<T> listener) {
		listeners.remove(listener);
	}
	
	public void removeAllListeners() {
		listeners.clear();
	}
	
	@Override
	public void valueChanged(T event) {
		listeners.forEach(listener -> listener.valueChanged(event));
	}
}
