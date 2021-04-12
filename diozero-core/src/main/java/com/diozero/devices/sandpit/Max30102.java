/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     Max30102.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2021 diozero
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

package com.diozero.devices.sandpit;

import com.diozero.api.DeviceInterface;
import com.diozero.api.I2CConstants;
import com.diozero.api.I2CDevice;
import com.diozero.api.RuntimeIOException;
import com.diozero.util.RangeUtil;
import com.diozero.util.SleepUtil;

/**
 * maxim integrated High-sensitivity Pulse Oximeter and Heart-rate Sensor.
 * <a href=
 * "https://datasheets.maximintegrated.com/en/ds/MAX30102.pdf">Datasheet</a>
 * 
 * <pre>
 * INT  IRD  RD   GND
 * GND  SCL  SDA  VIN
 * 
 * INT: Interrupt pin
 * IRD: The IR LED ground of the MAX30102 chip, generally not connected
 * RD: RED LED ground terminal of MAX30102 chip, generally not connected
 * GND: Ground
 * SCL: I2C Clock
 * SDA: I2C Data
 * VIN: The main power input terminal 1.8-5V
 * 
 * 3-bit pad: Select the pull-up level of the bus, depending on the pin master voltage,
 * select 1.8v or 3_3v (this terminal contains 3.3V and above)
 * </pre>
 * 
 * The active-low interrupt pin pulls low when an interrupt is triggered. The
 * pin is open-drain, meaning it requires a pull-up resistor (min 4.7kOhm).
 * 
 * Credit to:
 * https://makersportal.com/blog/2019/6/24/arduino-heart-rate-monitor-using-max30102-and-pulse-oximetry
 * https://github.com/sparkfun/SparkFun_MAX3010x_Sensor_Library
 * https://github.com/doug-burrell/max30102/blob/master/max30102.py
 */
public class Max30102 implements DeviceInterface {
	public static final int FIFO_ALMOST_FULL_MAX = 15;
	public static final float LED_MAX_CURRENT_MA = 51f;

	// Default device address
	private static int DEVICE_ADDRESS = 0x57;

	// Registers
	private static final int REG_INTR_STATUS_1 = 0x00;
	private static final int REG_INTR_STATUS_2 = 0x01;
	private static final int REG_INTR_ENABLE_1 = 0x02;
	private static final int REG_INTR_ENABLE_2 = 0x03;
	private static final int REG_FIFO_WRITE_PTR = 0x04;
	private static final int REG_OVERFLOW_CTR = 0x05;
	private static final int REG_FIFO_READ_PTR = 0x06;
	private static final int REG_FIFO_DATA = 0x07;
	private static final int REG_FIFO_CONFIG = 0x08;
	private static final int REG_MODE_CONFIG = 0x09;
	private static final int REG_SPO2_CONFIG = 0x0a;
	private static final int REG_LED1_PULSE_AMPL = 0x0c;
	private static final int REG_LED2_PULSE_AMPL = 0x0d;
	private static final int REG_MULTI_LED_CTRL1 = 0x11;
	private static final int REG_MULTI_LED_CTRL2 = 0x12;
	private static final int REG_DIE_TEMP_INT = 0x1f;
	private static final int REG_DIE_TEMP_FRC = 0x20;
	private static final int REG_DIE_TEMP_CONFIG = 0x21;
	private static final int REG_REVISION_ID = 0xfe;
	private static final int REG_PART_ID = 0xff;

	private static final byte PART_ID = 0x15;

	// Interrupt Status / Enable #1 (0x00 / 0x02)

	private static final int INT1_FIFO_ALMOST_FULL_BIT = 7;
	/**
	 * FIFO Almost Full Flag bit mask.
	 * 
	 * In SpO2 and HR modes, this interrupt triggers when the FIFO write pointer has
	 * a certain number of free spaces remaining. The trigger can be set by the
	 * FIFO_A_FULL[3:0] register. The interrupt is cleared by reading the Interrupt
	 * Status 1 register (0x00).
	 */
	private static final int INT1_FIFO_ALMOST_FULL_MASK = 1 << INT1_FIFO_ALMOST_FULL_BIT;

	private static final int INT1_PPG_RDY_BIT = 6;
	/**
	 * New FIFO Data Ready bit mask.
	 * 
	 * In SpO2 and HR modes, this interrupt triggers when there is a new sample in
	 * the data FIFO. The interrupt is cleared by reading the Interrupt Status 1
	 * register (0x00), or by reading the FIFO_DATA register.
	 */
	private static final byte INT1_PPG_RDY_DATA_MASK = 1 << INT1_PPG_RDY_BIT;

