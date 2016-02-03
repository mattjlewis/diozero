package com.diozero.internal.provider.mcp3008;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.MCP3008;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.GpioAnalogueInputDeviceInterface;

public class MCP3008AnalogueInputPin extends AbstractDevice implements GpioAnalogueInputDeviceInterface {
	private static final Logger logger = LogManager.getLogger(MCP3008AnalogueInputPin.class);
	
	private MCP3008 mcp3008;
	private int pinNumber;

	public MCP3008AnalogueInputPin(MCP3008 mcp3008, String key, int pinNumber) {
		super(key, mcp3008);
		this.mcp3008 = mcp3008;
		this.pinNumber = pinNumber;
	}

	@Override
	public void closeDevice() {
		logger.debug("closeDevice()");
		// TODO Nothing to do?
	}

	@Override
	public float getValue() throws IOException {
		return mcp3008.getVoltage(pinNumber);
	}

	@Override
	public int getPin() {
		return pinNumber;
	}
}
