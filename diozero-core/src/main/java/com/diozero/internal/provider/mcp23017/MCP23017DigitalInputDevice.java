package com.diozero.internal.provider.mcp23017;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.MCP23017;
import com.diozero.api.DigitalPinEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.GpioDigitalInputDeviceInterface;
import com.diozero.internal.spi.InternalPinListener;

// TODO Implement interrupt support for detecting value changes
public class MCP23017DigitalInputDevice extends AbstractDevice implements GpioDigitalInputDeviceInterface {
	private static final Logger logger = LogManager.getLogger(MCP23017DigitalInputDevice.class);

	private MCP23017 mcp23017;
	private int pinNumber;
	private InternalPinListener listener;
	private GpioEventTrigger trigger;

	public MCP23017DigitalInputDevice(MCP23017 mcp23017, String key, int pinNumber, GpioEventTrigger trigger) {
		super(key, mcp23017);

		this.mcp23017 = mcp23017;
		this.pinNumber = pinNumber;
		this.trigger = trigger;
	}

	@Override
	public void closeDevice() throws IOException {
		logger.debug("closeDevice()");
		removeListener();
		mcp23017.closePin(pinNumber);
	}

	@Override
	public boolean getValue() throws IOException {
		return mcp23017.getValue(pinNumber);
	}

	@Override
	public int getPin() {
		return pinNumber;
	}

	@Override
	public void setDebounceTimeMillis(int debounceTime) {
		// TODO Auto-generated method stub
	}

	@Override
	public void setListener(InternalPinListener listener) {
		this.listener = listener;
	}
	
	public void valueChanged(DigitalPinEvent event) {
		if (listener != null) {
			listener.valueChanged(event);
		}
	}

	@Override
	public void removeListener() {
		listener = null;
	}
}
