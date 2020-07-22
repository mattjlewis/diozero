package com.diozero.internal.provider.wiringpi;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - wiringPi provider
 * Filename:     WiringPiRawPerfTest.java  
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


import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.GpioUtil;

public class WiringPiRawPerfTest {
	private static final int DEFAULT_ITERATIONS = 5_000_000;
	
	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Usage: " + WiringPiRawPerfTest.class.getName() + " <pin-number> [<iterations>]");
			System.exit(1);
		}
		
		final int pin = Integer.parseInt(args[0]);
		final int iterations = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_ITERATIONS;
		
		Gpio.wiringPiSetupGpio();
		if (GpioUtil.isExported(pin)) {
			GpioUtil.setDirection(pin, GpioUtil.DIRECTION_OUT);
		} else {
			GpioUtil.export(pin, GpioUtil.DIRECTION_OUT);
		}
		Gpio.pinMode(pin, Gpio.OUTPUT);

		for (int j=0; j<5; j++) {
			long start_nano = System.nanoTime();
			for (int i=0; i<iterations; i++) {
				Gpio.digitalWrite(pin, true);
				Gpio.digitalWrite(pin, false);
			}
			long duration_ns = (System.nanoTime() - start_nano);
			System.out.format("Duration for %d iterations: %.4fs%n",
					Integer.valueOf(iterations), Float.valueOf(((float)duration_ns) / 1000 / 1000 / 1000));
		}
		
		GpioUtil.unexport(pin);
	}
}
