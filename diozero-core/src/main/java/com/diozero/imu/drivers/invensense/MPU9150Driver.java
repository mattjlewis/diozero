package com.diozero.imu.drivers.invensense;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 diozero
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


import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.api.I2CConstants;
import com.diozero.api.I2CDevice;
import com.diozero.util.IOUtil;
import com.diozero.util.SleepUtil;

@SuppressWarnings("unused")
public class MPU9150Driver extends I2CDevice implements MPU9150Constants, AK8975Constants {
	private static final Logger logger = LogManager.getLogger(MPU9150Driver.class);
	
	private static final byte AKM_DATA_READY	  = 0x01;
	private static final byte AKM_DATA_OVERRUN	= 0x02;
	private static final byte AKM_OVERFLOW		= (byte)0x80;
	private static final byte AKM_DATA_ERROR	  = 0x40;

	private static final int MAX_PACKET_LENGTH = 12;
	private static final int MPU6050_TEMP_OFFSET = -521;
	private static final int MPU6500_TEMP_OFFSET = 0;
	private static final int MAX_FIFO = 1024;
	private static final int MPU6050_TEMP_SENS = 340;
	private static final int MPU6500_TEMP_SENS = 321;

	private int devAddr;
	/* Matches gyro_cfg >> 3 & 0x03 */
	private GyroFullScaleRange gyro_fsr;
	/* Matches accel_cfg >> 3 & 0x03 */
	private AccelFullScaleRange accel_fsr;
	/* Enabled sensors. Uses same masks as fifo_en, NOT pwr_mgmt_2. */
	private byte sensors;
	private LowPassFilter lpf;
	private ClockSource clk_src;
	/* Sample rate, NOT rate divider. */
	private int sample_rate;
	/* Matches fifo_en register. */
	private byte fifo_enable;
	/* Matches int enable register. */
	private boolean int_enable;
	/* true if devices on auxiliary I2C bus appear on the primary. */
	private Boolean bypass_mode;
	/* true if half-sensitivity.
	 * NOTE: This doesn't belong here, but everything else in hw_s is const,
	 * and this allows us to save some precious RAM.
	 */
	private boolean accel_half;
	/* true if device in low-power accel-only mode. */
	private boolean lp_accel_mode;
	/* true if interrupts are only triggered on motion events. */
	private boolean int_motion_only;
	//struct motion_int_cache_s cache;
	/* true for active low interrupts. */
	private boolean active_low_int;
	/* true for latched interrupts. */
	private boolean latched_int;
	/* true if DMP is enabled. */
	private boolean dmp_on;
	/* Ensures that DMP will only be loaded once. */
	private boolean dmp_loaded;
	/* Sampling rate used when DMP is enabled. */
	private int dmp_sample_rate;
	/* Compass sample rate. */
	private int compass_sample_rate;
	private byte compass_addr;
	private AK8975Driver magSensor;
	
	/** Default constructor, uses default I2C address.
	 * @throws IOException 
	 * @see MPU9150_DEFAULT_ADDRESS
	 */
	public MPU9150Driver(int controllerNumber, int addressSize, int clockFreq) throws IOException {
		this(controllerNumber, addressSize, clockFreq, MPU9150_DEFAULT_ADDRESS);
	}
	
	/** Specific address constructor.
	 * @param address I2C address
	 * @throws IOException
	 * @see MPU9150_DEFAULT_ADDRESS
	 * @see MPU9150_ADDRESS_AD0_LOW
	 * @see MPU9150_ADDRESS_AD0_HIGH
	 */
	public MPU9150Driver(int controllerNumber, int addressSize, int clockFreq, int devAddr) throws IOException {
		super(controllerNumber, devAddr, addressSize, clockFreq);
		this.devAddr = devAddr;
	}
	
	@Override
	public void close() throws IOException {
		if (magSensor != null) { try { magSensor.close(); } catch (IOException ioe) { } }
		super.close();
	}
	
	/**
	 *  @brief	  Enable/disable data ready interrupt.
	 *  If the DMP is on, the DMP interrupt is enabled. Otherwise, the data ready
	 *  interrupt is used.
	 *  @param[in]  enable	  1 to enable interrupt.
	 */
	public boolean set_int_enable(boolean enable) throws IOException {
		byte tmp;
	
		if (dmp_on) {
			if (enable) {
				tmp = BIT_DMP_INT_EN;
			} else {
				tmp = 0x00;
			}
			// int_enable == 0x38 == MPU9150_RA_INT_ENABLE
			writeByte(MPU9150_RA_INT_ENABLE, tmp);
			int_enable = enable;
		} else {
			if (sensors == 0) {
				return false;
			}
			if (enable && int_enable) {
				return true;
			}
			if (enable) {
				tmp = BIT_DATA_RDY_EN;
			} else {
				tmp = 0x00;
			}
			// int_enable == 0x38 == MPU9150_RA_INT_ENABLE
			writeByte(MPU9150_RA_INT_ENABLE, tmp);
			int_enable = enable;
		}
		return true;
	}

	/**
	 *  @brief	  Initialize hardware.
	 *  Initial configuration:\n
	 *  Gyro FSR: +/- 2000DPS\n
	 *  Accel FSR +/- 2G\n
	 *  DLPF: 42Hz\n
	 *  FIFO rate: 50Hz\n
	 *  Clock source: Gyro PLL\n
	 *  FIFO: Disabled.\n
	 *  Data ready interrupt: Disabled, active low, unlatched.
	 *  @param[in]  int_param   Platform-specific parameters to interrupt API.
	 */
	public void mpu_init() throws IOException {
		/* Reset device. */
		// pwr_mgmt_1 == 0x6B, MPU9150_RA_PWR_MGMT_1, BIT_RESET (pin 7, 0x80)
		writeByte(MPU9150_RA_PWR_MGMT_1, BIT_RESET);
		SleepUtil.sleepMillis(100);

		/* Wake up chip. */
		writeByte(MPU9150_RA_PWR_MGMT_1, 0);

		accel_half = false;
		
		/* Set to invalid values to ensure no I2C writes are skipped. */
		sensors = (byte)0xFF;
		gyro_fsr = null;
		accel_fsr = null;
		lpf = null;
		sample_rate = 0xFFFF;
		fifo_enable = (byte)0xFF;
		bypass_mode = null;
		compass_sample_rate = 0xFFFF;
		/* mpu_set_sensors always preserves this setting. */
		clk_src = ClockSource.INV_CLK_PLL;
		/* Handled in next call to mpu_set_bypass. */
		active_low_int = true;
		latched_int = false;
		int_motion_only = false;
		lp_accel_mode = false;
		//memset(&st.chip_cfg.cache, 0, sizeof(st.chip_cfg.cache));
		dmp_on = false;
		dmp_loaded = false;
		dmp_sample_rate = 0;

		mpu_set_gyro_fsr(GyroFullScaleRange.INV_FSR_2000DPS);
		mpu_set_accel_fsr(AccelFullScaleRange.INV_FSR_2G);
		mpu_set_lpf(42);
		mpu_set_sample_rate(50);
		mpu_configure_fifo((byte)0);

		//if (int_param)
		//	reg_int_cb(int_param);
		
		setup_compass();
		mpu_set_compass_sample_rate(10);

		mpu_set_sensors((byte)0);
	}

