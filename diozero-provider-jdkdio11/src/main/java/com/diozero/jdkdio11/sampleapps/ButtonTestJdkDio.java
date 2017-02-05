package com.diozero.jdkdio11.sampleapps;

/*
 * #%L
 * Device I/O Zero - JDK Device I/O v1.0 provider
 * %%
 * Copyright (C) 2016 diozero
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

import com.diozero.util.SleepUtil;

import jdk.dio.DeviceConfig;
import jdk.dio.DeviceManager;
import jdk.dio.gpio.*;

public class ButtonTestJdkDio implements PinListener {
	public static void main(String[] args) {
		new ButtonTestJdkDio().test(12);
	}
	
	public void test(int gpio) {
		GPIOPinConfig pin_config = new GPIOPinConfig.Builder().setControllerNumber(DeviceConfig.UNASSIGNED).setPinNumber(gpio).
			setDirection(GPIOPinConfig.DIR_INPUT_ONLY).setDriveMode(GPIOPinConfig.MODE_INPUT_PULL_UP).setTrigger(GPIOPinConfig.TRIGGER_BOTH_EDGES).build();
		try (GPIOPin pin = DeviceManager.open(GPIOPin.class, pin_config)) {
			pin.setInputListener(this);
			System.out.println("Waiting 20s for events..., thread name=" + Thread.currentThread().getName());
			SleepUtil.sleepSeconds(20);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void valueChanged(PinEvent event) {
		System.out.println("valueChanged(" + event.getValue() + "), thread name=" + Thread.currentThread().getName());
	}
}
