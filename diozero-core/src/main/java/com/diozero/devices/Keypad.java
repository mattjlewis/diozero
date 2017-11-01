package com.diozero.devices;

/*-
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Core
 * Filename:     Keypad.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2017 mattjlewis
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

import java.util.ArrayList;
import java.util.List;

import org.pmw.tinylog.Logger;

import com.diozero.api.DigitalInputDevice;
import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.util.SleepUtil;

/**
 * http://www.instructables.com/id/Connecting-a-4-x-4-Membrane-Keypad-to-an-Arduino/
 * https://github.com/Chris--A/Keypad/tree/master/src
 */
public class Keypad {
	public static final char NO_KEY = '\0';
	
	private DigitalInputDevice[] rows;
	private DigitalOutputDevice[] cols;
	private char[][] keys;
	// Last read on / off state for all keys
	private boolean[][] values;
	
	public Keypad(int[] rowGpios, int[] colGpios, char[][] keys) {
		// Validate keys array dimensions matches rowGpios and colGpios
		if (keys.length != rowGpios.length || keys[0].length != colGpios.length) {
			throw new IllegalArgumentException("Array dimensions do not match");
		}
		
		this.keys = keys;
		
		rows = new DigitalInputDevice[rowGpios.length];
		cols = new DigitalOutputDevice[colGpios.length];
		int i=0;
		for (int gpio : rowGpios) {
			rows[i++] = new DigitalInputDevice(gpio, GpioPullUpDown.PULL_UP, GpioEventTrigger.NONE);
		}
		i=0;
		for (int gpio : colGpios) {
			cols[i++] = new DigitalOutputDevice(gpio, false, false);
		}
		
		values = new boolean[rows.length][cols.length];
	}
	
	public List<Character> getKeys() {
		scanKeys();
		
		List<Character> keys_pressed = new ArrayList<>();
		for (int r=0; r<rows.length; r++) {
			for (int c=0; c<cols.length; c++) {
				if (values[r][c]) {
					keys_pressed.add(Character.valueOf(keys[r][c]));
				}
			}
		}
		
		return keys_pressed;
	}
	
	private void scanKeys() {
		for (byte c=0; c<cols.length; c++) {
			// Begin column pulse output.
			cols[c].on();
			for (byte r=0; r<rows.length; r++) {
				// keypress is active low so invert to high.
				values[r][c] = rows[r].isActive();
			}
			// Set pin to high impedance input. Effectively ends column pulse.
			cols[c].off();
		}
	}
}
