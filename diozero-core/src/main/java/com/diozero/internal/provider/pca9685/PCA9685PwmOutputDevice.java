package com.diozero.internal.provider.pca9685;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.PwmOutputDeviceInterface;
import com.diozero.sandpit.PCA9685;

public class PCA9685PwmOutputDevice extends AbstractDevice implements PwmOutputDeviceInterface {
	private static final Logger logger = LogManager.getLogger(PCA9685PwmOutputDevice.class);

	private PCA9685 pca9685;
	private int channel;
	
	public PCA9685PwmOutputDevice(PCA9685 pca9685, String key, int channel) {
		super(key, pca9685);
		
		this.pca9685 = pca9685;
		this.channel = channel;
	}

	@Override
	public int getPin() {
		return channel;
	}

	@Override
	public float getValue() throws IOException {
		return pca9685.getValue(channel);
	}

	@Override
	public void setValue(float value) throws IOException {
		pca9685.setValue(channel, value);
	}

	@Override
	protected void closeDevice() throws IOException {
		logger.debug("closeDevice()");
		pca9685.closeChannel(channel);
	}
}