	/**
	 *  @brief	  Enter low-power accel-only mode.
	 *  In low-power accel mode, the chip goes to sleep and only wakes up to sample
	 *  the accelerometer at one of the following frequencies:
	 *   MPU6050: 1.25Hz, 5Hz, 20Hz, 40Hz
	 *   MPU6500: 1.25Hz, 2.5Hz, 5Hz, 10Hz, 20Hz, 40Hz, 80Hz, 160Hz, 320Hz, 640Hz
	 *  If the requested rate is not one listed above, the device will be set to
	 *  the next highest rate. Requesting a rate above the maximum supported
	 *  frequency will result in an error.
	 *  To select a fractional wake-up frequency, round down the value passed to rate.
	 *  @param[in]  rate		Minimum sampling rate, or zero to disable LP accel mode.
	 *  @return	 true if successful.
	 */
	public boolean mpu_lp_accel_mode(int rate) throws IOException {
		if (rate > 40) {
			return false;
		}
	
		byte[] tmp = new byte[2];
		if (rate == 0) {
			mpu_set_int_latched(false);
			tmp[0] = 0;
			tmp[1] = BIT_STBY_XYZG;
			writeBytes(MPU9150_RA_PWR_MGMT_1, 2, tmp);
			lp_accel_mode = false;
			return true;
		}
		/* For LP accel, we automatically configure the hardware to produce latched
		 * interrupts. In LP accel mode, the hardware cycles into sleep mode before
		 * it gets a chance to deassert the interrupt pin; therefore, we shift this
		 * responsibility over to the MCU.
		 *
		 * Any register read will clear the interrupt.
		 */
		mpu_set_int_latched(true);
		
		tmp[0] = BIT_LPA_CYCLE;
		if (rate == 1) {
			tmp[1] = LowPowerAccelWakeupRate.INV_LPA_1_25HZ.getValue();
			mpu_set_lpf(5);
		} else if (rate <= 5) {
			tmp[1] = LowPowerAccelWakeupRate.INV_LPA_5HZ.getValue();
			mpu_set_lpf(5);
		} else if (rate <= 20) {
			tmp[1] = LowPowerAccelWakeupRate.INV_LPA_20HZ.getValue();
			mpu_set_lpf(10);
		} else {
			tmp[1] = LowPowerAccelWakeupRate.INV_LPA_40HZ.getValue();
			mpu_set_lpf(20);
		}
		tmp[1] = (byte) (((tmp[1]&0xff) << 6) | BIT_STBY_XYZG);
		// pwr_mgmt_1 == 0x6B, MPU9150_RA_PWR_MGMT_1, BIT_RESET (pin 7, 0x80)
		writeBytes(MPU9150_RA_PWR_MGMT_1, 2, tmp);
		sensors = INV_XYZ_ACCEL;
		clk_src = ClockSource.INV_CLK_INTERNAL;
		lp_accel_mode = true;
		mpu_configure_fifo((byte)0);
	
		return true;
	}

	/**
	 *  @brief	  Read raw gyro data directly from the registers.
	 *  @return		Raw data in hardware units.
	 */
	public short[] mpu_get_gyro_reg() throws IOException {
		if ((sensors & INV_XYZ_GYRO) == 0) {
			return null;
		}
	
		// raw_gyro == 0x43 == MPU9150_RA_GYRO_XOUT_H
		ByteBuffer buffer = ByteBuffer.wrap(readBytes(MPU9150_RA_GYRO_XOUT_H, 6));
		short x = IOUtil.getShort(buffer);
		short y = IOUtil.getShort(buffer);
		short z = IOUtil.getShort(buffer);
		System.out.format("gyro reg values = (%d, %d, %d)%n", Short.valueOf(x), Short.valueOf(y), Short.valueOf(z));
		/*byte[] data = readBytes(MPU9150_RA_GYRO_XOUT_H, 6)
		short x = (short)((data[0] << 8) | (data[1] & 0xff));
		short y = (short)((data[2] << 8) | (data[3] & 0xff));
		short z = (short)((data[4] << 8) | (data[5] & 0xff));*/
		
		return new short[] {x, y, z};
	}

	/**
	 * Each 16-bit accelerometer measurement has a full scale defined in ACCEL_FS (Register 28).
	 * For each full scale setting, the accelerometers' sensitivity per LSB in ACCEL_xOUT is shown in the table below.
	 * AFS_SEL	Full Scale Range	LSB Sensitivity
	 * 0		+/-2g				16384 LSB/mg
	 * 1		+/-4g				8192 LSB/mg
	 * 2		+/-8g				4096 LSB/mg
	 * 3		+/-16g				2048 LSB/mg
	 *  @brief	  Read raw accel data directly from the registers.
	 *  @return		Raw data in hardware units.
	 */
	public short[] mpu_get_accel_reg() throws IOException {
		if ((sensors & INV_XYZ_ACCEL) == 0) {
			return null;
		}
	
		// raw_accel == 0x3B == MPU9150_RA_ACCEL_XOUT_H
		ByteBuffer buffer = ByteBuffer.wrap(readBytes(MPU9150_RA_ACCEL_XOUT_H, 6));
		short x = buffer.getShort();
		short y = buffer.getShort();
		short z = buffer.getShort();
		/*
		byte[] data = readBytes(MPU9150_RA_ACCEL_XOUT_H, 6);
		short x = (short)((data[0] << 8) | (data[1] & 0xff));
		short y = (short)((data[2] << 8) | (data[3] & 0xff));
		short z = (short)((data[4] << 8) | (data[5] & 0xff));
		*/
		
		return new short[] {x, y, z};
	}

	/**
	 *  @brief	  Read temperature data directly from the registers.
	 *  The scale factor and offset for the temperature sensor are found in the Electrical
	 *  Specifications table in the MPU-9150 Product Specification document.
	 *  The temperature in degrees C for a given register value may be computed as:
	 *  Temperature in degrees C = (TEMP_OUT Register Value as a signed quantity)/340 + 35
	 *  Please note that the math in the above equation is in decimal.
	 *  @return		Temperature
	 */
	public float mpu_get_temperature() throws IOException {
		if (sensors == 0) {
			return -1;
		}
	
		// temp == 0x41 == MPU9150_RA_TEMP_OUT_H
		short raw = readShort(MPU9150_RA_TEMP_OUT_H, I2CConstants.SUB_ADDRESS_SIZE_1_BYTE);
		//raw = (tmp[0] << 8) | (tmp[1] & 0xff);
	
		float val = ((raw - MPU6050_TEMP_OFFSET) / (float)MPU6050_TEMP_SENS) + 35;
		return val;
	}

	/**
	 *  @brief	  Read biases to the accel bias 6050 registers.
	 *  This function reads from the MPU6050 accel offset cancellations registers.
	 *  The format are G in +-8G format. The register is initialised with OTP 
	 *  factory trim values.
	 *  @return  accel_bias  returned structure with the accel bias
	 */
	public short[] mpu_read_6050_accel_bias() throws IOException {
		short[] accel_bias = new short[3];
		
		/*
		byte[] bias_x_bytes = readBytes(MPU9150_RA_XA_OFFS_H, 2);
		byte[] bias_y_bytes = readBytes(MPU9150_RA_YA_OFFS_H, 2);
		byte[] bias_z_bytes = readBytes(MPU9150_RA_ZA_OFFS_H, 2);
		accel_bias[0] = (bias_x_bytes[0] << 8) | (bias_x_bytes[1] & 0xff);
		accel_bias[1] = (bias_y_bytes[2] << 8) | (bias_y_bytes[3] & 0xff);
		accel_bias[2] = (bias_z_bytes[4] << 8) | (bias_z_bytes[5] & 0xff);
		*/
		ByteBuffer buffer = ByteBuffer.wrap(readBytes(MPU9150_RA_XA_OFFS_H, 6));
		accel_bias[0] = IOUtil.getShort(buffer);
		accel_bias[1] = IOUtil.getShort(buffer);
		accel_bias[2] = IOUtil.getShort(buffer);
		
		return accel_bias;
	}
	
