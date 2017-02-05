package com.diozero;

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
import java.nio.ByteBuffer;

import org.pmw.tinylog.Logger;

import com.diozero.api.*;
import com.diozero.internal.spi.*;
import com.diozero.util.RuntimeIOException;

public class McpAdc extends AbstractDeviceFactory implements AnalogInputDeviceFactoryInterface, Closeable {
	/** @see <a href="http://ww1.microchip.com/downloads/en/DeviceDoc/21293C.pdf">MCP3001</a> */
	public static final Type MCP3001 = Type.MCP3001;
	/** @see <a href="http://ww1.microchip.com/downloads/en/DeviceDoc/21294E.pdf">MCP3002</a> */
	public static final Type MCP3002 = Type.MCP3002;
	/** @see <a href="http://ww1.microchip.com/downloads/en/DeviceDoc/21295d.pdf">MCP3004</a> */
	public static final Type MCP3004 = Type.MCP3004;
	/** @see <a href="http://ww1.microchip.com/downloads/en/DeviceDoc/21295d.pdf">MCP3008</a> */
	public static final Type MCP3008 = Type.MCP3008;
	/** @see <a href="http://ww1.microchip.com/downloads/en/DeviceDoc/21290F.pdf">MCP3201</a> */
	public static final Type MCP3201 = Type.MCP3201;
	/** @see <a href="http://ww1.microchip.com/downloads/en/DeviceDoc/21034F.pdf">MCP3202</a> */
	public static final Type MCP3202 = Type.MCP3202;
	/** @see <a href="http://ww1.microchip.com/downloads/en/DeviceDoc/21298e.pdf">MCP3204</a> */
	public static final Type MCP3204 = Type.MCP3204;
	/** @see <a href="http://ww1.microchip.com/downloads/en/DeviceDoc/21298e.pdf">MCP3208</a> */
	public static final Type MCP3208 = Type.MCP3208;
	/** @see <a href="http://ww1.microchip.com/downloads/en/DeviceDoc/21700E.pdf">MCP3301</a> */
	public static final Type MCP3301 = Type.MCP3301;
	/** @see <a href="http://ww1.microchip.com/downloads/en/DeviceDoc/21697F.pdf">MCP3302</a> */
	public static final Type MCP3302 = Type.MCP3302;
	/** @see <a href="http://ww1.microchip.com/downloads/en/DeviceDoc/21697F.pdf">MCP3304</a> */
	public static final Type MCP3304 = Type.MCP3304;
	
	private Type type;
	private SpiDevice spiDevice;
	
	public McpAdc(Type type, int chipSelect) throws RuntimeIOException {
		this(type, SPIConstants.DEFAULT_SPI_CONTROLLER, chipSelect);
	}

	public McpAdc(Type type, int controller, int chipSelect) throws RuntimeIOException {
		super(type.name() + "-" + controller + "-" + chipSelect + "-");
		
		this.type = type;
		
		spiDevice = new SpiDevice(controller, chipSelect, type.getMaxFreq2v7(), SpiClockMode.MODE_0, false);
	}
	
	@Override
	public void close() throws RuntimeIOException {
		Logger.debug("close()");
		// Close all open pins before closing the SPI device itself
		shutdown();
		spiDevice.close();
	}

