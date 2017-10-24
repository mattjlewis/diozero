package com.diozero.internal.provider.remote.firmata;

/*-
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Remote Provider
 * Filename:     FirmataProtocolHandlerTestApp.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2017 mattjlewis
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

import org.pmw.tinylog.Logger;

import com.diozero.devices.LED;
import com.diozero.devices.PwmLed;
import com.diozero.util.DeviceFactoryHelper;

public class FirmataProtocolHandlerTestApp {
	public static void main(String[] args) {
		System.setProperty("FIRMATA_TCP_HOST", "192.168.1.215");
		System.setProperty(DeviceFactoryHelper.DEVICE_FACTORY_PROP,
				"com.diozero.internal.provider.remote.devicefactory.RemoteDeviceFactory");

		int delay = 250;
		try {
			try (LED led = new LED(16, false)) {
				for (int i = 0; i < 5; i++) {
					Logger.debug("On");
					led.on();
					Thread.sleep(delay);
					Logger.debug("Off");
					led.off();
					Thread.sleep(delay);
				}
			}

			delay = 20;
			float step = 0.01f;
			try (PwmLed pwm_led = new PwmLed(16, 0)) {
				for (float f = 0; f < 1f; f += step) {
					pwm_led.setValue(f);
					Thread.sleep(delay);
				}
				for (float f = 1f; f >= 0; f -= step) {
					pwm_led.setValue(f);
					Thread.sleep(delay);
				}
			}
		} catch (InterruptedException e) {
		} finally {
			DeviceFactoryHelper.getNativeDeviceFactory().close();
		}
	}
}
