package com.diozero.util;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Core
 * Filename:     BoardInfo.java  
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


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.pmw.tinylog.Logger;

import com.diozero.internal.provider.mmap.MmapGpioInterface;

@SuppressWarnings("static-method")
public abstract class BoardInfo extends BoardPinInfo {
	private static final float DEFAULT_ADC_VREF = 1.8f;
	private static final String TEMP_FILE = "/sys/class/thermal/thermal_zone0/temp";
	
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
	
	public abstract void initialisePins();

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

	public boolean sameMakeAndModel(BoardInfo boardInfo) {
		return make.equals(boardInfo.getMake()) && model.equals(boardInfo.getModel());
	}
	
	public int getPwmChip(int pwmNum) {
		return -1;
	}

	public MmapGpioInterface createMmapGpio() {
		return null;
	}
	
	public float getCpuTemperature() {
		try {
			return Integer.parseInt(Files.lines(Paths.get(TEMP_FILE)).findFirst().orElse("0")) / 1000f;
		} catch (IOException e) {
			Logger.warn(e, "Error reading {}: {}", TEMP_FILE, e);
			return 0;
		}
	}
	
	public Collection<Integer> getI2CBuses() {
		try {
			List<Integer> i2c_buses = new ArrayList<>();
			Files.newDirectoryStream(Paths.get("/dev"), "i2c-*")
					.forEach(path -> i2c_buses.add(Integer.valueOf(path.toString().split("-")[1])));
			return i2c_buses;
		} catch (IOException e) {
			Logger.error(e, "Error: {}", e);
			return null;
		}
	}

	@Override
	public String toString() {
		return "BoardInfo [make=" + make + ", model=" + model + ", memory=" + memory + ", libraryPath=" + libraryPath
				+ ", adcVRef=" + adcVRef + "]";
	}
}
