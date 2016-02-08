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


import java.io.IOException;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.LED;
import com.diozero.MCP23017;
import com.diozero.api.*;
import com.diozero.util.SleepUtil;

/**
 * To run (note this hangs the Pi when using wiringPi provider):
 * JDK Device I/O 1.0:
 *  sudo java -cp log4j-api-2.5.jar:log4j-core-2.5.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-jdkdio10-0.2-SNAPSHOT.jar:dio-1.0.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.MCP23017Test 21 20
 * JDK Device I/O 1.1:
 *  sudo java -cp log4j-api-2.5.jar:log4j-core-2.5.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-jdkdio11-0.2-SNAPSHOT.jar:dio-1.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.MCP23017Test 21 20
 * Pi4j:
 *  sudo java -cp log4j-api-2.5.jar:log4j-core-2.5.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-pi4j-0.2-SNAPSHOT.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.MCP23017Test 21 20
 * wiringPi:
 *  sudo java -cp log4j-api-2.5.jar:log4j-core-2.5.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-wiringpi-0.2-SNAPSHOT.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.MCP23017Test 21 20
 * pigpgioJ:
 *  sudo java -cp log4j-api-2.5.jar:log4j-core-2.5.jar:diozero-core-0.2-SNAPSHOT.jar:diozero-provider-pigpio-0.2-SNAPSHOT.jar:pigpioj-java-0.0.1-SNAPSHOT.jar -Djava.library.path=. com.diozero.sampleapps.MCP23017Test 21 20
 */
public class MCP23017Test implements Consumer<DigitalPinEvent> {
	private static final Logger logger = LogManager.getLogger(MCP23017Test.class);
	
	public static void main(String[] args) {
		if (args.length < 2) {
			logger.error("Usage: MCP23017Test <int-a pin> <int-b pin>");
			System.exit(1);
		}
		int int_a_pin = Integer.parseInt(args[0]);
		int int_b_pin = Integer.parseInt(args[1]);
		new MCP23017Test().test(int_a_pin, int_b_pin);
	}
	
	private LED led;
	
	public MCP23017Test() {
	}
	
	public void test(int intAPin, int intBPin) {
		try (MCP23017 mcp23017 = new MCP23017(intAPin, intBPin)) {
			try (DigitalInputDevice button = mcp23017.provisionDigitalInputDevice(0, GpioPullUpDown.PULL_UP, GpioEventTrigger.BOTH)) {
				led = new LED(mcp23017.provisionDigitalOutputPin(1, false), true);
				button.setConsumer(this);
				logger.debug("Sleeping for 20s");
				SleepUtil.sleepSeconds(10);
				
				SleepUtil.sleepSeconds(1);
				
				logger.debug("On");
				led.on();
				SleepUtil.sleepSeconds(1);
				
				logger.debug("Off");
				led.off();
				SleepUtil.sleepSeconds(1);
				
				logger.debug("Blink");
				led.blink(0.5f, 0.5f, 10, false);
				
				logger.debug("Done");
			} finally {
				if (led != null) { led.close(); }
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void accept(DigitalPinEvent event) {
		logger.debug("accept(" + event + ")");
		if (led != null) {
			try { led.setValue(!event.getValue()); } catch (IOException e) { logger.error("Error: " + e, e); }
		}
	}
}
