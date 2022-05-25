package com.diozero.internal.provider.firmata;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Firmata
 * Filename:     FirmataDigitalInputOutputDevice.java
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

import com.diozero.api.DeviceMode;
import com.diozero.api.DigitalInputEvent;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.provider.firmata.adapter.FirmataAdapter;
import com.diozero.internal.provider.firmata.adapter.FirmataProtocol.PinMode;
import com.diozero.internal.spi.AbstractInputDevice;
import com.diozero.internal.spi.GpioDigitalInputOutputDeviceInterface;

public class FirmataDigitalInputOutputDevice extends AbstractInputDevice<DigitalInputEvent>
		implements GpioDigitalInputOutputDeviceInterface {
	private FirmataAdapter adapter;
	private int gpio;
	private DeviceMode mode;

	public FirmataDigitalInputOutputDevice(FirmataDeviceFactory deviceFactory, String key, int gpio, DeviceMode mode) {
		super(key, deviceFactory);

		this.gpio = gpio;

		adapter = deviceFactory.getFirmataAdapter();

		setMode(mode);
	}

	@Override
	public void setValue(boolean value) throws RuntimeIOException {
		adapter.setDigitalValue(gpio, value);
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
	public DeviceMode getMode() {
		return mode;
	}

	@Override
	public void setMode(DeviceMode mode) {
		adapter.setPinMode(gpio, mode == DeviceMode.DIGITAL_INPUT ? PinMode.DIGITAL_INPUT : PinMode.DIGITAL_OUTPUT);
	}

	@Override
	public void enableListener() {
		disableListener();

		adapter.enableDigitalReporting(gpio, true);
	}

	@Override
	public void disableListener() {
		adapter.enableDigitalReporting(gpio, true);
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		if (mode == DeviceMode.DIGITAL_OUTPUT) {
			setValue(false);
		}
		// TODO Nothing else to do?
	}
}
