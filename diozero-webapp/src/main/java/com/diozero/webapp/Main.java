package com.diozero.webapp;

/*
 * #%L
 * Device I/O Zero - Web application
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


import java.util.HashMap;
import java.util.Map;

import com.diozero.api.*;
import com.diozero.util.BoardInfo;
import com.diozero.util.DeviceFactoryHelper;
import com.diozero.util.RuntimeIOException;

import spark.*;
import spark.template.freemarker.FreeMarkerEngine;

public class Main {
	private static BoardInfo boardInfo;
	private static TemplateEngine templateEngine;
	private static Map<Integer, DigitalOutputDevice> gpios;
	private static Map<Integer, PwmOutputDevice> pwms;
	
	private static TemplateViewRoute home = (Request request, Response response) -> {
		return buildModelAndView(request, null, "home.ftl");
	};
	
	public static void main(String[] args) {
		boardInfo = DeviceFactoryHelper.getNativeDeviceFactory().getBoardInfo();
		gpios = new HashMap<>();
		pwms = new HashMap<>();
		
		Spark.port(8080);
		Spark.staticFileLocation("/public");
		
		GpioController gpio_controller = new GpioController();
		Spark.get("/home", home, getTemplateEngine());
		Spark.get("/gpio/", gpio_controller.control, getTemplateEngine());
		Spark.get("/gpio/:gpio", gpio_controller.control, getTemplateEngine());
		Spark.get("/gpio/:gpio/:command", gpio_controller.control, getTemplateEngine());
		
		PwmController pwm_controller = new PwmController();
		Spark.get("/pwm", pwm_controller.control, getTemplateEngine());
		Spark.get("/pwm/", pwm_controller.control, getTemplateEngine());
		Spark.get("/pwm/:pwm", pwm_controller.control, getTemplateEngine());
	}

	private static synchronized TemplateEngine getTemplateEngine() {
		if (templateEngine == null) {
			/*
			VelocityEngine configured_engine = new VelocityEngine();
			configured_engine.setProperty("runtime.references.strict", Boolean.TRUE);
			configured_engine.setProperty("resource.loader", "class");
			configured_engine.setProperty("class.resource.loader.class",
					"org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");

			templateEngine = new VelocityTemplateEngine(configured_engine);
			 */
			//templateEngine = new VelocityTemplateEngine();
			templateEngine = new FreeMarkerEngine();
		}
		
		return templateEngine;
	}

	static ModelAndView buildModelAndView(Request request, OutputDeviceInterface output, String viewName) {
		Map<String, Object> model = new HashMap<>();
		model.put("boardInfo", boardInfo);
		if (output != null) {
			model.put("output", output);
		}
		
		return new ModelAndView(model, viewName);
	}

	static DigitalOutputDevice getOutputDevice(Request request) {
		String gpio_param = request.params("gpio");
		if (gpio_param == null || gpio_param.trim().length() == 0) {
			return null;
		}
		
		try {
			Integer gpio = Integer.valueOf(gpio_param);
			
			DigitalOutputDevice output = gpios.get(gpio);
			if (output == null) {
				PinInfo pin_info = boardInfo.getByGpioNumber(gpio.intValue());
				if (pin_info == null || ! pin_info.getModes().contains(DeviceMode.DIGITAL_OUTPUT)) {
					System.err.println("Error: Invalid GPIO #" + gpio);
					return null;
				}
				
				System.out.println("Creating DigitalOutputDevice for GPIO #" + gpio);
				output = new DigitalOutputDevice(gpio.intValue());
				gpios.put(gpio, output);
			}
			
			return output;
		} catch (NumberFormatException | RuntimeIOException e) {
			System.err.println("Error: " + e);
			return null;
		}
	}

	static PwmOutputDevice getPwmDevice(Request request) {
		String pwm_param = request.params("pwm");
		if (pwm_param == null || pwm_param.trim().length() == 0) {
			return null;
		}
		
		try {
			Integer pwm = Integer.valueOf(pwm_param);
			
			PwmOutputDevice output = pwms.get(pwm);
			if (output == null) {
				PinInfo pin_info = boardInfo.getByPwmNumber(pwm.intValue());
				if (pin_info == null || ! pin_info.getModes().contains(DeviceMode.PWM_OUTPUT)) {
					System.err.println("Error: Invalid PWM #" + pwm);
					return null;
				}
				
				System.out.println("Creating PwmOutputDevice for PWM #" + pwm);
				output = new PwmOutputDevice(pwm.intValue());
				pwms.put(pwm, output);
			}
			
			return output;
		} catch (NumberFormatException | RuntimeIOException e) {
			System.err.println("Error: " + e);
			e.printStackTrace(System.err);
			return null;
		}
	}
}
