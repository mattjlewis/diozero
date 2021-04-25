package com.diozero.internal.provider.bbbiolib;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - BBBioLib
 * Filename:     BbbIoLibDigitalInputDevice.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2021 diozero
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
import com.diozero.api.PinInfo;
import com.diozero.api.RuntimeIOException;
import com.diozero.api.function.DeviceEventConsumer;
import com.diozero.internal.spi.AbstractInputDevice;
import com.diozero.internal.spi.GpioDigitalInputDeviceInterface;

public class BbbIoLibDigitalInputDevice extends AbstractInputDevice<DigitalInputEvent>
		implements GpioDigitalInputDeviceInterface, DeviceEventConsumer<DigitalInputEvent> {
	private PinInfo pinInfo;
	private GpioDigitalInputDeviceInterface defaultDigitialInput;

	public BbbIoLibDigitalInputDevice(BbbIoLibDeviceFactory deviceFactory, String key, PinInfo pinInfo,
			GpioPullUpDown pud, GpioEventTrigger trigger) {
		super(key, deviceFactory);

		this.pinInfo = pinInfo;

		// pud?
		int rc = BbbIoLibNative.setDir(BbbIoLibDeviceFactory.getPort(pinInfo), (byte) pinInfo.getPhysicalPin(),
				BbbIoLibNative.BBBIO_DIR_IN);
		if (rc < 0) {
			throw new RuntimeIOException(
					"Error in BBBioLib.setDir(" + getGpio() + ", " + BbbIoLibNative.BBBIO_DIR_IN + ")");
		}

		defaultDigitialInput = deviceFactory.getDefaultDeviceFactory().provisionDigitalInputDevice(pinInfo, pud,
				trigger);
	}

	@Override
	public int getGpio() {
		return pinInfo.getDeviceNumber();
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		int rc = BbbIoLibNative.getValue(BbbIoLibDeviceFactory.getPort(pinInfo), (byte) pinInfo.getPhysicalPin());
		if (rc < 0) {
			throw new RuntimeIOException("Error in BBBioLib.getValue(" + getGpio() + ")");
		}

		return rc != 0;
	}

	@Override
	public void setDebounceTimeMillis(int debounceTime) {
		throw new UnsupportedOperationException("Debounce not supported in pigpioj");
	}

	@Override
	protected void enableListener() {
		defaultDigitialInput.setListener(this);
	}

	@Override
	protected void disableListener() {
		defaultDigitialInput.removeListener();
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.trace("closeDevice()");
		disableListener();
		if (defaultDigitialInput != null) {
			defaultDigitialInput.close();
			defaultDigitialInput = null;
		}
	}
}
