package com.diozero.internal.provider.piconzero;

import org.pmw.tinylog.Logger;

import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.GpioAnalogInputDeviceInterface;
import com.diozero.sandpit.PiconZero;
import com.diozero.util.RuntimeIOException;

public class PiconZeroAnalogInputDevice extends AbstractDevice implements GpioAnalogInputDeviceInterface {
	private PiconZero piconZero;
	private int channel;

	public PiconZeroAnalogInputDevice(PiconZero piconZero, String key, int channel) {
		super(key, piconZero);
		
		this.piconZero = piconZero;
		this.channel = channel;
	}

	@Override
	public void closeDevice() {
		Logger.debug("closeDevice()");
		piconZero.closeChannel(channel);
	}

	@Override
	public float getValue() throws RuntimeIOException {
		return piconZero.getValue(channel);
	}

	@Override
	public int getPin() {
		return channel;
	}
}
