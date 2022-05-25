package com.diozero.internal.provider.firmata;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Firmata
 * Filename:     FirmataDeviceFactory.java
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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.tinylog.Logger;

import com.diozero.api.AnalogInputEvent;
import com.diozero.api.DeviceAlreadyOpenedException;
import com.diozero.api.DeviceMode;
import com.diozero.api.DigitalInputEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.I2CConstants;
import com.diozero.api.InvalidModeException;
import com.diozero.api.PinInfo;
import com.diozero.api.RuntimeIOException;
import com.diozero.api.SerialConstants;
import com.diozero.api.SerialDevice;
import com.diozero.api.SpiClockMode;
import com.diozero.internal.provider.firmata.adapter.FirmataAdapter;
import com.diozero.internal.provider.firmata.adapter.FirmataEventListener;
import com.diozero.internal.provider.firmata.adapter.FirmataProtocol.PinCapability;
import com.diozero.internal.provider.firmata.adapter.FirmataProtocol.PinMode;
import com.diozero.internal.provider.firmata.adapter.FirmataTransport;
import com.diozero.internal.provider.firmata.adapter.SerialFirmataTransport;
import com.diozero.internal.provider.firmata.adapter.SocketFirmataTransport;
import com.diozero.internal.spi.AbstractDevice;
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
import com.diozero.util.RangeUtil;

/*
 * Try out ConfigurableFirmata - is there actually any difference to the StandardFirmata protocol?
 * Wireless access to Firmata devices (network and Bluetooth).
 * E.g. ESP32 - https://learn.sparkfun.com/tutorials/esp32-thing-hookup-guide?_ga=1.116824388.33505106.1471290985#installing-the-esp32-arduino-core
 * Firmata GitHub issue #315 - https://github.com/firmata/arduino/issues/315
 */
public class FirmataDeviceFactory extends BaseNativeDeviceFactory implements FirmataEventListener {
	public static final String DEVICE_NAME = "Firmata";

	static final String SERIAL_PORT_PROP = "diozero.firmata.serialPort";
	static final String SERIAL_BAUD_PROP = "diozero.firmata.serialBaud";
	static final String SERIAL_DATA_BITS_PROP = "diozero.firmata.serialDataBits";
	static final String SERIAL_STOP_BITS_PROP = "diozero.firmata.serialStopBits";
	static final String SERIAL_PARITY_PROP = "diozero.firmata.serialParity";
	static final String TCP_HOST_PROP = "diozero.firmata.tcpHostname";
	static final String TCP_PORT_PROP = "diozero.firmata.tcpPort";
	static final int DEFAULT_TCP_PORT = 3030;

	private String serialPortName;
	private FirmataAdapter adapter;

	@SuppressWarnings("resource")
	public static FirmataDeviceFactory newSerialInstance(String serialPortName) {
		SerialDevice.DataBits data_bits = SerialConstants.DEFAULT_DATA_BITS;
		String val = PropertyUtil.getProperty(SERIAL_DATA_BITS_PROP, null);
		if (val != null) {
			data_bits = SerialDevice.DataBits.valueOf(val.trim());
		}

		SerialDevice.StopBits stop_bits = SerialConstants.DEFAULT_STOP_BITS;
		val = PropertyUtil.getProperty(SERIAL_STOP_BITS_PROP, null);
		if (val != null) {
			stop_bits = SerialDevice.StopBits.valueOf(val.trim());
		}

		SerialDevice.Parity parity = SerialConstants.DEFAULT_PARITY;
		val = PropertyUtil.getProperty(SERIAL_PARITY_PROP, null);
		if (val != null) {
			parity = SerialDevice.Parity.valueOf(val.trim());
		}

		return new FirmataDeviceFactory(new SerialFirmataTransport(serialPortName,
				PropertyUtil.getIntProperty(SERIAL_BAUD_PROP, SerialConstants.BAUD_57600), data_bits, stop_bits, parity,
				SerialConstants.DEFAULT_READ_BLOCKING, SerialConstants.DEFAULT_MIN_READ_CHARS,
				SerialConstants.DEFAULT_READ_TIMEOUT_MILLIS));
	}

	public static FirmataDeviceFactory newSocketInstance(String hostname) {
		return newSocketInstance(hostname, PropertyUtil.getIntProperty(TCP_PORT_PROP, DEFAULT_TCP_PORT));
	}

