package com.diozero.internal.provider.mmap;

/*
 * #%L
 * Device I/O Zero - Java Native provider for the Raspberry Pi
 * %%
 * Copyright (C) 2016 mattjlewis
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
import com.diozero.internal.provider.*;
import com.diozero.internal.provider.sysfs.SysFsDeviceFactory;
import com.diozero.util.LibraryLoader;
import com.diozero.util.RuntimeIOException;

public class MmapDeviceFactory extends BaseNativeDeviceFactory {
	private MmapGpioInterface mmapGpio;
	private SysFsDeviceFactory sysFsDeviceFactory;
	
	public MmapDeviceFactory() {
		LibraryLoader.loadLibrary(MmapDeviceFactory.class, "diozerommap");
		
		mmapGpio = getBoardInfo().createMmapGpio();
		if (mmapGpio == null) {
			throw new RuntimeException("Memory mapped GPIO is not supported on board " + getBoardInfo());
		}
		
		mmapGpio.initialise();
		sysFsDeviceFactory = new SysFsDeviceFactory();
	}
	
	MmapGpioInterface getMmapGpio() {
		return mmapGpio;
	}
	
	SysFsDeviceFactory getSysFsDeviceFactory() {
		return sysFsDeviceFactory;
	}
	
	@Override
	public void close() {
		super.close();
		mmapGpio.close();
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	public int getBoardPwmFrequency() {
		throw new UnsupportedOperationException("PWM not yet supported");
	}

	@Override
	public void setBoardPwmFrequency(int pwmFrequency) {
		throw new UnsupportedOperationException("PWM not yet supported");
	}

	@Override
	public GpioDigitalInputDeviceInterface createDigitalInputDevice(String key, PinInfo pinInfo, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws RuntimeIOException {
		return new MmapDigitalInputDevice(this, key, pinInfo, pud, trigger);
	}

	@Override
	public GpioDigitalOutputDeviceInterface createDigitalOutputDevice(String key, PinInfo pinInfo, boolean initialValue)
			throws RuntimeIOException {
		return new MmapDigitalOutputDevice(this, key, pinInfo, initialValue);
	}

	@Override
	public GpioDigitalInputOutputDeviceInterface createDigitalInputOutputDevice(String key, PinInfo pinInfo, DeviceMode mode)
			throws RuntimeIOException {
		return new MmapDigitalInputOutputDevice(this, key, pinInfo, mode);
	}

	@Override
	public PwmOutputDeviceInterface createPwmOutputDevice(String key, PinInfo pinInfo, int pwmFrequency,
			float initialValue) throws RuntimeIOException {
		return sysFsDeviceFactory.createPwmOutputDevice(key, pinInfo, pwmFrequency, initialValue);
	}

	@Override
	public AnalogInputDeviceInterface createAnalogInputDevice(String key, PinInfo pinInfo) throws RuntimeIOException {
		return sysFsDeviceFactory.createAnalogInputDevice(key, pinInfo);
	}

	@Override
	public AnalogOutputDeviceInterface createAnalogOutputDevice(String key, PinInfo pinInfo) throws RuntimeIOException {
		return sysFsDeviceFactory.createAnalogOutputDevice(key, pinInfo);
	}

	@Override
	protected SpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect, int frequency,
			SpiClockMode spiClockMode, boolean lsbFirst) throws RuntimeIOException {
		return sysFsDeviceFactory.createSpiDevice(key, controller, chipSelect, frequency, spiClockMode, lsbFirst);
	}

	@Override
	protected I2CDeviceInterface createI2CDevice(String key, int controller, int address, int addressSize,
			int clockFrequency) throws RuntimeIOException {
		return sysFsDeviceFactory.createI2CDevice(key, controller, address, addressSize, clockFrequency);
	}
}
