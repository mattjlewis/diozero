package com.diozero.internal.provider.firmata;

import java.io.IOException;

import org.firmata4j.IOEvent;
import org.firmata4j.Pin;
import org.firmata4j.Pin.Mode;
import org.firmata4j.PinEventListener;
import org.pmw.tinylog.Logger;

import com.diozero.api.DigitalInputEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.internal.spi.AbstractInputDevice;
import com.diozero.internal.spi.GpioDigitalInputDeviceInterface;
import com.diozero.util.RuntimeIOException;

public class FirmataDigitalInputDevice extends AbstractInputDevice<DigitalInputEvent>
implements GpioDigitalInputDeviceInterface, PinEventListener {
	private Pin pin;

	public FirmataDigitalInputDevice(FirmataDeviceFactory deviceFactory, String key, int deviceNumber,
			GpioPullUpDown pud, GpioEventTrigger trigger) {
		super(key, deviceFactory);
		
		pin = deviceFactory.getIoDevice().getPin(deviceNumber);
		try {
			pin.setMode(Mode.INPUT);
		} catch (IOException e) {
			throw new RuntimeIOException("Error setting pin mode to input for pin " + deviceNumber);
		}
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		return pin.getValue() != 0;
	}

	@Override
	public int getGpio() {
		return pin.getIndex();
	}

	@Override
	public void setDebounceTimeMillis(int debounceTime) {
		throw new UnsupportedOperationException("Debounce not supported");
	}

	@Override
	public void enableListener() {
		disableListener();
		
		pin.addEventListener(this);
	}
	
	@Override
	public void disableListener() {
		pin.removeEventListener(this);
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.info("closeDevice()");
		disableListener();
	}

	@Override
	public void onModeChange(IOEvent event) {
		Logger.warn("Mode changed from digital input to ?");
	}

	@Override
	public void onValueChange(IOEvent event) {
		valueChanged(new DigitalInputEvent(pin.getIndex(), event.getTimestamp(), System.nanoTime(), event.getValue() != 0));
	}
}
