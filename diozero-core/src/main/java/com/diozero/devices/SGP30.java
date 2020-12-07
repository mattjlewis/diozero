package com.diozero.devices;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     SGP30.java  
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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.tinylog.Logger;

import com.diozero.api.I2CDevice;
import com.diozero.util.Crc;
import com.diozero.util.DiozeroScheduler;
import com.diozero.util.SleepUtil;

/*!
 * https://www.sensirion.com/en/environmental-sensors/gas-sensors/sgp30/
 * Datasheet: https://www.sensirion.com/fileadmin/user_upload/customers/sensirion/Dokumente/9_Gas_Sensors/Datasheets/Sensirion_Gas_Sensors_SGP30_Datasheet.pdf
 * Sensirion C: https://github.com/Sensirion/embedded-sgp/tree/master/sgp30
 * Pimoroni Python: https://github.com/pimoroni/sgp30-python
 * Sparkfun Arduino: https://github.com/sparkfun/SparkFun_SGP30_Arduino_Library
 * Adafruit Arduino: https://github.com/adafruit/Adafruit_SGP30
 */
public class SGP30 implements Closeable, Runnable {
	public static final int PRODUCT_TYPE = 0;

	public static final int I2C_ADDRESS = 0x58;

	private static final Crc.Params CRC8_PARAMS = new Crc.Params(0x31, 0xff, false, false, 0x00);
	
	/* command and constants for reading the serial ID */
	private static final short CMD_GET_SERIAL_ID = 0x3682;
	private static final int CMD_GET_SERIAL_ID_WORDS = 3;
	private static final int CMD_GET_SERIAL_ID_DELAY_MS = 1;

	/* command and constants for reading the featureset version */
	private static final short CMD_GET_FEATURESET = 0x202f;
	private static final int CMD_GET_FEATURESET_WORDS = 1;
	private static final int CMD_GET_FEATURESET_DELAY_MS = 10;

	/* command and constants for on-chip self-test */
	private static final short CMD_MEASURE_TEST = 0x2032;
	private static final int CMD_MEASURE_TEST_WORDS = 1;
	private static final int CMD_MEASURE_TEST_DELAY_MS = 220;
	private static final int CMD_MEASURE_TEST_OK = 0xd400;

	/* command and constants for IAQ init */
	private static final short CMD_IAQ_INIT = 0x2003;
	private static final int CMD_IAQ_INIT_DELAY_MS = 10;

	/* command and constants for IAQ measure */
	private static final short CMD_IAQ_MEASURE = 0x2008;
	private static final int CMD_IAQ_MEASURE_WORDS = 2;
	private static final int CMD_IAQ_MEASURE_DELAY_MS = 12;

	/* command and constants for getting IAQ baseline */
	private static final short CMD_GET_IAQ_BASELINE = 0x2015;
	private static final int CMD_GET_IAQ_BASELINE_WORDS = 2;
	private static final int CMD_GET_IAQ_BASELINE_DELAY_MS = 10;

	/* command and constants for setting IAQ baseline */
	private static final short CMD_SET_IAQ_BASELINE = 0x201e;
	private static final int CMD_SET_IAQ_BASELINE_DELAY_MS = 10;

	/* command and constants for raw measure */
	private static final short CMD_RAW_MEASURE = 0x2050;
	private static final int CMD_RAW_MEASURE_WORDS = 2;
	private static final int CMD_RAW_MEASURE_DELAY_MS = 25;

	/* command and constants for setting absolute humidity */
	private static final short CMD_SET_ABSOLUTE_HUMIDITY = 0x2061;
	private static final int CMD_SET_ABSOLUTE_HUMIDITY_DELAY_MS = 10;

	/* command and constants for getting TVOC inceptive baseline */
	private static final short CMD_GET_TVOC_INCEPTIVE_BASELINE = 0x20b3;
	private static final int CMD_GET_TVOC_INCEPTIVE_BASELINE_WORDS = 1;
	private static final int CMD_GET_TVOC_INCEPTIVE_BASELINE_DELAY_MS = 10;

