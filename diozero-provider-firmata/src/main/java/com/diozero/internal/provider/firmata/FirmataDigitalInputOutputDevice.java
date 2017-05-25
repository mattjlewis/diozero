package com.diozero.internal.provider.firmata;

/*
 * #%L
 * Device I/O Zero - Firmata
 * %%
 * Copyright (C) 2016 - 2017 mattjlewis
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */


import java.io.IOException;

import org.firmata4j.IOEvent;
import org.firmata4j.Pin;
import org.firmata4j.Pin.Mode;
import org.firmata4j.PinEventListener;
import org.pmw.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.DigitalInputEvent;
import com.diozero.internal.provider.AbstractInputDevice;
import com.diozero.internal.provider.GpioDigitalInputOutputDeviceInterface;
import com.diozero.util.RuntimeIOException;

public class FirmataDigitalInputOutputDevice extends AbstractInputDevice<DigitalInputEvent>
implements GpioDigitalInputOutputDeviceInterface, PinEventListener {
	private Pin pin;
	private DeviceMode mode;

	public FirmataDigitalInputOutputDevice(FirmataDeviceFactory deviceFactory, String key, int deviceNumber,
			DeviceMode mode) {
		super(key, deviceFactory);
		
		pin = deviceFactory.getIoDevice().getPin(deviceNumber);
		
		setMode(mode);
	}

	@Override
	public void setValue(boolean value) throws RuntimeIOException {
		try {
			pin.setValue(value ? 1 : 0);
		} catch (IOException e) {
			throw new RuntimeIOException("Error setting output value for pin " + pin.getIndex());
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
	public DeviceMode getMode() {
		return mode;
	}

	@Override
	public void setMode(DeviceMode mode) {
		Mode firmata_mode;
		switch (mode) {
		case DIGITAL_INPUT:
			firmata_mode = Mode.INPUT;
			break;
		case DIGITAL_OUTPUT:
			firmata_mode = Mode.OUTPUT;
			break;
		default:
			throw new IllegalArgumentException("Invalid mode " + mode);
		}
		try {
			pin.setMode(firmata_mode);
			this.mode = mode;
		} catch (IllegalArgumentException | IOException e) {
			throw new RuntimeIOException("Error setting mode to " + mode + " for pin " + pin.getIndex());
		}
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
		if (mode == DeviceMode.DIGITAL_OUTPUT) {
			setValue(false);
		}
		// TODO Nothing else to do?
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