	/**
	 *  @brief	  Push biases to the gyro bias 6500/6050 registers.
	 *  This function expects biases relative to the current sensor output, and
	 *  these biases will be added to the factory-supplied values. Bias inputs are LSB
	 *  in +-1000dps format.
	 *  @param[in]  gyro_bias  New biases.
	 */
	public void mpu_set_gyro_bias_reg(short[] gyro_bias) throws IOException {
		for(int i=0; i<3; i++) {
			gyro_bias[i] = (short)-gyro_bias[i];
		}
		/*
		byte data[] = {0, 0, 0, 0, 0, 0};
		data[0] = (byte)((gyro_bias[0] >> 8) & 0xff);
		data[1] = (byte)((gyro_bias[0]) & 0xff);
		data[2] = (byte)((gyro_bias[1] >> 8) & 0xff);
		data[3] = (byte)((gyro_bias[1]) & 0xff);
		data[4] = (byte)((gyro_bias[2] >> 8) & 0xff);
		data[5] = (byte)((gyro_bias[2]) & 0xff);
		*/
		writeShort(MPU9150_RA_XG_OFFS_USRH, gyro_bias[0]);
		writeShort(MPU9150_RA_YG_OFFS_USRH, gyro_bias[1]);
		writeShort(MPU9150_RA_ZG_OFFS_USRH, gyro_bias[2]);
	}

	/**
	 *  @brief	  Push biases to the accel bias 6050 registers.
	 *  This function expects biases relative to the current sensor output, and
	 *  these biases will be added to the factory-supplied values. Bias inputs are LSB
	 *  in +-16G format.
	 *  @param[in]  accel_bias  New biases.
	 */
	public void mpu_set_accel_bias_6050_reg(short[] accel_bias) throws IOException {
		short accel_reg_bias[] = mpu_read_6050_accel_bias();
	
		accel_reg_bias[0] -= (accel_bias[0] & ~1);
		accel_reg_bias[1] -= (accel_bias[1] & ~1);
		accel_reg_bias[2] -= (accel_bias[2] & ~1);
	
		/*
		byte data[] = {0, 0, 0, 0, 0, 0};
		data[0] = (byte)((accel_reg_bias[0] >> 8) & 0xff);
		data[1] = (byte)((accel_reg_bias[0]) & 0xff);
		data[2] = (byte)((accel_reg_bias[1] >> 8) & 0xff);
		data[3] = (byte)((accel_reg_bias[1]) & 0xff);
		data[4] = (byte)((accel_reg_bias[2] >> 8) & 0xff);
		data[5] = (byte)((accel_reg_bias[2]) & 0xff);
		*/
	
		writeShort(MPU9150_RA_XA_OFFS_H, accel_reg_bias[0]);
		writeShort(MPU9150_RA_YA_OFFS_H, accel_reg_bias[1]);
		writeShort(MPU9150_RA_ZA_OFFS_H, accel_reg_bias[2]);
	}

	/**
	 *  @brief  Reset FIFO read/write pointers.
	 */
	public boolean mpu_reset_fifo() throws IOException {
		if (sensors == 0) {
			logger.warn("mpu_reset_fifo(), sensors==0, returning");
			return false;
		}
	
		byte data = 0;
		// int_enable == 0x38 == MPU9150_RA_INT_ENABLE
		writeByte(MPU9150_RA_INT_ENABLE, data);
		// fifo_en == 0x23 == MPU9150_RA_FIFO_EN
		writeByte(MPU9150_RA_FIFO_EN, data);
		// user_ctrl == 0x6a == MPU9150_RA_USER_CTRL for MPU-6050
		writeByte(MPU9150_RA_USER_CTRL, data);
	
		if (dmp_on) {
			data = BIT_FIFO_RST | BIT_DMP_RST;
			// user_ctrl == 0x6a == MPU9150_RA_USER_CTRL for MPU-6050
			writeByte(MPU9150_RA_USER_CTRL, data);
			SleepUtil.sleepMillis(50);
			data = BIT_DMP_EN | BIT_FIFO_EN;
			if ((sensors & INV_XYZ_COMPASS) != 0) {
				data |= BIT_AUX_IF_EN;
			}
			// user_ctrl == 0x6a == MPU9150_RA_USER_CTRL for MPU-6050
			writeByte(MPU9150_RA_USER_CTRL, data);
			if (int_enable) {
				data = BIT_DMP_INT_EN;
			} else {
				data = 0;
			}
			// int_enable == 0x38 == MPU9150_RA_INT_ENABLE
			writeByte(MPU9150_RA_INT_ENABLE, data);
			data = 0;
			// fifo_en == 0x23 == MPU9150_RA_FIFO_EN
			writeByte(MPU9150_RA_FIFO_EN, data);
		} else {
			data = BIT_FIFO_RST;
			// user_ctrl == 0x6a == MPU9150_RA_USER_CTRL for MPU-6050
			writeByte(MPU9150_RA_USER_CTRL, data);
			if ((bypass_mode != null && bypass_mode.booleanValue()) || ((sensors & INV_XYZ_COMPASS) == 0)) {
				data = BIT_FIFO_EN;
			} else {
				data = BIT_FIFO_EN | BIT_AUX_IF_EN;
			}
			// user_ctrl == 0x6a == MPU9150_RA_USER_CTRL for MPU-6050
			writeByte(MPU9150_RA_USER_CTRL, data);
			SleepUtil.sleepMillis(50);
			if (int_enable) {
				data = BIT_DATA_RDY_EN;
			} else {
				data = 0;
			}
			// int_enable == 0x38 == MPU9150_RA_INT_ENABLE
			writeByte(MPU9150_RA_INT_ENABLE, data);
			// fifo_en == 0x23 == MPU9150_RA_FIFO_EN
			writeByte(MPU9150_RA_FIFO_EN, fifo_enable);
		}
		
		return true;
	}

	/**
	 *  @brief	  Get the gyro full-scale range.
	 *  @return fsr Current full-scale range.
	 */
	public GyroFullScaleRange mpu_get_gyro_fsr() {
		return gyro_fsr;
	}

	/**
	 *  @brief	  Set the gyro full-scale range.
	 *  @param[in]  fsr Desired full-scale range.
	 */
	public boolean mpu_set_gyro_fsr(GyroFullScaleRange fsr) throws IOException {
		if (sensors == 0) {
			return false;
		}

		if (gyro_fsr == fsr) {
			return true;
		}

		System.out.format("Setting gyro config to 0x%x%n", Byte.valueOf(fsr.getBitVal()));
		// gyro_cfg == 0x1B == MPU9150_RA_GYRO_CONFIG
		writeByte(MPU9150_RA_GYRO_CONFIG, fsr.getBitVal());
		
		gyro_fsr = fsr;

		return true;
	}
	
	public AccelFullScaleRange mpu_get_accel_fsr() {
		if (accel_half) {
			// TODO Missing accel_half compensation
			//fsr <<= 1;
		}
		return accel_fsr;
	}

	/**
	 *  @brief	  Set the accel full-scale range.
	 *  @param[in]  fsr Desired full-scale range.
	 * @throws IOException
	 */
	public boolean mpu_set_accel_fsr(AccelFullScaleRange fsr) throws IOException {
		if (sensors == 0) {
			return false;
		}
		
		if (accel_fsr == fsr) {
			return true;
		}

		System.out.format("Setting accel config to 0x%x%n", Byte.valueOf(fsr.getBitVal()));
		// accel_cfg == 0x1C == MPU9150_RA_ACCEL_CONFIG
		writeByte(MPU9150_RA_ACCEL_CONFIG, fsr.getBitVal());
		accel_fsr = fsr;
		
		return true;
	}
	
	public LowPassFilter mpu_get_lpf() {
		return lpf;
	}
	
	/**
	 *  @brief	  Set digital low pass filter.
	 *  The following LPF settings are supported: 188, 98, 42, 20, 10, 5.
	 *  @param[in]  lpf Desired LPF setting.
	 * @throws IOException
	 */
	public boolean mpu_set_lpf(int frequency) throws IOException {
		return mpu_set_lpf(LowPassFilter.getForFrequency(frequency));
	}
	
