package com.diozero.devices;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     PCF8591.java  
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

import java.nio.ByteOrder;

import org.tinylog.Logger;

import com.diozero.api.AnalogInputEvent;
import com.diozero.api.I2CConstants;
import com.diozero.api.I2CDevice;
import com.diozero.api.PinInfo;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.AbstractDeviceFactory;
import com.diozero.internal.spi.AbstractInputDevice;
import com.diozero.internal.spi.AnalogInputDeviceFactoryInterface;
import com.diozero.internal.spi.AnalogInputDeviceInterface;
import com.diozero.internal.spi.AnalogOutputDeviceFactoryInterface;
import com.diozero.internal.spi.AnalogOutputDeviceInterface;
import com.diozero.sbc.BoardPinInfo;

/**
 * <p>
 * Analog to Digital Converter. 4 analog in / 1 analog out.
 * </p>
 * <p>
 * Datasheet: <a href=
 * "http://www.nxp.com/documents/data_sheet/PCF8591.pdf">http://www.nxp.com/documents/data_sheet/PCF8591.pdf</a>.
 * </p>
 * <p>
 * Note the <a href=
 * "http://www.raspoid.com/source/src__main__com__raspoid__additionalcomponents__adc__PCF8591.java">raspoid</a>
 * driver states there is a <em>known bug when reading digital values from
 * PCF8591 if analog output is disabled</em>.
 * </p>
 * <p>
 * <a href=
 * "https://brainfyre.wordpress.com/2012/10/25/pcf8591-yl-40-ad-da-module-review/">Instructions</a>:<br>
 * The jumpers control whether analog input channels of the IC are connected to
 * the analog sources:
 * </p>
 * <ul>
 * <li>Jumper P4 for AIN1: The temperature sensed by the R6 thermister is
 * provided to the ADC.</li>
 * <li>Jumper P5 to AIN0: The R7 photocell voltage (resistance drop) is provided
 * to the DAC.</li>
 * <li>Jumper P6 to AIN3: The single turn 10K ohm trimpot voltage (resistance
 * drop ? brighter light, lower resistance).</li>
 * </ul>
 * <p>
 * From my experiments, the inputs / jumpers are configured as follows:
 * </p>
 * <ul>
 * <li>AIN0: trimpot (P6)</li>
 * <li>AIN1: LDR (P5)</li>
 * <li>AIN2: ?temp? (P4)</li>
 * <li>AIN3: AIN3</li>
 * </ul>
 * <p>
 * Removing a jumper allows an input channel to be fed from one of the external
 * pins, labelled accordingly.
 * </p>
 */
