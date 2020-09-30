package com.diozero.internal.provider.remote.voodoospark;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Remote Provider
 * Filename:     VoodooSparkProtocolHandler.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 diozero
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

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import org.tinylog.Logger;

import com.diozero.api.DigitalInputEvent;
import com.diozero.internal.provider.remote.devicefactory.RemoteDeviceFactory;
import com.diozero.remote.message.GetBoardInfo;
import com.diozero.remote.message.GetBoardInfoResponse;
import com.diozero.remote.message.GpioAnalogRead;
import com.diozero.remote.message.GpioAnalogReadResponse;
import com.diozero.remote.message.GpioAnalogWrite;
import com.diozero.remote.message.GpioClose;
import com.diozero.remote.message.GpioDigitalRead;
import com.diozero.remote.message.GpioDigitalReadResponse;
import com.diozero.remote.message.GpioDigitalWrite;
import com.diozero.remote.message.GpioEvents;
import com.diozero.remote.message.GpioInfo;
import com.diozero.remote.message.GpioPwmRead;
import com.diozero.remote.message.GpioPwmReadResponse;
import com.diozero.remote.message.GpioPwmWrite;
import com.diozero.remote.message.I2CClose;
import com.diozero.remote.message.I2COpen;
import com.diozero.remote.message.I2CRead;
import com.diozero.remote.message.I2CReadByte;
import com.diozero.remote.message.I2CReadByteData;
import com.diozero.remote.message.I2CReadByteResponse;
import com.diozero.remote.message.I2CReadI2CBlockData;
import com.diozero.remote.message.I2CReadResponse;
import com.diozero.remote.message.I2CWrite;
import com.diozero.remote.message.I2CWriteByte;
import com.diozero.remote.message.I2CWriteByteData;
import com.diozero.remote.message.I2CWriteI2CBlockData;
import com.diozero.remote.message.ProvisionAnalogInputDevice;
import com.diozero.remote.message.ProvisionAnalogOutputDevice;
import com.diozero.remote.message.ProvisionDigitalInputDevice;
import com.diozero.remote.message.ProvisionDigitalInputOutputDevice;
import com.diozero.remote.message.ProvisionDigitalOutputDevice;
import com.diozero.remote.message.ProvisionPwmOutputDevice;
import com.diozero.remote.message.RemoteProtocolInterface;
import com.diozero.remote.message.Response;
import com.diozero.remote.message.SpiClose;
import com.diozero.remote.message.SpiOpen;
import com.diozero.remote.message.SpiResponse;
import com.diozero.remote.message.SpiWrite;
import com.diozero.remote.message.SpiWriteAndRead;
import com.diozero.util.PropertyUtil;
import com.diozero.util.RangeUtil;
import com.diozero.util.RuntimeIOException;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.MessageToByteEncoder;

public class VoodooSparkProtocolHandler implements RemoteProtocolInterface {
	private static final String DEVICE_ID_PROP = "PARTICLE_DEVICE_ID";
	private static final String ACCESS_TOKEN_PROP = "PARTICLE_TOKEN";
	
	static final int ANALOG_MAX = (int) (Math.pow(2, 12) - 1);
	static final int PWM_MAX = (int) (Math.pow(2, 8) - 1);
	private static final int DEFAULT_FREQUENCY = 500;

	// Voodoo Spark network commands
	private static final byte PIN_MODE = 0x00;
	private static final byte DIGITAL_WRITE = 0x01;
	private static final byte ANALOG_WRITE = 0x02;
	private static final byte DIGITAL_READ = 0x03;
	private static final byte ANALOG_READ = 0x04;
	private static final byte REPORTING = 0x05;
	private static final byte SET_SAMPLE_INTERVAL = 0x06;
	private static final byte INTERNAL_RGB = 0x07;
	private static final byte PING_READ = 0x08;
	/* NOTE GAP */
	//private static final byte SERIAL_BEGIN = 0x10;
	//private static final byte SERIAL_END = 0x11;
	//private static final byte SERIAL_PEEK = 0x12;
	//private static final byte SERIAL_AVAILABLE = 0x13;
	//private static final byte SERIAL_WRITE = 0x14;
	//private static final byte SERIAL_READ = 0x15;
	//private static final byte SERIAL_FLUSH = 0x16;
	/* NOTE GAP */
	//private static final byte SPI_BEGIN = 0x20;
	//private static final byte SPI_END = 0x21;
	//private static final byte SPI_SET_BIT_ORDER = 0x22;
	//private static final byte SPI_SET_CLOCK = 0x23;
	//private static final byte SPI_SET_DATA_MODE = 0x24;
	//private static final byte SPI_TRANSFER = 0x25;
	// /* NOTE GAP */
	private static final byte I2C_CONFIG = 0x30;
	private static final byte I2C_WRITE = 0x31;
	private static final byte I2C_READ = 0x32;
	private static final byte I2C_READ_CONTINUOUS = 0x33;
	private static final byte I2C_REGISTER_NOT_SPECIFIED = (byte) 0xFF;
	/* NOTE GAP */
	private static final byte SERVO_WRITE = 0x41;
	private static final byte ACTION_RANGE = 0x46;
	
