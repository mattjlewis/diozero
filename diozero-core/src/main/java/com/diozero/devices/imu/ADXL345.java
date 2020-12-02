package com.diozero.devices.imu;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     ADXL345.java  
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

import java.nio.ByteBuffer;
import java.util.Arrays;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import com.diozero.api.I2CConstants;
import com.diozero.api.I2CDevice;
import com.diozero.api.RuntimeIOException;
import com.diozero.util.BitManipulation;

/**
 * http://www.analog.com/media/en/technical-documentation/data-sheets/ADXL345.PDF
 */
public class ADXL345 implements ImuInterface {
	// 13-bit
	private static final int RESOLUTION = 13;
	private static final int RANGE = (int) Math.pow(2, RESOLUTION);
	// From -16 to 16
	private static final double MAX_RANGE = 16 * 2;
	private static final double SCALE_FACTOR = MAX_RANGE / RANGE;

	private static final int ADXL345_ADDRESS = 0x53;

	private static final int[] RANGE_LIST = { 2, 4, 8, 16 };

	// Registers
	private static final int THRESH_TAP_REG = 0x1D; // Tap threshold
	private static final int OFSX = 0x1E; // X-axis offset
	private static final int OFSY = 0x1F; // Y-axis offset
	private static final int OFSZ = 0x20; // Z-axis offset
	private static final int TAP_DUR = 0x21; // Tap duration
	private static final int TAP_LATENCY = 0x22; // Tap latency
	private static final int TAP_WINDOW_REG = 0x23; // Tap window
	private static final int THRESH_ACT_REG = 0x24; // Activity threshold
	private static final int THRESH_INACT_REG = 0x25; // Inactivity threshold
	private static final int TIME_INACT_REG = 0x26; // Inactivity time
	private static final int ACT_INACT_CTL_REG = 0x27; // Axis enable control for activity and inactivity detection
	private static final int THRESH_FF_REG = 0x28; // Free-fall threshold
	private static final int TIME_FF_REG = 0x29; // Free-fall time
	private static final int TAP_AXES_REG = 0x2A; // Axis control for single tap/double tap
	private static final int ACT_TAP_STATUS = 0x2B; // Source of single tap/double tap
	private static final int BW_RATE = 0x2C; // Data rate and power mode control
	private static final int POWER_CTL = 0x2D; // Power-saving features control
	private static final int INT_ENABLE = 0x2E; // Interrupt enable control
	private static final int INT_MAP = 0x2F; // Interrupt mapping control
	private static final int INT_SOURCE = 0x30; // Source of interrupts
	private static final int DATA_FORMAT = 0x31; // Data format control
	private static final int AXES_DATA = 0x32; // 3 sets of 2 byte axis data
	private static final int FIFO_CTL = 0x38; // FIFO control
	private static final int FIFO_STATUS = 0x39; // FIFO status

	private static final byte POWER_CTL_LINK_BIT = 5;
	private static final byte POWER_CTL_LINK = BitManipulation.getBitMask(POWER_CTL_LINK_BIT);
	private static final byte POWER_CTL_AUTO_SLEEP_BIT = 4;
	private static final byte POWER_CTL_AUTO_SLEEP = BitManipulation.getBitMask(POWER_CTL_AUTO_SLEEP_BIT);
	private static final byte POWER_CTL_MEASURE_BIT = 3;
	private static final byte POWER_CTL_MEASURE = BitManipulation.getBitMask(POWER_CTL_MEASURE_BIT);
	private static final byte POWER_CTL_SLEEP_BIT = 2;
	private static final byte POWER_CTL_SLEEP = BitManipulation.getBitMask(POWER_CTL_SLEEP_BIT);
	private static final byte LOW_POWER_MODE_BIT = 4;
	private static final byte LOW_POWER_MODE = BitManipulation.getBitMask(LOW_POWER_MODE_BIT);
	private static final byte SUPPRESS_DOUBLE_TAP_BIT = 3;
	private static final byte SUPPRESS_DOUBLE_TAP = BitManipulation.getBitMask(SUPPRESS_DOUBLE_TAP_BIT);
	private static final byte FULL_RESOLUTION_MODE_BIT = 3;
	private static final byte FULL_RESOLUTION_MODE = BitManipulation.getBitMask(FULL_RESOLUTION_MODE_BIT);
	private static final byte SELF_TEST_MODE_BIT = 7;
	private static final byte SELF_TEST_MODE = BitManipulation.getBitMask(SELF_TEST_MODE_BIT);