	/**
	 * Read the raw integer value 0..(range-1) or -range..(range-1)
	 * If differential read see the table below for channel selection.
	 * <pre>{@code
	 * Single |    |    |    | Pin | Input  | Channel  
	 * / Diff | D2 | D1 | D0 | Num | Config | Selection
	 * -------+----+----+----+-----+--------+----------
	 *   1    | 0  | 0  | 0  |  0  | Single |   CH0
	 *   1    | 0  | 0  | 1  |  1  | Single |   CH1
	 *   1    | 0  | 1  | 0  |  2  | Single |   CH2
	 *   1    | 0  | 1  | 1  |  3  | Single |   CH3
	 *   1    | 1  | 0  | 0  |  4  | Single |   CH4
	 *   1    | 1  | 0  | 1  |  5  | Single |   CH5
	 *   1    | 1  | 1  | 0  |  6  | Single |   CH6
	 *   1    | 1  | 1  | 1  |  7  | Single |   CH7
	 * -------+----+----+----+-----+--------+----------
	 *   0    | 0  | 0  | 0  |  0  | Single | CH0=IN+
	 *        |    |    |    |     | Single | CH1=IN-
	 * -------+----+----+----+-----+--------+----------
	 *   0    | 0  | 0  | 1  |  1  | Single | CH0=IN-
	 *        |    |    |    |     | Single | CH1=IN+
	 * -------+----+----+----+-----+--------+----------
	 *   0    | 0  | 1  | 0  |  2  | Single | CH2=IN+
	 *        |    |    |    |     | Single | CH3=IN-
	 * -------+----+----+----+-----+--------+----------
	 *   0    | 0  | 1  | 1  |  3  | Single | CH2=IN-
	 *        |    |    |    |     | Single | CH3=IN+
	 * -------+----+----+----+-----+--------+----------
	 *   0    | 1  | 0  | 0  |  4  | Single | CH4=IN+
	 *        |    |    |    |     | Single | CH5=IN-
	 * -------+----+----+----+-----+--------+----------
	 *   0    | 1  | 0  | 1  |  5  | Single | CH4=IN-
	 *        |    |    |    |     | Single | CH5=IN+
	 * -------+----+----+----+-----+--------+----------
	 *   0    | 1  | 1  | 0  |  6  | Single | CH6=IN+
	 *        |    |    |    |     | Single | CH7=IN-
	 * -------+----+----+----+-----+--------+----------
	 *   0    | 1  | 1  | 1  |  7  | Single | CH6=IN-
	 *        |    |    |    |     | Single | CH7=IN+
	 * }</pre>
	 * @throws RuntimeIOException 
	 */
	private int getRawValue(int adcPin, boolean differentialRead) throws RuntimeIOException {
		if (adcPin < 0 || adcPin >= type.getNumPins()) {
			throw new IllegalArgumentException(
					"Invalid channel number (" + adcPin + "), must be >= 0 and < " + type.getNumPins());
		}
		
		ByteBuffer out;
		if (type == Type.MCP3301) {
			// MCP3301 always operates in differential mode so has no control data - just send 2 bytes (0, 0)
			out = ByteBuffer.allocate(2);
		} else {
			/*
			 * The transmit bits start with a start bit "1" followed by the
			 * single/differential bit (M) which is 1 for single-ended read, and
			 * 0 for differential read, followed by 3 bits for the channel (C).
			 * The remainder of the transmission are "don't care" bits (x).
			 * Tx   0001MCCC xxxxxxxx xxxxxxxx
			 */
			out = ByteBuffer.allocate(3);
			out.put((byte) (0x10 | (differentialRead ? 0 : 0x08 ) | adcPin));
		}
		// Pad with 2 zero bytes
		out.put((byte) 0);
		out.put((byte) 0);
		
		out.flip();
		ByteBuffer in = spiDevice.writeAndRead(out);
		//Logger.debug(String.format("0x%x, 0x%x, 0x%x",
		//		Byte.valueOf(in.get(0)), Byte.valueOf(in.get(1)), Byte.valueOf(in.get(2))));

		return extractValue(in);
	}
	
	private int extractValue(ByteBuffer in) {
		// MCP3301 has just one input so doesn't need to send any control data, therefore only receives 2 bytes
		// Skip the first byte for all other MCP33xx models
		if (! type.isModel3301()) {
			in.get();
		}
		
		/*
		 * Rx x0RRRRRR RRRRxxxx for the 30xx (10-bit unsigned)
		 * Rx x0RRRRRR RRRRRRxx for the 32xx (12-bit unsigned)
		 * Rx x0SRRRRR RRRRRRRx for the 33xx (13-bit signed)
		 */
		if (type.isSigned()) {
			// Relies on the >> operator to preserve the sign bit
			return ((short) (in.getShort() << 2)) >> (14+2-type.getResolution());
		}
		
		// Note can't use >>> to propagate MSB 0s as it doesn't work with short, only integer
		return (in.getShort() & 0x3fff) >> (14 - type.getResolution());
	}
	
	// TODO Support for differential mode
	/**
	 * Read the analog value in the range 0..1 or -1..1 (if the ADC type is signed)
	 * @param adcPin Pin on the MCP device
	 * @return the unscaled value (-1..1)
	 * @throws RuntimeIOException if an I/O error occurs
	 */
	public float getValue(int adcPin) throws RuntimeIOException {
		return getRawValue(adcPin, false) / (float)type.getRange();
	}

	/**
	 * Device Factory SPI method
	 * @param gpio Pin on the MCP device
	 */
	@Override
	public GpioAnalogInputDeviceInterface provisionAnalogInputPin(int gpio)
			throws RuntimeIOException {
		if (gpio < 0 || gpio >= type.getNumPins()) {
			throw new IllegalArgumentException(
					"Invalid channel number (" + gpio + "), must be >= 0 and < " + type.getNumPins());
		}
		
		String key = createPinKey(gpio);
		
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}
		
