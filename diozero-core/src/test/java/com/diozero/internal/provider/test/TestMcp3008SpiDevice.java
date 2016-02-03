package com.diozero.internal.provider.test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.api.SpiClockMode;
import com.diozero.internal.spi.DeviceFactoryInterface;

public class TestMcp3008SpiDevice extends TestSpiDevice {
	private static final Logger logger = LogManager.getLogger(TestMcp3008SpiDevice.class);
	
	private static final int RANGE = (int)Math.pow(2, 10);
	private static final Random random = new Random();

	public TestMcp3008SpiDevice(String key, DeviceFactoryInterface deviceFactory, int controller, int chipSelect, int frequency,
			SpiClockMode spiClockMode) {
		super(key, deviceFactory, controller, chipSelect, frequency, spiClockMode);
	}

	@Override
	public ByteBuffer writeAndRead(ByteBuffer out) throws IOException {
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
		assert (b == 0x01);
		b = out.get();
		int pin = (b >> 4) & 0x07;
		logger.debug("Received read request for pin " + pin);
		b = out.get();
		assert (b == 0);
		
		int temp = random.nextInt(RANGE);
		ByteBuffer dst = ByteBuffer.allocateDirect(3);
		dst.put((byte)0);
		dst.put((byte)((temp >> 8) & 0x03));
		dst.put((byte)(temp & 0xff));
		dst.flip();
		
		return dst;
	}
}
