package com.diozero.internal.provider.piconzero;

import org.pmw.tinylog.Logger;

import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.PwmOutputDeviceInterface;
import com.diozero.sandpit.PiconZero;
import com.diozero.util.RuntimeIOException;

public class PiconZeroPwmOutputDevice extends AbstractDevice implements PwmOutputDeviceInterface {

	private PiconZero piconZero;
	private int channel;
	private float value;

	public PiconZeroPwmOutputDevice(PiconZero piconZero, String key, int channel, float initialValue) {
		super(key, piconZero);
		
		this.piconZero = piconZero;
		this.channel = channel;
	}

	@Override
	public int getPin() {
		return channel;
	}

	@Override
	public float getValue() throws RuntimeIOException {
		return value;
	}

	@Override
	public void setValue(float value) throws RuntimeIOException {
		piconZero.setValue(channel, value);
		this.value = value;
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.debug("closeDevice()");
		piconZero.closeChannel(channel);
	}
}
