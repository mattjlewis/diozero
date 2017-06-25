package com.diozero;

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

import org.pmw.tinylog.Logger;

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
public class McpEeprom implements Closeable {
	public static enum Type {
		MCP_24xx256(2, 64, 5, 256), MCP_24xx512(2, 128, 5, 512);
		
		private int addressSizeBytes;
		private int pageSizeBytes;
		private int writeCycleTimeMillis;
		private int memorySizeBits;
		private int memorySizeBytes;
		
		private Type(int addressSizeBytes, int pageSizeBytes, int writeCycleTimeMillis, int memorySizeKibibits) {
			this.addressSizeBytes = addressSizeBytes;
			this.pageSizeBytes = pageSizeBytes;
			this.writeCycleTimeMillis = writeCycleTimeMillis;
			memorySizeBits = memorySizeKibibits * 1024;
			memorySizeBytes = memorySizeBits / 8;
		}

		public int getAddressSizeBytes() {
			return addressSizeBytes;
		}

		public int getPageSizeBytes() {
			return pageSizeBytes;
		}

		public int getWriteCycleTimeMillis() {
			return writeCycleTimeMillis;
		}

		public int getMemorySizeBits() {
			return memorySizeBits;
		}

		public int getMemorySizeBytes() {
			return memorySizeBytes;
		}
	}
	private static final int DEFAULT_ADDRESS = 0x50;
	
	private I2CDevice device;
	private Type type;
	
	public McpEeprom(int controller, Type type) {
		this(controller, DEFAULT_ADDRESS, type);
	}
	
	public McpEeprom(int controller, int address, Type type) {
		this.type = type;
		device = new I2CDevice(controller, address);
	}
	
	private byte[] getAddressByteArray(int address) {
		byte[] address_bytes = new byte[type.getAddressSizeBytes()];
		
		for (int i=0; i<type.getAddressSizeBytes(); i++) {
			address_bytes[i] = (byte) ((address >> 8 * (type.getAddressSizeBytes() - (i+1))) & 0xff);
		}
		
		return address_bytes;
	}
	
	public byte readCurrentAddress() {
		return device.readByte();
	}
	
	public byte readByte(int address) {
		device.write(getAddressByteArray(address));
		return device.readByte();
	}
	
	public byte[] readBytes(int address, int length) {
		device.write(getAddressByteArray(address));
		return device.read(length);
	}
	
	public void writeByte(int address, int data) {
		writeByte(address, (byte) data);
	}
	
	public void writeByte(int address, byte data) {
		byte[] addr_bytes = getAddressByteArray(address);
		byte[] buffer = new byte[addr_bytes.length+1];
		System.arraycopy(addr_bytes, 0, buffer, 0, addr_bytes.length);
		buffer[addr_bytes.length] = data;
		device.write(buffer);
		SleepUtil.sleepMillis(type.getWriteCycleTimeMillis());
	}
	
	public void writeBytes(int address, byte[] data) {
		if ((address + data.length) > type.getMemorySizeBytes()) {
			Logger.error("Attempt to write beyond memory size - no data written");
			return;
		}
		
		int page = address / type.getPageSizeBytes();
		int bytes_remaining = data.length;
		do {
			int remaining_page_size = type.getPageSizeBytes() - (address % type.getPageSizeBytes());
			int bytes_to_write = remaining_page_size < bytes_remaining ? remaining_page_size : bytes_remaining;
			
			byte[] addr_bytes = getAddressByteArray(address);
			byte[] buffer = new byte[addr_bytes.length+bytes_to_write];
			System.arraycopy(addr_bytes, 0, buffer, 0, addr_bytes.length);
			System.arraycopy(data, data.length - bytes_remaining, buffer, addr_bytes.length, bytes_to_write);
			device.write(buffer);
			
			bytes_remaining -= bytes_to_write;
			page++;
			address = page * type.getPageSizeBytes();
			
			SleepUtil.sleepMillis(type.getWriteCycleTimeMillis());
		} while (bytes_remaining > 0);
	}

	@Override
	public void close() {
		device.close();
	}
}
