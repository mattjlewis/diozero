package com.diozero;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     BitSetTest.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
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


import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.tinylog.Logger;

import com.diozero.devices.McpAdc;
import com.diozero.util.BitManipulation;

@SuppressWarnings("static-method")
public class BitSetTest {
	private static final int PINS_PER_PORT = 8;
	
	@Test
	public void test() {
		AtomicInteger register = new AtomicInteger();
        byte reg = (byte) register.getAndIncrement();
        register.compareAndSet(256, 1);
        System.out.println(reg);
        reg = (byte) register.getAndIncrement();
        register.compareAndSet(256, 1);
        System.out.println(reg);

		
		int i = 1998;
		byte wait_irq = 0x30;
		
		byte n = 0x44;
		//if ~((i != 0) and ~(n & 0x01) and ~(n & waitIRq)):
		if ((i == 0)/* || ((n & 0x01) != 0)*/ || ((n & wait_irq) != 0)) {
			System.out.println("Match");
		} else {
			System.out.println("No match");
		}

		n = 0x45;
		if ((i == 0)/* || ((n & 0x01) != 0)*/ || ((n & wait_irq) != 0)) {
			System.out.println("Match");
		} else {
			System.out.println("No match");
		}

		n = 0x64;
		if ((i == 0)/* || ((n & 0x01) != 0)*/ || ((n & wait_irq) != 0)) {
			System.out.println("Match");
		} else {
			System.out.println("No match");
		}

		n = 0x00;
		if ((i == 0)/* || ((n & 0x01) != 0)*/ || ((n & wait_irq) != 0)) {
			System.out.println("Match");
		} else {
			System.out.println("No match");
		}

		n = 0x01;
		if ((i == 0)/* || ((n & 0x01) != 0)*/ || ((n & wait_irq) != 0)) {
			System.out.println("Match");
		} else {
			System.out.println("No match");
		}

		n = (byte) 0xff;
		if ((i == 0)/* || ((n & 0x01) != 0)*/ || ((n & wait_irq) != 0)) {
			System.out.println("Match");
		} else {
			System.out.println("No match");
		}
		
		int address = 0xf7;
		int value = 0x45;
		System.out.format("write(0x%02x, 0x%02x)%n", Integer.valueOf(address&0xff), Integer.valueOf(value&0xff));

		i = 0;
		n = 0;
		boolean match = (i == 0) || ((n&0x04) != 0);
		System.out.println(match);
		match = ! ((i != 0) && ((n&0x04) == 0));
		System.out.println(match);
		boolean not_match = ((i != 0) && (n & 0x04) == 0);
		System.out.println(not_match);
		
		i = 1;
		n = 0;
		match = (i == 0) || ((n&0x04) != 0);
		System.out.println(match);
		match = ! ((i != 0) && ((n&0x04) == 0));
		System.out.println(match);
		not_match = ((i != 0) && (n & 0x04) == 0);
		System.out.println(not_match);
		
		i = 0;
		n = 0x04;
		match = (i == 0) || ((n&0x04) != 0);
		System.out.println(match);
		match = ! ((i != 0) && ((n&0x04) == 0));
		System.out.println(match);
		not_match = ((i != 0) && (n & 0x04) == 0);
		System.out.println(not_match);
		
		i = 1;
		n = 0x04;
		match = (i == 0) || ((n&0x04) != 0);
		System.out.println(match);
		match = ! ((i != 0) && ((n&0x04) == 0));
		System.out.println(match);
		not_match = ((i != 0) && (n & 0x04) == 0);
		System.out.println(not_match);
		
		System.out.format("0x%02x%n", Integer.valueOf(1<<7));
		System.out.format("0x%08x%n", Integer.valueOf(1<<16));
		System.out.format("0x%02x%n", Integer.valueOf(0x0f<<4));
		System.out.format("0x%02x%n", Integer.valueOf((0x0f<<4) & 0xf0));
		
		byte b = (byte) 0xff;
		byte b2 = (byte) (b >>> 4);
		System.out.format("b=0x%02x, b2=0x%02x%n", Byte.valueOf(b), Byte.valueOf(b2));
		
		byte val = 0;
		val = BitManipulation.setBitValue(val, true, 1);
		byte expected_val = 2;
		Assertions.assertEquals(expected_val, val);
		val = BitManipulation.setBitValue(val, true, 0);
		expected_val += 1;
		Assertions.assertEquals(expected_val, val);
		val = BitManipulation.setBitValue(val, true, 4);
		expected_val += 16;
		Assertions.assertEquals(expected_val, val);
		val = BitManipulation.setBitValue(val, false, 5);
		Assertions.assertEquals(expected_val, val);
		val = BitManipulation.setBitValue(val, false, 1);
		expected_val -= 2;
		Assertions.assertEquals(expected_val, val);
		
		byte mask = BitManipulation.getBitMask(4, 5);
		Assertions.assertEquals(16+32, mask);
	}
	
