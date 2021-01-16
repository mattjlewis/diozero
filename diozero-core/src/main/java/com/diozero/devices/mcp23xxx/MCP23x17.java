package com.diozero.devices.mcp23xxx;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     MCP23x17.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
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

/**
 * <a href="Wiring">https://www.instructables.com/Arduino-NANO-Tests-2-MCP23S17-IO-Xpanders</a>
 * 
 * <pre>
 * I2C |   SPI | Notch | SPI/I2C
 *     |  GPB0 |  1 28 | GPA7
 *     |  GPB1 |  2 27 | GPA6
 *     |  GPB2 |  3 26 | GPA5
 *     |  GPB3 |  4 25 | GPA4
 *     |  GPB4 |  5 24 | GPA3
 *     |  GPB5 |  6 23 | GPA2
 *     |  GPB6 |  7 22 | GPA1
 *     |  GPB7 |  8 21 | GPA0
 *     |  3v3  |  9 20 | INT A
 *     |  GND  | 10 19 | INT B
 *  NC |   CS  | 11 18 | 3v3 (via 1KOhm resister)
 * SCK |  SCK  | 12 17 | GND
 * SDA | MOSI  | 13 16 | GND
 *  NC | MISO  | 14 15 | GND
 * </pre>
 */
public abstract class MCP23x17 extends MCP23xxx {
	private static final int NUM_PORTS = 2;
	
	// Bank=0 Registers (the default)
	private static final int BANK0_IODIRA = 0x00;
	private static final int BANK0_IODIRB = 0x01;
	private static final int[] BANK0_IODIR_REG = { BANK0_IODIRA, BANK0_IODIRB };
	private static final int BANK0_IPOLA = 0x02;
	private static final int BANK0_IPOLB = 0x03;
	private static final int[] BANK0_IPOL_REG = { BANK0_IPOLA, BANK0_IPOLB };
	private static final int BANK0_GPINTENA = 0x04;
	private static final int BANK0_GPINTENB = 0x05;
	private static final int[] BANK0_GPINTEN_REG = { BANK0_GPINTENA, BANK0_GPINTENB };
	private static final int BANK0_DEFVALA = 0x06;
	private static final int BANK0_DEFVALB = 0x07;
	private static final int[] BANK0_DEFVAL_REG = { BANK0_DEFVALA, BANK0_DEFVALB };
	private static final int BANK0_INTCONA = 0x08;
	private static final int BANK0_INTCONB = 0x09;
	private static final int[] BANK0_INTCON_REG = { BANK0_INTCONA, BANK0_INTCONB };
	private static final int BANK0_IOCONA = 0x0a;
	private static final int BANK0_IOCONB = 0x0b;
	private static final int[] BANK0_IOCON_REG = { BANK0_IOCONA, BANK0_IOCONB };
	private static final int BANK0_GPPUA = 0x0c;
	private static final int BANK0_GPPUB = 0x0d;
	private static final int[] BANK0_GPPU_REG = { BANK0_GPPUA, BANK0_GPPUB };
	private static final int BANK0_INTFA = 0x0e;
	private static final int BANK0_INTFB = 0x0f;
	private static final int[] BANK0_INTF_REG = { BANK0_INTFA, BANK0_INTFB };
	private static final int BANK0_INTCAPA = 0x10;
	private static final int BANK0_INTCAPB = 0x11;
	private static final int[] BANK0_INTCAP_REG = { BANK0_INTCAPA, BANK0_INTCAPB };
	private static final int BANK0_GPIOA = 0x12;
	private static final int BANK0_GPIOB = 0x13;
	private static final int[] BANK0_GPIO_REG = { BANK0_GPIOA, BANK0_GPIOB };
	private static final int BANK0_OLATA = 0x14;
	private static final int BANK0_OLATB = 0x15;
	private static final int[] BANK0_OLAT_REG = { BANK0_OLATA, BANK0_OLATB };
	// Bank=1 Registers
	/*
	private static final int BANK1_IODIRA = 0x00;
	private static final int BANK1_IODIRB = 0x10;
	private static final int[] BANK1_IODIR_REG = { BANK1_IODIRA, BANK1_IODIRB };
	private static final int BANK1_IPOLA = 0x01;
	private static final int BANK1_IPOLB = 0x11;
	private static final int[] BANK1_IPOL_REG = { BANK1_IPOLA, BANK1_IPOLB };
	private static final int BANK1_GPINTENA = 0x02;
	private static final int BANK1_GPINTENB = 0x12;
	private static final int[] BANK1_GPINTEN_REG = { BANK1_GPINTENA, BANK1_GPINTENB };
	private static final int BANK1_DEFVALA = 0x03;
	private static final int BANK1_DEFVALB = 0x13;
	private static final int[] BANK1_DEFVAL_REG = { BANK1_DEFVALA, BANK1_DEFVALB };
	private static final int BANK1_INTCONA = 0x04;
	private static final int BANK1_INTCONB = 0x14;
	private static final int[] BANK1_INTCON_REG = { BANK1_INTCONA, BANK1_INTCONB };
	private static final int BANK1_IOCONA = 0x05;
	private static final int BANK1_IOCONB = 0x15;
	private static final int[] BANK1_IOCON_REG = { BANK1_IOCONA, BANK1_IOCONB };
	private static final int BANK1_GPPUA = 0x06;
	private static final int BANK1_GPPUB = 0x16;
	private static final int[] BANK1_GPPU_REG = { BANK1_GPPUA, BANK1_GPPUB };
	private static final int BANK1_INTFA = 0x07;
	private static final int BANK1_INTFB = 0x17;
	private static final int[] BANK1_INTF_REG = { BANK1_INTFA, BANK1_INTFB };
	private static final int BANK1_INTCAPA = 0x08;
	private static final int BANK1_INTCAPB = 0x18;
	private static final int[] BANK1_INTCAP_REG = { BANK1_INTCAPA, BANK1_INTCAPB };
	private static final int BANK1_GPIOA = 0x09;
	private static final int BANK1_GPIOB = 0x19;
	private static final int[] BANK1_GPIO_REG = { BANK1_GPIOA, BANK1_GPIOB };
	private static final int BANK1_OLATA = 0x0a;
	private static final int BANK1_OLATB = 0x1a;
	private static final int[] BANK1_OLAT_REG = { BANK1_OLATA, BANK1_OLATB };
	*/
	
