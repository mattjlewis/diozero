package com.diozero.internal.provider.mock;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Mock provider
 * Filename:     MockDeviceFactory.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2023 diozero
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
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import com.diozero.api.DeviceMode;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.I2CConstants.AddressSize;
import com.diozero.api.I2CDeviceInterface;
import com.diozero.api.PinInfo;
import com.diozero.api.RuntimeIOException;
import com.diozero.api.SerialConstants.DataBits;
import com.diozero.api.SerialConstants.Parity;
import com.diozero.api.SerialConstants.StopBits;
import com.diozero.api.SpiClockMode;
import com.diozero.devices.PCA9685;
import com.diozero.internal.board.GenericLinuxArmBoardInfo;
import com.diozero.internal.provider.mock.devices.MockPca9685;
import com.diozero.internal.spi.AnalogInputDeviceInterface;
import com.diozero.internal.spi.AnalogOutputDeviceInterface;
import com.diozero.internal.spi.BaseNativeDeviceFactory;
import com.diozero.internal.spi.GpioDigitalInputDeviceInterface;
import com.diozero.internal.spi.GpioDigitalInputOutputDeviceInterface;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;
import com.diozero.internal.spi.InternalI2CDeviceInterface;
import com.diozero.internal.spi.InternalPwmOutputDeviceInterface;
import com.diozero.internal.spi.InternalSerialDeviceInterface;
import com.diozero.internal.spi.InternalServoDeviceInterface;
import com.diozero.internal.spi.InternalSpiDeviceInterface;
import com.diozero.sbc.BoardInfo;
import com.diozero.sbc.LocalSystemInfo;
import com.diozero.util.PropertyUtil;

public class MockDeviceFactory extends BaseNativeDeviceFactory {
	private static final int ALL_I2C_FUNCS = I2CDeviceInterface.I2C_FUNC_I2C | I2CDeviceInterface.I2C_FUNC_10BIT_ADDR
			| I2CDeviceInterface.I2C_FUNC_PROTOCOL_MANGLING | I2CDeviceInterface.I2C_FUNC_SMBUS_PEC
			| I2CDeviceInterface.I2C_FUNC_NOSTART | I2CDeviceInterface.I2C_FUNC_SMBUS_BLOCK_PROC_CALL
			| I2CDeviceInterface.I2C_FUNC_SMBUS_QUICK | I2CDeviceInterface.I2C_FUNC_SMBUS_READ_BYTE
			| I2CDeviceInterface.I2C_FUNC_SMBUS_WRITE_BYTE | I2CDeviceInterface.I2C_FUNC_SMBUS_READ_BYTE_DATA
			| I2CDeviceInterface.I2C_FUNC_SMBUS_WRITE_BYTE_DATA | I2CDeviceInterface.I2C_FUNC_SMBUS_READ_WORD_DATA
			| I2CDeviceInterface.I2C_FUNC_SMBUS_WRITE_WORD_DATA | I2CDeviceInterface.I2C_FUNC_SMBUS_PROC_CALL
			| I2CDeviceInterface.I2C_FUNC_SMBUS_READ_BLOCK_DATA | I2CDeviceInterface.I2C_FUNC_SMBUS_WRITE_BLOCK_DATA
			| I2CDeviceInterface.I2C_FUNC_SMBUS_READ_I2C_BLOCK | I2CDeviceInterface.I2C_FUNC_SMBUS_WRITE_I2C_BLOCK;

	private Optional<Class<? extends GpioDigitalInputDeviceInterface>> digitalInputDeviceClass;
	private Optional<Class<? extends GpioDigitalOutputDeviceInterface>> digitalOutputDeviceClass;
	private Optional<Class<? extends GpioDigitalInputOutputDeviceInterface>> digitalInputOutputDeviceClass;
	private Optional<Class<? extends InternalPwmOutputDeviceInterface>> pwmOutputDeviceClass;
	private Optional<Class<? extends InternalServoDeviceInterface>> servoDeviceClass;
	private Optional<Class<? extends AnalogInputDeviceInterface>> analogInputDeviceClass;
	private Optional<Class<? extends AnalogOutputDeviceInterface>> analogOutputDeviceClass;
	private Optional<Class<? extends InternalSpiDeviceInterface>> spiDeviceClass;
	private Optional<Class<? extends InternalI2CDeviceInterface>> i2cDeviceClass;
	private Optional<Class<? extends InternalSerialDeviceInterface>> serialDeviceClass;

	private BoardInfo boardInfo;
	private int boardPwmFrequency;
	private int boardServoFrequency;
	private List<Integer> i2cBusNumbers;
	private Map<Integer, MockGpio> gpios;