	/**
	 *  @brief	  Set digital low pass filter.
	 *  The following LPF settings are supported: 188, 98, 42, 20, 10, 5.
	 *  @param[in]  lpf Desired LPF setting.
	 * @throws IOException
	 */
	public boolean mpu_set_lpf(LowPassFilter lpf) throws IOException {
		if (sensors == 0) {
			return false;
		}

		if (this.lpf == lpf) {
			return true;
		}

		System.out.format("Setting LPF to %d, bit value 0x%x%n", Integer.valueOf(lpf.getFreq()), Byte.valueOf(lpf.getBitVal()));
		// lpf == 0x1A == MPU9150_RA_CONFIG
		writeByte(MPU9150_RA_CONFIG, lpf.getBitVal());
		
		this.lpf = lpf;
		
		return true;
	}
	
	public int mpu_get_sample_rate() {
		return sample_rate;
	}
	
	/**
	 *  @brief	  Set sampling rate.
	 *  Sampling rate must be between 4Hz and 1kHz.
	 *  @param[in]  rate	Desired sampling rate (Hz).
	 */
	public boolean mpu_set_sample_rate(int rate) throws IOException {
		logger.debug("mpu_set_sample_rate(" + rate + ")");
		if (sensors == 0) {
			return false;
		}

		if (dmp_on) {
			logger.warn("mpu_set_sample_rate() DMP is on, returning");
			return false;
		}

		if (lp_accel_mode) {
			if (rate != 0 && (rate <= 40)) {
				/* Just stay in low-power accel mode. */
				logger.debug("Just setting lp_accel_mode to " + rate);
				mpu_lp_accel_mode(rate);
				return true;
			}
			
			/* Requested rate exceeds the allowed frequencies in LP accel mode,
			 * switch back to full-power mode.
			 */
			logger.debug("Setting lp_accel_mode to 0");
			mpu_lp_accel_mode(0);
		}
		int new_rate = rate;
		if (new_rate < 4) {
			new_rate = 4;
		} else if (new_rate > 1000) {
			new_rate = 1000;
		}

		int data = 1000 / new_rate - 1;
		logger.debug("Setting sample rate to " + data);
		// rate_div == 0x19 == MPU9150_RA_SMPL_RATE_DIV
		writeByte(MPU9150_RA_SMPL_RATE_DIV, data);

		sample_rate = 1000 / (1 + data);

		mpu_set_compass_sample_rate(Math.min(compass_sample_rate, MAX_COMPASS_SAMPLE_RATE));

		logger.debug("Automatically setting LPF to " + (sample_rate >> 1));
		/* Automatically set LPF to 1/2 sampling rate. */
		mpu_set_lpf(sample_rate >> 1);
		
		return true;
	}
	
	public int mpu_get_compass_sample_rate() {
		return compass_sample_rate;
	}

	/**
	 *  @brief	  Set compass sampling rate.
	 *  The compass on the auxiliary I2C bus is read by the MPU hardware at a
	 *  maximum of 100Hz. The actual rate can be set to a fraction of the gyro
	 *  sampling rate.
	 *
	 *  \n WARNING: The new rate may be different than what was requested. Call
	 *  mpu_get_compass_sample_rate to check the actual setting.
	 *  @param[in]  rate	Desired compass sampling rate (Hz).
	 */
	public boolean mpu_set_compass_sample_rate(int rate) throws IOException {
		logger.debug("mpu_set_compass_sample_rate(" + rate + ")");
		if (rate == 0 || rate > sample_rate || rate > MAX_COMPASS_SAMPLE_RATE) {
			return false;
		}
	
		byte div = (byte)(sample_rate / rate - 1);
		// s4_ctrl == 0x34 == MPU9150_RA_I2C_SLV4_CTRL
		writeByte(MPU9150_RA_I2C_SLV4_CTRL, div);
		compass_sample_rate = sample_rate / (div + 1);
		
		return true;
	}
	
	/**
	 *  @brief	  Get gyro sensitivity scale factor.
	 *  @return sens	Conversion from hardware units to dps.
	 */
	public double mpu_get_gyro_sens() {
		/*
		float sens;
		switch (gyro_fsr) {
		case INV_FSR_250DPS:
			sens = 131.0f;
			break;
		case INV_FSR_500DPS:
			sens = 65.5f;
			break;
		case INV_FSR_1000DPS:
			sens = 32.8f;
			break;
		case INV_FSR_2000DPS:
			sens = 16.4f;
			break;
		default:
			sens = -1f;
		}
		
		return sens;
		*/

		return gyro_fsr.getSensitivityScaleFactor();
	}
	
	/**
	 *  @brief	  Get accel sensitivity scale factor.
	 *  @return sens	Conversion from hardware units to g's.
	 */
	public int mpu_get_accel_sens() {
		/*
		int sens;
		switch (accel_fsr) {
		case INV_FSR_2G:
			sens = 16384;
			break;
		case INV_FSR_4G:
			sens = 8192;
			break;
		case INV_FSR_8G:
			sens = 4096;
			break;
		case INV_FSR_16G:
			sens = 2048;
			break;
		default:
			return -1;
		}
		*/
		
		int sens = accel_fsr.getSensitivityScaleFactor();
		if (accel_half) {
			sens >>= 1;
		}
		
		return sens;
	}
	
	/**
	 *  @brief	  Get current FIFO configuration.
	 *  @e sensors can contain a combination of the following flags:
	 *  \n INV_X_GYRO, INV_Y_GYRO, INV_Z_GYRO
	 *  \n INV_XYZ_GYRO
	 *  \n INV_XYZ_ACCEL
	 *  @return sensors Mask of sensors in FIFO.
	 */
	public byte mpu_get_fifo_config() {
		return fifo_enable;
	}

	/**
	 *  @brief	  Select which sensors are pushed to FIFO.
	 *  @e sensors can contain a combination of the following flags:
	 *   INV_X_GYRO, INV_Y_GYRO, INV_Z_GYRO
	 *   INV_XYZ_GYRO
	 *   INV_XYZ_ACCEL
	 *  @param[in]  sensors Mask of sensors to push to FIFO.
	 */
	public boolean mpu_configure_fifo(byte newSensors) throws IOException {
		System.out.format("mpu_configure_fifo 0x%x%n", Byte.valueOf(newSensors));
		if (dmp_on) {
			logger.info("DMP is on, returning");
			return true;
		}
		
		/* Compass data isn't going into the FIFO. Stop trying. */
		newSensors &= ~INV_XYZ_COMPASS;
	
		if (sensors == 0) {
			logger.error("mpu_configure_fifo() sensors == 0, returning");
			return false;
		}
		
		byte prev = fifo_enable;
		fifo_enable = (byte)(newSensors & sensors);
		boolean result;
		if (fifo_enable != newSensors) {
			/* You're not getting what you asked for. Some sensors are asleep. */
			logger.info("You're not getting what you asked for. Some sensors are asleep.");
			result = false;
		} else {
			result = true;
		}
		if (newSensors != 0 || lp_accel_mode) {
			set_int_enable(true);
		} else {
			set_int_enable(false);
		}
		if (newSensors != 0) {
			if (mpu_reset_fifo()) {
				fifo_enable = prev;
				return false;
			}
		}
	
		return result;
	}
	
	/**
	 *  @brief	  Get current power state.
	 *  @param[in]  power_on	1 if turned on, 0 if suspended.
	 */
	public boolean mpu_get_power_state() {
		if (sensors != 0) {
			return true;
		}
		return false;
	}

