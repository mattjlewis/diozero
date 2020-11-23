package com.diozero.devices;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     BH1750.java  
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


import java.io.Closeable;

import com.diozero.api.I2CDevice;
import com.diozero.api.RuntimeIOException;
import com.diozero.util.SleepUtil;

/**
 * BH1750 Luminosity sensor
 * <a href="http://www.mouser.com/ds/2/348/bh1750fvi-e-186247.pdf">Datasheet</a>
 * <a href="https://gist.github.com/oskar456/95c66d564c58361ecf9f">Python code</a>
 */
@SuppressWarnings("unused")
public class BH1750 implements LuminositySensorInterface, Closeable {
	private static final int DEFAULT_ADDRESS = 0b010_0011;
	
	// No active state.
	private static final byte POWER_DOWN = 0b0000_0000;
	// Waiting for measurement command.
	private static final byte POWER_ON = 0b0000_0001;
	// Reset Data register value. Reset command is not acceptable in Power Down mode. 
	private static final byte RESET = 0b0000_0111;
	public static enum Mode {
		// Start measurement at 1lx resolution. Measurement Time is typically 120ms. 
		CONTINUOUS_HIGH_RES_MODE((byte) 0b0001_0000, 120),
		// Start measurement at 0.5lx resolution. Measurement Time is typically 120ms.
		CONTINUOUS_HIGH_RES_MODE_2((byte) 0b0001_0001, 120),
		// Start measurement at 4lx resolution. Measurement Time is typically 16ms. 
		CONTINUOUS_LOW_RES_MODE((byte) 0b0001_0011, 16),
		// Start measurement at 1lx resolution. Measurement Time is typically 120ms.
		// It is automatically set to Power Down mode after measurement.
		ONE_TIME_HIGH_RES_MODE((byte) 0010_0000, 120),
		// Start measurement at 0.5lx resolution. Measurement Time is typically 120ms.
		// It is automatically set to Power Down mode after measurement.
		ONE_TIME_HIGH_RES_MODE_2((byte) 0010_0001, 120),
		// Start measurement at 4lx resolution. Measurement Time is typically 16ms. 
		// It is automatically set to Power Down mode after measurement
		ONE_TIME_LOW_RES_MODE((byte) 0010_0011, 16);
		
		private byte command;
		private int measurementTimeMs;
		private Mode(byte command, int measurementTimeMs) {
			this.command = command;
			this.measurementTimeMs = measurementTimeMs;
		}
		
		public byte getCommand() {
			return command;
		}
		
		public int getMeasurementTimeMs() {
			return measurementTimeMs;
		}
	}
	private static final Mode DEFAULT_MODE = Mode.CONTINUOUS_HIGH_RES_MODE;
	
	private I2CDevice device;
	private Mode mode;
	
	public BH1750(int controller) {
		this(controller, DEFAULT_ADDRESS, DEFAULT_MODE);
	}
	
	public BH1750(int controller, int address) {
		this(controller, DEFAULT_ADDRESS, DEFAULT_MODE);
	}
	
	public BH1750(int controller, int address, Mode mode) {
		device = new I2CDevice(controller, address);
		setMode(mode);
	}
	
	@Override
	public void close() {
		device.close();
	}
	
	public Mode getMode() {
		return mode;
	}
	
	public void setMode(Mode mode) {
		device.writeByte(mode.getCommand());
		SleepUtil.sleepMillis(mode.getMeasurementTimeMs());
		this.mode = mode;
	}
	
	public void reset() {
		device.writeByte(POWER_ON);
		device.writeByte(RESET);
	}

	@Override
	public float getLuminosity() throws RuntimeIOException {
		return device.readUShort(mode.getCommand()) / 1.2f;
	}
}
