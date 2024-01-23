package com.diozero.remote.server.grpc;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Remote Server
 * Filename:     I2CServiceImpl.java
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
import com.diozero.api.I2CDeviceInterface;
import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.InternalDeviceInterface;
import com.diozero.internal.spi.NativeDeviceFactoryInterface;
import com.diozero.remote.DiozeroProtosConverter;
import com.diozero.remote.message.protobuf.BooleanResponse;
import com.diozero.remote.message.protobuf.ByteResponse;
import com.diozero.remote.message.protobuf.BytesResponse;
import com.diozero.remote.message.protobuf.I2C;
import com.diozero.remote.message.protobuf.I2CServiceGrpc;
import com.diozero.remote.message.protobuf.Response;
import com.diozero.remote.message.protobuf.Status;
import com.diozero.remote.message.protobuf.WordResponse;
import com.diozero.sbc.DeviceFactoryHelper;
import com.google.protobuf.ByteString;

import io.grpc.stub.StreamObserver;

public class I2CServiceImpl extends I2CServiceGrpc.I2CServiceImplBase {
	private NativeDeviceFactoryInterface deviceFactory;

	public I2CServiceImpl() {
		this(DeviceFactoryHelper.getNativeDeviceFactory());
	}

	public I2CServiceImpl(NativeDeviceFactoryInterface deviceFactory) {
		this.deviceFactory = deviceFactory;
	}

