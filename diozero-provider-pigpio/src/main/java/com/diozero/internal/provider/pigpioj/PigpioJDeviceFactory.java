package com.diozero.internal.provider.pigpioj;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - pigpioj provider
 * Filename:     PigpioJDeviceFactory.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2022 diozero
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

import java.util.List;

import org.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.I2CConstants;
import com.diozero.api.PinInfo;
import com.diozero.api.RuntimeIOException;
import com.diozero.api.SerialDevice;
import com.diozero.api.SpiClockMode;
import com.diozero.internal.board.raspberrypi.RaspberryPiBoardInfoProvider;
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

import uk.pigpioj.PigpioConstants;
import uk.pigpioj.PigpioInterface;
import uk.pigpioj.PigpioJ;
import uk.pigpioj.PigpioSocket;

public class PigpioJDeviceFactory extends BaseNativeDeviceFactory {
	private static final int PIGPIO_SPI_BUFFER_SIZE = (int) (Math.pow(2, 16) - 1);
	private int boardPwmFrequency;
	private int boardServoFrequency = 50;
	private PigpioInterface pigpioImpl;

	@SuppressWarnings("resource")
	public static PigpioJDeviceFactory newSocketInstance(String hostname) {
		return new PigpioJDeviceFactory(PigpioJ.newSocketImplementation(hostname));
	}

	@SuppressWarnings("resource")
	public static PigpioJDeviceFactory newSocketInstance(String hostname, int port) {
		return new PigpioJDeviceFactory(PigpioJ.newSocketImplementation(hostname, port));
	}

	@SuppressWarnings("resource")
	public PigpioJDeviceFactory() {
		this(PigpioJ.autoDetectedImplementation());
	}

	public PigpioJDeviceFactory(PigpioInterface pigpioImpl) {
		this.pigpioImpl = pigpioImpl;
	}

	public PigpioInterface getPigpio() {
		return pigpioImpl;
	}

