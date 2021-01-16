package com.diozero.internal.provider.remote.firmata;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Remote Provider
 * Filename:     FirmataProtocolHandlerTestApp.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
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

import org.tinylog.Logger;

import com.diozero.api.AnalogInputDevice;
import com.diozero.devices.LED;
import com.diozero.devices.PwmLed;
import com.diozero.sbc.BoardInfo;
import com.diozero.sbc.DeviceFactoryHelper;

public class FirmataProtocolHandlerTestApp {
	public static void main(String[] args) {
		System.setProperty("FIRMATA_TCP_HOST", "192.168.1.215");
		System.setProperty(DeviceFactoryHelper.DEVICE_FACTORY_PROP,
				"com.diozero.internal.provider.remote.devicefactory.RemoteDeviceFactory");
		
		BoardInfo board_info =  DeviceFactoryHelper.getNativeDeviceFactory().getBoardInfo();
		Logger.debug("Board info:");
		Logger.debug("Name: {}, Make: {}, Model: {}", board_info.getName(), board_info.getMake(), board_info.getModel());
		Logger.debug("GPIOs: {}", board_info.getGpioPins());
		Logger.debug("ADCs: {}", board_info.getAdcPins());
		Logger.debug("DACs: {}", board_info.getDacPins());
		Logger.debug("Headers: {}", board_info.getHeaders());

		int delay = 500;
		try {
			try (LED led = new LED(16, false)) {
				for (int i = 0; i < 2; i++) {
					Logger.debug("On");
					led.on();
					Thread.sleep(delay);
					
					Logger.debug("Off");
					led.off();
					Thread.sleep(delay);
					
					Logger.debug("Toggle");
					led.toggle();
					Thread.sleep(delay);
					
					Logger.debug("Toggle");
					led.toggle();
					Thread.sleep(delay);
				}
			}

			try (PwmLed pwm_led = new PwmLed(16, 0)) {
				pwm_led.pulse(2, 50, 2, false);
			}
			
			delay = 50;
			try (AnalogInputDevice ai = new AnalogInputDevice(17, 3.3f); PwmLed pwm_led = new PwmLed(16, 1)) {
				for (int i=0; i<200; i++) {
					float unscaled = ai.getUnscaledValue();
					pwm_led.setValue(1 - unscaled);
					Thread.sleep(delay);
				}
			}
		} catch (InterruptedException e) {
		} finally {
			DeviceFactoryHelper.getNativeDeviceFactory().close();
		}
	}
}
