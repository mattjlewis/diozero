package com.diozero.webapp;

import java.util.HashMap;
import java.util.Map;

import com.diozero.api.DigitalOutputDevice;
import com.diozero.util.BoardInfo;
import com.diozero.util.RuntimeIOException;
import com.diozero.util.SystemInfo;

import spark.*;

public class GpioController {
	private BoardInfo boardInfo;
	private Map<Integer, DigitalOutputDevice> gpios;
	
	public TemplateViewRoute home = (Request request, Response response) -> {
		return buildModelAndView(request);
	};
	
	@SuppressWarnings("resource")
	public TemplateViewRoute control = (Request request, Response response) -> {
		DigitalOutputDevice output = getOutputDevice(request);
		if (output != null) {
			String command = request.params("command");
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
		return buildModelAndView(request, output);
	};
	
	public GpioController() {
		boardInfo = SystemInfo.getBoardInfo();
		gpios = new HashMap<>();
	}

	private ModelAndView buildModelAndView(Request request) {
		return buildModelAndView(request, null);
	}

	private ModelAndView buildModelAndView(Request request, DigitalOutputDevice output) {
		Map<String, Object> model = new HashMap<>();
		model.put("boardInfo", boardInfo);
		model.put("output", output);
		
		return new ModelAndView(model, "gpio.ftl");
	}

	private DigitalOutputDevice getOutputDevice(Request request) {
		String gpio_param = request.params("gpio");
		if (gpio_param == null || gpio_param.trim().length() == 0) {
			return null;
		}
		try {
			Integer gpio = new Integer(gpio_param);
			DigitalOutputDevice output = gpios.get(gpio);
			if (output == null) {
				System.out.println("Creating DigitalOutputDevice for GPIO #" + gpio);
				output = new DigitalOutputDevice(gpio.intValue());
				System.out.println("Created DigitalOutputDevice for GPIO #" + gpio);
				gpios.put(gpio, output);
			}
			return output;
		} catch (NumberFormatException | RuntimeIOException e) {
			System.out.println("Error: " + e);
			return null;
		}
	}
}