	/* command and constants for setting TVOC baseline */
	private static final short CMD_SET_TVOC_BASELINE = 0x2077;
	private static final int CMD_SET_TVOC_BASELINE_DELAY_MS = 10;

	private I2CDevice device;
	private long startTimeMs;
	private ScheduledFuture<?> future;
	private SGP30Measurement lastMeasurement;
	private Consumer<SGP30Measurement> measurementListener;

	public SGP30(int controller) {
		this(controller, I2C_ADDRESS);
	}

	public SGP30(int controller, int address) {
		device = I2CDevice.builder(address).setController(controller).setByteOrder(ByteOrder.BIG_ENDIAN).build();
	}

	public void start(Consumer<SGP30Measurement> consumer) {
		this.measurementListener = consumer;
		Logger.debug("start");
		iaqInit();
		// According to the datasheet there is a measurement immediately after init
		measureIaq();
		startTimeMs = System.currentTimeMillis();
		future = DiozeroScheduler.getDaemonInstance().scheduleAtFixedRate(this, 1000, 1000, TimeUnit.MILLISECONDS);
	}

	public void stop() {
		if (future != null) {
			future.cancel(true);
		}
		future = null;
		startTimeMs = 0;
	}

	@Override
	public void run() {
		int seconds_since_start = (int) ((System.currentTimeMillis() - startTimeMs) / 1000);
		Logger.debug("Getting measurement @{}s{}", Integer.valueOf(seconds_since_start),
				(seconds_since_start <= 15) ? " (initialising)" : "");

		lastMeasurement = measureIaq();
		Logger.debug("Measurement: {}", lastMeasurement);
		if (measurementListener != null) {
			measurementListener.accept(lastMeasurement);
		}
	}

	public SGP30Measurement getLastMeasurement() {
		return lastMeasurement;
	}

	public FeatureSetVersion getFeatureSetVersion() {
		return new FeatureSetVersion(
				command(CMD_GET_FEATURESET, CMD_GET_FEATURESET_WORDS, CMD_GET_FEATURESET_DELAY_MS));
	}

	public long getSerialId() {
		int[] response = command(CMD_GET_SERIAL_ID, CMD_GET_SERIAL_ID_WORDS, CMD_GET_SERIAL_ID_DELAY_MS);

		return (response[0] << 32) | (response[1] << 16) | response[2];
	}

	public void measureTest() {
		int[] response = command(CMD_MEASURE_TEST, CMD_MEASURE_TEST_WORDS, CMD_MEASURE_TEST_DELAY_MS);
		if (response[0] == CMD_MEASURE_TEST_OK) {
			Logger.info("measureTest success");
		} else {
			Logger.error("measureTest error, expected {}, got {}", Integer.valueOf(CMD_MEASURE_TEST_OK),
					Integer.valueOf(response[0]));
		}
	}

	private void iaqInit() {
		command(CMD_IAQ_INIT, 0, CMD_IAQ_INIT_DELAY_MS);
	}

	private SGP30Measurement measureIaq() {
		return new SGP30Measurement(command(CMD_IAQ_MEASURE, CMD_IAQ_MEASURE_WORDS, CMD_IAQ_MEASURE_DELAY_MS));
	}

	public RawMeasurement rawMeasurement() {
		return new RawMeasurement(command(CMD_RAW_MEASURE, CMD_RAW_MEASURE_WORDS, CMD_RAW_MEASURE_DELAY_MS));
	}

	public void setHumidityCompensation(short humidity) {
		// Can only be set after iaq_init, can also be set between measurements
		command(CMD_SET_ABSOLUTE_HUMIDITY, 0, CMD_SET_ABSOLUTE_HUMIDITY_DELAY_MS, humidity);
	}

	public SGP30Measurement getIaqBaseline() {
		return new SGP30Measurement(
				command(CMD_GET_IAQ_BASELINE, CMD_GET_IAQ_BASELINE_WORDS, CMD_GET_IAQ_BASELINE_DELAY_MS));
	}

