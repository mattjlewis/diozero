package com.diozero.devices.mcp23xxx;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     MCP23x08.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2021 diozero
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


import com.diozero.api.PinInfo;
import com.diozero.sbc.BoardPinInfo;

public abstract class MCP23x08 extends MCP23xxx {
	private static final int NUM_PORTS = 1;
	
	/** Controls the direction of the data I/O. When a bit is set, the corresponding pin becomes an
	 * input. When a bit is clear, the corresponding pin becomes an output */
	private static final int[] IODIR_REG = { 0x00 };
	/** This register allows the user to configure the polarity on the corresponding GPIO port bits.
	 * If a bit is set, the corresponding GPIO register bit will reflect the inverted value on the pin */
	private static final int[] IPOL_REG = { 0x01 };
	/** The GPINTEN register controls the interrupt-on-change feature for each pin. If a bit is set,
	 * the corresponding pin is enabled for interrupt-on-change. The DEFVAL and INTCON registers
	 * must also be configured if any pins are enabled for interrupt-on-change */
	private static final int[] GPINTEN_REG = { 0x02 };
	/** The default comparison value is configured in the DEFVAL register. If enabled
	 * (via GPINTEN and INTCON) to compare against the DEFVAL register, an opposite
	 * value on the associated pin will cause an interrupt to occur */
	private static final int[] DEFVAL_REG = { 0x03 };
	/** The INTCON register controls how the associated pin value is compared for the
	 * interrupt-on-change feature. If a bit is set, the corresponding I/O pin is compared
	 * against the associated bit in the DEFVAL register. If a bit value is clear, the
	 * corresponding I/O pin is compared against the previous value */
	private static final int[] INTCON_REG = { 0x04 };
	/** I/O configuration register */
	private static final int[] IOCON_REG = { 0x05 };
	/** The GPPU register controls the pull-up resistors for the port pins. If a bit is
	 * set and the corresponding pin is configured as an input, the corresponding port pin is
	 * internally pulled up with a 100 kOhm resistor */
	private static final int[] GPPU_REG = { 0x06 };
	/** The INTF register reflects the interrupt condition on the port pins of any pin that is
	 * enabled for interrupts via the GPINTEN register. A 'set' bit indicates that the
	 * associated pin caused the interrupt. This register is 'read-only'. Writes to this
	 * register will be ignored */
	private static final int[] INTF_REG = { 0x07 };
	/** The INTCAP register captures the GPIO port value at the time the interrupt occurred.
	 * The register is 'read-only' and is updated only when an interrupt occurs. The register
	 * will remain unchanged until the interrupt is cleared via a read of INTCAP or GPIO. */
	private static final int[] INTCAP_REG = { 0x08 };
	/** The GPIO register reflects the value on the port. Reading from this register reads
	 * the port. Writing to this register modifies the Output Latch (OLAT) register */
	private static final int[] GPIO_REG = { 0x09 };
	/** The OLAT register provides access to the output latches. A read from this register
	 * results in a read of the OLAT and not the port itself. A write to this register
	 * modifies the output latches that modifies the pins configured as outputs */
	private static final int[] OLAT_REG = { 0x0A };

	private BoardPinInfo boardPinInfo;

	public MCP23x08(String deviceName) {
		this(deviceName, INTERRUPT_GPIO_NOT_SET, INTERRUPT_GPIO_NOT_SET);
	}
	
	public MCP23x08(String deviceName, int interruptGpio) {
		this(deviceName, interruptGpio, interruptGpio);
	}
	
	public MCP23x08(String deviceName, int interruptGpioA, int interruptGpioB) {
		super(NUM_PORTS, deviceName, interruptGpioA, interruptGpioB);
		
		boardPinInfo = new MCP23x08BoardPinInfo();
	}
	
	@Override
	protected int getIODirReg(int port) {
		return IODIR_REG[port];
	}

	@Override
	protected int getIPolReg(int port) {
		return IPOL_REG[port];
	}

	@Override
	protected int getGPIntEnReg(int port) {
		return GPINTEN_REG[port];
	}

	@Override
	protected int getDefValReg(int port) {
		return DEFVAL_REG[port];
	}

	@Override
	protected int getIntConReg(int port) {
		return INTCON_REG[port];
	}

	@Override
	protected int getIOConReg(int port) {
		return IOCON_REG[port];
	}

	@Override
	protected int getGPPullUpReg(int port) {
		return GPPU_REG[port];
	}

	@Override
	protected int getIntFReg(int port) {
		return INTF_REG[port];
	}

	@Override
	protected int getIntCapReg(int port) {
		return INTCAP_REG[port];
	}

	@Override
	protected int getGPIOReg(int port) {
		return GPIO_REG[port];
	}

	@Override
	protected int getOLatReg(int port) {
		return OLAT_REG[port];
	}

	@Override
	public BoardPinInfo getBoardPinInfo() {
		return boardPinInfo;
	}
	
	public static class MCP23x08BoardPinInfo extends BoardPinInfo {
		public MCP23x08BoardPinInfo() {
			for (int i=0; i<8; i++) {
				addGpioPinInfo(i, 10+i, PinInfo.DIGITAL_IN_OUT);
			}
		}
	}
}
