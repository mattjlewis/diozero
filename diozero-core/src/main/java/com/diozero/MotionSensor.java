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


import java.io.IOException;

import com.diozero.api.GpioPullUpDown;
import com.diozero.api.SmoothedInputDevice;

/**
 * A PIR (Passive Infra-Red) motion sensor.
 * 
 * A typical PIR device has a small circuit board with three pins: VCC, OUT, and
 * GND. VCC should be connected to the Pi's +5V pin, GND to one of the Pi's
 * ground pins, and finally OUT to the GPIO specified as the value of the 'pin'
 * parameter in the constructor.
 * 
 * This class defaults 'queue_len' to 1, effectively removing the averaging of
 * the internal queue. If your PIR sensor has a short fall time and is
 * particularly "jittery" you may wish to set this to a higher value (e.g. 5) to
 * mitigate this.
 */
public class MotionSensor extends SmoothedInputDevice {
	public MotionSensor(int pinNumber) throws IOException {
		this(pinNumber, 0.5f, 1, 10, false);
	}
	
	public MotionSensor(int pinNumber, float threshold, int queueLen, int sampleRate, boolean partial)
			throws IOException {
		super(pinNumber, GpioPullUpDown.NONE, threshold, queueLen, 1f / sampleRate, partial);
	}

	// TODO Implementation
}
