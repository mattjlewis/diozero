package com.diozero.internal.provider.remote;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - MQTT Provider
 * Filename:     ProtobufBaseAsyncProtocolHandler.java  
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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

import org.pmw.tinylog.Logger;

import com.diozero.internal.provider.NativeDeviceFactoryInterface;
import com.diozero.remote.message.Response;
import com.diozero.util.RuntimeIOException;
import com.google.protobuf.GeneratedMessageV3;

public abstract class ProtobufBaseAsyncProtocolHandler extends BaseAsyncProtocolHandler {
	private static final long TIMEOUT_MS = 1000;

	protected abstract void sendMessage(String url, GeneratedMessageV3 message) throws Exception;

	public ProtobufBaseAsyncProtocolHandler(NativeDeviceFactoryInterface deviceFactory) {
		super(deviceFactory);
	}

	protected Response requestResponse(String url, GeneratedMessageV3 request, String correlationId) {
		Condition condition = lock.newCondition();
		conditions.put(correlationId, condition);

		lock.lock();
		try {
			sendMessage(url, request);

			condition.await(TIMEOUT_MS, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			Logger.warn(e, "Interrupted: {}", e);
		} catch (Exception e) {
			throw new RuntimeIOException(e);
		}

		Response response = responses.remove(correlationId);
		if (response == null) {
			throw new RuntimeIOException("Cannot find response message for " + correlationId);
		}

		return response;
	}
}
