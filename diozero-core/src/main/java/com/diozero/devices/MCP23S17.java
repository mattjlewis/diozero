package com.diozero.devices;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Core
 * Filename:     MCP23S17.java  
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


import com.diozero.api.SPIConstants;
import com.diozero.api.SpiClockMode;
import com.diozero.api.SpiDevice;
import com.diozero.internal.provider.mcp23xxx.MCP23x17;
import com.diozero.util.RuntimeIOException;

public class MCP23S17 extends MCP23x17 {
	// SPI Address Register  0b[0 1 0 0 A2 A1 A0 R/W]
	private static final byte ADDRESS_MASK = 0b01000000; // 0x40 [0100 0000] [A2 = 0 | A1 = 0 | A0 = 0]
	//private static final byte DEFAULT_ADDRESS = 0b00; // [A2 = 0 | A1 = 0 | A0 = 0]
	private static final byte WRITE_FLAG = 0b00000000;    // 0x00
	private static final byte READ_FLAG  = 0b00000001;    // 0x01
	private static final String DEVICE_NAME = "MCP23S17";

	private SpiDevice device;
	private byte boardAddress;

	public MCP23S17(int address) throws RuntimeIOException {
		this(SPIConstants.DEFAULT_SPI_CONTROLLER, SPIConstants.CE0, address, INTERRUPT_GPIO_NOT_SET, INTERRUPT_GPIO_NOT_SET);
	}

	public MCP23S17(int address, int interruptGpio) throws RuntimeIOException {
		this(SPIConstants.DEFAULT_SPI_CONTROLLER, SPIConstants.CE0, address, interruptGpio, interruptGpio);
	}

	public MCP23S17(int address, int interruptGpioA, int interruptGpioB) throws RuntimeIOException {
		this(SPIConstants.DEFAULT_SPI_CONTROLLER, SPIConstants.CE0, address, interruptGpioA, interruptGpioB);
	}

	public MCP23S17(int controller, int chipSelect, int address, int interruptGpio) throws RuntimeIOException {
		this(controller, chipSelect, address, interruptGpio, interruptGpio);
	}

	public MCP23S17(int controller, int chipSelect, int address,
			int interruptGpioA, int interruptGpioB) throws RuntimeIOException {
		super(DEVICE_NAME + "-" + controller + "-" + chipSelect, interruptGpioA, interruptGpioB);
		
		device = new SpiDevice(controller, chipSelect, 1_000_000, SpiClockMode.MODE_0, false);
		this.boardAddress = (byte) ((address & 0b111) << 1 | ADDRESS_MASK);
		
		initialise();
	}
	
	@Override
	public void close() throws RuntimeIOException {
		super.close();
		device.close();
	}
	
	@Override
	protected byte readByte(int register) {
		byte[] tx = new byte[3];
		tx[0] = (byte) (boardAddress | READ_FLAG);
		tx[1] = (byte) register;
		tx[2] = (byte) 0;

		byte[] rx = device.writeAndRead(tx);

		return rx[2];
	}
	
	@Override
	protected void writeByte(int register, byte value) {
		byte[] tx = new byte[3];
		tx[0] = (byte) (boardAddress | WRITE_FLAG);
		tx[1] = (byte) register;
		tx[2] = value;

		device.write(tx);
	}
}
