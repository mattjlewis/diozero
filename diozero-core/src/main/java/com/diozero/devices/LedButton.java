package com.diozero.devices;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     LedButton.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2023 diozero
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