	private static final int INT1_ALC_OVF_BIT = 5;
	/**
	 * Ambient Light Cancellation Overflow bit mask.
	 * 
	 * Ambient Light Cancellation Overflow This interrupt triggers when the ambient
	 * light cancellation function of the SpO2/HR photodiode has reached its maximum
	 * limit, and therefore, ambient light is affecting the output of the ADC. The
	 * interrupt is cleared by reading the Interrupt Status 1 register (0x00).
	 */
	private static final byte INT1_ALC_OVF_MASK = 1 << INT1_ALC_OVF_BIT;

	private static final int INT1_PWR_RDY_BIT = 0;
	/**
	 * Power Ready Flag bit mask.
	 * 
	 * On power-up a power-ready interrupt is triggered to signal that the module is
	 * powered-up and ready to collect data.
	 * 
	 * Note can only be read from the Interrupt Status 1 register, cannot be written
	 * to the Interrupt Enable 1 register.
	 */
	private static final byte INT1_PWR_RDY_MASK = 1 << INT1_PWR_RDY_BIT;

	// Interrupt Status #2 (0x01 / 0x03)

	private static final int INT2_DIE_TEMP_RDY_BIT = 1;
	/**
	 * Internal Temperature Ready Flag bit mask.
	 *
	 * When an internal die temperature conversion is finished, this interrupt is
	 * triggered so the processor can read the temperature data registers. The
	 * interrupt is cleared by reading either the Interrupt Status 2 register (0x01)
	 * or the TFRAC register (0x20).
	 */
	private static final byte INT2_DIE_TEMP_RDY_MASK = 1 << INT2_DIE_TEMP_RDY_BIT;

	// FIFO Config (0x08)

	public static enum SampleAveraging {
		_1(0b000), _2(0b001), _4(0b010), _8(0b011), _16(0b100), _32(0b101);

		private static final int BIT_SHIFT = 5;

		private byte mask;

		private SampleAveraging(int val) {
			this.mask = (byte) (val << BIT_SHIFT);
		}

		byte getMask() {
			return mask;
		}
	}

	/**
	 * Controls the behaviour of the FIFO when it becomes completely filled with
	 * data
	 */
	public static enum FifoRolloverOnFull {
		/**
		 * The FIFO address rolls over to zero and the FIFO continues to fill with new
		 * data
		 */
		ENABLED(1),
		/**
		 * The FIFO is not updated until FIFO_DATA is read or the WRITE/READ positions
		 * are changed
		 */
		DISABLED(0);

		private static final int BIT_SHIFT = 4;

		private byte mask;

		private FifoRolloverOnFull(int val) {
			this.mask = (byte) (val << BIT_SHIFT);
		}

		byte getMask() {
			return mask;
		}
	}

	// Mode Configuration (0x09)

	private static final int MODE_CONFIG_SHUTDOWN_BIT = 7;
	private static final int MODE_CONFIG_SHUTDOWN_MASK = 1 << MODE_CONFIG_SHUTDOWN_BIT;
	private static final int MODE_CONFIG_RESET_BIT = 6;
	private static final byte MODE_CONFIG_RESET_MASK = 1 << MODE_CONFIG_RESET_BIT;

	public static enum Mode {
		HEART_RATE(0b010), SPO2(0b011), MULTI_LED(0b011);

		private static final int BIT_SHIFT = 0;

		private byte mask;

		private Mode(int val) {
			this.mask = (byte) (val << BIT_SHIFT);
		}

		byte getMask() {
			return mask;
		}
	}

	// SpO2 Configuration (0x0A)

	public static enum SpO2AdcRange {
		_1(0b00, 7.81f, 2048), _2(0b01, 15.63f, 4096), _3(0b10, 31.25f, 8192), _4(0b11, 62.5f, 16384);

		private static final int BIT_SHIFT = 5;

		private byte mask;
		private float lsbSize;
		private int fullScale;

		private SpO2AdcRange(int val, float lsbSize, int fullScale) {
			this.mask = (byte) (val << BIT_SHIFT);
			this.lsbSize = lsbSize;
			this.fullScale = fullScale;
		}

		byte getMask() {
			return mask;
		}

		public float getLsbSize() {
			return lsbSize;
		}

		public int getFullScale() {
			return fullScale;
		}
	}

	public static enum SpO2SampleRate {
		_50(0b000, 50), _100(0b001, 100), _200(0b010, 200), _400(0b011, 400), _800(0b100, 800), _1000(0b101, 1000),
		_1600(0b110, 1600), _3200(0b111, 3200);

