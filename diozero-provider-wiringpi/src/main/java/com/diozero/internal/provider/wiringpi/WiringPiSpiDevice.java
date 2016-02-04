package com.diozero.internal.provider.wiringpi;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.api.SpiClockMode;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.SpiDeviceInterface;
import com.pi4j.wiringpi.Spi;

public class WiringPiSpiDevice extends AbstractDevice implements SpiDeviceInterface {
	private static final Logger logger = LogManager.getLogger(WiringPiSpiDevice.class);
	
	//private final SpiDevice spiDevice;
	private int controller;
	private int chipSelect;
	
	public WiringPiSpiDevice(String key, DeviceFactoryInterface deviceFactory, int controller,
			int chipSelect, int speed, SpiClockMode mode) throws IOException {
		super(key, deviceFactory);
		
		this.controller = controller;
		this.chipSelect = chipSelect;
		
		int wpi_spi_mode;
		switch (mode) {
		case MODE_0:
			wpi_spi_mode = Spi.MODE_0;
			break;
		case MODE_1:
			wpi_spi_mode = Spi.MODE_1;
			break;
		case MODE_2:
			wpi_spi_mode = Spi.MODE_2;
			break;
		case MODE_3:
		default:
			wpi_spi_mode = Spi.MODE_3;
			break;
		}
		
		//spiDevice = SpiFactory.getInstance(SpiChannel.getByNumber(chipSelect), speed, SpiMode.getByNumber(mode.getMode()));
		if (Spi.wiringPiSPISetupMode(controller, speed, wpi_spi_mode) == -1) {
			throw new IOException("Error initialising SPI controller " + controller + ", speed=" + speed + ", mode=" + wpi_spi_mode);
		}
	}

	@Override
	public void closeDevice() throws IOException {
		logger.debug("closeDevice()");
		// No way to close a wiringPi SPI device file handle?!
		//spiDevice.close();
	}

	@Override
	public ByteBuffer writeAndRead(ByteBuffer out) throws IOException {
		byte[] out_array = out.array();
		byte[] buffer = new byte[out_array.length];
		System.arraycopy(out_array, 0, buffer, 0, out_array.length);
		if (Spi.wiringPiSPIDataRW(controller, buffer) == -1) {
			throw new IOException("Error writing " + out_array.length + " bytes of data to SPI controller " + controller);
		}
		return ByteBuffer.wrap(buffer);
		//return spiDevice.write(out);
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
