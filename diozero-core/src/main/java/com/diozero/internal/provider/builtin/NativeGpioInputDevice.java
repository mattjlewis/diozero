package com.diozero.internal.provider.builtin;

import org.tinylog.Logger;

import com.diozero.api.DigitalInputEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.PinInfo;
import com.diozero.internal.provider.AbstractInputDevice;
import com.diozero.internal.provider.GpioDigitalInputDeviceInterface;
import com.diozero.internal.provider.builtin.gpio.GpioLine;
import com.diozero.internal.provider.builtin.gpio.GpioLineEventListener;
import com.diozero.internal.provider.builtin.gpio.GpioChip;
import com.diozero.util.RuntimeIOException;

public class NativeGpioInputDevice extends AbstractInputDevice<DigitalInputEvent>
		implements GpioDigitalInputDeviceInterface, GpioLineEventListener {
	private GpioChip chip;
	private int gpio;
	private GpioLine line;

	public NativeGpioInputDevice(DefaultDeviceFactory deviceFactory, String key, GpioChip chip, PinInfo pinInfo,
			GpioPullUpDown pud, GpioEventTrigger trigger) {
		super(key, deviceFactory);

		gpio = pinInfo.getDeviceNumber();
		int offset = pinInfo.getLineOffset();
		if (offset == PinInfo.NOT_DEFINED) {
			throw new IllegalArgumentException("Line offset not defined for pin " + pinInfo);
		}
		this.chip = chip;

		line = chip.provisionGpioInputDevice(offset, pud, trigger);
	}

	@Override
	public int getGpio() {
		return gpio;
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		return line.getValue() == 0 ? false : true;
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
		line.close();
	}

	@Override
	public void event(int lineFd, int eventDataId, long timestampNanos) {
		valueChanged(new DigitalInputEvent(gpio, timestampNanos / 1_000_000, timestampNanos,
				eventDataId == GpioChip.GPIOEVENT_EVENT_RISING_EDGE));
	}
}
