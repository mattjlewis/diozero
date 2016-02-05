package com.diozero.internal.provider.pca9685;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 diozero
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */


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