		GpioAnalogInputDeviceInterface device = new McpAdcAnalogInputDevice(this, key, gpio);
		deviceOpened(device);
		
		return device;
	}

	@Override
	public String getName() {
		return type.name() + "-" + spiDevice.getController() + "-" + spiDevice.getChipSelect();
	}
	
	/**
	 * The MCP3204/3208 devices offer the choice of using the analog input channels configured
	 * as single-ended inputs or pseudo-differential pairs.  When used in the pseudo-differential
	 * mode, each channel pair (i.e., CH0 and CH1, CH2 and CH3 etc.) is programmed to be the IN+
	 * and IN- inputs as part of the command string transmitted to the device. The IN+ input can
	 * range from IN- to (VREF + IN-). The IN- input is limited to ±100 mV from the VSS rail.
	 * The IN- input can be used to cancel small signal common-mode noise which is present on both
	 * the IN+ and IN- inputs.
	 * When operating in the pseudo-differential mode, if the voltage level of IN+ is equal to
	 * or less than IN-, the resultant code will be 000h. If the voltage at IN+ is equal to or
	 * greater than {[VREF  + (IN-)] - 1 LSB}, then the output code will be FFFh. If the voltage
	 * level at IN- is more than 1 LSB below VSS, the voltage level at the IN+ input will have
	 * to go below VSS to see the 000h output code. Conversely, if IN- is more than 1 LSB above
	 * VSS, then the FFFh code will not be seen unless the IN+ input level goes above VREF level.
	 */
	public static enum Type {
		MCP3001(1, 10, 1_050_000, 2_800_000), MCP3002(2, 10, 1_200_000, 3_200_000),
		MCP3004(4, 10, 1_350_000, 3_600_000), MCP3008(8, 10, 1_350_000, 3_600_000),
		MCP3201(1, 12, 800_000, 1_600_000), MCP3202(2, 12, 900_000, 1_800_000),
		MCP3204(4, 12, 1_000_000, 2_000_000), MCP3208(8, 12, 1_000_000, 2_000_000),
		MCP3301(1, 13, 1_000_000, 1_700_000, true),
		MCP3302(4, 13, 1_350_000, 2_000_000, true), MCP3304(8, 13, 1_350_000, 2_000_000, true);
		
		private int numPins;
		private int resolution;
		private int maxFreq2v7;
		private int maxFreq5v0;
		private boolean signed;
		private int range;

		private Type(int numPins, int resolution, int maxFreq2v7, int maxFreq5v0) {
			this(numPins, resolution, maxFreq2v7, maxFreq5v0, false);
		}

		private Type(int numPins, int resolution, int maxFreq2v7, int maxFreq5v0, boolean signed) {
			this.numPins = numPins;
			this.resolution = resolution;
			this.maxFreq2v7 = maxFreq2v7;
			this.maxFreq5v0 = maxFreq5v0;
			this.signed = signed;
			range = (int)Math.pow(2, resolution) / (signed ? 2 : 1);
		}
		
		public int getNumPins() {
			return numPins;
		}
		
		public int getResolution() {
			return resolution;
		}
		
		public int getMaxFreq2v7() {
			return maxFreq2v7;
		}
		
		public int getMaxFreq5v0() {
			return maxFreq5v0;
		}
		
		public boolean isSigned() {
			return signed;
		}
		
		public int getRange() {
			return range;
		}
		
		public boolean isModel33() {
			// FIXME Need a cleaner way of doing this
			return name().substring(0, 5).equals("MCP33");
		}
		
		public boolean isModel3301() {
			// FIXME Need a cleaner way of doing this
			return name().equals("MCP3301");
		}
	}
	
	private static class McpAdcAnalogInputDevice extends AbstractInputDevice<AnalogInputEvent> implements GpioAnalogInputDeviceInterface {
		private McpAdc mcp3xxx;
		private int gpio;

		public McpAdcAnalogInputDevice(McpAdc mcp3xxx, String key, int gpio) {
			super(key, mcp3xxx);
			
			this.mcp3xxx = mcp3xxx;
			this.gpio = gpio;
		}

		@Override
		public void closeDevice() {
			Logger.debug("closeDevice()");
			// TODO Nothing to do?
		}

		@Override
		public float getValue() throws RuntimeIOException {
			return mcp3xxx.getValue(gpio);
		}

		@Override
		public int getGpio() {
			return gpio;
		}
	}
}
