package com.diozero.util;

import java.util.List;
import java.util.Map;

import com.diozero.internal.spi.GpioDeviceInterface;
import com.diozero.internal.spi.GpioDeviceInterface.Mode;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 mattjlewis
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


public abstract class BoardInfo {
	private String make;
	private String model;
	private int memory;
	private Map<Integer, List<GpioDeviceInterface.Mode>> pins;
	private String libraryPath;
	
	public BoardInfo(String make, String model, int memory, Map<Integer, List<GpioDeviceInterface.Mode>> pins,
			String libraryPath) {
		this.make = make;
		this.model = model;
		this.memory = memory;
		this.pins = pins;
		this.libraryPath = libraryPath;
	}

	public String getMake() {
		return make;
	}

	public String getModel() {
		return model;
	}

	public int getMemory() {
		return memory;
	}

	public String getLibraryPath() {
		return libraryPath;
	}

	@Override
	public String toString() {
		return "BoardInfo [make=" + make + ", model=" + model + ", memory=" + memory + "]";
	}

	public boolean sameMakeAndModel(BoardInfo boardInfo) {
		return make.equals(boardInfo.getMake()) && model.equals(boardInfo.getModel());
	}

	public boolean isSupported(Mode mode, int pin) {
		List<Mode> modes = pins.get(Integer.valueOf(pin));
		
		return mode == null ? false : modes.contains(mode);
	}
}
