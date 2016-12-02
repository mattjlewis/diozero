package com.diozero.internal.provider.piconzero;

import com.diozero.api.*;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.GpioDigitalInputDeviceInterface;
import com.diozero.sandpit.PiconZero;
import com.diozero.util.RuntimeIOException;

public class PiconZeroDigitalInputDevice extends AbstractDevice implements GpioDigitalInputDeviceInterface {
	private PiconZero piconZero;
	private int channel;

	public PiconZeroDigitalInputDevice(PiconZero piconZero, String key, int channel, GpioPullUpDown pud,
			GpioEventTrigger trigger) {
		super(key, piconZero);
		
		this.piconZero = piconZero;
		this.channel = channel;
	}
	
	@Override
	public void closeDevice() {
		piconZero.closeChannel(channel);
	}
	
	@Override
	public int getPin() {
		return channel;
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		return piconZero.readInput(channel) != 0;
	}

	@Override
	public void setDebounceTimeMillis(int debounceTime) {
		throw new UnsupportedOperationException("Not supported");
	}

	@Override
	public void setListener(InputEventListener<DigitalInputEvent> listener) {
		// TODO Need to implement a polling mechanism
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public void removeListener() {
	}
}