		private static final int BIT_SHIFT = 2;

		private byte mask;
		private int sampleRate;

		private SpO2SampleRate(int val, int sampleRate) {
			this.mask = (byte) (val << BIT_SHIFT);
			this.sampleRate = sampleRate;
		}

		byte getMask() {
			return mask;
		}

		public int getSampleRate() {
			return sampleRate;
		}
	}

	public static enum LedPulseWidth {
		_69(0b00, 68.95f, 15), _118(0b01, 117.78f, 16), _215(0b10, 215.44f, 17), _411(0b11, 410.75f, 18);

		private static final int BIT_SHIFT = 0;

		private byte mask;
		private float pulseWidthUs;
		private int adcResolution;

		private LedPulseWidth(int val, float pulseWidthUs, int adcResolution) {
			this.mask = (byte) (val << BIT_SHIFT);
			this.pulseWidthUs = pulseWidthUs;
			this.adcResolution = adcResolution;
		}

		byte getMask() {
			return mask;
		}

		public float getPulseWidthUs() {
			return pulseWidthUs;
		}

		public int getAdcResolution() {
			return adcResolution;
		}
	}

	// Die Temperate Config (0x21)
	private static final int DIE_TEMP_EN_BIT = 0;
	private static final int DIE_TEMP_EN_MASK = 1 << DIE_TEMP_EN_BIT;

	private I2CDevice device;

	public Max30102() {
		this(I2CConstants.CONTROLLER_0);
	}

	public Max30102(int controller) {
		device = I2CDevice.builder(DEVICE_ADDRESS).setController(controller).build();

		// Validate the part id
		byte part_id = getPartId();
		if (part_id != PART_ID) {
			throw new RuntimeIOException("Unexpected part id: " + part_id + ", expected: " + PART_ID);
		}

		// Note that resetting the device does not trigger a PWR_RDY interrupt event
		reset();

		// Reading the interrupt registers clears the interrupt status
		byte intr_1 = device.readByteData(REG_INTR_STATUS_1);
		byte intr_2 = device.readByteData(REG_INTR_STATUS_2);
	}

	@Override
	public void close() {
		shutdown();
		device.close();
	}

