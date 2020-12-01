package com.diozero.remote.server.http;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Remote Server
 * Filename:     DiozeroController.java  
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

import static spark.Spark.post;

import com.diozero.api.DigitalInputEvent;
import com.diozero.remote.http.HttpProviderConstants;
import com.diozero.remote.message.GpioAnalogRead;
import com.diozero.remote.message.GpioAnalogWrite;
import com.diozero.remote.message.GpioClose;
import com.diozero.remote.message.GpioDigitalRead;
import com.diozero.remote.message.GpioDigitalWrite;
import com.diozero.remote.message.GpioEvents;
import com.diozero.remote.message.GpioPwmRead;
import com.diozero.remote.message.GpioPwmWrite;
import com.diozero.remote.message.I2CBlockProcessCall;
import com.diozero.remote.message.I2CClose;
import com.diozero.remote.message.I2COpen;
import com.diozero.remote.message.I2CProbe;
import com.diozero.remote.message.I2CProcessCall;
import com.diozero.remote.message.I2CReadBlockData;
import com.diozero.remote.message.I2CReadByte;
import com.diozero.remote.message.I2CReadByteData;
import com.diozero.remote.message.I2CReadBytes;
import com.diozero.remote.message.I2CReadI2CBlockData;
import com.diozero.remote.message.I2CReadWordData;
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
import com.diozero.remote.message.Response;
import com.diozero.remote.message.SerialBytesAvailable;
import com.diozero.remote.message.SerialClose;
import com.diozero.remote.message.SerialOpen;
import com.diozero.remote.message.SerialRead;
import com.diozero.remote.message.SerialReadByte;
import com.diozero.remote.message.SerialReadBytes;
import com.diozero.remote.message.SerialWriteByte;
import com.diozero.remote.message.SerialWriteBytes;
import com.diozero.remote.message.SpiClose;
import com.diozero.remote.message.SpiOpen;
import com.diozero.remote.message.SpiWrite;
import com.diozero.remote.message.SpiWriteAndRead;
import com.diozero.remote.server.BaseRemoteServer;
import com.google.gson.Gson;

public class DiozeroController extends BaseRemoteServer {
	private Gson gson = new Gson();
	
	public DiozeroController() {
	}

