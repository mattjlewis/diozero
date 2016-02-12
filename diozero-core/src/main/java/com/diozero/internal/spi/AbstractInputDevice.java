package com.diozero.internal.spi;

import com.diozero.api.DeviceEvent;
import com.diozero.api.InputEventListener;

public abstract class AbstractInputDevice<T extends DeviceEvent> extends AbstractDevice {
	private InputEventListener<T> listener;

	public AbstractInputDevice(String key, DeviceFactoryInterface deviceFactory) {
		super(key, deviceFactory);
	}
	
	public void valueChanged(T event) {
		if (listener != null) {
			listener.valueChanged(event);
		}
	}

	public final void setListener(InputEventListener<T> listener) {
		this.listener = listener;
		enableListener();
	}

	public final void removeListener() {
		disableListener();
		listener = null;
	}
	
	protected void enableListener() {
	}
	
	protected void disableListener() {
	}
}
