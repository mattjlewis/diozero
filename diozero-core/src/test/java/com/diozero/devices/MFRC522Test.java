package com.diozero.devices;

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
