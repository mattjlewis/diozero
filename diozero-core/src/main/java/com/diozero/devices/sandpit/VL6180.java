package com.diozero.devices.sandpit;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     VL6180.java
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

import java.nio.ByteOrder;
import java.util.Date;
import java.util.GregorianCalendar;

import org.tinylog.Logger;

import com.diozero.api.I2CDevice;
import com.diozero.api.I2CDeviceInterface.I2CMessage;
import com.diozero.api.RuntimeIOException;
import com.diozero.devices.DistanceSensorInterface;
import com.diozero.util.SleepUtil;

/**
 * References:
 * <ul>
 * <li><a href=
 * "https://www.st.com/resource/en/datasheet/vl6180.pdf">Datasheet</a></li>
 * <li><a href=
 * "https://www.st.com/resource/en/application_note/dm00122600-vl6180x-basic-ranging-application-note-stmicroelectronics.pdf">Application
 * notes</a></li>
 * <li><a href=
 * "https://github.com/sparkfun/SparkFun_ToF_Range_Finder-VL6180_Arduino_Library/blob/master/src/SparkFun_VL6180X.cpp">SparkFun
 * C++ implementation</a></li>
 * <li><a href=
 * "https://github.com/pololu/vl6180x-arduino/blob/master/VL6180X.cpp">Pololu
 * C++ implementation</a></li>
 * <li><a href=
 * "https://github.com/adafruit/Adafruit_CircuitPython_VL6180X">Adafruit Python
 * implementation</a></li>
 * </ul>
 */
public class VL6180 implements DistanceSensorInterface {
	// Register addresses
	// Identification
	private static final int IDENTIFICATION_MODEL_ID = 0x0000;
	private static final int IDENTIFICATION_MODEL_REV_MAJOR = 0x0001;
	private static final int IDENTIFICATION_MODEL_REV_MINOR = 0x0002;
	private static final int IDENTIFICATION_MODULE_REV_MAJOR = 0x0003;
	private static final int IDENTIFICATION_MODULE_REV_MINOR = 0x0004;
	private static final int IDENTIFICATION_DATE = 0x0006; // 16-bit value
	private static final int IDENTIFICATION_TIME = 0x0008; // 16-bit value
	// System setup
	private static final int SYSTEM_MODE_GPIO0 = 0x0010;
	private static final int SYSTEM_MODE_GPIO1 = 0x0011;
	private static final int SYSTEM_HISTORY_CTRL = 0x0012;
	private static final int SYSTEM_INTERRUPT_CONFIG_GPIO = 0x0014;
	private static final int SYSTEM_INTERRUPT_CLEAR = 0x0015;
	private static final int SYSTEM_FRESH_OUT_OF_RESET = 0x0016;
	private static final int SYSTEM_GROUPED_PARAMETER_HOLD = 0x0017;
	// Range setup
	private static final int SYSRANGE_START = 0x0018;
	private static final int SYSRANGE_THRESH_HIGH = 0x0019;
	private static final int SYSRANGE_THRESH_LOW = 0x001A;
	private static final int SYSRANGE_INTERMEASUREMENT_PERIOD = 0x001B;
	private static final int SYSRANGE_MAX_CONVERGENCE_TIME = 0x001C;
	private static final int SYSRANGE_CROSSTALK_COMPENSATION_RATE = 0x001E;
	private static final int SYSRANGE_CROSSTALK_VALID_HEIGHT = 0x0021;
	private static final int SYSRANGE_EARLY_CONVERGENCE_ESTIMATE = 0x0022;
	private static final int SYSRANGE_PART_TO_PART_RANGE_OFFSET = 0x0024;
	private static final int SYSRANGE_RANGE_IGNORE_VALID_HEIGHT = 0x0025;
	private static final int SYSRANGE_RANGE_IGNORE_THRESHOLD = 0x0026;
	private static final int SYSRANGE_MAX_AMBIENT_LEVEL_MULT = 0x002C;
	private static final int SYSRANGE_RANGE_CHECK_ENABLES = 0x002D;
	private static final int SYSRANGE_VHV_RECALIBRATE = 0x002E;
	private static final int SYSRANGE_VHV_REPEAT_RATE = 0x0031;
	// Ambient Light Sensor
	private static final int SYSALS_START = 0x0038;
	private static final int SYSALS_THRESH_HIGH = 0x003A;
	private static final int SYSALS_THRESH_LOW = 0x003C;
	private static final int SYSALS_INTERMEASUREMENT_PERIOD = 0x003E;
	private static final int SYSALS_ANALOGUE_GAIN = 0x003F;
	private static final int SYSALS_INTEGRATION_PERIOD = 0x0040;
	// Results
	private static final int RESULT_RANGE_STATUS = 0x004D;
	private static final int RESULT_ALS_STATUS = 0x004E;
	private static final int RESULT_INTERRUPT_STATUS_GPIO = 0x004F;
	private static final int RESULT_ALS_VAL = 0x0050;
	private static final int RESULT_HISTORY_BUFFER = 0x0052;
	private static final int RESULT_RANGE_VAL = 0x0062;
	private static final int RESULT_RANGE_RAW = 0x0064;
	private static final int RESULT_RANGE_RETURN_RATE = 0x0066;
	private static final int RESULT_RANGE_REFERENCE_RATE = 0x0068;
	private static final int RESULT_RANGE_RETURN_SIGNAL_COUNT = 0x006C;
	private static final int RESULT_RANGE_REFERENCE_SIGNAL_COUNT = 0x0070;
	private static final int RESULT_RANGE_RETURN_AMB_COUNT = 0x0074;
	private static final int RESULT_RANGE_REFERENCE_AMB_COUNT = 0x0078;
	private static final int RESULT_RANGE_RETURN_CONV_TIME = 0x007C;
	private static final int RESULT_RANGE_REFERENCE_CONV_TIME = 0x0080;

