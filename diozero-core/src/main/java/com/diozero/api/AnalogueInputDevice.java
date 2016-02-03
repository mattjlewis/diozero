package com.diozero.api;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.internal.spi.AnalogueInputDeviceFactoryInterface;
import com.diozero.internal.spi.GpioAnalogueInputDeviceInterface;

public class AnalogueInputDevice extends GpioDevice {
	private static final Logger logger = LogManager.getLogger(AnalogueInputDevice.class);
	
	private GpioAnalogueInputDeviceInterface device;

	public AnalogueInputDevice(int pinNumber) throws IOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory().provisionAnalogueInputPin(pinNumber));
	}

	public AnalogueInputDevice(AnalogueInputDeviceFactoryInterface deviceFactory, int pinNumber) throws IOException {
		this(deviceFactory.provisionAnalogueInputPin(pinNumber));
	}

	public AnalogueInputDevice(GpioAnalogueInputDeviceInterface device) {
		super(device.getPin());
		this.device = device;
	}

	@Override
	public void close() throws IOException {
		logger.debug("close()");
		if (device != null) { device.close(); }
	}
	
	public float getValue() throws IOException {
		return device.getValue();
	}
}
