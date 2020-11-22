package com.diozero.devices.motor;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     DigitalMotor.java  
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


import java.io.Closeable;

import org.tinylog.Logger;

import com.diozero.api.DigitalOutputDevice;
import com.diozero.internal.spi.GpioDeviceFactoryInterface;
import com.diozero.sbc.DeviceFactoryHelper;
import com.diozero.util.RuntimeIOException;

public class DigitalMotor implements Closeable {
	private DigitalOutputDevice forward;
	private DigitalOutputDevice backward;
	
	public DigitalMotor(int forwardGpio, int backwardGpio) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), forwardGpio, backwardGpio);
	}
	
	public DigitalMotor(GpioDeviceFactoryInterface deviceFactory, int forwardGpio, int backwardGpio) throws RuntimeIOException {
		forward = new DigitalOutputDevice(deviceFactory, forwardGpio, true, false);
		backward = new DigitalOutputDevice(deviceFactory, backwardGpio, true, false);
	}

	@Override
	public void close() {
		Logger.trace("close()");
		forward.close();
		backward.close();
	}
	
	// Exposed operations
	public void forward() throws RuntimeIOException {
		forward.on();
		backward.off();
	}
	
	public void backward() throws RuntimeIOException {
		forward.off();
		backward.on();
	}
	
	public void stop() throws RuntimeIOException {
		forward.off();
		backward.off();
	}
	
	public void reverse() throws RuntimeIOException {
		if (!isActive()) {
			return;
		}
		forward.toggle();
		backward.toggle();
	}
	
	public boolean isActive() throws RuntimeIOException {
		return forward.isOn() || backward.isOn();
	}
	
	/**
	 * Represents the speed of the motor as a floating point value between -1
	 *   (full speed backward) and 1 (full speed forward)
	 * @throws RuntimeIOException if an I/O error occurs
	 * @return current value for this motor in the range -1 (backwards) to 1 (forwards)
	 */
	public float getValue() throws RuntimeIOException {
		if (forward.isOn()) {
			return 1;
		} else if (backward.isOn()) {
			return -1;
		}
		
		return 0;
	}
}
