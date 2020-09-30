package com.diozero.internal.provider.remote;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - MQTT Provider
 * Filename:     BaseAsyncProtocolHandler.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.tinylog.Logger;

import com.diozero.api.DigitalInputEvent;
import com.diozero.api.PinInfo;
import com.diozero.internal.provider.NativeDeviceFactoryInterface;
import com.diozero.internal.provider.remote.devicefactory.RemoteDigitalInputDevice;
import com.diozero.remote.message.RemoteProtocolInterface;
import com.diozero.remote.message.Response;

public abstract class BaseAsyncProtocolHandler implements RemoteProtocolInterface {
	private NativeDeviceFactoryInterface deviceFactory;
	protected Lock lock;
	protected Map<String, Condition> conditions;
	protected Map<String, Response> responses;

	public BaseAsyncProtocolHandler(NativeDeviceFactoryInterface deviceFactory) {
		this.deviceFactory = deviceFactory;

		lock = new ReentrantLock();
		conditions = new HashMap<>();
		responses = new HashMap<>();
	}

	protected void processResponse(Response response) {
		responses.put(response.getCorrelationId(), response);

		Condition condition = conditions.remove(response.getCorrelationId());
		if (condition == null) {
			Logger.error("No condition for correlation id {}", response.getCorrelationId());
		} else {
			lock.lock();
			try {
				condition.signalAll();
			} finally {
				lock.unlock();
			}
		}
	}

	protected void processEvent(DigitalInputEvent event) {
		// Locate the input device for this GPIO
		PinInfo pin_info = deviceFactory.getBoardPinInfo().getByGpioNumber(event.getGpio());
		if (pin_info == null) {
			Logger.error("PinInfo not found for GPIO " + event.getGpio());
			return;
		}

		@SuppressWarnings("resource")
		RemoteDigitalInputDevice input_device = deviceFactory.getDevice(deviceFactory.createPinKey(pin_info),
				RemoteDigitalInputDevice.class);
		if (input_device == null) {
			Logger.error("Digital input device not found for GPIO " + event.getGpio());
			return;
		}

		input_device.valueChanged(event);
	}
}
