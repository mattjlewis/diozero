package com.diozero.devices;

import org.tinylog.Logger;

import com.diozero.api.DeviceInterface;
import com.diozero.api.DigitalInputEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.RuntimeIOException;
import com.diozero.api.function.DeviceEventConsumer;

/**
 * A button with an embedded LED. Supported modes:
 * <ul>
 * <li>Light the button when pressed</li>
 * <li>Light then flash the button when pressed
 * <li>
 * </ul>
 */
public class LedButton implements DeviceInterface, DeviceEventConsumer<DigitalInputEvent> {
	private Button button;
	private LED led;

	public LedButton(int buttonGpio, int ledGpio) {
		button = new Button(buttonGpio, GpioPullUpDown.PULL_UP, GpioEventTrigger.BOTH);
		led = new LED(ledGpio);

		button.addListener(this);
	}

	@Override
	public void close() throws RuntimeIOException {
		button.close();
		led.close();
	}

	@Override
	public void accept(DigitalInputEvent event) {
		Logger.debug(event);
		led.setOn(event.isActive());
	}
}
