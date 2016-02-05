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

import com.diozero.api.DigitalOutputDevice;

public class DigitalMotor implements Closeable {
	private static final Logger logger = LogManager.getLogger(DigitalMotor.class);
	
	private DigitalOutputDevice forward;
	private DigitalOutputDevice backward;
	
	public DigitalMotor(int forwardPin, int backwardPin) throws IOException {
		forward = new DigitalOutputDevice(forwardPin);
		backward = new DigitalOutputDevice(forwardPin);
	}

	@Override
	public void close() {
		logger.debug("close()");
		forward.close();
		backward.close();
	}
	
	// Exposed operations
	public void forward() throws IOException {
		forward.on();
		backward.off();
	}
	
	public void backward() throws IOException {
		forward.off();
		backward.on();
	}
	
	public void stop() throws IOException {
		forward.off();
		backward.off();
	}
	
	public void reverse() throws IOException {
		if (!isActive()) {
			return;
		}
		forward.toggle();
		backward.toggle();
	}
	
	public boolean isActive() throws IOException {
		return forward.isOn() || backward.isOn();
	}
	
	/**
	 * Represents the speed of the motor as a floating point value between -1
	 *   (full speed backward) and 1 (full speed forward)
	 * @throws IOException 
	 */
	public float getValue() throws IOException {
		if (forward.isOn()) {
			return 1;
		} else if (backward.isOn()) {
			return -1;
		}
		
		return 0;
	}
}
