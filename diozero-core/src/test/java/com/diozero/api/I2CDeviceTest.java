package com.diozero.api;

import com.diozero.internal.spi.I2CDeviceFactoryInterface;
import com.diozero.internal.spi.InternalI2CDeviceInterface;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

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

        I2CDevice device = new I2CDevice(factory, controller, address, addressSize, defaultByteOrder);
        device.writeBytes(ByteBuffer.wrap(Arrays.copyOf(bytes, bytes.length)));

        verify(delegate).writeBytes(captor.capture());
        byte[] value = captor.getValue();

        assertEquals(Arrays.toString(bytes), Arrays.toString(value));
    }
}
