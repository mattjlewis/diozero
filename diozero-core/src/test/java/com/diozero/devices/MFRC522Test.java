package com.diozero.devices;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     MFRC522Test.java
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.diozero.devices.MFRC522.AntennaGain;

@SuppressWarnings("static-method")
public class MFRC522Test {
	@Test
	public void testGetGain1() {
		AntennaGain gain = AntennaGain.DB_23A;
		byte value = (byte) (gain.getValue() | 0b10001111);
		AntennaGain got_gain = AntennaGain.forValue(value);
		Assertions.assertEquals(gain, got_gain);

		gain = AntennaGain.DB_18A;
		value = (byte) (gain.getValue() | 0b10001111);
		got_gain = AntennaGain.forValue(value);
		Assertions.assertEquals(gain, got_gain);

		gain = AntennaGain.DB_48;
		value = (byte) (gain.getValue() | 0b10001111);
		got_gain = AntennaGain.forValue(value);
		Assertions.assertEquals(gain, got_gain);
	}

	@Test
	public void testGetGain2() {
		AntennaGain gain = AntennaGain.DB_23A;
		AntennaGain got_gain = AntennaGain.forValue(gain.getValue());
		Assertions.assertEquals(gain, got_gain);

		gain = AntennaGain.DB_18A;
		got_gain = AntennaGain.forValue(gain.getValue());
		Assertions.assertEquals(gain, got_gain);

		gain = AntennaGain.DB_48;
		got_gain = AntennaGain.forValue(gain.getValue());
		Assertions.assertEquals(gain, got_gain);
	}

	@Test
	public void testSetGain() {
		AntennaGain new_gain = AntennaGain.DB_48;

		// "Read" the current gain
		AntennaGain current_gain = AntennaGain.DB_38;
		byte current_mask = (byte) 0b10001111;
		// Add some noise to the reserved bytes
		byte current_reg_val = (byte) (current_mask | current_gain.getValue());

		// Mask out the current gain byte value ...
		byte new_reg_val = (byte) (current_reg_val & ~(0x07 << 4));
		// ... and update with the new gain value
		new_reg_val |= new_gain.getValue();

		Assertions.assertEquals((byte) (current_mask | new_gain.getValue()), new_reg_val);
		Assertions.assertEquals(new_gain, AntennaGain.forValue(new_reg_val));
	}
}
