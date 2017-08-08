package com.diozero.remote.message.test;

/*-
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - MQTT Server Host Process
 * Filename:     GsonTest.java  
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

import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.remote.message.ProvisionDigitalInputDevice;
import com.diozero.remote.message.SpiClose;
import com.google.gson.Gson;

public class GsonTest {
	public static void main(String[] args) {
		Gson gson = new Gson();
		
		SpiClose spi_close = new SpiClose(1, 0);
		System.out.println(spi_close);
		
		String json = gson.toJson(spi_close);
		System.out.println(json);
		
		spi_close = gson.fromJson(json, SpiClose.class);
		System.out.println(spi_close);
		
		ProvisionDigitalInputDevice gpio_input = new ProvisionDigitalInputDevice(22, GpioPullUpDown.PULL_DOWN, GpioEventTrigger.RISING);
		
		json = gson.toJson(gpio_input);
		System.out.println(json);
		
		gpio_input = gson.fromJson(json, ProvisionDigitalInputDevice.class);
		System.out.println(gpio_input);
	}
}
