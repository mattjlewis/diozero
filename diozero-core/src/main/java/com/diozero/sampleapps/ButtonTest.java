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
import com.diozero.api.GpioPullUpDown;
import com.diozero.util.RuntimeIOException;
import com.diozero.util.SleepUtil;

/**
 * Input test application. To run:
 * <ul>
 * <li>JDK Device I/O 1.0:<br>
 *  {@code sudo java -cp tinylog-1.1.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-jdkdio10-$DIOZERO_VERSION.jar:dio-1.0.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.ButtonTest 25}</li>
 * <li>JDK Device I/O 1.1:<br>
 *  {@code sudo java -cp tinylog-1.1.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-jdkdio11-$DIOZERO_VERSION.jar:dio-1.1-dev-linux-armv6hf.jar -Djava.library.path=. com.diozero.sampleapps.ButtonTest 25}</li>
 * <li>Pi4j:<br>
 *  {@code sudo java -cp tinylog-1.1.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-pi4j-$DIOZERO_VERSION.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.ButtonTest 25}</li>
 * <li>wiringPi:<br>
 *  {@code sudo java -cp tinylog-1.1.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-wiringpi-$DIOZERO_VERSION.jar:pi4j-core-1.1-SNAPSHOT.jar com.diozero.sampleapps.ButtonTest 25}</li>
 * <li>pigpgioJ:<br>
 *  {@code sudo java -cp tinylog-1.1.jar:diozero-core-$DIOZERO_VERSION.jar:diozero-provider-pigpio-$DIOZERO_VERSION.jar:pigpioj-java-1.0.0.jar com.diozero.sampleapps.ButtonTest 25}</li>
 * </ul>
 */
public class ButtonTest {
	public static void main(String[] args) {
		if (args.length < 1) {
			Logger.error("Usage: {} <input-pin>", ButtonTest.class.getName());
			System.exit(1);
		}
		test(Integer.parseInt(args[0]));
	}
	
	public static void test(int inputPin) {
		try (Button button = new Button(inputPin, GpioPullUpDown.PULL_UP)) {
			button.whenPressed(() -> Logger.info("Pressed"));
			button.whenReleased(() -> Logger.info("Released"));
			button.addListener(event -> Logger.info("valueChanged({})", event));
			Logger.debug("Waiting for 10s - *** Press the button connected to input pin " + inputPin + " ***");
			SleepUtil.sleepSeconds(10);
		} catch (RuntimeIOException ioe) {
			Logger.error(ioe, "Error: {}", ioe);
		}
	}
}
