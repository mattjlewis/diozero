package com.diozero.sandpit;

import com.diozero.api.I2CConstants;
import com.diozero.api.I2CDevice;
import com.diozero.util.RuntimeIOException;

/**
 * STMicroelectronics HTS221 "ultra compact sensor for relative humidity and temperature". Datasheet:
 * http://www2.st.com/content/ccc/resource/technical/document/datasheet/4d/9a/9c/ad/25/07/42/34/DM00116291.pdf/files/DM00116291.pdf/jcr:content/translations/en.DM00116291.pdf
 */
@SuppressWarnings("unused")
public class HTS221 {
	private static final int DEFAULT_DEVICE_ADDRESS = 0x5f;
	// Register map
	private static final int WHO_AM_I = 0x0f;
	/** Set to 1 to read, 0 to write. */
	private static int READ = 0x80;
	/** Humidity and temperature average configuration. */
	private static final int AV_CONF = 0x10;
	/** Control Register 1 (power down control, block data update, output data rate). */
	private static final int CTRL1 = 0x20;
	/** Control Register 2 (reboot, heater, one shot enable). */
	private static final int CTRL2 = 0x21;
	/** Control Register 3 (data ready output signal). */
	private static final int CTRL3 = 0x22;
	/** Status register; the content of this register is updated every one-shot reading,
	 * and after completion of every ODR cycle, regardless of BDU value in CTRL_REG1. */
	private static final int STATUS = 0x27;
	/** Humidity output registers (2s complement, signed short). */
	private static final int HUMIDITY_OUT = 0x28;
	/** Temperature output registers (2s complement, signed short). */
	private static final int TEMP_OUT = 0x2a;
	
	// Calibration data registers
	/** Unsigned byte. */
	private static final int H0_rH_x2 = 0x30;
	/** Unsigned byte. */
	private static final int H1_rH_x2 = 0x31;
	/** Unsigned byte. */
	private static final int T0_degC_x8 = 0x32;
	/** Unsigned byte. */
	private static final int T1_degC_x8 = 0x33;
	private static final int T1_T0_MSB = 0x35;
	/** Signed short. */
	private static final int H0_T0_OUT = 0x36;
	/** Signed short. */
	private static final int H1_T0_OUT = 0x3a;
	/** Signed short. */
	private static final int T0_OUT = 0x3c;
	/** Signed short. */
	private static final int T1_OUT = 0x3e;
	
	// Flags for Control Register 1
	private static final byte CR1_PD_CONTROL_BIT = 7;
	/** The PD bit is used to turn on the device. The device is in power-down mode when
	 * PD = ‘0’ (default value after boot). The device is active when PD is set to ‘1’. */
	private static final byte CR1_PD_CONTROL = (byte) (1 << CR1_PD_CONTROL_BIT);
	private static final byte CR1_BDU_BIT = 2;
	/** (0: continuous update; 1: output registers not updated until MSB and LSB reading) */
	private static final byte CR1_BDU = 1 << CR1_BDU_BIT;
	private static final byte CR1_ODR_ONE_SHOT = 0b00;
	private static final byte CR1_ODR_1HZ      = 0b01;
	private static final byte CR1_ODR_7HZ      = 0b10;
	private static final byte CR1_ODR_12_5HZ   = 0b11;
	
	private I2CDevice device;
	private float h0Rh;
	private float h1Rh;
	private short h0T0Out;
	private short h1T0Out;
	private float t0DegC;
	private float t1DegC;
	private short t0Out;
	private short t1Out;
	
	public HTS221() {
		this(I2CConstants.BUS_1, DEFAULT_DEVICE_ADDRESS);
	}
	
