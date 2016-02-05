package com.diozero;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 diozero
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
