package com.diozero.devices;

/*-
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Core
 * Filename:     ColourSsdOled.java  
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

import com.diozero.api.DigitalOutputDevice;

public abstract class ColourSsdOled extends SsdOled {
	private static final int RED_BITS = 5;
	private static final int GREEN_BITS = 6;
	private static final int BLUE_BITS = 5;
	public static final byte MAX_RED = (byte) (Math.pow(2, RED_BITS) - 1);
	public static final byte MAX_GREEN = (byte) (Math.pow(2, GREEN_BITS) - 1);
	public static final byte MAX_BLUE = (byte) (Math.pow(2, BLUE_BITS) - 1);
	
	public ColourSsdOled(int controller, int chipSelect, DigitalOutputDevice dcPin, DigitalOutputDevice resetPin,
			int width, int height, int imageType) {
		super(controller, chipSelect, dcPin, resetPin, width, height, imageType);
		
		// 16 bit colour hence 2x
		buffer = new byte[2 * width * height];
	}

	public abstract void setPixel(int x, int y, byte red, byte green, byte blue, boolean render);
	public abstract void setContrast(byte level);
}
