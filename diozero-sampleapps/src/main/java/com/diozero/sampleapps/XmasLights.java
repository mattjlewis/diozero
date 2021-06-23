package com.diozero.sampleapps;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Sample applications
 * Filename:     XmasLights.java
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

import com.diozero.devices.LedBarGraph;
import com.diozero.devices.MCP23017;
import com.diozero.devices.mcp23xxx.MCP23xxx;
import com.diozero.util.SleepUtil;

public class XmasLights {
	public static void main(String[] args) {
		int controller = 1;
		if (args.length > 0) {
			controller = Integer.parseInt(args[0]);
		}

		try (MCP23017 gpio_expander = new MCP23017(controller, MCP23017.DEVICE_ADDRESS,
				MCP23xxx.INTERRUPT_GPIO_NOT_SET);
				LedBarGraph led_bar_graph = new LedBarGraph(gpio_expander, 8, 9, 10, 11, 12, 13, 14, 15, 7, 6, 5, 4, 3,
						2, 1, 0)) {
			while (true) {
				for (int i = 0; i < 4; i++) {
					Logger.debug("toggle");
					led_bar_graph.toggle();
					SleepUtil.sleepMillis(500);
				}

				Logger.debug("blinking for 3s");
				led_bar_graph.blink(0.25f, 0.25f, 6, null);
				SleepUtil.sleepSeconds(3);
				led_bar_graph.off();

				Logger.debug("gradual on");
				for (float f = 0; f <= 1; f += 0.02) {
					led_bar_graph.setValue(f);
					SleepUtil.sleepMillis(10);
				}

				Logger.debug("gradual off");
				for (float f = 1; f >= 0; f -= 0.02) {
					led_bar_graph.setValue(f);
					SleepUtil.sleepMillis(10);
				}

				led_bar_graph.off();
			}
		}
	}
}
