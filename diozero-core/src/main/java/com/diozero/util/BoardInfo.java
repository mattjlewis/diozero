package com.diozero.util;

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

public class BoardInfo extends BoardPinInfo {
	private static final float DEFAULT_ADC_VREF = 1.8f;
	
	private String make;
	private String model;
	private int memory;
	private String libraryPath;
	private float adcVRef;
	
	public BoardInfo(String make, String model, int memory, String libraryPath) {
		this(make, model, memory, libraryPath, DEFAULT_ADC_VREF);
	}
	
	public BoardInfo(String make, String model, int memory, String libraryPath, float adcVRef) {
		this.make = make;
		this.model = model;
		this.memory = memory;
		this.libraryPath = libraryPath;
		this.adcVRef = adcVRef;
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
	
	public float getAdcVRef() {
		return adcVRef;
	}
	
	public String getName() {
		return make + " " + model;
	}

	@Override
	public String toString() {
		return "BoardInfo [make=" + make + ", model=" + model + ", memory=" + memory + ", libraryPath=" + libraryPath
				+ ", adcVRef=" + adcVRef + "]";
	}

	public boolean sameMakeAndModel(BoardInfo boardInfo) {
		return make.equals(boardInfo.getMake()) && model.equals(boardInfo.getModel());
	}
	
	@SuppressWarnings("static-method")
	public int getPwmChip(int pwmNum) {
		return -1;
	}
}