@SuppressWarnings("unused")
public class PCF8591 extends AbstractDeviceFactory
		implements AnalogInputDeviceFactoryInterface, AnalogOutputDeviceFactoryInterface {
	private static final String DEVICE_NAME = "PCF8591";
	private static final int RESOLUTION = 8;
	private static final float RANGE = (float) Math.pow(2, RESOLUTION);
	private static final float DEFAULT_VREF = 3.3f;
	private static final int DEFAULT_ADDRESS = 0x48;
	// Flags for the control byte
	// [0:1] A/D Channel Number
	// [2] Auto increment flag (active if 1)
	// [3] 0
	// [4:5] Analog input mode
	// [6] Analog output enable flag (analog output active if 1)
	// [7] 0
	/**
	 * If the auto-increment flag is set to 1, the channel number is incremented
	 * automatically after each A/D conversion.
	 */
	private static final byte AUTO_INCREMENT_FLAG = 0b0000_0100; // 0x04
	private static final byte ANALOG_OUTPUT_ENABLE_MASK = 0b0100_0000; // 0x40

	private I2CDevice device;
	private boolean outputEnabled = false;
	private InputMode inputMode;
	private BoardPinInfo boardPinInfo;
	private float vRef;

	public PCF8591() {
		this(I2CConstants.CONTROLLER_1, DEFAULT_ADDRESS, InputMode.FOUR_SINGLE_ENDED_INPUTS, true, DEFAULT_VREF);
	}

	public PCF8591(int controller) {
		this(controller, DEFAULT_ADDRESS, InputMode.FOUR_SINGLE_ENDED_INPUTS, true, DEFAULT_VREF);
	}

	public PCF8591(int controller, int address, InputMode inputMode, boolean outputEnabled, float vRef) {
		super(DEVICE_NAME + "-" + controller + "-" + address);

		this.inputMode = inputMode;
		this.outputEnabled = outputEnabled;
		this.vRef = vRef;

		device = I2CDevice.builder(address).setController(controller).setByteOrder(ByteOrder.LITTLE_ENDIAN).build();

		boardPinInfo = new PCF8591BoardPinInfo(inputMode);
	}

	@Override
	public float getVRef() {
		return vRef;
	}

	@Override
	public BoardPinInfo getBoardPinInfo() {
		return boardPinInfo;
	}

	@Override
	public void close() throws RuntimeIOException {
		device.close();
	}

	@Override
	public String getName() {
		return DEVICE_NAME + "-" + device.getController() + "-" + device.getAddress();
	}

	@Override
	public AnalogInputDeviceInterface createAnalogInputDevice(String key, PinInfo pinInfo) throws RuntimeIOException {
		return new PCF8591AnalogInputDevice(this, key, pinInfo.getDeviceNumber());
	}

	@Override
	public AnalogOutputDeviceInterface createAnalogOutputDevice(String key, PinInfo pinInfo, float initialValue)
			throws RuntimeIOException {
		return new PCF8591AnalogOutputDevice(this, key, pinInfo.getDeviceNumber(), initialValue);
	}

	/**
	 * Read the analog value in the range 0..1
	 * 
	 * @param adcPin Pin on the MCP device
	 * @return The unscaled value (0..1)
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public float getValue(int adcPin) throws RuntimeIOException {
		return getRawValue(adcPin) / RANGE;
	}

	/**
	 * Set the analog output value.
	 * 
	 * @param dacPin The analog output channel.
	 * @param value  Analogue output value (0..1).
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void setValue(int dacPin, float value) throws RuntimeIOException {
		outputEnabled = true;
		if (dacPin < 0 || dacPin >= 1) {
			throw new IllegalArgumentException("Invalid output channel number (" + dacPin + ")");
		}
		if (value < 0 || value >= 1) {
			throw new IllegalArgumentException("Invalid output value (" + value + ", must be 0..1");
		}
		// device.writeByte(ANALOG_OUTPUT_ENABLE_MASK, (byte) (value * (RANGE-1)));
		byte[] data = new byte[2];
		data[0] = ANALOG_OUTPUT_ENABLE_MASK;
		data[1] = (byte) (value * (RANGE - 1));
		device.writeBytes(data);
	}

	private int getRawValue(int adcPin) {
		if (adcPin < 0 || adcPin >= inputMode.getNumPins()) {
			throw new IllegalArgumentException(
					"Invalid input channel number (" + adcPin + ") for input mode " + inputMode.getName());
		}

		// Set output enable?
		byte control_byte = (byte) ((outputEnabled ? ANALOG_OUTPUT_ENABLE_MASK : 0) | inputMode.getControlFlags()
				| adcPin);
		// Note if the auto-increment flag is set you need to read 5 bytes (if all
		// channels are in single input mode),
		// 1 for the previous value + 1 for each channel.

		device.writeByte(control_byte);

		byte[] data = device.readBytes(2);
		// Note data[0] is the previous value held in the DAC register, data[1] is value
		// of data byte 1

		return data[1] & 0xff;
	}

	public int getNumPins() {
		return inputMode.getNumPins();
	}

	public void setOutputEnabledFlag(boolean outputEnabled) {
		this.outputEnabled = outputEnabled;
	}

	private static class PCF8591AnalogInputDevice extends AbstractInputDevice<AnalogInputEvent>
			implements AnalogInputDeviceInterface {
		private PCF8591 pcf8591;
		private int adcNumber;

		public PCF8591AnalogInputDevice(PCF8591 pcf8591, String key, int adcNumber) {
			super(key, pcf8591);

			this.pcf8591 = pcf8591;
			this.adcNumber = adcNumber;
		}

		@Override
		protected void closeDevice() {
			Logger.trace("closeDevice()");
			// TODO Nothing to do?
		}

		@Override
		public float getValue() throws RuntimeIOException {
			return pcf8591.getValue(adcNumber);
		}

		@Override
		public int getAdcNumber() {
			return adcNumber;
		}
	}

	private static class PCF8591AnalogOutputDevice extends AbstractDevice implements AnalogOutputDeviceInterface {
		private int adcNumber;
		private PCF8591 pcf8591;

		public PCF8591AnalogOutputDevice(PCF8591 pcf8591, String key, int adcNumber, float initialValue) {
			super(key, pcf8591);

			this.pcf8591 = pcf8591;
			this.adcNumber = adcNumber;
			
			setValue(initialValue);
		}

		@Override
		protected void closeDevice() throws RuntimeIOException {
			Logger.trace("closeDevice()");
			// TODO Nothing to do?
		}

		@Override
		public int getAdcNumber() {
			return adcNumber;
		}

		@Override
		public float getValue() throws RuntimeIOException {
			return pcf8591.getValue(adcNumber);
		}

		@Override
		public void setValue(float value) throws RuntimeIOException {
			pcf8591.setValue(adcNumber, value);
		}
	}

	public static enum InputMode {
		FOUR_SINGLE_ENDED_INPUTS(0b00, 4, "Four single-ended inputs"),
		/** Channel 0=AIN0-AIN3, Channel 1=AIN1-AIN3, Channel 2=AIN2-AIN3. */
		THREE_DIFFERENTIAL_INPUTS(0b01, 3, "Three differential inputs"),
		/** Channel 0=AIN0, Channel 1=AIN1, Channel 2=AIN2-AIN3. */
		SINGLE_ENDED_AND_DIFFERENTIAL_MIXED(0b10, 3, "Single-ended and differential mixed"),
		/** Channel 0=AIN0-AIN1, Channel 1=AIN2-AIN3. */
		TWO_DIFFERENTIAL_INPUTS(0b11, 2, "Two differential inputs");

		private static final int INPUT_MODE_SHIFT_LEFT = 4;

		private byte controlFlags;
		private int numPins;
		private String name;

		private InputMode(int val, int numPins, String name) {
			this.controlFlags = (byte) (val << INPUT_MODE_SHIFT_LEFT);
			this.numPins = numPins;
			this.name = name;
		}

		public byte getControlFlags() {
			return controlFlags;
		}

		public int getNumPins() {
			return numPins;
		}

		public String getName() {
			return name;
		}
	}

	public static class PCF8591BoardPinInfo extends BoardPinInfo {
		private InputMode inputMode;

		public PCF8591BoardPinInfo(InputMode inputMode) {
			this.inputMode = inputMode;

			addDacPinInfo(0, "AOUT", 15);
			switch (inputMode) {
			case FOUR_SINGLE_ENDED_INPUTS:
				addAdcPinInfo(0, 1);
				addAdcPinInfo(1, 2);
				addAdcPinInfo(2, 3);
				addAdcPinInfo(3, 4);
				break;
			case THREE_DIFFERENTIAL_INPUTS:
				addAdcPinInfo(0, "AIN0-AIN3", 1);
				addAdcPinInfo(1, "AIN1-AIN3", 2);
				addAdcPinInfo(2, "AIN2-AIN3", 3);
				break;
			case SINGLE_ENDED_AND_DIFFERENTIAL_MIXED:
				addAdcPinInfo(0, "AIN0", 1);
				addAdcPinInfo(1, "AIN1", 2);
				addAdcPinInfo(2, "AIN2-AIN3", 3);
				break;
			case TWO_DIFFERENTIAL_INPUTS:
				addAdcPinInfo(0, "AIN0-AIN1", 1);
				addAdcPinInfo(1, "AIN2-AIN3", 3);
				break;
			}
		}
	}
}
