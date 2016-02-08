package com.diozero.internal.provider.pigpioj;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.api.SpiClockMode;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.SpiDeviceInterface;
import com.diozero.pigpioj.PigpioSPI;

public class PigpioJSpiDevice extends AbstractDevice implements SpiDeviceInterface {
	private static final Logger logger = LogManager.getLogger(PigpioJSpiDevice.class);

	private static final int CLOSED = -1;
	
	private int handle = CLOSED;
	private int controller;
	private int chipSelect;

	public PigpioJSpiDevice(String key, DeviceFactoryInterface deviceFactory, int controller,
			int chipSelect, int frequency, SpiClockMode spiClockMode) throws IOException {
		super(key, deviceFactory);
		
		this.controller = controller;
		this.chipSelect = chipSelect;
		
		int flags = createSpiFlags(spiClockMode, chipSelect);
		handle = PigpioSPI.spiOpen(controller, frequency, flags);
		logger.debug("SPI device (" + controller + "-" + chipSelect + ") opened, handle=" + handle);
		if (handle < 0) {
			handle = CLOSED;
			throw new IOException(String.format("Error opening SPI device on controller %d, chip-select %d",
					Integer.valueOf(controller), Integer.valueOf(chipSelect)));
		}
	}

	@Override
	public ByteBuffer writeAndRead(ByteBuffer out) throws IOException {
		if (! isOpen()) {
			throw new IllegalStateException("SPI Device " + controller + "-" + chipSelect + " is closed");
		}
		
		int count = out.remaining();
		byte[] tx = new byte[count];
		out.get(tx);
		byte[] rx = new byte[count];
		PigpioSPI.spiXfer(handle, tx, rx, count);
		
		return ByteBuffer.wrap(rx);
	}

	@Override
	public int getController() {
		return controller;
	}

	@Override
	public int getChipSelect() {
		return chipSelect;
	}

	@Override
	public boolean isOpen() {
		return handle >= 0;
	}

	@Override
	protected void closeDevice() throws IOException {
		PigpioSPI.spiClose(handle);
		handle = CLOSED;
	}
	
	/**
	 * 21 20 19 18 17 16 15 14 13 12 11 10  9  8  7  6  5  4  3  2  1  0
	 *  b  b  b  b  b  b  R  T  n  n  n  n  W  A u2 u1 u0 p2 p1 p0  m  m
	 * mm defines the SPI mode.
	 * Warning: modes 1 and 3 do not appear to work on the auxiliary device.
	 * Mode POL PHA
	 *  0    0   0
	 *  1    0   1
	 *  2    1   0
	 *  3    1   1
	 * px is 0 if CEx is active low (default) and 1 for active high.
	 * ux is 0 if the CEx gpio is reserved for SPI (default) and 1 otherwise.
	 * A is 0 for the standard SPI device, 1 for the auxiliary SPI. The auxiliary device
	 * is only present on the A+/B+/Pi2/Zero.
	 * W is 0 if the device is not 3-wire, 1 if the device is 3-wire. Standard SPI device only.
	 * nnnn defines the number of bytes (0-15) to write before switching the MOSI line
	 * to MISO to read data. This field is ignored if W is not set. Standard SPI device only.
	 * T is 1 if the least significant bit is transmitted on MOSI first, the default (0)
	 * shifts the most significant bit out first. Auxiliary SPI device only.
	 * R is 1 if the least significant bit is received on MISO first, the default (0) receives
	 * the most significant bit first. Auxiliary SPI device only.
	 * bbbbbb defines the word size in bits (0-32).
	 * The default (0) sets 8 bits per word. Auxiliary SPI device only.
	 * The other bits in flags should be set to zero
	 */
	private static int createSpiFlags(SpiClockMode clockMode, int chipEnable) {
		int flags = clockMode.getMode();
		
		// CE0 is the standard SPI device, CE1 is auxiliary
		if (chipEnable == 1) {
			flags |= 0x0100;
		}
		
		return flags;
	}
}
