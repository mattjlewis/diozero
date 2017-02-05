package com.diozero.internal.provider.mcp23xxx;

import com.diozero.api.I2CConstants;
import com.diozero.api.I2CDevice;
import com.diozero.util.RuntimeIOException;

public class MCP23008 extends MCP23x08 {
	// Default I2C address
	private static final int DEVICE_ADDRESS = 0x20;
	private static final String DEVICE_NAME = "MCP23008";

	private I2CDevice device;

	public MCP23008() throws RuntimeIOException {
		this(I2CConstants.BUS_1, DEVICE_ADDRESS, INTERRUPT_PIN_NOT_SET, INTERRUPT_PIN_NOT_SET);
	}

	public MCP23008(int interruptGpio) throws RuntimeIOException {
		this(I2CConstants.BUS_1, DEVICE_ADDRESS, interruptGpio, interruptGpio);
	}

	public MCP23008(int interruptGpioA, int interruptGpioB) throws RuntimeIOException {
		this(I2CConstants.BUS_1, DEVICE_ADDRESS, interruptGpioA, interruptGpioB);
	}

	public MCP23008(int controller, int address, int interruptGpio) throws RuntimeIOException {
		this(controller, address, interruptGpio, interruptGpio);
	}

	public MCP23008(int controller, int address, int interruptGpioA, int interruptGpioB) throws RuntimeIOException {
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
	protected byte readByte(int register) {
		return device.readByte(register);
	}

	@Override
	protected void writeByte(int register, byte value) {
		device.writeByte(register, value);
	}
}
