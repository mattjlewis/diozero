package com.diozero.devices;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     PiconZero.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2021 diozero
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
import java.util.EnumSet;

import org.tinylog.Logger;

import com.diozero.api.AnalogInputEvent;
import com.diozero.api.DeviceAlreadyOpenedException;
import com.diozero.api.DeviceInterface;
import com.diozero.api.DeviceMode;
import com.diozero.api.DigitalInputEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.I2CConstants;
import com.diozero.api.I2CDevice;
import com.diozero.api.PinInfo;
import com.diozero.api.RuntimeIOException;
import com.diozero.api.function.DeviceEventConsumer;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.AbstractDeviceFactory;
import com.diozero.internal.spi.AbstractInputDevice;
import com.diozero.internal.spi.AnalogInputDeviceFactoryInterface;
import com.diozero.internal.spi.AnalogInputDeviceInterface;
import com.diozero.internal.spi.AnalogOutputDeviceFactoryInterface;
import com.diozero.internal.spi.AnalogOutputDeviceInterface;
import com.diozero.internal.spi.GpioDeviceFactoryInterface;
import com.diozero.internal.spi.GpioDigitalInputDeviceInterface;
import com.diozero.internal.spi.GpioDigitalInputOutputDeviceInterface;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;
import com.diozero.internal.spi.InternalPwmOutputDeviceInterface;
import com.diozero.internal.spi.InternalServoDeviceInterface;
import com.diozero.internal.spi.PwmOutputDeviceFactoryInterface;
import com.diozero.internal.spi.ServoDeviceFactoryInterface;
import com.diozero.sbc.BoardPinInfo;
import com.diozero.util.RangeUtil;
import com.diozero.util.SleepUtil;