	@SuppressWarnings("resource")
	public static FirmataDeviceFactory newSocketInstance(String hostname, int port) {
		return new FirmataDeviceFactory(new SocketFirmataTransport(hostname, port));
	}

	@SuppressWarnings("resource")
	public FirmataDeviceFactory() {
		serialPortName = PropertyUtil.getProperty(SERIAL_PORT_PROP, null);
		if (serialPortName == null) {
			String hostname = PropertyUtil.getProperty(TCP_HOST_PROP, null);
			if (hostname == null) {
				throw new IllegalArgumentException(
						"Error, either " + SERIAL_PORT_PROP + " or " + TCP_HOST_PROP + " must be set");
			}
			int port = PropertyUtil.getIntProperty(TCP_PORT_PROP, DEFAULT_TCP_PORT);
			adapter = new FirmataAdapter(new SocketFirmataTransport(hostname, port), this);
		} else {
			SerialDevice.DataBits data_bits = SerialConstants.DEFAULT_DATA_BITS;
			String val = PropertyUtil.getProperty(SERIAL_DATA_BITS_PROP, null);
			if (val != null) {
				data_bits = SerialDevice.DataBits.valueOf(val.trim());
			}

			SerialDevice.StopBits stop_bits = SerialConstants.DEFAULT_STOP_BITS;
			val = PropertyUtil.getProperty(SERIAL_STOP_BITS_PROP, null);
			if (val != null) {
				stop_bits = SerialDevice.StopBits.valueOf(val.trim());
			}

			SerialDevice.Parity parity = SerialConstants.DEFAULT_PARITY;
			val = PropertyUtil.getProperty(SERIAL_PARITY_PROP, null);
			if (val != null) {
				parity = SerialDevice.Parity.valueOf(val.trim());
			}

			adapter = new FirmataAdapter(new SerialFirmataTransport(serialPortName,
					PropertyUtil.getIntProperty(SERIAL_BAUD_PROP, SerialConstants.BAUD_57600), data_bits, stop_bits,
					parity, SerialConstants.DEFAULT_READ_BLOCKING, SerialConstants.DEFAULT_MIN_READ_CHARS,
					SerialConstants.DEFAULT_READ_TIMEOUT_MILLIS), this);
		}
	}

	public FirmataDeviceFactory(FirmataTransport transport) {
		this.adapter = new FirmataAdapter(transport, this);
	}

	@Override
	public void start() {
		adapter.start();
	}

	@Override
	public void shutdown() {
		Logger.trace("shutdown()");

		if (adapter != null) {
			adapter.close();
		}
	}

	public FirmataAdapter getFirmataAdapter() {
		return adapter;
	}

	@Override
	public String getName() {
		return DEVICE_NAME;
	}

	@Override
	protected BoardInfo lookupBoardInfo() {
		return new FirmataAdapterBoardInfo(adapter);
	}

	@Override
	public int getBoardPwmFrequency() {
		/*
		 * https://www.arduino.cc/en/Tutorial/SecretsOfArduinoPWM // Note that the base
		 * frequency for pins 3, 9, 10, and 11 is 31250 Hz and for pins 5 and 6 is 62500
		 * Hz switch (gpio) { case 3: case 9: case 10: case 11: return 31250; case 5:
		 * case 6: return 62500; } return -1;
		 */
		//
		return 62500;
	}

	@Override
	public void setBoardPwmFrequency(int frequency) {
		// Ignore
		Logger.warn("Not implemented");
	}

	@Override
	public int getBoardServoFrequency() {
		return 50;
	}

	@Override
	public void setBoardServoFrequency(int frequency) {
		// Ignore
		Logger.warn("Not implemented");
	}

	@Override
	public DeviceMode getGpioMode(int gpio) {
		adapter.refreshPinState(gpio);

		DeviceMode mode;
		PinMode pin_mode = adapter.getPinMode(gpio);
		switch (pin_mode) {
		case DIGITAL_INPUT:
		case INPUT_PULLUP:
			mode = DeviceMode.DIGITAL_INPUT;
			break;
		case DIGITAL_OUTPUT:
			mode = DeviceMode.DIGITAL_OUTPUT;
			break;
		case ANALOG_INPUT:
			mode = DeviceMode.ANALOG_INPUT;
			break;
		case PWM:
			mode = DeviceMode.PWM_OUTPUT;
			break;
		case SERVO:
			mode = DeviceMode.SERVO;
			break;
		case I2C:
			mode = DeviceMode.I2C;
			break;
		case SERIAL:
			mode = DeviceMode.SERIAL;
			break;
		default:
			Logger.debug("Unhandled Firmata pin mode for pin {}: {}", Integer.valueOf(gpio), pin_mode);
			mode = DeviceMode.UNKNOWN;
		}

		return mode;
	}