	@Test
	public void testMcp23017() {
		int gpio = 7;
		byte bit = (byte)(gpio % PINS_PER_PORT);
		Assertions.assertEquals(7, bit);
		int port = gpio / PINS_PER_PORT;
		Assertions.assertEquals(0, port);
		
		gpio = 0;
		bit = (byte)(gpio % PINS_PER_PORT);
		Assertions.assertEquals(0, bit);
		port = gpio / PINS_PER_PORT;
		Assertions.assertEquals(0, port);
		
		gpio = 8;
		bit = (byte)(gpio % PINS_PER_PORT);
		Assertions.assertEquals(0, bit);
		port = gpio / PINS_PER_PORT;
		Assertions.assertEquals(1, port);
		
		gpio = 15;
		bit = (byte)(gpio % PINS_PER_PORT);
		Assertions.assertEquals(7, bit);
		port = gpio / PINS_PER_PORT;
		Assertions.assertEquals(1, port);
	}
	
	@Test
	public void testMcpAdc() {
		// MCP3304 ADC
		// x0SRRRRR RRRRRRRx
		// 00011111 11111110 0 1111 1111 1111 +4095
		// 00011111 11111100 0 1111 1111 1110 +4094
		// 00000000 00000100 0 0000 0000 0010 +2
		// 00000000 00000010 0 0000 0000 0001 +1
		// 00000000 00000000 0 0000 0000 0000 0
		// 00111111 11111110 1 1111 1111 1111 -1
		// 00111111 11111100 1 1111 1111 1110 -2
		// 00100000 00000010 1 0000 0000 0001 -4095
		// 00100000 00000000 1 0000 0000 0000 -4096
		
		byte[][] values = {
			{ (byte) 0b00011111, (byte) 0b11111110 },
			{ (byte) 0b00011111, (byte) 0b11111100 },
			{ (byte) 0b00000000, (byte) 0b00000100 },
			{ (byte) 0b00000000, (byte) 0b00000010 },
			{ (byte) 0b00000000, (byte) 0b00000000 },
			{ (byte) 0b00111111, (byte) 0b11111110 },
			{ (byte) 0b00111111, (byte) 0b11111100 },
			{ (byte) 0b00100000, (byte) 0b00000010 },
			{ (byte) 0b00100000, (byte) 0b00000000 },
		};
		int[] expected = { 4095, 4094, 2, 1, 0, -1, -2, -4095, -4096 };
		
		ByteBuffer buffer = ByteBuffer.allocateDirect(2);
		for (int i=0; i<values.length; i++) {
			buffer.put(values[i][0]);
			buffer.put(values[i][1]);
			buffer.flip();
			int v = extractValue(buffer, McpAdc.Type.MCP3304);
			Assertions.assertEquals(expected[i], v);
			buffer.clear();
		}

		int resolution = 12;
		int m = (1 << resolution-8) - 1;
		Assertions.assertEquals(0b00001111, m);
		
		resolution = 10;
		m = (1 << resolution-8) - 1;
		Assertions.assertEquals(0b00000011, m);
		
		// Rx   x0RRRRRR RRRRxxxx for the 300x (10 bit)
		// Rx   x0RRRRRR RRRRRRxx for the 320x (12 bit)
		// Rx   x0SRRRRR RRRRRRRx for the 330x (13 bit signed)
		buffer.put((byte)0x01);
		buffer.put((byte)0xfe);
		buffer.flip();
		int v = (buffer.getShort() << 2) >> 3;
		Assertions.assertEquals(255, v);
		
		buffer.clear();
		buffer.put((byte) 0b00011111);
		buffer.put((byte) 0xfe);
		buffer.flip();
		v = ((short)(buffer.getShort() << 2)) >> 3;
		Assertions.assertEquals(4095, v);
		
		buffer.clear();
		buffer.put((byte)0b00111111);
		buffer.put((byte)0xfe);
		buffer.flip();
		v = ((short)(buffer.getShort() << 2)) >> 3;
		Assertions.assertEquals(-1, v);
		
		buffer.clear();
		buffer.put((byte)0b00100000);
		buffer.put((byte)0x00);
		buffer.flip();
		v = ((short)(buffer.getShort() << 2)) >> 3;
		Assertions.assertEquals(-4096, v);
		
		Assertions.assertTrue("MCP3301".substring(0, 5).equals("MCP33"));
		Assertions.assertTrue("MCP3302".substring(0, 5).equals("MCP33"));
		Assertions.assertTrue("MCP3304".substring(0, 5).equals("MCP33"));
		
		McpAdc.Type type = McpAdc.Type.valueOf("MCP3008");
		Assertions.assertEquals(McpAdc.Type.MCP3008, type);
		
		// Unsigned
		type = McpAdc.Type.MCP3208;
		buffer.rewind();
		//buffer.put((byte)0b00111111);
		//buffer.put((byte)0xfe);
		buffer.put((byte)0xff);
		buffer.put((byte)0xff);
		buffer.flip();
		// Doesn't work as >>> is promoted to integer
		short s = (short)((short)(buffer.getShort() << 2) >>> (short)(14+2-type.getResolution()));
		System.out.format("Resolution=%d, s=%d, expected %d%n", Integer.valueOf(14+2-type.getResolution()),
				Integer.valueOf(s), Integer.valueOf((int)Math.pow(2, type.getResolution())-1));
		
		buffer.rewind();
		v = (buffer.getShort() & 0x3fff) >> (14 - type.getResolution());
		System.out.format("Resolution=%d, v=%d%n", Integer.valueOf(14+2-type.getResolution()), Integer.valueOf(v));
		
		// Signed
		type = McpAdc.Type.MCP3304;
		buffer.rewind();
		v = ((short)(buffer.getShort() << 2)) >> (14+2-type.getResolution());
		System.out.format("Resolution=%d, v=%d%n", Integer.valueOf(14+2-type.getResolution()), Integer.valueOf(v));
		
		// Unsigned
		type = McpAdc.Type.MCP3208;
		buffer.rewind();
		if (type.isSigned()) {
			v = ((short)(buffer.getShort() << 2)) >> (14+2-type.getResolution());
		} else {
			v = (buffer.getShort() & 0x3fff) >> (14 - type.getResolution());
		}
		System.out.format("Resolution=%d, v=%d%n", Integer.valueOf(14+2-type.getResolution()), Integer.valueOf(v));
		
		// Signed
		type = McpAdc.Type.MCP3304;
		buffer.rewind();
		if (type.isSigned()) {
			v = ((short)(buffer.getShort() << 2)) >> (14+2-type.getResolution());
		} else {
			v = (buffer.getShort() & 0x3fff) >> (14 - type.getResolution());
		}
		System.out.format("Right shift=%d, v=%d%n", Integer.valueOf(14+2-type.getResolution()), Integer.valueOf(v));
	}
	
	private static int extractValue(ByteBuffer in, McpAdc.Type type) {
		/*
		 * Rx x0RRRRRR RRRRxxxx for the 30xx (10-bit unsigned)
		 * Rx x0RRRRRR RRRRRRxx for the 32xx (12-bit unsigned)
		 * Rx x0SRRRRR RRRRRRRx for the 33xx (13-bit signed)
		 */
		if (type.isSigned()) {
			short s = in.getShort();
			Logger.debug("Signed reading, s={}", Short.valueOf(s));
			// Relies on the >> operator to preserve the sign bit
			return ((short) (s << 2)) >> (14+2-type.getResolution());
		}
		
		// Note can't use >>> to propagate MSB 0s as it doesn't work with short, only integer
		return (in.getShort() & 0x3fff) >> (14 - type.getResolution());
	}
}
