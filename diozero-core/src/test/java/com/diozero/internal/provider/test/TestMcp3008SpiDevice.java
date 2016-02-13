package com.diozero.internal.provider.test;

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

import java.nio.ByteBuffer;
import java.util.Random;

import org.junit.Assert;
import org.pmw.tinylog.Logger;

import com.diozero.api.SpiClockMode;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.util.RuntimeIOException;

public class TestMcp3008SpiDevice extends TestSpiDevice {
	private static final int RANGE = (int)Math.pow(2, 10);
	private static final Random random = new Random();

	public TestMcp3008SpiDevice(String key, DeviceFactoryInterface deviceFactory, int controller, int chipSelect, int frequency,
			SpiClockMode spiClockMode) {
		super(key, deviceFactory, controller, chipSelect, frequency, spiClockMode);
	}

	@Override
	public ByteBuffer writeAndRead(ByteBuffer out) throws RuntimeIOException {
		/*
		ByteBuffer out = ByteBuffer.allocate(3);
		out.put((byte) 0x01);
		out.put((byte) ((adcPin | 0x08) << 4));
		out.put((byte) 0);
		out.flip();
		ByteBuffer in;
		try {
			in = writeAndRead(out);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		int high = 0x03 & in.get(1);
		int low = 0xff & in.get(2);

		return (high << 8) | low;
		*/
		byte b = out.get();
		Assert.assertEquals(0x01, b);
		b = out.get();
		int pin = (b >> 4) & 0x07;
		Logger.debug("Received read request for pin {}", Integer.valueOf(pin));
		b = out.get();
		Assert.assertEquals(0, b);
		
		int temp = random.nextInt(RANGE);
		ByteBuffer dst = ByteBuffer.allocateDirect(3);
		dst.put((byte)0);
		dst.put((byte)((temp >> 8) & 0x03));
		dst.put((byte)(temp & 0xff));
		dst.flip();
		
		return dst;
	}
}