	/**
	 *  @brief	  Turn specific sensors on/off.
	 *  @e sensors can contain a combination of the following flags:
	 *   INV_X_GYRO, INV_Y_GYRO, INV_Z_GYRO
	 *   INV_XYZ_GYRO
	 *   INV_XYZ_ACCEL
	 *   INV_XYZ_COMPASS
	 *  @param[in]  sensors	Mask of sensors to wake.
	 *  @return	 true if successful.
	 */
	public boolean mpu_set_sensors(byte newSensors) throws IOException {
		byte data;
		byte user_ctrl;
	
		if ((newSensors & INV_XYZ_GYRO) != 0) {
			data = ClockSource.INV_CLK_PLL.getVal();
		} else if (newSensors != 0) {
			data = ClockSource.INV_CLK_INTERNAL.getVal();
		} else {
			data = BIT_SLEEP;
		}
		try {
			// pwr_mgmt_1 == 0x6B, MPU9150_RA_PWR_MGMT_1
			writeByte(MPU9150_RA_PWR_MGMT_1, data);
		} catch (IOException ioe) {
			System.out.format("Error in mpu_set_sensors(%x): %s%n", new Byte(newSensors), ioe);
			ioe.printStackTrace();
			sensors = 0;
			return false;
		}
		clk_src = ClockSource.values()[data & ~BIT_SLEEP];
	
		data = 0;
		if ((newSensors & INV_X_GYRO) == 0) {
			data |= BIT_STBY_XG;
		}
		if ((newSensors & INV_Y_GYRO) == 0) {
			data |= BIT_STBY_YG;
		}
		if ((newSensors & INV_Z_GYRO) == 0) {
			data |= BIT_STBY_ZG;
		}
		if ((newSensors & INV_XYZ_ACCEL) == 0) {
			data |= BIT_STBY_XYZA;
		}
		try {
			// pwr_mgmt_2 == 0x6C == MPU9150_RA_PWR_MGMT_2
			writeByte(MPU9150_RA_PWR_MGMT_2, data);
		} catch (IOException ioe) {
			System.out.format("Error in mpu_set_sensors(%x): %s%n", new Byte(newSensors), ioe);
			ioe.printStackTrace();
			sensors = 0;
			return false;
		}
	
		if (newSensors != 0 && (newSensors != INV_XYZ_ACCEL)) {
			/* Latched interrupts only used in LP accel mode. */
			mpu_set_int_latched(false);
		}
	
	//#ifdef AK89xx_SECONDARY
		// user_ctrl == 0x6a == MPU9150_RA_USER_CTRL for MPU-6050
		user_ctrl = readByte(MPU9150_RA_USER_CTRL);
		/* Handle AKM power management. */
		if ((newSensors & INV_XYZ_COMPASS) != 0) {
			data = AKM_SINGLE_MEASUREMENT;
			user_ctrl |= BIT_AUX_IF_EN;
		} else {
			data = AKM_POWER_DOWN;
			user_ctrl &= ~BIT_AUX_IF_EN;
		}
		if (dmp_on) {
			user_ctrl |= BIT_DMP_EN;
		} else {
			user_ctrl &= ~BIT_DMP_EN;
		}
		// s1_do == 0x64 == MPU9150_RA_I2C_SLV1_DO
		writeByte(MPU9150_RA_I2C_SLV1_DO, data);
		/* Enable/disable I2C master mode. */
		// user_ctrl == 0x6a == MPU9150_RA_USER_CTRL for MPU-6050
		writeByte(MPU9150_RA_USER_CTRL, user_ctrl);
	//#endif
	
		sensors = newSensors;
		lp_accel_mode = false;
		SleepUtil.sleepMillis(50);
		return true;
	}
	
	/**
	 *  @brief	  Read the MPU interrupt status registers.
	 *  @return status  Mask of interrupt bits.
	 */
	public short mpu_get_int_status() throws IOException {
		if (sensors == 0) {
			return -1;
		}
		
		// dmp_int_status == 0x39 == MPU9150_RA_DMP_INT_STATUS
		return readShort(MPU9150_RA_DMP_INT_STATUS, I2CConstants.SUB_ADDRESS_SIZE_1_BYTE);
	}
	
	/**
	 *  @brief	  Get one packet from the FIFO.
	 *  If @e sensors does not contain a particular sensor, disregard the data
	 *  returned to that pointer.
	 *  \n @e sensors can contain a combination of the following flags:
	 *  \n INV_X_GYRO, INV_Y_GYRO, INV_Z_GYRO
	 *  \n INV_XYZ_GYRO
	 *  \n INV_XYZ_ACCEL
	 *  \n If the FIFO has no new data, @e sensors will be zero.
	 *  \n If the FIFO is disabled, @e sensors will be zero and this function will
	 *  return a non-zero error code.
	 *  @return FIFOData:
	 *   gyro		Gyro data in hardware units.
	 *   accel	   Accel data in hardware units.
	 *   timestamp   Timestamp in milliseconds.
	 *   sensors	 Mask of sensors read from FIFO.
	 *   more		Number of remaining packets.
	 */
	public FIFOData mpu_read_fifo() throws IOException {

		if (dmp_on) {
			return null;
		}

		if (sensors == 0) {
			logger.warn("mpu_read_fifo() sensors == 0, returning");
			return null;
		}
		if (fifo_enable == 0) {
			logger.warn("mpu_read_fifo() fifo_enable == 0, returning");
			return null;
		}

		/* Assumes maximum packet size is gyro (6) + accel (6). */
		short packet_size = 0;
		if ((fifo_enable & INV_X_GYRO) != 0) {
			packet_size += 2;
		}
		if ((fifo_enable & INV_Y_GYRO) != 0) {
			packet_size += 2;
		}
		if ((fifo_enable & INV_Z_GYRO) != 0) {
			packet_size += 2;
		}
		if ((fifo_enable & INV_XYZ_ACCEL) != 0) {
			packet_size += 6;
		}

		byte[] data = new byte[MAX_PACKET_LENGTH];
		// fifo_count_h == 0x72 == MPU9150_RA_FIFO_COUNTH
		//fifo_count = readUShort(MPU9150_RA_FIFO_COUNTH, SUB_ADDRESS_SIZE_1_BYTE);
		data = readBytes(MPU9150_RA_FIFO_COUNTH, 2);
		int fifo_count = ((data[0] & 0xff) << 8) | (data[1] & 0xff);
		//fifo_count = (data[0] << 8) | data[1];
		if (fifo_count < packet_size) {
			System.out.format("mpu_read_fifo() fifo_count(%d) < packet_size(%d)%n",
					Integer.valueOf(fifo_count), Integer.valueOf(packet_size));
			return null;
		}
//		log_i("FIFO count: %hd\n", fifo_count);
		if (fifo_count > (MAX_FIFO >> 1)) {
			System.out.format("mpu_read_fifo() fifo_count(%d) is more than 50% full");
			/* FIFO is 50% full, better check overflow bit. */
			// int_status == 0x3a == MPU9150_RA_INT_STATUS
			data[0] = readByte(MPU9150_RA_INT_STATUS);
			if ((data[0] & BIT_FIFO_OVERFLOW) != 0) {
				System.out.format("mpu_read_fifo() overflow bit is set");
				mpu_reset_fifo();
				return null;
			}
		}
		long timestamp = System.currentTimeMillis();

		// fifo_r_w == 0x74 == MPU9150_RA_FIFO_R_W
		data = readBytes(MPU9150_RA_FIFO_R_W, packet_size);
		
		int more = fifo_count / packet_size - 1;
		short fifo_sensors = 0;
		// Accel and gyro data are both signed short values
		short[] accel = new short[3];
		short[] gyro = new short[3];
		int index = 0;
		if ((index != packet_size) && (fifo_enable & INV_XYZ_ACCEL) != 0) {
			accel[0] = (short)((data[index+0] << 8) | (data[index+1] & 0xff));
			accel[1] = (short)((data[index+2] << 8) | (data[index+3] & 0xff));
			accel[2] = (short)((data[index+4] << 8) | (data[index+5] & 0xff));
			fifo_sensors |= INV_XYZ_ACCEL;
			index += 6;
		}
		if ((index != packet_size) && (fifo_enable & INV_X_GYRO) != 0) {
			gyro[0] = (short)((data[index+0] << 8) | (data[index+1] & 0xff));
			fifo_sensors |= INV_X_GYRO;
			index += 2;
		}
		if ((index != packet_size) && (fifo_enable & INV_Y_GYRO) != 0) {
			gyro[1] = (short)((data[index+0] << 8) | (data[index+1] & 0xff));
			fifo_sensors |= INV_Y_GYRO;
			index += 2;
		}
		if ((index != packet_size) && (fifo_enable & INV_Z_GYRO) != 0) {
			gyro[2] = (short)((data[index+0] << 8) | (data[index+1] & 0xff));
			fifo_sensors |= INV_Z_GYRO;
			index += 2;
		}
		
		return new FIFOData(gyro, accel, null, timestamp, fifo_sensors, more);
	}
	
