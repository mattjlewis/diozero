package com.diozero;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 diozero
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
