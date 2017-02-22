package com.diozero.internal.provider.firmata;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.firmata4j.I2CDevice;
import org.firmata4j.I2CEvent;
import org.firmata4j.I2CListener;
import org.pmw.tinylog.Logger;

import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.I2CDeviceInterface;
import com.diozero.util.RuntimeIOException;

/**
 * Does not currently work, I am unclear as to how the this Java Firmata I2C implementation is supposed to work.
 */
public class FirmataI2CDevice extends AbstractDevice implements I2CDeviceInterface, I2CListener {
	private I2CDevice i2cDevice;

	public FirmataI2CDevice(FirmataDeviceFactory deviceFactory, String key, int controller, int address,
			int addressSize, int clockFrequency) {
		super(key, deviceFactory);
		
		try {
			i2cDevice = deviceFactory.getIoDevice().getI2CDevice((byte) address);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public void read(int register, int subAddressSize, ByteBuffer buffer) throws RuntimeIOException {
		/*
		try {
			i2cDevice.ask((byte) buffer.remaining(), this);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
		*/
	}

	@Override
	public void read(ByteBuffer buffer) throws RuntimeException {
		/*
		try {
			i2cDevice.ask((byte) buffer.remaining(), this);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
		*/
	}

	@Override
	public void write(int register, int subAddressSize, ByteBuffer buffer) throws RuntimeIOException {
		/*
		byte[] data = new byte[buffer.remaining()+1];
		data[0] = (byte) register;
		buffer.get(data, 1, buffer.remaining());
		try {
			i2cDevice.tell(data);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
		*/
	}

	@Override
	public void write(ByteBuffer buffer) throws RuntimeException {
		byte[] data = new byte[buffer.remaining()];
		buffer.get(data, 0, buffer.remaining());
		try {
			i2cDevice.tell(data);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		/*
		try { i2cDevice.stopReceivingUpdates(); } catch (IOException e) { }
		i2cDevice.unsubscribe(this);
		*/
	}

	@Override
	public void onReceive(I2CEvent event) {
		Logger.info(event);
	}
}
