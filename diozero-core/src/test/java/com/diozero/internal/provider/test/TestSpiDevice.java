package com.diozero.internal.provider.test;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.api.SpiClockMode;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.SpiDeviceInterface;

public abstract class TestSpiDevice extends AbstractDevice implements SpiDeviceInterface {
	private static final Logger logger = LogManager.getLogger(TestSpiDevice.class);
	
	private int controller;
	private int chipSelect;

	public TestSpiDevice(String key, DeviceFactoryInterface deviceFactory, int controller, int chipSelect, int frequency,
			SpiClockMode spiClockMode) {
		super(key, deviceFactory);
		
		this.controller = controller;
		this.chipSelect = chipSelect;
	}

	@Override
	public void closeDevice() throws IOException {
		logger.debug("closeDevice()");
	}

	@Override
	public final int getController() {
		return controller;
	}

	@Override
	public final int getChipSelect() {
		return chipSelect;
	}
}