	private static final float THRESH_TAP_RANGE = 16f;
	private static final float THRESH_TAP_LSB = THRESH_TAP_RANGE / 0xFF;
	private static final float OFFSET_RANGE = 2f;
	private static final float OFFSET_LSB = OFFSET_RANGE / 0x7F;
	private static final float TAP_DURATION_MS_RANGE = 160;
	private static final float TAP_DURATION_MS_LSB = TAP_DURATION_MS_RANGE / 0xff;
	private static final float TAP_LATENCY_MS_RANGE = 320;
	private static final float TAP_LATENCY_MS_LSB = TAP_LATENCY_MS_RANGE / 0xff;
	private static final float TAP_WINDOW_MS_RANGE = 320;
	private static final float TAP_WINDOW_MS_LSB = TAP_WINDOW_MS_RANGE / 0xff;
	private static final float ACTIVITY_THRESHOLD_RANGE = 16;
	private static final float ACTIVITY_THRESHOLD_LSB = ACTIVITY_THRESHOLD_RANGE / 0xff;
	private static final float INACTIVITY_THRESHOLD_RANGE = 16;
	private static final float INACTIVITY_THRESHOLD_LSB = INACTIVITY_THRESHOLD_RANGE / 0xff;
	private static final float INACTIVITY_TIME_RANGE = 256;
	private static final float INACTIVITY_TIME_LSB = INACTIVITY_TIME_RANGE / 0xff;
	private static final float FREEFALL_THRESHOLD_RANGE = 16;
	private static final float FREEFALL_THRESHOLD_LSB = FREEFALL_THRESHOLD_RANGE / 0xff;
	private static final float FREEFALL_TIME_RANGE = 1280;
	private static final float FREEFALL_TIME_LSB = FREEFALL_TIME_RANGE / 0xff;

	private I2CDevice device;

	public ADXL345() {
		device = I2CDevice.builder(ADXL345_ADDRESS).setController(I2CConstants.CONTROLLER_1).build();
		setNormalMeasurementMode();
	}

	/**
	 * Get the tap threshold in g
	 * 
	 * @return Tap threshold (g)
	 */
	public float getTapThreshold() {
		return device.readUByte(THRESH_TAP_REG) * THRESH_TAP_LSB;
	}

	/**
	 * Set the tap threshold in g
	 * 
	 * @param tapThreshold The threshold value in g for tap interrupts
	 */
	public void setTapThreshold(float tapThreshold) {
		if (tapThreshold < 0 || tapThreshold > THRESH_TAP_RANGE) {
			throw new IllegalArgumentException(
					"Illegal tap threshold value (" + tapThreshold + "), must be 0.." + THRESH_TAP_RANGE);
		}
		device.writeByteData(THRESH_TAP_REG, (byte) (Math.floor(tapThreshold / THRESH_TAP_LSB)));
	}

	private float getOffset(int register) {
		// Signed 8-bit
		return device.readByteData(register) * OFFSET_LSB;
	}

	private void setOffset(int register, float offset) {
		if (offset < 0 || offset > OFFSET_RANGE) {
			throw new IllegalArgumentException("Illegal offset value (" + offset + "), must be 0.." + OFFSET_RANGE);
		}
		device.writeByteData(register, (byte) (Math.floor(offset / OFFSET_LSB)));
	}

	public float getOffsetX() {
		return getOffset(OFSX);
	}

	/**
	 * Set the X-axis offset in g
	 * 
	 * @param offset Offset value (g)
	 */
	public void setOffsetX(float offset) {
		setOffset(OFSX, offset);
	}

	public float getOffsetY() {
		return getOffset(OFSY);
	}

	/**
	 * Set the Y-axis offset in g
	 * 
	 * @param offset Offset value (g)
	 */
	public void setOffsetY(float offset) {
		setOffset(OFSY, offset);
	}

	public float getOffsetZ() {
		return getOffset(OFSZ);
	}

	/**
	 * Set the Z-axis offset in g
	 * 
	 * @param offset Offset value (g)
	 */
	public void setOffsetZ(float offset) {
		setOffset(OFSZ, offset);
	}

	public float[] getOffsets() {
		return new float[] { getOffset(OFSX), getOffset(OFSY), getOffset(OFSZ) };
	}

	public void setOffsets(float offsetX, float offsetY, float offsetZ) {
		setOffset(OFSX, offsetX);
		setOffset(OFSY, offsetY);
		setOffset(OFSZ, offsetZ);
	}

