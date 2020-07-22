package com.diozero.internal.provider.bbbiolib;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - BBBioLib
 * Filename:     BbbIoLibDeviceFactory.java  
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


import com.diozero.api.*;
import com.diozero.internal.board.beaglebone.BeagleBoneBoardInfoProvider.BeagleBoneBlackBoardInfo;
import com.diozero.internal.provider.*;
import com.diozero.internal.provider.sysfs.SysFsDeviceFactory;
import com.diozero.internal.provider.sysfs.SysFsI2CDevice;
import com.diozero.internal.provider.sysfs.SysFsSpiDevice;
import com.diozero.util.RuntimeIOException;

public class BbbIoLibDeviceFactory extends BaseNativeDeviceFactory {
	private int boardPwmFrequency;
	private SysFsDeviceFactory sysFsDeviceFactory;
	
	public BbbIoLibDeviceFactory() {
		int rc = BbbIoLibNative.init();
		if (rc < 0) {
			throw new RuntimeIOException("Error in BBBioLib.init()");
		}
		sysFsDeviceFactory = new SysFsDeviceFactory();
	}
	
	SysFsDeviceFactory getSysFsDeviceFactory() {
		return sysFsDeviceFactory;
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}
	
	@Override
	public void close() {
		BbbIoLibNative.shutdown();
		super.close();
	}

	@Override
	public int getBoardPwmFrequency() {
		return boardPwmFrequency;
	}

	@Override
	public void setBoardPwmFrequency(int pwmFrequency) {
		boardPwmFrequency = pwmFrequency;
	}

	@Override
	public GpioDigitalInputDeviceInterface createDigitalInputDevice(String key, PinInfo pinInfo, GpioPullUpDown pud,
			GpioEventTrigger trigger) {
		return new BbbIoLibDigitalInputDevice(this, key, pinInfo, pud, trigger);
	}

	@Override
	public GpioDigitalOutputDeviceInterface createDigitalOutputDevice(String key, PinInfo pinInfo,
			boolean initialValue) {
		return new BbbIoLibDigitalOutputDevice(this, key, pinInfo, initialValue);
	}

	@Override
	public GpioDigitalInputOutputDeviceInterface createDigitalInputOutputDevice(String key, PinInfo pinInfo,
			DeviceMode mode) {
		return new BbbIoLibDigitalInputOutputDevice(this, key, pinInfo, mode);
	}

	@Override
	public PwmOutputDeviceInterface createPwmOutputDevice(String key, PinInfo pinInfo, int pwmFrequency,
			float initialValue) {
		throw new UnsupportedOperationException("PWM output support not yet implemented");
		//return new BbbIoLibPwmOutputDevice(this, key, pinInfo, initialValue);
	}

	@Override
	public AnalogInputDeviceInterface createAnalogInputDevice(String key, PinInfo pinInfo) {
		throw new UnsupportedOperationException("Analog input support not yet implemented");
	}

	@Override
	public AnalogOutputDeviceInterface createAnalogOutputDevice(String key, PinInfo pinInfo) {
		throw new UnsupportedOperationException("Analog output not supported");
	}

	@Override
	public SpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect, int frequency,
			SpiClockMode spiClockMode, boolean lsbFirst) throws RuntimeIOException {
		return new SysFsSpiDevice(this, key, controller, chipSelect, frequency, spiClockMode, lsbFirst);
	}

	@Override
	public I2CDeviceInterface createI2CDevice(String key, int controller, int address, int addressSize,
			int clockFrequency) throws RuntimeIOException {
		return new SysFsI2CDevice(this, key, controller, address, addressSize, clockFrequency);
	}
	
	static byte getPort(PinInfo pinInfo) {
		return (byte) (pinInfo.getHeader() == BeagleBoneBlackBoardInfo.P8_HEADER ? 8 : 9);
	}
}
