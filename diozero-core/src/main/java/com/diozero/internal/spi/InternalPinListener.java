package com.diozero.internal.spi;

import com.diozero.api.DigitalPinEvent;

public interface InternalPinListener {
	void valueChanged(DigitalPinEvent event);
}