	/**
	 * Get the tap duration in milliseconds
	 * 
	 * @return Tap duration (milliseconds)
	 */
	public float getTapDuration() {
		return device.readUByte(TAP_DUR) * TAP_DURATION_MS_LSB;
	}

	/**
	 * Set the tap duration in mS
	 * 
	 * @param tapDuration The maximum time in ms that an event must be above to
	 *                    qualify as a tap event
	 */
	public void setTapDuration(float tapDuration) {
		if (tapDuration < 0 || tapDuration > TAP_DURATION_MS_RANGE) {
			throw new IllegalArgumentException(
					"Illegal tap duration value (" + tapDuration + "), must be 0.." + TAP_DURATION_MS_RANGE);
		}
		device.writeByteData(TAP_DUR, (byte) (Math.floor(tapDuration / TAP_DURATION_MS_LSB)));
	}

	/**
	 * Get the tap latency in milliseconds
	 * 
	 * @return The tap latency (milliseconds)
	 */
	public float getTapLatency() {
		return device.readUByte(TAP_LATENCY) * TAP_LATENCY_MS_LSB;
	}

	/**
	 * Set the tap latency in mS
	 * 
	 * @param tapLatency The wait time in mS from the detection of a tap event to
	 *                   the start of the time window during which a possible second
	 *                   tap event can be detected
	 */
	public void setTapLatency(float tapLatency) {
		if (tapLatency < 0 || tapLatency > TAP_LATENCY_MS_RANGE) {
			throw new IllegalArgumentException(
					"Illegal tap latency value (" + tapLatency + "), must be 0.." + TAP_LATENCY_MS_RANGE);
		}
		device.writeByteData(TAP_LATENCY, (byte) (Math.floor(tapLatency / TAP_LATENCY_MS_LSB)));
	}

	/**
	 * Get the tap window in milliseconds
	 * 
	 * @return Tap window (milliseconds)
	 */
	public float getTapWindow() {
		return device.readUByte(TAP_WINDOW_REG) * TAP_WINDOW_MS_LSB;
	}

	/**
	 * Set the tap window in mS
	 * 
	 * @param tapWindow The amount of time in milliseconds after the expiration of
	 *                  the latency time during which a second valid tap can begin
	 */
	public void setTapWindow(float tapWindow) {
		if (tapWindow < 0 || tapWindow > TAP_WINDOW_MS_RANGE) {
			throw new IllegalArgumentException(
					"Illegal tap window value (" + tapWindow + "), must be 0.." + TAP_WINDOW_MS_RANGE);
		}
		device.writeByteData(TAP_WINDOW_REG, (byte) (Math.floor(tapWindow / TAP_WINDOW_MS_LSB)));
	}

	public float getActivityThreshold() {
		return device.readUByte(THRESH_ACT_REG) * ACTIVITY_THRESHOLD_LSB;
	}

	/**
	 * Set the activity threshold value in g
	 * 
	 * @param activityThreshold The threshold value for detecting activity
	 */
	public void setActivityThreshold(float activityThreshold) {
		if (activityThreshold < 0 || activityThreshold > ACTIVITY_THRESHOLD_RANGE) {
			throw new IllegalArgumentException("Illegal activity threshold value (" + activityThreshold
					+ "), must be 0.." + ACTIVITY_THRESHOLD_RANGE);
		}
		device.writeByteData(THRESH_ACT_REG, (byte) (Math.floor(activityThreshold / ACTIVITY_THRESHOLD_LSB)));
	}

	public float getInactivityThreshold() {
		return device.readUByte(THRESH_INACT_REG) * INACTIVITY_THRESHOLD_LSB;
	}

	/**
	 * Set the inactivity threshold value in g
	 * 
	 * @param inactivityThreshold The threshold value for detecting inactivity
	 */
	public void setInactivityThreshold(float inactivityThreshold) {
		if (inactivityThreshold < 0 || inactivityThreshold > INACTIVITY_THRESHOLD_RANGE) {
			throw new IllegalArgumentException("Illegal inactivity threshold value (" + inactivityThreshold
					+ "), must be 0.." + INACTIVITY_THRESHOLD_RANGE);
		}
		device.writeByteData(THRESH_INACT_REG, (byte) (Math.floor(inactivityThreshold / INACTIVITY_THRESHOLD_LSB)));
	}

	public float getInactivityTime() {
		return device.readUByte(TIME_INACT_REG) * INACTIVITY_TIME_LSB;
	}

