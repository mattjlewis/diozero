package com.diozero.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     I2CDeviceTest.java
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

import com.diozero.internal.spi.I2CDeviceFactoryInterface;
import com.diozero.internal.spi.InternalI2CDeviceInterface;

@SuppressWarnings("static-method")
class I2CDeviceTest {
	@Test
	void writeBytes() {
		I2CConstants.AddressSize addressSize = I2CConstants.AddressSize.valueOf(0);
		ByteOrder defaultByteOrder = I2CDevice.Builder.DEFAULT_BYTE_ORDER;
		int controller = 0;
		int address = 0;

		ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
		InternalI2CDeviceInterface delegate = mock(InternalI2CDeviceInterface.class);
		I2CDeviceFactoryInterface factory = mock(I2CDeviceFactoryInterface.class);

		when(factory.provisionI2CDevice(controller, address, addressSize)).thenReturn(delegate);

		byte[] bytes = new byte[2];
		bytes[0] = 1;
		bytes[1] = 2;

		final I2CDeviceInterface device = I2CDevice.builder(address).setDeviceFactory(factory).setController(controller)
				.setAddressSize(addressSize).setByteOrder(defaultByteOrder).build();
		device.writeBytes(ByteBuffer.wrap(Arrays.copyOf(bytes, bytes.length)));

		verify(delegate).writeBytes(captor.capture());
		byte[] value = captor.getValue();

		assertEquals(Arrays.toString(bytes), Arrays.toString(value));
	}
}
