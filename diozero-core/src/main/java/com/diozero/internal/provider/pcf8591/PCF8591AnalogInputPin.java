package com.diozero.internal.provider.pcf8591;

import org.pmw.tinylog.Logger;

import com.diozero.api.AnalogInputEvent;
import com.diozero.internal.spi.AbstractInputDevice;
import com.diozero.internal.spi.GpioAnalogInputDeviceInterface;
import com.diozero.sandpit.PCF8591;
import com.diozero.util.RuntimeIOException;

public class PCF8591AnalogInputPin extends AbstractInputDevice<AnalogInputEvent> implements GpioAnalogInputDeviceInterface {
	private PCF8591 pcf8591;
	private int pinNumber;

	public PCF8591AnalogInputPin(PCF8591 pcf8591, String key, int pinNumber) {
		super(key, pcf8591);
		
		this.pcf8591 = pcf8591;
		this.pinNumber = pinNumber;
	}

	@Override
	public void closeDevice() {
		Logger.debug("closeDevice()");
		// TODO Nothing to do?
	}

	@Override
	public float getValue() throws RuntimeIOException {
		return pcf8591.getValue(pinNumber);
	}

	@Override
	public int getPin() {
		return pinNumber;
	}
}
