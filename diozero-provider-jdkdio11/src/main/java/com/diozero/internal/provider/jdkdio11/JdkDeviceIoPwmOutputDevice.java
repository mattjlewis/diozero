package com.diozero.internal.provider.jdkdio11;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.api.PwmType;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.PwmOutputDeviceInterface;

import jdk.dio.DeviceConfig;
import jdk.dio.DeviceManager;
import jdk.dio.gpio.GPIOPinConfig;
import jdk.dio.pwm.PWMChannel;
import jdk.dio.pwm.PWMChannelConfig;

/**
 * Represent Pulse Width Modulation controlled output device
 */
public class JdkDeviceIoPwmOutputDevice extends AbstractDevice implements PwmOutputDeviceInterface {
	private static final Logger logger = LogManager.getLogger(JdkDeviceIoPwmOutputDevice.class);

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
	
	JdkDeviceIoPwmOutputDevice(String key, DeviceFactoryInterface deviceFactory, int pinNumber,
			float initialValue, PwmType pwmType) throws IOException {
		super(key, deviceFactory);
		
		/*
		 * From the JavaDoc:
		PWMChannelConfig config = new PWMChannelConfig.Builder()
			     .setControllerNumber(1)
			     .setChannelNumber(1)
			     .setOutputConfig(new GPIOPinConfig.Builder()
			                          .setControllerNumber(3)
			                          .setPinNumber(1)
			                          .setDirection(GPIOPinConfig.DIR_OUTPUT_ONLY)
			                          .setDriveMode(GPIOPinConfig.MODE_OUTPUT_PUSH_PULL)
			                          .build())
			     .setScaleFactor(1.0)
			     .setPulsePeriod(10)
			     .setIdleState(IDLE_STATE_HIGH)
			     .setPulseAlignment(ALIGN_CENTER)
			     .setOutputBufferSize(0)
			     .build();
		*/
		
		GPIOPinConfig gpio_pin_config = new GPIOPinConfig.Builder().setControllerNumber(DeviceConfig.UNASSIGNED)
				.setPinNumber(pinNumber).setDirection(GPIOPinConfig.DIR_OUTPUT_ONLY)
				.setDriveMode(GPIOPinConfig.MODE_OUTPUT_PUSH_PULL)
				.setInitValue(false).build();
		this.frequency = DEFAULT_FREQUENCY;
		// TODO What value for PWM Channel Number?! Same as the GPIO Output Pin number?
		int pwm_channel_number = pinNumber;
		int idle_state = DeviceConfig.UNASSIGNED;		// IDLE_STATE_HIGH or IDLE_STATE_LOW
		pulsePeriodMs = (int)(1000 / frequency);
		int pulse_alignment = DeviceConfig.UNASSIGNED;	// ALIGN_CENTER, ALIGN_LEFT or ALIGN_RIGHT
		pwmChannelConfig = new PWMChannelConfig.Builder().setControllerNumber(DeviceConfig.UNASSIGNED)
				.setChannelNumber(pwm_channel_number).setIdleState(idle_state).setPulsePeriod(pulsePeriodMs)
				.setPulseAlignment(pulse_alignment).setOutputConfig(gpio_pin_config).build();
		pwmChannel = DeviceManager.open(PWMChannel.class, pwmChannelConfig);
	}

	@Override
	public void closeDevice() throws IOException {
		logger.debug("closeDevice()");
		if (pwmChannel.isOpen()) {
			pwmChannel.close();
		}
	}

	// Exposed properties
	@Override
	public int getPin() {
		return pwmChannelConfig.getOutputConfig().getPinNumber();
	}
	
	public float getFrequency() {
		return frequency;
	}
	
	public void setFrequency(float frequency) throws IOException {
		float min_frequency = 1000f / pwmChannel.getMinPulsePeriod();
		float max_frequency = 1000f / pwmChannel.getMaxPulsePeriod();
		if (frequency < min_frequency || frequency > max_frequency) {
			throw new java.lang.IllegalArgumentException(
					String.format("Frequency (%f) must be between %f and %f", Float.valueOf(frequency),
					Float.valueOf(min_frequency), Float.valueOf(max_frequency)));
		}
		this.frequency = frequency;
		pulsePeriodMs = (int)(1000 / frequency);
		synchronized (pwmChannel) {
			pwmChannel.setPulsePeriod(pulsePeriodMs);
		}
	}
	
	@Override
	public float getValue() {
		return value;
	}
	
	@Override
	public void setValue(float value) throws IOException {
		if (value < 0 || value > 1) {
			throw new java.lang.IllegalArgumentException("Value (" + value + ") must be between 0 and 1");
		}
		
		synchronized (pwmChannel) {
			this.value = value;
			pulseWidthMs = (int)(value * pulsePeriodMs);
			
			if (value == 0) {
				pwmChannel.stopGeneration();
				return;
			}
			
			pwmChannel.startGeneration(pulseWidthMs);
		}
	}
}
