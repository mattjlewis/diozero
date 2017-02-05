package com.diozero.internal.provider.mcp23xxx;

import com.diozero.api.I2CConstants;
import com.diozero.api.I2CDevice;
import com.diozero.util.RuntimeIOException;

/**
 * Datasheet: <a href="http://ww1.microchip.com/downloads/en/DeviceDoc/21952b.pdf">http://ww1.microchip.com/downloads/en/DeviceDoc/21952b.pdf</a>.
 * <p>The MCP23X17 consists of multiple 8-bit configuration registers for input, output and polarity selection. The
 * system master can enable the I/Os as either inputs or outputs by writing the I/O configuration bits (IODIRA/B).
 * The data for each input or output is kept in the corresponding input or output register. The polarity of
 * the Input Port register can be inverted with the Polarity Inversion register. All registers can be read by the
 * system master.</p>
 * <p>The 16-bit I/O port functionally consists of two 8-bit ports (PORTA and PORTB). The MCP23X17 can be
 * configured to operate in the 8-bit or 16-bit modes via IOCON.BANK.</p>
 * <p>There are two interrupt pins, INTA and INTB, that can be associated with their respective ports, or can be
 * logically OR'ed together so that both pins will activate if either port causes an interrupt.
 * A special mode (Byte mode with IOCON.BANK = 0) causes the address pointer to toggle between
 * associated A/B register pairs. For example, if the BANK bit is cleared and the Address Pointer is initially set
 * to address 12h (GPIOA) or 13h (GPIOB), the pointer will toggle between GPIOA and GPIOB. Note that the
 * Address Pointer can initially point to either address in the register pair.</p>
 */
public class MCP23017 extends MCP23x17 {
	// Default I2C address
	private static final int DEVICE_ADDRESS = 0x20;
	private static final String DEVICE_NAME = "MCP23017";

	private I2CDevice device;

	public MCP23017() throws RuntimeIOException {
		this(I2CConstants.BUS_1, DEVICE_ADDRESS, INTERRUPT_PIN_NOT_SET, INTERRUPT_PIN_NOT_SET);
	}

	public MCP23017(int interruptGpio) throws RuntimeIOException {
		this(I2CConstants.BUS_1, DEVICE_ADDRESS, interruptGpio, interruptGpio);
	}

	public MCP23017(int interruptGpioA, int interruptGpioB) throws RuntimeIOException {
		this(I2CConstants.BUS_1, DEVICE_ADDRESS, interruptGpioA, interruptGpioB);
	}

	public MCP23017(int controller, int address, int interruptGpio) throws RuntimeIOException {
		this(controller, address, interruptGpio, interruptGpio);
	}

	public MCP23017(int controller, int address, int interruptGpioA, int interruptGpioB) throws RuntimeIOException {
		super(DEVICE_NAME + "-" + controller + "-" + address + "-");
		
		device = new I2CDevice(controller, address, I2CConstants.ADDR_SIZE_7, I2CConstants.DEFAULT_CLOCK_FREQUENCY);
		
		initialise();
	}
	
	@Override
	public void close() throws RuntimeIOException {
		super.close();
		device.close();
	}
	
	@Override
	protected void writeByte(int register, byte value) {
		device.writeByte(register, value);
	}
	
	@Override
	protected byte readByte(int register) {
		return device.readByte(register);
	}
}
