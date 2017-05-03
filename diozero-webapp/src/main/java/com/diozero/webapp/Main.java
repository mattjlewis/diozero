package com.diozero.webapp;

import spark.Spark;
import spark.TemplateEngine;
import spark.template.freemarker.FreeMarkerEngine;

public class Main {
	private static TemplateEngine templateEngine;
	
	public static void main(String[] args) {
		Spark.port(8080);
		Spark.staticFileLocation("/public");
		
		GpioController gpio_controller = new GpioController();
		
		Spark.get("/gpio", gpio_controller.control, getTemplateEngine());
		Spark.get("/gpio/:command/:gpio", gpio_controller.control, getTemplateEngine());
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
}
