package com.diozero.internal.provider.remote.grpc;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Remote Provider
 * Filename:     GrpcClientI2CDevice.java
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.tinylog.Logger;

import com.diozero.api.I2CConstants;
import com.diozero.api.I2CDevice.ProbeMode;
import com.diozero.api.I2CDeviceInterface;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.InternalI2CDeviceInterface;
import com.diozero.remote.DiozeroProtosConverter;
import com.diozero.remote.message.protobuf.BooleanResponse;
import com.diozero.remote.message.protobuf.ByteResponse;
import com.diozero.remote.message.protobuf.BytesResponse;
import com.diozero.remote.message.protobuf.I2C;
import com.diozero.remote.message.protobuf.I2CServiceGrpc.I2CServiceBlockingStub;
import com.diozero.remote.message.protobuf.Response;
import com.diozero.remote.message.protobuf.Status;
import com.diozero.remote.message.protobuf.WordResponse;
import com.google.protobuf.ByteString;

import io.grpc.StatusRuntimeException;

public class GrpcClientI2CDevice extends AbstractDevice implements InternalI2CDeviceInterface {
	private I2CServiceBlockingStub i2cBlockingStub;
	private int controller;
	private int address;

	public GrpcClientI2CDevice(GrpcClientDeviceFactory deviceFactory, String key, int controller, int address,
			I2CConstants.AddressSize addressSize) {
		super(key, deviceFactory);

		i2cBlockingStub = deviceFactory.getI2CServiceStub();

		this.controller = controller;
		this.address = address;

		try {
			Response response = i2cBlockingStub.open(I2C.Open.newBuilder().setController(controller).setAddress(address)
					.setAddressSize(addressSize.getSize()).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in I2C open: " + response.getDetail());
			}
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in I2C open: " + e, e);
		}
	}

	@Override
	public boolean probe(ProbeMode mode) throws RuntimeIOException {
		try {
			BooleanResponse response = i2cBlockingStub.probe(I2C.Probe.newBuilder().setController(controller)
					.setAddress(address).setProbeMode(DiozeroProtosConverter.convert(mode)).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in I2C probe: " + response.getDetail());
			}

			return response.getData();
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in I2C probe: " + e, e);
		}
	}

	@Override
	public void writeQuick(byte bit) {
		try {
			Response response = i2cBlockingStub
					.writeQuick(I2C.Bit.newBuilder().setController(controller).setAddress(address).setBit(bit).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in I2C write quick: " + response.getDetail());
			}
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in I2C write quick: " + e, e);
		}
	}

	@Override
	public byte readByte() throws RuntimeIOException {
		try {
			ByteResponse response = i2cBlockingStub
					.readByte(I2C.Identifier.newBuilder().setController(controller).setAddress(address).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in I2C read byte: " + response.getDetail());
			}

			return (byte) response.getData();
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in I2C read byte: " + e, e);
		}
	}

	@Override
	public void writeByte(byte b) throws RuntimeIOException {
		try {
			Response response = i2cBlockingStub.writeByte(I2C.ByteMessage.newBuilder().setController(controller)
					.setAddress(address).setData(b & 0xff).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in I2C write byte: " + response.getDetail());
			}
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in I2C write byte: " + e, e);
		}
	}

	@Override
	public byte readByteData(int register) throws RuntimeIOException {
		try {
			ByteResponse response = i2cBlockingStub.readByteData(I2C.Register.newBuilder().setController(controller)
					.setAddress(address).setRegister(register).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in I2C read byte data: " + response.getDetail());
			}

			return (byte) response.getData();
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in I2C read byte data: " + e, e);
		}
	}

	@Override
	public void writeByteData(int register, byte b) throws RuntimeIOException {
		try {
			Response response = i2cBlockingStub.writeByteData(I2C.RegisterAndByte.newBuilder().setController(controller)
					.setAddress(address).setRegister(register).setData(b & 0xff).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in I2C write byte data: " + response.getDetail());
			}
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in I2C write byte data: " + e, e);
		}
	}

	@Override
	public short readWordData(int register) throws RuntimeIOException {
		try {
			WordResponse response = i2cBlockingStub.readWordData(I2C.Register.newBuilder().setController(controller)
					.setAddress(address).setRegister(register).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in I2C read word data: " + response.getDetail());
			}

			return (short) response.getData();
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in I2C read word data: " + e, e);
		}
	}

	@Override
	public void writeWordData(int register, short s) throws RuntimeIOException {
		try {
			Response response = i2cBlockingStub.writeWordData(I2C.RegisterAndWordData.newBuilder()
					.setController(controller).setAddress(address).setRegister(register).setData(s & 0xffff).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in I2C write word data: " + response.getDetail());
			}
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in I2C write word data: " + e, e);
		}
	}

	@Override
	public short processCall(int register, short s) throws RuntimeIOException {
		try {
			WordResponse response = i2cBlockingStub.processCall(I2C.RegisterAndWordData.newBuilder()
					.setController(controller).setAddress(address).setRegister(register).setData(s & 0xffff).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in I2C process call: " + response.getDetail());
			}

			return (short) response.getData();
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in I2C process call: " + e, e);
		}
	}

