package com.diozero.internal.provider.mcp23017;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.MCP23017;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;

public class MCP23017DigitalOutputDevice extends AbstractDevice implements GpioDigitalOutputDeviceInterface {
	private static final Logger logger = LogManager.getLogger(MCP23017DigitalOutputDevice.class);

	private MCP23017 mcp23017;
	private int pinNumber;

	public MCP23017DigitalOutputDevice(MCP23017 mcp23017, String key, int pinNumber) {
		super(key, mcp23017);
		
		this.mcp23017 = mcp23017;
		this.pinNumber = pinNumber;
	}

	@Override
	public boolean getValue() throws IOException {
		return mcp23017.getValue(pinNumber);
	}

	@Override
	public void setValue(boolean value) throws IOException {
		mcp23017.setValue(pinNumber, value);
	}

	@Override
	public int getPin() {
		return pinNumber;
	}

	@Override
	protected void closeDevice() throws IOException {
		logger.debug("closeDevice()");
		mcp23017.closePin(pinNumber);
	}
}
