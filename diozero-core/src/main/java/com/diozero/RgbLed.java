package com.diozero;

import java.io.Closeable;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RgbLed implements Closeable {
	private static final Logger logger = LogManager.getLogger(RgbLed.class);
	
	private LED redLED;
	private LED greenLED;
	private LED blueLED;
	
	public RgbLed(int redPin, int greenPin, int bluePin) throws IOException {
		redLED = new LED(redPin);
		greenLED = new LED(greenPin);
		blueLED = new LED(bluePin);
	}
	
	@Override
	public void close() {
		logger.debug("close()");
		redLED.close();
		greenLED.close();
		blueLED.close();
	}
	
	// Exposed operations
	public void on() throws IOException {
		redLED.on();
		greenLED.on();
		blueLED.on();
	}
	
	public void off() throws IOException {
		redLED.off();
		greenLED.off();
		blueLED.off();
	}
	
	public void toggle() throws IOException {
		redLED.toggle();
		greenLED.toggle();
		blueLED.toggle();
	}
}
