package com.diozero.internal.provider.firmata.ds18b20;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Firmata
 * Filename:     DS18B20Resolution.java
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

public enum DS18B20Resolution {
	// Actually 93.75ms, 187.5ms, 375ms, 750ms
	_9BIT(0b00, 94), _10BIT(0b01, 188), _11BIT(0b10, 375), _12BIT(0b11, 750);

	private byte value;
	private int temperatureConversionTime;

	private DS18B20Resolution(int value, int temperatureConversionTime) {
		this.value = (byte) value;
		this.temperatureConversionTime = temperatureConversionTime;
	}

	public byte value() {
		return value;
	}

	public int temperatureConversionTime() {
		return temperatureConversionTime;
	}

	public byte toConfigReg() {
		return (byte) (value << 5 | 0x1f);
	}

	public static DS18B20Resolution fromConfigReg(byte configRegValue) {
		switch ((configRegValue >> 5) & 0b11) {
		case 0b00:
			return _9BIT;
		case 0b01:
			return _10BIT;
		case 0b10:
			return _11BIT;
		case 0b11:
		default:
			return _12BIT;
		}
	}
}