	/**
	 *  @brief	  Get one unparsed packet from the FIFO.
	 *  This function should be used if the packet is to be parsed elsewhere.
	 *  @param[in]  length  Length of one FIFO packet.
	 *  @param[in]  data	FIFO packet.
	 *  @return  more	Number of remaining packets.
	 */
	public FIFOStream mpu_read_fifo_stream(int length) throws IOException {
		if (!dmp_on) {
			logger.warn("mpu_read_fifo_stream(), dmp_on is false, returning");
			return null;
		}
		if (sensors == 0) {
			logger.warn("mpu_read_fifo_stream(), sensors == 0, returning");
			return null;
		}
		
		// fifo_count_h == 0x72 == MPU9150_RA_FIFO_COUNTH
		//int fifo_count = readUShort(MPU9150_RA_FIFO_COUNTH, SUB_ADDRESS_SIZE_1_BYTE);
		byte[] tmp = readBytes(MPU9150_RA_FIFO_COUNTH, 2);
		//System.out.format("mpu_read_fifo_stream(), fifo count msb=0x%x, lsb=0x%x%n",
		//		Byte.valueOf(tmp[0]), Byte.valueOf(tmp[1]));
		int fifo_count = ((tmp[0] & 0xff) << 8) | (tmp[1] & 0xff);
		//int fifo_count = readUShort(MPU9150_RA_FIFO_COUNTH, SUB_ADDRESS_SIZE_1_BYTE);
		if (fifo_count < length) {
			//logger.trace("mpu_read_fifo_stream() fifo_count (" + fifo_count + ") < length (" + length + ")");
			return null;
		}
		//logger.trace("Got a FIFO packet! fifo_count=" + fifo_count);
		if (fifo_count > (MAX_FIFO >> 1)) {
			/* FIFO is 50% full, better check overflow bit. */
			// int_status == 0x3a == MPU9150_RA_INT_STATUS
			byte int_status = readByte(MPU9150_RA_INT_STATUS);
			if ((int_status & BIT_FIFO_OVERFLOW) != 0) {
				logger.info("resetting FIFO as overflowing");
				mpu_reset_fifo();
				return null;
			}
		}

		// fifo_r_w == 0x74 == MPU9150_RA_FIFO_R_W
		byte[] data = readBytes(MPU9150_RA_FIFO_R_W, length);
		// unsigned char more;
		short more = (short)(fifo_count / length - 1);

		return new FIFOStream(data, more);
	}

	/**
	 *  @brief	  Set device to bypass mode.
	 *  @param[in]  bypass_on   1 to enable bypass mode.
	 */
	public void mpu_set_bypass(boolean bypass_on) throws IOException {
		/** Was:
		// Set i2c bypass enable pin to true to access magnetometer
		// MPU9150_RA_INT_PIN_CFG = 0x37, MPU9150_INTCFG_I2C_BYPASS_EN_BIT = 0x02
		//writeByte(MPU9150_RA_INT_PIN_CFG, 0x02);
		setI2CBypassEnabled(true);
		 */
		if (bypass_mode != null && bypass_mode.booleanValue() == bypass_on) {
			return;
		}

		byte tmp;

		if (bypass_on) {
			// user_ctrl == 0x6a == MPU9150_RA_USER_CTRL for MPU-6050
			tmp = readByte(MPU9150_RA_USER_CTRL);
			// BIT_AUX_IF_EN == 0x20 (bit 5, I2C_MST_EN, MPU9150_USERCTRL_I2C_MST_EN_BIT)
			tmp &= ~BIT_AUX_IF_EN;
			writeByte(MPU9150_RA_USER_CTRL, tmp);
			// Can be achieved by this instead:
			//setI2CMasterModeEnabled(true);
			
			SleepUtil.sleepMillis(3);
			
			// BIT_BYPASS_EN = 0x02 (bit 1, I2C_BYPASS_EN, MPU9150_INTCFG_I2C_BYPASS_EN_BIT)
			tmp = BIT_BYPASS_EN;
			if (active_low_int) {
				// BIT_ACTL == 0x80 (bit 7, INT_LEVEL, MPU9150_INTCFG_INT_LEVEL_BIT)
				tmp |= BIT_ACTL;
			}
			if (latched_int) {
				// BIT_LATCH_EN == 0x20 (bit 5, LATCH_INT_EN)
				// BIT_ANY_RD_CLR == 0x10 (bit 4, INT_RD_CLEAR)
				tmp |= BIT_LATCH_EN | BIT_ANY_RD_CLR;
			}
			// int_pin_cfg == 0x37 == MPU9150_RA_INT_PIN_CFG
			writeByte(MPU9150_RA_INT_PIN_CFG, tmp);
		} else {
			/* Enable I2C master mode if compass is being used. */
			tmp = readByte(MPU9150_RA_USER_CTRL);
			if ((sensors & INV_XYZ_COMPASS) != 0) {
				tmp |= BIT_AUX_IF_EN;
			} else {
				tmp &= ~BIT_AUX_IF_EN;
			}
			writeByte(MPU9150_RA_USER_CTRL, tmp);
			SleepUtil.sleepMillis(3);
			if (active_low_int) {
				tmp = BIT_ACTL;
			} else {
				tmp = 0;
			}
			if (latched_int) {
				tmp |= BIT_LATCH_EN | BIT_ANY_RD_CLR;
			}
			writeByte(MPU9150_RA_INT_PIN_CFG, tmp);
		}
		bypass_mode = Boolean.valueOf(bypass_on);
	}

	/**
	 *  @brief	  Set interrupt level.
	 *  @param[in]  active_low  1 for active low, 0 for active high.
	 */
	public void mpu_set_int_level(boolean active_low) {
		active_low_int = active_low;
	}
	
	/**
	 *  @brief	  Enable latched interrupts.
	 *  Any MPU register will clear the interrupt.
	 *  @param[in]  enable  1 to enable, 0 to disable.
	 */
	public void mpu_set_int_latched(boolean enable) throws IOException {
		if (latched_int == enable) {
			return;
		}
	
		byte tmp;
		if (enable) {
			tmp = BIT_LATCH_EN | BIT_ANY_RD_CLR;
		} else {
			tmp = 0;
		}
		if (bypass_mode != null && bypass_mode.booleanValue()) {
			tmp |= BIT_BYPASS_EN;
		}
		if (active_low_int) {
			tmp |= BIT_ACTL;
		}
		// int_pin_cfg == 0x37 == MPU9150_RA_INT_PIN_CFG
		writeByte(MPU9150_RA_INT_PIN_CFG, tmp);
		latched_int = enable;
	}
	
	public float[] get_accel_prod_shift() throws IOException {
		byte[] tmp = new byte[4];
		tmp = readBytes(0x0D, 4);
		//if (i2c_read(st.hw->addr, 0x0D, 4, tmp))
		// return 0x07;

		float[] st_shift = new float[3];
		byte[] shift_code = new byte[3];
		shift_code[0] = (byte)(((tmp[0] & 0xE0) >> 3) | ((tmp[3] & 0x30) >> 4));
		shift_code[1] = (byte)(((tmp[1] & 0xE0) >> 3) | ((tmp[3] & 0x0C) >> 2));
		shift_code[2] = (byte)(((tmp[2] & 0xE0) >> 3) | (tmp[3] & 0x03));
		for (int ii = 0; ii < 3; ii++) {
			if (shift_code[ii] == 0) {
				st_shift[ii] = 0.f;
				continue;
			}
			/* Equivalent to..
			 * st_shift[ii] = 0.34f * powf(0.92f/0.34f, (shift_code[ii]-1) / 30.f)
			 */
			st_shift[ii] = 0.34f;
			while (--shift_code[ii] != 0) {
				st_shift[ii] *= 1.034f;
			}
		}
		return st_shift;
	}
	
