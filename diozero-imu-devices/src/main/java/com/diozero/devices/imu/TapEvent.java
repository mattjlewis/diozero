package com.diozero.devices.imu;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - IMU device classes
 * Filename:     TapEvent.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2022 diozero
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


public class TapEvent {
	private TapType direction;
	private short count;

	public TapEvent(TapType direction, short count) {
		this.direction = direction;
		this.count = count;
	}

	public TapType getDirection() {
		return direction;
	}

	public short getCount() {
		return count;
	}

	@Override
	public String toString() {
		return "TapCallbackEvent [direction=" + direction + ", count=" + count + "]";
	}
	
	public static enum TapAxisType {
		TAP_X(1), TAP_Y(2), TAP_Z(4), TAP_XYZ(7);
		
		private int val;
		
		private TapAxisType(int val) {
			this.val = val;
		}
		
		public int getVal() {
			return val;
		}
	}
	
	public static enum TapType {
		UNKNOWN, TAP_X_UP, TAP_X_DOWN, TAP_Y_UP, TAP_Y_DOWN, TAP_Z_UP, TAP_Z_DOWN
	}
}
