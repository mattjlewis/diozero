package com.diozero.firmata;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Remote Provider
 * Filename:     FirmataAdapterTestApp.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
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

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.List;

import com.diozero.firmata.FirmataAdapter.FirmwareDetails;
import com.diozero.firmata.FirmataAdapter.PinState;
import com.diozero.firmata.FirmataAdapter.ProtocolVersion;
import com.diozero.firmata.FirmataEventListener.EventType;
import com.diozero.firmata.FirmataProtocol.PinCapability;
import com.diozero.firmata.FirmataProtocol.PinMode;

public class FirmataAdapterTestApp {
	public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException {
		try (FirmataAdapter fa = new SocketFirmataAdapter(FirmataAdapterTestApp::event, "192.168.1.215", 3030)) {
			test(fa);
		}
		
		System.out.println("main: done");
	}
	
	private static void event(EventType eventType, int gpio, int value, long epochTime, long nanoTime) {
		System.out.println(
				"Event: " + eventType + ", #" + gpio + ": " + value + " @ " + epochTime + "ms (" + nanoTime + "ns)");
	}

	@SuppressWarnings("boxing")
	private static void test(FirmataAdapter fa) throws InterruptedException {
		ProtocolVersion version = fa.getProtocolVersion();
		System.out.format("Protocol version: %d.%d%n", version.getMajor(), version.getMinor());
		FirmwareDetails firmware = fa.getFirmware();
		System.out.format("Firmware: v%d.%d '%s'%n", firmware.getMajor(), firmware.getMinor(), firmware.getName());
		List<List<PinCapability>> board_capabilities = fa.getBoardCapabilities();
		System.out.println("Board Capabilities: " + board_capabilities);
		
		byte gpio = 16;
		int delay = 500;
		
		fa.setPinMode(gpio, PinMode.DIGITAL_OUTPUT);
		Thread.sleep(delay);
		fa.setPinMode(gpio, PinMode.DIGITAL_INPUT);
		Thread.sleep(delay);
		fa.setPinMode(gpio, PinMode.DIGITAL_OUTPUT);
		Thread.sleep(delay);
		fa.setPinMode(gpio, PinMode.PWM);
		Thread.sleep(delay);
		fa.setPinMode(gpio, PinMode.SERVO);
		Thread.sleep(delay);
		fa.setPinMode(gpio, PinMode.DIGITAL_OUTPUT);
		Thread.sleep(delay);
		
		// Set value low (on as active high)
		fa.setDigitalValue(gpio, false);
		Thread.sleep(delay);
		PinState state = fa.getPinState(gpio);
		System.out.println("State: " + state);
		System.out.println("Value: " + fa.getValue(gpio));
		
		// Set value high (off as active high)
		fa.setDigitalValue(gpio, true);
		Thread.sleep(delay);
		state = fa.getPinState(gpio);
		System.out.println("State: " + state);
		System.out.println("Value: " + fa.getValue(gpio));
		
		// Set value low (on as active high)
		fa.setDigitalValue(gpio, false);
		Thread.sleep(delay);
		state = fa.getPinState(gpio);
		System.out.println("State: " + state);
		System.out.println("Value: " + fa.getValue(gpio));
		
		// Enable reporting for gpio
		fa.enableDigitalReporting(gpio, true);
		Thread.sleep(delay);
		
		// Disable reporting for gpio
		fa.enableDigitalReporting(gpio, false);
		Thread.sleep(delay);
		
		// Enable reporting for gpio
		fa.enableDigitalReporting(gpio, true);
		Thread.sleep(delay);
		
		// Toggle gpio low/high
		for (int i=0; i<5; i++) {
			// Set value low
			fa.setDigitalValue(gpio, false);
			System.out.println("Value: " + fa.getValue(gpio));
			Thread.sleep(delay);
			
			// Set value high
			fa.setDigitalValue(gpio, true);
			System.out.println("Value: " + fa.getValue(gpio));
			Thread.sleep(delay);
		}
		
		// Disable reporting for gpio
		fa.enableDigitalReporting(gpio, false);
		
		gpio = 16;
		fa.enableDigitalReporting(gpio, true);
		Thread.sleep(delay);
		
		fa.setPinMode(gpio, PinMode.PWM);
		fa.setValue(gpio, 0);
		Thread.sleep(delay);
		System.out.println(fa.getPinState(gpio));
		Thread.sleep(delay);

		fa.setValue(gpio, (int) Math.pow(2, 10)-1);
		Thread.sleep(delay);
		System.out.println(fa.getPinState(gpio));
		Thread.sleep(delay);
		
		delay = 20;
		int step = 5;
		for (int i=0; i<(int) Math.pow(2, 10)-1; i+=step) {
			fa.setValue(gpio, i);
			Thread.sleep(delay);
		}
		for (int i=(int) Math.pow(2, 10)-1; i>0; i-=step) {
			fa.setValue(gpio, i);
			Thread.sleep(delay);
		}
		
		fa.setPinMode(gpio, PinMode.DIGITAL_INPUT);
		fa.enableDigitalReporting(gpio, false);
		
		gpio = 17;
		delay = 100;
		fa.setPinMode(gpio, PinMode.ANALOG_INPUT);
		for (int i=0; i<100; i++) {
			System.out.println("Analog value: " + fa.getValue(gpio));
			Thread.sleep(delay);
		}
		
		/*
		byte[] rows = {5, 4, 2, 14};
		byte[] cols = {12, 13, 15, 3};
		for (int i=0; i<rows.length; i++) {
			// Set mode to digital input
			fa.setPinMode(rows[i], PinMode.INPUT_PULLUP);
			Thread.sleep(500);
			// Enable reporting
			fa.enableReporting(rows[i], true);
			Thread.sleep(500);
		}
		for (int i=0; i<cols.length; i++) {
			// Set mode to digital input
			fa.setPinMode(cols[i], PinMode.DIGITAL_INPUT);
			Thread.sleep(500);
			// Enable reporting
			fa.enableReporting(cols[i], true);
			Thread.sleep(500);
		}
		*/
		
		System.out.println("Waiting for 10s...");
		Thread.sleep(10_000);
	}
}
