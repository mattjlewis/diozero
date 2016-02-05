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

import com.diozero.LED;
import com.diozero.MCP23017;
import com.diozero.api.*;
import com.diozero.util.SleepUtil;

public class MCP23017Test implements Consumer<DigitalPinEvent> {
	public static void main(String[] args) {
		new MCP23017Test().test();
	}
	
	private LED led;
	
	public MCP23017Test() {
	}
	
	public void test() {
		try (MCP23017 mcp23017 = new MCP23017(21, 20)) {
			try (DigitalInputDevice button = mcp23017.provisionDigitalInputDevice(0, GpioPullUpDown.PULL_UP, GpioEventTrigger.BOTH)) {
				led = new LED(mcp23017.provisionDigitalOutputPin(1, false), true);
				button.setConsumer(this);
				System.out.println("Sleeping for 20s");
				SleepUtil.sleepSeconds(10);
				
				SleepUtil.sleepSeconds(1);
				
				System.out.println("On");
				led.on();
				SleepUtil.sleepSeconds(1);
				
				System.out.println("Off");
				led.off();
				SleepUtil.sleepSeconds(1);
				
				System.out.println("Blink");
				led.blink(0.5f, 0.5f, 10, false);
				
				System.out.println("Done");
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
		System.out.println("accept(" + event + ")");
		if (led != null) {
			try { led.setValue(!event.getValue()); } catch (IOException e) { System.out.println("Error: " + e); }
		}
	}
}
