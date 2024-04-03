package com.diozero.devices;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     MCP23008.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2024 diozero
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

import com.diozero.api.I2CConstants;
import com.diozero.api.I2CDevice;
import com.diozero.api.I2CDeviceInterface;
import com.diozero.api.RuntimeIOException;
import com.diozero.devices.mcp23xxx.MCP23x08;

public class MCP23008 extends MCP23x08 {
	// Default I2C address
	public static final int DEVICE_ADDRESS = 0x20;
	private static final String DEVICE_NAME = "MCP23008";

	private I2CDeviceInterface device;

	public MCP23008() throws RuntimeIOException {
		this(I2CConstants.CONTROLLER_1, DEVICE_ADDRESS, INTERRUPT_GPIO_NOT_SET, INTERRUPT_GPIO_NOT_SET);
	}

	public MCP23008(int interruptGpio) throws RuntimeIOException {
		this(I2CConstants.CONTROLLER_1, DEVICE_ADDRESS, interruptGpio, interruptGpio);
	}

	public MCP23008(int interruptGpioA, int interruptGpioB) throws RuntimeIOException {
		this(I2CConstants.CONTROLLER_1, DEVICE_ADDRESS, interruptGpioA, interruptGpioB);
	}

	public MCP23008(int controller, int address, int interruptGpio) throws RuntimeIOException {
		this(controller, address, interruptGpio, interruptGpio);
	}

	public MCP23008(int controller, int address, int interruptGpioA, int interruptGpioB) throws RuntimeIOException {
		super(DEVICE_NAME + "-" + controller + "-" + address, interruptGpioA, interruptGpioB);

		device = I2CDevice.builder(address).setController(controller).build();

		initialise();
	}

	@Override
	public void close() throws RuntimeIOException {
		super.close();
		device.close();
	}

	@Override
	protected byte readByte(int register) {
		return device.readByteData(register);
	}

	@Override
	protected void writeByte(int register, byte value) {
		device.writeByteData(register, value);
	}
}
