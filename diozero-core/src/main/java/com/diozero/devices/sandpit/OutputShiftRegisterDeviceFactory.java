package com.diozero.devices.sandpit;

import java.util.EnumSet;

import org.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.PinInfo;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.SoftwarePwmOutputDevice;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.AbstractDeviceFactory;
import com.diozero.internal.spi.GpioDeviceFactoryInterface;
import com.diozero.internal.spi.GpioDigitalInputDeviceInterface;
import com.diozero.internal.spi.GpioDigitalInputOutputDeviceInterface;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;
import com.diozero.internal.spi.InternalPwmOutputDeviceInterface;
import com.diozero.internal.spi.PwmOutputDeviceFactoryInterface;
import com.diozero.sbc.BoardPinInfo;

public class OutputShiftRegisterDeviceFactory extends AbstractDeviceFactory
		implements GpioDeviceFactoryInterface, PwmOutputDeviceFactoryInterface {
	private static final String DEVICE_NAME = "OutputShiftRegister";
	private static final int DEFAULT_PWM_FREQUENCY = 50;

	/** DS: Serial Data Input [SER Pin 14] */
	private final DigitalOutputDevice dataPin;
	/** SH_CP: Shift Register Clock Pin [SRCLK Pin 11] */
	private final DigitalOutputDevice clockPin;
	/** ST_CP. Storage Register Clock Pin / Shift Output [RCLK Pin 12] */
	private final DigitalOutputDevice latchPin;

	private boolean[] buf;
	private boolean[] values;

	private BoardPinInfo boardPinInfo;

	public OutputShiftRegisterDeviceFactory(int dataGpio, int clockGpio, int latchGpio, int size) {
		super(DEVICE_NAME);

		dataPin = new DigitalOutputDevice(dataGpio);
		clockPin = new DigitalOutputDevice(clockGpio);
		latchPin = new DigitalOutputDevice(latchGpio);

		buf = new boolean[size];
		values = new boolean[size];

		boardPinInfo = new BoardPinInfo();
		for (int i = 0; i < size; i++) {
			boardPinInfo.addGpioPinInfo(i, i, EnumSet.of(DeviceMode.DIGITAL_OUTPUT));
		}
	}

	public boolean get(int outputPin) {
		return values[outputPin];
	}

	public void set(int outputPin, boolean value) {
		buf[outputPin] = value;
	}

	public void flush() {
		// Ground the latch pin and hold low for as long as you are transmitting
		latchPin.off();

		shiftOut();

		// Return the latch pin to high to signal to the chip that it no longer needs to
		// listen for information
		latchPin.on();
	}

	private void shiftOut() {
		for (int i = buf.length - 1; i >= 0; i--) {
			dataPin.setOn(buf[i]);
			values[i] = buf[i];

			clockPin.on();
			clockPin.off();
		}
	}

	public void clear() {
		for (int i = 0; i < buf.length; i++) {
			buf[i] = false;
		}
		flush();
	}

	@Override
	public void close() throws RuntimeIOException {
		clear();

		latchPin.close();
		clockPin.close();
		dataPin.close();
	}

	@Override
	public String getName() {
		return DEVICE_NAME;
	}

	@Override
	public BoardPinInfo getBoardPinInfo() {
		return boardPinInfo;
	}

	@Override
	public GpioDigitalInputDeviceInterface createDigitalInputDevice(String key, PinInfo pinInfo, GpioPullUpDown pud,
			GpioEventTrigger trigger) {
		throw new UnsupportedOperationException("Only digital output is supported");
	}

	@Override
	public GpioDigitalOutputDeviceInterface createDigitalOutputDevice(String key, PinInfo pinInfo,
			boolean initialValue) {
		return new OsrDigitalOutputDevice(this, key, pinInfo, initialValue);
	}

	@Override
	public GpioDigitalInputOutputDeviceInterface createDigitalInputOutputDevice(String key, PinInfo pinInfo,
			DeviceMode mode) {
		throw new UnsupportedOperationException("Only digital output is supported");
	}

	static final class OsrDigitalOutputDevice extends AbstractDevice implements GpioDigitalOutputDeviceInterface {
		private OutputShiftRegisterDeviceFactory osrDeviceFactory;
		private int gpio;

		public OsrDigitalOutputDevice(OutputShiftRegisterDeviceFactory osrDeviceFactory, String key, PinInfo pinInfo,
				boolean initialValue) {
			super(key, osrDeviceFactory);

			this.osrDeviceFactory = osrDeviceFactory;
			this.gpio = pinInfo.getDeviceNumber();

			setValue(initialValue);
		}

		@Override
		public int getGpio() {
			return gpio;
		}

		@Override
		public boolean getValue() throws RuntimeIOException {
			return osrDeviceFactory.get(gpio);
		}

		@Override
		public void setValue(boolean value) throws RuntimeIOException {
			osrDeviceFactory.set(gpio, value);
			osrDeviceFactory.flush();
		}

		@Override
		protected void closeDevice() throws RuntimeIOException {
			// Nothing to do?
			setValue(false);
		}
	}

	@Override
	public int getBoardPwmFrequency() {
		return DEFAULT_PWM_FREQUENCY;
	}

	@Override
	public void setBoardPwmFrequency(int pwmFrequency) {
		Logger.warn("PWM frequency is fixed");
	}

	@Override
	public InternalPwmOutputDeviceInterface createPwmOutputDevice(String key, PinInfo pinInfo, int pwmFrequency,
			float initialValue) {
		Logger.warn("Using software PWM on gpio {}", Integer.valueOf(pinInfo.getDeviceNumber()));

		// Need to make sure the keys are different
		// Note this is replicating the functionality in provisionDigitalOutputDevice.
		// That method can't be called as it will throw a device already opened
		// exception.
		// Also note that SoftwarePwmOutputDevice has special cleanup functionality.
		GpioDigitalOutputDeviceInterface gpio_output_device = createDigitalOutputDevice("PWM-" + key, pinInfo, false);
		deviceOpened(gpio_output_device);

		return new SoftwarePwmOutputDevice(key, this, gpio_output_device, pwmFrequency, initialValue);
	}
}
