package com.diozero.internal.provider.remote.http;

/*-
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Remote Provider
 * Filename:     JsonHttpProtocolHandler.java  
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

import java.io.IOException;

import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.diozero.internal.provider.NativeDeviceFactoryInterface;
import com.diozero.internal.provider.remote.devicefactory.ProtocolHandlerInterface;
import com.diozero.remote.http.HttpProviderConstants;
import com.diozero.remote.message.GpioClose;
import com.diozero.remote.message.GpioDigitalRead;
import com.diozero.remote.message.GpioDigitalReadResponse;
import com.diozero.remote.message.GpioDigitalWrite;
import com.diozero.remote.message.GpioEvents;
import com.diozero.remote.message.ProvisionDigitalInputDevice;
import com.diozero.remote.message.ProvisionDigitalInputOutputDevice;
import com.diozero.remote.message.ProvisionDigitalOutputDevice;
import com.diozero.remote.message.ProvisionSpiDevice;
import com.diozero.remote.message.Response;
import com.diozero.remote.message.SpiClose;
import com.diozero.remote.message.SpiResponse;
import com.diozero.remote.message.SpiWrite;
import com.diozero.remote.message.SpiWriteAndRead;
import com.diozero.util.PropertyUtil;
import com.diozero.util.RuntimeIOException;
import com.google.gson.Gson;

public class JsonHttpProtocolHandler implements ProtocolHandlerInterface {
	private static final String HOSTNAME_PROP = "HTTP_PROVIDER_HOST";
	private static final String PORT_PROP = "HTTP_PROVIDER_PORT";
	private static final int DEFAULT_PORT = 8080;
	
	private NativeDeviceFactoryInterface deviceFactory;
	private HttpHost httpHost;
	private HttpClient httpClient;
	private Gson gson;
	
	public JsonHttpProtocolHandler(NativeDeviceFactoryInterface deviceFactory) {
		this.deviceFactory = deviceFactory;

		String hostname = PropertyUtil.getProperty(HOSTNAME_PROP, null);
		if (hostname == null) {
			throw new RuntimeIOException("Property '" + HOSTNAME_PROP + "' must be set");
		}

		int port = PropertyUtil.getIntProperty(PORT_PROP, DEFAULT_PORT);
		
		httpHost = new HttpHost(hostname, port);
		httpClient = HttpClients.createDefault();
		gson = new Gson();
	}

	@Override
	public void close() {
	}
	
	private <T> T requestResponse(String url, Object request, Class<T> responseClass) {
		HttpPost post = new HttpPost(url);
		try {
			post.setEntity(new StringEntity(gson.toJson(request), ContentType.APPLICATION_JSON));
			
			HttpResponse response = httpClient.execute(httpHost, post);
			int status = response.getStatusLine().getStatusCode();
			if (status != HttpStatus.SC_OK) {
				throw new RuntimeIOException("Unexpected response code: " + status);
			}
			
			return gson.fromJson(EntityUtils.toString(response.getEntity()), responseClass);
		} catch (IOException e) {
			throw new RuntimeIOException("HTTP error: " + e, e);
		}
	}

	@Override
	public Response sendRequest(ProvisionDigitalInputDevice request) {
		return requestResponse(HttpProviderConstants.GPIO_PROVISION_INPUT_URL, request, Response.class);
	}

	@Override
	public Response sendRequest(ProvisionDigitalOutputDevice request) {
		return requestResponse(HttpProviderConstants.GPIO_PROVISION_OUTPUT_URL, request, Response.class);
	}

	@Override
	public Response sendRequest(ProvisionDigitalInputOutputDevice request) {
		return requestResponse(HttpProviderConstants.GPIO_PROVISION_INPUT_OUTPUT_URL, request, Response.class);
	}

	@Override
	public GpioDigitalReadResponse sendRequest(GpioDigitalRead request) {
		return requestResponse(HttpProviderConstants.GPIO_READ_URL, request, GpioDigitalReadResponse.class);
	}

	@Override
	public Response sendRequest(GpioDigitalWrite request) {
		return requestResponse(HttpProviderConstants.GPIO_WRITE_URL, request, Response.class);
	}

	@Override
	public Response sendRequest(GpioEvents request) {
		return requestResponse(HttpProviderConstants.GPIO_EVENTS_URL, request, Response.class);
	}

	@Override
	public Response sendRequest(GpioClose request) {
		return requestResponse(HttpProviderConstants.GPIO_CLOSE_URL, request, Response.class);
	}

	@Override
	public Response sendRequest(ProvisionSpiDevice request) {
		System.out.println(request);
		return requestResponse(HttpProviderConstants.SPI_PROVISION_URL, request, Response.class);
	}

	@Override
	public Response sendRequest(SpiWrite request) {
		return requestResponse(HttpProviderConstants.SPI_WRITE_URL, request, Response.class);
	}

	@Override
	public SpiResponse sendRequest(SpiWriteAndRead request) {
		return requestResponse(HttpProviderConstants.SPI_WRITE_AND_READ_URL, request, SpiResponse.class);
	}

	@Override
	public Response sendRequest(SpiClose request) {
		return requestResponse(HttpProviderConstants.SPI_CLOSE_URL, request, Response.class);
	}
}
