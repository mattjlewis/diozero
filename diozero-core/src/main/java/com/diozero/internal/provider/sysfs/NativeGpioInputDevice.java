package com.diozero.internal.provider.sysfs;

import org.tinylog.Logger;

import com.diozero.api.DigitalInputEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.PinInfo;
import com.diozero.internal.provider.AbstractInputDevice;
import com.diozero.internal.provider.GpioDigitalInputDeviceInterface;
import com.diozero.util.RuntimeIOException;

public class NativeGpioInputDevice extends AbstractInputDevice<DigitalInputEvent>
		implements GpioDigitalInputDeviceInterface, GpioLineEventListener {
	private NativeGpioChip chip;
	private int gpio;
	private int offset;
	private GpioLine line;

	public NativeGpioInputDevice(DefaultDeviceFactory deviceFactory, String key, NativeGpioChip chip, PinInfo pinInfo,
			GpioPullUpDown pud, GpioEventTrigger trigger) {
		super(key, deviceFactory);

		gpio = pinInfo.getDeviceNumber();
		offset = chip.getOffset(gpio);
		if (offset == -1) {
			throw new IllegalArgumentException("Invalid GPIO " + gpio + " - offset not found");
		}
		this.chip = chip;

		line = chip.provisionGpioInputDevice(gpio, pud, trigger);
	}

	@Override
	public int getGpio() {
		return gpio;
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		return chip.getValue(offset) == 0 ? false : true;
	}

	@Override
	public void setDebounceTimeMillis(int debounceTime) {
		Logger.warn("Debounce not supported");
	}

	@Override
	protected void enableListener() {
		chip.register(line.getFd(), this);
	}

	@Override
	protected void disableListener() {
		chip.deregister(line.getFd());
	}

	@Override
	public void closeDevice() {
		NativeGpioChip.close(line.getFd());
	}

	@Override
	public void event(int gpioOffset, int eventDataId, long timestampNanos) {
		valueChanged(new DigitalInputEvent(gpio, timestampNanos / 1_000_000, timestampNanos,
				eventDataId == NativeGpioChip.GPIOEVENT_EVENT_RISING_EDGE));
	}
}
