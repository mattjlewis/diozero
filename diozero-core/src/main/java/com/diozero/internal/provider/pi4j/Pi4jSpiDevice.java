package com.diozero.internal.provider.pi4j;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.api.SpiClockMode;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.SpiDeviceInterface;
import com.pi4j.io.spi.*;

public class Pi4jSpiDevice extends AbstractDevice implements SpiDeviceInterface {
	private static final Logger logger = LogManager.getLogger(Pi4jSpiDevice.class);

	private final SpiDevice spiDevice;
	private int controller;
	private int chipSelect;
	
	public Pi4jSpiDevice(String key, DeviceFactoryInterface deviceFactory, int controller, int chipSelect, int speed, SpiClockMode mode) throws IOException {
		super(key, deviceFactory);
		
		this.controller = controller;
		this.chipSelect = chipSelect;
		spiDevice = SpiFactory.getInstance(SpiChannel.getByNumber(chipSelect), speed, SpiMode.getByNumber(mode.getMode()));
	}

	@Override
	public void closeDevice() throws IOException {
		logger.debug("closeDevice()");
		// No way to close a Pi4J SPI Device?!
		//spiDevice.close();
	}

	@Override
	public ByteBuffer writeAndRead(ByteBuffer out) throws IOException {
		return spiDevice.write(out);
	}

	@Override
	public int getController() {
		return controller;
	}

	@Override
	public int getChipSelect() {
		return chipSelect;
	}
}
