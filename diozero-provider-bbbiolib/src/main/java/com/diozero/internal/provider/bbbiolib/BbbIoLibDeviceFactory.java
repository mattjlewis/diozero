package com.diozero.internal.provider.bbbiolib;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - BBBioLib
 * Filename:     BbbIoLibDeviceFactory.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
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
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.PinInfo;
import com.diozero.api.SerialDevice;
import com.diozero.api.SpiClockMode;
import com.diozero.internal.board.beaglebone.BeagleBoneBoardInfoProvider.BeagleBoneBlackBoardInfo;
import com.diozero.internal.provider.AnalogInputDeviceInterface;
import com.diozero.internal.provider.AnalogOutputDeviceInterface;
import com.diozero.internal.provider.BaseNativeDeviceFactory;
import com.diozero.internal.provider.GpioDigitalInputDeviceInterface;
import com.diozero.internal.provider.GpioDigitalInputOutputDeviceInterface;
import com.diozero.internal.provider.GpioDigitalOutputDeviceInterface;
import com.diozero.internal.provider.I2CDeviceInterface;
import com.diozero.internal.provider.PwmOutputDeviceInterface;
import com.diozero.internal.provider.SerialDeviceInterface;
import com.diozero.internal.provider.SpiDeviceInterface;
import com.diozero.internal.provider.sysfs.DefaultDeviceFactory;
import com.diozero.internal.provider.sysfs.I2CDeviceWrapper;
import com.diozero.internal.provider.sysfs.NativeSpiDeviceWrapper;
import com.diozero.util.RuntimeIOException;

public class BbbIoLibDeviceFactory extends BaseNativeDeviceFactory {
	private int boardPwmFrequency;
	private DefaultDeviceFactory defaultDeviceFactory;

	public BbbIoLibDeviceFactory() {
		int rc = BbbIoLibNative.init();
		if (rc < 0) {
			throw new RuntimeIOException("Error in BBBioLib.init()");
		}
		defaultDeviceFactory = new DefaultDeviceFactory();
	}
	
	@Override
	public void start() {
		defaultDeviceFactory.start();
	}

	DefaultDeviceFactory getDefaultDeviceFactory() {
		return defaultDeviceFactory;
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	public void shutdown() {
		BbbIoLibNative.shutdown();
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
		// return new BbbIoLibPwmOutputDevice(this, key, pinInfo, initialValue);
	}

	@Override
	public AnalogInputDeviceInterface createAnalogInputDevice(String key, PinInfo pinInfo) {
		throw new UnsupportedOperationException("Analog input support not yet implemented");
	}

	@Override
	public AnalogOutputDeviceInterface createAnalogOutputDevice(String key, PinInfo pinInfo, float initialValue) {
		throw new UnsupportedOperationException("Analog output not supported");
	}

	@Override
	public SpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect, int frequency,
			SpiClockMode spiClockMode, boolean lsbFirst) throws RuntimeIOException {
		return new NativeSpiDeviceWrapper(this, key, controller, chipSelect, frequency, spiClockMode, lsbFirst);
	}

	@Override
	public I2CDeviceInterface createI2CDevice(String key, int controller, int address, int addressSize,
			int clockFrequency) throws RuntimeIOException {
		return new I2CDeviceWrapper(this, key, controller, address, addressSize, clockFrequency);
	}

	@Override
	public SerialDeviceInterface createSerialDevice(String key, String deviceName, int baud, SerialDevice.DataBits dataBits,
			SerialDevice.StopBits stopBits, SerialDevice.Parity parity, boolean readBlocking, int minReadChars,
			int readTimeoutMillis) throws RuntimeIOException {
		throw new UnsupportedOperationException("Serial communication not available in the device factory");
	}

	static byte getPort(PinInfo pinInfo) {
		return (byte) (pinInfo.getHeader() == BeagleBoneBlackBoardInfo.P8_HEADER ? 8 : 9);
	}
}
