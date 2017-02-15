package com.diozero.internal.provider.jpi;

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
import com.diozero.internal.board.odroid.OdroidBoardInfoProvider;
import com.diozero.internal.board.raspberrypi.RaspberryPiBoardInfoProvider;
import com.diozero.internal.provider.jpi.odroid.OdroidC2MmapGpio;
import com.diozero.internal.provider.jpi.rpi.RPiMmapGpio;
import com.diozero.internal.provider.sysfs.SysFsDeviceFactory;
import com.diozero.internal.provider.sysfs.SysFsI2cDevice;
import com.diozero.internal.spi.*;
import com.diozero.util.LibraryLoader;
import com.diozero.util.RuntimeIOException;

public class JPiDeviceFactory extends BaseNativeDeviceFactory {
	private MmapGpioInterface mmapGpio;
	private SysFsDeviceFactory sysFsDeviceFactory;
	
	public JPiDeviceFactory() {
		LibraryLoader.loadLibrary(JPiDeviceFactory.class, "jpi");
		
		if (boardInfo.sameMakeAndModel(OdroidBoardInfoProvider.ODROID_C2)) {
			mmapGpio = new OdroidC2MmapGpio();
		} else if (boardInfo.getMake().equals(RaspberryPiBoardInfoProvider.MAKE)) {
			mmapGpio = new RPiMmapGpio();
		} else {
			throw new RuntimeException("This provider is currently only supported on Raspberry Pi and Odroid C2 boards");
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
	public void shutdown() {
		super.shutdown();
		mmapGpio.terminate();
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	public int getPwmFrequency(int gpio) {
		throw new UnsupportedOperationException("PWM not yet supported");
	}

	@Override
	public void setPwmFrequency(int gpio, int pwmFrequency) {
		throw new UnsupportedOperationException("PWM not yet supported");
	}

	@Override
	public GpioDigitalInputDeviceInterface createDigitalInputDevice(String key, PinInfo pinInfo, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws RuntimeIOException {
		return new JPiDigitalInputDevice(this, key, pinInfo.getDeviceNumber(), pud, trigger);
	}

	@Override
	public GpioDigitalOutputDeviceInterface createDigitalOutputDevice(String key, PinInfo pinInfo, boolean initialValue)
			throws RuntimeIOException {
		return new JPiDigitalOutputDevice(this, key, pinInfo.getDeviceNumber(), initialValue);
	}

	@Override
	public GpioDigitalInputOutputDeviceInterface createDigitalInputOutputDevice(String key, PinInfo pinInfo, DeviceMode mode)
			throws RuntimeIOException {
		return new JPiDigitalInputOutputDevice(this, key, pinInfo.getDeviceNumber(), mode);
	}

	@Override
	public PwmOutputDeviceInterface createPwmOutputDevice(String key, PinInfo pinInfo, float initialValue) throws RuntimeIOException {
		throw new UnsupportedOperationException("PWM not yet supported");
	}

	@Override
	public AnalogInputDeviceInterface createAnalogInputDevice(String key, PinInfo pinInfo) throws RuntimeIOException {
		throw new UnsupportedOperationException("Analog input not supported");
	}

	@Override
	public AnalogOutputDeviceInterface createAnalogOutputDevice(String key, PinInfo pinInfo) throws RuntimeIOException {
		throw new UnsupportedOperationException("Analog devices aren't supported on this device");
	}

	@Override
	protected SpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect, int frequency,
			SpiClockMode spiClockMode, boolean lsbFirst) throws RuntimeIOException {
		throw new UnsupportedOperationException("SPI not yet supported");
	}

	@Override
	protected I2CDeviceInterface createI2CDevice(String key, int controller, int address, int addressSize,
			int clockFrequency) throws RuntimeIOException {
		// FIXME Remove this duplicate class!
		return new SysFsI2cDevice(this, key, controller, address, addressSize, clockFrequency);
	}
}