	public HTS221(int controller, int address) throws RuntimeIOException {
		device = new I2CDevice(controller, address, I2CConstants.ADDR_SIZE_7, I2CConstants.DEFAULT_CLOCK_FREQUENCY);
		
		// Select average configuration register
		// Bits [5:3] AVGT2-0 To select the numbers of averaged temperature samples (2 - 256).
		// Bits [2:0] AVGH2-0 To select the numbers of averaged humidity samples (4 - 512).
		// Val | Nr. Samples | Noise (RMS)
		//     | Temp | Hum. | T Deg C | rH% 
		// 000 |   2  |   4  |  0.08   | 0.4l
		// 001 |   4  |   8  |  0.05   | 0.3
		// 010 |   8  |  16  |  0.04   | 0.2
		// 011 |  16  |  32  |  0.03   | 0.15
		// 100 |  32  |  64  |  0.02   | 0.1
		// 101 |  64  | 128  |  0.015  | 0.07
		// 110 | 128  | 256  |  0.01   | 0.05
		// 111 | 256  | 512  |  0.007  | 0.03
		// Temperature average samples = 16, humidity average samples = 32
		device.writeByte(AV_CONF, (byte) (0b011 << 3 | 0b011));
		
		// Select control register1
		// Power on, block data update, data rate o/p = 1 Hz (0x85 / 0b10000101)
		// Note RTIMULib uses 0x87 (0b10000111) - 12.5Hz
		device.writeByte(CTRL1, (byte) (CR1_PD_CONTROL | CR1_BDU | CR1_ODR_1HZ));
		//Thread.sleep(500);

		// Read Calibration values from the non-volatile memory of the device
		// Humidity Calibration values
		// Read 1 byte of data from address 0x30(48)
		//int H0 = (val[0] & 0xFF) / 2;
		h0Rh = device.readUByte(H0_rH_x2 | READ) / 2f;
		// Read 1 byte of data from address 0x31(49)
		//int H1 = (val[1] & 0xFF) / 2;
		h1Rh = device.readUByte(H1_rH_x2 | READ) / 2f;

		// Read H0_T0_OUT register
		h0T0Out = device.readShort(H0_T0_OUT | READ);

		// Read H1_T0_OUT register
		h1T0Out = device.readByte(H1_T0_OUT | READ);

		// Read the MSB values for T0 and T1
		int t1_t0_msb = device.readByte(T1_T0_MSB | READ) & 0x0F;

		// Temperature calibration values (10-bit unsigned)
		t0DegC = (((t1_t0_msb & 0b0011) << 8) | device.readUByte(T0_degC_x8 | READ)) / 8f;
		t1DegC = (((t1_t0_msb & 0b1100) << 6) | device.readUByte(T1_degC_x8 | READ)) / 8f;
		
		// Read T0_OUT register
		t0Out = device.readShort(T0_OUT | READ);

		// Read T1_OUT register
		t1Out = device.readShort(T1_OUT | READ);
		
		// Read 1 byte of data from address 0x3E(62)
		byte lsb = device.readByte(T1_OUT | READ);
		// Read 1 byte of data from address 0x3F(63)
		byte msb = device.readByte(T1_OUT+1 | READ);
		int T3 = ((msb & 0xFF) << 8) + (lsb & 0xFF);
		// FIXME Delete once verified
		System.out.println("t1_out (using readShort)=" + t1Out + ", T3 (using readByte)=" + T3);
	}
	
	public double getHumidity() {
		// Read raw humidity
		int humidity_raw = device.readShort(HUMIDITY_OUT | READ);
		//int humidity_raw = ((data[1] & 0xFF) * 256) + (data[0] & 0xFF);
		
		return (h1Rh - h0Rh) * (humidity_raw - h0T0Out) / (h1T0Out - h0T0Out) + h0Rh;
	}
	
	/**
	 * Get temperature (degrees C).
	 * @return Temperature in degrees C.
	 */
	public double getTemperature() {
		// Read raw temperature
		int temp_raw = device.readShort(TEMP_OUT | READ);
		/*int temp = ((data[3] & 0xFF) * 256) + (data[2] & 0xFF);
		if (temp > 32767) {
			temp -= 65536;
		}*/
		
		return (t1DegC - t0DegC) * (temp_raw - t0Out) / (t1Out - t0Out) + t0DegC;
	}
}
