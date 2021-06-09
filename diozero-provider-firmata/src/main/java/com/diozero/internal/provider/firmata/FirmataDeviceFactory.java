package com.diozero.internal.provider.firmata;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Firmata
 * Filename:     FirmataDeviceFactory.java
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

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.firmata4j.IODevice;
import org.firmata4j.Pin;
import org.firmata4j.Pin.Mode;
import org.firmata4j.firmata.FirmataDevice;
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
import com.diozero.internal.provider.firmata.adapter.SerialFirmataAdapter;
import com.diozero.internal.provider.firmata.adapter.SocketFirmataAdapter;
import com.diozero.internal.spi.AnalogInputDeviceInterface;
import com.diozero.internal.spi.AnalogOutputDeviceInterface;
import com.diozero.internal.spi.BaseNativeDeviceFactory;
import com.diozero.internal.spi.GpioDigitalInputDeviceInterface;
import com.diozero.internal.spi.GpioDigitalInputOutputDeviceInterface;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;
import com.diozero.internal.spi.InternalI2CDeviceInterface;
import com.diozero.internal.spi.InternalSerialDeviceInterface;
import com.diozero.internal.spi.InternalSpiDeviceInterface;
import com.diozero.internal.spi.PwmOutputDeviceInterface;
import com.diozero.sbc.BoardInfo;
import com.diozero.sbc.LocalSystemInfo;
import com.diozero.util.PropertyUtil;
import com.diozero.util.RangeUtil;

public class FirmataDeviceFactory extends BaseNativeDeviceFactory implements FirmataEventListener {
	public static final String DEVICE_NAME = "Firmata";

	private static final String USE_FIRMATA4J_ADAPTER_PROP = "diozero.firmata.useFirmata4jAdapter";
	private static final String SERIAL_PORT_PROP = "diozero.firmata.serialPort";
	private static final String TCP_HOST_PROP = "diozero.firmata.tcpHostname";
	private static final String TCP_PORT_PROP = "diozero.firmata.tcpPort";
	private static final int DEFAULT_TCP_PORT = 3030;

	private boolean useFirmata4jAdapter;
	private String serialPortName;
	private IODevice ioDevice;
	private FirmataAdapter adapter;

	public FirmataDeviceFactory() {
		Logger.warn("*** Do NOT use this device factory for servo control; not yet implemented!");

		// FIXME Switch entirely to the diozero Firmata adapter
		useFirmata4jAdapter = PropertyUtil.getBooleanProperty(USE_FIRMATA4J_ADAPTER_PROP, true);
		if (useFirmata4jAdapter) {
			serialPortName = PropertyUtil.getProperty(SERIAL_PORT_PROP, null);
			if (serialPortName == null) {
				throw new IllegalArgumentException("Error, " + SERIAL_PORT_PROP + " not set");
			}

			ioDevice = new FirmataDevice(serialPortName);
		} else {
			serialPortName = PropertyUtil.getProperty(SERIAL_PORT_PROP, null);
			if (serialPortName == null) {
				String hostname = PropertyUtil.getProperty(TCP_HOST_PROP, null);
				if (hostname == null) {
					throw new IllegalArgumentException(
							"Error, either " + SERIAL_PORT_PROP + " or " + TCP_HOST_PROP + " must be set");
				}
				int port = PropertyUtil.getIntProperty(TCP_PORT_PROP, DEFAULT_TCP_PORT);
				adapter = new SocketFirmataAdapter(this, hostname, port);
			} else {
				adapter = new SerialFirmataAdapter(this, serialPortName, SerialConstants.BAUD_57600,
						SerialConstants.DataBits.CS8, SerialConstants.StopBits.ONE_STOP_BIT,
						SerialConstants.Parity.NO_PARITY, true, 1, 0);
			}
		}
	}

	@Override
	public void start() {
		if (useFirmata4jAdapter) {
			try {
				ioDevice.start();
				Logger.info("Waiting for Firmata device '" + serialPortName + "' to initialise");
				ioDevice.ensureInitializationIsDone();
				Logger.info("Firmata device '" + serialPortName + "' successfully initialised");
			} catch (IOException | InterruptedException e) {
				throw new RuntimeIOException(e);
			}
		} else {
			adapter.start();
			// TODO Configure the Firmata device's I2C delay?
			// adapter.i2cConfig(max_delay_ms);
		}
	}

	@Override
	public void shutdown() {
		Logger.trace("shutdown()");

		if (ioDevice != null) {
			try {
				ioDevice.stop();
			} catch (Exception e) {
			}
		}
		if (adapter != null) {
			adapter.close();
		}
	}

	IODevice getIoDevice() {
		return ioDevice;
	}

	FirmataAdapter getFirmataAdapter() {
		return adapter;
	}

	@Override
	public String getName() {
		return DEVICE_NAME;
	}

