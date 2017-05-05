package com.diozero.internal.provider.jdkdio10;

/*
 * #%L
 * Device I/O Zero - JDK Device I/O v1.0 provider
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

import org.pmw.tinylog.Logger;

import com.diozero.api.PwmType;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.PwmOutputDeviceInterface;
import com.diozero.util.RuntimeIOException;

import jdk.dio.DeviceConfig;
import jdk.dio.DeviceManager;
import jdk.dio.gpio.GPIOPinConfig;
import jdk.dio.pwm.PWMChannel;
import jdk.dio.pwm.PWMChannelConfig;

/**
 * Represent Pulse Width Modulation controlled output device
 */
public class JdkDeviceIoPwmOutputDevice extends AbstractDevice implements PwmOutputDeviceInterface {
	private static final float DEFAULT_FREQUENCY = 1000;
	
	// FIXME Note no PWM device support in JDK Device I/O, need an alternative e.g. servoblaster or pigpio
	private PWMChannelConfig pwmChannelConfig;
	private PWMChannel pwmChannel;
	private float frequency;
	// The pulse period in microseconds
	private int pulsePeriodMs;
	// Range 0..1
	private float value;
	private int pulseWidthMs;
	
	JdkDeviceIoPwmOutputDevice(String key, DeviceFactoryInterface deviceFactory, int gpio,
			float initialValue, PwmType pwmType) throws RuntimeIOException {
		super(key, deviceFactory);
		
		GPIOPinConfig gpio_pin_config = new GPIOPinConfig(DeviceConfig.DEFAULT, gpio, GPIOPinConfig.DIR_OUTPUT_ONLY,
				GPIOPinConfig.MODE_OUTPUT_PUSH_PULL, GPIOPinConfig.TRIGGER_NONE, false);
		this.frequency = DEFAULT_FREQUENCY;
		// TODO What value for PWM Channel Number?! Same as the GPIO Output Pin number?
		int pwm_channel_number = gpio;
		int idle_state = DeviceConfig.DEFAULT;		// IDLE_STATE_HIGH or IDLE_STATE_LOW
		pulsePeriodMs = Math.round(1000 / frequency);
		int pulse_alignment = DeviceConfig.DEFAULT;	// ALIGN_CENTER, ALIGN_LEFT or ALIGN_RIGHT
		pwmChannelConfig = new PWMChannelConfig(DeviceConfig.DEFAULT, pwm_channel_number, idle_state,
				pulsePeriodMs, pulse_alignment, gpio_pin_config);
		try {
			pwmChannel = DeviceManager.open(PWMChannel.class, pwmChannelConfig);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	@Override
	public void closeDevice() throws RuntimeIOException {
		Logger.debug("closeDevice()");
		if (pwmChannel.isOpen()) {
			try {
				pwmChannel.close();
			} catch (IOException e) {
				throw new RuntimeIOException(e);
			}
		}
	}

	// Exposed properties
	@Override
	public int getGpio() {
		return pwmChannelConfig.getOutputConfig().getPinNumber();
	}
	
	@Override
	public int getPwmNum() {
		return pwmChannelConfig.getOutputConfig().getPinNumber();
	}
	
	public float getFrequency() {
		return frequency;
	}
	
	public void setFrequency(float frequency) throws RuntimeIOException {
		try {
			float min_frequency = 1000f / pwmChannel.getMinPulsePeriod();
			float max_frequency = 1000f / pwmChannel.getMaxPulsePeriod();
			if (frequency < min_frequency || frequency > max_frequency) {
				throw new java.lang.IllegalArgumentException(
						String.format("Frequency (%f) must be between %f and %f", Float.valueOf(frequency),
						Float.valueOf(min_frequency), Float.valueOf(max_frequency)));
			}
			this.frequency = frequency;
			pulsePeriodMs = Math.round(1000 / frequency);
			synchronized (pwmChannel) {
				pwmChannel.setPulsePeriod(pulsePeriodMs);
			}
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}
	
	@Override
	public float getValue() {
		return value;
	}
	
	@Override
	public void setValue(float value) throws RuntimeIOException {
		if (value < 0 || value > 1) {
			throw new java.lang.IllegalArgumentException("Value (" + value + ") must be between 0 and 1");
		}
		
		synchronized (pwmChannel) {
			this.value = value;
			pulseWidthMs = Math.round(value * pulsePeriodMs);
			
			try {
				if (value == 0) {
					pwmChannel.stopGeneration();
					return;
				}
				
				pwmChannel.startGeneration(pulseWidthMs);
			} catch (IOException e) {
				throw new RuntimeIOException(e);
			}
		}
	}
}
