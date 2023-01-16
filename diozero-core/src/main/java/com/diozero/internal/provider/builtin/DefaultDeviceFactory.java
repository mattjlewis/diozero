package com.diozero.internal.provider.builtin;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     DefaultDeviceFactory.java
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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.I2CConstants;
import com.diozero.api.PinInfo;
import com.diozero.api.RuntimeIOException;
import com.diozero.api.SerialConstants;
import com.diozero.api.SpiClockMode;
import com.diozero.internal.SoftwarePwmOutputDevice;
import com.diozero.internal.board.GenericLinuxArmBoardInfo;
import com.diozero.internal.board.odroid.OdroidBoardInfoProvider;
import com.diozero.internal.board.odroid.OdroidC2SysFsPwmOutputDevice;
import com.diozero.internal.provider.builtin.gpio.GpioChip;
import com.diozero.internal.provider.builtin.gpio.GpioLine;
import com.diozero.internal.provider.builtin.i2c.NativeI2CDeviceJavaRaf;
import com.diozero.internal.provider.builtin.i2c.NativeI2CDeviceSMBus;
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
import com.diozero.internal.spi.MmapGpioInterface;
import com.diozero.sbc.BoardInfo;
import com.diozero.sbc.LocalSystemInfo;
import com.diozero.util.Diozero;
import com.diozero.util.EpollNative;
import com.diozero.util.LibraryLoader;
import com.diozero.util.PropertyUtil;

public class DefaultDeviceFactory extends BaseNativeDeviceFactory {
	private static final String GPIO_LINE_NUMBER_PATTERN = "GPIO(\\d+)";

	private static final String GPIO_USE_CHARDEV_PROP = "diozero.gpio.chardev";
	private static final String I2C_USE_JAVA_RAF_PROP = "diozero.i2c.javaraf";
	private static final String I2C_SLAVE_FORCE_PROP = "diozero.i2c.slaveforce";

	private static final boolean GPIO_USE_CHARDEV_DEFAULT = true;

	private int boardPwmFrequency;
	private int boardServoFrequency;
	private EpollNative epoll;
	private Map<Integer, GpioChip> chips;
	private boolean gpioUseCharDev;
	private boolean i2cUseJavaRaf;
	private boolean i2cSlaveForce;
	private MmapGpioInterface mmapGpio;

	public DefaultDeviceFactory() {
		gpioUseCharDev = PropertyUtil.getBooleanProperty(GPIO_USE_CHARDEV_PROP, GPIO_USE_CHARDEV_DEFAULT);
		i2cUseJavaRaf = PropertyUtil.isPropertySet(I2C_USE_JAVA_RAF_PROP);
		i2cSlaveForce = PropertyUtil.isPropertySet(I2C_SLAVE_FORCE_PROP);
	}

