package com.diozero.devices.oled;

import java.io.Closeable;
import java.io.IOException;

import com.diozero.api.I2CDevice;
import com.diozero.api.SpiDevice;

public interface SsdOledCommunicationChannel extends Closeable {
	void write(byte... data);
	void write(byte[] buffer, int offset, int length);
	@Override
	void close();
	
	public static class SpiCommunicationChannel implements SsdOledCommunicationChannel {
		private SpiDevice device;
		
		public SpiCommunicationChannel(int chipSelect, int controller, int spiFrequency) {
			device = SpiDevice.builder(chipSelect).setController(controller).setFrequency(spiFrequency).build();
		}

		@Override
		public void write(byte... data) {
			device.write(data);
		}

		@Override
		public void write(byte[] txBuffer, int txOffset, int length) {
			device.write(txBuffer, txOffset, length);
		}

		@Override
		public void close() {
			device.close();
		}
	}
	
	public static class I2cCommunicationChannel implements SsdOledCommunicationChannel {
		private I2CDevice device;

		@Override
		public void write(byte... commands) {
			// TODO Check I2C transaction size limit
			device.writeBytes(commands);
		}

		@Override
		public void write(byte[] buffer, int offset, int length) {
			// TODO Check I2C transaction size limit
			byte[] data = new byte[length];
			System.arraycopy(buffer, offset, data, 0, length);
			device.writeBytes(data);
		}

		@Override
		public void close() {
			device.close();
		}
	}
}
