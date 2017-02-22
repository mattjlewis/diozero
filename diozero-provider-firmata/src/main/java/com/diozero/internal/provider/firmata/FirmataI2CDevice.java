package com.diozero.internal.provider.firmata;

/*
 * #%L
 * Device I/O Zero - Firmata
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
