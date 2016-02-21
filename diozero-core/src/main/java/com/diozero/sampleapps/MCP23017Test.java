package com.diozero.sampleapps;

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

import org.pmw.tinylog.Logger;

import com.diozero.Button;
import com.diozero.LED;
import com.diozero.MCP23017;
import com.diozero.api.GpioPullUpDown;
import com.diozero.util.RuntimeIOException;
import com.diozero.util.SleepUtil;

/**
 * To run (note this hangs the Pi when using wiringPi provider):
 * JDK Device I/O 1.0:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.3-SNAPSHOT.jar:diozero-provider-jdkdio10-0.3-SNAPSHOT.jar:dio-1.0.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.MCP23017Test 21 20 0 1
 * JDK Device I/O 1.1:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.3-SNAPSHOT.jar:diozero-provider-jdkdio11-0.3-SNAPSHOT.jar:dio-1.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.MCP23017Test 21 20 0 1
 * Pi4j:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.3-SNAPSHOT.jar:diozero-provider-pi4j-0.3-SNAPSHOT.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.MCP23017Test 21 20 0 1
 * wiringPi:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.3-SNAPSHOT.jar:diozero-provider-wiringpi-0.3-SNAPSHOT.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.MCP23017Test 21 20 0 1
 * pigpgioJ:
 *  sudo java -cp tinylog-1.0.3.jar:diozero-core-0.3-SNAPSHOT.jar:diozero-provider-pigpio-0.3-SNAPSHOT.jar:pigpioj-java-1.0.0.jar com.diozero.sampleapps.MCP23017Test 21 20 0 1
 */
public class MCP23017Test {
	public static void main(String[] args) {
		if (args.length < 4) {
			Logger.error("Usage: {} <int-a pin> <int-b pin> <mcp23017-input-pin> <mcp23017-output-pin>",
					MCP23017Test.class.getName());
			System.exit(1);
		}
		int int_a_pin = Integer.parseInt(args[0]);
		int int_b_pin = Integer.parseInt(args[1]);
		int input_pin = Integer.parseInt(args[2]);
		int output_pin = Integer.parseInt(args[3]);
		test(int_a_pin, int_b_pin, input_pin, output_pin);
	}
	
	public static void test(int intAPin, int intBPin, int inputPin, int outputPin) {
		try (MCP23017 mcp23017 = new MCP23017(intAPin, intBPin);
				Button button = new Button(mcp23017, inputPin, GpioPullUpDown.PULL_UP);
				LED led = new LED(mcp23017, outputPin)) {
			Logger.debug("On");
			led.on();
			SleepUtil.sleepSeconds(1);

			Logger.debug("Off");
			led.off();
			SleepUtil.sleepSeconds(1);

			Logger.debug("Blink");
			led.blink(0.5f, 0.5f, 10, false);

			button.whenPressed(led::on);
			button.whenReleased(led::off);

			Logger.debug("Waiting for 10s - *** Press the button connected to MCP23017 pin {} ***",
					Integer.valueOf(inputPin));
			SleepUtil.sleepSeconds(10);
			button.whenPressed(null);
			button.whenReleased(null);
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
		}

		Logger.debug("Done");
	}
}
