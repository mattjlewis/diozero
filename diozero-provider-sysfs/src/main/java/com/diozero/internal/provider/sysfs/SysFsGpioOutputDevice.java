package com.diozero.internal.provider.sysfs;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;

import org.pmw.tinylog.Logger;

import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;
import com.diozero.util.RuntimeIOException;

public class SysFsGpioOutputDevice extends AbstractDevice implements GpioDigitalOutputDeviceInterface {
	private static final String VALUE_FILE = "value";
	private static final byte LOW_VALUE = '0';
	private static final byte HIGH_VALUE = '1';

	private SysFsDeviceFactory deviceFactory;
	private int pinNumber;
	private RandomAccessFile valueFile;

	public SysFsGpioOutputDevice(SysFsDeviceFactory deviceFactory, Path gpioDir, String key, int pinNumber,
			boolean initialValue) {
		super(key, deviceFactory);
		
		this.deviceFactory = deviceFactory;
		this.pinNumber = pinNumber;
		
		// TODO Set active_low value to 0
		
		try {
			valueFile = new RandomAccessFile(gpioDir.resolve(VALUE_FILE).toFile(), "rw");
		} catch (IOException e) {
			throw new RuntimeIOException("Error opening value file for GPIO " + pinNumber, e);
		}
		
		setValue(initialValue);
	}

	@Override
	public int getPin() {
		return pinNumber;
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		try {
			valueFile.seek(0);
			return valueFile.readByte() == HIGH_VALUE;
		} catch (IOException e) {
			throw new RuntimeIOException("Error reading value", e);
		}
	}

	@Override
	public void setValue(boolean value) throws RuntimeIOException {
		try {
			valueFile.seek(0);
			valueFile.writeByte(value ? HIGH_VALUE : LOW_VALUE);
		} catch (IOException e) {
			throw new RuntimeIOException("Error writing value", e);
		}
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.debug("closeDevice()");
		try {
			valueFile.close();
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
		deviceFactory.unexport(pinNumber);
	}
}
