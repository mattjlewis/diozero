package com.diozero.devices;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Core
 * Filename:     PCF8574.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2017 mattjlewis
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


import java.nio.ByteOrder;

import org.pmw.tinylog.Logger;

import com.diozero.api.*;
import com.diozero.internal.provider.*;
import com.diozero.util.*;

public class PCF8574 extends AbstractDeviceFactory implements GpioDeviceFactoryInterface, GpioExpander {
	private static final String DEVICE_NAME = "PCF8574";

	private static final int NUM_PINS = 8;
	
	private I2CDevice device;
	private MutableByte directions;
	private BoardPinInfo boardPinInfo;
	
	public PCF8574(int controller, int address, int addressSize, int frequency) {
		super(DEVICE_NAME + "-" + controller + "-" + address);
		
		boardPinInfo = new PCF8574BoardPinInfo();
		
		device = new I2CDevice(controller, address, addressSize, frequency, ByteOrder.LITTLE_ENDIAN);
		directions = new MutableByte();
	}

	@Override
	public GpioDigitalInputDeviceInterface createDigitalInputDevice(String key, PinInfo pinInfo, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws RuntimeIOException {
		int gpio = pinInfo.getDeviceNumber();
		setInputMode(gpio);
		return new PCF8574DigitalInputDevice(this, key, gpio, trigger);
	}

	@Override
	public GpioDigitalOutputDeviceInterface createDigitalOutputDevice(String key, PinInfo pinInfo,
			boolean initialValue) throws RuntimeIOException {
		int gpio = pinInfo.getDeviceNumber();
		setOutputMode(gpio);
		return new PCF8574DigitalOutputDevice(this, key, gpio, initialValue);
	}

	@Override
	public GpioDigitalInputOutputDeviceInterface createDigitalInputOutputDevice(String key, PinInfo pinInfo,
			DeviceMode mode) throws RuntimeIOException {
		return new PCF8574DigitalInputOutputDevice(this, key, pinInfo.getDeviceNumber(), mode);
	}
	
	@Override
	public void setDirections(int port, byte directions) {
		this.directions = new MutableByte(directions);
	}

	public byte getValues(int port) {
		return device.readByte(1);
	}

	@Override
	public void setValues(int port, byte values) {
		device.writeByte(values);
	}

	public boolean getValue(int gpio) {
		if (gpio < 0 || gpio >= NUM_PINS) {
			throw new IllegalArgumentException("Invalid GPIO: " + gpio + ". "
					+ DEVICE_NAME + " has " + NUM_PINS + " GPIOs; must be 0.." + (NUM_PINS - 1));
		}
		
		return (getValues(0) & gpio) != 0;
	}
	
	public void setValue(int gpio, boolean value) {
		if (gpio < 0 || gpio >= NUM_PINS) {
			throw new IllegalArgumentException("Invalid GPIO: " + gpio + ". "
					+ DEVICE_NAME + " has " + NUM_PINS + " GPIOs; must be 0.." + (NUM_PINS - 1));
		}
		
		byte old_val = getValues(0);
		byte new_val = BitManipulation.setBitValue(old_val, value, gpio);
		
		setValues(0, new_val);
	}

	@Override
	public void close() {
		device.close();
	}

	public void setInputMode(int gpio) {
		// Note nothing to do to set the in / out direction for the PCF8574
		// We do need to make note of pin direction though 
		directions.setBit((byte) gpio);
	}

	public void setOutputMode(int gpio) {
		// Note nothing to do to set the in / out direction for the PCF8574
		// We do need to make note of pin direction though 
		directions.unsetBit((byte) gpio);
	}

	@Override
	public String getName() {
		return DEVICE_NAME;
	}

	public void closePin(int gpio) {
		Logger.debug("closePin({})", Integer.valueOf(gpio));
		
		if (gpio < 0 || gpio >= NUM_PINS) {
			throw new IllegalArgumentException("Invalid GPIO: " + gpio + ". "
					+ DEVICE_NAME + " has " + NUM_PINS + " GPIOs; must be 0.." + (NUM_PINS - 1));
		}
		
		setInputMode(gpio);
	}

	@Override
	public BoardPinInfo getBoardPinInfo() {
		return boardPinInfo;
	}
	
	public static class PCF8574BoardPinInfo extends BoardPinInfo {
		public PCF8574BoardPinInfo() {
			for (int i=0; i<NUM_PINS; i++) {
				addGpioPinInfo(i, i, PinInfo.DIGITAL_IN_OUT);
			}
		}
	}

	private static class PCF8574DigitalInputDevice extends AbstractInputDevice<DigitalInputEvent> implements GpioDigitalInputDeviceInterface {
		private PCF8574 pcf8574;
		private int gpio;

		public PCF8574DigitalInputDevice(PCF8574 pcf8574, String key, int gpio, GpioEventTrigger trigger) {
			super(key, pcf8574);

			this.pcf8574 = pcf8574;
			this.gpio = gpio;
			// Note trigger is current ignored
		}

		@Override
		public void closeDevice() throws RuntimeIOException {
			Logger.debug("closeDevice()");
			removeListener();
			pcf8574.closePin(gpio);
		}

		@Override
		public boolean getValue() throws RuntimeIOException {
			return pcf8574.getValue(gpio);
		}

		@Override
		public int getGpio() {
			return gpio;
		}

		@Override
		public void setDebounceTimeMillis(int debounceTime) {
		}
	}

	private static class PCF8574DigitalInputOutputDevice extends AbstractInputDevice<DigitalInputEvent> implements GpioDigitalInputOutputDeviceInterface {
		private PCF8574 pcf8574;
		private int gpio;
		private DeviceMode mode;

		public PCF8574DigitalInputOutputDevice(PCF8574 pcf8574, String key, int gpio, DeviceMode mode) {
			super(key, pcf8574);
			
			this.pcf8574 = pcf8574;
			this.gpio = gpio;
			setMode(mode);
		}

		@Override
		public boolean getValue() throws RuntimeIOException {
			return pcf8574.getValue(gpio);
		}

		@Override
		public void setValue(boolean value) throws RuntimeIOException {
			if (mode != DeviceMode.DIGITAL_OUTPUT) {
				throw new IllegalStateException("Can only set output value for digital output pins");
			}
			pcf8574.setValue(gpio, value);
		}

		@Override
		public int getGpio() {
			return gpio;
		}

		@Override
		protected void closeDevice() throws RuntimeIOException {
			Logger.debug("closeDevice()");
			removeListener();
			pcf8574.closePin(gpio);
		}

		@Override
		public DeviceMode getMode() {
			return mode;
		}

		@Override
		public void setMode(DeviceMode mode) {
			if (mode == DeviceMode.DIGITAL_INPUT) {
				pcf8574.setInputMode(gpio);
			} else {
				pcf8574.setOutputMode(gpio);
			}
			this.mode = mode;
		}
	}
	
	private static class PCF8574DigitalOutputDevice extends AbstractDevice implements GpioDigitalOutputDeviceInterface {
		private PCF8574 pcf8574;
		private int gpio;

		public PCF8574DigitalOutputDevice(PCF8574 pcf8574, String key, int gpio, boolean initialValue) {
			super(key, pcf8574);
			
			this.pcf8574 = pcf8574;
			this.gpio = gpio;
			
			setValue(initialValue);
		}

		@Override
		public boolean getValue() throws RuntimeIOException {
			return pcf8574.getValue(gpio);
		}

		@Override
		public void setValue(boolean value) throws RuntimeIOException {
			pcf8574.setValue(gpio, value);
		}

		@Override
		public int getGpio() {
			return gpio;
		}

		@Override
		protected void closeDevice() throws RuntimeIOException {
			Logger.debug("closeDevice()");
			pcf8574.closePin(gpio);
		}
	}
}