	public MockDeviceFactory() {
		Properties props = new Properties();
		try (InputStream is = MockDeviceFactory.class.getResourceAsStream("/mock-board-info.properties")) {
			props.load(is);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}

		boardInfo = new MockBoardInfo(props, LocalSystemInfo.getInstance());
		boardPwmFrequency = Integer.parseInt(props.getProperty("PwmFrequency"));
		boardServoFrequency = Integer.parseInt(props.getProperty("ServoFrequency"));
		i2cBusNumbers = Arrays.stream(props.getProperty("I2CBusNumbers").split(",")).map(Integer::valueOf)
				.collect(Collectors.toList());

		gpios = new HashMap<>();

		digitalInputDeviceClass = getClass(
				PropertyUtil.getProperty("mock_digital_input_device",
						"com.diozero.internal.provider.mock.MockDigitalInputDevice"),
				GpioDigitalInputDeviceInterface.class);
		digitalOutputDeviceClass = getClass(
				PropertyUtil.getProperty("mock_digital_output_device",
						"com.diozero.internal.provider.mock.MockDigitalOutputDevice"),
				GpioDigitalOutputDeviceInterface.class);
		digitalInputOutputDeviceClass = getClass(
				PropertyUtil.getProperty("mock_digital_input_output_device",
						"com.diozero.internal.provider.mock.MockDigitalInputOutputDevice"),
				GpioDigitalInputOutputDeviceInterface.class);
		pwmOutputDeviceClass = getClass(
				PropertyUtil.getProperty("mock_pwm_output_device",
						"com.diozero.internal.provider.mock.MockPwmOutputDevice"),
				InternalPwmOutputDeviceInterface.class);
		servoDeviceClass = getClass(
				PropertyUtil.getProperty("mock_servo_device", "com.diozero.internal.provider.mock.MockServoDevice"),
				InternalServoDeviceInterface.class);
		analogInputDeviceClass = getClass(PropertyUtil.getProperty("mock_analog_input_device",
				"com.diozero.internal.provider.mock.MockAnalogInputDevice"), AnalogInputDeviceInterface.class);
		analogOutputDeviceClass = getClass(
				PropertyUtil.getProperty("mock_analog_output_device",
						"com.diozero.internal.provider.mock.MockAnalogOutputDevice"),
				AnalogOutputDeviceInterface.class);
		spiDeviceClass = getClass(
				PropertyUtil.getProperty("mock_spi_device", "com.diozero.internal.provider.mock.MockSpiDevice"),
				InternalSpiDeviceInterface.class);
		i2cDeviceClass = getClass(
				PropertyUtil.getProperty("mock_i2c_device", "com.diozero.internal.provider.mock.MockI2CDevice"),
				InternalI2CDeviceInterface.class);
		serialDeviceClass = getClass(
				PropertyUtil.getProperty("mock_serial_device", "com.diozero.internal.provider.mock.MockSerialDevice"),
				InternalSerialDeviceInterface.class);
	}

	@SuppressWarnings("unchecked")
	private static <T> Optional<Class<? extends T>> getClass(String name, Class<T> clz) {
		if (name == null) {
			return Optional.empty();
		}
		try {
			return Optional.of((Class<? extends T>) Class.forName(name));
		} catch (Exception e) {
			return Optional.empty();
		}
	}

