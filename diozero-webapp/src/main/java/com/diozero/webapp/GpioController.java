package com.diozero.webapp;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Web application
 * Filename:     GpioController.java  
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

import com.diozero.api.DigitalOutputDevice;

import spark.Request;
import spark.Response;
import spark.TemplateViewRoute;

public class GpioController {
	public TemplateViewRoute control = (Request request, Response response) -> {
		DigitalOutputDevice output = Main.getOutputDevice(request);
		if (output != null) {
			String command = request.params("command");
			if (command != null) {
				switch (command) {
				case "on":
					output.on();
					break;
				case "off":
					output.off();
					break;
				case "toggle":
					output.toggle();
					break;
				default:
					System.out.println("Invalid command '" + command + "'");
				}
			}
		}
		return Main.buildModelAndView(request, output, "gpio.ftl");
	};
}