	@Override
	public byte[] readBlockData(int register) throws RuntimeIOException {
		try {
			I2C.ByteArrayWithLengthResponse response = i2cBlockingStub.readBlockData(I2C.Register.newBuilder()
					.setController(controller).setAddress(address).setRegister(register).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in I2C read block data: " + response.getDetail());
			}

			return response.getData().toByteArray();
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in I2C read block data: " + e, e);
		}
	}

	@Override
	public void writeBlockData(int register, byte... data) throws RuntimeIOException {
		try {
			Response response = i2cBlockingStub
					.writeBlockData(I2C.RegisterAndByteArray.newBuilder().setController(controller).setAddress(address)
							.setRegister(register).setData(ByteString.copyFrom(data)).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in I2C write block data: " + response.getDetail());
			}
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in I2C write block data: " + e, e);
		}
	}

	@Override
	public byte[] blockProcessCall(int register, byte... txData) throws RuntimeIOException {
		try {
			BytesResponse response = i2cBlockingStub
					.blockProcessCall(I2C.RegisterAndByteArray.newBuilder().setController(controller)
							.setAddress(address).setRegister(register).setData(ByteString.copyFrom(txData)).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in I2C block process call: " + response.getDetail());
			}

			return response.getData().toByteArray();
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in I2C block process call: " + e, e);
		}
	}

	@Override
	public int readI2CBlockData(int register, byte[] buffer) throws RuntimeIOException {
		try {
			BytesResponse response = i2cBlockingStub
					.readI2CBlockData(I2C.RegisterAndNumBytes.newBuilder().setController(controller).setAddress(address)
							.setRegister(register).setLength(buffer.length).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in I2C read I2C block data: " + response.getDetail());
			}

			byte[] response_data = response.getData().toByteArray();
			System.arraycopy(response_data, 0, buffer, 0, response_data.length);

			return response_data.length;
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in I2C read I2C block: " + e, e);
		}
	}

	@Override
	public void writeI2CBlockData(int register, byte... data) throws RuntimeIOException {
		try {
			Response response = i2cBlockingStub
					.writeI2CBlockData(I2C.RegisterAndByteArray.newBuilder().setController(controller)
							.setAddress(address).setRegister(register).setData(ByteString.copyFrom(data)).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in I2C write I2C block data: " + response.getDetail());
			}
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in I2C write I2C block data: " + e, e);
		}
	}

	@Override
	public int readBytes(byte[] buffer) throws RuntimeIOException {
		try {
			BytesResponse response = i2cBlockingStub.readBytes(I2C.NumBytes.newBuilder().setController(controller)
					.setAddress(address).setLength(buffer.length).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in I2C read bytes: " + response.getDetail());
			}

			byte[] response_data = response.getData().toByteArray();
			System.arraycopy(response_data, 0, buffer, 0, response_data.length);

			return response_data.length;
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in I2C read bytes: " + e, e);
		}
	}

	@Override
	public void writeBytes(byte... data) throws RuntimeIOException {
		try {
			Response response = i2cBlockingStub.writeBytes(I2C.ByteArray.newBuilder().setController(controller)
					.setAddress(address).setData(ByteString.copyFrom(data)).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in I2C write bytes: " + response.getDetail());
			}
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in I2C write bytes: " + e, e);
		}
	}

	@Override
	public void readWrite(I2CDeviceInterface.I2CMessage[] messages, byte[] buffer) {
		try {
			I2C.ReadWrite.Builder request_builder = I2C.ReadWrite.newBuilder().setController(controller)
					.setAddress(address);

			// Extract the write message data
			int buffer_pos = 0;
			byte[] tx_data;
			try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
				for (I2CDeviceInterface.I2CMessage message : messages) {
					if (message.isWrite()) {
						baos.write(buffer, buffer_pos, message.getLength());
					}

					buffer_pos += message.getLength();

					request_builder.addMessage(I2C.I2CMessage.newBuilder().setFlags(message.getFlags())
							.setLen(message.getLength()).build());
				}

				tx_data = baos.toByteArray();
			} catch (IOException e) {
				throw new RuntimeIOException(e);
			}
			request_builder.setData(ByteString.copyFrom(tx_data));

			BytesResponse response = i2cBlockingStub.readWrite(request_builder.build());

			// Copy the read data back into buffer
			byte[] rx_data = response.getData().toByteArray();
			buffer_pos = 0;
			int rx_data_pos = 0;
			for (I2CDeviceInterface.I2CMessage message : messages) {
				if (message.isRead()) {
					System.arraycopy(rx_data, rx_data_pos, buffer, buffer_pos, message.getLength());
					rx_data_pos += message.getLength();
				}
				buffer_pos += message.getLength();
			}
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in I2C readWrite: " + e, e);
		}
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.trace("closeDevice() {}", getKey());
		try {
			Response response = i2cBlockingStub
					.close(I2C.Identifier.newBuilder().setController(controller).setAddress(address).build());
			if (response.getStatus() != Status.OK) {
				throw new RuntimeIOException("Error in I2C close: " + response.getDetail());
			}
		} catch (StatusRuntimeException e) {
			throw new RuntimeIOException("Error in I2C close: " + e, e);
		}
	}
}
