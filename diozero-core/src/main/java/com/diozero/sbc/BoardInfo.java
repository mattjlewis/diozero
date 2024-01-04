package com.diozero.sbc;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     BoardInfo.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2023 diozero
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

import com.diozero.internal.spi.MmapGpioInterface;

/**
 * Information about the connected SBC. Note that the connected board instance
 * might be a remote device, e.g. connected via serial, Bluetooth or TCP/IP. The
 * BoardInfo instance for the connected device must be obtained by calling
 * {@link com.diozero.internal.spi.NativeDeviceFactoryInterface#getBoardInfo()
 * getBoardInfo()} the on the
 * {@link com.diozero.internal.spi.NativeDeviceFactoryInterface
 * NativeDeviceFactoryInterface} instance returned from
 * {@link DeviceFactoryHelper#getNativeDeviceFactory()}.
 */
@SuppressWarnings("static-method")
public abstract class BoardInfo extends BoardPinInfo {
	public static final String UNKNOWN = "unknown";
	public static final float UNKNOWN_ADC_VREF = -1;

	private String make;
	private String model;
	private int memoryKb;
	private String osId;
	private String osVersion;

	public BoardInfo(String make, String model, int memoryKb, String osId, String osVersion) {
		this.make = make;
		this.model = model;
		this.memoryKb = memoryKb;
		this.osId = osId;
		this.osVersion = osVersion;
	}

	/**
	 * Pin initialisation is done separately to the constructor since all known
	 * BoardInfo instances get instantiated on startup by the Java ServiceLoader.
	 */
	public abstract void populateBoardPinInfo();

	/**
	 * The make of the connected board, e.g. "Raspberry Pi"
	 *
	 * @return the make of the connected board
	 */
	public String getMake() {
		return make;
	}

	/**
	 * The model of the connected board, e.g. "3B+"
	 *
	 * @return the model of the connected board
	 */
	public String getModel() {
		return model;
	}

	/**
	 * Get the memory (in KB) of the connected board
	 *
	 * @return memory in KB
	 */
	public int getMemoryKb() {
		return memoryKb;
	}

	public String getOperatingSystemId() {
		return osId;
	}

	public String getOperatingSystemVersion() {
		return osVersion;
	}

	/**
	 * Get the name of this board - usual a concatenation of make and model
	 *
	 * @return the name of this board
	 */
	public String getName() {
		return make + " " + model;
	}

	public String getLongName() {
		return getName();
	}

	/**
	 * Compare make and model
	 *
	 * @param make2  the make to compare
	 * @param model2 the model to compare
	 * @return true if the make and model are the same
	 */
	public boolean compareMakeAndModel(String make2, String model2) {
		return this.make.equals(make2) && this.model.equals(model2);
	}

	/**
	 * Instantiate the memory mapped GPIO interface for this board. Not that the
	 * caller needs to call {@link MmapGpioInterface#initialise initialise} prior to
	 * use.
	 *
	 * @return the MMAP GPIO interface implementation for this board, null if there
	 *         isn't one
	 */
	public MmapGpioInterface createMmapGpio() {
		return null;
	}

	@Override
	public String toString() {
		return "BoardInfo [make=" + make + ", model=" + model + ", memory=" + memoryKb + "]";
	}

	public boolean isBiasControlSupported() {
		return false;
	}
}