	/**
	 * Set the inactivity time value in mS
	 * 
	 * @param inactivityTime Value representing the amount of time that acceleration
	 *                       must be less than the value in the THRESH_INACT
	 *                       register for inactivity to be declared
	 */
	public void setInactivityTime(float inactivityTime) {
		if (inactivityTime < 0 || inactivityTime > INACTIVITY_TIME_RANGE) {
			throw new IllegalArgumentException(
					"Illegal inactivity time value (" + inactivityTime + "), must be 0.." + INACTIVITY_TIME_RANGE);
		}
		device.writeByteData(TIME_INACT_REG, (byte) (Math.floor(inactivityTime / INACTIVITY_TIME_LSB)));
	}

	/**
	 * D7 - Activity ac/dc D6 - ACT_X enable D5 - ACT_Y enable D4 - ACT_Z enable D3
	 * - Inactivity ac/dc D2 - INACT_X enable D1 - INACT_Y enable D0 - INACT_Z
	 * enable
	 * 
	 * A setting of 0 selects dc-coupled operation, and a setting of 1 enables
	 * ac-coupled operation. In dc-coupled operation, the current acceleration
	 * magnitude is compared directly with THRESH_ACT and THRESH_INACT to determine
	 * whether activity or inactivity is detected. In ac-coupled operation for
	 * activity detection, the acceleration value at the start of activity detection
	 * is taken as a reference value. New samples of acceleration are then compared
	 * to this reference value, and if the magnitude of the difference exceeds the
	 * THRESH_ACT value, the device triggers an activity interrupt. Similarly, in
	 * ac-coupled operation for inactivity detection, a reference value is used for
	 * comparison and is updated whenever the device exceeds the inactivity
	 * threshold. After the reference value is selected, the device compares the
	 * magnitude of the difference between the reference value and the current
	 * acceleration with THRESH_INACT. If the difference is less than the value in
	 * THRESH_INACT for the time in TIME_INACT, the device is considered inactive
	 * and the inactivity interrupt is triggered.
	 * 
	 * @return Activity / inativity control flags
	 */
	public byte getActivityInactivityControlFlags() {
		return device.readByteData(ACT_INACT_CTL_REG);
	}

	public void setActivityInactivityControlFlags(byte flags) {
		device.writeByteData(ACT_INACT_CTL_REG, flags);
	}

	public float getFreefallThreshold() {
		return device.readUByte(THRESH_FF_REG) * FREEFALL_THRESHOLD_LSB;
	}

	/**
	 * Set the freefall threshold value in g
	 * 
	 * @param freefallThreshold The threshold value for detecting inactivity
	 */
	public void setFreefallThreshold(float freefallThreshold) {
		if (freefallThreshold < 0 || freefallThreshold > FREEFALL_THRESHOLD_RANGE) {
			throw new IllegalArgumentException("Illegal freefall threshold value (" + freefallThreshold
					+ "), must be 0.." + FREEFALL_THRESHOLD_RANGE);
		}
		device.writeByteData(THRESH_FF_REG, (byte) (Math.floor(freefallThreshold / FREEFALL_THRESHOLD_LSB)));
	}

	public float getFreefallTime() {
		return device.readUByte(TIME_FF_REG) * FREEFALL_TIME_LSB;
	}

	/**
	 * Set the freefall time value in mS
	 * 
	 * @param freefallTime Value representing minimum time that the value of all
	 *                     axes must be less than THRESH_FF to generate a freefall
	 *                     interrupt
	 */
	public void setFreefallTime(float freefallTime) {
		if (freefallTime < 0 || freefallTime > FREEFALL_TIME_RANGE) {
			throw new IllegalArgumentException(
					"Illegal freefall time value (" + freefallTime + "), must be 0.." + FREEFALL_TIME_RANGE);
		}
		device.writeByteData(TIME_FF_REG, (byte) (Math.floor(freefallTime / FREEFALL_TIME_LSB)));
	}

	public boolean isDoubleTapSuppressed() {
		return (device.readByteData(TAP_AXES_REG) & SUPPRESS_DOUBLE_TAP) != 0;
	}

	public void setDoubleTapSuppressed(boolean doubleTapSuppressed) {
		byte old_val = device.readByteData(TAP_AXES_REG);
		if (doubleTapSuppressed != ((old_val & SUPPRESS_DOUBLE_TAP) != 0)) {
			device.writeByteData(TAP_AXES_REG,
					doubleTapSuppressed ? old_val | SUPPRESS_DOUBLE_TAP : old_val & ~SUPPRESS_DOUBLE_TAP);
		}
	}

