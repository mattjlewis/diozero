package com.diozero.internal.provider.piconzero;

import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;
import com.diozero.sandpit.PiconZero;

public class PiconZeroDigitalOutputDevice extends AbstractDevice implements GpioDigitalOutputDeviceInterface {
	private PiconZero piconZero;
	private int channel;
	private boolean value;

	public PiconZeroDigitalOutputDevice(PiconZero piconZero, String key, int channel, boolean initialValue) {
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
	public boolean getValue() {
		return value;
	}
	
	@Override
	public void setValue(boolean value) {
		piconZero.setOutput(channel, value ? 1 : 0);
		this.value = value;
	}
}
