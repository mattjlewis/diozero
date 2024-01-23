package com.diozero.internal.provider.firmata.ds18b20;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Firmata
 * Filename:     DS18B20Config.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2024 diozero
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

public class DS18B20Config {
	private byte alarmHighTrigger;
	private byte alarmLowTrigger;
	private DS18B20Resolution resolution;

	public DS18B20Config(byte alarmHighTrigger, byte alarmLowTrigger, DS18B20Resolution resolution) {
		this.resolution = resolution;
		this.alarmHighTrigger = alarmHighTrigger;
		this.alarmLowTrigger = alarmLowTrigger;
	}

	public byte getAlarmHighTrigger() {
		return alarmHighTrigger;
	}

	void setAlarmHighTrigger(byte alarmHighTrigger) {
		this.alarmHighTrigger = alarmHighTrigger;
	}

	public byte getAlarmLowTrigger() {
		return alarmLowTrigger;
	}

	void setAlarmLowTrigger(byte alarmLowTrigger) {
		this.alarmLowTrigger = alarmLowTrigger;
	}

	public DS18B20Resolution getResolution() {
		return resolution;
	}

	void setResolution(DS18B20Resolution resolution) {
		this.resolution = resolution;
	}
}