	public void get_st_biases() throws IOException {
		// TODO Implementation
		logger.error("get_st_biases NOT IMPLEMENTED!");
	}
	
	/**
	 *  @brief	  Write to the DMP memory.
	 *  This function prevents I2C writes past the bank boundaries. The DMP memory
	 *  is only accessible when the chip is awake.
	 *  @param[in]  mem_addr	Memory location (bank << 8 | start address)
	 *  @param[in]  length	  Number of bytes to write.
	 *  @param[in]  data		Bytes to write to memory.
	 */
	public void mpu_write_mem(int mem_addr, int length, byte[] data) throws IOException {
		if (sensors == 0) {
			logger.warn("mpu_write_mem(), sensors == 0, returning");
			return;
		}

		byte[] tmp = new byte[2];
		// Bank
		tmp[0] = (byte)(mem_addr >> 8);
		// Start address
		tmp[1] = (byte)(mem_addr & 0xFF);

		/* Check bank boundaries. */
		int start_address = tmp[1] & 0xFF;
		if (start_address + length > MPU9150_DMP_MEMORY_BANK_SIZE) {
			logger.error("mpu_write_mem(), check bank boundaries failed");
			return;
		}

		// bank_sel == 0x6D == MPU9150_RA_BANK_SEL
		//if (i2c_write(st.hw->addr, st.reg->bank_sel, 2, tmp))
		writeBytes(MPU9150_RA_BANK_SEL, 2, tmp);
		// mem_r_w == 0x6F == MPU9150_RA_MEM_R_W
		//if (i2c_write(st.hw->addr, st.reg->mem_r_w, length, data))
		writeBytes(MPU9150_RA_MEM_R_W, length, data);
	}
	
	/**
	 *  @brief	  Read from the DMP memory.
	 *  This function prevents I2C reads past the bank boundaries. The DMP memory
	 *  is only accessible when the chip is awake.
	 *  @param[in]  mem_addr	Memory location (bank << 8 | start address)
	 *  @param[in]  length	  Number of bytes to read.
	 *  @return data		Bytes read from memory.
	 */
	public byte[] mpu_read_mem(int mem_addr, int length) throws IOException {

		if (sensors == 0) {
			return null;
		}
		
		byte[] tmp = new byte[2];
		tmp[0] = (byte)(mem_addr >> 8);		// memory bank
		tmp[1] = (byte)(mem_addr & 0xFF);	// start address
		int tmp_1_unsigned = tmp[1] & 0xFF;

		/* Check bank boundaries. */
		if (tmp_1_unsigned + length > MPU9150_DMP_MEMORY_BANK_SIZE) {
			logger.error("mpu_read_mem(), check bank boundaries failed");
			return null;
		}

		// bank_sel == 0x6D == MPU9150_RA_BANK_SEL
		//if (i2c_write(st.hw->addr, st.reg->bank_sel, 2, tmp))
		writeBytes(MPU9150_RA_BANK_SEL, 2, tmp);
		
		// mem_r_w == 0x6F == MPU9150_RA_MEM_R_W
		//if (i2c_read(st.hw->addr, st.reg->mem_r_w, length, data))
		return readBytes(MPU9150_RA_MEM_R_W, length);
	}
	
	/**
	 *  @brief	  Load and verify DMP image.
	 *  @param[in]  length	  Length of DMP image.
	 *  @param[in]  firmware	DMP code.
	 *  @param[in]  start_addr  Starting address of DMP code memory.
	 *  @param[in]  sample_rate Fixed sampling rate used when DMP is enabled.
	 */
	public void mpu_load_firmware(int length, byte[] firmware, short start_addr, int sample_rate) throws IOException {
		/* Must divide evenly into st.hw->bank_size to avoid bank crossings. */
		if (dmp_loaded) {
			/* DMP should only be loaded once. */
			logger.warn("mpu_load_firmware(), DMP already loaded, returning");
			return;
		}

		int LOAD_CHUNK = 16;

		int this_write;
		for (int ii = 0; ii < length; ii += this_write) {
			this_write = Math.min(LOAD_CHUNK, length - ii);
			//logger.trace("mpu_load_firmware, this_write=" + this_write);
			/*
			if (mpu_write_mem(ii, this_write, (unsigned char*)&firmware[ii]))
				return -1;
			*/
			byte[] chunk = Arrays.copyOfRange(firmware, ii, ii+this_write);
			mpu_write_mem(ii, this_write, chunk);
			/*
			if (mpu_read_mem(ii, this_write, cur))
				return -1;
			*/
			byte[] cur = mpu_read_mem(ii, this_write);
			//if (memcmp(firmware+ii, cur, this_write))
			//	return -2;
			if (!Arrays.equals(chunk, cur)) {
				logger.debug("mpu_load_firmware(), mpu_read_mem(" + ii + ", " + this_write + ") != data chunk just written");
			}
		}

		/* Set program start address. */
		/*
		short[] tmp = new short[2];
		tmp[0] = (short)(start_addr >> 8);
		tmp[1] = (short)(start_addr & 0xFF);
		*/
		// prgm_start_h == 0x70 == MPU9150_RA_DMP_CFG_1
		writeShort(MPU9150_RA_DMP_CFG_1, start_addr);

		dmp_loaded = true;
		dmp_sample_rate = sample_rate;
	}
	
	/**
	 *  @brief	  Enable/disable DMP support.
	 *  @param[in]  enable  1 to turn on the DMP.
	 */
	public void mpu_set_dmp_state(boolean enable) throws IOException {
		if (dmp_on == enable) {
			return;
		}

		byte tmp;
		if (enable) {
			if (!dmp_loaded) {
				logger.warn("mpu_set_dmp_state(), dmp not loaded, returning");
				return;
			}
			/* Disable data ready interrupt. */
			set_int_enable(false);
			/* Disable bypass mode. */
			mpu_set_bypass(false);
			/* Keep constant sample rate, FIFO rate controlled by DMP. */
			mpu_set_sample_rate(dmp_sample_rate);
			/* Remove FIFO elements. */
			tmp = 0;
			writeByte(MPU9150_RA_FIFO_EN, tmp);
			dmp_on = true;
			/* Enable DMP interrupt. */
			set_int_enable(true);
			mpu_reset_fifo();
		} else {
			/* Disable DMP interrupt. */
			set_int_enable(false);
			/* Restore FIFO settings. */
			tmp = fifo_enable;
			writeByte(MPU9150_RA_FIFO_EN, tmp);
			dmp_on = true;
			mpu_reset_fifo();
		}
	}
	
	/**
	 *  @brief	  Get DMP state.
	 *  @return enabled true if enabled.
	 */
	public boolean mpu_get_dmp_state() {
		return dmp_on;
	}
	
