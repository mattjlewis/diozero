package com.diozero.sandpit;

import java.io.Closeable;

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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import org.pmw.tinylog.Logger;

import com.diozero.api.*;
import com.diozero.internal.provider.piconzero.*;
import com.diozero.internal.spi.*;
import com.diozero.internal.spi.GpioDeviceInterface.Mode;
import com.diozero.util.RuntimeIOException;
import com.diozero.util.SleepUtil;

public class PiconZero extends AbstractDeviceFactory
implements GpioDeviceFactoryInterface, AnalogInputDeviceFactoryInterface, PwmOutputDeviceFactoryInterface, Closeable {
	public static enum InputConfig {
		DIGITAL(0), DIGITAL_PULL_UP(128), ANALOG(1), DS18B20(2);
		
		private int value;
		private InputConfig(int value) {
			this.value = value;
		}
		
		public int getValue() {
			return value;
		}
	}
	
	public static enum OutputConfig {
		DIGITAL(0), PWM(1), SERVO(2), WS2812B(3);
		
		private int value;
		private OutputConfig(int value) {
			this.value = value;
		}
		
		public int getValue() {
			return value;
		}
	}
	
	private static final int DEFAULT_ADDRESS = 0x22;
	
	private static final int REVISION_REG = 0x00;
	private static final int WS2812B_SET_PIXEL_NOUPDATE_REG = 0x00;
	private static final int WS2812B_SET_PIXEL_UPDATE_REG = 0x01;
	private static final int MOTOR0_REG = 0x00;
	private static final int INPUT0_REG = 0x01;
	private static final int OUTPUT0_CONFIG_REG = 0x02;
	private static final int OUTPUT0_REG = 0x08;
	private static final int INPUT0_CONFIG_REG = 0x0e;
	private static final int SET_BRIGHTNESS_REG = 0x12;
	private static final int UPDATE_NOW_REG = 0x13;
	private static final int RESET_REG = 0x14;
	
	private static final String DEVICE_NAME = "PiconZero";

	private static final int ALL_PIXELS = 100;
	private static final int MAX_VALUE = 100;
	private static final int NUM_INPUT_CHANNELS = 4;
	private static final int NUM_OUTPUT_CHANNELS = 6;
	private static final float MAX_ANALOG_VALUE = 1023;

	private I2CDevice device;
	private String keyPrefix;
	private InputConfig[] inputConfigs = new InputConfig[NUM_INPUT_CHANNELS];
	
	public PiconZero() {
		this(I2CConstants.BUS_1, DEFAULT_ADDRESS);
	}
	
	public PiconZero(int bus, int address) {
		device = new I2CDevice(bus, address, I2CConstants.ADDR_SIZE_7,
				I2CConstants.DEFAULT_CLOCK_FREQUENCY, ByteOrder.LITTLE_ENDIAN);
		keyPrefix = DEVICE_NAME + "-" + device.getController() + "-" + address + "-";
		
		reset();
	}
	
	private static void validateInputChannel(int channel) {
		if (channel < 0 || channel >= NUM_INPUT_CHANNELS) {
			throw new IllegalArgumentException("Invalid channel value (" + channel + "), must be 0..3");
		}
	}
	
	private static void validateOutputChannel(int channel) {
		if (channel < 0 || channel >= NUM_OUTPUT_CHANNELS) {
			throw new IllegalArgumentException("Invalid channel value (" + channel + "), must be 0..5");
		}
	}
	
	private static void validateChannelMode(int channel, InputConfig config) {
		if (channel < 0 || channel >= NUM_INPUT_CHANNELS) {
			throw new IllegalArgumentException("Invalid channel value (" + channel + "), must be 0..3");
		}
	}
	
	private static void validateChannelMode(int channel, OutputConfig config) {
		if (channel < 0 || channel >= NUM_OUTPUT_CHANNELS) {
			throw new IllegalArgumentException("Invalid channel value (" + channel + "), must be 0..5");
		}
		if (channel != 5 && config == OutputConfig.WS2812B) {
			throw new IllegalArgumentException("Invalid channel for WS2812B NeoPixels - only supported on channel 5");
		}
	}
	
	public void reset() {
		device.writeByte(RESET_REG, 0);
		SleepUtil.sleepMillis(1);
	}
	
	/**
	 * Get the board revision details
	 * @return revision[0]: Board type (2 == PiconZero); revision[1]: Firmware version
	 */
	public byte[] getRevision() {
		ByteBuffer buffer = device.read(REVISION_REG, 2);
		byte[] arr = new byte[buffer.remaining()];
		buffer.get(arr);
		return arr;
	}
	
	/**
	 * Set motor output value
	 * @param motor must be in range 0..1
	 * @param speed must be in range -128 - +127. Values of -127, -128, +127 are treated as always ON, no PWM
	 */
	public void setMotor(int motor, int speed) {
		if (motor < 0 || motor > 1) {
			throw new IllegalArgumentException("Invalid motor number (" + motor + ") must be 0..1");
		}
		device.writeByte(MOTOR0_REG + motor, speed);
	}
	
	/**
	 * Set configuration of selected input channel
	 * @param channel Input channel (0..3)
	 * @param config Input configuration (0: Digital, 1: Analog, 2: DS18B20)
	 */
	public void setInputConfig(int channel, InputConfig config) {
		validateChannelMode(channel, config);
		
		device.writeByte(INPUT0_CONFIG_REG + channel, config.getValue());
		inputConfigs[channel] = config; 
	}
	
	/**
	 * Set configuration of selected output
	 * @param channel Output channel (0..5)
	 * @param config Output configuration (0: Digital, 1: PWM, 2: Servo, 3: Neopixel WS2812B)
	 */
	public void setOutputConfig(int channel, OutputConfig config) {
		validateChannelMode(channel, config);
		
		device.writeByte(OUTPUT0_CONFIG_REG + channel, config.getValue());
	}
	
	public int readInput(int channel) {
		validateInputChannel(channel);
		
		return device.readUShort(INPUT0_REG + channel);
	}
	
	public float getValue(int channel) {
		int input = readInput(channel);
		
		switch (inputConfigs[channel]) {
		case ANALOG:
			return input / MAX_ANALOG_VALUE;
		case DIGITAL:
		case DIGITAL_PULL_UP:
		case DS18B20:
		default:
			return input;
		}
	}
	
	/**
	 * Set output data for selected output channel. 
	 * @param channel 0..5
	 * @param value output value:
	 * <pre>
	 * Mode  Name    Type    Values
	 * 0     On/Off  Byte    0 is OFF, 1 is ON
	 * 1     PWM     Byte    0 to 100 percentage of ON time
	 * 2     Servo   Byte    -100 to + 100 Position in degrees
	 * 3*    WS2812B 4 Bytes 0:Pixel ID, 1:Red, 2:Green, 3:Blue
	 * </pre>
	 * * Don't use this method if the output mode is WS2812B.
	 */
	public void setOutput(int channel, int value) {
		validateOutputChannel(channel);
		// Should really validate value based on the output config however we
		// don't actually need to - the PiconZero handles it for us plus would
		// require us to store the current output configuration
		
		device.writeByte(OUTPUT0_REG + channel, value);
	}
	
	/**
	 * Set output data for selected output channel. 
	 * @param channel 0..5
	 * @param value output value:
	 * <pre>
	 * Mode  Name    Type    Values
	 * 0     On/Off  Byte    0 is OFF, 1 is ON
	 * 1     PWM     Byte    0 to 1 percentage of ON time
	 * 2     Servo   Byte    -1 to + 1 Position in degrees
	 * 3*    WS2812B 4 Bytes 0:Pixel ID, 1:Red, 2:Green, 3:Blue
	 * </pre>
	 * * Don't use this method if the output mode is WS2812B.
	 */
	public void setValue(int channel, float value) {
		setOutput(channel, Math.round(MAX_VALUE * value));
	}
	
	/**
	 * Set the colour of an individual pixel (always output channel 5)
	 * @param pixel 0..63
	 * @param red 0..255
	 * @param green 0..255
	 * @param blue 0..255
	 * @param update update the pixel now if true
	 */
	public void setPixel(int pixel, int red, int green, int blue, boolean update) {
		//pixelData = [Pixel, Red, Green, Blue]
		//bus.write_i2c_block_data (pzaddr, Update, pixelData)
		byte[] pixel_data = new byte[] { (byte) pixel, (byte) red, (byte) green, (byte) blue };
		device.writeBytes(update ? WS2812B_SET_PIXEL_UPDATE_REG : WS2812B_SET_PIXEL_NOUPDATE_REG, 4, pixel_data);
	}
	
	/**
	 * Sets all pixels with the selected red, green and blue values (0 to 255) [Available from firmware revision 07]
	 * @param red 0..255
	 * @param green 0..255
	 * @param blue 0..255
	 * @param update update the pixel now if true
	 */
	public void setAllPixels(int red, int green, int blue, boolean update) {
		//pixelData = [100, Red, Green, Blue]
		//bus.write_i2c_block_data (pzaddr, Update, pixelData)
		setPixel(ALL_PIXELS, red, green, blue, update);
	}
	
	public void updatePixels() {
		device.writeByte(UPDATE_NOW_REG, 0);
	}
	
	public void setBrightness(int brightness) {
		device.writeByte(SET_BRIGHTNESS_REG, brightness);
	}

	@Override
	public void close() throws RuntimeIOException {
		reset();
		device.close();
	}

	@Override
	public String getName() {
		return DEVICE_NAME + "-" + device.getController() + "-" + device.getAddress();
	}

	@Override
	public int getPwmFrequency(int pinNumber) {
		// Not supported
		return 50;
	}

	@Override
	public void setPwmFrequency(int pinNumber, int pwmFrequency) {
		// Not supported
	}

	@Override
	public GpioAnalogInputDeviceInterface provisionAnalogInputPin(int pinNumber) throws RuntimeIOException {
		validateChannelMode(pinNumber, InputConfig.ANALOG);
		
		String key = keyPrefix + pinNumber;
		
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}
		
		GpioAnalogInputDeviceInterface device = new PiconZeroAnalogInputDevice(this, key, pinNumber);
		deviceOpened(device);
		
		return device;
	}

	@Override
	public GpioDigitalInputDeviceInterface provisionDigitalInputPin(int pinNumber, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws RuntimeIOException {
		validateChannelMode(pinNumber, pud == GpioPullUpDown.PULL_UP ? InputConfig.DIGITAL_PULL_UP : InputConfig.DIGITAL);
		
		String key = keyPrefix + pinNumber;
		
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}
		
		GpioDigitalInputDeviceInterface device = new PiconZeroDigitalInputDevice(this, key, pinNumber, pud, trigger);
		deviceOpened(device);
		
		return device;
	}

	@Override
	public GpioDigitalOutputDeviceInterface provisionDigitalOutputPin(int pinNumber, boolean initialValue)
			throws RuntimeIOException {
		validateChannelMode(pinNumber, OutputConfig.DIGITAL);
		
		String key = keyPrefix + pinNumber;
		
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}
		
		GpioDigitalOutputDeviceInterface device = new PiconZeroDigitalOutputDevice(this, key, pinNumber, initialValue);
		deviceOpened(device);
		
		return device;
	}

	@Override
	public PwmOutputDeviceInterface provisionPwmOutputPin(int pinNumber, float initialValue) throws RuntimeIOException {
		validateChannelMode(pinNumber, OutputConfig.PWM);
		
		String key = keyPrefix + pinNumber;
		
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}
		
		PwmOutputDeviceInterface device = new PiconZeroPwmOutputDevice(this, key, pinNumber, initialValue);
		deviceOpened(device);
		
		return device;
	}

	@Override
	public GpioDigitalInputOutputDeviceInterface provisionDigitalInputOutputPin(int pinNumber, Mode mode)
			throws RuntimeIOException {
		throw new UnsupportedOperationException("DigitalInputOutDevice isn't supported on PiconZero");
	}

	public void closeChannel(int channel) {
		Logger.debug("closeChannel({})", Integer.valueOf(channel));
		setInputConfig(channel, InputConfig.DIGITAL);
	}
}
