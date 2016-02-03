package com.diozero;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.api.I2CConstants;
import com.diozero.api.I2CDevice;
import com.diozero.api.TemperaturePressureSensorInterface;
import com.diozero.util.IOUtil;
import com.diozero.util.SleepUtil;

@SuppressWarnings("unused")
public class BMP180 extends I2CDevice implements TemperaturePressureSensorInterface {
	private static final Logger logger = LogManager.getLogger(BMP180.class);
	
	public static void main(String[] args) {
		try (BMP180 bmp180 = new BMP180(1, I2CConstants.ADDR_SIZE_7, I2CConstants.DEFAULT_CLOCK_FREQUENCY)) {
			bmp180.init(BMPMode.STANDARD);
			logger.debug("Opened device");

			double temp = bmp180.getTemperature();
			System.out.format("Temperature=%.2f%n", Double.valueOf(temp));
			double pressure = bmp180.getPressure();
			System.out.format("Pressure=%.2f%n", Double.valueOf(pressure));
			
			bmp180.close();
		} catch (IOException ioe) {
			logger.error("Error: " + ioe, ioe);
		}
	}
	
	/**
	 * Device address BMP180 address is 0x77
	 */
	private static final int BMP180_ADDR = 0x77;

	/**
	 * Total number of bytes used for calibration
	 */
	private static final int CALIBRATION_BYTES = 22;

	// BMP085 EEPROM Registers
	private static final int EEPROM_START = 0xAA;
	private static final int BMP085_CAL_AC1 = 0xAA;
	private static final int BMP085_CAL_AC2 = 0xAC;
	private static final int BMP085_CAL_AC3 = 0xAE;
	private static final int BMP085_CAL_AC4 = 0xB0;
	private static final int BMP085_CAL_AC5 = 0xB2;
	private static final int BMP085_CAL_AC6 = 0xB4;
	private static final int BMP085_CAL_B1 = 0xB6;
	private static final int BMP085_CAL_B2 = 0xB8;
	private static final int BMP085_CAL_MB = 0xBA;
	private static final int BMP085_CAL_MC = 0xBC;
	private static final int BMP085_CAL_MD = 0xBE;
	private static final int BMP085_CONTROL = 0xF4;
	private static final int CONTROL_REGISTER = 0xF4; // BMP180 control register
	private static final int BMP085_TEMPDATA = 0xF6;
	private static final int TEMP_ADDR = 0xF6; // Temperature read address
	private static final int BMP085_PRESSUREDATA = 0xF6;
	private static final int PRESS_ADDR = 0xF6; // Pressure read address

	// Commands
	private static final byte GET_TEMP_CMD = (byte) 0x2E; // Read temperature command
	private static final byte GET_PRESSURE_COMMAND = (byte) 0x34; // Read pressure command

	/**
	 * EEPROM registers - these represent calibration data
	 */
	private short calAC1;
	private short calAC2;
	private short calAC3;
	private int calAC4;
	private int calAC5;
	private int calAC6;
	private short calB1;
	private short calB2;
	private short calMB;
	private short calMC;
	private short calMD;

	// Barometer configuration
	private BMPMode mode;
	
	public BMP180() throws IOException {
		this(1, I2CConstants.ADDR_SIZE_7, I2CConstants.DEFAULT_CLOCK_FREQUENCY);
	}
	
	public BMP180(int controllerNumber, int addressSize, int clockFrequency) throws IOException {
		super(controllerNumber, BMP180_ADDR, addressSize, clockFrequency);
	}

	/**
	 * This method reads the calibration data common for the Temperature sensor
	 * and Barometer sensor included in the BMP180
	 **/
	public void init(BMPMode mode) throws IOException {
		this.mode = mode;
		
		getCalibrationData();
	}

	/**
	 * Method for reading the calibration data. Do not worry too much about this
	 * method. Normally this information is given in the device information
	 * sheet.
	 *
	 * @throws IOException
	 */
	private void getCalibrationData() throws IOException {
		// Read all of the calibration data into a byte array
		ByteBuffer calibData = ByteBuffer.allocateDirect(CALIBRATION_BYTES);
		read(EEPROM_START, I2CConstants.SUB_ADDRESS_SIZE_1_BYTE, calibData);
		// Read each of the pairs of data as a signed short
		calibData.rewind();
		calAC1 = calibData.getShort();
		calAC2 = calibData.getShort();
		calAC3 = calibData.getShort();

		// Unsigned short values
		calAC4 = IOUtil.getUShort(calibData);
		calAC5 = IOUtil.getUShort(calibData);
		calAC6 = IOUtil.getUShort(calibData);

		// Signed sort values
		calB1 = calibData.getShort();
		calB2 = calibData.getShort();
		calMB = calibData.getShort();
		calMC = calibData.getShort();
		calMD = calibData.getShort();
	}

