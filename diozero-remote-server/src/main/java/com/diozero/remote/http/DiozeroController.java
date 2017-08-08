package com.diozero.remote.http;

/*-
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - MQTT Server Host Process
 * Filename:     DiozeroController.java  
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

import static spark.Spark.post;

import com.diozero.api.DigitalInputEvent;
import com.diozero.remote.BaseRemoteServer;
import com.diozero.remote.message.GpioClose;
import com.diozero.remote.message.GpioDigitalRead;
import com.diozero.remote.message.GpioDigitalWrite;
import com.diozero.remote.message.GpioEvents;
import com.diozero.remote.message.ProvisionDigitalInputDevice;
import com.diozero.remote.message.ProvisionDigitalInputOutputDevice;
import com.diozero.remote.message.ProvisionDigitalOutputDevice;
import com.diozero.remote.message.ProvisionSpiDevice;
import com.diozero.remote.message.Response;
import com.diozero.remote.message.SpiClose;
import com.diozero.remote.message.SpiWrite;
import com.diozero.remote.message.SpiWriteAndRead;
import com.google.gson.Gson;

public class DiozeroController extends BaseRemoteServer {
	private Gson gson = new Gson();
	
	public DiozeroController() {
	}

	public void init() {
		post(HttpProviderConstants.GPIO_PROVISION_INPUT_URL, (req, res) -> {
			return processRequest(gson.fromJson(req.body(), ProvisionDigitalInputDevice.class));
		}, gson::toJson);
		post(HttpProviderConstants.GPIO_PROVISION_OUTPUT_URL, (req, res) -> {
			return processRequest(gson.fromJson(req.body(), ProvisionDigitalOutputDevice.class));
		}, gson::toJson);
		post(HttpProviderConstants.GPIO_PROVISION_INPUT_OUTPUT_URL, (req, res) -> {
			return processRequest(gson.fromJson(req.body(), ProvisionDigitalInputOutputDevice.class));
		}, gson::toJson);
		post(HttpProviderConstants.GPIO_READ_URL, (req, res) -> {
			return processRequest(gson.fromJson(req.body(), GpioDigitalRead.class));
		}, gson::toJson);
		post(HttpProviderConstants.GPIO_WRITE_URL, (req, res) -> {
			return processRequest(gson.fromJson(req.body(), GpioDigitalWrite.class));
		}, gson::toJson);
		post(HttpProviderConstants.GPIO_EVENTS_URL, (req, res) -> {
			return processRequest(gson.fromJson(req.body(), GpioEvents.class));
		}, gson::toJson);
		post(HttpProviderConstants.GPIO_CLOSE_URL, (req, res) -> {
			return processRequest(gson.fromJson(req.body(), GpioClose.class));
		}, gson::toJson);
		
		post(HttpProviderConstants.SPI_PROVISION_URL, (req, res) -> {
			return processRequest(gson.fromJson(req.body(), ProvisionSpiDevice.class));
		}, gson::toJson);
		post(HttpProviderConstants.SPI_WRITE_URL, (req, res) -> {
			long start = System.currentTimeMillis();
			Response response = processRequest(gson.fromJson(req.body(), SpiWrite.class));
			long duration = System.currentTimeMillis() - start;
			System.out.println("Took: " + duration + " ms");
			return response;
		}, gson::toJson);
		post(HttpProviderConstants.SPI_WRITE_AND_READ_URL, (req, res) -> {
			return processRequest(gson.fromJson(req.body(), SpiWriteAndRead.class));
		}, gson::toJson);
		post(HttpProviderConstants.SPI_CLOSE_URL, (req, res) -> {
			return processRequest(gson.fromJson(req.body(), SpiClose.class));
		}, gson::toJson);
	}

	@Override
	public void valueChanged(DigitalInputEvent event) {
		// TODO Auto-generated method stub
	}
}