public class PiconZero extends AbstractDeviceFactory
		implements GpioDeviceFactoryInterface, PwmOutputDeviceFactoryInterface, ServoDeviceFactoryInterface,
		AnalogInputDeviceFactoryInterface, AnalogOutputDeviceFactoryInterface, DeviceInterface {
	public enum InputConfig {
		DIGITAL(0), DIGITAL_PULL_UP(128), ANALOG(1), DS18B20(2);

		private int value;

		InputConfig(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public enum OutputConfig {
		DIGITAL(0), PWM(1), SERVO(2), WS2812B(3);

		private int value;

		OutputConfig(int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	private static final int PICON_ZERO_PWM_FREQUENCY = 50;

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
	private static final float VREF = 5;

	public static final int ALL_PIXELS = 100;
	public static final int MAX_OUTPUT_VALUE = 100;
	public static final int NUM_MOTORS = 2;
	public static final int NUM_INPUT_CHANNELS = 4;
	public static final int NUM_OUTPUT_CHANNELS = 6;
	public static final float MAX_ANALOG_INPUT_VALUE = (float) Math.pow(2, 10) - 1;
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
		this(I2CConstants.CONTROLLER_1, DEFAULT_ADDRESS);

		boardPinInfo = new PiconZeroBoardPinInfo();
	}

	public PiconZero(int controller, int address) {
		super(DEVICE_NAME + "-" + controller + "-" + address);

		device = I2CDevice.builder(address).setController(controller).setByteOrder(ByteOrder.LITTLE_ENDIAN).build();

		reset();
	}

	@Override
	public float getVRef() {
		return VREF;
	}

	@Override
	public String getName() {
		return DEVICE_NAME + "-" + device.getController() + "-" + device.getAddress();
	}

	private static void validateMotor(int motor) {
		if (motor < 0 || motor >= NUM_MOTORS) {
			throw new IllegalArgumentException("Invalid motor number (" + motor + ") must be 0.." + (NUM_MOTORS - 1));
		}
	}

	private static void validateInputChannel(int channel) {
		if (channel < 0 || channel >= NUM_INPUT_CHANNELS) {
			throw new IllegalArgumentException(
					"Invalid channel value (" + channel + "), must be 0.." + (NUM_INPUT_CHANNELS - 1));
		}
	}

	private static void validateOutputChannel(int channel) {
		if (channel < 0 || channel >= NUM_OUTPUT_CHANNELS) {
			throw new IllegalArgumentException(
					"Invalid channel value (" + channel + "), must be 0.." + (NUM_OUTPUT_CHANNELS - 1));
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
		device.writeByteData(register, value);
		SleepUtil.sleepMillis(1);
	}

	private void writeBytes(int register, byte[] data) throws RuntimeIOException {
		device.writeI2CBlockData(register, data);
		SleepUtil.sleepMillis(1);
	}

	/**
	 * Get the board revision details
	 *
	 * @return revision[0]: Board type (2 == PiconZero); revision[1]: Firmware
	 *         version
	 */
	public byte[] getRevision() {
		ByteBuffer buffer = device.readI2CBlockDataByteBuffer(REVISION_REG, 2);
		byte[] arr = new byte[buffer.remaining()];
		buffer.get(arr);
		return arr;
	}

	/**
	 * Set configuration of selected input channel
	 *
	 * @param channel Input channel (0..3)
	 * @param config  Input configuration (0: Digital, 1: Analog, 2: DS18B20)
	 */
	public void setInputConfig(int channel, InputConfig config) {
		validateChannelMode(channel, config);

		writeByte(INPUT0_CONFIG_REG + channel, config.getValue());
		inputConfigs[channel] = config;
	}

	/**
	 * Set configuration of selected output
	 *
	 * @param channel Output channel (0..5)
	 * @param config  Output configuration (0: Digital, 1: PWM, 2: Servo, 3:
	 *                Neopixel WS2812B)
	 */
	public void setOutputConfig(int channel, OutputConfig config) {
		Logger.debug("setOutputConfig({}, {})", Integer.valueOf(channel), config);
		validateChannelMode(channel, config);

		writeByte(OUTPUT0_CONFIG_REG + channel, config.getValue());
		outputConfigs[channel] = config;
	}

	/**
	 * Get motor output value (normalised to range -1..1)
	 *
	 * @param motor Motor number (0 or 1)
	 * @return Current motor speed in range -1..1
	 */
	public float getMotor(int motor) {
		return RangeUtil.map(getMotorValue(motor), MIN_MOTOR_VALUE, MAX_MOTOR_VALUE, -1f, 1f);
	}

	/**
	 * Set motor output value (normalised to range -1..1)
	 *
	 * @param motor Motor number (0 or 1)
	 * @param speed Must be in range -1..1
	 */
	public void setMotor(int motor, float speed) {
		setMotorValue(motor, RangeUtil.map(speed, -1f, 1f, MIN_MOTOR_VALUE, MAX_MOTOR_VALUE));
	}

	/**
	 * Get the current motor speed (PiconZero range -128..127)
	 *
	 * @param motor Motor number (0 or 1)
	 * @return Motor speed in range -128..127
	 */
	public int getMotorValue(int motor) {
		validateMotor(motor);
		return motorValues[motor];
	}

	/**
	 * Set motor output value (PiconZero range -128..127)
	 *
	 * @param motor Motor number (0 or 1)
	 * @param speed Must be in range -128..127
	 */
	public void setMotorValue(int motor, int speed) {
		Logger.debug("setMotorValue({}, {})", Integer.valueOf(motor), Integer.valueOf(speed));
		validateMotor(motor);
		writeByte(MOTOR0_REG + motor, speed);
		motorValues[motor] = speed;
	}

	/**
	 * Read input value in PiconZero range
	 *
	 * @param channel Input to read
	 * @return Value in PiconZero range
	 */
	public int getInputValue(int channel) {
		validateInputChannel(channel);

		return device.readUShort(INPUT0_REG + channel);
	}

	/**
	 * Set output data for selected output channel in PiconZero range.
	 *
	 * @param channel 0..5
	 * @param value   output value:
	 *
	 *                <pre>
	 * Mode  Name    Type    Values
	 * 0     On/Off  Byte    0 is OFF, 1 is ON
	 * 1     PWM     Byte    0 to 100 percentage of ON time
	 * 2     Servo   Byte    0..180 position in degrees with 90 as the mid point
	 * 3*    WS2812B 4 Bytes 0:Pixel ID, 1:Red, 2:Green, 3:Blue
	 *                </pre>
	 *
	 *                * Don't use this method if the output mode is WS2812B.
	 */
	public void setOutputValue(int channel, int value) {
		Logger.debug("setOutputValue({}, {})", Integer.valueOf(channel), Integer.valueOf(value));
		validateOutputChannel(channel);
		// Should really validate value based on the output config however we
		// don't actually need to - the PiconZero handles it for us

		writeByte(OUTPUT0_REG + channel, value);
	}

	public void setOutputValue(int channel, boolean value) {
		setOutputValue(channel, value ? 1 : 0);
	}

	/**
	 * Set the colour of an individual pixel (always output channel 5)
	 *
	 * @param pixel  0..63
	 * @param red    0..255
	 * @param green  0..255
	 * @param blue   0..255
	 * @param update update the pixel now if true
	 */
	public void setPixel(int pixel, int red, int green, int blue, boolean update) {
		// pixelData = [Pixel, Red, Green, Blue]
		// bus.write_i2c_block_data (pzaddr, Update, pixelData)
		byte[] pixel_data = new byte[] { (byte) pixel, (byte) red, (byte) green, (byte) blue };
		writeBytes(update ? WS2812B_SET_PIXEL_UPDATE_REG : WS2812B_SET_PIXEL_NOUPDATE_REG, pixel_data);
	}

	/**
	 * Sets all pixels with the selected red, green and blue values (0 to 255)
	 * [Available from firmware revision 07]
	 *
	 * @param red    0..255
	 * @param green  0..255
	 * @param blue   0..255
	 * @param update update the pixel now if true
	 */
	public void setAllPixels(int red, int green, int blue, boolean update) {
		// pixelData = [100, Red, Green, Blue]
		// bus.write_i2c_block_data (pzaddr, Update, pixelData)
		setPixel(ALL_PIXELS, red, green, blue, update);
	}

	public void updatePixels() {
		writeByte(UPDATE_NOW_REG, 0);
	}

	public void setBrightness(int brightness) {
		writeByte(SET_BRIGHTNESS_REG, brightness);
	}

	@Override
	public int getBoardPwmFrequency() {
		return boardPwmFrequency;
	}

	@Override
	public void setBoardPwmFrequency(int pwmFrequency) {
		// Not supported
		Logger.warn("Cannot change PWM frequency for the Picon Zero");
	}

	@Override
	public int getBoardServoFrequency() {
		return boardPwmFrequency;
	}

	@Override
	public void setBoardServoFrequency(int boardFrequency) {
		// Not supported
		Logger.warn("Cannot change servo frequency for the Picon Zero");
	}

	@Override
	public BoardPinInfo getBoardPinInfo() {
		return boardPinInfo;
	}

	@Override
	public GpioDigitalInputDeviceInterface createDigitalInputDevice(String key, PinInfo pinInfo, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws RuntimeIOException {
		setInputConfig(pinInfo.getPhysicalPin(),
				pud == GpioPullUpDown.PULL_UP ? InputConfig.DIGITAL_PULL_UP : InputConfig.DIGITAL);
		return new PiconZeroDigitalInputDevice(this, key, pinInfo, pud, trigger);
	}

	@Override
	public GpioDigitalOutputDeviceInterface createDigitalOutputDevice(String key, PinInfo pinInfo, boolean initialValue)
			throws RuntimeIOException {
		setOutputConfig(pinInfo.getPhysicalPin(), OutputConfig.DIGITAL);
		return new PiconZeroDigitalOutputDevice(this, key, pinInfo.getDeviceNumber(), pinInfo.getPhysicalPin(),
				initialValue);
	}

	@Override
	public GpioDigitalInputOutputDeviceInterface createDigitalInputOutputDevice(String key, PinInfo pinInfo,
			DeviceMode mode) throws RuntimeIOException {
		throw new UnsupportedOperationException("DigitalInputOutputDevice isn't possible with the PiconZero");
	}

	@Override
	public InternalPwmOutputDeviceInterface createPwmOutputDevice(String key, PinInfo pinInfo, int pwmFrequency,
			float initialValue) throws RuntimeIOException {
		setOutputConfig(pinInfo.getPhysicalPin(), OutputConfig.PWM);
		return new PiconZeroPwmOutputDevice(this, key, pinInfo.getDeviceNumber(), pinInfo.getPhysicalPin(),
				initialValue);
	}

	@Override
	public InternalServoDeviceInterface createServoDevice(String key, PinInfo pinInfo, int frequency,
			int minPulseWidthUs, int maxPulseWidthUs, int initialPulseWidthUs) {
		// Note the PiconZero Servo output frequency is fixed at 50Hz
		setOutputConfig(pinInfo.getPhysicalPin(), OutputConfig.SERVO);
		return new PiconZeroServoDevice(this, key, pinInfo.getDeviceNumber(), pinInfo.getPhysicalPin(), minPulseWidthUs,
				maxPulseWidthUs, initialPulseWidthUs);
	}

	@Override
	public AnalogInputDeviceInterface createAnalogInputDevice(String key, PinInfo pinInfo) {
		setInputConfig(pinInfo.getPhysicalPin(), InputConfig.ANALOG);

		return new PiconZeroAnalogInputDevice(this, key, pinInfo);
	}

	@Override
	public AnalogOutputDeviceInterface createAnalogOutputDevice(String key, PinInfo pinInfo, float initialValue)
			throws RuntimeIOException {
		return new PiconZeroAnalogOutputDevice(this, key, pinInfo.getDeviceNumber(), pinInfo.getPhysicalPin(),
				initialValue);
	}

	@Override
	public AnalogInputDeviceInterface provisionAnalogInputDevice(PinInfo pinInfo) throws RuntimeIOException {
		// Special case - PiconZero can switch between digital and analog input hence
		// use of gpio rather than adc
		if (!pinInfo.isSupported(DeviceMode.ANALOG_INPUT)) {
			throw new IllegalArgumentException("Invalid mode (analog input) for pin " + pinInfo);
		}

		String key = createPinKey(pinInfo);

		// Check if this pin is already provisioned
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}

		AnalogInputDeviceInterface ain = createAnalogInputDevice(key, pinInfo);
		deviceOpened(ain);

		return ain;
	}

	/**
	 * Reset the board.
	 */
	public void reset() {
		Logger.debug("reset()");
		writeByte(RESET_REG, 0);
	}

	public void closeChannel(int channel) {
		Logger.trace("closeChannel({})", Integer.valueOf(channel));
		// setInputConfig(channel, InputConfig.DIGITAL);
	}

	@Override
	public void close() throws RuntimeIOException {
		Logger.trace("close()");
		if (isClosed()) {
			Logger.trace("Already closed");
			return;
		}

		// Close all provisioned pins before closing the I2C device itself
		super.close();

		setMotor(0, 0);
		setMotor(1, 0);
		reset();

		// Finally close the I2C device itself
		device.close();
	}

	public static class PiconZeroBoardPinInfo extends BoardPinInfo {
		public static final int MOTOR1 = 0;
		public static final int MOTOR2 = 1;

		public static final int MOTOR1_GPIO = 10;
		public static final int MOTOR2_GPIO = 11;

		private static final EnumSet<DeviceMode> DIGITAL_OUTPUT_PWM_SERVO = EnumSet.of(DeviceMode.DIGITAL_OUTPUT,
				DeviceMode.PWM_OUTPUT, DeviceMode.SERVO);

		public PiconZeroBoardPinInfo() {
			// GPIO0-5 - Output
			// Note doesn't include built-in WS2812B capabilities
			for (int i = 0; i < NUM_OUTPUT_CHANNELS; i++) {
				addGpioPinInfo(i, i, DIGITAL_OUTPUT_PWM_SERVO);
			}
			// GPIO6-9 - Input
			// Note doesn't include built-in DS18B20 capability
			for (int i = 0; i < NUM_INPUT_CHANNELS; i++) {
				addGpioPinInfo(NUM_OUTPUT_CHANNELS + i, i, PinInfo.DIGITAL_ANALOG_INPUT);
			}
			// GPIO10-11 - Motors
			for (int i = 0; i < NUM_MOTORS; i++) {
				addDacPinInfo(NUM_OUTPUT_CHANNELS + NUM_INPUT_CHANNELS + i, i);
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
			Logger.trace("closeDevice()");
			piconZero.closeChannel(getChannel());
		}

		@Override
		public float getValue() throws RuntimeIOException {
			return piconZero.getInputValue(getChannel());
		}

		@Override
		public int getAdcNumber() {
			return pinInfo.getDeviceNumber();
		}

		public int getChannel() {
			return pinInfo.getPhysicalPin();
		}
	}

	public static class PiconZeroDigitalInputDevice extends AbstractDevice implements GpioDigitalInputDeviceInterface {
		private PiconZero piconZero;
		private PinInfo pinInfo;

		public PiconZeroDigitalInputDevice(PiconZero piconZero, String key, PinInfo pinInfo, GpioPullUpDown pud,
				GpioEventTrigger trigger) {
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
			return pinInfo.getPhysicalPin();
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
		public void setListener(DeviceEventConsumer<DigitalInputEvent> listener) {
			// TODO Need to implement a polling mechanism
			throw new UnsupportedOperationException("Not yet implemented");
		}

		@Override
		public void removeListener() {
			// TODO Need to implement a polling mechanism
			throw new UnsupportedOperationException("Not yet implemented");
		}
	}

	public static class PiconZeroDigitalOutputDevice extends AbstractDevice
			implements GpioDigitalOutputDeviceInterface {
		private PiconZero piconZero;
		private int gpio;
		private int channel;
		private boolean value;

		public PiconZeroDigitalOutputDevice(PiconZero piconZero, String key, int gpio, int channel,
				boolean initialValue) {
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
			piconZero.setOutputValue(channel, value);
			this.value = value;
		}
	}

	public static class PiconZeroPwmOutputDevice extends AbstractDevice implements InternalPwmOutputDeviceInterface {
		private PiconZero piconZero;
		private int gpio;
		private int channel;
		private float value;

		public PiconZeroPwmOutputDevice(PiconZero piconZero, String key, int gpio, int channel, float initialValue) {
			super(key, piconZero);

			this.piconZero = piconZero;
			this.gpio = gpio;
			this.channel = channel;

			setValue(initialValue);
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
			// Convert to % "on" value in the range 0..100
			int on = Math.round(value * MAX_OUTPUT_VALUE);
			piconZero.setOutputValue(channel, on);
			this.value = value;
		}

		@Override
		protected void closeDevice() throws RuntimeIOException {
			Logger.trace("closeDevice()");
			piconZero.closeChannel(channel);
		}

		@Override
		public int getPwmFrequency() {
			return piconZero.getBoardPwmFrequency();
		}

		@Override
		public void setPwmFrequency(int frequency) throws RuntimeIOException {
			throw new UnsupportedOperationException("Cannot change the PWM frequency for the Picon Zero");
		}
	}

	public static class PiconZeroServoDevice extends AbstractDevice implements InternalServoDeviceInterface {
		private PiconZero piconZero;
		private int gpio;
		private int channel;
		private int minPulseWidthUs;
		private int maxPulseWidthUs;

		public PiconZeroServoDevice(PiconZero piconZero, String key, int gpio, int channel, int minPulseWidthUs,
				int maxPulseWidthUs, int initialPulseWidthUs) {
			super(key, piconZero);

			this.piconZero = piconZero;
			this.gpio = gpio;
			this.channel = channel;

			this.minPulseWidthUs = minPulseWidthUs;
			this.maxPulseWidthUs = maxPulseWidthUs;

			setPulseWidthUs(initialPulseWidthUs);
		}

		@Override
		public int getGpio() {
			return gpio;
		}

		public int getChannel() {
			return channel;
		}

		@Override
		public int getServoNum() {
			return channel;
		}

		@Override
		public int getPulseWidthUs() throws RuntimeIOException {
			return RangeUtil.map(piconZero.getInputValue(channel), 0, 180, minPulseWidthUs, maxPulseWidthUs, true);
		}

		@Override
		public void setPulseWidthUs(int pulseWidthUs) throws RuntimeIOException {
			// Convert the pulse width value to approximate angle in degrees
			piconZero.setOutputValue(channel,
					RangeUtil.map(pulseWidthUs, minPulseWidthUs, maxPulseWidthUs, 0, 180, true));
		}

		@Override
		protected void closeDevice() throws RuntimeIOException {
			Logger.trace("closeDevice()");
			piconZero.closeChannel(channel);
		}

		@Override
		public int getServoFrequency() {
			return piconZero.getBoardPwmFrequency();
		}

		@Override
		public void setServoFrequency(int frequencyHz) throws RuntimeIOException {
			throw new UnsupportedOperationException("Cannot change the servo frequency for the Picon Zero");
		}
	}

	public static class PiconZeroAnalogOutputDevice extends AbstractDevice implements AnalogOutputDeviceInterface {
		private PiconZero piconZero;
		private int adcNumber;
		private int channel;
		private float value;

		public PiconZeroAnalogOutputDevice(PiconZero piconZero, String key, int adcNumber, int channel,
				float initialValue) {
			super(key, piconZero);

			this.piconZero = piconZero;
			this.adcNumber = adcNumber;
			this.channel = channel;

			setValue(initialValue);
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
			Logger.trace("closeDevice()");
			setValue(0);
		}
	}
}
