package com.diozero;

import java.nio.ByteBuffer;

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
	}
	
	@Test
	public void testMcp23017() {
		int pinNumber = 7;
		byte bit = (byte)(pinNumber % PINS_PER_PORT);
		Assert.assertEquals(7, bit);
		int port = pinNumber / PINS_PER_PORT;
		Assert.assertEquals(0, port);
		
		pinNumber = 0;
		bit = (byte)(pinNumber % PINS_PER_PORT);
		Assert.assertEquals(0, bit);
		port = pinNumber / PINS_PER_PORT;
		Assert.assertEquals(0, port);
		
		pinNumber = 8;
		bit = (byte)(pinNumber % PINS_PER_PORT);
		Assert.assertEquals(0, bit);
		port = pinNumber / PINS_PER_PORT;
		Assert.assertEquals(1, port);
		
		pinNumber = 15;
		bit = (byte)(pinNumber % PINS_PER_PORT);
		Assert.assertEquals(7, bit);
		port = pinNumber / PINS_PER_PORT;
		Assert.assertEquals(1, port);
	}
	
	@Test
	public void testMcpAdc() {
		int resolution = 12;
		int m = (1 << resolution-8) - 1;
		Assert.assertEquals(0b00001111, m);
		
		resolution = 10;
		m = (1 << resolution-8) - 1;
		Assert.assertEquals(0b00000011, m);
		
		// Rx   x0RRRRRR RRRRxxxx for the 300x (10 bit)
		// Rx   x0RRRRRR RRRRRRxx for the 320x (12 bit)
		// Rx   x0SRRRRR RRRRRRRx for the 330x (13 bit signed)
		ByteBuffer buffer = ByteBuffer.allocateDirect(2);
		buffer.put((byte)0x01);
		buffer.put((byte)0xfe);
		buffer.flip();
		int v = (buffer.getShort() << 2) >> 3;
		Assert.assertEquals(255, v);
		
		buffer.rewind();
		buffer.put((byte)0b11111);
		buffer.put((byte)0xfe);
		buffer.flip();
		v = ((short)(buffer.getShort() << 2)) >> 3;
		Assert.assertEquals(4095, v);
		
		buffer.rewind();
		buffer.put((byte)0b111111);
		buffer.put((byte)0xfe);
		buffer.flip();
		v = ((short)(buffer.getShort() << 2)) >> 3;
		Assert.assertEquals(-1, v);
		
		buffer.rewind();
		buffer.put((byte)0b100000);
		buffer.put((byte)0x0);
		buffer.flip();
		v = ((short)(buffer.getShort() << 2)) >> 3;
		Assert.assertEquals(-4096, v);
		
		Assert.assertTrue("MCP3301".substring(0, 5).equals("MCP33"));
		Assert.assertTrue("MCP3302".substring(0, 5).equals("MCP33"));
		Assert.assertTrue("MCP3304".substring(0, 5).equals("MCP33"));
		
		McpAdc.Type type = McpAdc.Type.valueOf("MCP3008");
		Assert.assertEquals(McpAdc.Type.MCP3008, type);
		
		// Unsigned
		type = McpAdc.Type.MCP3208;
		buffer.rewind();
		//buffer.put((byte)0b111111);
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
		System.out.format("Resolution=%d, v=%d%n", Integer.valueOf(14+2-type.getResolution()), Integer.valueOf(v));
	}
}
