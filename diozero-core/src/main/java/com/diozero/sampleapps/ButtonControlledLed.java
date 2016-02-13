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
import com.diozero.api.GpioPullUpDown;
import com.diozero.util.RuntimeIOException;
import com.diozero.util.SleepUtil;

public class ButtonControlledLed {
	public static void main(String[] args) {
		if (args.length < 2) {
			Logger.error("Usage: ButtonControlledLed <button-pin> <led-pin>");
			System.exit(1);
		}
		test(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
	}
	
	public static void test(int buttonPin, int ledPin) {
		try (Button b = new Button(buttonPin, GpioPullUpDown.PULL_UP); LED led = new LED(ledPin)) {
			b.whenPressed(led::on);
			b.whenReleased(led::off);
			
			Logger.info("Waiting for 10s - *** Press the button connected to pin {} ***", Integer.valueOf(buttonPin));
			SleepUtil.sleepSeconds(10);
		} catch (RuntimeIOException e) {
			Logger.error(e, "Error: {}", e);
		}
	}
}
