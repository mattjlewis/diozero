package com.diozero.internal.provider.mcp23xxx;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.pmw.tinylog.Logger;

import com.diozero.api.I2CConstants;
import com.diozero.api.SPIConstants;
import com.diozero.api.SpiDevice;
import com.diozero.util.RuntimeIOException;

public class MCP23S17 extends MCP23x17 {
	// SPI Address Register  0b[0 1 0 0 A2 A1 A0 R/W]
	private static final byte ADDRESS_0 = 0b01000000; // 0x40 [0100 0000] [A2 = 0 | A1 = 0 | A0 = 0]
	private static final byte ADDRESS_1 = 0b01000010; // 0x42 [0100 0010] [A2 = 0 | A1 = 0 | A0 = 1]
	private static final byte ADDRESS_2 = 0b01000100; // 0x44 [0100 0100] [A2 = 0 | A1 = 1 | A0 = 0]
	private static final byte ADDRESS_3 = 0b01000110; // 0x46 [0100 0110] [A2 = 0 | A1 = 1 | A0 = 1]
	private static final byte ADDRESS_4 = 0b01001000; // 0x48 [0100 1000] [A2 = 1 | A1 = 0 | A0 = 0]
	private static final byte ADDRESS_5 = 0b01001010; // 0x4A [0100 1010] [A2 = 1 | A1 = 0 | A0 = 1]
	private static final byte ADDRESS_6 = 0b01001100; // 0x4C [0100 1100] [A2 = 1 | A1 = 1 | A0 = 0]
	private static final byte ADDRESS_7 = 0b01001110; // 0x4E [0100 1110] [A2 = 1 | A1 = 1 | A0 = 1]
	private static final byte DEFAULT_ADDRESS = ADDRESS_0;
	public static final byte WRITE_FLAG = 0b00000000;    // 0x00
	public static final byte READ_FLAG  = 0b00000001;    // 0x01
	private static final String DEVICE_NAME = "MCP23S17";

	private SpiDevice device;
	private byte address = DEFAULT_ADDRESS;

	public MCP23S17() throws RuntimeIOException {
		this(SPIConstants.DEFAULT_SPI_CONTROLLER, SPIConstants.CE0, INTERRUPT_PIN_NOT_SET, INTERRUPT_PIN_NOT_SET);
	}

	public MCP23S17(int interruptGpio) throws RuntimeIOException {
		this(I2CConstants.BUS_1, SPIConstants.CE0, interruptGpio, interruptGpio);
	}

	public MCP23S17(int interruptGpioA, int interruptGpioB) throws RuntimeIOException {
		this(I2CConstants.BUS_1, SPIConstants.CE0, interruptGpioA, interruptGpioB);
	}

	public MCP23S17(int controller, int chipSelect, int interruptGpio) throws RuntimeIOException {
		this(controller, chipSelect, interruptGpio, interruptGpio);
	}

	public MCP23S17(int controller, int chipSelect, int interruptGpioA, int interruptGpioB) throws RuntimeIOException {
		super(DEVICE_NAME + "-" + controller + "-" + chipSelect + "-");
		
		device = new SpiDevice(controller, chipSelect);
		
		initialise();
	}
	
	@Override
	public void close() throws RuntimeIOException {
		super.close();
		device.close();
	}
	
	@Override
	protected byte readByte(int register) {
		ByteBuffer tx = ByteBuffer.allocate(3);
		tx.put((byte) (address | READ_FLAG));
		tx.put((byte) register);
		tx.put((byte) 0);
		tx.flip();

        ByteBuffer rx = device.writeAndRead(tx);

        Logger.info("{}, {}, {}", rx.get(0), rx.get(1), rx.get(2));
        return (byte) (rx.get(2) & 0xFF);
	}
	
	@Override
	protected void writeByte(int register, byte value) {
		ByteBuffer tx = ByteBuffer.allocate(3);
		tx.put((byte) (address | WRITE_FLAG));
		tx.put((byte) register);
		tx.put(value);
		tx.flip();

		device.writeAndRead(tx);
	}
}
