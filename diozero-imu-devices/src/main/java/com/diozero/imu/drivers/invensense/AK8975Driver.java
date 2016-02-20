package com.diozero.imu.drivers.invensense;

/*
 * #%L
 * Device I/O Zero - Core
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

import java.io.Closeable;

import com.diozero.api.I2CDevice;
import com.diozero.util.RuntimeIOException;
import com.diozero.util.SleepUtil;

/**
 * Output data resolution is 13 bit (0.3 uT per LSB), Full scale measurement range is +/-1200 uT
 */
public class AK8975Driver implements Closeable, AK8975Constants {
	private short[] mag_sens_adj = new short[3];
	private I2CDevice i2cDevice;

	public AK8975Driver(int controllerNumber, int addressSize, int clockFrequency) throws RuntimeIOException {
		this(controllerNumber, addressSize, clockFrequency, AK8975_MAG_ADDRESS);
	}

	public AK8975Driver(int controllerNumber, int addressSize, int clockFrequency, int address) throws RuntimeIOException {
		i2cDevice = new I2CDevice(controllerNumber, address, addressSize, clockFrequency);
	}
	
	public void init() throws RuntimeIOException {
		byte[] data = new byte[4];
		data[0] = AKM_POWER_DOWN;
		i2cDevice.writeByte(AKM_REG_CNTL, data[0]);
		SleepUtil.sleepMillis(1);

		data[0] = AKM_FUSE_ROM_ACCESS;
		i2cDevice.writeByte(AKM_REG_CNTL, data[0]);
		SleepUtil.sleepMillis(1);

		/* Get sensitivity adjustment data from fuse ROM. */
		data = i2cDevice.readBytes(AKM_REG_ASAX, 3);
		mag_sens_adj[0] = (short)(data[0] + 128);
		mag_sens_adj[1] = (short)(data[1] + 128);
		mag_sens_adj[2] = (short)(data[2] + 128);

		data[0] = AKM_POWER_DOWN;
		i2cDevice.writeByte(AKM_REG_CNTL, data[0]);
		SleepUtil.sleepMillis(1);
	}

	public short[] get_mag_sens_adj() {
		return mag_sens_adj;
	}
	
	@Override
	public void close() {
		i2cDevice.close();
	}
}