	public byte getTapActivityStatusFlags() {
		return device.readByteData(ACT_TAP_STATUS);
	}

	public boolean isLowPowerMode() {
		return (device.readByteData(BW_RATE) & LOW_POWER_MODE) != 0;
	}

	public void setLowPowerMode(boolean lowPowerMode) {
		byte old_val = device.readByteData(BW_RATE);
		if (lowPowerMode != ((old_val & LOW_POWER_MODE) != 0)) {
			device.writeByteData(BW_RATE, lowPowerMode ? old_val | LOW_POWER_MODE : old_val & ~LOW_POWER_MODE);
		}
	}

	public OutputDataRateType getBandwidthDataRate() throws RuntimeIOException {
		return OutputDataRateType.TYPES[device.readByteData(BW_RATE) & 0x0f];
	}

	public void setBandwidthRate(OutputDataRateType dataRate) throws RuntimeIOException {
		byte old_val = device.readByteData(BW_RATE);
		device.writeByteData(BW_RATE, (old_val & 0xf0) | dataRate.code);
	}

	public void setNormalMeasurementMode() throws RuntimeIOException {
		setPowerControlFlags(POWER_CTL_MEASURE);
	}

	public void setPowerControlFlags(byte powerControlValue) throws RuntimeIOException {
		// Enable measure mode
		device.writeByteData(POWER_CTL, powerControlValue);
	}

	public byte getInterruptEnableFlags() {
		return device.readByteData(INT_ENABLE);
	}

	public void setInterruptEnableFlags(byte flags) {
		device.writeByteData(INT_ENABLE, flags);
	}

	public byte getInterruptMapFlags() {
		return device.readByteData(INT_MAP);
	}

	public void setInterruptMapFlagS(byte flags) {
		device.writeByteData(INT_MAP, flags);
	}

	public byte getInterruptSourceFlags() {
		return device.readByteData(INT_SOURCE);
	}

	public boolean isFullResolutionMode() {
		return (device.readByteData(DATA_FORMAT) & FULL_RESOLUTION_MODE) != 0;
	}

	public void setFullResolutionMode(boolean fullResolution) {
		byte old_val = device.readByteData(DATA_FORMAT);
		if (fullResolution != ((old_val & FULL_RESOLUTION_MODE) != 0)) {
			device.writeByteData(DATA_FORMAT, fullResolution ? old_val | FULL_RESOLUTION_MODE : old_val & ~SELF_TEST_MODE);
		}
	}

	public boolean isSelfTestMode() {
		return (device.readByteData(DATA_FORMAT) & SELF_TEST_MODE) != 0;
	}

	public void setSelfTestMode(boolean selfTest) {
		byte old_val = device.readByteData(DATA_FORMAT);
		if (selfTest != ((old_val & SELF_TEST_MODE) != 0)) {
			device.writeByteData(DATA_FORMAT, selfTest ? old_val | SELF_TEST_MODE : old_val & ~SELF_TEST_MODE);
		}
	}

	public int getAccelFsr() {
		return RANGE_LIST[device.readByteData(DATA_FORMAT) & 0x3];
	}

	public void setAccelFsr(int range) {
		for (int i : RANGE_LIST) {
			if (RANGE_LIST[i] == range) {
				device.writeByteData(DATA_FORMAT, i);
				return;
			}
		}

		throw new IllegalArgumentException(
				"Invalid range value (" + range + "), must be one of " + Arrays.toString(RANGE_LIST));
	}

	/**
	 * D7 D6 | D5 | D4 D3 D2 D1 D0 FIFO_MODE | Trigger | Samples FIFO modes: 0
	 * Bypass - FIFO is bypassed 1 FIFO - FIFO collects up to 32 values and then
	 * stops collecting data, collecting new data only when FIFO is not full 2
	 * Stream - FIFO holds the last 32 data values. When FIFO is full, the oldest
	 * data is overwritten with newer data 3 Trigger - When triggered by the trigger
	 * bit, FIFO holds the last data samples before the trigger event and then
	 * continues to collect data until full. New data is collected only when FIFO is
	 * not full Trigger bit: A value of 0 in the trigger bit links the trigger event
	 * of trigger mode to INT1, and a value of 1 links the trigger event to INT2
	 * Samples: The function of these bits depends on the FIFO mode selected (see
	 * below). Entering a value of 0 in the samples bits immediately sets the
	 * watermark status bit in the INT_SOURCE register, regardless of which FIFO
	 * mode is selected. Undesirable operation may occur if a value of 0 is used for
	 * the samples bits when trigger mode is used FIFO Mode | Samples Bits Function
	 * Bypass | None. FIFO | Specifies how many FIFO entries are needed to trigger a
	 * watermark interrupt. Stream | Specifies how many FIFO entries are needed to
	 * trigger a watermark interrupt. Trigger | Specifies how many FIFO samples are
	 * retained in the FIFO buffer before a trigger event.
	 * 
	 * @return FIFO Control flags
	 */
	public byte getFifoControlFlags() {
		return device.readByteData(FIFO_CTL);
	}

