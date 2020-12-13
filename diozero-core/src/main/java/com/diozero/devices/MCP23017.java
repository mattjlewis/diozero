package com.diozero.devices;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     MCP23017.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
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
import com.diozero.api.RuntimeIOException;
import com.diozero.devices.mcp23xxx.MCP23x17;

/**
 * Datasheet: <a href="http://ww1.microchip.com/downloads/en/DeviceDoc/21952b.pdf">http://ww1.microchip.com/downloads/en/DeviceDoc/21952b.pdf</a>.
 * <p>The MCP23X17 consists of multiple 8-bit configuration registers for input, output and polarity selection. The
 * system master can enable the I/Os as either inputs or outputs by writing the I/O configuration bits (IODIRA/B).
 * The data for each input or output is kept in the corresponding input or output register. The polarity of
 * the Input Port register can be inverted with the Polarity Inversion register. All registers can be read by the
 * system master.</p>
 * <p>The 16-bit I/O port functionally consists of two 8-bit ports (PORTA and PORTB). The MCP23X17 can be
 * configured to operate in the 8-bit or 16-bit modes via IOCON.BANK.</p>
 * <p>There are two interrupt GPIOs, INTA and INTB, that can be associated with their respective ports, or can be
 * logically OR'ed together so that both GPIOs will activate if either port causes an interrupt.
 * A special mode (Byte mode with IOCON.BANK = 0) causes the address pointer to toggle between
 * associated A/B register pairs. For example, if the BANK bit is cleared and the Address Pointer is initially set
 * to address 12h (GPIOA) or 13h (GPIOB), the pointer will toggle between GPIOA and GPIOB. Note that the
 * Address Pointer can initially point to either address in the register pair.</p>
 */
public class MCP23017 extends MCP23x17 {
	// Default I2C address
	public static final int DEVICE_ADDRESS = 0x20;
	private static final String DEVICE_NAME = "MCP23017";

	private I2CDevice device;

	public MCP23017() throws RuntimeIOException {
		this(I2CConstants.CONTROLLER_1, DEVICE_ADDRESS, INTERRUPT_GPIO_NOT_SET, INTERRUPT_GPIO_NOT_SET);
	}

	public MCP23017(int interruptGpio) throws RuntimeIOException {
		this(I2CConstants.CONTROLLER_1, DEVICE_ADDRESS, interruptGpio, interruptGpio);
	}

	public MCP23017(int interruptGpioA, int interruptGpioB) throws RuntimeIOException {
		this(I2CConstants.CONTROLLER_1, DEVICE_ADDRESS, interruptGpioA, interruptGpioB);
	}

	public MCP23017(int controller, int address, int interruptGpio) throws RuntimeIOException {
		this(controller, address, interruptGpio, interruptGpio);
	}

	public MCP23017(int controller, int address, int interruptGpioA, int interruptGpioB) throws RuntimeIOException {
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
	protected void writeByte(int register, byte value) {
		device.writeByteData(register, value);
	}
	
	@Override
	protected byte readByte(int register) {
		return device.readByteData(register);
	}
}
