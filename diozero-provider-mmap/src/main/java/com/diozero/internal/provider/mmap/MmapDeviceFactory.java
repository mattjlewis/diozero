package com.diozero.internal.provider.mmap;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - High performance mmap GPIO control
 * Filename:     MmapDeviceFactory.java  
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
import com.diozero.internal.provider.builtin.DefaultDeviceFactory;
import com.diozero.internal.spi.GpioDigitalInputDeviceInterface;
import com.diozero.internal.spi.GpioDigitalInputOutputDeviceInterface;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;
import com.diozero.internal.spi.MmapGpioInterface;
import com.diozero.util.RuntimeIOException;

public class MmapDeviceFactory extends DefaultDeviceFactory {
	private MmapGpioInterface mmapGpio;
	
	/*-
	 * Note this cannot be called in the constructor as createMmapGpio() calls LibraryLoader.loadLibrary() which in turn
	 * calls DeviceFactoryHelper.getNativeDeviceFactory().getBoardInfo().getLibraryPath() which causes an infinite loop
	 * as this class is constructed by DeviceFactoryHelper.getNativeDeviceFactory() .
	 * FIXME Update - this issue is fixed now and shouldn't be a problem as LibraryLoader no longer calls getNativeDeviceFactory()
	 * 
	 * Thoughts on a generic mmap driver: https://github.com/torvalds/linux/blob/master/drivers/gpio/gpio-mmio.c
	 */
	synchronized MmapGpioInterface getMmapGpio() {
		if (mmapGpio == null) {
			mmapGpio = getBoardInfo().createMmapGpio();
			if (mmapGpio == null) {
				throw new RuntimeException("Memory mapped GPIO is not supported on board " + getBoardInfo());
			}
			
			mmapGpio.initialise();
		}
		return mmapGpio;
	}
	
	@Override
	public void shutdown() {
		if (mmapGpio != null) {
			mmapGpio.close();
		}
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
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
}
