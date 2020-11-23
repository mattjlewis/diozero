package com.diozero.internal.provider.remote.http;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Remote Provider
 * Filename:     JsonHttpProtocolHandler.java  
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

import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.NativeDeviceFactoryInterface;
import com.diozero.remote.http.HttpProviderConstants;
import com.diozero.remote.message.GetBoardInfoRequest;
import com.diozero.remote.message.GetBoardInfoResponse;
import com.diozero.remote.message.GpioAnalogRead;
import com.diozero.remote.message.GpioAnalogReadResponse;
import com.diozero.remote.message.GpioAnalogWrite;
import com.diozero.remote.message.GpioClose;
import com.diozero.remote.message.GpioDigitalRead;
import com.diozero.remote.message.GpioDigitalReadResponse;
import com.diozero.remote.message.GpioDigitalWrite;
import com.diozero.remote.message.GpioEvents;
import com.diozero.remote.message.GpioPwmRead;
import com.diozero.remote.message.GpioPwmReadResponse;
import com.diozero.remote.message.GpioPwmWrite;
import com.diozero.remote.message.I2CBlockProcessCall;
import com.diozero.remote.message.I2CBooleanResponse;
import com.diozero.remote.message.I2CByteResponse;
import com.diozero.remote.message.I2CBytesResponse;
import com.diozero.remote.message.I2CClose;
import com.diozero.remote.message.I2COpen;
import com.diozero.remote.message.I2CProbe;
import com.diozero.remote.message.I2CProcessCall;
import com.diozero.remote.message.I2CReadBlockData;
import com.diozero.remote.message.I2CReadBlockDataResponse;
import com.diozero.remote.message.I2CReadByte;
import com.diozero.remote.message.I2CReadByteData;
import com.diozero.remote.message.I2CReadBytes;
import com.diozero.remote.message.I2CReadI2CBlockData;
import com.diozero.remote.message.I2CReadWordData;
import com.diozero.remote.message.I2CWordResponse;
import com.diozero.remote.message.I2CWriteBlockData;
import com.diozero.remote.message.I2CWriteByte;
import com.diozero.remote.message.I2CWriteByteData;
import com.diozero.remote.message.I2CWriteBytes;
import com.diozero.remote.message.I2CWriteI2CBlockData;
import com.diozero.remote.message.I2CWriteQuick;
import com.diozero.remote.message.I2CWriteWordData;
import com.diozero.remote.message.ProvisionAnalogInputDevice;
import com.diozero.remote.message.ProvisionAnalogOutputDevice;
import com.diozero.remote.message.ProvisionDigitalInputDevice;
import com.diozero.remote.message.ProvisionDigitalInputOutputDevice;
import com.diozero.remote.message.ProvisionDigitalOutputDevice;
import com.diozero.remote.message.ProvisionPwmOutputDevice;
import com.diozero.remote.message.RemoteProtocolInterface;
import com.diozero.remote.message.Response;
import com.diozero.remote.message.SerialBytesAvailable;
import com.diozero.remote.message.SerialBytesAvailableResponse;
import com.diozero.remote.message.SerialClose;
import com.diozero.remote.message.SerialOpen;
import com.diozero.remote.message.SerialRead;
import com.diozero.remote.message.SerialReadByte;
import com.diozero.remote.message.SerialReadByteResponse;
import com.diozero.remote.message.SerialReadBytes;
import com.diozero.remote.message.SerialReadBytesResponse;
import com.diozero.remote.message.SerialReadResponse;
import com.diozero.remote.message.SerialWriteByte;
import com.diozero.remote.message.SerialWriteBytes;
import com.diozero.remote.message.SpiClose;
import com.diozero.remote.message.SpiOpen;
import com.diozero.remote.message.SpiResponse;
import com.diozero.remote.message.SpiWrite;
import com.diozero.remote.message.SpiWriteAndRead;
import com.diozero.util.PropertyUtil;
import com.google.gson.Gson;

public class JsonHttpProtocolHandler implements RemoteProtocolInterface {
	private static final String HOSTNAME_PROP = "HTTP_PROVIDER_HOST";
	private static final String PORT_PROP = "HTTP_PROVIDER_PORT";
	private static final int DEFAULT_PORT = 8080;

	private HttpHost httpHost;
	private HttpClient httpClient;
	private Gson gson;

