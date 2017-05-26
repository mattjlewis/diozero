package com.diozero.sandpit;

/*
 * #%L
 * Device I/O Zero - Core
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


import java.io.Closeable;

import com.diozero.api.I2CDevice;
import com.diozero.util.SleepUtil;

/**
 * See the <a href="http://www.microchip.com/ParamChartSearch/chart.aspx?branchID=1100111">Microchip website</a>.
 * <a href="http://ww1.microchip.com/downloads/en/DeviceDoc/21754M.pdf">Datasheet</a>.
 * Connections:
 * <pre>
 * GND  A0 1  8 Vcc 3v3
 * GND  A1 2  7 WP (Write Protect Input, Vss=write enabled, Vcc=write disabled)
 * GND  A2 3  6 SCL
 * GND Vss 4  5 SDA
 * </pre>
 */
public class Eeprom24LC512 implements Closeable {
	private static final int DEFAULT_ADDRESS = 0x50;
	
	private I2CDevice device;
	private int pageSize = 128;
	private int writeCycleTimeMs = 5;
	
	public Eeprom24LC512(int controller) {
		this(controller, DEFAULT_ADDRESS);
	}
	
	public Eeprom24LC512(int controller, int address) {
		device = new I2CDevice(controller, address);
	}
	
	private void setAddress(int address) {
		byte addr_msb = (byte) ((address >> 8) & 0xff);
		byte addr_lsb = (byte) (address & 0xff);
		device.write(new byte[] { addr_msb, addr_lsb });
	}
	
	public byte currentAddressRead() {
		return device.readByte();
	}
	
	public byte randomRead(int address) {
		setAddress(address);
		return device.readByte();
	}
	
	public byte[] sequentialRead(int address, int length) {
		setAddress(address);
		return device.read(length);
	}
	
	public void writeByte(int address, int data) {
		writeByte(address, (byte) data);
	}
	
	public void writeByte(int address, byte data) {
		byte addr_msb = (byte) ((address >> 8) & 0xff);
		byte addr_lsb = (byte) (address & 0xff);
		device.write(new byte[] { addr_msb, addr_lsb, data });
		SleepUtil.sleepMillis(writeCycleTimeMs);
	}
	
	public void pageWrite(int address, byte[] data) {
		// TODO Check max page size
		byte[] buffer = new byte[2 + data.length];
		byte addr_msb = (byte) ((address >> 8) & 0xff);
		byte addr_lsb = (byte) (address & 0xff);
		buffer[0] = addr_msb;
		buffer[1] = addr_lsb;
		System.arraycopy(data, 0, buffer, 2, data.length);
		device.write(buffer);
		SleepUtil.sleepMillis(writeCycleTimeMs);
	}

	@Override
	public void close() {
		device.close();
	}
}
