package com.diozero.sampleapps.perf;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     GpioPerfTest.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2021 diozero
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

import org.tinylog.Logger;

import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.PinInfo;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.NativeDeviceFactoryInterface;
import com.diozero.sbc.DeviceFactoryHelper;
import com.diozero.util.LibraryLoader;

/**
 * GPIO output performance test application. To run:
 * <ul>
 * <li>built-in:<br>
 * {@code java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar com.diozero.sampleapps.perf.GpioPerfTest 12 100000}</li>
 * <li>pigpgioj:<br>
 * {@code sudo java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-pigpio-$DIOZERO_VERSION.jar:pigpioj-java-2.4.jar com.diozero.sampleapps.perf.GpioPerfTest 12 5000000}</li>
 * <li>mmap:<br>
 * {@code java -cp tinylog-api-$TINYLOG_VERSION.jar:tinylog-impl-$TINYLOG_VERSION.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-sampleapps-$DIOZERO_VERSION.jar:diozero-provider-mmap-$DIOZERO_VERSION.jar com.diozero.sampleapps.perf.GpioPerfTest 12 40000000}</li>
 * </ul>
 *
 * Useful commands:
 *
 * <pre>
 * Generate a JFR recording:
 * java -XX:StartFlightRecording=filename=recording.jfr,maxsize=1g,settings=profile,path-to-gc-roots=true -cp diozero-sampleapps-1.2.0.jar com.diozero.sampleapps.perf.GpioPerfTest 21 10000000
 * Use the experimental GraalVM compiler (add -Dgraal.PrintCompilation=true to see info):
 * java -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:+UseJVMCICompiler -cp diozero-sampleapps-1.2.0.jar com.diozero.sampleapps.perf.GpioPerfTest 21 50000000
 * Use the traditional compiler:
 * java -XX:+UnlockExperimentalVMOptions -XX:+EnableJVMCI -XX:-UseJVMCICompiler -cp diozero-sampleapps-1.2.0.jar com.diozero.sampleapps.perf.GpioPerfTest 21 50000000
 * </pre>
 */
public class GpioPerfTest {
	private static final int ITERATIONS = 100_000;

	public static void main(String[] args) {
		if (args.length < 1) {
			Logger.error("Usage: {} <pin-number> [<iterations>]", GpioPerfTest.class.getName());
			System.exit(1);
		}

		int gpio_num = Integer.parseInt(args[0]);

		int iterations = ITERATIONS;
		if (args.length > 1) {
			iterations = Integer.parseInt(args[1]);
		}

		// Load the diozero system utils library
		LibraryLoader.loadSystemUtils();
		// Initialise diozero
		NativeDeviceFactoryInterface ndfi = DeviceFactoryHelper.getNativeDeviceFactory();
		// Get the PinInfo instance
		PinInfo pin_info = ndfi.getBoardPinInfo().getByGpioNumberOrThrow(gpio_num);
		// Initialise tinylog
		Logger.info("Starting GPIO performance test on SBC {} using GPIO {} with {} iterations; provider: {}",
				ndfi.getBoardInfo().getName(), Integer.valueOf(gpio_num), Integer.valueOf(iterations), ndfi.getName());

		test(pin_info, iterations);
	}

	public static void test(PinInfo pinInfo, int iterations) {
		try (DigitalOutputDevice gpio = DigitalOutputDevice.Builder.builder(pinInfo).build()) {
			boolean on = gpio.isActiveHigh();
			boolean off = !on;
			for (int j = 0; j < 5; j++) {
				long start_ms = System.currentTimeMillis();
				for (int i = 0; i < iterations; i++) {
					gpio.setValue(on);
					gpio.setValue(off);
				}
				long duration_ms = System.currentTimeMillis() - start_ms;
				double frequency = iterations / (duration_ms / 1000.0);

				Logger.info("Duration for {#,###} iterations: {#0.000} s, frequency: {#,###} Hz",
						Integer.valueOf(iterations), Float.valueOf(((float) duration_ms) / 1000),
						Double.valueOf(frequency));
			}
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
		}
	}
}