	@Override
	public void shutdown() {
		pigpioImpl.close();
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	protected BoardInfo lookupBoardInfo() {
		int hw_rev = pigpioImpl.getHardwareRevision();
		String hw_rev_hex = String.format("%04x", Integer.valueOf(hw_rev));
		Logger.debug("Hardware revision: {} (0x{})", Integer.valueOf(hw_rev), hw_rev_hex);
		BoardInfo board_info = RaspberryPiBoardInfoProvider.lookupByRevision(hw_rev_hex);
		if (board_info == null) {
			Logger.error("Failed to load RPi board info for {} (0x{})");
			throw new RuntimeException(
					"Error initialising board info for hardware revision " + hw_rev + " (0x" + hw_rev_hex + ")");
		}

		return board_info;
	}

	@Override
	public int getBoardPwmFrequency() {
		return boardPwmFrequency;
	}

	@Override
	public void setBoardPwmFrequency(int pwmFrequency) {
		boardPwmFrequency = pwmFrequency;
	}

	@Override
	public int getBoardServoFrequency() {
		return boardServoFrequency;
	}

	@Override
	public void setBoardServoFrequency(int servoFrequency) {
		boardServoFrequency = servoFrequency;
	}

	@Override
	public int getSpiBufferSize() {
		return PIGPIO_SPI_BUFFER_SIZE;
	}

	@Override
	public DeviceMode getGpioMode(int gpio) {
		switch (pigpioImpl.getMode(gpio)) {
		case PigpioConstants.MODE_PI_INPUT:
			return DeviceMode.DIGITAL_INPUT;
		case PigpioConstants.MODE_PI_OUTPUT:
			return DeviceMode.DIGITAL_OUTPUT;
		case PigpioConstants.MODE_PI_ALT0:
			if (gpio == 12 || gpio == 13 || gpio == 40 || gpio == 41 || gpio == 45) {
				return DeviceMode.PWM_OUTPUT;
			}
			return DeviceMode.UNKNOWN;
		case PigpioConstants.MODE_PI_ALT5:
			if (gpio == 18 || gpio == 19) {
				return DeviceMode.PWM_OUTPUT;
			}
			return DeviceMode.UNKNOWN;
		default:
			return DeviceMode.UNKNOWN;
		}
	}

	@Override
	public int getGpioValue(int gpio) {
		return pigpioImpl.read(gpio);
	}

	@Override
	public List<Integer> getI2CBusNumbers() {
		// pigpiod does not expose this API, test if using JNI or sockets implementation
		if (pigpioImpl instanceof PigpioSocket) {
			throw new UnsupportedOperationException("pigpiod does not provide this interface");
		}
		return LocalSystemInfo.getI2CBusNumbers();
	}

	@Override
	public int getI2CFunctionalities(int controller) {
		// pigpiod does not expose this API, test if using JNI or sockets implementation
		if (pigpioImpl instanceof PigpioSocket) {
			throw new UnsupportedOperationException("pigpiod does not provide this interface");
		}
		return LocalSystemInfo.getI2CFunctionalities(controller);
	}

	@Override
	public GpioDigitalInputDeviceInterface createDigitalInputDevice(String key, PinInfo pinInfo, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws RuntimeIOException {
		return new PigpioJDigitalInputDevice(key, this, pigpioImpl, pinInfo.getDeviceNumber(), pud, trigger);
	}

	@Override
	public GpioDigitalOutputDeviceInterface createDigitalOutputDevice(String key, PinInfo pinInfo, boolean initialValue)
			throws RuntimeIOException {
		return new PigpioJDigitalOutputDevice(key, this, pigpioImpl, pinInfo.getDeviceNumber(), initialValue);
	}

	@Override
	public GpioDigitalInputOutputDeviceInterface createDigitalInputOutputDevice(String key, PinInfo pinInfo,
			DeviceMode mode) throws RuntimeIOException {
		return new PigpioJDigitalInputOutputDevice(key, this, pigpioImpl, pinInfo.getDeviceNumber(), mode);
	}

	@Override
	public InternalPwmOutputDeviceInterface createPwmOutputDevice(String key, PinInfo pinInfo, int pwmFrequency,
			float initialValue) throws RuntimeIOException {
		return new PigpioJPwmOutputDevice(key, this, pigpioImpl, pinInfo.getDeviceNumber(), initialValue,
				setPwmFrequency(pinInfo.getDeviceNumber(), pwmFrequency));
	}

	@Override
	public InternalServoDeviceInterface createServoDevice(String key, PinInfo pinInfo, int frequency,
			int minPulseWidthUs, int maxPulseWidthUs, int initialPulseWidthUs) {
		return new PigpioJServoDevice(key, this, pigpioImpl, pinInfo.getDeviceNumber(), minPulseWidthUs,
				maxPulseWidthUs, initialPulseWidthUs);
	}

	@Override
	public AnalogInputDeviceInterface createAnalogInputDevice(String key, PinInfo pinInfo) throws RuntimeIOException {
		throw new UnsupportedOperationException("Analog input pins not supported");
	}

	@Override
	public AnalogOutputDeviceInterface createAnalogOutputDevice(String key, PinInfo pinInfo, float initialValue)
			throws RuntimeIOException {
		throw new UnsupportedOperationException("Analog output devices aren't supported on this device");
	}

	@Override
	public InternalSpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect, int frequency,
			SpiClockMode spiClockMode, boolean lsbFirst) throws RuntimeIOException {
		return new PigpioJSpiDevice(key, this, pigpioImpl, controller, chipSelect, frequency, spiClockMode, lsbFirst);
	}

	@Override
	public InternalI2CDeviceInterface createI2CDevice(String key, int controller, int address,
			I2CConstants.AddressSize addressSize) throws RuntimeIOException {
		return new PigpioJI2CDevice(key, this, pigpioImpl, controller, address, addressSize);
	}

	@Override
	public InternalSerialDeviceInterface createSerialDevice(String key, String deviceFile, int baud,
			SerialDevice.DataBits dataBits, SerialDevice.StopBits stopBits, SerialDevice.Parity parity,
			boolean readBlocking, int minReadChars, int readTimeoutMillis) throws RuntimeIOException {
		return new PigpioJSerialDevice(key, this, pigpioImpl, deviceFile, baud, dataBits, stopBits, parity,
				readBlocking, minReadChars, readTimeoutMillis);
	}

	public PigpioJBitBangI2CDevice createI2CBitBangDevice(int sdaPin, int sclPin, int baud) {
		return new PigpioJBitBangI2CDevice("PigpioJ-BitBangI2C-" + sdaPin, this, sdaPin, sclPin, baud);
	}

	private int setPwmFrequency(int gpio, int pwmFrequency) {
		int old_freq = pigpioImpl.getPWMFrequency(gpio);
		int old_range = pigpioImpl.getPWMRange(gpio);
		int old_real_range = pigpioImpl.getPWMRealRange(gpio);
		pigpioImpl.setPWMFrequency(gpio, pwmFrequency);
		pigpioImpl.setPWMRange(gpio, pigpioImpl.getPWMRealRange(gpio));
		int new_freq = pigpioImpl.getPWMFrequency(gpio);
		int new_range = pigpioImpl.getPWMRange(gpio);
		int new_real_range = pigpioImpl.getPWMRealRange(gpio);
		Logger.info(
				"setPwmFrequency({}, {}), old freq={}, old real range={}, old range={},"
						+ " new freq={}, new real range={}, new range={}",
				Integer.valueOf(gpio), Integer.valueOf(pwmFrequency), Integer.valueOf(old_freq),
				Integer.valueOf(old_real_range), Integer.valueOf(old_range), Integer.valueOf(new_freq),
				Integer.valueOf(new_real_range), Integer.valueOf(new_range));

		return new_range;
	}

	static int getPigpioJPullUpDown(GpioPullUpDown pud) {
		int pigpio_pud;
		switch (pud) {
		case PULL_DOWN:
			pigpio_pud = PigpioConstants.PI_PUD_DOWN;
			break;
		case PULL_UP:
			pigpio_pud = PigpioConstants.PI_PUD_UP;
			break;
		case NONE:
		default:
			pigpio_pud = PigpioConstants.PI_PUD_OFF;
			break;
		}
		return pigpio_pud;
	}
}