	/* This initialisation is similar to the one in ak8975.c. */
	public boolean setup_compass() throws IOException {
		mpu_set_bypass(true);
	
		/* Find compass. Possible addresses range from 0x0C to 0x0F. */
		byte akm_addr = AK8975Driver.AK8975_MAG_ADDRESS;
		/* Assume it's on 0x0C...
		for (akm_addr = 0x0C; akm_addr <= 0x0F; akm_addr++) {
			int result;
			result = read(akm_addr, AKM_REG_WHOAMI, 1, data);
			if (data[0] == AKM_WHOAMI) {
				break;
			}
		}
		*/

		if (akm_addr > 0x0F) {
			/* TODO: Handle this case in all compass-related functions. */
			logger.warn("Compass not found.\n");
			return false;
		}

		compass_addr = akm_addr;

		magSensor = new AK8975Driver(getController(), getAddressSize(), getClockFrequency(), compass_addr);
		magSensor.init();

		mpu_set_bypass(false);
		
		/* Set up master mode, master clock, and ES bit. */
		byte data = 0x40;
		/*
		 * IST_MST_CLK is Bit3-Bit0
		 * I2C_MST_CLK	I2C Master Clock Speed	8MHz Clock Divider
		 * 0			348 kHz					23
		 * 1			333 kHz					24
		 * 2			320 kHz					25
		 * 3			308 kHz					26
		 * 4			296 kHz					27
		 * 5			286 kHz					28
		 * 6			276 kHz					29
		 * 7			267 kHz					30
		 * 8			258 kHz					31
		 * 9			500 kHz					16
		 * 10			471 kHz					17
		 * 11			444 kHz					18
		 * 12			421 kHz					19
		 * 13			400 kHz					20
		 * 14			381 kHz					21
		 * 15			364 kHz					22
		 */
		// i2c_mst == 0x24 == MPU9150_RA_I2C_MST_CTRL
		writeByte(MPU9150_RA_I2C_MST_CTRL, data);

		/* Slave 0 reads from AKM data registers. */
		data = (byte)(BIT_I2C_READ | compass_addr);
		// s0_addr == 0x25 == MPU9150_RA_I2C_SLV0_ADDR
		writeByte(MPU9150_RA_I2C_SLV0_ADDR, data);

		/* Compass reads start at this register. */
		data = AKM_REG_ST1;
		// s0_reg == 0x26 == MPU9150_RA_I2C_SLV0_REG
		writeByte(MPU9150_RA_I2C_SLV0_REG, data);

		/* Enable slave 0, 8-byte reads. */
		data = BIT_SLAVE_EN | 8;
		// s0_ctrl == 0x27 == MPU9150_RA_I2C_SLV0_CTRL
		writeByte(MPU9150_RA_I2C_SLV0_CTRL, data);

		/* Slave 1 changes AKM measurement mode. */
		data = compass_addr;
		// s1_addr == 0x28 == MPU9150_RA_I2C_SLV1_ADDR
		writeByte(MPU9150_RA_I2C_SLV1_ADDR, data);

		/* AKM measurement mode register. */
		data = AKM_REG_CNTL;
		// s1_reg == 0x29 == MPU9150_RA_I2C_SLV1_REG
		writeByte(MPU9150_RA_I2C_SLV1_REG, data);

		/* Enable slave 1, 1-byte writes. */
		data = BIT_SLAVE_EN | 1;
		// s1_ctrl == 0x2A == MPU9150_RA_I2C_SLV1_CTRL
		writeByte(MPU9150_RA_I2C_SLV1_CTRL, data);

		/* Set slave 1 data. */
		data = AKM_SINGLE_MEASUREMENT;
		// s1_do == 0x64 == MPU9150_RA_I2C_SLV1_DO
		writeByte(MPU9150_RA_I2C_SLV1_DO, data);

		/* Trigger slave 0 and slave 1 actions at each sample. */
		data = 0x03;
		// i2c_delay_ctrl == 0x67 == MPU9150_RA_I2C_MST_DELAY_CTRL
		writeByte(MPU9150_RA_I2C_MST_DELAY_CTRL, data);

	//#ifdef MPU9150
		/* For the MPU9150, the auxiliary I2C bus needs to be set to VDD. */
		data = BIT_I2C_MST_VDDIO;
		// yg_offs_tc == 0x01 == MPU9150_RA_YG_OFFS_TC
		writeByte(MPU9150_RA_YG_OFFS_TC, data);
	//#endif

		return true;
	}
	
	/**
	 *  @brief	  Read raw compass data.
	 *  @param[out] data		Raw data in hardware units.
	 *  @param[out] timestamp   Timestamp in milliseconds. Null if not needed.
	 *  @return	 0 if successful.
	 */
	public short[] mpu_get_compass_reg() throws IOException {
		if ((sensors & INV_XYZ_COMPASS) == 0) {
			logger.warn("mpu_get_compass_reg(), INV_XYZ_COMPASS not set in sensors");
			return null;
		}

		// raw_compass == 0x49 == MPU9150_RA_EXT_SENS_DATA_00
		byte[] tmp = readBytes(MPU9150_RA_EXT_SENS_DATA_00, 8);
		/* AK8975 doesn't have the overrun error bit. */
		if ((tmp[0] & AKM_DATA_READY) == 0) {
			return null;
		}
		if (((tmp[7] & AKM_OVERFLOW) != 0) || ((tmp[7] & AKM_DATA_ERROR) != 0)) {
			return null;
		}
		
		short[] data = new short[3];
		// Note Little Endian!
		data[0] = (short)(((tmp[2] & 0xff) << 8) | (tmp[1] & 0xff));
		data[1] = (short)(((tmp[4] & 0xff) << 8) | (tmp[3] & 0xff));
		data[2] = (short)(((tmp[6] & 0xff) << 8) | (tmp[5] & 0xff));

		short[] mag_sens_adj = magSensor.get_mag_sens_adj();
		//System.out.format("mag_sens_adj=(%d, %d, %d)%n", mag_sens_adj[0], mag_sens_adj[1], mag_sens_adj[2]);
		// TODO Try to understand this...
		data[0] = (short)((data[0] * mag_sens_adj[0]) >> 8);
		data[1] = (short)((data[1] * mag_sens_adj[1]) >> 8);
		data[2] = (short)((data[2] * mag_sens_adj[2]) >> 8);

		return data;
	}
	
	/**
	 *  @brief	  Get the compass full-scale range.
	 *  @return fsr Current full-scale range.
	 */
	public int mpu_get_compass_fsr() {
		return AK8975_FSR;
	}
	
	/**
	 *  @brief	  Enters LP accel motion interrupt mode.
	 *  The behaviour of this feature is very different between the MPU6050 and the
	 *  MPU6500. Each chip's version of this feature is explained below.
	 *
	 *  The hardware motion threshold can be between 32mg and 8160mg in 32mg
	 *  increments.
	 *
	 *  Low-power accel mode supports the following frequencies:
	 *  1.25Hz, 5Hz, 20Hz, 40Hz
	 *
	 *  MPU6500:
	 *  Unlike the MPU6050 version, the hardware does not "lock in" a reference
	 *  sample. The hardware monitors the accel data and detects any large change
	 *  over a short period of time.
	 *
	 *  The hardware motion threshold can be between 4mg and 1020mg in 4mg
	 *  increments.
	 *
	 *  MPU6500 Low-power accel mode supports the following frequencies:
	 *  1.25Hz, 2.5Hz, 5Hz, 10Hz, 20Hz, 40Hz, 80Hz, 160Hz, 320Hz, 640Hz
	 *
	 *  NOTES:
	 *  The driver will round down @e thresh to the nearest supported value if
	 *  an unsupported threshold is selected.
	 *  To select a fractional wake-up frequency, round down the value passed to
	 *  @e lpa_freq.
	 *  The MPU6500 does not support a delay parameter. If this function is used
	 *  for the MPU6500, the value passed to @e time will be ignored.
	 *  To disable this mode, set @e lpa_freq to zero. The driver will restore
	 *  the previous configuration.
	 *
	 *  @param[in]  thresh	  Motion threshold in mg.
	 *  @param[in]  time		Duration in milliseconds that the accel data must
	 *						  exceed @e thresh before motion is reported.
	 *  @param[in]  lpa_freq	Minimum sampling rate, or zero to disable.
	 */
	 public void mpu_lp_motion_interrupt(int thresh, int time, int lpa_freq) throws IOException {
		 // TODO Implementation
		 logger.error("mpu_lp_motion_interrupt NOT IMPLEMENTED!");
	 }
}
