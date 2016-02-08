package com.diozero;

import java.io.IOException;
import java.nio.ByteBuffer;

import org.junit.Assert;
import org.junit.Test;

import com.diozero.api.*;
import com.diozero.internal.provider.test.TestDeviceFactory;
import com.diozero.internal.spi.I2CDeviceInterface;
import com.diozero.internal.spi.SpiDeviceInterface;

@SuppressWarnings("static-method")
public class CleanupTest {
	@Test
	public void test() {
		TestDeviceFactory tdf = (TestDeviceFactory)DeviceFactoryHelper.getNativeDeviceFactory();
		DeviceStates ds = tdf.getDeviceStates();
		Assert.assertTrue(ds.size() == 0);
		try (I2CDeviceInterface device = tdf.provisionI2CDevice(0, 0, 0, 0)) {
			device.read(0, I2CConstants.ADDR_SIZE_7, ByteBuffer.allocateDirect(5));
			Assert.assertTrue(ds.size() == 1);
			device.close();
			Assert.assertTrue(ds.size() == 0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Check the log for the above - make sure there is a warning about closing already closed device
		
		Assert.assertTrue(ds.size() == 0);
		try (SpiDeviceInterface device = tdf.provisionSpiDevice(0, 0, 0, SpiClockMode.MODE_0)) {
			ByteBuffer out = ByteBuffer.allocate(3);
			out.put((byte) 0x01);
			out.put((byte) ((1 | 0x08) << 4));
			out.put((byte) 0);
			out.flip();
			device.writeAndRead(out);
			Assert.assertTrue(ds.size() == 1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Assert.assertTrue(ds.size() == 0);
		tdf.closeAll();
	}
}
