package com.diozero.internal.provider.jdkdio10;

/*
 * #%L
 * Device I/O Zero - JDK Device I/O v1.0 provider
 * %%
 * Copyright (C) 2016 diozero
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


import java.io.IOException;

import org.pmw.tinylog.Logger;

import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;

import jdk.dio.DeviceConfig;
import jdk.dio.DeviceManager;
import jdk.dio.gpio.GPIOPin;
import jdk.dio.gpio.GPIOPinConfig;

public class JdkDeviceIoGpioOutputDevice extends AbstractDevice implements GpioDigitalOutputDeviceInterface {
	private GPIOPinConfig pinConfig;
	private GPIOPin pin;
	
	JdkDeviceIoGpioOutputDevice(String key, DeviceFactoryInterface deviceFactory, int pinNumber, boolean initialValue) throws IOException {
		super(key, deviceFactory);
		
		pinConfig = new GPIOPinConfig(DeviceConfig.DEFAULT, pinNumber, GPIOPinConfig.DIR_OUTPUT_ONLY,
				GPIOPinConfig.DEFAULT, GPIOPinConfig.TRIGGER_NONE, initialValue);
				//GPIOPinConfig.MODE_OUTPUT_PUSH_PULL, GPIOPinConfig.TRIGGER_NONE, false);
		pin = DeviceManager.open(GPIOPin.class, pinConfig);
	}

	@Override
	public void closeDevice() throws IOException {
		Logger.debug("closeDevice()");
		if (pin.isOpen()) {
			pin.close();
		}
	}
	
	// Exposed properties
	@Override
	public int getPin() {
		return pinConfig.getPinNumber();
	}
	
	@Override
	public boolean getValue() throws IOException {
		return pin.getValue();
	}
	
	@Override
	public void setValue(boolean value) throws IOException {
		pin.setValue(value);
	}
}
