package com.diozero.sampleapps;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Sample applications
 * Filename:     LedBarGraphTest.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 mattjlewis
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


import java.util.Arrays;
import java.util.List;

import org.pmw.tinylog.Logger;

import com.diozero.api.Animation;
import com.diozero.api.AnimationInstance.KeyFrame;
import com.diozero.api.easing.Sine;
import com.diozero.devices.LedBarGraph;
import com.diozero.devices.MCP23008;
import com.diozero.util.SleepUtil;

public class LedBarGraphTest {
	public static void main(String[] args) {
		if (args.length < 1) {
			Logger.error("Usage: {} <LED GPIOs>", LedBarGraph.class.getName());
			System.exit(1);
		}
		
		String[] gpio_string = args[0].split(",");
		int[] gpios = new int[gpio_string.length];
		for (int i=0; i<gpio_string.length; i++) {
			gpios[i] = Integer.parseInt(gpio_string[i]);
		}
		
		test(gpios);
	}
	
	private static void test(int[] gpios) {
		int delay = 10;
		
		int duration = 4000;
		float[] cue_points = new float[] { 0, 0.5f, 1 };
		List<KeyFrame[]> key_frames = KeyFrame.fromValues(new float[][] { {0}, {1f}, {0} });
		try (MCP23008 expander = new MCP23008();
				LedBarGraph led_bar_graph = new LedBarGraph(expander, gpios)) {
			Animation anim = new Animation(Arrays.asList(led_bar_graph), 50, Sine::easeIn, 1f);
			anim.setLoop(true);
			anim.enqueue(duration, cue_points, key_frames);
			anim.play();
			
			Logger.info("Sleeping for {} seconds", Integer.valueOf(delay));
			SleepUtil.sleepSeconds(delay);
		}
	}
}