	/*
	 */
	public void setIaqBaseline(SGP30Measurement baseline) {
		if (future != null) {
			// Can only be sent after iaq_init and before the first iaq_measure
			Logger.error("IAQ baseline can only be sent after iaq_init and before the first iaq_measure");
			// TODO Error?
		}
		command(CMD_SET_IAQ_BASELINE, 0, CMD_SET_IAQ_BASELINE_DELAY_MS, baseline.getTotalVOC(),
				baseline.getCO2Equivalent());
	}

	public int getTvocInceptiveBaseline() {
		return command(CMD_GET_TVOC_INCEPTIVE_BASELINE, CMD_GET_TVOC_INCEPTIVE_BASELINE_WORDS,
				CMD_GET_TVOC_INCEPTIVE_BASELINE_DELAY_MS)[0] & 0xffff;
	}

	public void setTvocInceptiveBaseline(short baseline) {
		command(CMD_SET_TVOC_BASELINE, 0, CMD_SET_TVOC_BASELINE_DELAY_MS, baseline);
	}

	private synchronized int[] command(short command, int responseLength, int delayMs, int... dataWords) {
		ByteBuffer buffer = ByteBuffer.allocate(2 + dataWords.length * 3);
		buffer.order(ByteOrder.BIG_ENDIAN);
		buffer.putShort(command);
		if (dataWords.length > 0) {
			for (int i = 0; i < dataWords.length; i++) {
				short data = (short) dataWords[i];
				buffer.putShort(data);
				buffer.put((byte) Crc.crc8(CRC8_PARAMS, data));
			}
		}
		buffer.rewind();
		device.writeBytes(buffer);

		SleepUtil.sleepMillis(delayMs);

		int[] response = new int[responseLength];
		if (responseLength > 0) {
			buffer = ByteBuffer.allocateDirect(3 * responseLength);
			buffer = device.readBytesAsByteBuffer(3 * responseLength);
			for (int i = 0; i < responseLength; i++) {
				int data = buffer.getShort() & 0xffff;
				int crc = buffer.get() & 0xff;
				int calc_crc = Crc.crc8(CRC8_PARAMS, (short) data);
				if (calc_crc != crc) {
					// TODO Throw a runtime I/O error?
					Logger.error("CRC mismatch: got: {}, calculated: {}", Integer.valueOf(crc),
							Integer.valueOf(calc_crc));
				}
				response[i] = data;
			}
		}

		return response;
	}

	@Override
	public void close() {
		try {
			device.close();
		} catch (Exception e) {
			// Ignore
		}
	}

	public static final class FeatureSetVersion {
		int productType;
		int productVersion;

		public FeatureSetVersion(int[] raw) {
			this((raw[0] >> 12) & 0xf, raw[0] & 0xFF);
		}

		public FeatureSetVersion(int productType, int productVersion) {
			this.productType = productType;
			this.productVersion = productVersion;
		}

		public int getProductType() {
			return productType;
		}

		public int getProductVersion() {
			return productVersion;
		}

		@Override
		public String toString() {
			return String.format("FeatureSetVersion [productType=0x%02X, productVersion=0x%02X]",
					Integer.valueOf(productType), Integer.valueOf(productVersion));
		}
	}

	public static final class SGP30Measurement {
		int co2Equivalent;
		// Total Volatile Organic Compounds
		int totalVOC;

		public SGP30Measurement(int[] raw) {
			co2Equivalent = raw[0];
			totalVOC = raw[1];
		}

		public int getCO2Equivalent() {
			return co2Equivalent;
		}

		public int getTotalVOC() {
			return totalVOC;
		}

		@Override
		public String toString() {
			return "SGP30Measurement [CO2 Equivalent=" + co2Equivalent + ", Total VOC=" + totalVOC + "]";
		}
	}

	public static final class RawMeasurement {
		private int h2;
		private int ethanol;

		public RawMeasurement(int[] raw) {
			h2 = raw[0];
			ethanol = raw[1];
		}

		public int getH2() {
			return h2;
		}

		public int getEthanol() {
			return ethanol;
		}

		@Override
		public String toString() {
			return "RawMeasurement [h2=" + h2 + ", ethanol=" + ethanol + "]";
		}
	}
}