	@Override
	public void start() {
		Logger.debug("Using {} GPIO implementation", gpioUseCharDev ? "chardev" : "sysfs");

		LibraryLoader.loadSystemUtils();

		if (gpioUseCharDev) {
			try {
				// Open all gpiochips
				chips = GpioChip.openAllChips();

				Logger.debug("Found {} GPIO chips", Integer.valueOf(chips.size()));

				// Validate the data in BoardPinInfo
				final BoardInfo bpi = getBoardInfo();
				if (bpi instanceof GenericLinuxArmBoardInfo) {
					bpi.getChipMapping().ifPresent(chip_mapping -> updateChipIds(chips, chip_mapping, bpi));
				}

				// Validate the board pin info matches the GPIO chip and line offsets
				Logger.debug("Validating BoardPinInfo against detected GPIO chip and line offsets...");
				bpi.getGpioPins().forEach(pin_info -> {
					GpioChip chip = chips.get(Integer.valueOf(pin_info.getChip()));
					if (chip == null) {
						if (pin_info.getChip() != -1) {
							Logger.warn("No such chip for id {}", Integer.valueOf(pin_info.getChip()));
						}
					} else {
						if (chip.getLineByOffset(pin_info.getLineOffset()) == null) {
							Logger.warn("No such line offset {} for chip {}", Integer.valueOf(pin_info.getLineOffset()),
									Integer.valueOf(pin_info.getChip()));
						}
					}
				});

				// Validate the GPIO chip and line offsets match those detected
				Logger.debug("Validating detected GPIO chip and line offsets against BoardPinInfo...");
				chips.values().forEach(chip -> {
					for (GpioLine gpio_line : chip.getLines()) {
						PinInfo pin_info = null;
						final String line_name = gpio_line.getName().trim();

						// Try to find this GPIO in the board pin info by the assumed system name
						if (!line_name.isEmpty()) {
							pin_info = bpi.getByName(line_name);
						}

						// If the pin couldn't be found for the assigned name try to find the pin info
						// by chip and line offset number
						if (pin_info == null) {
							// Note that getByChipAndLineOffset doesn't create missing entries
							pin_info = bpi.getByChipAndLineOffset(chip.getChipId(), gpio_line.getOffset()).orElse(null);
						}

						// Finally, if still not found see if the name is in the format GPIOnn and
						// lookup by GPIO number
						if (pin_info == null && line_name.matches(GPIO_LINE_NUMBER_PATTERN)) {
							// Note that this isn't reliable - GPIO names are often missing or not in this
							// format
							// Note that the unknown / generic board info classes will create missing pin
							// info objects if you call getByGpioNumber - we don't want that to happen here
							pin_info = bpi.getGpios()
									.get(Integer.valueOf(line_name.replaceAll(GPIO_LINE_NUMBER_PATTERN, "$1")));
						}

						// XXX This includes a bit of a hack to ignore pins with the name "NC" - the BBB
						// does this for quite a few pins which triggers the warning message
						if (pin_info == null && !line_name.isEmpty() && !line_name.equals("NC")) {
							Logger.debug("Detected GPIO line ({} {}-{}) that isn't configured in BoardPinInfo",
									line_name, Integer.valueOf(chip.getChipId()),
									Integer.valueOf(gpio_line.getOffset()));
							// Add a new pin info to the board pin info
							int gpio_num;
							if (line_name.matches(GPIO_LINE_NUMBER_PATTERN)) {
								gpio_num = Integer.parseInt(line_name.replaceAll(GPIO_LINE_NUMBER_PATTERN, "$1"));
							} else {
								// Calculate the GPIO number
								gpio_num = chip.getLineOffset() + gpio_line.getOffset();
							}
							pin_info = bpi.addGpioPinInfo(gpio_num, line_name, PinInfo.NOT_DEFINED,
									PinInfo.DIGITAL_IN_OUT, chip.getChipId(), gpio_line.getOffset());
							Logger.debug("Added pin info {}", pin_info);
						} else if (pin_info != null) {
							if (pin_info.getChip() != chip.getChipId()
									|| pin_info.getLineOffset() != gpio_line.getOffset()) {
								Logger.warn(
										"Configured pin chip and line offset ({}-{}) doesn't match that detected ({}-{}), line name '{}' - updating",
										Integer.valueOf(pin_info.getChip()), Integer.valueOf(pin_info.getLineOffset()),
										Integer.valueOf(chip.getChipId()), Integer.valueOf(gpio_line.getOffset()),
										gpio_line.getName());
								pin_info.setChip(chip.getChipId());
								pin_info.setLineOffset(gpio_line.getOffset());
							}

							if (!line_name.isEmpty() && !pin_info.getName().equals(line_name)) {
								Logger.info("Configured pin name ({}) doesn't match that detected ({})",
										pin_info.getName(), line_name);
								// XXX What to do about it - update the board pin info? Just ignore for now.
							}
						}
					}
				});
			} catch (IOException e) {
				throw new RuntimeIOException("Error initialising GPIO chips: " + e.getMessage(), e);
			}
		}

		// See if memory mapped GPIO control is implemented for this board and
		// accessible for this user
		if (new File("/dev/gpiomem").canWrite() || new File("/dev/mem").canWrite()) {
			try {
				mmapGpio = getBoardInfo().createMmapGpio();
				mmapGpio.initialise();
			} catch (Throwable t) {
				// Ignore
				Logger.debug(t, "Failed to instantiate mmap gpio: {}", t);
				mmapGpio = null;
			}
		}

		if (mmapGpio == null) {
			Logger.warn("GPIO pull-up / pull-down control is not available for this board");
		} else {
			Logger.debug("Memory mapped GPIO is available for this board");
		}
	}

