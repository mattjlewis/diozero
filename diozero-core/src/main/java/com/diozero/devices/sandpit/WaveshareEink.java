package com.diozero.devices.sandpit;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     WaveshareEink.java
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

import com.diozero.api.DeviceInterface;
import com.diozero.api.DigitalInputDevice;
import com.diozero.api.DigitalOutputDevice;
import com.diozero.api.RuntimeIOException;
import com.diozero.api.SpiDevice;
import com.diozero.util.SleepUtil;

/**
 * 7.5" Datasheet:
 * https://www.waveshare.com/wiki/File:7.5inch-e-paper-specification.pdf 7.5" V2
 * Datasheet:
 * https://www.waveshare.com/w/upload/6/60/7.5inch_e-Paper_V2_Specification.pdf
 * https://www.waveshare.com/wiki/7.5inch_e-Paper_HAT
 *
 * Code: https://github.com/waveshare/e-Paper
 *
 * OTP: One Time Programmable memory, not programmed into registers by the
 * driver SW for this controller. LUT: Waveform Look Up Table
 */
public abstract class WaveshareEink implements DeviceInterface {
	public enum Model {
		// https://github.com/waveshare/e-Paper/blob/master/RaspberryPi_JetsonNano/python/lib/waveshare_epd/epd7in5_V2.py
		_7x5V2(800, 480);

		private int width;
		private int height;

		Model(int width, int height) {
			this.width = width;
			this.height = height;
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}
	}

	protected Model model;
	protected SpiDevice device;
	protected DigitalOutputDevice reset;
	protected DigitalOutputDevice dataOrCommand;
	protected DigitalInputDevice busy;
	protected int prodRev;
	protected int lutRev;
	protected int chipRev;

	public WaveshareEink(Model model, int controller, int chipSelect, DigitalOutputDevice reset,
			DigitalOutputDevice dataOrCommand, DigitalInputDevice busy) {
		this.model = model;

		device = SpiDevice.builder(chipSelect).setController(controller).setFrequency(4_000_000).build();

		this.reset = reset;
		this.dataOrCommand = dataOrCommand;
		this.busy = busy;

		init();
	}

	protected abstract void init();

	protected void reset() {
		reset.on();
		SleepUtil.sleepMillis(200);
		reset.off();
		SleepUtil.sleepMillis(2);
		reset.on();
		SleepUtil.sleepMillis(200);
	}

	protected abstract void sleep();

	protected abstract void clear();

	protected abstract void readBusy();

	protected void commandAndData(byte command, byte... data) {
		command(command);
		// Note that data.length == 0 for C&D(byte); rather than null
		if (data != null && data.length > 0) {
			data(data);
		}
	}

	protected void command(byte command) {
		dataOrCommand.off();
		device.write(command);
	}

	protected void data(byte... data) {
		dataOrCommand.on();
		device.write(data);
	}

	@Override
	public void close() throws RuntimeIOException {
		sleep();
		reset.off();
		dataOrCommand.off();

		device.close();
		reset.close();
		dataOrCommand.close();
	}

	protected abstract void getRevision();

	public int getProductRevision() {
		return prodRev;
	}

	public int getLookupTableRevision() {
		return lutRev;
	}

	public int getChipRevision() {
		return chipRev;
	}
}