	public JsonHttpProtocolHandler(NativeDeviceFactoryInterface deviceFactory) {
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
	public void start() {
		// Ignore
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
	public GetBoardInfoResponse request(GetBoardInfoRequest request) {
		return requestResponse(HttpProviderConstants.GET_BOARD_GPIO_INFO, request, GetBoardInfoResponse.class);
	}

	@Override
	public Response request(ProvisionDigitalInputDevice request) {
		return requestResponse(HttpProviderConstants.GPIO_PROVISION_DIGITAL_INPUT_URL, request, Response.class);
	}

	@Override
	public Response request(ProvisionDigitalOutputDevice request) {
		return requestResponse(HttpProviderConstants.GPIO_PROVISION_DIGITAL_OUTPUT_URL, request, Response.class);
	}

	@Override
	public Response request(ProvisionDigitalInputOutputDevice request) {
		return requestResponse(HttpProviderConstants.GPIO_PROVISION_DIGITAL_INPUT_OUTPUT_URL, request, Response.class);
	}

	@Override
	public Response request(ProvisionPwmOutputDevice request) {
		return requestResponse(HttpProviderConstants.GPIO_PROVISION_PWM_OUTPUT_URL, request, Response.class);
	}

	@Override
	public Response request(ProvisionAnalogInputDevice request) {
		return requestResponse(HttpProviderConstants.GPIO_PROVISION_ANALOG_INPUT_URL, request, Response.class);
	}

	@Override
	public Response request(ProvisionAnalogOutputDevice request) {
		return requestResponse(HttpProviderConstants.GPIO_PROVISION_ANALOG_OUTPUT_URL, request, Response.class);
	}

	@Override
	public GpioDigitalReadResponse request(GpioDigitalRead request) {
		return requestResponse(HttpProviderConstants.GPIO_DIGITAL_READ_URL, request, GpioDigitalReadResponse.class);
	}

	@Override
	public Response request(GpioDigitalWrite request) {
		return requestResponse(HttpProviderConstants.GPIO_DIGITAL_WRITE_URL, request, Response.class);
	}

	@Override
	public GpioPwmReadResponse request(GpioPwmRead request) {
		return requestResponse(HttpProviderConstants.GPIO_PWM_READ_URL, request, GpioPwmReadResponse.class);
	}

	@Override
	public Response request(GpioPwmWrite request) {
		return requestResponse(HttpProviderConstants.GPIO_PWM_WRITE_URL, request, Response.class);
	}

	@Override
	public GpioAnalogReadResponse request(GpioAnalogRead request) {
		return requestResponse(HttpProviderConstants.GPIO_ANALOG_READ_URL, request, GpioAnalogReadResponse.class);
	}

	@Override
	public Response request(GpioAnalogWrite request) {
		return requestResponse(HttpProviderConstants.GPIO_ANALOG_WRITE_URL, request, Response.class);
	}

	@Override
	public Response request(GpioEvents request) {
		return requestResponse(HttpProviderConstants.GPIO_EVENTS_URL, request, Response.class);
	}

	@Override
	public Response request(GpioClose request) {
		return requestResponse(HttpProviderConstants.GPIO_CLOSE_URL, request, Response.class);
	}

	@Override
	public Response request(I2COpen request) {
		return requestResponse(HttpProviderConstants.I2C_OPEN_URL, request, Response.class);
	}

	@Override
	public I2CBooleanResponse request(I2CProbe request) {
		return requestResponse(HttpProviderConstants.I2C_PROBE_URL, request, I2CBooleanResponse.class);
	}

	@Override
	public Response request(I2CWriteQuick request) {
		return requestResponse(HttpProviderConstants.I2C_WRITE_QUICK_URL, request, Response.class);
	}

	@Override
	public I2CByteResponse request(I2CReadByte request) {
		return requestResponse(HttpProviderConstants.I2C_READ_BYTE_URL, request, I2CByteResponse.class);
	}

	@Override
	public Response request(I2CWriteByte request) {
		return requestResponse(HttpProviderConstants.I2C_WRITE_BYTE_URL, request, Response.class);
	}

	@Override
	public I2CBytesResponse request(I2CReadBytes request) {
		return requestResponse(HttpProviderConstants.I2C_READ_BYTES_URL, request, I2CBytesResponse.class);
	}

	@Override
	public Response request(I2CWriteBytes request) {
		return requestResponse(HttpProviderConstants.I2C_WRITE_BYTES_URL, request, Response.class);
	}

	@Override
	public I2CByteResponse request(I2CReadByteData request) {
		return requestResponse(HttpProviderConstants.I2C_READ_BYTE_DATA_URL, request, I2CByteResponse.class);
	}

	@Override
	public Response request(I2CWriteByteData request) {
		return requestResponse(HttpProviderConstants.I2C_WRITE_BYTE_DATA_URL, request, Response.class);
	}

	@Override
	public I2CWordResponse request(I2CReadWordData request) {
		return requestResponse(HttpProviderConstants.I2C_READ_WORD_DATA_URL, request, I2CWordResponse.class);
	}

	@Override
	public Response request(I2CWriteWordData request) {
		return requestResponse(HttpProviderConstants.I2C_WRITE_WORD_DATA_URL, request, Response.class);
	}

	@Override
	public I2CWordResponse request(I2CProcessCall request) {
		return requestResponse(HttpProviderConstants.I2C_PROCESS_CALL_URL, request, I2CWordResponse.class);
	}

	@Override
	public I2CReadBlockDataResponse request(I2CReadBlockData request) {
		return requestResponse(HttpProviderConstants.I2C_READ_BLOCK_DATA_URL, request, I2CReadBlockDataResponse.class);
	}

	@Override
	public Response request(I2CWriteBlockData request) {
		return requestResponse(HttpProviderConstants.I2C_WRITE_BLOCK_DATA_URL, request, Response.class);
	}

	@Override
	public I2CBytesResponse request(I2CBlockProcessCall request) {
		return requestResponse(HttpProviderConstants.I2C_BLOCK_PROCESS_CALL_URL, request, I2CBytesResponse.class);
	}

	@Override
	public I2CBytesResponse request(I2CReadI2CBlockData request) {
		return requestResponse(HttpProviderConstants.I2C_READ_I2C_BLOCK_DATA_URL, request, I2CBytesResponse.class);
	}

	@Override
	public Response request(I2CWriteI2CBlockData request) {
		return requestResponse(HttpProviderConstants.I2C_WRITE_I2C_BLOCK_DATA_URL, request, Response.class);
	}

	@Override
	public Response request(I2CClose request) {
		return requestResponse(HttpProviderConstants.I2C_CLOSE_URL, request, Response.class);
	}

	@Override
	public Response request(SpiOpen request) {
		return requestResponse(HttpProviderConstants.SPI_OPEN_URL, request, Response.class);
	}

	@Override
	public Response request(SpiWrite request) {
		return requestResponse(HttpProviderConstants.SPI_WRITE_URL, request, Response.class);
	}

	@Override
	public SpiResponse request(SpiWriteAndRead request) {
		return requestResponse(HttpProviderConstants.SPI_WRITE_AND_READ_URL, request, SpiResponse.class);
	}

	@Override
	public Response request(SpiClose request) {
		return requestResponse(HttpProviderConstants.SPI_CLOSE_URL, request, Response.class);
	}

	@Override
	public Response request(SerialOpen request) {
		return requestResponse(HttpProviderConstants.SERIAL_OPEN_URL, request, Response.class);
	}

	@Override
	public SerialReadResponse request(SerialRead request) {
		return requestResponse(HttpProviderConstants.SERIAL_READ_URL, request, SerialReadResponse.class);
	}

	@Override
	public SerialReadByteResponse request(SerialReadByte request) {
		return requestResponse(HttpProviderConstants.SERIAL_READ_BYTE_URL, request, SerialReadByteResponse.class);
	}

	@Override
	public Response request(SerialWriteByte request) {
		return requestResponse(HttpProviderConstants.SERIAL_WRITE_BYTE_URL, request, Response.class);
	}

	@Override
	public SerialReadBytesResponse request(SerialReadBytes request) {
		return requestResponse(HttpProviderConstants.SERIAL_READ_BYTES_URL, request, SerialReadBytesResponse.class);
	}

	@Override
	public Response request(SerialWriteBytes request) {
		return requestResponse(HttpProviderConstants.SERIAL_WRITE_BYTES_URL, request, Response.class);
	}

	@Override
	public SerialBytesAvailableResponse request(SerialBytesAvailable request) {
		return requestResponse(HttpProviderConstants.SERIAL_BYTES_AVAILABLE_URL, request,
				SerialBytesAvailableResponse.class);
	}

	@Override
	public Response request(SerialClose request) {
		return requestResponse(HttpProviderConstants.SERIAL_CLOSE_URL, request, Response.class);
	}
}
