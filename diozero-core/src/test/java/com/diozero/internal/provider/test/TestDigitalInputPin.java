package com.diozero.internal.provider.test;

import java.io.IOException;

import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.internal.spi.*;

public class TestDigitalInputPin extends AbstractDevice implements GpioDigitalInputDeviceInterface {
	private int pinNumber;

	public TestDigitalInputPin(String key, DeviceFactoryInterface deviceFactory, int pinNumber, GpioPullUpDown pud, GpioEventTrigger trigger) {
		super(key, deviceFactory);
		
		this.pinNumber = pinNumber;
	}

	@Override
	public boolean getValue() throws IOException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getPin() {
		// TODO Auto-generated method stub
		return pinNumber;
	}

	@Override
	public void setDebounceTimeMillis(int debounceTime) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setListener(InternalPinListener listener) {
		// TODO Auto-generated method stub
	}

	@Override
	public void closeDevice() throws IOException {
		// TODO Auto-generated method stub
	}

	@Override
	public void removeListener() {
		// TODO Auto-generated method stub
	}
}
