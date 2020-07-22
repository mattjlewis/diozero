package com.diozero.devices;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     PiconZero.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
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

import com.diozero.api.AnalogInputEvent;
import com.diozero.api.DeviceAlreadyOpenedException;
import com.diozero.api.DeviceMode;
import com.diozero.api.DigitalInputEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.I2CConstants;
import com.diozero.api.I2CDevice;
import com.diozero.api.InputEventListener;
import com.diozero.api.PinInfo;
import com.diozero.internal.provider.AbstractDevice;
import com.diozero.internal.provider.AbstractDeviceFactory;
import com.diozero.internal.provider.AbstractInputDevice;
import com.diozero.internal.provider.AnalogInputDeviceFactoryInterface;
import com.diozero.internal.provider.AnalogInputDeviceInterface;
import com.diozero.internal.provider.AnalogOutputDeviceFactoryInterface;
import com.diozero.internal.provider.AnalogOutputDeviceInterface;
import com.diozero.internal.provider.GpioDeviceFactoryInterface;
import com.diozero.internal.provider.GpioDigitalInputDeviceInterface;
import com.diozero.internal.provider.GpioDigitalInputOutputDeviceInterface;
import com.diozero.internal.provider.GpioDigitalOutputDeviceInterface;
import com.diozero.internal.provider.PwmOutputDeviceFactoryInterface;
import com.diozero.internal.provider.PwmOutputDeviceInterface;
import com.diozero.util.BoardPinInfo;
import com.diozero.util.RangeUtil;
import com.diozero.util.RuntimeIOException;
import com.diozero.util.SleepUtil;