	@Override
	protected BoardInfo lookupBoardInfo() {
		if (useFirmata4jAdapter) {
			return new Firmata4jBoardInfo(ioDevice);
		}
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
	public DeviceMode getGpioMode(int gpio) {
		DeviceMode mode = DeviceMode.UNKNOWN;
		if (adapter != null) {
			switch (adapter.getPinMode(gpio)) {
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
			default:
				mode = DeviceMode.UNKNOWN;
			}
		} else {
			switch (ioDevice.getPin(gpio).getMode()) {
			case INPUT:
			case PULLUP:
				mode = DeviceMode.DIGITAL_INPUT;
				break;
			case OUTPUT:
				mode = DeviceMode.DIGITAL_OUTPUT;
				break;
			case ANALOG:
				mode = DeviceMode.ANALOG_INPUT;
				break;
			case PWM:
				mode = DeviceMode.PWM_OUTPUT;
				break;
			case SERVO:
				mode = DeviceMode.SERVO;
				break;
			default:
				mode = DeviceMode.UNKNOWN;
			}
		}

		return mode;
	}

	@Override
	public int getGpioValue(int gpio) {
		if (adapter != null) {
			return adapter.getValue(gpio);
		}
		return (int) ioDevice.getPin(gpio).getValue();
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
	public PwmOutputDeviceInterface createPwmOutputDevice(String key, PinInfo pinInfo, int pwmFrequency,
			float initialValue) throws RuntimeIOException {
		Logger.warn("PWM frequency will be ignored - Firmata does not allow this to be specified");
		return new FirmataPwmOutputDevice(this, key, pinInfo.getDeviceNumber(), initialValue);
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
		PinInfo pin_info = getBoardPinInfo().getByGpioNumberOrThrow(gpio);
		switch (eventType) {
		case DIGITAL:
			FirmataDigitalInputDevice did = getDevice(createPinKey(pin_info));
			did.accept(new DigitalInputEvent(gpio, epochTime, nanoTime, value != 0));
			break;
		case ANALOG:
			FirmataAnalogInputDevice aid = getDevice(createPinKey(pin_info));
			float f = RangeUtil.map(value, 0, adapter.getMax(gpio, PinMode.ANALOG_INPUT), 0f, 1f, true);
			aid.accept(new AnalogInputEvent(gpio, epochTime, nanoTime, f));
			break;
		default:
		}
	}

	public static class Firmata4jBoardInfo extends BoardInfo {
		private IODevice ioDevice;

		public Firmata4jBoardInfo(IODevice ioDevice) {
			super("Firmata", "Unknown", -1, "firmata");
			this.ioDevice = ioDevice;
		}

		@Override
		public void populateBoardPinInfo() {
			for (Pin pin : ioDevice.getPins()) {
				int pin_number = pin.getIndex();
				Set<DeviceMode> supported_modes = convertModes(pin.getSupportedModes());
				addGpioPinInfo(pin_number, pin_number, supported_modes);
				if (supported_modes.contains(DeviceMode.ANALOG_INPUT)) {
					addAdcPinInfo(pin_number, pin_number);
				}
				if (supported_modes.contains(DeviceMode.ANALOG_OUTPUT)) {
					addDacPinInfo(pin_number, pin_number);
				}
			}
		}

		private static Set<DeviceMode> convertModes(Set<Mode> firmataModes) {
			Set<DeviceMode> modes = new HashSet<>();

			for (Mode firmata_mode : firmataModes) {
				switch (firmata_mode) {
				case INPUT:
					modes.add(DeviceMode.DIGITAL_INPUT);
					break;
				case OUTPUT:
					modes.add(DeviceMode.DIGITAL_OUTPUT);
					break;
				case ANALOG:
					modes.add(DeviceMode.ANALOG_INPUT);
					break;
				case PWM:
					modes.add(DeviceMode.PWM_OUTPUT);
					break;
				default:
					// Ignore
				}
			}

			return modes;
		}
	}

	public static class FirmataAdapterBoardInfo extends BoardInfo {
		// TODO Check this value
		private static final float ADC_VREF = 3.3f;

		private FirmataAdapter adapter;

		public FirmataAdapterBoardInfo(FirmataAdapter adapter) {
			super(adapter.getFirmware().getName(),
					adapter.getFirmware().getVersionString() + "(" + adapter.getProtocolVersion() + ")", -1, ADC_VREF,
					LocalSystemInfo.getInstance().getDefaultLibraryPath());
			this.adapter = adapter;
		}

		@Override
		public void populateBoardPinInfo() {
			List<List<PinCapability>> board_capabilities = adapter.getBoardCapabilities();

			int gpio = 0;
			for (List<PinCapability> pin_capabilities : board_capabilities) {
				Set<DeviceMode> modes = convert(pin_capabilities);
				if (modes.contains(DeviceMode.DIGITAL_INPUT) || modes.contains(DeviceMode.DIGITAL_OUTPUT)
						|| modes.contains(DeviceMode.PWM_OUTPUT)) {
					addGpioPinInfo(gpio, gpio, modes);
				}
				if (modes.contains(DeviceMode.ANALOG_INPUT)) {
					addAdcPinInfo(gpio, gpio);
				}
				if (modes.contains(DeviceMode.ANALOG_OUTPUT)) {
					addDacPinInfo(gpio, gpio);
				}

				gpio++;
			}
		}

		private static Set<DeviceMode> convert(List<PinCapability> pinCapabilities) {
			Set<DeviceMode> modes = new HashSet<>();

			for (PinCapability capability : pinCapabilities) {
				switch (capability.getMode()) {
				case DIGITAL_INPUT:
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
				default:
					// Ignore
					Logger.debug("Ignoring Pin mode " + capability.getMode());
				}
			}

			return modes;
		}
	}
}
