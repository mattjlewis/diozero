package com.diozero;

import java.nio.ByteOrder;

import org.pmw.tinylog.Logger;

import com.diozero.api.*;
import com.diozero.internal.provider.pcf8574.PCF8574DigitalInputDevice;
import com.diozero.internal.provider.pcf8574.PCF8574DigitalInputOutputDevice;
import com.diozero.internal.provider.pcf8574.PCF8574DigitalOutputDevice;
import com.diozero.internal.spi.*;
import com.diozero.internal.spi.GpioDeviceInterface.Mode;
import com.diozero.util.BitManipulation;
import com.diozero.util.MutableByte;
import com.diozero.util.RuntimeIOException;

public class PCF8574 extends AbstractDeviceFactory implements GpioDeviceFactoryInterface, GpioExpander {
	private static final String DEVICE_NAME = "PCF8574";

	private static final int NUM_PINS = 8;
	
	private I2CDevice device;
	private MutableByte directions;
	
	public PCF8574(int controller, int address, int addressSize, int frequency) {
		super(DEVICE_NAME + "-" + controller + "-" + address);
		
		device = new I2CDevice(controller, address, addressSize, frequency, ByteOrder.LITTLE_ENDIAN);
		directions = new MutableByte();
	}

	@Override
	public GpioDigitalInputDeviceInterface provisionDigitalInputPin(int gpio, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws RuntimeIOException {
		if (gpio < 0 || gpio >= NUM_PINS) {
			throw new IllegalArgumentException(
					"Invalid GPIO (" + gpio + "); must be 0.." + (NUM_PINS - 1));
		}
		
		String key = createPinKey(gpio);
		
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}
		
		setInputMode(gpio);
		
		GpioDigitalInputDeviceInterface in_device = new PCF8574DigitalInputDevice(this, key, gpio, trigger);
		deviceOpened(in_device);
		
		return in_device;
	}

	@Override
	public GpioDigitalOutputDeviceInterface provisionDigitalOutputPin(int gpio, boolean initialValue) throws RuntimeIOException {
		if (gpio < 0 || gpio >= NUM_PINS) {
			throw new IllegalArgumentException(
					"Invalid GPIO (" + gpio + "); must be 0.." + (NUM_PINS - 1));
		}
		
		String key = createPinKey(gpio);
		
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}
		
		setOutputMode(gpio);
		
		GpioDigitalOutputDeviceInterface out_device = new PCF8574DigitalOutputDevice(this, key, gpio);
		deviceOpened(out_device);
		out_device.setValue(initialValue);
		
		return out_device;
	}

	@Override
	public GpioDigitalInputOutputDeviceInterface provisionDigitalInputOutputPin(int gpio, Mode mode)
			throws RuntimeIOException {
		if (gpio < 0 || gpio >= NUM_PINS) {
			throw new IllegalArgumentException(
					"Invalid GPIO (" + gpio + "); must be 0.." + (NUM_PINS - 1));
		}
		
		String key = createPinKey(gpio);
		
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}
		
		GpioDigitalInputOutputDeviceInterface inout_device = new PCF8574DigitalInputOutputDevice(this, key, gpio, mode);
		deviceOpened(inout_device);
		
		return inout_device;
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
}
