package com.diozero.internal.provider.bbbiolib;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - BBBioLib
 * Filename:     BbbIoLibDigitalOutputDevice.java
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

import com.diozero.api.PinInfo;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;

public class BbbIoLibDigitalOutputDevice extends AbstractDevice implements GpioDigitalOutputDeviceInterface {
	private PinInfo pinInfo;

	public BbbIoLibDigitalOutputDevice(BbbIoLibDeviceFactory deviceFactory, String key, PinInfo pinInfo,
			boolean initialValue) {
		super(key, deviceFactory);

		this.pinInfo = pinInfo;

		BbbIoLibNative.setDir(BbbIoLibDeviceFactory.getPort(pinInfo), (byte) pinInfo.getPhysicalPin(),
				BbbIoLibNative.BBBIO_DIR_OUT);

		setValue(initialValue);
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

		return rc == 1;
	}

	@Override
	public void setValue(boolean value) throws RuntimeIOException {
		BbbIoLibNative.setValue(BbbIoLibDeviceFactory.getPort(pinInfo), (byte) pinInfo.getPhysicalPin(), value);
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.trace("closeDevice()");
		// Revert to input mode?
		BbbIoLibNative.setDir(BbbIoLibDeviceFactory.getPort(pinInfo), (byte) pinInfo.getPhysicalPin(),
				BbbIoLibNative.BBBIO_DIR_IN);
	}
}
