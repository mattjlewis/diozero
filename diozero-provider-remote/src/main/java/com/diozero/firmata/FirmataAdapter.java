package com.diozero.firmata;

/*-
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Remote Provider
 * Filename:     FirmataAdapter.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 mattjlewis
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
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.pmw.tinylog.Logger;

public class FirmataAdapter implements FirmataProtocol, Runnable, Closeable {
	private static final int I2C_NO_REGISTER = 0;
	private static final int NOT_SET = -1;
	private static final byte ANALOG_NOT_SUPPORTED = 127;

	private FirmataEventListener eventListener;
	private AtomicBoolean running;
	private InputStream is;
	private OutputStream os;
	private Queue<ResponseMessage> responseQueue;
	private Lock lock;
	private Condition condition;
	private ExecutorService executor;
	private Future<?> future;
	private FirmwareDetails firmware;
	private Map<Integer, Pin> pins;
	private List<List<PinCapability>> boardCapabilities;
	private Map<Integer, Integer> adcToGpioMapping;

	public FirmataAdapter(FirmataEventListener eventListener) {
		this.eventListener = eventListener;

		running = new AtomicBoolean(false);
		responseQueue = new ConcurrentLinkedQueue<>();
		lock = new ReentrantLock();
		condition = lock.newCondition();
		pins = new ConcurrentHashMap<>();
		adcToGpioMapping = new ConcurrentHashMap<>();

		executor = Executors.newSingleThreadExecutor();
	}

	protected void connect(InputStream input, OutputStream output) throws IOException {
		this.is = input;
		this.os = output;

		future = executor.submit(this);

		initialiseBoard();
	}

	public int getMax(int gpio, PinMode mode) {
		Pin pin = pins.get(Integer.valueOf(gpio));
		if (pin == null) {
			return 1;
		}
		return pin.getMax(mode);
	}

	public void initialiseBoard() throws IOException {
		firmware = getFirmwareInternal();

		CapabilityResponse board_capabilities = sendMessage(new byte[] { START_SYSEX, CAPABILITY_QUERY, END_SYSEX },
				CapabilityResponse.class);

		// Cache the pin capabilities
		int gpio = 0;
		for (List<PinCapability> pin_capabilities : board_capabilities.getCapabilities()) {
			Pin pin = new Pin(gpio, pin_capabilities);
			pins.put(Integer.valueOf(gpio), pin);
			gpio++;
		}

		boardCapabilities = board_capabilities.getCapabilities();

		AnalogMappingResponse mapping = getAnalogMapping();
		gpio = 0;
		for (byte channel : mapping.getChannels()) {
			if (channel != ANALOG_NOT_SUPPORTED) {
				adcToGpioMapping.put(Integer.valueOf(channel), Integer.valueOf(gpio));
			}
			gpio++;
		}
	}

	public ProtocolVersion getProtocolVersion() throws IOException {
		return sendMessage(new byte[] { PROTOCOL_VERSION }, ProtocolVersion.class);
	}

	public FirmwareDetails getFirmware() {
		return firmware;
	}

	private FirmwareDetails getFirmwareInternal() throws IOException {
		return sendMessage(new byte[] { START_SYSEX, REPORT_FIRMWARE, END_SYSEX }, FirmwareDetails.class);
	}

	public List<List<PinCapability>> getBoardCapabilities() {
		return boardCapabilities;
	}

	public PinState getPinState(int gpio) throws IOException {
		PinState pin_state = sendMessage(new byte[] { START_SYSEX, PIN_STATE_QUERY, (byte) gpio, END_SYSEX },
				PinState.class);

		// Update the cached pin mode and value
		Pin pin = pins.get(Integer.valueOf(gpio));
		if (pin == null) {
			Logger.warn("No such GPIO #{}", Integer.valueOf(gpio));
		} else {
			pin.setMode(pin_state.getMode());
			// For output modes, the state is any value that has been previously written to
			// the pin
			if (pin_state.getMode().isOutput()) {
				pin.setValue(pin_state.getState());
			}
		}

		return pin_state;
	}

	public void setSamplingInterval(int intervalMs) throws IOException {
		byte[] lsb_msb = convertToLsbMsb(intervalMs);
		sendMessage(new byte[] { START_SYSEX, SAMPLING_INTERVAL, (byte) (intervalMs & 0x7f), lsb_msb[0], lsb_msb[1],
				END_SYSEX });
	}

	// Note enables / disables reporting for all GPIOs in this bank of GPIOs
	public void enableDigitalReporting(int gpio, boolean enabled) throws IOException {
		Logger.debug("Enable digital reporting: " + enabled);
		sendMessage(new byte[] { (byte) (REPORT_DIGITAL_PORT | (gpio >> 3)), (byte) gpio, (byte) (enabled ? 1 : 0) });
	}

	public PinMode getPinMode(int gpio) {
		Pin pin = pins.get(Integer.valueOf(gpio));
		if (pin == null) {
			return PinMode.UNKNOWN;
		}
		return pin.getMode();
	}

	public void setPinMode(int gpio, PinMode pinMode) throws IOException {
		Logger.debug("setPinMode(" + gpio + ", " + pinMode + ")");
		sendMessage(new byte[] { SET_PIN_MODE, (byte) gpio, (byte) pinMode.ordinal() });

		// Update the cached pin mode
		Pin pin = pins.get(Integer.valueOf(gpio));
		if (pin != null) {
			pin.setMode(pinMode);
		}
	}

	public void setDigitalValues(int port, byte values) throws IOException {
		Logger.debug("setValues({}, {})", Integer.valueOf(port), Integer.valueOf(values));
		byte[] lsb_msb = convertToLsbMsb(values);
		sendMessage(new byte[] { (byte) (DIGITAL_IO_START | (port & 0x0f)), lsb_msb[0], lsb_msb[1] });

		// Update the cached values
		for (int i = 0; i < 8; i++) {
			int gpio = 8 * port + i;
			Pin pin = pins.get(Integer.valueOf(gpio));
			if (pin != null) {
				pin.setValue((values & (1 << i)) != 0);
			}
		}
	}

	public boolean getDigitalValue(int gpio) {
		Pin pin = pins.get(Integer.valueOf(gpio));
		if (pin == null) {
			Logger.warn("No such GPIO #{}", Integer.valueOf(gpio));
			return false;
		}
		return pin.getValue() != 0;
	}

	public void setDigitalValue(int gpio, boolean value) throws IOException {
		sendMessage(new byte[] { SET_DIGITAL_PIN_VALUE, (byte) gpio, (byte) (value ? 1 : 0) });

		// Update the cached value
		Pin pin = pins.get(Integer.valueOf(gpio));
		if (pin != null) {
			pin.setValue(value);
		}
	}

	public int getValue(int gpio) {
		Pin pin = pins.get(Integer.valueOf(gpio));
		if (pin == null) {
			Logger.warn("No such GPIO #{}", Integer.valueOf(gpio));
			return NOT_SET;
		}
		return pin.getValue();
	}

	public void setValue(int gpio, int value) throws IOException {
		// Non-extended analog accommodates 16 ports (E0-Ef), with a max value of 16384
		// (2^14)
		if (gpio < 16 && value < 16384) {
			byte[] lsb_msb = convertToLsbMsb(value);
			sendMessage(new byte[] { (byte) (ANALOG_IO_START | gpio), lsb_msb[0], lsb_msb[1] });
		} else {
			byte[] bytes = convertToBytes(value);
			byte[] data = new byte[4 + bytes.length];
			data[0] = START_SYSEX;
			data[1] = EXTENDED_ANALOG;
			data[2] = (byte) gpio;
			System.arraycopy(bytes, 0, data, 3, bytes.length);
			data[data.length - 1] = END_SYSEX;
			sendMessage(data);
		}
	}

	private AnalogMappingResponse getAnalogMapping() throws IOException {
		return sendMessage(new byte[] { START_SYSEX, ANALOG_MAPPING_QUERY, END_SYSEX }, AnalogMappingResponse.class);
	}

	public I2CResponse i2cRead(int slaveAddress, boolean autoRestart, boolean addressSize10Bit, int length)
			throws IOException {
		byte[] data = new byte[7];
		int index = 0;
		data[index++] = START_SYSEX;
		data[index++] = I2C_REQUEST;
		byte[] address_lsb_msb = convertToI2CSlaveAddressLsbMsb(slaveAddress, autoRestart, addressSize10Bit,
				I2CMode.READ_ONCE);
		data[index++] = address_lsb_msb[0];
		data[index++] = address_lsb_msb[1];
		byte[] length_lsb_msb = convertToLsbMsb(length);
		data[index++] = length_lsb_msb[0];
		data[index++] = length_lsb_msb[1];
		data[index++] = END_SYSEX;

		return sendMessage(data, I2CResponse.class);
	}

	public void i2cWrite(int slaveAddress, boolean autoRestart, boolean addressSize10Bit, byte[] data)
			throws IOException {
		i2cWriteData(slaveAddress, autoRestart, addressSize10Bit, I2C_NO_REGISTER, data);
	}

	public I2CResponse i2cReadData(int slaveAddress, boolean autoRestart, boolean addressSize10Bit, int register,
			int length) throws IOException {
		byte[] data = new byte[9];
		int index = 0;
		data[index++] = START_SYSEX;
		data[index++] = I2C_REQUEST;
		byte[] address_lsb_msb = convertToI2CSlaveAddressLsbMsb(slaveAddress, autoRestart, addressSize10Bit,
				I2CMode.READ_ONCE);
		data[index++] = address_lsb_msb[0];
		data[index++] = address_lsb_msb[1];
		byte[] register_lsb_msb = convertToLsbMsb(register);
		data[index++] = register_lsb_msb[0];
		data[index++] = register_lsb_msb[1];
		byte[] length_lsb_msb = convertToLsbMsb(length);
		data[index++] = length_lsb_msb[0];
		data[index++] = length_lsb_msb[1];
		data[index++] = END_SYSEX;

		return sendMessage(data, I2CResponse.class);
	}

	public void i2cWriteData(int slaveAddress, boolean autoRestart, boolean addressSize10Bit, int register, byte[] data)
			throws IOException {
		byte[] buffer = new byte[7 + 2 * data.length];
		int index = 0;
		buffer[index++] = START_SYSEX;
		buffer[index++] = I2C_REQUEST;
		byte[] address_lsb_msb = convertToI2CSlaveAddressLsbMsb(slaveAddress, autoRestart, addressSize10Bit,
				I2CMode.WRITE);
		buffer[index++] = address_lsb_msb[0];
		buffer[index++] = address_lsb_msb[1];
		byte[] register_lsb_msb = convertToLsbMsb(register);
		data[index++] = register_lsb_msb[0];
		data[index++] = register_lsb_msb[1];
		for (int i = 0; i < data.length; i++) {
			byte[] data_lsb_msb = convertToLsbMsb(data[i]);
			buffer[index++] = data_lsb_msb[0];
			buffer[index++] = data_lsb_msb[1];
		}
		buffer[index++] = END_SYSEX;

		sendMessage(buffer);
	}

	private void sendMessage(byte[] request) throws IOException {
		sendMessage(request, null);
	}

	@SuppressWarnings("unchecked")
	private <T extends ResponseMessage> T sendMessage(byte[] request, Class<T> responseClass) throws IOException {
		ResponseMessage response = null;

		lock.lock();
		try {
			os.write(request);

			if (responseClass != null) {
				// Wait for the response message
				condition.await();
				response = responseQueue.remove();
			}
		} catch (InterruptedException e) {
			// Ignore
		} finally {
			lock.unlock();
		}

		return (T) response;
	}

	@Override
	public void close() {
		lock.lock();
		try {
			condition.signal();
		} finally {
			lock.unlock();
		}

		running.compareAndSet(true, false);
		if (!future.cancel(true)) {
			Logger.warn("Task could not be cancelled");
		}

		if (is != null) {
			try {
				is.close();
				is = null;
			} catch (IOException e) {
			}
		}
		if (os != null) {
			try {
				os.close();
				os = null;
			} catch (IOException e) {
			}
		}

		executor.shutdown();
	}

	@Override
	public void run() {
		if (!running.compareAndSet(false, true)) {
			Logger.warn("Already running?");
			return;
		}

		try {
			while (running.get()) {
				int i = is.read();
				if (i == -1) {
					running.compareAndSet(true, false);
					break;
				}

				long nano_time = System.nanoTime();
				long epoch_time = System.currentTimeMillis();

				byte b = (byte) i;
				if (b == START_SYSEX) {
					lock.lock();
					try {
						responseQueue.add(readSysEx());
						condition.signal();
					} finally {
						lock.unlock();
					}
				} else if (b == PROTOCOL_VERSION) {
					lock.lock();
					try {
						responseQueue.add(readVersionResponse());
						condition.signal();
					} finally {
						lock.unlock();
					}
				} else if (b >= DIGITAL_IO_START && b <= DIGITAL_IO_END) {
					processDigitalResponse(readDataResponse(b - DIGITAL_IO_START), epoch_time, nano_time);
				} else if (b >= ANALOG_IO_START && b <= ANALOG_IO_END) {
					processAnalogResponse(readDataResponse(b - ANALOG_IO_START), epoch_time, nano_time);
				} else {
					Logger.warn("Unrecognised response: " + b);
				}
			}
		} catch (IOException e) {
			running.compareAndSet(true, false);
			Logger.debug("I/O error: {}", e);
		}

		Logger.debug("Thread: done");
	}

	private void processDigitalResponse(DataResponse response, long epochTime, long nanoTime) {
		Logger.debug(response);
		int port = response.getPort();
		int value = response.getValue();
		// Update the cached values
		for (int x = 0; x < 8; x++) {
			int gpio = 8 * port + x;
			Pin pin = pins.get(Integer.valueOf(gpio));
			if (pin != null) {
				int val = value & (1 << x);
				pin.setValue(val);
				eventListener.event(FirmataEventListener.EventType.DIGITAL, gpio, val, epochTime, nanoTime);
			}
		}
	}

	private void processAnalogResponse(DataResponse response, long eventTime, long nanoTime) {
		// Update the cached value
		int adc = response.getPort();
		Integer gpio = adcToGpioMapping.get(Integer.valueOf(adc));
		if (gpio != null) {
			Pin pin = pins.get(Integer.valueOf(gpio.intValue()));
			if (pin != null) {
				pin.setValue(response.getValue());
				eventListener.event(FirmataEventListener.EventType.ANALOG, gpio.intValue(), response.getValue(),
						eventTime, nanoTime);
			}
		}
	}

	private SysExResponse readSysEx() throws IOException {
		byte sysex_cmd = readByte();
		// long start = System.currentTimeMillis();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		while (true) {
			byte b = readByte();
			if (b == END_SYSEX) {
				break;
			}
			baos.write(b);
		}
		ByteBuffer buffer = ByteBuffer.wrap(baos.toByteArray());
		// long duration = System.currentTimeMillis() - start;
		// Logger.debug("Took " + duration + "ms to read the SysEx message");

		SysExResponse response = null;
		switch (sysex_cmd) {
		case REPORT_FIRMWARE:
			byte major = buffer.get();
			byte minor = buffer.get();

			byte[] name_buffer = new byte[buffer.remaining()];
			buffer.get(name_buffer);
			String name = new String(name_buffer, Charset.forName("UTF-16LE"));

			response = new FirmwareDetails(major, minor, name);
			break;
		case CAPABILITY_RESPONSE:
			List<List<PinCapability>> capabilities = new ArrayList<>();
			List<PinCapability> pin_capabilities = new ArrayList<>();
			while (buffer.remaining() > 0) {
				while (true) {
					byte b = buffer.get();
					if (b == 0x7f) {
						break;
					}
					PinMode mode = PinMode.valueOf(b);
					byte resolution = buffer.get();
					pin_capabilities.add(new PinCapability(mode, resolution));
				}

				capabilities.add(Collections.unmodifiableList(pin_capabilities));
				pin_capabilities = new ArrayList<>();
			}
			response = new CapabilityResponse(capabilities);
			break;
		case PIN_STATE_RESPONSE:
			byte pin = buffer.get();
			PinMode pin_mode = PinMode.valueOf(buffer.get());
			byte[] pin_state = new byte[buffer.remaining()];
			buffer.get(pin_state);
			response = new PinState(pin, pin_mode, convertToValue(pin_state));
			break;
		case ANALOG_MAPPING_RESPONSE:
			byte[] channels = new byte[buffer.remaining()];
			buffer.get(channels);
			response = new AnalogMappingResponse(channels);
			break;
		case I2C_REPLY:
			int slave_address = convertToValue(buffer.get(), buffer.get());
			int register = convertToValue(buffer.get(), buffer.get());
			byte[] data = new byte[buffer.remaining() / 2];
			for (int i = 0; i < data.length; i++) {
				data[i] = (byte) convertToValue(buffer.get(), buffer.get());
			}
			buffer.get(data);
			response = new I2CResponse(slave_address, register, data);
			break;
		default:
			Logger.warn("Unhandled sysex command: 0x{}", Integer.toHexString(sysex_cmd));
		}

		return response;
	}

	private ProtocolVersion readVersionResponse() throws IOException {
		return new ProtocolVersion(readByte(), readByte());
	}

	private DataResponse readDataResponse(int port) throws IOException {
		return new DataResponse(port, readShort());
	}

	private byte readByte() throws IOException {
		int i = is.read();
		if (i == -1) {
			throw new IOException("End of stream unexpected");
		}
		return (byte) i;
	}

	private int readShort() throws IOException {
		// LSB (bits 0-6), MSB(bits 7-13)
		return convertToValue(readByte(), readByte());
		// return ((msb & 0x7f) << 7) | (lsb & 0x7f);
	}

	static byte[] convertToLsbMsb(int value) {
		return new byte[] { (byte) (value & 0x7f), (byte) ((value >> 7) & 0x7f) };
	}

	static byte[] convertToBytes(int value) {
		int num_bytes;
		if (value < 128 || value >= 268435456) { // 2^7 or greater than max val (2^28)
			num_bytes = 1;
		} else if (value < 16384) { // 2^14
			num_bytes = 2;
		} else if (value < 2097152) { // 2^21
			num_bytes = 3;
		} else {
			num_bytes = 4;
		}
		byte[] bytes = new byte[num_bytes];
		for (int i = 0; i < num_bytes; i++) {
			bytes[i] = (byte) ((value >> (i * 7)) & 0x7f);
		}
		return bytes;
	}

	static int convertToValue(byte... values) {
		int value = 0;
		for (int i = 0; i < values.length; i++) {
			value |= ((values[i] & 0x7f) << (i * 7));
		}
		return value;
	}

	/*
	 * slave address (MSB) + read/write and address mode bits
	 * {bit 7: always 0}
	 * {bit 6: auto restart transmission, 0 = stop (default), 1 = restart}
	 * {bit 5: address mode, 1 = 10-bit mode}
	 * {bits 4-3: read/write, 00 = write, 01 = read once, 10 = read continuously, 11 = stop reading}
	 * {bits 2-0: slave address MSB in 10-bit mode, not used in 7-bit mode}
	 */
	static byte[] convertToI2CSlaveAddressLsbMsb(int slaveAddress, boolean autoRestart, boolean addressSize10Bit,
			I2CMode mode) {
		byte[] lsb_msb = convertToLsbMsb(slaveAddress);

		lsb_msb[1] &= 0x7;
		if (autoRestart) {
			lsb_msb[1] |= 0x40;
		}
		if (addressSize10Bit) {
			lsb_msb[1] |= 0x20;
		}
		lsb_msb[1] |= (mode.ordinal() << 3);

		return lsb_msb;
	}

	static class ResponseMessage {
	}

	static class SysExResponse extends ResponseMessage {
	}

	public static class FirmwareDetails extends SysExResponse {
		private byte major;
		private byte minor;
		private String name;

		FirmwareDetails(byte major, byte minor, String name) {
			this.major = major;
			this.minor = minor;
			this.name = name;
		}

		public byte getMajor() {
			return major;
		}

		public byte getMinor() {
			return minor;
		}

		public String getName() {
			return name;
		}

		@Override
		public String toString() {
			return "FirmwareResponse [major=" + major + ", minor=" + minor + ", name=" + name + "]";
		}
	}

	static class CapabilityResponse extends SysExResponse {
		private List<List<PinCapability>> capabilities;

		CapabilityResponse(List<List<PinCapability>> capabilities) {
			this.capabilities = capabilities;
		}

		public List<List<PinCapability>> getCapabilities() {
			return capabilities;
		}

		@Override
		public String toString() {
			return "CapabilityResponse [capabilities=" + capabilities + "]";
		}
	}

	/**
	 * The pin state is any data written to the pin (it is important to note that
	 * pin state != pin value). For output modes (digital output, PWM, and Servo),
	 * the state is any value that has been previously written to the pin. For input
	 * modes, typically the state is zero. However, for digital inputs, the state is
	 * the status of the pull-up resistor which is 1 if enabled, 0 if disabled.
	 * 
	 * The pin state query can also be used as a verification after sending pin
	 * modes or data messages.
	 */
	static class PinState extends SysExResponse {
		private byte pin;
		private PinMode mode;
		private int state;

		public PinState(byte pin, PinMode mode, int state) {
			this.pin = pin;
			this.mode = mode;
			this.state = state;
		}

		public byte getPin() {
			return pin;
		}

		public PinMode getMode() {
			return mode;
		}

		public int getState() {
			return state;
		}

		@Override
		public String toString() {
			return "PinStateResponse [pin=" + pin + ", mode=" + mode + ", state=" + state + "]";
		}
	}

	static class ProtocolVersion extends ResponseMessage {
		private byte major;
		private byte minor;

		ProtocolVersion(byte major, byte minor) {
			this.major = major;
			this.minor = minor;
		}

		public byte getMajor() {
			return major;
		}

		public byte getMinor() {
			return minor;
		}

		@Override
		public String toString() {
			return "VersionResponse [major=" + major + ", minor=" + minor + "]";
		}
	}

	static class DataResponse extends ResponseMessage {
		/**
		 * For analog responses port is the relative analog pin number, for digital it
		 * is for a bank of 8 GPIOs.
		 */
		private int port;
		private int value;

		DataResponse(int port, int value) {
			this.port = port;
			// LSB (pins 0-6 bit-mask), MSB (pin 7 bit-mask)
			this.value = value;
		}

		public int getPort() {
			return port;
		}

		public int getValue() {
			return value;
		}

		@Override
		public String toString() {
			return String.format("DataResponse [port=%d, value=0x%x]", Integer.valueOf(port), Integer.valueOf(value));
		}
	}

	static class AnalogMappingResponse extends SysExResponse {
		private byte[] channels;

		public AnalogMappingResponse(byte[] channels) {
			this.channels = channels;
		}

		public byte[] getChannels() {
			return channels;
		}

		@Override
		public String toString() {
			return "AnalogMappingResponse [channels=" + Arrays.toString(channels) + "]";
		}
	}

	static class Pin {
		private int gpio;
		// List of pin mode and resolution pairs
		private List<PinCapability> capabilities;
		private PinMode mode = PinMode.UNKNOWN;
		private int value;
		private List<PinMode> modes;

		public Pin(int gpio, List<PinCapability> capabilities) {
			this.gpio = gpio;
			this.capabilities = capabilities;
			modes = new ArrayList<>();
			capabilities.forEach(pc -> modes.add(pc.getMode()));
		}

		public int getGpio() {
			return gpio;
		}

		public List<PinCapability> getCapabilities() {
			return capabilities;
		}

		public boolean isSupported(PinMode pinMode) {
			return modes.contains(pinMode);
		}

		public int getResolution(PinMode pinMode) {
			if (modes.contains(pinMode)) {
				for (PinCapability pc : capabilities) {
					if (pc.getMode() == pinMode) {
						return pc.getResolution();
					}
				}
			}

			return 1;
		}

		public int getMax(PinMode pinMode) {
			if (modes.contains(pinMode)) {
				for (PinCapability pc : capabilities) {
					if (pc.getMode() == pinMode) {
						return pc.getMax();
					}
				}
			}

			return 1;
		}

		public PinMode getMode() {
			return mode;
		}

		public void setMode(PinMode mode) {
			this.mode = mode;
		}

		public int getValue() {
			return value;
		}

		public void setValue(int value) {
			this.value = value;
		}

		public void setValue(boolean value) {
			this.value = value ? 1 : 0;
		}
	}

	static enum I2CMode {
		WRITE, READ_ONCE, READ_CONTINUOUSLY, STOP_READING;
	}

	public static class I2CResponse extends SysExResponse {
		private int slaveAddress;
		private int register;
		private byte[] data;

		public I2CResponse(int slaveAddress, int register, byte[] data) {
			this.slaveAddress = slaveAddress;
			this.register = register;
			this.data = data;
		}

		public int getSlaveAddress() {
			return slaveAddress;
		}

		public int getRegister() {
			return register;
		}

		public byte[] getData() {
			return data;
		}
	}
}
