package com.diozero.internal.provider.sysfs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.stream.Collectors;

import org.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.PinInfo;
import com.diozero.api.PwmPinInfo;
import com.diozero.api.SerialDevice;
import com.diozero.api.SpiClockMode;
import com.diozero.internal.SoftwarePwmOutputDevice;
import com.diozero.internal.board.odroid.OdroidBoardInfoProvider;
import com.diozero.internal.board.odroid.OdroidC2SysFsPwmOutputDevice;
import com.diozero.internal.provider.AnalogInputDeviceInterface;
import com.diozero.internal.provider.AnalogOutputDeviceInterface;
import com.diozero.internal.provider.BaseNativeDeviceFactory;
import com.diozero.internal.provider.GpioDigitalInputDeviceInterface;
import com.diozero.internal.provider.GpioDigitalInputOutputDeviceInterface;
import com.diozero.internal.provider.GpioDigitalOutputDeviceInterface;
import com.diozero.internal.provider.I2CDeviceInterface;
import com.diozero.internal.provider.PwmOutputDeviceInterface;
import com.diozero.internal.provider.SerialDeviceInterface;
import com.diozero.internal.provider.SpiDeviceInterface;
import com.diozero.util.BoardPinInfo;
import com.diozero.util.EpollNative;
import com.diozero.util.PropertyUtil;
import com.diozero.util.RuntimeIOException;

public class DefaultDeviceFactory extends BaseNativeDeviceFactory {
	private static final String USE_GPIO_CHARDEV_PROP = "diozero.gpio.chardev";

	private int boardPwmFrequency;
	private EpollNative epoll;
	private Map<Integer, NativeGpioChip> chips;
	private boolean useGpioCharDev = false;

	public DefaultDeviceFactory() {
		useGpioCharDev = PropertyUtil.getBooleanProperty(USE_GPIO_CHARDEV_PROP, useGpioCharDev);
	}

	private static final String GPIO_LINE_NUMBER_PATTERN = "GPIO(\\d+)";

