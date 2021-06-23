package com.diozero.internal.provider.builtin;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     SysFsPwmOutputDevice.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
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
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.tinylog.Logger;

import com.diozero.api.DeviceMode;
import com.diozero.api.PinInfo;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.InternalPwmOutputDeviceInterface;
import com.diozero.internal.spi.MmapGpioInterface;
import com.diozero.internal.spi.NativeDeviceFactoryInterface;
import com.diozero.util.SleepUtil;

public class SysFsPwmOutputDevice extends AbstractDevice implements InternalPwmOutputDeviceInterface {
	private int gpio;
	private int pwmChip;
	private int pwmNum;
	private Path pwmRoot;
	private RandomAccessFile dutyFile;
	private int periodNs;

	public SysFsPwmOutputDevice(String key, DeviceFactoryInterface deviceFactory, PinInfo pinInfo, int frequencyHz,
			float initialValue, MmapGpioInterface mmapGpio) {
		super(key, deviceFactory);

		gpio = pinInfo.getDeviceNumber();
		pwmChip = deviceFactory.getBoardPinInfo().getPwmChipNumberOverride(pinInfo);
		pwmNum = pinInfo.getPwmNum();

		Path pwm_chip_root = Paths.get("/sys/class/pwm/pwmchip" + pwmChip);
		File pwm_chip_root_file = pwm_chip_root.toFile();
		if (!pwm_chip_root_file.exists() || !pwm_chip_root_file.canWrite()) {
			throw new RuntimeIOException(
					"PWM chip sysfs folder '" + pwm_chip_root.toAbsolutePath() + "' doesn't exist or is unwritable");
		}

		pwmRoot = pwm_chip_root.resolve("pwm" + pwmNum);

		try {
			if (!pwmRoot.toFile().exists()) {
				File export_file = pwm_chip_root.resolve("export").toFile();
				try (FileWriter writer = new FileWriter(export_file)) {
					writer.write(String.valueOf(pwmNum));
				}
			}
			// Need to wait for the sysfs file to be setup correctly
			SleepUtil.sleepMillis(50);

			dutyFile = new RandomAccessFile(pwmRoot.resolve("duty_cycle").toFile(), "rw");
		} catch (IOException e) {
			throw new RuntimeIOException("Error opening PWM #" + pwmNum, e);
		}

		if (mmapGpio != null && mmapGpio.getMode(gpio) != DeviceMode.PWM_OUTPUT) {
			mmapGpio.setMode(gpio, DeviceMode.PWM_OUTPUT);
		}

		// The order is important, cannot set the polarity or value if the period is 0
		setPwmFrequency(frequencyHz);
		setValue(initialValue);
		writePolarity(pwmRoot, Polarity.NORMAL);
		writeEnabled(pwmRoot, true);
	}

	@Override
	public int getGpio() {
		return gpio;
	}

	@Override
	public int getPwmNum() {
		return pwmNum;
	}

	@Override
	public float getValue() {
		return getDutyNs() / (float) periodNs;
	}

	@Override
	public void setValue(float value) {
		// TODO Bounds checking
		setDutyNs((int) (value * periodNs));
	}

	@Override
	public int getPwmFrequency() throws RuntimeIOException {
		return 1_000_000_000 / periodNs;
	}

	@Override
	public void setPwmFrequency(int frequencyHz) throws RuntimeIOException {
		setPeriodNs(1_000_000_000 / frequencyHz);
	}

	@Override
	protected void closeDevice() {
		setDutyNs(0);
		try {
			writeEnabled(pwmRoot, false);
		} catch (Exception e) {
			// Ignore
		}
		try {
			dutyFile.close();
		} catch (Exception e) {
			// Ignore
		}
		Path pwm_chip_root = Paths.get("/sys/class/pwm/pwmchip" + pwmChip);
		try (FileWriter writer = new FileWriter(pwm_chip_root.resolve("unexport").toFile())) {
			writer.write(String.valueOf(pwmNum));
		} catch (Exception e) {
			Logger.warn(e, "Error closing pwm #" + pwmNum);
		}
	}

	private void setPeriodNs(int periodNs) {
		Logger.debug("setPeriodNs(" + periodNs + ")");

		// The duty value is represented as duty nanoseconds hence needs to be adjusted
		// if the frequency changes.
		// Get the current duty value
		int current_duty_ns = getDutyNs();
		if (current_duty_ns != 0) {
			// Temporarily set to 0 while we change the PWM period
			setDutyNs(0);
		}

		// Calculate the ratio between old and new frequency
		float ratio = (periodNs - this.periodNs) / (float) this.periodNs;

		// Write the new period
		writePeriodNs(pwmRoot, periodNs);
		this.periodNs = periodNs;

		// Recalculate the equivalent duty value
		int new_duty_ns = Math.round(current_duty_ns + current_duty_ns * ratio);

		// Update with the equivalent duty value
		setDutyNs(new_duty_ns);
	}

	private int getDutyNs() throws RuntimeIOException {
		try {
			dutyFile.seek(0);
			return Integer.parseInt(dutyFile.readLine());
		} catch (IOException e) {
			close();
			throw new RuntimeIOException("Error getting PWM output value");
		}
	}

	private void setDutyNs(int dutyNs) {
		Logger.debug("setDutyNs(" + dutyNs + ")");

		try {
			// dutyFile.seek(0);
			dutyFile.writeBytes(Integer.toString(dutyNs));
			// dutyFile.flush();
		} catch (IOException e) {
			close();
			throw new RuntimeIOException("Error setting duty for PWM #" + pwmNum, e);
		}
	}

	private static void writePeriodNs(Path pwmRoot, int periodNs) throws RuntimeIOException {
		try (FileWriter writer = new FileWriter(pwmRoot.resolve("period").toFile())) {
			writer.write(Integer.toString(periodNs));
		} catch (IOException e) {
			throw new RuntimeIOException("Error writing to period file: " + e, e);
		}
	}

	private static void writePolarity(Path pwmRoot, Polarity polarity) throws RuntimeIOException {
		Logger.debug("writePolarity(" + polarity + ")");

		try (FileWriter writer = new FileWriter(pwmRoot.resolve("polarity").toFile())) {
			writer.write(polarity.getValue());
			writer.flush();
		} catch (IOException e) {
			throw new RuntimeIOException("Error writing to polarity file: " + e, e);
		}
	}

	private static void writeEnabled(Path pwmRoot, boolean enabled) throws RuntimeIOException {
		Logger.debug("writeEnabled(" + enabled + ")");

		try (FileWriter writer = new FileWriter(pwmRoot.resolve("enable").toFile())) {
			writer.write(enabled ? "1" : "0");
		} catch (IOException e) {
			throw new RuntimeIOException("Error writing to enabled file: " + e, e);
		}
	}

	public enum Polarity {
		NORMAL("normal"), INVERSED("inversed");

		private String value;

		Polarity(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
	}

	public static boolean isSupported(NativeDeviceFactoryInterface deviceFactory, PinInfo pinInfo) {
		int pwm_chip = deviceFactory.getBoardInfo().getPwmChipNumberOverride(pinInfo);

		if (pwm_chip == PinInfo.NOT_DEFINED || pinInfo.getPwmNum() == PinInfo.NOT_DEFINED) {
			return false;
		}

		// Check the directory exists and is readable
		File f = Paths.get("/sys/class/pwm/pwmchip" + pwm_chip).toFile();
		return f.exists() && f.isDirectory() && f.canRead();
	}
}
