package com.diozero.api;

import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.internal.spi.SpiDeviceInterface;

/**
 * https://www.raspberrypi.org/documentation/hardware/raspberrypi/spi/README.md
 * For modern Raspberry Pis:
 * 2 SPI controllers, 0 (SPI-0) and 1 (SPI-1)
 * Controller 0 has 2 channels (CE-0 on physical pin 24, CE-1 on physical pin 26)
 * Controller 1 has 3 channels (CE-0 on physical pin 12, CE-1 on physical pin 11, CE-2 on physical pin 36)
 * SPI-1 is more limited that SPI-0 on the Raspberry Pi (https://www.raspberrypi.org/forums/viewtopic.php?t=81903&p=579154)
 * - The SPI-1 clock is derived from the system clock therefore you have to be careful when over/underclocking to set the right divisor
 * - Limited IRQ support, no thresholding on the FIFO except "TX empty" or "done".
 * - No DMA support (no peripheral DREQ)
 */
public class SpiDevice implements Closeable, SPIConstants {
	private static final Logger logger = LogManager.getLogger(SpiDevice.class);
	
	private SpiDeviceInterface device;
	
	public SpiDevice(int chipSelect) throws IOException {
		this(DEFAULT_SPI_CONTROLLER, chipSelect, DEFAULT_SPI_CLOCK_FREQUENCY, DEFAULT_SPI_CLOCK_MODE);
	}
	
	public SpiDevice(int controller, int chipSelect) throws IOException {
		this(controller, chipSelect, DEFAULT_SPI_CLOCK_FREQUENCY, DEFAULT_SPI_CLOCK_MODE);
	}
	
	public SpiDevice(int controller, int chipSelect, int frequency, SpiClockMode mode) throws IOException {
		device = DeviceFactoryHelper.getNativeDeviceFactory().provisionSpiDevice(controller, chipSelect, frequency, mode);
	}

	@Override
	public void close() throws IOException {
		logger.debug("close()");
		device.close();
	}

	public ByteBuffer writeAndRead(ByteBuffer out) throws IOException {
		return device.writeAndRead(out);
	}
	
	public int getController() {
		return device.getController();
	}
	
	public int getChipSelect() {
		return device.getChipSelect();
	}
}
