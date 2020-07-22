package com.diozero.internal.provider.bbbiolib;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - BBBioLib
 * Filename:     BbbIoLibDigitalInputOutputDevice.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 mattjlewis
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


import org.pmw.tinylog.Logger;

import com.diozero.api.*;
import com.diozero.internal.provider.AbstractInputDevice;
import com.diozero.internal.provider.GpioDigitalInputDeviceInterface;
import com.diozero.internal.provider.GpioDigitalInputOutputDeviceInterface;
import com.diozero.util.RuntimeIOException;

public class BbbIoLibDigitalInputOutputDevice extends AbstractInputDevice<DigitalInputEvent>
implements GpioDigitalInputOutputDeviceInterface, InputEventListener<DigitalInputEvent> {
	private PinInfo pinInfo;
	private DeviceMode mode;
	private GpioDigitalInputDeviceInterface sysFsDigitialInput;

	public BbbIoLibDigitalInputOutputDevice(BbbIoLibDeviceFactory deviceFactory, String key, PinInfo pinInfo,
			DeviceMode mode) {
		super(key, deviceFactory);
		
		this.pinInfo = pinInfo;

		sysFsDigitialInput = deviceFactory.getSysFsDeviceFactory().provisionDigitalInputDevice(
				getGpio(), GpioPullUpDown.NONE, GpioEventTrigger.BOTH);
		
		setMode(mode);
	}

	@Override
	public int getGpio() {
		return pinInfo.getDeviceNumber();
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		int rc = BbbIoLibNative.getValue(BbbIoLibDeviceFactory.getPort(pinInfo), (byte) pinInfo.getPinNumber());
		if (rc < 0) {
			throw new RuntimeIOException("Error in BBBioLib.getValue(" + getGpio() + ")");
		}
		
		return rc == 1;
	}

	@Override
	public void setValue(boolean value) throws RuntimeIOException {
		BbbIoLibNative.setValue(BbbIoLibDeviceFactory.getPort(pinInfo), (byte) pinInfo.getPinNumber(), value);
	}

	@Override
	public DeviceMode getMode() {
		return mode;
	}

	@Override
	public void setMode(DeviceMode mode) {
		int rc = BbbIoLibNative.setDir(BbbIoLibDeviceFactory.getPort(pinInfo), (byte) pinInfo.getPinNumber(),
				mode == DeviceMode.DIGITAL_INPUT ? BbbIoLibNative.BBBIO_DIR_IN : BbbIoLibNative.BBBIO_DIR_OUT);
		if (rc < 0) {
			throw new RuntimeIOException("Error setting direction for gpio " + getGpio());
		}
		this.mode = mode;
	}
	
	@Override
	protected void enableListener() {
		sysFsDigitialInput.setListener(this);
	}
	
	@Override
	protected void disableListener() {
		sysFsDigitialInput.removeListener();
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.debug("closeDevice()");
		disableListener();
		// FIXME No BBBioLib close method?
		setMode(DeviceMode.DIGITAL_INPUT);
	}
}