	private static final int READOUT_AVERAGING_SAMPLE_PERIOD = 0x010A;
	private static final int FIRMWARE_BOOTUP = 0x0119;
	private static final int FIRMWARE_RESULT_SCALER = 0x0120;
	private static final int I2C_SLAVE_DEVICE_ADDRESS = 0x0212;
	private static final int INTERLEAVED_MODE_ENABLE = 0x02A3;

	private static final int DEFAULT_ADDRESS = 0x29;
	public static final byte VL6180_MODEL_ID = (byte) 0xb4;

	private I2CDevice device;
	private short modelId;
	private short modelMajor;
	private short modelMinor;
	private short moduleMajor;
	private short moduleMinor;
	private Date manufactureDateTime;
	private int manufacturePhase;

	public VL6180() {
		device = I2CDevice.builder(DEFAULT_ADDRESS).setByteOrder(ByteOrder.BIG_ENDIAN).build();

		init();

		modelId = (short) (readByte(IDENTIFICATION_MODEL_ID) & 0xff);
		modelMajor = (short) (readByte(IDENTIFICATION_MODEL_REV_MAJOR) & 0x07);
		modelMinor = (short) (readByte(IDENTIFICATION_MODEL_REV_MINOR) & 0x07);
		moduleMajor = (short) (readByte(IDENTIFICATION_MODULE_REV_MAJOR) & 0x07);
		moduleMinor = (short) (readByte(IDENTIFICATION_MODULE_REV_MINOR) & 0x07);
		int id_date = readShort(IDENTIFICATION_DATE) & 0xffff;
		int year = (id_date >> 12) & 0xf;
		int month = (id_date >> 8) & 0xf;
		int day_of_month = (id_date >> 3) & 0x1f;
		manufacturePhase = id_date & 0x07;
		int time = (readShort(IDENTIFICATION_TIME) & 0xffff) * 2;
		manufactureDateTime = new GregorianCalendar(2010 + year, month - 1, day_of_month - 1, time / 60 / 60,
				(time / 60) % 60, time % 60).getTime();
	}

	public int getModelId() {
		return modelId;
	}

	public short getModelMajor() {
		return modelMajor;
	}

	public short getModelMinor() {
		return modelMinor;
	}

	public short getModuleMajor() {
		return moduleMajor;
	}

	public short getModuleMinor() {
		return moduleMinor;
	}

	public Date getManufactureDateTime() {
		return manufactureDateTime;
	}

	public int getManufacturePhase() {
		return manufacturePhase;
	}

