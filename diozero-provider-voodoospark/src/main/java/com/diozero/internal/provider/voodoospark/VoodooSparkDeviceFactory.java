package com.diozero.internal.provider.voodoospark;

/*-
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Voodoo Spark provider for Particle Photon
 * Filename:     VoodooSparkDeviceFactory.java  
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
import java.util.Date;
import java.util.EnumSet;
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

import com.diozero.api.AbstractDigitalInputDevice;
import com.diozero.api.DeviceInterface;
import com.diozero.api.DeviceMode;
import com.diozero.api.DigitalInputEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.api.I2CConstants;
import com.diozero.api.I2CDeviceInterface;
import com.diozero.api.PinInfo;
import com.diozero.api.RuntimeIOException;
import com.diozero.api.SerialDevice;
import com.diozero.api.SerialDeviceInterface;
import com.diozero.api.SpiClockMode;
import com.diozero.api.SpiDeviceInterface;
import com.diozero.internal.spi.AnalogInputDeviceInterface;
import com.diozero.internal.spi.AnalogOutputDeviceInterface;
import com.diozero.internal.spi.BaseNativeDeviceFactory;
import com.diozero.internal.spi.GpioDigitalInputDeviceInterface;
import com.diozero.internal.spi.GpioDigitalInputOutputDeviceInterface;
import com.diozero.internal.spi.GpioDigitalOutputDeviceInterface;
import com.diozero.internal.spi.PwmOutputDeviceInterface;
import com.diozero.sbc.BoardInfo;
import com.diozero.util.PropertyUtil;
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

public class VoodooSparkDeviceFactory extends BaseNativeDeviceFactory {
	public static final String DEVICE_NAME = "VoodooSpark";

	private static final String DEVICE_ID_PROP = "PARTICLE_DEVICE_ID";
	private static final String ACCESS_TOKEN_PROP = "PARTICLE_TOKEN";

	static final int MAX_ANALOG_VALUE = (int) (Math.pow(2, 12) - 1);
	static final int MAX_PWM_VALUE = (int) (Math.pow(2, 8) - 1);
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
	// private static final byte SERIAL_BEGIN = 0x10;
	// private static final byte SERIAL_END = 0x11;
	// private static final byte SERIAL_PEEK = 0x12;
	// private static final byte SERIAL_AVAILABLE = 0x13;
	// private static final byte SERIAL_WRITE = 0x14;
	// private static final byte SERIAL_READ = 0x15;
	// private static final byte SERIAL_FLUSH = 0x16;
	/* NOTE GAP */
	// private static final byte SPI_BEGIN = 0x20;
	// private static final byte SPI_END = 0x21;
	// private static final byte SPI_SET_BIT_ORDER = 0x22;
	// private static final byte SPI_SET_CLOCK = 0x23;
	// private static final byte SPI_SET_DATA_MODE = 0x24;
	// private static final byte SPI_TRANSFER = 0x25;
	// /* NOTE GAP */
	private static final byte I2C_CONFIG = 0x30;
	private static final byte I2C_WRITE = 0x31;
	private static final byte I2C_READ = 0x32;
	private static final byte I2C_READ_CONTINUOUS = 0x33;
	private static final byte I2C_REGISTER_NOT_SPECIFIED = (byte) 0xFF;
	/* NOTE GAP */
	private static final byte SERVO_WRITE = 0x41;
	private static final byte ACTION_RANGE = 0x46;

	private Queue<ResponseMessage> messageQueue;
	private EventLoopGroup workerGroup;
	private Channel messageChannel;
	private Lock lock;
	private Condition condition;
	private ChannelFuture lastWriteFuture;
	private int timeoutMs;

	public VoodooSparkDeviceFactory() {
		String device_id = PropertyUtil.getProperty(DEVICE_ID_PROP, null);
		String access_token = PropertyUtil.getProperty(ACCESS_TOKEN_PROP, null);
		if (device_id == null || access_token == null) {
			Logger.error("Both {} and {} properties must be set", DEVICE_ID_PROP, ACCESS_TOKEN_PROP);
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
	public void shutdown() {
		if (messageChannel == null || !messageChannel.isOpen()) {
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
	public String getName() {
		return DEVICE_NAME;
	}

	@Override
	protected BoardInfo initialiseBoardInfo() {
		BoardInfo board_info = new ParticlePhotonBoardInfo();
		board_info.initialisePins();

		return board_info;
	}

	@Override
	public int getBoardPwmFrequency() {
		return DEFAULT_FREQUENCY;
	}

	@Override
	public void setBoardPwmFrequency(int pwmFrequency) {
		// Ignore
		Logger.warn("Not implemented");
	}

	@Override
	public GpioDigitalInputDeviceInterface createDigitalInputDevice(String key, PinInfo pinInfo, GpioPullUpDown pud,
			GpioEventTrigger trigger) {
		return new VoodooSparkDigitalInputDevice(this, key, pinInfo, pud, trigger);
	}

	@Override
	public GpioDigitalOutputDeviceInterface createDigitalOutputDevice(String key, PinInfo pinInfo,
			boolean initialValue) {
		return new VoodooSparkDigitalOutputDevice(this, key, pinInfo, initialValue);
	}

	@Override
	public GpioDigitalInputOutputDeviceInterface createDigitalInputOutputDevice(String key, PinInfo pinInfo,
			DeviceMode mode) {
		return new VoodooSparkDigitalInputOutputDevice(this, key, pinInfo, mode);
	}

	@Override
	public PwmOutputDeviceInterface createPwmOutputDevice(String key, PinInfo pinInfo, int pwmFrequency,
			float initialValue) {
		return new VoodooSparkPwmOutputDevice(this, key, pinInfo, pwmFrequency, initialValue);
	}

	@Override
	public AnalogInputDeviceInterface createAnalogInputDevice(String key, PinInfo pinInfo) {
		return new VoodooSparkAnalogInputDevice(this, key, pinInfo);
	}

	@Override
	public AnalogOutputDeviceInterface createAnalogOutputDevice(String key, PinInfo pinInfo, float initialValue) {
		return new VoodooSparkAnalogOutputDevice(this, key, pinInfo, initialValue);
	}

	@Override
	public SpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect, int frequency,
			SpiClockMode spiClockMode, boolean lsbFirst) throws RuntimeIOException {
		throw new UnsupportedOperationException("SPI isn't supported with Voodoo Spark firmware");
	}

	@Override
	public I2CDeviceInterface createI2CDevice(String key, int controller, int address,
			I2CConstants.AddressSize addressSize) throws RuntimeIOException {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public SerialDeviceInterface createSerialDevice(String key, String deviceFile, int baud,
			SerialDevice.DataBits dataBits, SerialDevice.StopBits stopBits, SerialDevice.Parity parity,
			boolean readBlocking, int minReadChars, int readTimeoutMillis) throws RuntimeIOException {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	void setPinMode(int gpio, PinMode mode) {
		sendMessage(new PinModeMessage(gpio, mode));
	}

	boolean getValue(int gpio) {
		ResponseMessage response = sendMessage(new DigitalReadMessage(gpio));
		if (response == null) {
			return false;
		}
		// Validate GPIO returned matches that requested
		if (response.pinOrPort != gpio) {
			Logger.error("Returned GPIO ({}) doesn't match that requested ({})", Byte.valueOf(response.pinOrPort),
					Integer.valueOf(gpio));
		}
		return response.lsb != 0;
	}

	void setValue(int gpio, boolean value) {
		sendMessage(new DigitalWriteMessage(gpio, value));
	}

	int getAnalogValue(int gpio) {
		ResponseMessage response = sendMessage(new AnalogReadMessage(gpio));
		if (response == null) {
			return -1;
		}
		// Validate GPIO returned matches that requested
		if (response.pinOrPort != gpio) {
			Logger.error("Returned GPIO ({}) doesn't match that requested ({})", Byte.valueOf(response.pinOrPort),
					Integer.valueOf(gpio));
		}
		return (response.msb << 7) | response.lsb;
	}

	void setAnalogValue(int gpio, int value) {
		sendMessage(new AnalogWriteMessage(gpio, value));
	}

	void addReporting(int gpio, boolean analog) {
		sendMessage(new ReportingMessage((byte) gpio, analog));
	}

	void setSampleInterval(int intervalMs) {
		sendMessage(new SetSampleIntervalMessage(intervalMs));
	}

	void setInternalRgb(byte red, byte green, byte blue) {
		sendMessage(new InternalRgbMessage(red, green, blue));
	}

	private synchronized ResponseMessage sendMessage(Message message) {
		ResponseMessage rm = null;

		lock.lock();
		try {
			lastWriteFuture = messageChannel.writeAndFlush(message);
			lastWriteFuture.get();

			if (message.responseExpected) {
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
			long epoch_time = System.currentTimeMillis();

			Logger.info("Reporting message: {}", msg);

			// Notify the listeners for each GPIO in this port for which reporting has been
			// enabled
			for (int i = 0; i < 8; i++) {
				// Note can only get reports for GPIOs 0-7 and 10-17
				int gpio = msg.pinOrPort * 10 + i;

				// TODO Need to check that reporting has been enabled for this GPIO!

				PinInfo pin_info = getBoardPinInfo().getByGpioNumber(gpio);
				DeviceInterface device = getDevice(createPinKey(pin_info));
				if (device != null) {
					// What about analog events?!
					AbstractDigitalInputDevice input_device = (AbstractDigitalInputDevice) device;
					input_device.valueChanged(new DigitalInputEvent(gpio, epoch_time, 0, (msg.lsb & (1 << i)) != 0));
				}
			}
		} else {
			lock.lock();
			try {
				messageQueue.add(msg);
				condition.signalAll();
			} finally {
				lock.unlock();
			}
		}
	}

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
					+ ", lastHandshakeAt=" + lastHandshakeAt + ", deviceId=" + deviceId + ", productId=" + productId
					+ "]";
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

	static enum PinMode {
		DIGITAL_INPUT(0), DIGITAL_OUTPUT(1), ANALOG_INPUT(2), ANALOG_OUTPUT(3), // Note for PWM as well as true analog
																				// output
		SERVO(4), I2C(6);

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
			super(VoodooSparkDeviceFactory.PIN_MODE);
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
			super(VoodooSparkDeviceFactory.DIGITAL_WRITE);
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
			super(VoodooSparkDeviceFactory.ANALOG_WRITE);
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
			super(VoodooSparkDeviceFactory.DIGITAL_READ, true);
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

		public ReportingMessage(byte gpio, boolean analog) {
			super(REPORTING);
			this.gpio = gpio;
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
			super(VoodooSparkDeviceFactory.INTERNAL_RGB);
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

	public static class ParticlePhotonBoardInfo extends BoardInfo {
		public static final String MAKE = "Particle";
		public static final String MODEL = "Photon";

		public ParticlePhotonBoardInfo() {
			super(MAKE, MODEL, -1, MAKE.toLowerCase());
		}

		@Override
		public void initialisePins() {
			int pin = 1;
			// This pin can be used as an input or output
			// As an input, supply 3.6 to 5.5VDC to power the Photon
			// When the Photon is powered via the USB port, this pin will output a voltage
			// of approximately 4.8VDC
			addGeneralPinInfo(pin++, PinInfo.VCC_5V);
			addGeneralPinInfo(pin++, PinInfo.GROUND);
			int gpio_num = 19;
			addGpioPinInfo(gpio_num--, "UART TX", pin++, PinInfo.DIGITAL_IN_OUT_PWM); // GPIO 19
			addGpioPinInfo(gpio_num--, "UART RX", pin++, PinInfo.DIGITAL_IN_OUT_PWM); // GPIO 18
			// Active-high wakeup pin, wakes the module from sleep/standby modes
			// When not used as a WAKEUP, this pin can also be used as a digital GPIO, ADC
			// input or PWM
			// Note aka A7
			addGpioPinInfo(gpio_num--, "WKP", pin++, EnumSet.of(DeviceMode.ANALOG_INPUT, DeviceMode.DIGITAL_INPUT,
					DeviceMode.DIGITAL_OUTPUT, DeviceMode.PWM_OUTPUT)); // GPIO 17
			// 12-bit Digital-to-Analog (D/A) output (0-4095), and also a digital GPIO
			// DAC is used as DAC or DAC1 in software, and A3 is a second DAC output used as
			// DAC2 in software
			// Note aka A6
			addGpioPinInfo(gpio_num--, "DAC", pin++,
					EnumSet.of(DeviceMode.ANALOG_OUTPUT, DeviceMode.DIGITAL_INPUT, DeviceMode.DIGITAL_OUTPUT)); // GPIO
																												// 16
			// 12-bit Analog-to-Digital (A/D) inputs (0-4095), and also digital GPIOs
			// SPI1 MOSI
			addGpioPinInfo(gpio_num--, "A5", pin++, EnumSet.of(DeviceMode.ANALOG_INPUT, DeviceMode.DIGITAL_INPUT,
					DeviceMode.DIGITAL_OUTPUT, DeviceMode.PWM_OUTPUT)); // GPIO 15
			// SPI1 MISO
			addGpioPinInfo(gpio_num--, "A4", pin++, EnumSet.of(DeviceMode.ANALOG_INPUT, DeviceMode.DIGITAL_INPUT,
					DeviceMode.DIGITAL_OUTPUT, DeviceMode.PWM_OUTPUT)); // GPIO 14
			// SPI1 SCK
			addGpioPinInfo(gpio_num--, "A3", pin++,
					EnumSet.of(DeviceMode.ANALOG_INPUT, DeviceMode.DIGITAL_INPUT, DeviceMode.DIGITAL_OUTPUT)); // GPIO
																												// 13
			// SPI1 SS
			addGpioPinInfo(gpio_num--, "A2", pin++,
					EnumSet.of(DeviceMode.ANALOG_INPUT, DeviceMode.DIGITAL_INPUT, DeviceMode.DIGITAL_OUTPUT)); // GPIO
																												// 12
			addGpioPinInfo(gpio_num--, "A1", pin++, EnumSet.of(DeviceMode.ANALOG_INPUT, DeviceMode.DIGITAL_INPUT,
					DeviceMode.DIGITAL_OUTPUT, DeviceMode.PWM_OUTPUT)); // GPIO 11
			addGpioPinInfo(gpio_num--, "A0", pin++, EnumSet.of(DeviceMode.ANALOG_INPUT, DeviceMode.DIGITAL_INPUT,
					DeviceMode.DIGITAL_OUTPUT, DeviceMode.PWM_OUTPUT)); // GPIO 10
			// Digital only GPIO pins. D0-D3 may also be used as a PWM output
			gpio_num = 0;
			addGpioPinInfo(gpio_num++, "D0", pin++, PinInfo.DIGITAL_IN_OUT_PWM); // SDA
			addGpioPinInfo(gpio_num++, "D1", pin++, PinInfo.DIGITAL_IN_OUT_PWM); // SCL
			addGpioPinInfo(gpio_num++, "D2", pin++, PinInfo.DIGITAL_IN_OUT_PWM); // SPI3 MOSI
			addGpioPinInfo(gpio_num++, "D3", pin++, PinInfo.DIGITAL_IN_OUT_PWM); // SPI3 MISO
			addGpioPinInfo(gpio_num++, "D4", pin++, PinInfo.DIGITAL_IN_OUT); // SPI3 SCK
			addGpioPinInfo(gpio_num++, "D5", pin++, PinInfo.DIGITAL_IN_OUT); // SPI3 SS
			addGpioPinInfo(gpio_num++, "D6", pin++, PinInfo.DIGITAL_IN_OUT);
			// Onboard LED
			addGpioPinInfo(gpio_num++, "D7", pin++, PinInfo.DIGITAL_IN_OUT);
			addGeneralPinInfo(pin++, PinInfo.GROUND);
			// Supply to the internal RTC, backup registers and SRAM when 3V3 is not present
			addGeneralPinInfo(pin++, "VBAT");
			// Active-low reset input
			addGeneralPinInfo(pin++, "RST");
			addGeneralPinInfo(pin++, PinInfo.VCC_3V3);
		}
	}

	public static void main(String[] args) throws InterruptedException {
		try (VoodooSparkDeviceFactory df = new VoodooSparkDeviceFactory()) {
			int gpio = 7;
			int pin = 7;
			boolean value = false;
			try (GpioDigitalOutputDeviceInterface output = df.createDigitalOutputDevice("GPIO-" + gpio,
					new PinInfo("GPIO", "default", gpio, pin, "GPIO-" + gpio, PinInfo.DIGITAL_IN_OUT_PWM), value)) {
				for (int i = 0; i < 4; i++) {
					value = output.getValue();
					output.setValue(!value);
					Thread.sleep(500);
				}
			}

			gpio = 0;
			df.setPinMode(gpio, PinMode.DIGITAL_OUTPUT);
			// df.addReporting(gpio, false);
			// df.setSampleInterval(100);

			for (int i = 0; i < 5; i++) {
				df.setValue(gpio, true);
				Thread.sleep(500);
				df.setValue(gpio, false);
				Thread.sleep(500);
			}

			/*
			 * for (int red=0; red<255; red++) { df.setInternalRgb((byte) red, (byte) 0,
			 * (byte) 0); Thread.sleep(50); }
			 * 
			 * for (int green=0; green<255; green++) { df.setInternalRgb((byte) 0, (byte)
			 * green, (byte) 0); Thread.sleep(50); }
			 * 
			 * for (int blue=0; blue<255; blue++) { df.setInternalRgb((byte) 0, (byte) 0,
			 * (byte) blue); Thread.sleep(50); }
			 */

			analogTest(df, gpio);
			gpio = 13;
			analogTest(df, gpio);
			gpio = 16;
			analogTest(df, gpio);
		}
	}

	private static void analogTest(VoodooSparkDeviceFactory df, int gpio) throws InterruptedException {
		df.setPinMode(gpio, PinMode.ANALOG_OUTPUT);
		// df.addReporting(gpio, true);

		int max = gpio < 10 ? MAX_PWM_VALUE : MAX_ANALOG_VALUE;
		int step = (max + 1) / 256;

		int val = 0;
		df.setAnalogValue(gpio, val);
		System.out.println("Min. val = " + val);
		Thread.sleep(500);

		val = max / 2;
		df.setAnalogValue(gpio, val);
		System.out.println("Mid. val = " + val);
		Thread.sleep(500);

		val = max;
		df.setAnalogValue(gpio, val);
		System.out.println("Max. val = " + val);
		Thread.sleep(500);

		val = max / 2;
		df.setAnalogValue(gpio, val);
		System.out.println("Mid. val = " + val);
		Thread.sleep(500);

		val = 0;
		df.setAnalogValue(gpio, val);
		System.out.println("Min. val = " + val);
		Thread.sleep(500);

		for (int i = 0; i < max + 1; i += step) {
			df.setAnalogValue(gpio, i);
			Thread.sleep(20);
		}
		for (int i = 0; i < max + 1; i += step) {
			df.setAnalogValue(gpio, max - i);
			Thread.sleep(20);
		}
	}
}