	private RemoteDeviceFactory deviceFactory;
	private Queue<ResponseMessage> messageQueue;
	private EventLoopGroup workerGroup;
	private Channel messageChannel;
	private Lock lock;
	private Condition condition;
	private ChannelFuture lastWriteFuture;
	private int timeoutMs;

	public VoodooSparkProtocolHandler(RemoteDeviceFactory deviceFactory) {
		this.deviceFactory = deviceFactory;
		
		String device_id = PropertyUtil.getProperty(DEVICE_ID_PROP, null);
		String access_token = PropertyUtil.getProperty(ACCESS_TOKEN_PROP, null);
		if (device_id == null || access_token == null) {
			Logger.error("Both {} and {} properties must be set", DEVICE_ID_PROP, ACCESS_TOKEN_PROP);
			throw new IllegalArgumentException(String.format("Both %s and %s properties must be set",
					DEVICE_ID_PROP, ACCESS_TOKEN_PROP));
		}
		
		timeoutMs = 2000;
		messageQueue = new LinkedList<>();
		lock = new ReentrantLock();
		condition = lock.newCondition();
		
		// Lookup the local IP address using the Particle "endpoint" custom variable
		try {
			URL url = new URL(String.format("https://api.particle.io/v1/devices/%s/endpoint?access_token=%s", device_id,
					URLEncoder.encode(access_token, StandardCharsets.UTF_8.name())));
			Endpoint endpoint = new Gson().fromJson(new InputStreamReader(url.openStream()), Endpoint.class);
			Logger.debug(endpoint);
			String[] ip_port = endpoint.result.split(":");
			
			connect(ip_port[0], Integer.parseInt(ip_port[1]));
		} catch (IOException | NumberFormatException | InterruptedException e) {
			// 403 - device id not found
			// 401 - bad access token
			Logger.error(e, "Error: {}", e);
			throw new RuntimeIOException("Error getting local endpoint", e);
		}
	}
	
	private void connect(String host, int port) throws InterruptedException {
		workerGroup = new NioEventLoopGroup();
		
		ResponseHandler rh = new ResponseHandler(this::messageReceived);
		
		Bootstrap b1 = new Bootstrap();
		b1.group(workerGroup).channel(NioSocketChannel.class).handler(new ChannelInitializer<SocketChannel>() {
			@Override
			public void initChannel(SocketChannel ch) throws Exception {
				ch.pipeline().addLast(new ResponseDecoder(), new MessageEncoder(), rh);
			}
		});
		
		// Connect
		messageChannel = b1.connect(host, port).sync().channel();
	}

	@Override
	public void close() {
		if (messageChannel == null || ! messageChannel.isOpen()) {
			return;
		}
		
		messageChannel.close();
		
		try {
			messageChannel.closeFuture().sync();
			
			// Wait until all messages are flushed before closing the channel.
			if (lastWriteFuture != null) {
				lastWriteFuture.sync();
			}
		} catch (InterruptedException e) {
			System.err.println("Error: " + e);
			e.printStackTrace(System.err);
		} finally {
			workerGroup.shutdownGracefully();
		}
	}

	@Override
	public GetBoardInfoResponse request(GetBoardInfo request) {
		List<GpioInfo> gpios = new ArrayList<>();
		
		// TODO Implementation
		
		return new GetBoardInfoResponse("VoodooSpark", "Unknown", -1, gpios, request.getCorrelationId());
	}