	private static void updateChipIds(Map<Integer, GpioChip> chips, Map<String, Integer> chipMapping, BoardInfo bpi) {
		if (chips == null || chips.isEmpty()) {
			return;
		}

		Logger.debug("Processing chipMapping {}", chipMapping);

		for (final Map.Entry<String, Integer> mapping : chipMapping.entrySet()) {
			final GpioChip chip = chips.get(mapping.getValue());
			// If the labels don't match ...
			if (chip != null && !mapping.getKey().equals(chip.getLabel())) {
				Logger.debug("Boarddef chip id {} ('{}') doesn't match actual id {} ('{}')", mapping.getValue(),
						mapping.getKey(), Integer.valueOf(chip.getChipId()), chip.getLabel());
				// Locate the actual GPIO chip with this label and update the chip id
				chips.values().stream().filter(ch -> ch.getLabel().equals(mapping.getKey())).findFirst()
						.ifPresent(ch -> bpi.updateGpioChipId(mapping.getValue().intValue(), ch.getChipId()));
			}
		}
	}

	@Override
	public void shutdown() {
		Logger.trace("shutdown()");

		if (chips != null) {
			chips.values().forEach(chip -> chip.close());
			chips.clear();
		}

		if (epoll != null) {
			epoll.close();
		}

		if (mmapGpio != null) {
			mmapGpio.close();
		}
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
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
	public DeviceMode getGpioMode(int gpio) {
		if (mmapGpio != null) {
			return mmapGpio.getMode(gpio);
		}
		return DeviceMode.UNKNOWN;
	}

	@Override
	public int getGpioValue(int gpio) {
		if (mmapGpio != null) {
			return mmapGpio.gpioRead(gpio) ? 1 : 0;
		}
		return Diozero.UNKNOWN_VALUE;
	}

	@Override
	public List<Integer> getI2CBusNumbers() {
		return LocalSystemInfo.getI2CBusNumbers();
	}

	@Override
	public int getI2CFunctionalities(int controller) {
		return LocalSystemInfo.getI2CFunctionalities(controller);
	}

	@Override
	public GpioDigitalInputDeviceInterface createDigitalInputDevice(String key, PinInfo pinInfo, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws RuntimeIOException {
		if (gpioUseCharDev) {
			if (pinInfo.getChip() == PinInfo.NOT_DEFINED) {
				throw new IllegalArgumentException("Chip not defined for pin " + pinInfo);
			}

			GpioChip chip = chips.get(Integer.valueOf(pinInfo.getChip()));
			if (chip == null) {
				throw new IllegalArgumentException("Can't find chip for id " + pinInfo.getChip());
			}

			return new NativeGpioInputDevice(this, key, chip, pinInfo, pud, trigger, mmapGpio);
		}

		// Note not possible to set pud value using sysfs
		return new SysFsDigitalInputDevice(this, key, pinInfo, trigger);
	}

	@Override
	public GpioDigitalOutputDeviceInterface createDigitalOutputDevice(String key, PinInfo pinInfo, boolean initialValue)
			throws RuntimeIOException {
		if (gpioUseCharDev) {
			if (pinInfo.getChip() == PinInfo.NOT_DEFINED) {
				throw new IllegalArgumentException("Chip not defined for pin " + pinInfo);
			}

			GpioChip chip = chips.get(Integer.valueOf(pinInfo.getChip()));
			if (chip == null) {
				throw new IllegalArgumentException("Can't find chip for id " + pinInfo.getChip());
			}

			return new NativeGpioOutputDevice(this, key, chip, pinInfo, initialValue, mmapGpio);
		}

		return new SysFsDigitalOutputDevice(this, key, pinInfo, initialValue, mmapGpio);
	}

	@Override
	public GpioDigitalInputOutputDeviceInterface createDigitalInputOutputDevice(String key, PinInfo pinInfo,
			DeviceMode mode) throws RuntimeIOException {
		if (gpioUseCharDev) {
			if (pinInfo.getChip() == PinInfo.NOT_DEFINED) {
				throw new IllegalArgumentException("Chip not defined for pin " + pinInfo);
			}

			GpioChip chip = chips.get(Integer.valueOf(pinInfo.getChip()));
			if (chip == null) {
				throw new IllegalArgumentException("Can't find chip for id " + pinInfo.getChip());
			}

			return new NativeGpioInputOutputDevice(this, key, chip, pinInfo, mode, mmapGpio);
		}

		return new SysFsDigitalInputOutputDevice(this, key, pinInfo, mode);
	}

	@Override
	public InternalPwmOutputDeviceInterface createPwmOutputDevice(String key, PinInfo pinInfo, int pwmFrequency,
			float initialValue) throws RuntimeIOException {
		try {
			if (SysFsPwmOutputDevice.isSupported(this, pinInfo)) {
				// Odroid C2 runs with an older 3.x kernel hence has a different sysfs interface
				if (getBoardInfo().compareMakeAndModel(OdroidBoardInfoProvider.MAKE,
						OdroidBoardInfoProvider.C2_HARDWARE_ID)) {
					return new OdroidC2SysFsPwmOutputDevice(key, this, pinInfo, pwmFrequency, initialValue);
				}

				return new SysFsPwmOutputDevice(key, this, pinInfo, pwmFrequency, initialValue, mmapGpio);
			}
		} catch (RuntimeIOException e) {
			Logger.warn(e, "Error creating sysfs PWM output device: {}", e);
		}

		// Need to make sure the keys are different
		// Note this is replicating the functionality in provisionDigitalOutputDevice.
		// That method can't be called as it will throw a device already opened
		// exception.
		// Also note that SoftwarePwmOutputDevice has special cleanup functionality.
		GpioDigitalOutputDeviceInterface gpio_output_device = createDigitalOutputDevice("PWM-" + key, pinInfo, false);
		deviceOpened(gpio_output_device);

		return new SoftwarePwmOutputDevice(key, this, gpio_output_device, pwmFrequency, initialValue);
	}

	@Override
	public InternalServoDeviceInterface createServoDevice(String key, PinInfo pinInfo, int frequencyHz,
			int minPulseWidthUs, int maxPulseWidthUs, int initialPulseWidthUs) {
		// Note that by default there is no native support for servo devices in the
		// default device factory - only via PWM control. Firmata devices do have native
		// support for servo control

		throw new UnsupportedOperationException("Native servo devices are not supported - use PWM instead");
	}

	@Override
	public AnalogInputDeviceInterface createAnalogInputDevice(String key, PinInfo pinInfo) throws RuntimeIOException {
		// FIXME Work out the system device number! ("/sys/bus/iio/devices/iio:deviceN")
		int device = 0;
		return new SysFsAnalogInputDevice(this, key, device, pinInfo.getDeviceNumber());
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
		return new DefaultNativeSpiDevice(this, key, controller, chipSelect, frequency, spiClockMode, lsbFirst);
	}

	@Override
	public InternalI2CDeviceInterface createI2CDevice(String key, int controller, int address,
			I2CConstants.AddressSize addressSize) throws RuntimeIOException {
		if (i2cUseJavaRaf) {
			return new NativeI2CDeviceJavaRaf(this, key, controller, address, addressSize, i2cSlaveForce);
		}
		return new NativeI2CDeviceSMBus(this, key, controller, address, addressSize, i2cSlaveForce);
	}

	@Override
	public InternalSerialDeviceInterface createSerialDevice(String key, String deviceFile, int baud,
			SerialConstants.DataBits dataBits, SerialConstants.StopBits stopBits, SerialConstants.Parity parity,
			boolean readBlocking, int minReadChars, int readTimeoutMillis) throws RuntimeIOException {
		return new DefaultNativeSerialDevice(this, key, deviceFile, baud, dataBits, stopBits, parity, readBlocking,
				minReadChars, readTimeoutMillis);
	}

	synchronized EpollNative getEpoll() {
		// Has to be lazy loaded as cannot call System.loadLibrary within device factory
		// initialisation
		if (epoll == null) {
			epoll = new EpollNative();
			epoll.enableEvents();
		}
		return epoll;
	}
}