public class PiconZero extends AbstractDeviceFactory
implements GpioDeviceFactoryInterface, PwmOutputDeviceFactoryInterface,
	AnalogInputDeviceFactoryInterface, AnalogOutputDeviceFactoryInterface {
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
	
	private static final int PICON_ZERO_PWM_FREQUENCY = 50;
	
	private static final int DEFAULT_ADDRESS = 0x22;
	private static final int MAX_I2C_RETRIES = 5;
	
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
	private static final float VREF = 5;

	public static final int ALL_PIXELS = 100;
	public static final int MAX_OUTPUT_VALUE = 100;
	public static final int NUM_MOTORS = 2;
	public static final int NUM_INPUT_CHANNELS = 4;
	public static final int NUM_OUTPUT_CHANNELS = 6;
	public static final float MAX_ANALOG_INPUT_VALUE = (float) Math.pow(2, 10)-1;
	public static final int MAX_MOTOR_VALUE = 127;
	public static final int MIN_MOTOR_VALUE = -128;
	public static final int SERVO_CENTRE = 90;

	private I2CDevice device;
	private BoardPinInfo boardPinInfo;
	private OutputConfig[] outputConfigs = new OutputConfig[NUM_OUTPUT_CHANNELS];
	private InputConfig[] inputConfigs = new InputConfig[NUM_INPUT_CHANNELS];
	private int[] motorValues = new int[NUM_MOTORS];
	private int boardPwmFrequency = PICON_ZERO_PWM_FREQUENCY;
	
	public PiconZero() {
		this(I2CConstants.BUS_1, DEFAULT_ADDRESS);
		
		boardPinInfo = new PiconZeroBoardPinInfo();
	}
	
	public PiconZero(int controller, int address) {
		super(DEVICE_NAME + "-" + controller + "-" + address);
		
		device = new I2CDevice(controller, address, I2CConstants.ADDR_SIZE_7,
				I2CConstants.DEFAULT_CLOCK_FREQUENCY, ByteOrder.LITTLE_ENDIAN);
		
		reset();
	}
	
	@Override
	public float getVRef() {
		return VREF;
	}
	
	private static void validateMotor(int motor) {
		if (motor < 0 || motor >= NUM_MOTORS) {
			throw new IllegalArgumentException("Invalid motor number (" + motor + ") must be 0.." + (NUM_MOTORS-1));
		}
	}
	
	private static void validateInputChannel(int channel) {
		if (channel < 0 || channel >= NUM_INPUT_CHANNELS) {
			throw new IllegalArgumentException("Invalid channel value (" + channel + "), must be 0.." + (NUM_INPUT_CHANNELS-1));
		}
	}
	
	private static void validateOutputChannel(int channel) {
		if (channel < 0 || channel >= NUM_OUTPUT_CHANNELS) {
			throw new IllegalArgumentException("Invalid channel value (" + channel + "), must be 0.." + (NUM_OUTPUT_CHANNELS-1));
		}
	}
	
	private static void validateChannelMode(int channel, InputConfig config) {
		validateInputChannel(channel);
	}
	
	private static void validateChannelMode(int channel, OutputConfig config) {
		validateOutputChannel(channel);
		if (channel != 5 && config == OutputConfig.WS2812B) {
			throw new IllegalArgumentException("Invalid channel for WS2812B NeoPixels - only supported on channel 5");
		}
	}
	
	private void writeByte(int register, int value) throws RuntimeIOException {
		for (int i=0; i<MAX_I2C_RETRIES; i++) {
			try {
				device.writeByte(register, value);
				SleepUtil.sleepMillis(1);
				return;
			} catch (RuntimeIOException e) {
				Logger.warn(e, "Retrying I2C call, attempt # {}, error: {}", Integer.valueOf(i+1), e);
			}
		}
	}
	
	private void writeBytes(int register, int length, byte[] data) throws RuntimeIOException {
		device.writeBytes(register, length, data);
		SleepUtil.sleepMillis(1);
	}
	
	/**
	 * Reset the board.
	 */
	public void reset() {
		writeByte(RESET_REG, 0);
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
	 * Set configuration of selected input channel
	 * @param channel Input channel (0..3)
	 * @param config Input configuration (0: Digital, 1: Analog, 2: DS18B20)
	 */
	public void setInputConfig(int channel, InputConfig config) {
		validateChannelMode(channel, config);
		
		writeByte(INPUT0_CONFIG_REG + channel, config.getValue());
		inputConfigs[channel] = config; 
	}
	
	/**
	 * Set configuration of selected output
	 * @param channel Output channel (0..5)
	 * @param config Output configuration (0: Digital, 1: PWM, 2: Servo, 3: Neopixel WS2812B)
	 */
	public void setOutputConfig(int channel, OutputConfig config) {
		validateChannelMode(channel, config);
		
		writeByte(OUTPUT0_CONFIG_REG + channel, config.getValue());
		outputConfigs[channel] = config; 
	}
	
	/**
	 * Set motor output value (normalised to range -1..1)
	 * @param motor Motor number (0 or 1)
	 * @param speed Must be in range -1..1
	 */
	public void setMotor(int motor, float speed) {
		setMotorValue(motor, RangeUtil.map(speed, -1f, 1f, MIN_MOTOR_VALUE, MAX_MOTOR_VALUE));
	}
	
	/**
	 * Get motor output value (normalised to range -1..1)
	 * @param motor Motor number (0 or 1)
	 * @return Current motor speed in range -1..1
	 */
	public float getMotor(int motor) {
		return RangeUtil.map(getMotorValue(motor), MIN_MOTOR_VALUE, MAX_MOTOR_VALUE, -1f, 1f);
	}
	
	/**
	 * Set motor output value (PiconZero range -128..127)
	 * @param motor Motor number (0 or 1)
	 * @param speed Must be in range -128..127
	 */
	public void setMotorValue(int motor, int speed) {
		validateMotor(motor);
		writeByte(MOTOR0_REG + motor, speed);
		motorValues[motor] = speed;
	}
	
	/**
	 * Get the current motor speed (PiconZero range -128..127)
	 * @param motor Motor number (0 or 1)
	 * @return Motor speed in range -128..127
	 */
	public int getMotorValue(int motor) {
		validateMotor(motor);
		return motorValues[motor];
	}
	
	/**
	 * Read input value in normalised range (0..1)
	 * @param channel Input to read
	 * @return Normalised value (0..1)
	 */
	public float getValue(int channel) {
		int input = getInputValue(channel);
		
		switch (inputConfigs[channel]) {
		case ANALOG:
			return RangeUtil.map(input, 0, MAX_ANALOG_INPUT_VALUE, 0f, 1f);
		case DIGITAL:
		case DIGITAL_PULL_UP:
		case DS18B20:
		default:
			return input;
		}
	}
	
	/**
	 * <p>Set output value for the specified channel (normalised).</p> 
	 * <p><em>* Don't use this method if the output mode is WS2812B.</em></p>
	 * @param channel 0..5
	 * @param value Normalised output value:
	 * <pre>
	 * Mode  Name    Type    Values
	 * 0     On/Off  Byte    0 is OFF, 1 is ON
	 * 1     PWM     Byte    0 to 1 percentage of ON time
	 * 2     Servo   Byte    -1 to + 1 Position in degrees (0 is centre)
	 * 3*    WS2812B 4 Bytes 0:Pixel ID, 1:Red, 2:Green, 3:Blue
	 * </pre>
	 */
	public void setValue(int channel, float value) {
		int pz_value;
		if (outputConfigs[channel] == OutputConfig.SERVO) {
			pz_value = RangeUtil.map(value, -1, 1, 0, 180);
		} else {
			pz_value = RangeUtil.map(value, 0, 1, 0, MAX_OUTPUT_VALUE);
		}
		setOutputValue(channel, pz_value);
	}

	public void setValue(int channel, boolean value) {
		setOutputValue(channel, value ? 1 : 0);
	}
	
	/**
	 * Read input value in PiconZero range
	 * @param channel Input to read
	 * @return Value in PiconZero range
	 */
	public int getInputValue(int channel) {
		validateInputChannel(channel);
		
		return device.readUShort(INPUT0_REG + channel);
	}

	/**
	 * Set output data for selected output channel in PiconZero range. 
	 * @param channel 0..5
	 * @param value output value:
	 * <pre>
	 * Mode  Name    Type    Values
	 * 0     On/Off  Byte    0 is OFF, 1 is ON
	 * 1     PWM     Byte    0 to 100 percentage of ON time
	 * 2     Servo   Byte    Position in degrees with 90 as the mid point
	 * 3*    WS2812B 4 Bytes 0:Pixel ID, 1:Red, 2:Green, 3:Blue
	 * </pre>
	 * * Don't use this method if the output mode is WS2812B.
	 */
	public void setOutputValue(int channel, int value) {
		validateOutputChannel(channel);
		// Should really validate value based on the output config however we
		// don't actually need to - the PiconZero handles it for us
		
		writeByte(OUTPUT0_REG + channel, value);
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
		writeBytes(update ? WS2812B_SET_PIXEL_UPDATE_REG : WS2812B_SET_PIXEL_NOUPDATE_REG, 4, pixel_data);
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
		writeByte(UPDATE_NOW_REG, 0);
	}
	
	public void setBrightness(int brightness) {
		writeByte(SET_BRIGHTNESS_REG, brightness);
	}

	@Override
	public String getName() {
		return DEVICE_NAME + "-" + device.getController() + "-" + device.getAddress();
	}

	@Override
	public int getBoardPwmFrequency() {
		return boardPwmFrequency ;
	}

	@Override
	public void setBoardPwmFrequency(int pwmFrequency) {
		// Not supported
		Logger.warn("Cannot change PWM frequency for the Piconzero");
	}

	@Override
	public BoardPinInfo getBoardPinInfo() {
		return boardPinInfo;
	}
	
	@Override
	public GpioDigitalInputDeviceInterface createDigitalInputDevice(String key, PinInfo pinInfo, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws RuntimeIOException {
		setInputConfig(pinInfo.getPinNumber(), pud == GpioPullUpDown.PULL_UP ? InputConfig.DIGITAL_PULL_UP : InputConfig.DIGITAL);
		return new PiconZeroDigitalInputDevice(this, key, pinInfo, pud, trigger);
	}
	
	@Override
	public GpioDigitalOutputDeviceInterface createDigitalOutputDevice(String key, PinInfo pinInfo,
			boolean initialValue) throws RuntimeIOException {
		setOutputConfig(pinInfo.getPinNumber(), OutputConfig.DIGITAL);
		return new PiconZeroDigitalOutputDevice(this, key, pinInfo.getDeviceNumber(), pinInfo.getPinNumber(), initialValue);
	}
	
	@Override
	public GpioDigitalInputOutputDeviceInterface createDigitalInputOutputDevice(String key, PinInfo pinInfo,
			DeviceMode mode) throws RuntimeIOException {
		throw new UnsupportedOperationException("DigitalInputOutputDevice isn't possible with the PiconZero");
	}
	
	@Override
	public PwmOutputDeviceInterface createPwmOutputDevice(String key, PinInfo pinInfo,
			int pwmFrequency, float initialValue) throws RuntimeIOException {
		setOutputConfig(pinInfo.getPinNumber(), OutputConfig.PWM);
		return new PiconZeroPwmOutputDevice(this, key, pinInfo.getDeviceNumber(), pinInfo.getPinNumber(), initialValue);
	}
	
	@Override
	public AnalogInputDeviceInterface createAnalogInputDevice(String key, PinInfo pinInfo) {
		setInputConfig(pinInfo.getPinNumber(), InputConfig.ANALOG);
		
		return new PiconZeroAnalogInputDevice(this, key, pinInfo);
	}

	@Override
	public AnalogOutputDeviceInterface createAnalogOutputDevice(String key, PinInfo pinInfo) throws RuntimeIOException {
		return new PiconZeroAnalogOutputDevice(this, key, pinInfo.getDeviceNumber(), pinInfo.getPinNumber());
	}

	@Override
	public AnalogInputDeviceInterface provisionAnalogInputDevice(int gpio) throws RuntimeIOException {
		// Special case - PiconZero can switch between digital and analog input hence use of gpio rather than adc
		PinInfo pin_info = boardPinInfo.getByGpioNumber(gpio);
		if (pin_info == null || ! pin_info.isSupported(DeviceMode.ANALOG_INPUT)) {
			throw new IllegalArgumentException("Invalid mode (analog input) for GPIO " + gpio);
		}
		
		String key = createPinKey(pin_info);
		
		// Check if this pin is already provisioned
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}
		
		AnalogInputDeviceInterface device = createAnalogInputDevice(key, pin_info);
		deviceOpened(device);
		
		return device;
	}
	
	public void closeChannel(int channel) {
		Logger.debug("closeChannel({})", Integer.valueOf(channel));
		setInputConfig(channel, InputConfig.DIGITAL);
	}

	@Override
	public void close() throws RuntimeIOException {
		Logger.debug("close({})");
		setMotor(0, 0);
		setMotor(1, 0);
		reset();
		device.close();
	}
	
	public static class PiconZeroBoardPinInfo extends BoardPinInfo {
		public static final String OUTPUTS_HEADER = "OUTPUTS";
		public static final String INPUTS_HEADER = "INPUTS";
		public static final String MOTORS_HEADER = "MOTORS";
		
		public PiconZeroBoardPinInfo() {
			// GPIO0-5 - Output
			// Note doesn't include built-in servo and WS2812B capabilities
			for (int i=0; i<NUM_OUTPUT_CHANNELS; i++) {
				addGpioPinInfo(OUTPUTS_HEADER, i, i, PinInfo.DIGITAL_PWM_OUTPUT);
			}
			// GPIO6-9 - Input
			// Note doesn't include built-in DS18B20 capability
			for (int i=0; i<NUM_INPUT_CHANNELS; i++) {
				addGpioPinInfo(INPUTS_HEADER, NUM_OUTPUT_CHANNELS+i, i, PinInfo.DIGITAL_ANALOG_INPUT);
			}
			// GPIO10-11 - Motors
			for (int i=0; i<NUM_MOTORS; i++) {
				addDacPinInfo(MOTORS_HEADER, i, i);
			}
		}
	}
	
	public static class PiconZeroAnalogInputDevice extends AbstractInputDevice<AnalogInputEvent>
			implements AnalogInputDeviceInterface {
		private PiconZero piconZero;
		private PinInfo pinInfo;
	
		public PiconZeroAnalogInputDevice(PiconZero piconZero, String key, PinInfo pinInfo) {
			super(key, piconZero);
			
			this.piconZero = piconZero;
			this.pinInfo = pinInfo;
		}
	
		@Override
		protected void closeDevice() {
			Logger.debug("closeDevice()");
			piconZero.closeChannel(getChannel());
		}
	
		@Override
		public float getValue() throws RuntimeIOException {
			return piconZero.getValue(getChannel());
		}
	
		@Override
		public int getAdcNumber() {
			return pinInfo.getDeviceNumber();
		}
		
		public int getChannel() {
			return pinInfo.getPinNumber();
		}
	}
	
	public static class PiconZeroDigitalInputDevice extends AbstractDevice implements GpioDigitalInputDeviceInterface {
		private PiconZero piconZero;
		private PinInfo pinInfo;
	
		public PiconZeroDigitalInputDevice(PiconZero piconZero, String key,
				PinInfo pinInfo, GpioPullUpDown pud, GpioEventTrigger trigger) {
			super(key, piconZero);
			
			this.piconZero = piconZero;
			this.pinInfo = pinInfo;
		}
		
		@Override
		protected void closeDevice() {
			piconZero.closeChannel(getChannel());
		}
		
		@Override
		public int getGpio() {
			return pinInfo.getDeviceNumber();
		}
		
		public int getChannel() {
			return pinInfo.getPinNumber();
		}
	
		@Override
		public boolean getValue() throws RuntimeIOException {
			return piconZero.getInputValue(getChannel()) != 0;
		}
	
		@Override
		public void setDebounceTimeMillis(int debounceTime) {
			throw new UnsupportedOperationException("Not supported");
		}
	
		@Override
		public void setListener(InputEventListener<DigitalInputEvent> listener) {
			// TODO Need to implement a polling mechanism
			throw new UnsupportedOperationException("Not yet implemented");
		}
	
		@Override
		public void removeListener() {
			// TODO Need to implement a polling mechanism
			throw new UnsupportedOperationException("Not yet implemented");
		}
	}

	public static class PiconZeroDigitalOutputDevice extends AbstractDevice implements GpioDigitalOutputDeviceInterface {
		private PiconZero piconZero;
		private int gpio;
		private int channel;
		private boolean value;
	
		public PiconZeroDigitalOutputDevice(PiconZero piconZero, String key, int gpio, int channel, boolean initialValue) {
			super(key, piconZero);
			
			this.piconZero = piconZero;
			this.gpio = gpio;
			this.channel = channel;
		}
	
		@Override
		protected void closeDevice() {
			piconZero.closeChannel(channel);
		}
		
		@Override
		public int getGpio() {
			return gpio;
		}

		public int getChannel() {
			return channel;
		}
		
		@Override
		public boolean getValue() {
			return value;
		}
		
		@Override
		public void setValue(boolean value) {
			piconZero.setValue(channel, value);
			this.value = value;
		}
	}

	public static class PiconZeroPwmOutputDevice extends AbstractDevice implements PwmOutputDeviceInterface {
		private PiconZero piconZero;
		private int gpio;
		private int channel;
		private float value;
	
		public PiconZeroPwmOutputDevice(PiconZero piconZero, String key, int gpio, int channel, float initialValue) {
			super(key, piconZero);
			
			this.piconZero = piconZero;
			this.gpio = gpio;
			this.channel = channel;
		}
	
		@Override
		public int getGpio() {
			return gpio;
		}
		
		public int getChannel() {
			return channel;
		}
	
		@Override
		public int getPwmNum() {
			return channel;
		}
	
		@Override
		public float getValue() throws RuntimeIOException {
			return value;
		}
	
		@Override
		public void setValue(float value) throws RuntimeIOException {
			piconZero.setValue(channel, value);
			this.value = value;
		}
	
		@Override
		protected void closeDevice() throws RuntimeIOException {
			Logger.debug("closeDevice()");
			piconZero.closeChannel(channel);
		}
	}
	
	public static class PiconZeroAnalogOutputDevice extends AbstractDevice implements AnalogOutputDeviceInterface {
		private PiconZero piconZero;
		private int adcNumber;
		private int channel;
		private float value;
		
		public PiconZeroAnalogOutputDevice(PiconZero piconZero, String key, int adcNumber, int channel) {
			super(key, piconZero);
			
			this.piconZero = piconZero;
			this.adcNumber = adcNumber;
			this.channel = channel;
		}

		@Override
		public float getValue() throws RuntimeIOException {
			return value;
		}

		@Override
		public void setValue(float value) throws RuntimeIOException {
			piconZero.setMotor(channel, value);
			this.value = value;
		}

		@Override
		public int getAdcNumber() {
			return adcNumber;
		}

		@Override
		protected void closeDevice() throws RuntimeIOException {
			Logger.debug("closeDevice()");
			setValue(0);
		}
	}
}
