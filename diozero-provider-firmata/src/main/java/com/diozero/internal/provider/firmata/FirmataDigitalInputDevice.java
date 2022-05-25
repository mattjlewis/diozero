package com.diozero.internal.provider.firmata;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Firmata
 * Filename:     FirmataDigitalInputDevice.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2022 diozero
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

import org.tinylog.Logger;

import com.diozero.api.DigitalInputEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.provider.firmata.adapter.FirmataAdapter;
import com.diozero.internal.provider.firmata.adapter.FirmataProtocol.PinMode;
import com.diozero.internal.spi.AbstractInputDevice;
import com.diozero.internal.spi.GpioDigitalInputDeviceInterface;

public class FirmataDigitalInputDevice extends AbstractInputDevice<DigitalInputEvent>
		implements GpioDigitalInputDeviceInterface {
	private FirmataAdapter adapter;
	private int gpio;

	public FirmataDigitalInputDevice(FirmataDeviceFactory deviceFactory, String key, int gpio, GpioPullUpDown pud,
			GpioEventTrigger trigger) {
		super(key, deviceFactory);

		this.gpio = gpio;

		adapter = deviceFactory.getFirmataAdapter();
		adapter.setPinMode(gpio, pud == GpioPullUpDown.PULL_UP ? PinMode.INPUT_PULLUP : PinMode.DIGITAL_INPUT);
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		return adapter.getDigitalValue(gpio);
	}

	@Override
	public int getGpio() {
		return gpio;
	}

	@Override
	public void setDebounceTimeMillis(int debounceTime) {
		throw new UnsupportedOperationException("Debounce not supported");
	}

	@Override
	public void enableListener() {
		adapter.enableDigitalReporting(gpio, true);
	}

	@Override
	public void disableListener() {
		adapter.enableDigitalReporting(gpio, false);
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.trace("closeDevice()");
		disableListener();
	}
}
