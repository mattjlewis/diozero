package com.diozero;

import org.junit.Assert;
import org.junit.Test;

import com.diozero.util.BitManipulation;

@SuppressWarnings("static-method")
public class BitSetTest {
	private static final int PINS_PER_PORT = 8;
	
	@Test
	public void test() {
		byte val = 0;
		val = BitManipulation.setBitValue(val, true, 1);
		byte expected_val = 2;
		Assert.assertEquals(val, expected_val);
		val = BitManipulation.setBitValue(val, true, 0);
		expected_val += 1;
		Assert.assertEquals(val, expected_val);
		val = BitManipulation.setBitValue(val, true, 4);
		expected_val += 16;
		Assert.assertEquals(val, expected_val);
		val = BitManipulation.setBitValue(val, false, 5);
		Assert.assertEquals(val, expected_val);
		val = BitManipulation.setBitValue(val, false, 1);
		expected_val -= 2;
		Assert.assertEquals(val, expected_val);
		
		byte mask = BitManipulation.getBitMask(4, 5);
		Assert.assertEquals(mask, 16+32);
		
		int pinNumber = 7;
		byte bit = (byte)(pinNumber % PINS_PER_PORT);
		Assert.assertEquals(bit, 7);
		int port = pinNumber / PINS_PER_PORT;
		Assert.assertEquals(port, 0);
		
		pinNumber = 0;
		bit = (byte)(pinNumber % PINS_PER_PORT);
		Assert.assertEquals(bit, 0);
		port = pinNumber / PINS_PER_PORT;
		Assert.assertEquals(port, 0);
		
		pinNumber = 8;
		bit = (byte)(pinNumber % PINS_PER_PORT);
		Assert.assertEquals(bit, 0);
		port = pinNumber / PINS_PER_PORT;
		Assert.assertEquals(port, 1);
		
		pinNumber = 15;
		bit = (byte)(pinNumber % PINS_PER_PORT);
		Assert.assertEquals(bit, 7);
		port = pinNumber / PINS_PER_PORT;
		Assert.assertEquals(port, 1);
	}
}