	@Override
	public int getGpioValue(int gpio) {
		adapter.refreshPinState(gpio);

		return adapter.getValue(gpio);
	}

	@Override
	public AnalogInputDeviceInterface provisionAnalogInputDevice(PinInfo pinInfo) {
		// Special case - The Arduino can switch between digital and analog input hence
		// use of gpio rather than adc
		if (!pinInfo.isSupported(DeviceMode.ANALOG_INPUT)) {
			throw new InvalidModeException("Invalid mode (analog input) for pin " + pinInfo);
		}

		String key = createPinKey(pinInfo);

		// Check if this pin is already provisioned
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}

		AnalogInputDeviceInterface device = createAnalogInputDevice(key, pinInfo);
		deviceOpened(device);

		return device;
	}

	@Override
	public GpioDigitalInputDeviceInterface createDigitalInputDevice(String key, PinInfo pinInfo, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws RuntimeIOException {
		return new FirmataDigitalInputDevice(this, key, pinInfo.getDeviceNumber(), pud, trigger);
	}

	@Override
	public GpioDigitalOutputDeviceInterface createDigitalOutputDevice(String key, PinInfo pinInfo, boolean initialValue)
			throws RuntimeIOException {
		return new FirmataDigitalOutputDevice(this, key, pinInfo.getDeviceNumber(), initialValue);
	}

	@Override
	public GpioDigitalInputOutputDeviceInterface createDigitalInputOutputDevice(String key, PinInfo pinInfo,
			DeviceMode mode) throws RuntimeIOException {
		return new FirmataDigitalInputOutputDevice(this, key, pinInfo.getDeviceNumber(), mode);
	}

	@Override
	public InternalPwmOutputDeviceInterface createPwmOutputDevice(String key, PinInfo pinInfo, int pwmFrequency,
			float initialValue) throws RuntimeIOException {
		if (!pinInfo.getModes().contains(DeviceMode.PWM_OUTPUT)) {
			throw new InvalidModeException("Invalid mode (PWM) for GPIO " + pinInfo);
		}

		Logger.warn("PWM frequency will be ignored - Firmata does not allow this to be specified");
		return new FirmataPwmOutputDevice(this, key, pinInfo.getDeviceNumber(), initialValue);
	}

	@Override
	public InternalServoDeviceInterface createServoDevice(String key, PinInfo pinInfo, int frequency,
			int minPulseWidthUs, int maxPulseWidthUs, int initialPulseWidthUs) {
		return new FirmataServoDevice(this, key, pinInfo.getDeviceNumber(), minPulseWidthUs, maxPulseWidthUs,
				initialPulseWidthUs);
	}

	@Override
	public AnalogInputDeviceInterface createAnalogInputDevice(String key, PinInfo pinInfo) throws RuntimeIOException {
		return new FirmataAnalogInputDevice(this, key, pinInfo.getDeviceNumber());
	}

	@Override
	public AnalogOutputDeviceInterface createAnalogOutputDevice(String key, PinInfo pinInfo, float initialValue)
			throws RuntimeIOException {
		throw new UnsupportedOperationException("Analog output not supported by device factory '"
				+ getClass().getSimpleName() + "' on device '" + getBoardInfo().getName() + "'");
	}

	@Override
	public InternalSpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect, int frequency,
			SpiClockMode spiClockMode, boolean lsbFirst) throws RuntimeIOException {
		throw new UnsupportedOperationException("SPI is not supported by device factory '" + getClass().getSimpleName()
				+ "' on device '" + getBoardInfo().getName() + "'");
	}

	@Override
	public InternalI2CDeviceInterface createI2CDevice(String key, int controller, int address,
			I2CConstants.AddressSize addressSize) throws RuntimeIOException {
		return new FirmataI2CDevice(this, key, controller, address, addressSize);
	}

	@Override
	public InternalSerialDeviceInterface createSerialDevice(String key, String deviceFile, int baud,
			SerialDevice.DataBits dataBits, SerialDevice.StopBits stopBits, SerialDevice.Parity parity,
			boolean readBlocking, int minReadChars, int readTimeoutMillis) throws RuntimeIOException {
		throw new UnsupportedOperationException("Serial communication not available in the device factory");
	}

	@Override
	public void event(EventType eventType, int gpio, int value, long epochTime, long nanoTime) {
		Optional<PinInfo> pin_info_opt = getBoardPinInfo().getByGpioNumber(gpio);
		if (!pin_info_opt.isPresent()) {
			return;
		}
		PinInfo pin_info = pin_info_opt.get();
		switch (eventType) {
		case DIGITAL:
			// Need to do this as Firmata reports for banks of 8 GPIOs
			AbstractDevice device = getDevice(createPinKey(pin_info));
			if (device instanceof FirmataDigitalInputDevice) {
				FirmataDigitalInputDevice did = (FirmataDigitalInputDevice) device;
				if (did != null) {
					did.accept(new DigitalInputEvent(gpio, epochTime, nanoTime, value != 0));
				}
			}
			break;
		case ANALOG:
			FirmataAnalogInputDevice aid = getDevice(createPinKey(pin_info));
			if (aid != null) {
				float f = RangeUtil.map(value, 0, adapter.getMax(gpio, PinMode.ANALOG_INPUT), 0f, 1f, true);
				aid.accept(new AnalogInputEvent(gpio, epochTime, nanoTime, f));
			}
			break;
		default:
		}
	}

	public static class FirmataAdapterBoardInfo extends BoardInfo {
		// TODO Check this value
		private static final float ADC_VREF = 5f;

		private FirmataAdapter adapter;

		public FirmataAdapterBoardInfo(FirmataAdapter adapter) {
			super(adapter.getFirmware().getName(), adapter.getFirmware().getVersionString(), -1, ADC_VREF,
					LocalSystemInfo.getInstance().getDefaultLibraryPath(),
					LocalSystemInfo.getInstance().getOperatingSystemId(),
					LocalSystemInfo.getInstance().getOperatingSystemVersion());
			this.adapter = adapter;
		}

		@Override
		public void populateBoardPinInfo() {
			List<Set<PinCapability>> board_capabilities = adapter.getBoardCapabilities();

			int gpio_num = 0;
			int adc_num = 0;
			int physical_pin = 0;
			for (Set<PinCapability> pin_capabilities : board_capabilities) {
				Set<DeviceMode> modes = convert(pin_capabilities);
				if (modes.contains(DeviceMode.ANALOG_INPUT)) {
					addAdcPinInfo(physical_pin, "A" + adc_num, physical_pin);

					adc_num++;
				} else if (modes.contains(DeviceMode.DIGITAL_INPUT) || modes.contains(DeviceMode.DIGITAL_OUTPUT)
						|| modes.contains(DeviceMode.PWM_OUTPUT)) {
					addGpioPinInfo(physical_pin, "D" + gpio_num, physical_pin, modes);
				} else {
					Logger.debug("Skipping pin " + physical_pin + ", modes: " + modes);
				}

				gpio_num++;
				physical_pin++;
			}
		}

		private static Set<DeviceMode> convert(Set<PinCapability> pinCapabilities) {
			Set<DeviceMode> modes = new HashSet<>();

			for (PinCapability capability : pinCapabilities) {
				switch (capability.getMode()) {
				case DIGITAL_INPUT:
				case INPUT_PULLUP:
					modes.add(DeviceMode.DIGITAL_INPUT);
					break;
				case DIGITAL_OUTPUT:
					modes.add(DeviceMode.DIGITAL_OUTPUT);
					break;
				case ANALOG_INPUT:
					modes.add(DeviceMode.ANALOG_INPUT);
					break;
				case PWM:
					modes.add(DeviceMode.PWM_OUTPUT);
					break;
				case SERVO:
					modes.add(DeviceMode.SERVO);
					break;
				case I2C:
					modes.add(DeviceMode.I2C);
					break;
				case SERIAL:
					modes.add(DeviceMode.SERIAL);
					break;
				default:
					// Ignore
					Logger.debug("Ignoring Pin mode " + capability.getMode());
				}
			}

			return modes;
		}
	}
}
