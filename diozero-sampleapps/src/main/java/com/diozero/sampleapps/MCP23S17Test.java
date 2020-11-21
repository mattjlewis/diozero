package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Sample applications
 * Filename:     MCP23S17Test.java  
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

import org.tinylog.Logger;

import com.diozero.api.GpioPullUpDown;
import com.diozero.api.SpiConstants;
import com.diozero.devices.Button;
import com.diozero.devices.MCP23S17;
import com.diozero.util.SleepUtil;

public class MCP23S17Test {
	public static void main(String[] args) {
		int address = 0;
		try (MCP23S17 mcp23s17 = new MCP23S17(SpiConstants.DEFAULT_SPI_CONTROLLER, SpiConstants.CE1, address, 25);
				Button button0 = new Button(mcp23s17, 0, GpioPullUpDown.PULL_UP);
				Button button1 = new Button(mcp23s17, 1, GpioPullUpDown.PULL_UP);
				Button button2 = new Button(mcp23s17, 2, GpioPullUpDown.PULL_UP);
				Button button3 = new Button(mcp23s17, 3, GpioPullUpDown.PULL_UP);
				Button button4 = new Button(mcp23s17, 4, GpioPullUpDown.PULL_UP);
				Button button5 = new Button(mcp23s17, 5, GpioPullUpDown.PULL_UP);
				Button button6 = new Button(mcp23s17, 6, GpioPullUpDown.PULL_UP);
				Button button7 = new Button(mcp23s17, 7, GpioPullUpDown.PULL_UP)) {
			Logger.info("Using {}", mcp23s17.getName());
			button0.whenPressed(() -> Logger.info("0 Pressed"));
			button0.whenReleased(() -> Logger.info("0 Released"));
			button1.whenPressed(() -> Logger.info("1 Pressed"));
			button1.whenReleased(() -> Logger.info("1 Released"));
			button2.whenPressed(() -> Logger.info("2 Pressed"));
			button2.whenReleased(() -> Logger.info("2 Released"));
			button3.whenPressed(() -> Logger.info("3 Pressed"));
			button3.whenReleased(() -> Logger.info("3 Released"));
			button4.whenPressed(() -> Logger.info("4 Pressed"));
			button4.whenReleased(() -> Logger.info("4 Released"));
			button5.whenPressed(() -> Logger.info("5 Pressed"));
			button5.whenReleased(() -> Logger.info("5 Released"));
			button6.whenPressed(() -> Logger.info("6 Pressed"));
			button6.whenReleased(() -> Logger.info("6 Released"));
			button7.whenPressed(() -> Logger.info("7 Pressed"));
			button7.whenReleased(() -> Logger.info("7 Released"));
			double delay = 0.5;
			for (int i=0; i<40; i++) {
				Logger.info("button0.getValue()={}", Boolean.valueOf(button0.getValue()));
				Logger.info("button1.getValue()={}", Boolean.valueOf(button1.getValue()));
				Logger.info("button2.getValue()={}", Boolean.valueOf(button2.getValue()));
				Logger.info("button3.getValue()={}", Boolean.valueOf(button3.getValue()));
				Logger.info("button4.getValue()={}", Boolean.valueOf(button4.getValue()));
				Logger.info("button5.getValue()={}", Boolean.valueOf(button5.getValue()));
				Logger.info("button6.getValue()={}", Boolean.valueOf(button6.getValue()));
				Logger.info("button7.getValue()={}", Boolean.valueOf(button7.getValue()));
				Logger.info("mcp23s17.getValues(0)={}", Byte.valueOf(mcp23s17.getValues(0)));
				Logger.info("Sleeping for {} sec", Double.valueOf(delay));
				SleepUtil.sleepSeconds(delay);
			}
			//SleepUtil.sleepSeconds(10);
		} catch (Throwable t) {
			Logger.error(t, "Error: " + t);
		}
	}
}
