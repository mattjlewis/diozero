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
import com.diozero.internal.spi.MmapGpioInterface;
import com.diozero.internal.spi.NativeDeviceFactoryInterface;
import com.diozero.internal.spi.PwmOutputDeviceInterface;
import com.diozero.util.SleepUtil;

public class SysFsPwmOutputDevice extends AbstractDevice implements PwmOutputDeviceInterface {
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
		updatePolarity(Polarity.NORMAL);
		updateEnabled(true);
	}

	@Override
	protected void closeDevice() {
		try {
			updateEnabled(false);
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

	@Override
	public int getGpio() {
		return gpio;
	}

	@Override
	public int getPwmNum() {
		return pwmNum;
	}

	private int getValueRaw() throws RuntimeIOException {
		try {
			dutyFile.seek(0);
			return Integer.parseInt(dutyFile.readLine());
		} catch (IOException e) {
			close();
			throw new RuntimeIOException("Error getting PWM output value");
		}
	}

	@Override
	public float getValue() throws RuntimeIOException {
		return getValueRaw() / (float) periodNs;
	}

	@Override
	public void setValue(float value) throws RuntimeIOException {
		if (value < 0 || value > 1) {
			throw new IllegalArgumentException("Invalid value (" + value + "), must be 0..1");
		}

		try {
			// dutyFile.seek(0);
			dutyFile.writeBytes(Integer.toString(Math.round(value * periodNs)));
			// dutyFile.flush();
		} catch (IOException e) {
			close();
			throw new RuntimeIOException("Error setting duty for PWM #" + pwmNum, e);
		}
	}

	@Override
	public int getPwmFrequency() {
		return 1_000_000_000 / periodNs;
	}

	@Override
	public void setPwmFrequency(int frequencyHz) throws RuntimeIOException {
		// The value is represented as duty nanoseconds hence needs to be adjusted if
		// the frequency changes.
		// Get the current raw value
		int current_raw_value = getValueRaw();
		if (current_raw_value != 0) {
			// Temporarily set to 0 while we change the PWM period
			setValue(0);
		}
		float current_value = current_raw_value / (float) periodNs;

		// Recalculate and update the period
		periodNs = 1_000_000_000 / frequencyHz;
		updatePeriod(periodNs);

		// Restore the old value
		setValue(current_value);
	}

	private void updateEnabled(boolean enabled) throws RuntimeIOException {
		Logger.debug("updateEnabled(" + enabled + ")");
		try (FileWriter writer = new FileWriter(pwmRoot.resolve("enable").toFile())) {
			writer.write(enabled ? "1" : "0");
		} catch (IOException e) {
			close();
			throw new RuntimeIOException("Error writing to enabled file: " + e, e);
		}
	}

	private void updatePeriod(int periodNs) throws RuntimeIOException {
		Logger.debug("updatePeriod(" + periodNs + ")");
		try (FileWriter writer = new FileWriter(pwmRoot.resolve("period").toFile())) {
			writer.write(Integer.toString(periodNs));
		} catch (IOException e) {
			close();
			throw new RuntimeIOException("Error writing to period file: " + e, e);
		}
	}

	private void updatePolarity(Polarity polarity) throws RuntimeIOException {
		Logger.debug("updatePolarity(" + polarity + ")");
		try (FileWriter writer = new FileWriter(pwmRoot.resolve("polarity").toFile())) {
			writer.write(polarity.getValue());
			writer.flush();
		} catch (IOException e) {
			close();
			throw new RuntimeIOException("Error writing to polarity file: " + e, e);
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