	/** Controls the direction of the data I/O. When a bit is set, the corresponding pin becomes an
	 * input. When a bit is clear, the corresponding pin becomes an output */
	private static final int[] IODIR_REG = BANK0_IODIR_REG;
	/** This register allows the user to configure the polarity on the corresponding GPIO port bits.
	 * If a bit is set, the corresponding GPIO register bit will reflect the inverted value on the pin */
	private static final int[] IPOL_REG = BANK0_IPOL_REG;
	/** The GPINTEN register controls the interrupt-on-change feature for each pin. If a bit is set,
	 * the corresponding pin is enabled for interrupt-on-change. The DEFVAL and INTCON registers
	 * must also be configured if any pins are enabled for interrupt-on-change */
	private static final int[] GPINTEN_REG = BANK0_GPINTEN_REG;
	/** The default comparison value is configured in the DEFVAL register. If enabled
	 * (via GPINTEN and INTCON) to compare against the DEFVAL register, an opposite
	 * value on the associated pin will cause an interrupt to occur */
	private static final int[] DEFVAL_REG = BANK0_DEFVAL_REG;
	/** The INTCON register controls how the associated pin value is compared for the
	 * interrupt-on-change feature. If a bit is set, the corresponding I/O pin is compared
	 * against the associated bit in the DEFVAL register. If a bit value is clear, the
	 * corresponding I/O pin is compared against the previous value */
	private static final int[] INTCON_REG = BANK0_INTCON_REG;
	/** I/O configuration register */
	private static final int[] IOCON_REG = BANK0_IOCON_REG;
	/** The GPPU register controls the pull-up resistors for the port pins. If a bit is
	 * set and the corresponding pin is configured as an input, the corresponding port pin is
	 * internally pulled up with a 100 kOhm resistor */
	private static final int[] GPPU_REG = BANK0_GPPU_REG;
	/** The INTF register reflects the interrupt condition on the port pins of any pin that is
	 * enabled for interrupts via the GPINTEN register. A 'set' bit indicates that the
	 * associated pin caused the interrupt. This register is 'read-only'. Writes to this
	 * register will be ignored */
	private static final int[] INTF_REG = BANK0_INTF_REG;
	/** The INTCAP register captures the GPIO port value at the time the interrupt occurred.
	 * The register is 'read-only' and is updated only when an interrupt occurs. The register
	 * will remain unchanged until the interrupt is cleared via a read of INTCAP or GPIO. */
	private static final int[] INTCAP_REG = BANK0_INTCAP_REG;
	/** The GPIO register reflects the value on the port. Reading from this register reads
	 * the port. Writing to this register modifies the Output Latch (OLAT) register */
	private static final int[] GPIO_REG = BANK0_GPIO_REG;
	/** The OLAT register provides access to the output latches. A read from this register
	 * results in a read of the OLAT and not the port itself. A write to this register
	 * modifies the output latches that modifies the pins configured as outputs */
	private static final int[] OLAT_REG = BANK0_OLAT_REG;
	
	private BoardPinInfo boardPinInfo;

	public MCP23x17(String deviceName) {
		this(deviceName, INTERRUPT_GPIO_NOT_SET, INTERRUPT_GPIO_NOT_SET);
	}
	
	public MCP23x17(String deviceName, int interruptGpio) {
		this(deviceName, interruptGpio, interruptGpio);
	}
	
	public MCP23x17(String deviceName, int interruptGpioA, int interruptGpioB) {
		super(NUM_PORTS, deviceName, interruptGpioA, interruptGpioB);
		
		boardPinInfo = new MCP23x17BoardPinInfo();
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
	
	public static class MCP23x17BoardPinInfo extends BoardPinInfo {
		public static final String BANK_A = "BANK-A";
		public static final String BANK_B = "BANK-B";
		
		public MCP23x17BoardPinInfo() {
			for (int i=0; i<8; i++) {
				addGpioPinInfo(BANK_A, i, 21+i, PinInfo.DIGITAL_IN_OUT);
			}
			for (int i=0; i<8; i++) {
				addGpioPinInfo(BANK_B, 8+i, 1+i, PinInfo.DIGITAL_IN_OUT);
			}
		}
	}
}