	@Override
	public void start() {
		// GPIO
		post(HttpProviderConstants.GPIO_PROVISION_DIGITAL_INPUT_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), ProvisionDigitalInputDevice.class));
		}, gson::toJson);
		post(HttpProviderConstants.GPIO_PROVISION_DIGITAL_OUTPUT_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), ProvisionDigitalOutputDevice.class));
		}, gson::toJson);
		post(HttpProviderConstants.GPIO_PROVISION_DIGITAL_INPUT_OUTPUT_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), ProvisionDigitalInputOutputDevice.class));
		}, gson::toJson);
		post(HttpProviderConstants.GPIO_PROVISION_PWM_OUTPUT_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), ProvisionPwmOutputDevice.class));
		}, gson::toJson);
		post(HttpProviderConstants.GPIO_PROVISION_ANALOG_INPUT_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), ProvisionAnalogInputDevice.class));
		}, gson::toJson);
		post(HttpProviderConstants.GPIO_PROVISION_ANALOG_OUTPUT_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), ProvisionAnalogOutputDevice.class));
		}, gson::toJson);
		post(HttpProviderConstants.GPIO_DIGITAL_READ_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), GpioDigitalRead.class));
		}, gson::toJson);
		post(HttpProviderConstants.GPIO_DIGITAL_WRITE_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), GpioDigitalWrite.class));
		}, gson::toJson);
		post(HttpProviderConstants.GPIO_EVENTS_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), GpioEvents.class));
		}, gson::toJson);
		post(HttpProviderConstants.GPIO_PWM_READ_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), GpioPwmRead.class));
		}, gson::toJson);
		post(HttpProviderConstants.GPIO_PWM_WRITE_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), GpioPwmWrite.class));
		}, gson::toJson);
		post(HttpProviderConstants.GPIO_ANALOG_READ_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), GpioAnalogRead.class));
		}, gson::toJson);
		post(HttpProviderConstants.GPIO_ANALOG_WRITE_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), GpioAnalogWrite.class));
		}, gson::toJson);
		post(HttpProviderConstants.GPIO_CLOSE_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), GpioClose.class));
		}, gson::toJson);

		// I2C
		post(HttpProviderConstants.I2C_OPEN_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), I2COpen.class));
		}, gson::toJson);
		post(HttpProviderConstants.I2C_PROBE_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), I2CProbe.class));
		}, gson::toJson);
		post(HttpProviderConstants.I2C_WRITE_QUICK_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), I2CWriteQuick.class));
		}, gson::toJson);
		post(HttpProviderConstants.I2C_READ_BYTE_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), I2CReadByte.class));
		}, gson::toJson);
		post(HttpProviderConstants.I2C_WRITE_BYTE_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), I2CWriteByte.class));
		}, gson::toJson);
		post(HttpProviderConstants.I2C_READ_BYTES_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), I2CReadBytes.class));
		}, gson::toJson);
		post(HttpProviderConstants.I2C_WRITE_BYTES_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), I2CWriteBytes.class));
		}, gson::toJson);
		post(HttpProviderConstants.I2C_READ_BYTE_DATA_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), I2CReadByteData.class));
		}, gson::toJson);
		post(HttpProviderConstants.I2C_WRITE_BYTE_DATA_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), I2CWriteByteData.class));
		}, gson::toJson);
		post(HttpProviderConstants.I2C_READ_WORD_DATA_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), I2CReadWordData.class));
		}, gson::toJson);
		post(HttpProviderConstants.I2C_WRITE_WORD_DATA_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), I2CWriteWordData.class));
		}, gson::toJson);
		post(HttpProviderConstants.I2C_PROCESS_CALL_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), I2CProcessCall.class));
		}, gson::toJson);
		post(HttpProviderConstants.I2C_READ_BLOCK_DATA_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), I2CReadBlockData.class));
		}, gson::toJson);
		post(HttpProviderConstants.I2C_WRITE_BLOCK_DATA_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), I2CWriteBlockData.class));
		}, gson::toJson);
		post(HttpProviderConstants.I2C_BLOCK_PROCESS_CALL_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), I2CBlockProcessCall.class));
		}, gson::toJson);
		post(HttpProviderConstants.I2C_READ_I2C_BLOCK_DATA_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), I2CReadI2CBlockData.class));
		}, gson::toJson);
		post(HttpProviderConstants.I2C_WRITE_I2C_BLOCK_DATA_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), I2CWriteI2CBlockData.class));
		}, gson::toJson);
		post(HttpProviderConstants.I2C_CLOSE_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), I2CClose.class));
		}, gson::toJson);

		// SPI
		post(HttpProviderConstants.SPI_OPEN_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), SpiOpen.class));
		}, gson::toJson);
		post(HttpProviderConstants.SPI_WRITE_URL, (req, res) -> {
			long start = System.currentTimeMillis();
			Response response = request(gson.fromJson(req.body(), SpiWrite.class));
			long duration = System.currentTimeMillis() - start;
			System.out.println("Took: " + duration + " ms");
			return response;
		}, gson::toJson);
		post(HttpProviderConstants.SPI_WRITE_AND_READ_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), SpiWriteAndRead.class));
		}, gson::toJson);
		post(HttpProviderConstants.SPI_CLOSE_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), SpiClose.class));
		}, gson::toJson);
		
		// Serial
		post(HttpProviderConstants.SERIAL_OPEN_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), SerialOpen.class));
		}, gson::toJson);
		post(HttpProviderConstants.SERIAL_READ_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), SerialRead.class));
		}, gson::toJson);
		post(HttpProviderConstants.SERIAL_READ_BYTE_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), SerialReadByte.class));
		}, gson::toJson);
		post(HttpProviderConstants.SERIAL_WRITE_BYTE_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), SerialWriteByte.class));
		}, gson::toJson);
		post(HttpProviderConstants.SERIAL_READ_BYTES_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), SerialReadBytes.class));
		}, gson::toJson);
		post(HttpProviderConstants.SERIAL_WRITE_BYTES_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), SerialWriteBytes.class));
		}, gson::toJson);
		post(HttpProviderConstants.SERIAL_BYTES_AVAILABLE_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), SerialBytesAvailable.class));
		}, gson::toJson);
		post(HttpProviderConstants.SERIAL_CLOSE_URL, (req, res) -> {
			return request(gson.fromJson(req.body(), SerialClose.class));
		}, gson::toJson);
	}

	@Override
	public void accept(DigitalInputEvent event) {
		// TODO Auto-generated method stub
	}
}