	@Override
	public void start() {
		if (useGpioCharDev) {
			Logger.warn("Note using new NativeGpioChip char-dev GPIO implementation");

			try {
				// Get all gpiochips
				chips = Files.list(Paths.get("/dev")).filter(p -> p.getFileName().toString().startsWith("gpiochip"))
						.map(p -> NativeGpioChip.openChip(p.toString()))
						.collect(Collectors.toMap(NativeGpioChip::getChipId, chip -> chip));

				Logger.debug("Found {} GPIO chips", Integer.valueOf(chips.size()));

				// Validate the data in BoardPinInfo
				BoardPinInfo bpi = getBoardPinInfo();

				chips.values().forEach(chip -> {
					for (GpioLine gpio_line : chip.getLines()) {
						PinInfo pin_info = null;
						String line_name = gpio_line.getName().trim();
						// To to find this GPIO in the board pin info by the assume system name
						if (!line_name.isEmpty()) {
							pin_info = bpi.getByName(line_name);
						}

						// If the pin couldn't be found for the assigned name try to find the pin info
						// by chip and line offset number
						if (pin_info == null) {
							pin_info = bpi.getByChipAndOffset(chip.getChipId(), gpio_line.getOffset());
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

						if (pin_info == null && !line_name.isEmpty()) {
							Logger.debug("Detected GPIO line ({} {}-{}) that isn't configured in BoardPinInfo",
									line_name, Integer.valueOf(chip.getChipId()),
									Integer.valueOf(gpio_line.getOffset()));
							// Add a new pin info to the board pin info
							if (line_name.matches(GPIO_LINE_NUMBER_PATTERN)) {
								pin_info = bpi.addGpioPinInfo(
										Integer.parseInt(line_name.replaceAll(GPIO_LINE_NUMBER_PATTERN, "$1")),
										line_name, PinInfo.NOT_DEFINED, PinInfo.DIGITAL_IN_OUT, chip.getChipId(),
										gpio_line.getOffset());
							}
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

							if (!pin_info.getName().equals(line_name)) {
								Logger.warn("Configured pin name ({}) doesn't match that detected ({})",
										pin_info.getName(), line_name);
								// TODO What to do about it - update the board pin info?
							}
						}
					}
				});
			} catch (IOException e) {
				throw new RuntimeIOException("Error initialising GPIO chips: " + e.getMessage(), e);
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
	public GpioDigitalInputDeviceInterface createDigitalInputDevice(String key, PinInfo pinInfo, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws RuntimeIOException {
		if (useGpioCharDev) {
			if (pinInfo.getChip() == PinInfo.NOT_DEFINED) {
				throw new IllegalArgumentException("Chip not defined for pin " + pinInfo);
			}

			NativeGpioChip chip = chips.get(Integer.valueOf(pinInfo.getChip()));
			if (chip == null) {
				throw new IllegalArgumentException("Can't find chip for id " + pinInfo.getChip());
			}

			return new NativeGpioInputDevice(this, key, chip, pinInfo, pud, trigger);
		}

		return new SysFsDigitalInputDevice(this, key, pinInfo, trigger);
	}

	@Override
	public GpioDigitalOutputDeviceInterface createDigitalOutputDevice(String key, PinInfo pinInfo, boolean initialValue)
			throws RuntimeIOException {
		if (useGpioCharDev) {
			if (pinInfo.getChip() == PinInfo.NOT_DEFINED) {
				throw new IllegalArgumentException("Chip not defined for pin " + pinInfo);
			}

			NativeGpioChip chip = chips.get(Integer.valueOf(pinInfo.getChip()));
			if (chip == null) {
				throw new IllegalArgumentException("Can't find chip for id " + pinInfo.getChip());
			}

			return new NativeGpioOutputDevice(this, key, chip, pinInfo, initialValue);
		}

		return new SysFsDigitalOutputDevice(this, key, pinInfo, initialValue);
	}

	@Override
	public GpioDigitalInputOutputDeviceInterface createDigitalInputOutputDevice(String key, PinInfo pinInfo,
			DeviceMode mode) throws RuntimeIOException {
		if (useGpioCharDev) {
			if (pinInfo.getChip() == PinInfo.NOT_DEFINED) {
				throw new IllegalArgumentException("Chip not defined for pin " + pinInfo);
			}

			NativeGpioChip chip = chips.get(Integer.valueOf(pinInfo.getChip()));
			if (chip == null) {
				throw new IllegalArgumentException("Can't find chip for id " + pinInfo.getChip());
			}

			return new NativeGpioInputOutputDevice(this, key, chip, pinInfo, mode);
		}

		return new SysFsDigitalInputOutputDevice(this, key, pinInfo, mode);
	}

	@Override
	public PwmOutputDeviceInterface createPwmOutputDevice(String key, PinInfo pinInfo, int pwmFrequency,
			float initialValue) throws RuntimeIOException {
		if (pinInfo instanceof PwmPinInfo) {
			PwmPinInfo pwm_pin_info = (PwmPinInfo) pinInfo;
			// Odroid C2 runs with an older 3.x kernel hence has a different sysfs interface
			if (getBoardInfo().sameMakeAndModel(OdroidBoardInfoProvider.ODROID_C2)) {
				return new OdroidC2SysFsPwmOutputDevice(key, this, pwm_pin_info, pwmFrequency, initialValue);
			}

			return new SysFsPwmOutputDevice(key, this, getBoardInfo().getPwmChip(pwm_pin_info.getPwmNum()),
					pwm_pin_info, pwmFrequency, initialValue);
		}

		return new SoftwarePwmOutputDevice(key, this, createDigitalOutputDevice(createPinKey(pinInfo), pinInfo, false),
				pwmFrequency, initialValue);
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
	public SpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect, int frequency,
			SpiClockMode spiClockMode, boolean lsbFirst) throws RuntimeIOException {
		return new NativeSpiDeviceWrapper(this, key, controller, chipSelect, frequency, spiClockMode, lsbFirst);
	}

	@Override
	public I2CDeviceInterface createI2CDevice(String key, int controller, int address, int addressSize,
			int clockFrequency) throws RuntimeIOException {
		return new I2CDeviceWrapper(this, key, controller, address, addressSize, clockFrequency);
	}

	@Override
	public SerialDeviceInterface createSerialDevice(String key, String deviceFile, int baud,
			SerialDevice.DataBits dataBits, SerialDevice.StopBits stopBits, SerialDevice.Parity parity,
			boolean readBlocking, int minReadChars, int readTimeoutMillis) throws RuntimeIOException {
		return new NativeSerialDeviceWrapper(this, key, deviceFile, baud, dataBits, stopBits, parity, readBlocking,
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
