package com.diozero.internal.provider.jdkdio10;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.api.SPIConstants;
import com.diozero.api.SpiClockMode;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.SpiDeviceInterface;

import jdk.dio.Device;
import jdk.dio.DeviceManager;
import jdk.dio.spibus.SPIDevice;
import jdk.dio.spibus.SPIDeviceConfig;

public class JdkDeviceIoSpiDevice extends AbstractDevice implements SpiDeviceInterface {
	private static final Logger logger = LogManager.getLogger(JdkDeviceIoSpiDevice.class);

	private SPIDeviceConfig deviceConfig;
	private SPIDevice device;
	
	public JdkDeviceIoSpiDevice(String key, DeviceFactoryInterface deviceFactory, int controller, int chipSelect, int frequency, SpiClockMode mode) throws IOException {
		super(key, deviceFactory);
		
		int cs_active = SPIDeviceConfig.CS_ACTIVE_LOW;
		//int clock_frequency = SPIDeviceConfig.DEFAULT;
		//int clock_frequency = RPiConstants.SPI_DEFAULT_CLOCK;
		int word_length = SPIConstants.DEFAULT_WORD_LENGTH;
		int bit_ordering = Device.BIG_ENDIAN;
		deviceConfig = new SPIDeviceConfig(controller, chipSelect, cs_active,
				frequency, mode.getMode(), word_length, bit_ordering);
		device = DeviceManager.open(deviceConfig);
	}

	@Override
	public void closeDevice() throws IOException {
		logger.debug("closeDevice()");
		if (device.isOpen()) {
			device.close();
		}
	}

	@Override
	public ByteBuffer writeAndRead(ByteBuffer src) throws IOException {
		if (! device.isOpen()) {
			throw new IllegalStateException("SPI Device " +
					deviceConfig.getControllerNumber() + "-" + deviceConfig.getAddress() + " is closed");
		}
		
		ByteBuffer dest = ByteBuffer.allocateDirect(src.capacity());
		device.writeAndRead(src, dest);
		return dest;
	}

	@Override
	public int getController() {
		return deviceConfig.getControllerNumber();
	}

	@Override
	public int getChipSelect() {
		return deviceConfig.getAddress();
	}
}
