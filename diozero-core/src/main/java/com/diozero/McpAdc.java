package com.diozero;

import java.io.Closeable;
import java.nio.ByteBuffer;

import org.pmw.tinylog.Logger;

import com.diozero.api.DeviceAlreadyOpenedException;
import com.diozero.api.SPIConstants;
import com.diozero.api.SpiDevice;
import com.diozero.internal.provider.mcpadc.McpAdcAnalogueInputPin;
import com.diozero.internal.spi.AbstractDeviceFactory;
import com.diozero.internal.spi.AnalogueInputDeviceFactoryInterface;
import com.diozero.internal.spi.GpioAnalogueInputDeviceInterface;
import com.diozero.util.RuntimeIOException;

public class McpAdc extends AbstractDeviceFactory implements AnalogueInputDeviceFactoryInterface, Closeable {
	/** @see http://ww1.microchip.com/downloads/en/DeviceDoc/21293C.pdf */
	public static final McpAdcType MCP3001 = McpAdcType.MCP3001;
	/** @see http://ww1.microchip.com/downloads/en/DeviceDoc/21294E.pdf */
	public static final McpAdcType MCP3002 = McpAdcType.MCP3002;
	/** @see http://ww1.microchip.com/downloads/en/DeviceDoc/21295d.pdf */
	public static final McpAdcType MCP3004 = McpAdcType.MCP3004;
	/** @see http://ww1.microchip.com/downloads/en/DeviceDoc/21295d.pdf */
	public static final McpAdcType MCP3008 = McpAdcType.MCP3008;
	/** @see http://ww1.microchip.com/downloads/en/DeviceDoc/21290F.pdf */
	public static final McpAdcType MCP3201 = McpAdcType.MCP3201;
	/** @see http://ww1.microchip.com/downloads/en/DeviceDoc/21034F.pdf */
	public static final McpAdcType MCP3202 = McpAdcType.MCP3202;
	/** @see http://ww1.microchip.com/downloads/en/DeviceDoc/21298e.pdf */
	public static final McpAdcType MCP3204 = McpAdcType.MCP3204;
	/** @see http://ww1.microchip.com/downloads/en/DeviceDoc/21298e.pdf */
	public static final McpAdcType MCP3208 = McpAdcType.MCP3208;
	/** @see http://ww1.microchip.com/downloads/en/DeviceDoc/21700E.pdf */
	public static final McpAdcType MCP3301 = McpAdcType.MCP3301;
	/** @see http://ww1.microchip.com/downloads/en/DeviceDoc/21697F.pdf */
	public static final McpAdcType MCP3302 = McpAdcType.MCP3302;
	/** @see http://ww1.microchip.com/downloads/en/DeviceDoc/21697F.pdf */
	public static final McpAdcType MCP3304 = McpAdcType.MCP3304;
	
	private McpAdcType type;
	private SpiDevice spiDevice;
	private String keyPrefix;
	
	public McpAdc(McpAdcType type, int chipSelect) throws RuntimeIOException {
		this(type, SPIConstants.DEFAULT_SPI_CONTROLLER, chipSelect);
	}

	public McpAdc(McpAdcType type, int controller, int chipSelect) throws RuntimeIOException {
		this.type = type;
		
		keyPrefix = type.name() + "-" + controller + "-" + chipSelect + "-";
		
		spiDevice = new SpiDevice(controller, chipSelect);
	}
	
	@Override
	public void close() throws RuntimeIOException {
		Logger.debug("close()");
		// Close all open pins before closing the SPI device itself
		closeAll();
		spiDevice.close();
	}

	/**
	 * Read the raw integer value 0..(range-1) or -range..(range-1)
	 * If differential read see the table below for channel selection
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
	 * 
	 * @throws RuntimeIOException 
	 */
	private int getRawValue(int adcPin, boolean differentialRead) throws RuntimeIOException {
		if (adcPin < 0 || adcPin >= type.numPins) {
			throw new IllegalArgumentException(
					"Invalid channel number (" + adcPin + "), must be >= 0 and < " + type.numPins);
		}
		
		ByteBuffer out;
		if (type == McpAdcType.MCP3301) {
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
			return ((short)(in.getShort() << 2)) >> (14+2-type.getResolution());
		}
		
		// Note can't use >>> to propagate MSB 0s as it doesn't work with short, only integer
		return (in.getShort() & 0x3fff) >> (14 - type.getResolution());
	}
	
	// TODO Support for differential mode
	/**
	 * Read the analogue value in the range 0..1 or -1..1 (if the ADC type is signed)
	 * @param adcPin
	 * @return
	 * @throws RuntimeIOException
	 */
	public float getValue(int adcPin) throws RuntimeIOException {
		return getRawValue(adcPin, false) / (float)type.range;
	}

	/**
	 * Device Factory SPI method
	 */
	@Override
	public GpioAnalogueInputDeviceInterface provisionAnalogueInputPin(int pinNumber)
			throws RuntimeIOException {
		if (pinNumber < 0 || pinNumber >= type.numPins) {
			throw new IllegalArgumentException(
					"Invalid channel number (" + pinNumber + "), must be >= 0 and < " + type.numPins);
		}
		
		String key = keyPrefix + pinNumber;
		
		if (isDeviceOpened(key)) {
			throw new DeviceAlreadyOpenedException("Device " + key + " is already in use");
		}
		
		GpioAnalogueInputDeviceInterface device = new McpAdcAnalogueInputPin(this, key, pinNumber);
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
	public static enum McpAdcType {
		MCP3001(1, 10), MCP3002(2, 10), MCP3004(4, 10), MCP3008(8, 10),
		MCP3201(1, 12), MCP3202(2, 12), MCP3204(4, 12), MCP3208(8, 12),
		MCP3301(1, 13, true), MCP3302(4, 13, true), MCP3304(8, 13, true);
		
		private int numPins;
		private int resolution;
		private boolean signed;
		private int range;

		private McpAdcType(int numPins, int resolution) {
			this(numPins, resolution, false);
		}

		private McpAdcType(int numPins, int resolution, boolean signed) {
			this.numPins = numPins;
			this.resolution = resolution;
			this.signed = signed;
			range = (int)Math.pow(2, resolution) / (signed ? 2 : 1);
		}
		
		public int getNumPins() {
			return numPins;
		}
		
		public int getResolution() {
			return resolution;
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
}
