package com.diozero.internal.provider.builtin;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     NativeGpioInputOutputDevice.java  
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
import com.diozero.api.DigitalInputEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.PinInfo;
import com.diozero.internal.provider.AbstractInputDevice;
import com.diozero.internal.provider.GpioDigitalInputOutputDeviceInterface;
import com.diozero.internal.provider.builtin.gpio.GpioLine;
import com.diozero.internal.provider.builtin.gpio.GpioLineEventListener;
import com.diozero.internal.provider.builtin.gpio.GpioChip;
import com.diozero.util.RuntimeIOException;

public class NativeGpioInputOutputDevice extends AbstractInputDevice<DigitalInputEvent>
		implements GpioDigitalInputOutputDeviceInterface, GpioLineEventListener {
	private GpioChip chip;
	private int gpio;
	private GpioLine line;

	public NativeGpioInputOutputDevice(DefaultDeviceFactory deviceFactory, String key, GpioChip chip,
			PinInfo pinInfo, DeviceMode mode) {
		super(key, deviceFactory);

		gpio = pinInfo.getDeviceNumber();
		int offset = pinInfo.getLineOffset();
		if (offset == PinInfo.NOT_DEFINED) {
			throw new IllegalArgumentException("Line offset not defined for pin " + pinInfo);
		}
		this.chip = chip;

		switch (mode) {
		case DIGITAL_INPUT:
			line = chip.provisionGpioInputDevice(offset, GpioPullUpDown.NONE, GpioEventTrigger.BOTH);
			break;
		case DIGITAL_OUTPUT:
			line = chip.provisionGpioOutputDevice(offset, 0);
			break;
		default:
			throw new IllegalArgumentException("Invalid line mode " + mode);
		}
	}

	@Override
	public DeviceMode getMode() {
		return line.getDirection() == GpioLine.Direction.INPUT ? DeviceMode.DIGITAL_INPUT : DeviceMode.DIGITAL_OUTPUT;
	}

	@Override
	public void setMode(DeviceMode mode) {
		// Detect a direction change
		switch (mode) {
		case DIGITAL_INPUT:
			if (line.getDirection() != GpioLine.Direction.INPUT) {
				line.close();
				line = chip.provisionGpioInputDevice(gpio, GpioPullUpDown.NONE, GpioEventTrigger.BOTH);
			}
			break;
		case DIGITAL_OUTPUT:
			if (line.getDirection() != GpioLine.Direction.OUTPUT) {
				line.close();
				line = chip.provisionGpioOutputDevice(gpio, 0);
			}
			break;
		default:
			throw new IllegalArgumentException("Invalid line mode " + mode);
		}
	}

	@Override
	public int getGpio() {
		return gpio;
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		return line.getValue() == 0 ? false : true;
	}

	@Override
	public void setValue(boolean value) throws RuntimeIOException {
		line.setValue(value ? 1 : 0);
	}

	@Override
	protected void enableListener() {
		chip.register(line.getFd(), this);
	}

	@Override
	protected void disableListener() {
		chip.deregister(line.getFd());
	}

	@Override
	public void closeDevice() {
		line.close();
	}

	@Override
	public void event(int gpioOffset, int eventDataId, long timestampNanos) {
		valueChanged(new DigitalInputEvent(gpio, timestampNanos / 1_000_000, timestampNanos,
				eventDataId == GpioChip.GPIOEVENT_EVENT_RISING_EDGE));
	}
}