	@Override
	public Response request(ProvisionDigitalInputDevice request) {
		sendMessage(new PinModeMessage(request.getGpio(), PinMode.DIGITAL_INPUT));
		
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public Response request(ProvisionDigitalOutputDevice request) {
		sendMessage(new PinModeMessage(request.getGpio(), PinMode.DIGITAL_OUTPUT));
		
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public Response request(ProvisionDigitalInputOutputDevice request) {
		sendMessage(new PinModeMessage(request.getGpio(), request.getOutput() ? PinMode.DIGITAL_OUTPUT : PinMode.DIGITAL_INPUT));
		
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public Response request(ProvisionPwmOutputDevice request) {
		sendMessage(new PinModeMessage(request.getGpio(), PinMode.ANALOG_OUTPUT));
		
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public Response request(ProvisionAnalogInputDevice request) {
		sendMessage(new PinModeMessage(request.getGpio(), PinMode.ANALOG_INPUT));
		
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public Response request(ProvisionAnalogOutputDevice request) {
		sendMessage(new PinModeMessage(request.getGpio(), PinMode.ANALOG_OUTPUT));
		
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public GpioDigitalReadResponse request(GpioDigitalRead request) {
		ResponseMessage rm = sendMessage(new DigitalReadMessage(request.getGpio()));
		return new GpioDigitalReadResponse(rm.lsb != 0, request.getCorrelationId());
	}

	@Override
	public Response request(GpioDigitalWrite request) {
		sendMessage(new DigitalWriteMessage(request.getGpio(), request.getValue()));
		
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public GpioPwmReadResponse request(GpioPwmRead request) {
		ResponseMessage rm = sendMessage(new AnalogReadMessage(request.getGpio()));
		float value = RangeUtil.map((rm.lsb & 0x7f) | ((rm.msb & 0x7f) << 7), 0, PWM_MAX, 0f, 1f, true);
		
		return new GpioPwmReadResponse(value, request.getCorrelationId());
	}

	@Override
	public Response request(GpioPwmWrite request) {
		int value = RangeUtil.map(request.getValue(), 0f, 1f, 0, PWM_MAX, true);
		sendMessage(new AnalogWriteMessage(request.getGpio(), value));
		
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public GpioAnalogReadResponse request(GpioAnalogRead request) {
		ResponseMessage rm = sendMessage(new AnalogReadMessage(request.getGpio()));
		float value = RangeUtil.map((rm.lsb & 0x7f) | ((rm.msb & 0x7f) << 7), 0, ANALOG_MAX, 0f, 1f, true);

		return new GpioAnalogReadResponse(value, request.getCorrelationId());
	}

	@Override
	public Response request(GpioAnalogWrite request) {
		int value = RangeUtil.map(request.getValue(), 0f, 1f, 0, ANALOG_MAX, true);
		sendMessage(new AnalogWriteMessage(request.getGpio(), value));
		
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public Response request(GpioEvents request) {
		sendMessage(new ReportingMessage(request.getGpio(), false));
		
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public Response request(GpioClose request) {
		sendMessage(new PinModeMessage(request.getGpio(), PinMode.DIGITAL_INPUT));
		
		return new Response(Response.Status.OK, null, request.getCorrelationId());
	}

	@Override
	public Response request(I2COpen request) {
		// TODO Implementation
		throw new UnsupportedOperationException();
	}

	@Override
	public I2CReadByteResponse request(I2CReadByte request) {
		// TODO Implementation
		throw new UnsupportedOperationException();
	}

	@Override
	public Response request(I2CWriteByte request) {
		// TODO Implementation
		throw new UnsupportedOperationException();
	}
	
	@Override
	public I2CReadResponse request(I2CRead request) {
		// TODO Implementation
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Response request(I2CWrite request) {
		// TODO Implementation
		throw new UnsupportedOperationException();
	}
	
	@Override
	public I2CReadByteResponse request(I2CReadByteData request) {
		// TODO Implementation
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Response request(I2CWriteByteData request) {
		// TODO Implementation
		throw new UnsupportedOperationException();
	}
	
	@Override
	public I2CReadResponse request(I2CReadI2CBlockData request) {
		// TODO Implementation
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Response request(I2CWriteI2CBlockData request) {
		// TODO Implementation
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Response request(I2CClose request) {
		// TODO Implementation
		throw new UnsupportedOperationException();
	}

	@Override
	public Response request(SpiOpen request) {
		// TODO Implementation
		throw new UnsupportedOperationException();
	}

	@Override
	public Response request(SpiWrite request) {
		// TODO Implementation
		throw new UnsupportedOperationException();
	}

	@Override
	public SpiResponse request(SpiWriteAndRead request) {
		// TODO Implementation
		throw new UnsupportedOperationException();
	}

	@Override
	public Response request(SpiClose request) {
		// TODO Implementation
		throw new UnsupportedOperationException();
	}
	
	private synchronized ResponseMessage sendMessage(Message message) {
		ResponseMessage rm = null;
		
		lock.lock();
		try {
			lastWriteFuture = messageChannel.writeAndFlush(message);
			lastWriteFuture.get();
			
			if (message.responseExpected ) {
				if (condition.await(timeoutMs, TimeUnit.MILLISECONDS)) {
					rm = messageQueue.remove();
					
					if (rm.cmd != message.cmd) {
						throw new RuntimeIOException(
								"Unexpected response: " + rm.cmd + ", was expecting " + message.cmd + "; discarding");
					}
				} else {
					throw new RuntimeIOException("Timeout waiting for response to command " + message.cmd);
				}
			}
		} catch (ExecutionException e) {
			throw new RuntimeIOException(e);
		} catch (InterruptedException e) {
			Logger.error(e, "Interrupted: {}", e);
		} finally {
			lock.unlock();
		}
		
		return rm;
	}
	
	void messageReceived(ResponseMessage msg) {
		if (msg.cmd == REPORTING) {
			long nano_time = System.currentTimeMillis();
			long epoch_time = System.currentTimeMillis();
			
			Logger.info("Reporting message: {}", msg);
			
			// Notify the listeners for each GPIO in this port for which reporting has been enabled
			for (int i=0; i<8; i++) {
				// Note can only get reports for GPIOs 0-7 and 10-17
				int gpio = msg.pinOrPort * 10 + i;
				
				// TODO Need to check that reporting has been enabled for this GPIO!
				// TODO What about analog events?
				
				deviceFactory
						.valueChanged(new DigitalInputEvent(gpio, epoch_time, nano_time, (msg.lsb & (1 << i)) != 0));
			}
		} else {
			lock.lock();
			try {
				messageQueue.add(msg);
				condition.signal();
			} finally {
				lock.unlock();
			}
		}
	}

	// Classes to support Particle Spark JSON variable API

	private static final class Endpoint {
		String cmd;
		String name;
		String result;
		CoreInfo coreInfo;
		
		@Override
		public String toString() {
			return "Endpoint [cmd=" + cmd + ", name=" + name + ", result=" + result + ", coreInfo=" + coreInfo + "]";
		}
	}
	
	private static final class CoreInfo {
		@SerializedName("last_app")
		String lastApp;
		@SerializedName("last_heard")
		Date lastHeard;
		boolean connected;
		@SerializedName("last_handshake_at")
		Date lastHandshakeAt;
		@SerializedName("deviceID")
		String deviceId;
		@SerializedName("product_id")
		int productId;
		
		@Override
		public String toString() {
			return "CoreInfo [lastApp=" + lastApp + ", lastHeard=" + lastHeard + ", connected=" + connected
					+ ", lastHandshakeAt=" + lastHandshakeAt + ", deviceId=" + deviceId + ", productId=" + productId + "]";
		}
	}
	
	// Classes to support Netty encode / decode
	
	static final class MessageEncoder extends MessageToByteEncoder<Message> {
		@Override
		protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception {
			out.writeBytes(msg.encode());
		}
	}
	
	static final class ResponseDecoder extends ByteToMessageDecoder {
		@Override
		protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
			in.markReaderIndex();
			if (in.readableBytes() < 4) {
				in.resetReaderIndex();
				return;
			}
			
			out.add(new ResponseMessage(in.readByte(), in.readByte(), in.readByte(), in.readByte()));
		}
	}
	
	@Sharable
	static class ResponseHandler extends SimpleChannelInboundHandler<ResponseMessage> {
		private Consumer<ResponseMessage> listener;
		
		ResponseHandler(Consumer<ResponseMessage> listener) {
			this.listener = listener;
		}
		
		@Override
		protected void channelRead0(ChannelHandlerContext context, ResponseMessage msg) {
			listener.accept(msg);
		}
		
		@Override
		public void exceptionCaught(ChannelHandlerContext context, Throwable cause) {
			Logger.error(cause, "exceptionCaught: {}", cause);
			context.close();
		}
	}
	
	// Request and response classes
	
	static enum PinMode {
		DIGITAL_INPUT(0),
		DIGITAL_OUTPUT(1),
		ANALOG_INPUT(2),
		ANALOG_OUTPUT(3), // Note for PWM as well as true analog output
		SERVO(4),
		I2C(6);
		
		private byte mode;
		private PinMode(int mode) {
			this.mode = (byte) mode;
		}

		public byte getMode() {
			return mode;
		}
	}
	
	static abstract class Message {
		byte cmd;
		boolean responseExpected;
		
		public Message(byte cmd) {
			this(cmd, false);
		}
		
		public Message(byte cmd, boolean responseExpected) {
			this.cmd = cmd;
			this.responseExpected = responseExpected;
		}
		
		abstract byte[] encode();
	}
	
	static class PinModeMessage extends Message {
		byte gpio;
		PinMode mode;
		
		PinModeMessage(int gpio, PinMode mode) {
			super(VoodooSparkProtocolHandler.PIN_MODE);
			this.gpio = (byte) gpio;
			this.mode = mode;
		}
		
		@Override
		byte[] encode() {
			return new byte[] { cmd, gpio, mode.getMode() };
		}
	}
	
	static class DigitalWriteMessage extends Message {
		byte gpio;
		boolean value;
		
		public DigitalWriteMessage(int gpio, boolean value) {
			super(VoodooSparkProtocolHandler.DIGITAL_WRITE);
			this.gpio = (byte) gpio;
			this.value = value;
		}
		
		@Override
		byte[] encode() {
			return new byte[] { cmd, gpio, value ? (byte) 1 : 0 };
		}

		@Override
		public String toString() {
			return "DigitalWriteMessage [gpio=" + gpio + ", value=" + value + "]";
		}
	}
	
	static class AnalogWriteMessage extends Message {
		byte gpio;
		int value;
		
		public AnalogWriteMessage(int gpio, int value) {
			super(VoodooSparkProtocolHandler.ANALOG_WRITE);
			this.gpio = (byte) gpio;
			this.value = value;
		}
		
		@Override
		byte[] encode() {
			return new byte[] { cmd, gpio, (byte) (value & 0x7f), (byte) ((value >> 7) & 0x7f) };
		}

		@Override
		public String toString() {
			return "AnalogWriteMessage [gpio=" + gpio + ", value=" + value + "]";
		}
	}
	
	static class DigitalReadMessage extends Message {
		byte gpio;
		
		public DigitalReadMessage(int gpio) {
			super(VoodooSparkProtocolHandler.DIGITAL_READ, true);
			this.gpio = (byte) gpio;
		}
		
		@Override
		byte[] encode() {
			return new byte[] { cmd, gpio };
		}
	}
	
	static class AnalogReadMessage extends Message {
		byte gpio;
		
		public AnalogReadMessage(int gpio) {
			super(ANALOG_READ, true);
			this.gpio = (byte) gpio;
		}
		
		@Override
		byte[] encode() {
			return new byte[] { cmd, gpio };
		}
	}
	
	static class ReportingMessage extends Message {
		private static final byte DIGITAL = 1;
		private static final byte ANALOG = 2;
		
		byte gpio;
		boolean analog;
		
		public ReportingMessage(int gpio, boolean analog) {
			super(REPORTING);
			this.gpio = (byte) gpio;
			this.analog = analog;
		}
		
		@Override
		public byte[] encode() {
			return new byte[] { cmd, gpio, analog ? ANALOG : DIGITAL };
		}
	}
	
	static class SetSampleIntervalMessage extends Message {
		int intervalMs;
		
		public SetSampleIntervalMessage(int intervalMs) {
			super(SET_SAMPLE_INTERVAL);
			this.intervalMs = intervalMs;
		}
		
		@Override
		public byte[] encode() {
			return new byte[] { cmd, (byte) (intervalMs & 0x7f), (byte) ((intervalMs >> 7) & 0x7f) };
		}
	}
	
	static class InternalRgbMessage extends Message {
		byte red, green, blue;
		
		public InternalRgbMessage(byte red, byte green, byte blue) {
			super(VoodooSparkProtocolHandler.INTERNAL_RGB);
			this.red = red;
			this.green = green;
			this.blue = blue;
		}
		
		@Override
		byte[] encode() {
			return new byte[] { cmd, red, green, blue };
		}
	}
	
	static class ResponseMessage {
		byte cmd, pinOrPort, lsb, msb;
		
		public ResponseMessage(byte cmd, byte pinOrPort, byte lsb, byte msb) {
			this.cmd = cmd;
			this.pinOrPort = pinOrPort;
			this.lsb = lsb;
			this.msb = msb;
		}

		@Override
		public String toString() {
			return "ResponseMessage [cmd=" + cmd + ", pinOrPort=" + pinOrPort + ", lsb=" + lsb + ", msb=" + msb + "]";
		}
	}
}