	private int readRawTemperature() throws IOException {
		// Write the read temperature command to the command register
		writeByte(CONTROL_REGISTER, GET_TEMP_CMD);

		// Wait 5m before reading the temperature
		SleepUtil.sleepMillis(5);

		// Read uncompressed data
		return readUShort(TEMP_ADDR, I2CConstants.SUB_ADDRESS_SIZE_1_BYTE);
	}

	/**
	 * Method for reading the temperature. Remember the sensor will provide us
	 * with raw data, and we need to transform in some analysed value to make
	 * sense. All the calculations are normally provided by the manufacturer. In
	 * our case we use the calibration data collected at construction time.
	 *
	 * @return Temperature in Celsius as a double
	 * @throws IOException
	 *             If there is an IO error reading the sensor
	 */
	@Override
	public double getTemperature() throws IOException {
		int UT = readRawTemperature();

		// Calculate the actual temperature
		int X1 = ((UT - calAC6) * calAC5) >> 15;
		int X2 = (calMC << 11) / (X1 + calMD);
		int B5 = X1 + X2;

		return ((B5 + 8) >> 4) / 10.0;
	}

	private int readRawPressure() throws IOException {
		// Write the read pressure command to the command register
		writeByte(CONTROL_REGISTER, mode.getPressureCommand());

		// Delay before reading the pressure - use the value determined by the
		// sampling mode
		SleepUtil.sleepMillis(mode.getDelay());

		// Read the non-compensated pressure value
		long val = readUInt(PRESS_ADDR, I2CConstants.SUB_ADDRESS_SIZE_1_BYTE, 3);

		// ((msb << 16) + (lsb << 8) + xlsb) >> (8 - self._mode)
		
		return (int)(val >> (8 - mode.getSamplingMode()));
	}

	/**
	 * Read the barometric pressure (in hPa) from the device.
	 *
	 * @return double Pressure measurement in hPa
	 */
	@Override
	public double getPressure() throws IOException {
		int sampling_mode = mode.getSamplingMode();
		int UT = readRawTemperature();
		int UP = readRawPressure();

		// Calculate true temperature coefficient B5.
		int X1 = ((UT - calAC6) * calAC5) >> 15;
		int X2 = (calMC << 11) / (X1 + calMD);
		int B5 = X1 + X2;

		// Calculate the true pressure
		int B6 = B5 - 4000;
		X1 = (calB2 * (B6 * B6) >> 12) >> 11;
		X2 = calAC2 * B6 >> 11;
		int X3 = X1 + X2;
		int B3 = ((((calAC1 * 4) + X3) << sampling_mode) + 2) / 4;
		X1 = calAC3 * B6 >> 13;
		X2 = (calB1 * ((B6 * B6) >> 12)) >> 16;
		X3 = ((X1 + X2) + 2) >> 2;
		int B4 = (calAC4 * (X3 + 32768)) >> 15;
		int B7 = (UP - B3) * (50000 >> sampling_mode);

		int pa;
		if (B7 < 0x80000000) {
			pa = (B7 * 2) / B4;
		} else {
			pa = (B7 / B4) * 2;
		}

		X1 = (pa >> 8) * (pa >> 8);
		X1 = (X1 * 3038) >> 16;
		X2 = (-7357 * pa) >> 16;

		pa += ((X1 + X2 + 3791) >> 4);

		return pa;
	}

	/**
	 * Relationship between sampling mode and conversion delay (in ms) for each
	 * sampling mode Ultra low power: 4.5 ms minimum conversion delay Standard:
	 * 7.5 ms High Resolution: 13.5 ms Ultra high Resolution: 25.5 ms
	 */
	public enum BMPMode {

		ULTRA_LOW_POWER(0, 5), STANDARD(1, 8), HIGH_RESOLUTION(2, 14), ULTRA_HIGH_RESOLUTION(3, 26);

		/**
		 * Sampling mode
		 */
		private final int samplingMode;

		/**
		 * Minimum delay required when reading pressure
		 */
		private final int delay;

		/**
		 * Command byte to read pressure based on sampling mode
		 */
		private final byte pressureCommand;

		/**
		 * Create a new instance of a BMPMode
		 * 
		 * @param samplingMode
		 * @param delay
		 */
		BMPMode(int samplingMode, int delay) {
			this.samplingMode = samplingMode;
			this.delay = delay;
			this.pressureCommand = (byte) (GET_PRESSURE_COMMAND + ((samplingMode << 6) & 0xC0));
		}

		/**
		 * Return the conversion delay (in ms) associated with this sampling
		 * mode
		 *
		 * @return delay
		 */
		public int getDelay() {
			return delay;
		}

		/**
		 * Return the pressure command to the control register for this sampling
		 * mode
		 *
		 * @return command
		 */
		public byte getPressureCommand() {
			return pressureCommand;
		}

		/**
		 * Return this sampling mode
		 *
		 * @return Sampling mode
		 */
		public int getSamplingMode() {
			return samplingMode;
		}
	}
}