	public void setFifoControlFlags(byte flags) {
		device.writeByteData(FIFO_CTL, flags);
	}

	/**
	 * D7 | D6 | D5 D4 D3 D2 D1 D0 FIFO Trig | 0 | Entries FIFO Trig: A 1 in the
	 * FIFO_TRIG bit corresponds to a trigger event occurring, and a 0 means that a
	 * FIFO trigger event has not occurred Entries: These bits report how many data
	 * values are stored in FIFO. Access to collect the data from FIFO is provided
	 * through the DATAX, DATAY, and DATAZ registers. FIFO reads must be done in
	 * burst or multiple-byte mode because each FIFO level is cleared after any read
	 * (single-or multiple-byte) of FIFO. FIFO stores a maximum of 32 entries, which
	 * equates to a maximum of 33 entries available at any given time because an
	 * additional entry is available at the output filter of the device.
	 * 
	 * @return FIFO status
	 */
	public byte getFifoStatus() {
		return device.readByteData(FIFO_STATUS);
	}

	@Override
	public ImuData getImuData() throws RuntimeIOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Vector3D getGyroData() throws RuntimeIOException {
		throw new UnsupportedOperationException("ADXL345 doesn't hava a gyro");
	}

	@Override
	public Vector3D getAccelerometerData() throws RuntimeIOException {
		byte[] data = new byte[6];
		device.readI2CBlockData(AXES_DATA, data);
		ByteBuffer buffer = ByteBuffer.wrap(data);
		short x = buffer.getShort();
		short y = buffer.getShort();
		short z = buffer.getShort();

		return ImuDataFactory.createVector(new short[] { x, y, z }, SCALE_FACTOR);
	}

	@Override
	public Vector3D getCompassData() throws RuntimeIOException {
		throw new UnsupportedOperationException("ADXL345 doesn't hava a compass");
	}

	@Override
	public boolean hasGyro() {
		return false;
	}

	@Override
	public boolean hasAccelerometer() {
		return true;
	}

	@Override
	public boolean hasCompass() {
		return false;
	}

	@Override
	public void addTapListener(TapListener listener) {
		// TODO Auto-generated method stub
	}

	@Override
	public void addOrientationListener(OrientationListener listener) {
		// TODO Auto-generated method stub
	}

	@Override
	public String getImuName() {
		return "ADXL345";
	}

	@Override
	public int getPollInterval() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void startRead() {
		setNormalMeasurementMode();
	}

	@Override
	public void stopRead() {
		setPowerControlFlags((byte) (POWER_CTL_AUTO_SLEEP | POWER_CTL_SLEEP));
	}

	public static class OutputDataRateType {
		public static final OutputDataRateType[] TYPES = { new OutputDataRateType(0B0000, 0.1f),
				new OutputDataRateType(0B0001, 0.2f), new OutputDataRateType(0B0010, 0.39f),
				new OutputDataRateType(0B0011, 0.78f), new OutputDataRateType(0B0100, 1.56f),
				new OutputDataRateType(0B0101, 3.13f), new OutputDataRateType(0B0110, 6.25f),
				new OutputDataRateType(0B0111, 12.5f), new OutputDataRateType(0B1000, 25f),
				new OutputDataRateType(0B1001, 50f), new OutputDataRateType(0B1010, 100f),
				new OutputDataRateType(0B1011, 200f), new OutputDataRateType(0B1100, 400f),
				new OutputDataRateType(0B1101, 800f), new OutputDataRateType(0B1110, 1600f),
				new OutputDataRateType(0B1111, 3200f) };
		private int code;
		private float outputDataRate;
		private float bandwidth;

		private OutputDataRateType(int code, float outputDataRate) {
			this.code = code;
			this.outputDataRate = outputDataRate;
			bandwidth = outputDataRate / 2;
		}

		public int getCode() {
			return code;
		}

		public float getOutputDataRate() {
			return outputDataRate;
		}

		public float getBandwidth() {
			return bandwidth;
		}
	}
}