	@Override
	public void open(I2C.Open request, StreamObserver<Response> responseObserver) {
		Logger.debug("I2C open request {}-{} {}", Integer.valueOf(request.getController()),
				Integer.valueOf(request.getAddress()), Integer.valueOf(request.getAddressSize()));

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		Response.Builder response_builder = Response.newBuilder();

		InternalDeviceInterface device = deviceFactory.getDevice(key);
		if (device != null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("I2C device already provisioned");
		} else {
			try {
				device = deviceFactory.provisionI2CDevice(controller, address,
						I2CConstants.AddressSize.valueOf(request.getAddressSize()));

				response_builder.setStatus(Status.OK);
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Runtime Error: " + e);
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void probe(I2C.Probe request, StreamObserver<BooleanResponse> responseObserver) {
		Logger.debug("I2C probe request {}-{} {}", Integer.valueOf(request.getController()),
				Integer.valueOf(request.getAddress()), request.getProbeMode());

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		BooleanResponse.Builder response_builder = BooleanResponse.newBuilder();

		I2CDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder
					.setDetail("I2C device " + controller + "-0x" + Integer.toHexString(address) + " not provisioned");
		} else {
			try {
				response_builder.setData(device.probe(DiozeroProtosConverter.convert(request.getProbeMode())));
				response_builder.setStatus(Status.OK);
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Runtime Error: " + e);
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void writeQuick(I2C.Bit request, StreamObserver<Response> responseObserver) {
		Logger.debug("I2C writeQuick request {}-{} {}", Integer.valueOf(request.getController()),
				Integer.valueOf(request.getAddress()), Integer.valueOf(request.getBit()));

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		Response.Builder response_builder = Response.newBuilder();

		I2CDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("I2C device not provisioned");
		} else {
			try {
				device.writeQuick((byte) request.getBit());

				response_builder.setStatus(Status.OK);
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Runtime Error: " + e);
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void readByte(I2C.Identifier request, StreamObserver<ByteResponse> responseObserver) {
		Logger.debug("I2C readByte request {}-{}", Integer.valueOf(request.getController()),
				Integer.valueOf(request.getAddress()));

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		ByteResponse.Builder response_builder = ByteResponse.newBuilder();

		I2CDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("I2C device not provisioned");
		} else {
			try {
				response_builder.setData(device.readByte());
				response_builder.setStatus(Status.OK);
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Runtime Error: " + e);
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void writeByte(I2C.ByteMessage request, StreamObserver<Response> responseObserver) {
		Logger.debug("I2C writeByte request {}-{} {}", Integer.valueOf(request.getController()),
				Integer.valueOf(request.getAddress()), Integer.valueOf(request.getData()));

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		Response.Builder response_builder = Response.newBuilder();

		I2CDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("I2C device not provisioned");
		} else {
			try {
				device.writeByte((byte) request.getData());
				response_builder.setStatus(Status.OK);
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Runtime Error: " + e);
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void readByteData(I2C.Register request, StreamObserver<ByteResponse> responseObserver) {
		Logger.debug("I2C readByteData request {}-{} {}", Integer.valueOf(request.getController()),
				Integer.valueOf(request.getAddress()), Integer.valueOf(request.getRegister()));

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		ByteResponse.Builder response_builder = ByteResponse.newBuilder();

		I2CDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("I2C device not provisioned");
		} else {
			try {
				response_builder.setData(device.readByteData(request.getRegister()));
				response_builder.setStatus(Status.OK);
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Runtime Error: " + e);
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void writeByteData(I2C.RegisterAndByte request, StreamObserver<Response> responseObserver) {
		Logger.debug("I2C writeByteData request {}-{} {}: {}", Integer.valueOf(request.getController()),
				Integer.valueOf(request.getAddress()), Integer.valueOf(request.getRegister()),
				Integer.valueOf(request.getData()));

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		Response.Builder response_builder = Response.newBuilder();

		I2CDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("I2C device not provisioned");
		} else {
			try {
				device.writeByteData(request.getRegister(), (byte) request.getData());

				response_builder.setStatus(Status.OK);
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Runtime Error: " + e);
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void readWordData(I2C.Register request, StreamObserver<WordResponse> responseObserver) {
		Logger.debug("I2C readWordData request {}-{} {}", Integer.valueOf(request.getController()),
				Integer.valueOf(request.getAddress()), Integer.valueOf(request.getRegister()));

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		WordResponse.Builder response_builder = WordResponse.newBuilder();

		I2CDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("I2C device not provisioned");
		} else {
			try {
				response_builder.setData(device.readWordData(request.getRegister()));
				response_builder.setStatus(Status.OK);
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Runtime Error: " + e);
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void writeWordData(I2C.RegisterAndWordData request, StreamObserver<Response> responseObserver) {
		Logger.debug("I2C writeWordData request {}-{} {}: {}", Integer.valueOf(request.getController()),
				Integer.valueOf(request.getAddress()), Integer.valueOf(request.getRegister()),
				Integer.valueOf(request.getData()));

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		Response.Builder response_builder = Response.newBuilder();

		I2CDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("I2C device not provisioned");
		} else {
			try {
				device.writeWordData(request.getRegister(), (short) request.getData());

				response_builder.setStatus(Status.OK);
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Runtime Error: " + e);
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void readBlockData(I2C.Register request, StreamObserver<I2C.ByteArrayWithLengthResponse> responseObserver) {
		Logger.debug("I2C readBlockData request {}-{} {}", Integer.valueOf(request.getController()),
				Integer.valueOf(request.getAddress()), Integer.valueOf(request.getRegister()));

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		I2C.ByteArrayWithLengthResponse.Builder response_builder = I2C.ByteArrayWithLengthResponse.newBuilder();

		I2CDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("I2C device not provisioned");
		} else {
			try {
				byte[] buffer = device.readBlockData(request.getRegister());

				response_builder.setData(ByteString.copyFrom(buffer));
				response_builder.setBytesRead(buffer.length);
				response_builder.setStatus(Status.OK);
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Runtime Error: " + e);
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void writeBlockData(I2C.RegisterAndByteArray request, StreamObserver<Response> responseObserver) {
		Logger.debug("I2C writeBlockData request {}-{} {} {} bytes", Integer.valueOf(request.getController()),
				Integer.valueOf(request.getAddress()), Integer.valueOf(request.getRegister()),
				Integer.valueOf(request.getData().size()));

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		Response.Builder response_builder = Response.newBuilder();

		I2CDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("I2C device not provisioned");
		} else {
			try {
				device.writeBlockData(request.getRegister(), request.getData().toByteArray());

				response_builder.setStatus(Status.OK);
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Runtime Error: " + e);
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void processCall(I2C.RegisterAndWordData request, StreamObserver<WordResponse> responseObserver) {
		Logger.debug("I2C processCall request {}-{} {}: {}", Integer.valueOf(request.getController()),
				Integer.valueOf(request.getAddress()), Integer.valueOf(request.getRegister()),
				Integer.valueOf(request.getData()));

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		WordResponse.Builder response_builder = WordResponse.newBuilder();

		I2CDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("I2C device not provisioned");
		} else {
			try {
				short result = device.processCall(request.getRegister(), (short) request.getData());

				response_builder.setData(result);
				response_builder.setStatus(Status.OK);
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Runtime Error: " + e);
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void readI2CBlockData(I2C.RegisterAndNumBytes request, StreamObserver<BytesResponse> responseObserver) {
		Logger.debug("I2C readI2CBlockData request {}-{} {} {} bytes", Integer.valueOf(request.getController()),
				Integer.valueOf(request.getAddress()), Integer.valueOf(request.getRegister()),
				Integer.valueOf(request.getLength()));

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		BytesResponse.Builder response_builder = BytesResponse.newBuilder();

		I2CDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("I2C device not provisioned");
		} else {
			try {
				byte[] buffer = new byte[request.getLength()];
				device.readI2CBlockData(request.getRegister(), buffer);

				response_builder.setData(ByteString.copyFrom(buffer));
				response_builder.setStatus(Status.OK);
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Runtime Error: " + e);
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void writeI2CBlockData(I2C.RegisterAndByteArray request, StreamObserver<Response> responseObserver) {
		Logger.debug("I2C writeI2CBlockData request {}-{} {} {} bytes", Integer.valueOf(request.getController()),
				Integer.valueOf(request.getAddress()), Integer.valueOf(request.getRegister()),
				Integer.valueOf(request.getData().size()));

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		Response.Builder response_builder = Response.newBuilder();

		I2CDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("I2C device not provisioned");
		} else {
			try {
				device.writeI2CBlockData(request.getRegister(), request.getData().toByteArray());

				response_builder.setStatus(Status.OK);
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Runtime Error: " + e);
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void blockProcessCall(I2C.RegisterAndByteArray request, StreamObserver<BytesResponse> responseObserver) {
		Logger.debug("I2C blockProcessCall request {}-{} {} {} bytes", Integer.valueOf(request.getController()),
				Integer.valueOf(request.getAddress()), Integer.valueOf(request.getRegister()),
				Integer.valueOf(request.getData().size()));

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		BytesResponse.Builder response_builder = BytesResponse.newBuilder();

		I2CDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("I2C device not provisioned");
		} else {
			try {
				byte[] result = device.blockProcessCall(request.getRegister(), request.getData().toByteArray());

				response_builder.setData(ByteString.copyFrom(result));
				response_builder.setStatus(Status.OK);
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Runtime Error: " + e);
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void readBytes(I2C.NumBytes request, StreamObserver<BytesResponse> responseObserver) {
		Logger.debug("I2C readBytes request {}-{} {} bytes", Integer.valueOf(request.getController()),
				Integer.valueOf(request.getAddress()), Integer.valueOf(request.getLength()));

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		BytesResponse.Builder response_builder = BytesResponse.newBuilder();

		I2CDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("I2C device not provisioned");
		} else {
			try {
				byte[] buffer = new byte[request.getLength()];
				device.readBytes(buffer);

				response_builder.setData(ByteString.copyFrom(buffer));
				response_builder.setStatus(Status.OK);
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Runtime Error: " + e);
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void writeBytes(I2C.ByteArray request, StreamObserver<Response> responseObserver) {
		Logger.debug("I2C writeBytes request {}-{} {} bytes", Integer.valueOf(request.getController()),
				Integer.valueOf(request.getAddress()), Integer.valueOf(request.getData().size()));

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		Response.Builder response_builder = Response.newBuilder();

		I2CDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("I2C device not provisioned");
		} else {
			try {
				byte[] data = request.getData().toByteArray();
				device.writeBytes(data);

				response_builder.setStatus(Status.OK);
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Runtime Error: " + e);
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	public void readWrite(I2C.ReadWrite request, StreamObserver<BytesResponse> responseObserver) {
		Logger.debug("I2C readWrite request {}-{} {} messages {} bytes", Integer.valueOf(request.getController()),
				Integer.valueOf(request.getAddress()), Integer.valueOf(request.getMessageList().size()),
				Integer.valueOf(request.getData().size()));

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		BytesResponse.Builder response_builder = BytesResponse.newBuilder();

		I2CDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("I2C device not provisioned");
		} else {
			try {
				byte[] write_data = request.getData().toByteArray();

				byte[] i2c_msgs_buffer;
				int read_buffer_length = 0;
				I2CDeviceInterface.I2CMessage[] messages = new I2CDeviceInterface.I2CMessage[request.getMessageCount()];
				try (ByteArrayOutputStream i2c_msgs_baos = new ByteArrayOutputStream()) {
					int i = 0;
					int write_data_pos = 0;
					for (I2C.I2CMessage message : request.getMessageList()) {
						messages[i] = new I2CDeviceInterface.I2CMessage(message.getFlags(), message.getLen());

						// Add to the i2c_msgs_buffer
						if (messages[i].isWrite()) {
							// Write - add the data to be written
							i2c_msgs_baos.write(write_data, write_data_pos, message.getLen());
							write_data_pos += message.getLen();
						} else {
							// Read - fill with zeros, will be populated by readWrite
							for (int x = 0; x < message.getLen(); x++) {
								i2c_msgs_baos.write(0);
							}
							read_buffer_length += message.getLen();
						}

						i++;
					}

					i2c_msgs_buffer = i2c_msgs_baos.toByteArray();
				}

				device.readWrite(messages, i2c_msgs_buffer);

				// Copy the read data from the i2c_msgs_buffer into the response read_buffer
				byte[] read_buffer = new byte[read_buffer_length];
				int i2c_msgs_buffer_pos = 0;
				int read_buffer_pos = 0;
				for (I2CDeviceInterface.I2CMessage message : messages) {
					if (message.isRead()) {
						// Read command, copy the data from i2c_msgs into read_buffer
						System.arraycopy(i2c_msgs_buffer, i2c_msgs_buffer_pos, read_buffer, read_buffer_pos,
								message.getLength());
						read_buffer_pos += message.getLength();
					}
					i2c_msgs_buffer_pos += message.getLength();
				}

				response_builder.setData(ByteString.copyFrom(read_buffer));
				response_builder.setStatus(Status.OK);
			} catch (IOException | RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Runtime Error: " + e);
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}

	@Override
	@SuppressWarnings("resource")
	public void close(I2C.Identifier request, StreamObserver<Response> responseObserver) {
		Logger.debug("I2C close request {}-{}", Integer.valueOf(request.getController()),
				Integer.valueOf(request.getAddress()));

		int controller = request.getController();
		int address = request.getAddress();
		String key = deviceFactory.createI2CKey(controller, address);

		Response.Builder response_builder = Response.newBuilder();

		I2CDeviceInterface device = deviceFactory.getDevice(key);
		if (device == null) {
			response_builder.setStatus(Status.ERROR);
			response_builder.setDetail("I2C device not provisioned");
		} else {
			try {
				device.close();

				response_builder.setStatus(Status.OK);
			} catch (RuntimeIOException e) {
				Logger.error(e, "Error: {}", e);
				response_builder.setStatus(Status.ERROR);
				response_builder.setDetail("Runtime Error: " + e);
			}
		}

		responseObserver.onNext(response_builder.build());
		responseObserver.onCompleted();
	}
}
