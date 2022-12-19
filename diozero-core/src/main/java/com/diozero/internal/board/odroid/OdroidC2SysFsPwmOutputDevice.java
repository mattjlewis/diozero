package com.diozero.internal.board.odroid;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     OdroidC2SysFsPwmOutputDevice.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2022 diozero
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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.tinylog.Logger;

import com.diozero.api.PinInfo;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.InternalPwmOutputDeviceInterface;

/**
 * <p>
 * <a href="http://odroid.com/dokuwiki/doku.php?id=en:c2_hardware_pwm">Setting
 * up</a>: 1 PWM Channel (GPIO 234; Pin 33):
 * </p>
 *
 * <pre>
 * {@code
 *sudo modprobe pwm-meson
 *sudo modprobe pwm-ctrl
 *}
 * </pre>
 * <p>
 * 2 PWM Channels (GPIO 234 &amp; 235; Pins 33 / 19):
 * </p>
 *
 * <pre>
 * {@code
 *sudo modprobe pwm-meson npwm=2
 *sudo modprobe pwm-ctrl
 *}
 * </pre>
 */
public class OdroidC2SysFsPwmOutputDevice extends AbstractDevice implements InternalPwmOutputDeviceInterface {
	private static Path PWM_ROOT = Paths.get("/sys/devices/platform/pwm-ctrl");

	private int range;
	private int gpio;
	private int pwmNum;
	private RandomAccessFile dutyFile;

	public OdroidC2SysFsPwmOutputDevice(String key, DeviceFactoryInterface deviceFactory, PinInfo pinInfo,
			int frequencyHz, float initialValue) {
		super(key, deviceFactory);

		this.range = 1023;
		this.gpio = pinInfo.getDeviceNumber();
		this.pwmNum = pinInfo.getPwmNum();

		try {
			dutyFile = new RandomAccessFile(PWM_ROOT.resolve("duty" + pwmNum).toFile(), "rw");
		} catch (IOException e) {
			throw new RuntimeIOException("Error opening duty file for PWM " + pwmNum, e);
		}

		setEnabled(pwmNum, true);
		setPwmFrequency(frequencyHz);

		setValue(initialValue);
	}

	@Override
	protected void closeDevice() {
		Logger.trace("closeDevice() {}", getKey());
		try {
			dutyFile.close();
		} catch (IOException e) {
			// Ignore
		}
		setEnabled(pwmNum, false);
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
	public float getValue() throws RuntimeIOException {
		try {
			dutyFile.seek(0);
			int raw_value = Integer.parseInt(dutyFile.readLine());

			return raw_value / (float) range;
		} catch (IOException e) {
			closeDevice();
			throw new RuntimeIOException("Error setting duty for PWM #" + pwmNum, e);
		}
	}

	@Override
	public void setValue(float value) throws RuntimeIOException {
		if (value < 0 || value > 1) {
			throw new IllegalArgumentException("Invalid value, must be 0..1");
		}

		try {
			dutyFile.seek(0);
			dutyFile.writeBytes(Integer.toString((int) Math.floor(value * range)));
		} catch (IOException e) {
			closeDevice();
			throw new RuntimeIOException("Error setting duty for PWM #" + pwmNum, e);
		}
	}

	@Override
	public int getPwmFrequency() {
		// freq value is in milli-Hz
		return readFreq(pwmNum) / 1_000;
	}

	@Override
	public void setPwmFrequency(int frequencyHz) {
		// TODO Preserve the relative value of the duty

		// freq value is in milli-Hz
		writeFreq(pwmNum, frequencyHz * 1_000);
	}

	private static void setEnabled(int pwmNum, boolean enabled) {
		File f = PWM_ROOT.resolve("enable" + pwmNum).toFile();
		try (FileWriter writer = new FileWriter(f)) {
			writer.write(enabled ? "1" : "0");
		} catch (IOException e) {
			throw new RuntimeIOException("Error enabling PWM on #" + pwmNum, e);
		}
	}

	static int readFreq(int pwmNum) {
		File f = PWM_ROOT.resolve("freq" + pwmNum).toFile();
		try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
			return Integer.parseInt(reader.readLine());
		} catch (IOException | NumberFormatException e) {
			throw new RuntimeIOException("Error getting frequency for PWM #" + pwmNum, e);
		}
	}

	static void writeFreq(int pwmNum, int value) throws RuntimeIOException {
		File f = PWM_ROOT.resolve("freq" + pwmNum).toFile();
		try (FileWriter writer = new FileWriter(f)) {
			writer.write(Integer.toString(value));
		} catch (IOException e) {
			throw new RuntimeIOException("Error setting frequency (" + value + ") for PWM #" + pwmNum, e);
		}
	}
}