	@Override
	public void shutdown() {
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	protected BoardInfo lookupBoardInfo() {
		return boardInfo;
	}

	@Override
	public int getBoardPwmFrequency() {
		return boardPwmFrequency;
	}

	@Override
	public void setBoardPwmFrequency(int pwmFrequency) {
		this.boardPwmFrequency = pwmFrequency;
	}

	@Override
	public int getBoardServoFrequency() {
		return boardServoFrequency;
	}

	@Override
	public void setBoardServoFrequency(int servoFrequency) {
		this.boardServoFrequency = servoFrequency;
	}

	@Override
	public List<Integer> getI2CBusNumbers() {
		return i2cBusNumbers;
	}

	@Override
	public int getI2CFunctionalities(int controller) {
		return ALL_I2C_FUNCS;
	}

	@Override
	public DeviceMode getGpioMode(int gpio) {
		if (gpios.containsKey(Integer.valueOf(gpio))) {
			return gpios.get(Integer.valueOf(gpio)).getMode();
		}
		return DeviceMode.UNKNOWN;
	}

	@Override
	public int getGpioValue(int gpio) {
		if (gpios.containsKey(Integer.valueOf(gpio))) {
			return gpios.get(Integer.valueOf(gpio)).getValue();
		}
		return 0;
	}

	@Override
	public GpioDigitalInputDeviceInterface createDigitalInputDevice(String key, PinInfo pinInfo, GpioPullUpDown pud,
			GpioEventTrigger trigger) {
		try {
			return digitalInputDeviceClass.orElseThrow(
					() -> new UnsupportedOperationException("Digital input implementation class hasn't been set"))
					.getConstructor(String.class, MockDeviceFactory.class, int.class, GpioPullUpDown.class,
							GpioEventTrigger.class)
					.newInstance(key, this, Integer.valueOf(pinInfo.getDeviceNumber()), pud, trigger);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public GpioDigitalOutputDeviceInterface createDigitalOutputDevice(String key, PinInfo pinInfo,
			boolean initialValue) {
		try {
			return digitalOutputDeviceClass.orElseThrow(
					() -> new UnsupportedOperationException("Digital output implementation class hasn't been set"))
					.getConstructor(String.class, MockDeviceFactory.class, int.class, boolean.class)
					.newInstance(key, this, Integer.valueOf(pinInfo.getDeviceNumber()), Boolean.valueOf(initialValue));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public GpioDigitalInputOutputDeviceInterface createDigitalInputOutputDevice(String key, PinInfo pinInfo,
			DeviceMode mode) {
		try {
			return digitalInputOutputDeviceClass
					.orElseThrow(() -> new UnsupportedOperationException(
							"Digital input/output implementation class hasn't been set"))
					.getConstructor(String.class, MockDeviceFactory.class, int.class, DeviceMode.class)
					.newInstance(key, this, Integer.valueOf(pinInfo.getDeviceNumber()), mode);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public InternalPwmOutputDeviceInterface createPwmOutputDevice(String key, PinInfo pinInfo, int pwmFrequency,
			float initialValue) {
		try {
			return pwmOutputDeviceClass
					.orElseThrow(
							() -> new UnsupportedOperationException("PWM output implementation class hasn't been set"))
					.getConstructor(String.class, MockDeviceFactory.class, int.class, int.class, int.class, float.class)
					.newInstance(key, this, Integer.valueOf(pinInfo.getDeviceNumber()),
							Integer.valueOf(pinInfo.getPwmNum()), Integer.valueOf(pwmFrequency),
							Float.valueOf(initialValue));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public InternalServoDeviceInterface createServoDevice(String key, PinInfo pinInfo, int frequencyHz,
			int minPulseWidthUs, int maxPulseWidthUs, int initialPulseWidthUs) {
		try {
			return servoDeviceClass
					.orElseThrow(() -> new UnsupportedOperationException("Servo implementation class hasn't been set"))
					.getConstructor(String.class, MockDeviceFactory.class, int.class, int.class, int.class, int.class,
							int.class, int.class)
					.newInstance(key, this, Integer.valueOf(pinInfo.getDeviceNumber()),
							Integer.valueOf(pinInfo.getPwmNum()), Integer.valueOf(frequencyHz),
							Integer.valueOf(minPulseWidthUs), Integer.valueOf(maxPulseWidthUs),
							Integer.valueOf(initialPulseWidthUs));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public AnalogInputDeviceInterface createAnalogInputDevice(String key, PinInfo pinInfo) {
		try {
			return analogInputDeviceClass.orElseThrow(
					() -> new UnsupportedOperationException("Analog input implementation class hasn't been set"))
					.getConstructor(String.class, MockDeviceFactory.class, int.class)
					.newInstance(key, this, Integer.valueOf(pinInfo.getDeviceNumber()));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public AnalogOutputDeviceInterface createAnalogOutputDevice(String key, PinInfo pinInfo, float initialValue) {
		try {
			return analogOutputDeviceClass.orElseThrow(
					() -> new UnsupportedOperationException("Analog output implementation class hasn't been set"))
					.getConstructor(String.class, MockDeviceFactory.class, int.class, float.class)
					.newInstance(key, this, Integer.valueOf(pinInfo.getDeviceNumber()), Float.valueOf(initialValue));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public InternalSpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect, int frequency,
			SpiClockMode spiClockMode, boolean lsbFirst) throws RuntimeIOException {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public InternalI2CDeviceInterface createI2CDevice(String key, int controller, int address, AddressSize addressSize)
			throws RuntimeIOException {
		switch (address) {
		case PCA9685.DEFAULT_ADDRESS:
			return new MockPca9685(key);
		default:
			throw new UnsupportedOperationException("Not yet implemented");
		}
	}

	@Override
	public InternalSerialDeviceInterface createSerialDevice(String key, String deviceFilename, int baud,
			DataBits dataBits, StopBits stopBits, Parity parity, boolean readBlocking, int minReadChars,
			int readTimeoutMillis) throws RuntimeIOException {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	private static class MockBoardInfo extends GenericLinuxArmBoardInfo {
		public MockBoardInfo(Properties props, LocalSystemInfo localSysInfo) {
			super(props.getProperty("Make"), props.getProperty("Model"), Integer.parseInt(props.getProperty("Memory")),
					Float.parseFloat(props.getProperty("ADCvRef")), "lib-mock");
		}

		@Override
		public List<String> getBoardCompatibility() {
			return Arrays.asList("mockboard");
		}
	}

	MockGpio provisionGpio(int gpio, DeviceMode mode, int value) {
		MockGpio mock_gpio = new MockGpio(gpio, mode, value);
		gpios.put(Integer.valueOf(gpio), mock_gpio);

		return mock_gpio;
	}

	void deprovisionGpio(MockGpio mockGpio) {
		gpios.remove(Integer.valueOf(mockGpio.getGpio()));
		mockGpio.setMode(DeviceMode.UNKNOWN);
	}
}