	private void init() {
		if (readByte(SYSTEM_FRESH_OUT_OF_RESET) == 1) {
			Logger.debug("initialising...");
			// Mandatory : private registers
			writeByte(0x207, 0x01);
			writeByte(0x208, 0x01);
			writeByte(0x096, 0x00);
			writeByte(0x097, 0xFD);
			writeByte(0x0E3, 0x01);
			writeByte(0x0E4, 0x03);
			writeByte(0x0E5, 0x02);
			writeByte(0x0E6, 0x01);
			writeByte(0x0E7, 0x03);
			writeByte(0x0F5, 0x02);
			writeByte(0x0D9, 0x05);
			writeByte(0x0DB, 0xCE);
			writeByte(0x0DC, 0x03);
			writeByte(0x0DD, 0xF8);
			writeByte(0x09F, 0x00);
			writeByte(0x0A3, 0x3C);
			writeByte(0x0B7, 0x00);
			writeByte(0x0BB, 0x3C);
			writeByte(0x0B2, 0x09);
			writeByte(0x0CA, 0x09);
			writeByte(0x198, 0x01);
			writeByte(0x1B0, 0x17);
			writeByte(0x1AD, 0x00);
			writeByte(0x0FF, 0x05);
			writeByte(0x100, 0x05);
			writeByte(0x199, 0x05);
			writeByte(0x1A6, 0x1B);
			writeByte(0x1AC, 0x3E);
			writeByte(0x1A7, 0x1F);
			writeByte(0x030, 0x00);
		}

		writeByte(SYSTEM_FRESH_OUT_OF_RESET, 0);
		SleepUtil.sleepMillis(10);

		// Recommended : Public registers - See data sheet for more detail
		// Enables polling for 'New Sample ready' when measurement completes
		writeByte(SYSTEM_MODE_GPIO1, 0x10);
		// Set the averaging sample period (compromise between lower noise and increased
		// execution time)
		writeByte(READOUT_AVERAGING_SAMPLE_PERIOD, 0x30);
		// Sets the light and dark gain (upper nibble). Dark gain should not be changed.
		writeByte(SYSALS_ANALOGUE_GAIN, 0x46);
		// Sets the # of range measurements after which auto calibration of system is
		// performed
		writeByte(SYSRANGE_VHV_REPEAT_RATE, 0xFF);
		// Set ALS integration time to 100ms
		writeByte(0x0041, 0x63);
		// Perform a single temperature calibration of the ranging sensor
		// TODO Check for completion by waiting for the bit to be cleared
		writeByte(SYSRANGE_VHV_RECALIBRATE, 0x01);
		// Optional: Public registers - See data sheet for more detail
		// Set default ranging inter-measurement period to 100ms
		writeByte(SYSRANGE_INTERMEASUREMENT_PERIOD, 0x09);
		// Set default ALS inter-measurement period to 500ms
		writeByte(SYSALS_INTERMEASUREMENT_PERIOD, 0x31);
		// Configures interrupt on 'New Sample Ready threshold event'
		// writeByte(SYSTEM_INTERRUPT_CONFIG_GPIO, 0x24);
		writeByte(SYSTEM_INTERRUPT_CONFIG_GPIO, 0x04);
	}

	private byte readByte(int register) {
		byte[] buffer = new byte[3];
		I2CMessage[] messages = new I2CMessage[2];
		messages[0] = new I2CMessage(I2CMessage.I2C_M_WR, 2);
		buffer[0] = (byte) ((register >> 8) & 0xff);
		buffer[1] = (byte) (register & 0xff);
		messages[1] = new I2CMessage(I2CMessage.I2C_M_RD, 1);

		device.readWrite(messages, buffer);

		return buffer[2];
	}

	private short readShort(int register) {
		byte[] buffer = new byte[4];
		I2CMessage[] messages = new I2CMessage[2];
		messages[0] = new I2CMessage(I2CMessage.I2C_M_WR, 2);
		buffer[0] = (byte) ((register >> 8) & 0xff);
		buffer[1] = (byte) (register & 0xff);
		messages[1] = new I2CMessage(I2CMessage.I2C_M_RD, 2);

		device.readWrite(messages, buffer);

		return (short) ((buffer[2] << 8) | (buffer[3] & 0xff));
	}

	private void writeByte(int register, int b) {
		/*-
		byte[] buffer = new byte[3];
		I2CMessage[] messages = new I2CMessage[1];
		messages[0] = new I2CMessage(I2CMessage.I2C_M_WR, 3);
		buffer[0] = (byte) ((register >> 8) & 0xff);
		buffer[1] = (byte) (register & 0xff);
		buffer[2] = (byte) b;
		
		device.readWrite(messages, buffer);
		*/

		byte[] buffer = new byte[3];
		buffer[0] = (byte) ((register >> 8) & 0xff);
		buffer[1] = (byte) (register & 0xff);
		buffer[2] = (byte) b;
		device.writeBytes(buffer);
	}

	private void writeShort(int register, int s) {
		/*-
		byte[] buffer = new byte[4];
		I2CMessage[] messages = new I2CMessage[1];
		messages[0] = new I2CMessage(I2CMessage.I2C_M_WR, 4);
		buffer[0] = (byte) ((register >> 8) & 0xff);
		buffer[1] = (byte) (register & 0xff);
		buffer[2] = (byte) ((s >> 8) & 0xff);
		buffer[3] = (byte) (s & 0xff);
		
		device.readWrite(messages, buffer);
		*/

		byte[] buffer = new byte[4];
		buffer[0] = (byte) ((register >> 8) & 0xff);
		buffer[1] = (byte) (register & 0xff);
		buffer[2] = (byte) ((s >> 8) & 0xff);
		buffer[3] = (byte) (s & 0xff);
		device.writeBytes(buffer);
	}

	@Override
	public void close() {
		device.close();
	}

	@Override
	public float getDistanceCm() throws RuntimeIOException {
		writeByte(SYSTEM_INTERRUPT_CLEAR, 0b101);

		// Start single-shot mode
		writeByte(SYSRANGE_START, 0b01);
		byte interrupt_status;
		long start_ms = System.currentTimeMillis();
		do {
			interrupt_status = readByte(RESULT_INTERRUPT_STATUS_GPIO);
		} while ((interrupt_status & 0x04) == 0 && System.currentTimeMillis() - start_ms < 1000);

		return (readByte(RESULT_RANGE_VAL) & 0xff) / 10f;
	}
}
