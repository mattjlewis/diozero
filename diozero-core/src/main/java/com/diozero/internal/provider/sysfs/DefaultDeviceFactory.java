package com.diozero.internal.provider.sysfs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
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
import com.diozero.util.EpollNative;
import com.diozero.util.PropertyUtil;
import com.diozero.util.RuntimeIOException;

public class DefaultDeviceFactory extends BaseNativeDeviceFactory {
	private static final String USE_GPIO_CHARDEV_PROP = "diozero.gpio.chardev";

	private int boardPwmFrequency;
	private EpollNative epoll;
	private Map<Integer, NativeGpioChip> chips;
	private Map<Integer, NativeGpioChip> gpioToChipMapping;
	private boolean useGpioCharDev = false;

	public DefaultDeviceFactory() {
		useGpioCharDev = PropertyUtil.getBooleanProperty(USE_GPIO_CHARDEV_PROP, useGpioCharDev);
	}

	@Override
	public void start() {
		if (useGpioCharDev) {
			Logger.warn("Note using new NativeGpioChip char-dev GPIO implementation");
			try {
				chips = Files.list(Paths.get("/dev")).filter(p -> p.toFile().isFile())
						.filter(p -> p.getFileName().toString().startsWith("gpiochip"))
						.map(p -> NativeGpioChip.openChip(p.toString().toString()))
						.collect(Collectors.toMap(NativeGpioChip::getChipId, chip -> chip));
				gpioToChipMapping = new HashMap<>();
				chips.values().forEach(chip -> {
					chip.getGpioLines()
							.forEach(gpio_line -> gpioToChipMapping.put(Integer.valueOf(gpio_line.getGpioNum()), chip));
				});
			} catch (IOException e) {
				throw new RuntimeIOException("Error initialising GPIO chips: " + e.getMessage(), e);
			}
		}
	}

	@Override
	public void shutdown() {
		Logger.trace("shutdown()");

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
			return new NativeGpioInputDevice(this, key,
					gpioToChipMapping.get(Integer.valueOf(pinInfo.getDeviceNumber())), pinInfo, pud, trigger);
		}
		return new SysFsDigitalInputDevice(this, key, pinInfo, trigger);
	}

	@Override
	public GpioDigitalOutputDeviceInterface createDigitalOutputDevice(String key, PinInfo pinInfo, boolean initialValue)
			throws RuntimeIOException {
		if (useGpioCharDev) {
			return new NativeGpioOutputDevice(this, key,
					gpioToChipMapping.get(Integer.valueOf(pinInfo.getDeviceNumber())), pinInfo, initialValue);
		}
		return new SysFsDigitalOutputDevice(this, key, pinInfo, initialValue);
	}

	@Override
	public GpioDigitalInputOutputDeviceInterface createDigitalInputOutputDevice(String key, PinInfo pinInfo,
			DeviceMode mode) throws RuntimeIOException {
		if (useGpioCharDev) {
			return new NativeGpioInputOutputDevice(this, key,
					gpioToChipMapping.get(Integer.valueOf(pinInfo.getDeviceNumber())), pinInfo, mode);
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

		SoftwarePwmOutputDevice pwm = new SoftwarePwmOutputDevice(key, this,
				createDigitalOutputDevice(createPinKey(pinInfo), pinInfo, false), pwmFrequency, initialValue);
		return pwm;
	}

	@Override
	public AnalogInputDeviceInterface createAnalogInputDevice(String key, PinInfo pinInfo) throws RuntimeIOException {
		// TODO How to work out the device number?
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
	public SerialDeviceInterface createSerialDevice(String key, String deviceName, int baud,
			SerialDevice.DataBits dataBits, SerialDevice.StopBits stopBits, SerialDevice.Parity parity,
			boolean readBlocking, int minReadChars, int readTimeoutMillis) throws RuntimeIOException {
		return new NativeSerialDeviceWrapper(this, key, deviceName, baud, dataBits, stopBits, parity, readBlocking,
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
