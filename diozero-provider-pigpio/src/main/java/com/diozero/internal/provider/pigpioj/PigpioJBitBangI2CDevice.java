package com.diozero.internal.provider.pigpioj;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.pmw.tinylog.Logger;

import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.pigpioj.PigpioBitBangI2C;
import com.diozero.util.RuntimeIOException;

public class PigpioJBitBangI2CDevice extends AbstractDevice {
	private int sda;
	private boolean open;
	
	public PigpioJBitBangI2CDevice(String key, DeviceFactoryInterface deviceFactory, int sda, int scl, int baud) {
		super(key, deviceFactory);
		
		this.sda = sda;
		
		int rc = PigpioBitBangI2C.bbI2COpen(sda, scl, baud);
		if (rc < 0) {
			throw new RuntimeIOException("Error in bbI2COpen(" + sda + ", " + scl + ", " + baud + "): " + rc);
		}
		open = true;
	}

	@Override
	public boolean isOpen() {
		return open;
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.debug("closeDevice()");
		int rc = PigpioBitBangI2C.bbI2CClose(sda);
		open = false;
		if (rc < 0) {
			throw new RuntimeIOException("Error calling PigpioBitBangI2C.bbI2CClose(" + sda + "), response: " + rc);
		}
	}
	
	public ByteBuffer bbI2CZip(ByteBuffer src, int readLen) throws RuntimeIOException {
		if (! isOpen()) {
			throw new IllegalStateException("BitBang I2C Device " + getKey() + " is closed");
		}
		
		src.order(ByteOrder.LITTLE_ENDIAN);
		
		int tx_count = src.remaining();
		byte[] tx = new byte[tx_count];
		src.get(tx);
		byte[] rx = new byte[readLen];
		int rc = PigpioBitBangI2C.bbI2CZip(sda, tx, tx_count, rx, readLen);
		if (rc < 0) {
			throw new RuntimeIOException("Error calling bbI2CZip: " + rc);
		}
		
		ByteBuffer out = ByteBuffer.wrap(rx);
		out.order(ByteOrder.LITTLE_ENDIAN);
		return out;
	}
}
