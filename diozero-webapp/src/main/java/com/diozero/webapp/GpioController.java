package com.diozero.webapp;

import com.diozero.api.DigitalOutputDevice;

import spark.Request;
import spark.Response;
import spark.TemplateViewRoute;

public class GpioController {
	@SuppressWarnings("resource")
	public TemplateViewRoute control = (Request request, Response response) -> {
		DigitalOutputDevice output = Main.getOutputDevice(request);
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
		return Main.buildModelAndView(request, output, "gpio.ftl");
	};
}
