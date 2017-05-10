package com.diozero.webapp;

import com.diozero.api.PwmOutputDevice;

import spark.Request;
import spark.Response;
import spark.TemplateViewRoute;

public class PwmController {
	
	@SuppressWarnings("resource")
	public TemplateViewRoute control = (Request request, Response response) -> {
		PwmOutputDevice output = Main.getPwmDevice(request);
		if (output != null) {
			try {
				output.setValue(Float.parseFloat(request.queryParams("val")));
			} catch (NumberFormatException e) {
				System.err.println("Error: " + e);
			}
		}
		return Main.buildModelAndView(request, output, "pwm.ftl");
	};
}
