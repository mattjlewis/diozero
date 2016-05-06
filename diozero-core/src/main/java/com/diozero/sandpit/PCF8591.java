package com.diozero.sandpit;

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

import java.io.Closeable;
import java.nio.ByteOrder;

import org.pmw.tinylog.Logger;

import com.diozero.api.DeviceAlreadyOpenedException;
import com.diozero.api.I2CConstants;
import com.diozero.api.I2CDevice;
import com.diozero.internal.provider.pcf8591.PCF8591AnalogInputPin;
import com.diozero.internal.spi.AbstractDeviceFactory;
import com.diozero.internal.spi.AnalogInputDeviceFactoryInterface;
import com.diozero.internal.spi.GpioAnalogInputDeviceInterface;
import com.diozero.util.RuntimeIOException;

/**
 * Datasheet: <a href="http://www.nxp.com/documents/data_sheet/PCF8591.pdf">http://www.nxp.com/documents/data_sheet/PCF8591.pdf</a>.
 */
@SuppressWarnings("unused")
public class PCF8591 extends AbstractDeviceFactory implements AnalogInputDeviceFactoryInterface, Closeable {
	private static final int RESOLUTION = 8;
	private static final float RANGE = (float) Math.pow(2, RESOLUTION);
	private static final int DEFAULT_ADDRESS = 0x48;
	private static final String DEVICE_NAME = "PCF8591";
	private static final int NUM_PINS = 4;
	// Flags for the control byte
	// [0:1] A/D Channel Number
	//   [2] Auto increment flag (active if 1)
	//   [3] 0
	// [4:5] Analog input mode
	//   [6] Analog output enable flag (analog output active if 1)
	//   [7] 0
	/** If the auto-increment flag is set to 1, the channel number is incremented
	 * automatically after each A/D conversion. */
	private static final byte AUTO_INCREMENT_FLAG       = 0b00000100; // 0000 0100 0x04
	private static final byte ANALOG_OUTPUT_ENABLE_FLAG = 0b01000000; // 0100 0000 0x40
	
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
	
	private I2CDevice device;
	private String keyPrefix;
	private boolean enableAnalogOutput = false;
	private InputMode inputMode;
	
	public PCF8591() {
		this(I2CConstants.BUS_1, DEFAULT_ADDRESS, InputMode.FOUR_SINGLE_ENDED_INPUTS);
	}

	public PCF8591(int controller, int address, InputMode inputMode) {
		this.inputMode = inputMode;
		
		device = new I2CDevice(controller, address, I2CConstants.ADDR_SIZE_7, I2CConstants.DEFAULT_CLOCK_FREQUENCY);
		keyPrefix = getName() + "-";
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
	public GpioAnalogInputDeviceInterface provisionAnalogInputPin(int pinNumber) throws RuntimeIOException {
		if (pinNumber < 0 || pinNumber >= inputMode.getNumPins()) {
			throw new IllegalArgumentException(
					"Invalid channel number (" + pinNumber + "), must be >= 0 and < " + inputMode.getNumPins());
		}
		
		String key = keyPrefix + pinNumber;
		
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}
		
		GpioAnalogInputDeviceInterface device = new PCF8591AnalogInputPin(this, key, pinNumber);
		deviceOpened(device);
		
		return device;
	}
	
	/**
	 * Read the analog value in the range 0..1
	 * @param adcPin Pin on the MCP device
	 * @return The unscaled value (0..1)
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public float getValue(int adcPin) throws RuntimeIOException {
		return getRawValue(adcPin) / RANGE;
	}
	
	/**
	 * Set the analog output value.
	 * @param dacPin The analog output channel.
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public void setValue(int dacPin) throws RuntimeIOException {
		if (dacPin < 0 || dacPin >= 1) {
			throw new IllegalArgumentException("Invalid output channel number (" + dacPin + ")");
		}
		// TODO
	}

	private int getRawValue(int adcPin) {
		if (adcPin < 0 || adcPin >= inputMode.getNumPins()) {
			throw new IllegalArgumentException("Invalid input channel number (" + adcPin + ") for input mode " + inputMode.getName());
		}
		
		// Set output enable?
		byte control_byte = (byte) (inputMode.getControlFlags() | adcPin);
		// Note if the auto-increment flag is set you need to read 5 bytes (if all channels are in single input mode),
		// 1 for the previous value + 1 for each channel.
		
		device.writeByte(control_byte, ByteOrder.LITTLE_ENDIAN);
		byte[] data = device.read(2);
		// TODO Validate this... If little endian is it the reverse of this?!
		// Note data[0] is the previous value held in the DAC register, data[1] is value of data byte 1
		Logger.info(String.format("data[1]=0x%2x, data[0]=0x%2x", Byte.valueOf(data[1]), Byte.valueOf(data[0])));
		
		return data[1] & 0xff;
	}

	public int getNumPins() {
		return inputMode.getNumPins();
	}
}