	public void reset() {
		// Send the reset bit only
		device.writeByteData(REG_MODE_CONFIG, MODE_CONFIG_RESET_MASK);

		// Poll for the reset bit to clear, timeout after 100ms
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start < 100) {
			if ((device.readByteData(REG_MODE_CONFIG) & MODE_CONFIG_RESET_MASK) == 0) {
				break;
			}
			SleepUtil.sleepMillis(1);
		}
	}

	public void shutdown() {
		// Send the shutdown bit only
		device.writeByteData(REG_MODE_CONFIG, MODE_CONFIG_SHUTDOWN_MASK);
	}

	public void wakeup() {
		device.writeByteData(REG_MODE_CONFIG, 0x00);
	}

	public byte getRevisionId() {
		return device.readByteData(REG_REVISION_ID);
	}

	public byte getPartId() {
		return device.readByteData(REG_PART_ID);
	}

	/**
	 * Setup the device.
	 * 
	 * * Note actual measured current can vary widely due to trimming methodology
	 * 
	 * @param sampleAveraging     number of samples averaged per FIFO sample
	 * @param fifoRolloverOnFull  whether or not the FIFO rolls over when full
	 * @param fifoAlmostFullValue set the number of data samples remaining in the
	 *                            FIFO when the interrupt is issued (range 0..15).
	 *                            E.g. if set to 0, the interrupt is issued when
	 *                            there are no data samples remaining in the FIFO
	 *                            (all 32 FIFO words have unread data, i.e. the FIFO
	 *                            is full), if set to 15, the interrupt is issued
	 *                            when there are 15 data samples remaining in the
	 *                            FIFO (17 unread)
	 * @param spo2AdcRange        SpO2 ADC range
	 * @param spo2SampleRate      SpO2 sample rate
	 * @param ledPulseWidth       LED pulse width (us)
	 * @param led1CurrentMA       LED-1 pulse amplitude (range 0..51 mA) *
	 * @param led2CurrentMA       LED-2 pulse amplitude (range 0..51 mA) *
	 */
	public void setup(SampleAveraging sampleAveraging, boolean fifoRolloverOnFull, int fifoAlmostFullValue, Mode mode,
			SpO2AdcRange spo2AdcRange, SpO2SampleRate spo2SampleRate, LedPulseWidth ledPulseWidth, float led1CurrentMA,
			float led2CurrentMA) {
		// fifoAlmostFullValue must be 0..15
		if (fifoAlmostFullValue < 0 || fifoAlmostFullValue > FIFO_ALMOST_FULL_MAX) {
			throw new IllegalArgumentException("Invalid fifoAlmostFullValue value (" + fifoAlmostFullValue
					+ "), must be 0.." + FIFO_ALMOST_FULL_MAX);
		}
		// led1CurrentMA must be 0..51
		if (led1CurrentMA < 0 || led1CurrentMA > LED_MAX_CURRENT_MA) {
			throw new IllegalArgumentException(
					"Invalid led1CurrentMA value (" + led1CurrentMA + "), must be 0.." + LED_MAX_CURRENT_MA);
		}
		// led2CurrentMA must be 0..51
		if (led2CurrentMA < 0 || led2CurrentMA > LED_MAX_CURRENT_MA) {
			throw new IllegalArgumentException(
					"Invalid led2CurrentMA value (" + led2CurrentMA + "), must be 0.." + LED_MAX_CURRENT_MA);
		}

		// P23 of the datasheet
		switch (mode) {
		case SPO2:
			// Validate LED Pulse Width us and SPS
			switch (ledPulseWidth) {
			case _69:
				if (spo2SampleRate.getSampleRate() > SpO2SampleRate._1600.getSampleRate()) {
					throw new IllegalArgumentException(
							"In SpO2 mode sample rate must be <= " + SpO2SampleRate._1600.getSampleRate()
									+ " for led pulse width " + ledPulseWidth.getPulseWidthUs());
				}
				break;
			case _118:
				if (spo2SampleRate.getSampleRate() > SpO2SampleRate._1000.getSampleRate()) {
					throw new IllegalArgumentException(
							"In SpO2 mode sample rate must be <= " + SpO2SampleRate._1000.getSampleRate()
									+ " for led pulse width " + ledPulseWidth.getPulseWidthUs());
				}
				break;
			case _215:
				if (spo2SampleRate.getSampleRate() > SpO2SampleRate._800.getSampleRate()) {
					throw new IllegalArgumentException(
							"In SpO2 mode sample rate must be <= " + SpO2SampleRate._800.getSampleRate()
									+ " for led pulse width " + ledPulseWidth.getPulseWidthUs());
				}
				break;
			case _411:
				if (spo2SampleRate.getSampleRate() > SpO2SampleRate._400.getSampleRate()) {
					throw new IllegalArgumentException(
							"In SpO2 mode sample rate must be <= " + SpO2SampleRate._400.getSampleRate()
									+ " for led pulse width " + ledPulseWidth.getPulseWidthUs());
				}
				break;
			}
			break;

		case HEART_RATE:
			// Validate LED Pulse Width us and SPS
			switch (ledPulseWidth) {
			case _118:
			case _215:
				if (spo2SampleRate.getSampleRate() > SpO2SampleRate._1600.getSampleRate()) {
					throw new IllegalArgumentException(
							"In heart rate mode sample rate must be <= " + SpO2SampleRate._1600.getSampleRate()
									+ " for led pulse width " + ledPulseWidth.getPulseWidthUs());
				}
				break;
			case _411:
				if (spo2SampleRate.getSampleRate() > SpO2SampleRate._1000.getSampleRate()) {
					throw new IllegalArgumentException(
							"In heart rate mode sample rate must be <= " + SpO2SampleRate._1000.getSampleRate()
									+ " for led pulse width " + ledPulseWidth.getPulseWidthUs());
				}
				break;
			}
			break;

		// TODO Support for multi-led mode - what needs to be validated?
		}

		// Enable all interrupts
		device.writeByteData(REG_INTR_ENABLE_1,
				INT1_FIFO_ALMOST_FULL_MASK | INT1_PPG_RDY_DATA_MASK | INT1_ALC_OVF_MASK);
		device.writeByteData(REG_INTR_ENABLE_2, INT2_DIE_TEMP_RDY_MASK);

		device.writeByteData(REG_FIFO_CONFIG, sampleAveraging.getMask()
				| (fifoRolloverOnFull ? FifoRolloverOnFull.ENABLED.getMask() : FifoRolloverOnFull.DISABLED.getMask())
				| fifoAlmostFullValue);
		// Sample avg = 4, FIFO rollover = false, FIFO almost full = 15 (17 unread)
		// device.writeByteData(REG_FIFO_CONFIG, 0b0100_1111);

		// TODO Multi-LED mode (red and IR) (see Multi-LED Mode Control Registers)
		device.writeByteData(REG_MODE_CONFIG, mode.getMask());

		device.writeByteData(REG_SPO2_CONFIG,
				spo2AdcRange.getMask() | spo2SampleRate.getMask() | ledPulseWidth.getMask());
		// SPO2_ADC range = 4096nA, SPO2 sample rate = 100Hz, LED pulse-width = 411uS
		// device.writeByteData(REG_SPO2_CONFIG, 0b0010_0111);

		device.writeByteData(REG_LED1_PULSE_AMPL, RangeUtil.map(led1CurrentMA, 0f, LED_MAX_CURRENT_MA, 0, 255));
		device.writeByteData(REG_LED2_PULSE_AMPL, RangeUtil.map(led2CurrentMA, 0f, LED_MAX_CURRENT_MA, 0, 255));
		// Choose value for ~7.1mA for LED1
		// device.writeByteData(REG_LED1_PULSE_AMPL, 0x24);
		// Choose value for ~7.1mA for LED2
		// device.writeByteData(REG_LED2_PULSE_AMPL, 0x24);

		// Reset the FIFO write pointer, overflow counter and FIFO read pointer
		clearFifo();
	}

	public int getDataPresent() {
		int read_ptr = device.readByteData(REG_FIFO_READ_PTR) & 0xff;
		int write_ptr = device.readByteData(REG_FIFO_WRITE_PTR) & 0xff;

		int num_samples = write_ptr - read_ptr;
		if (num_samples < 0) {
			num_samples += 32;
		}

		return num_samples;
	}

	public void readFifo() {
		// Reading the interrupt registers clears the interrupt status
		byte intr_1 = device.readByteData(REG_INTR_STATUS_1);
		byte intr_2 = device.readByteData(REG_INTR_STATUS_2);

		byte[] data = device.readI2CBlockDataByteArray(REG_FIFO_DATA, 6);

		int red_led = (data[0] << 16 | data[1] << 8 | data[2]) & 0x03FFFF;
		int ir_led = (data[3] << 16 | data[4] << 8 | data[5]) & 0x03FFFF;
	}

	public void clearFifo() {
		device.writeByteData(REG_FIFO_WRITE_PTR, 0);
		device.writeByteData(REG_OVERFLOW_CTR, 0);
		device.writeByteData(REG_FIFO_READ_PTR, 0);
	}

	public int getWritePointer() {
		return device.readByteData(REG_FIFO_WRITE_PTR) & 0xff;
	}

	public int getReadPointer() {
		return device.readByteData(REG_FIFO_READ_PTR) & 0xff;
	}

	public float readTemperatureCelsius() {
		// DIE_TEMP_RDY interrupt must be enabled

		// Step 1: Config die temperature register to take 1 temperature sample
		device.writeByteData(REG_DIE_TEMP_CONFIG, DIE_TEMP_EN_MASK);

		// Poll for bit to clear, reading is then complete
		// Timeout after 100ms
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start < 100) {
			if ((device.readByteData(REG_INTR_STATUS_2) & INT2_DIE_TEMP_RDY_MASK) != 0) {
				break;
			}
			SleepUtil.sleepMillis(1);
		}

		// Step 2: Read die temperature register (integer)
		int temp_int = device.readByteData(REG_DIE_TEMP_INT);
		int temp_frac = device.readByteData(REG_DIE_TEMP_FRC) & 0xff;
		// Causes the clearing of the DIE_TEMP_RDY interrupt

		// Step 3: Calculate temperature (datasheet pg. 23)
		return temp_int + (temp_frac * 0.0625f);
	}

	/**
	 * Poll the sensor for new data - call regularly. If new data is available, it
	 * updates the head and tail in the main struct
	 * 
	 * @return number of new samples obtained
	 */
	public int pollForData() {
		int read_pointer = getReadPointer();
		int write_pointer = getWritePointer();
		
		int number_of_samples = 0;

		// Is new data available?
		if (read_pointer != write_pointer) {
			//Calculate the number of readings we need to get from sensor
		    number_of_samples = write_pointer - read_pointer;
		    if (number_of_samples < 0) {
		    	number_of_samples += 32; //Wrap condition
		    }
		    
		    // Calc the number of bytes to read
		    // For this example we are just doing Red and IR (3 bytes each)
		    //int bytes_left_to_read = number_of_samples * activeLEDs * 3;
		}
		
		return 0;
	}

	public int getNumAvailableSamples() {
		return 0;
	}
}
