package com.diozero.internal.provider.firmata.adapter;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Firmata
 * Filename:     FirmataAdapter.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2023 diozero
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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.tinylog.Logger;

import com.diozero.api.RuntimeIOException;
import com.diozero.util.DiozeroScheduler;

public class FirmataAdapter implements FirmataProtocol, Runnable, AutoCloseable {
	private static final int I2C_NO_REGISTER = 0;
	private static final int NOT_SET = -1;
	private static final byte ANALOG_NOT_SUPPORTED = 127;

	private FirmataTransport transport;
	private FirmataEventListener eventListener;
	private AtomicBoolean running;
	private AtomicBoolean inShutdown;
	private BlockingQueue<ResponseMessage> responseQueue;
	private Future<?> future;
	private FirmwareDetails firmware;
	private Map<Integer, Pin> pins;
	private List<Set<PinCapability>> boardCapabilities;
	private Map<Integer, Integer> adcToPinNumberMapping;
	private Map<Byte, Boolean> taskIds;

	public FirmataAdapter(FirmataTransport transport, FirmataEventListener eventListener) {
		this.transport = transport;
		this.eventListener = eventListener;

		running = new AtomicBoolean(false);
		inShutdown = new AtomicBoolean(false);
		responseQueue = new LinkedBlockingQueue<>();
		pins = new ConcurrentHashMap<>();
		adcToPinNumberMapping = new ConcurrentHashMap<>();
		taskIds = new ConcurrentHashMap<>();
	}

	public final void start() {
		// Throw away any pending data available to read
		while (transport.bytesAvailable() > 0) {
			transport.readByte();
		}

		future = DiozeroScheduler.getNonDaemonInstance().submit(this);

		initialiseBoard();
	}

	@Override
	public void close() {
		Logger.trace("closing...");

		inShutdown.set(true);

		// First try to safely interrupt the serial device read by sending a controlled
		// message with an invalid pin mode and wait for the expected "Unknown pin mode"
		// STRING_DATA response
		Logger.trace("Sending invalid sysex command...");
		StringDataResponse response = sendMessage(new byte[] { START_SYSEX, (byte) 0x59, END_SYSEX },
				StringDataResponse.class);
		Logger.debug("Invalid sysex command response: '{}'", response.getValue());

		// Stop the thread that is reading responses (in particular the inputStream.read
		// call when used with blocking I/O)
		running.compareAndSet(true, false);
		if (future != null && !future.isCancelled() && !future.isDone()) {
			Logger.debug("future.isCancelled: {}, future.isDone: {}", Boolean.valueOf(future.isCancelled()),
					Boolean.valueOf(future.isDone()));
			if (!future.cancel(true)) {
				Logger.warn("Task could not be cancelled");
			}
			Logger.trace("future.isCancelled: {}, future.isDone: {}", Boolean.valueOf(future.isCancelled()),
					Boolean.valueOf(future.isDone()));
		}

		inShutdown.set(false);

		transport.close();

		Logger.trace("closed.");
	}

	public void systemReset() {
		Logger.debug("systemReset");
		sendMessage(new byte[] { SYSTEM_RESET });
	}

	public int getMax(int gpio, PinMode mode) {
		Pin pin = pins.get(Integer.valueOf(gpio));
		if (pin == null) {
			return 1;
		}
		return pin.getMax(mode);
	}

	public void initialiseBoard() {
		Logger.debug("initialiseBoard()");

		/*-
		System.out.println("initialiseBoard::Getting protocol version ...");
		ProtocolVersion p_version = getProtocolVersion();
		System.out.println("initialiseBoard::Got protocol version " + p_version);
		 */

		firmware = getFirmwareInternal();

		try {
			ReportFeaturesResponse features = sendMessage(new byte[] { START_SYSEX, REPORT_FEATURES, END_SYSEX },
					ReportFeaturesResponse.class);
			Logger.debug("features: {}", features);
		} catch (FirmataErrorMessage e) {
			Logger.info("Report Features not supported: {}", e);
		}

		CapabilityResponse board_capabilities = sendMessage(new byte[] { START_SYSEX, CAPABILITY_QUERY, END_SYSEX },
				CapabilityResponse.class);

		// Cache the pin capabilities
		boardCapabilities = board_capabilities.getCapabilities();
		int pin_num = 0;
		for (Set<PinCapability> pin_capabilities : boardCapabilities) {
			Pin pin = new Pin(pin_num, pin_capabilities);
			pins.put(Integer.valueOf(pin_num), pin);
			pin_num++;
		}

		AnalogMappingResponse mapping = getAnalogMapping();
		pin_num = 0;
		for (byte channel : mapping.getChannels()) {
			if (channel != ANALOG_NOT_SUPPORTED) {
				adcToPinNumberMapping.put(Integer.valueOf(channel), Integer.valueOf(pin_num));
			}
			pin_num++;
		}
	}

	public FirmwareDetails getFirmware() {
		return firmware;
	}

	public ProtocolVersionResponse getProtocolVersion() {
		return sendMessage(new byte[] { PROTOCOL_VERSION }, ProtocolVersionResponse.class);
	}

	private FirmwareDetails getFirmwareInternal() {
		return sendMessage(new byte[] { START_SYSEX, REPORT_FIRMWARE, END_SYSEX }, FirmwareDetails.class);
	}

	public List<Set<PinCapability>> getBoardCapabilities() {
		return boardCapabilities;
	}

	public void refreshPinState(int gpio) {
		PinStateResponse pin_state = sendMessage(new byte[] { START_SYSEX, PIN_STATE_QUERY, (byte) gpio, END_SYSEX },
				PinStateResponse.class);

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
	}

	public void setSamplingInterval(int intervalMs) {
		sendMessage(FirmataProtocol.createSetSamplingIntervalMessage(intervalMs));
	}

	public void enableAnalogReporting(int adcNum, boolean enabled) {
		Logger.debug("enableAnalogReporting({}, {})", Integer.valueOf(adcNum), Boolean.valueOf(enabled));
		sendMessage(FirmataProtocol.createEnableAnalogReportingMessage(adcNum, enabled));
	}

	// Note enables / disables reporting for all GPIOs in this bank of GPIOs
	public void enableDigitalReporting(int gpio, boolean enabled) {
		Logger.debug("enableDigitalReporting({}, {})", Integer.valueOf(gpio), Boolean.valueOf(enabled));
		sendMessage(FirmataProtocol.createEnableDigitalReportingMessage(gpio, enabled));
	}

	public PinMode getPinMode(int gpio) {
		Pin pin = pins.get(Integer.valueOf(gpio));
		if (pin == null) {
			return PinMode.UNKNOWN;
		}
		return pin.getMode();
	}

	public void setPinMode(int gpio, PinMode pinMode) {
		Logger.trace("setPinMode({}, {})", Integer.valueOf(gpio), pinMode);
		sendMessage(FirmataProtocol.createSetPinModeMessage((byte) gpio, pinMode));

		// Update the cached pin mode
		Pin pin = pins.get(Integer.valueOf(gpio));
		if (pin != null) {
			pin.setMode(pinMode);
		}
	}

	public void setDigitalValues(int port, byte values) {
		sendMessage(FirmataProtocol.createSetDigitalValuesMessage(port, values));

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

	public void setDigitalValue(int gpio, boolean value) {
		sendMessage(FirmataProtocol.createSetDigitalValueMessage(gpio, value));

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

	public void setValue(int gpio, int value) {
		Pin pin = pins.get(Integer.valueOf(gpio));
		if (pin == null) {
			Logger.warn("No such GPIO #{}", Integer.valueOf(gpio));
			return;
		}

		sendMessage(FirmataProtocol.createSetValueMessage(gpio, value));

		// Update the cached value
		pin.setValue(value);
	}

	public AnalogMappingResponse getAnalogMapping() throws RuntimeIOException {
		try {
			return sendMessage(new byte[] { START_SYSEX, ANALOG_MAPPING_QUERY, END_SYSEX },
					AnalogMappingResponse.class);
		} catch (FirmataErrorMessage e) {
			return new AnalogMappingResponse(new byte[] {});
		}
	}

	/**
	 * minPulse and maxPulse are both 14-bit unsigned integers.
	 *
	 * @param gpio     the gpio to configure
	 * @param minPulse new minimum pulse width
	 * @param maxPulse new maximum pulse width
	 */
	public void servoConfig(int gpio, int minPulse, int maxPulse) {
		sendMessage(FirmataProtocol.createServoConfigMessage(gpio, minPulse, maxPulse));
	}

	/**
	 * Configures I2C delay. Note that the delay is set for the entire Firmata
	 * device and not per connected I2C device.
	 *
	 * @param delayMs delay from when a register is written to and when data can be
	 *                read from that register (optional)
	 */
	public void i2cConfig(int delayMs) {
		Logger.debug("i2cConfig({})", Integer.valueOf(delayMs));
		if (delayMs < 0) {
			throw new IllegalArgumentException("Delay must be >= 0 microseconds");
		}
		if (delayMs > 255) {
			throw new IllegalArgumentException("Delay must be < 256 microseconds");
		}
		byte delay_lsb = (byte) (delayMs & 0x7F);
		byte delay_msb = 0;
		if (delayMs > 128) {
			delay_msb = 1;
		}

		byte[] data = new byte[5];
		int index = 0;
		data[index++] = START_SYSEX;
		data[index++] = I2C_CONFIG;
		data[index++] = delay_lsb;
		data[index++] = delay_msb;
		data[index++] = END_SYSEX;

		sendMessage(data);
	}

	public I2CResponse i2cRead(int slaveAddress, boolean autoRestart, boolean addressSize10Bit, int length)
			throws RuntimeIOException {
		Logger.trace("i2cRead({}, {}, {}, {})", Integer.valueOf(slaveAddress), Boolean.valueOf(autoRestart),
				Boolean.valueOf(addressSize10Bit), Integer.valueOf(length));
		byte[] data = new byte[7];
		int index = 0;
		data[index++] = START_SYSEX;
		data[index++] = I2C_REQUEST;
		byte[] address_lsb_msb = convertToI2CSlaveAddressLsbMsb(slaveAddress, autoRestart, addressSize10Bit,
				I2CMode.READ_ONCE);
		data[index++] = address_lsb_msb[0];
		data[index++] = address_lsb_msb[1];
		byte[] length_lsb_msb = FirmataProtocol.convertToLsbMsb(length);
		data[index++] = length_lsb_msb[0];
		data[index++] = length_lsb_msb[1];
		data[index++] = END_SYSEX;

		return sendMessage(data, I2CResponse.class);
	}

	public void i2cWrite(int slaveAddress, boolean autoRestart, boolean addressSize10Bit, byte[] data)
			throws RuntimeIOException {
		Logger.debug("i2cWrite({}, {}, {}, {} bytes)", Integer.valueOf(slaveAddress), Boolean.valueOf(autoRestart),
				Boolean.valueOf(addressSize10Bit), Integer.valueOf(data.length));
		i2cWriteData(slaveAddress, autoRestart, addressSize10Bit, I2C_NO_REGISTER, data);
	}

	public I2CResponse i2cReadData(int slaveAddress, boolean autoRestart, boolean addressSize10Bit, int register,
			int length) {
		Logger.trace("i2cReadData({}, {}, {}, {}, {})", Integer.valueOf(slaveAddress), Boolean.valueOf(autoRestart),
				Boolean.valueOf(addressSize10Bit), Integer.valueOf(register), Integer.valueOf(length));
		byte[] data = new byte[9];
		int index = 0;
		data[index++] = START_SYSEX;
		data[index++] = I2C_REQUEST;
		byte[] address_lsb_msb = convertToI2CSlaveAddressLsbMsb(slaveAddress, autoRestart, addressSize10Bit,
				I2CMode.READ_ONCE);
		data[index++] = address_lsb_msb[0];
		data[index++] = address_lsb_msb[1];
		byte[] register_lsb_msb = FirmataProtocol.convertToLsbMsb(register);
		data[index++] = register_lsb_msb[0];
		data[index++] = register_lsb_msb[1];
		byte[] length_lsb_msb = FirmataProtocol.convertToLsbMsb(length);
		data[index++] = length_lsb_msb[0];
		data[index++] = length_lsb_msb[1];
		data[index++] = END_SYSEX;

		return sendMessage(data, I2CResponse.class);
	}

	public void i2cWriteData(int slaveAddress, boolean autoRestart, boolean addressSize10Bit, int register, byte[] data)
			throws RuntimeIOException {
		Logger.debug("i2cWriteData({}, {}, {}, {}, {} bytes)", Integer.valueOf(slaveAddress),
				Boolean.valueOf(autoRestart), Boolean.valueOf(addressSize10Bit), Integer.valueOf(register),
				Integer.valueOf(data.length));
		byte[] buffer = new byte[7 + 2 * data.length];
		int index = 0;
		buffer[index++] = START_SYSEX;
		buffer[index++] = I2C_REQUEST;
		byte[] address_lsb_msb = convertToI2CSlaveAddressLsbMsb(slaveAddress, autoRestart, addressSize10Bit,
				I2CMode.WRITE);
		buffer[index++] = address_lsb_msb[0];
		buffer[index++] = address_lsb_msb[1];
		byte[] register_lsb_msb = FirmataProtocol.convertToLsbMsb(register);
		buffer[index++] = register_lsb_msb[0];
		buffer[index++] = register_lsb_msb[1];
		for (int i = 0; i < data.length; i++) {
			byte[] data_lsb_msb = FirmataProtocol.convertToLsbMsb(data[i]);
			buffer[index++] = data_lsb_msb[0];
			buffer[index++] = data_lsb_msb[1];
		}
		buffer[index++] = END_SYSEX;

		sendMessage(buffer);
	}

	public void createTask(int taskId, int length) {
		if (taskId < 0 || taskId > 0x7f) {
			throw new IllegalArgumentException("Invalid taskId (" + taskId + ") must be 0.." + 0x7f);
		}
		sendMessage(FirmataProtocol.createCreateTaskMessage(taskId, length));
	}

	public void addToTask(int taskId, byte[] taskData) {
		if (taskId < 0 || taskId > 0x7f) {
			throw new IllegalArgumentException("Invalid taskId (" + taskId + ") must be 0.." + 0x7f);
		}

		sendMessage(FirmataProtocol.createAddToTaskMessage(taskId, taskData));
	}

	public int createTask(byte[]... tasks) {
		// Refresh the list of tasks - this updates the internal cache of used task ids
		queryAllTasks();

		// Get the next task id
		byte task_id = 0;
		for (; task_id < MAX_TASK_ID && taskIds.get(Byte.valueOf(task_id)) != null; task_id++) {
		}
		if (task_id == MAX_TASK_ID) {
			throw new IllegalStateException("No free task ids");
		}

		int length = 0;
		for (int i = 0; i < tasks.length; i++) {
			length += tasks[i].length;
		}

		createTask(task_id, length);
		taskIds.put(Byte.valueOf(task_id), Boolean.TRUE);
		for (int i = 0; i < tasks.length; i++) {
			addToTask(task_id, tasks[i]);
		}

		return task_id;
	}

	public byte[] queryAllTasks() {
		return sendMessage(new byte[] { START_SYSEX, SCHEDULER_DATA, QUERY_ALL_FIRMATA_TASKS, END_SYSEX },
				SchedulerDataQueryAllTasksResponse.class).getTaskIds();
	}

	public SchedulerDataQueryTaskResponse queryTask(int taskId) {
		return sendMessage(new byte[] { START_SYSEX, SCHEDULER_DATA, QUERY_FIRMATA_TASK, (byte) taskId, END_SYSEX },
				SchedulerDataQueryTaskResponse.class);
	}

	public void scheduleTask(int taskId, int delayMs) {
		if (taskId < 0 || taskId > 0x7f) {
			throw new IllegalArgumentException("Invalid taskId (" + taskId + ") must be 0.." + 0x7f);
		}

		sendMessage(FirmataProtocol.createScheduleTaskMessage(taskId, delayMs));
	}

	public void deleteTask(int taskId) {
		sendMessage(FirmataProtocol.createDeleteTaskMessage(taskId));
		taskIds.remove(Byte.valueOf((byte) taskId));

	}

	public void schedulerReset() {
		sendMessage(FirmataProtocol.createSchedulerResetMessage());
		taskIds.clear();
	}

	private void sendMessage(byte[] request) throws RuntimeIOException {
		transport.write(request);
	}

	@SuppressWarnings("unchecked")
	private <T extends ResponseMessage> T sendMessage(byte[] request, Class<T> responseClass)
			throws RuntimeIOException, FirmataErrorMessage {
		ResponseMessage response = null;

		try {
			transport.write(request);

			do {
				Logger.trace("Waiting for a response of type {} ...", responseClass.getName());
				response = responseQueue.take();
				Logger.trace("Got response {}", response);
				if (response.isPoison()) {
					return null;
				}
				if (!response.getClass().isAssignableFrom(responseClass)
						&& response.getClass().isAssignableFrom(StringDataResponse.class)) {
					// Most likely a StringData error response
					StringDataResponse sdr = (StringDataResponse) response;
					if (sdr.getValue().equals("Unhandled sysex command")) {
						throw new FirmataErrorMessage(sdr.getValue());
					}
					// Otherwise log and ignore
					Logger.info("Got a string data response: '{}'", sdr.getValue());
				}
			} while (!response.getClass().isAssignableFrom(responseClass));
		} catch (InterruptedException e) {
			// Ignore
			Logger.trace("Interrupted");
		}

		return (T) response;
	}

	@Override
	public void run() {
		if (!running.compareAndSet(false, true)) {
			Logger.warn("Already running?");
			return;
		}

		try {
			while (running.get()) {
				/*-
				 * When attempting to read a file (other than a pipe or FIFO) that supports
				 * non-blocking reads and has no data currently available:
				 * If O_NONBLOCK is set, read() shall return -1 and set errno to [EAGAIN].
				 * If O_NONBLOCK is clear, read() shall block the calling thread until
				 * some data becomes available.
				 */
				int i = transport.read();
				if (i == -1) {
					Logger.warn("Read -1 from device, exiting read responses loop...");
					running.compareAndSet(true, false);
					break;
				}

				long nano_time = System.nanoTime();
				long epoch_time = System.currentTimeMillis();

				byte b = (byte) (i & 0xff);
				if (b == START_SYSEX) {
					Logger.trace("Processing sysex message...");
					responseQueue.offer(readSysEx(null));
					Logger.trace("Added sysex message to queue");
				} else if (b == PROTOCOL_VERSION) {
					Logger.trace("Processing protocol version message...");
					responseQueue.offer(readVersionResponse());
					Logger.trace("Added protocol version message to queue");
				} else if (b == REPORT_FIRMWARE) {
					Logger.trace("Processing firmware version message...");
					responseQueue.add(readSysEx(Byte.valueOf(b)));
					Logger.trace("Added firmware version message to queue");
				} else if (b >= DIGITAL_IO_START && b <= DIGITAL_IO_END) {
					Logger.trace("Processing digital read response message...");
					processDigitalResponse(readDataResponse(b - DIGITAL_IO_START), epoch_time, nano_time);
				} else if (b >= ANALOG_IO_START && b <= ANALOG_IO_END) {
					Logger.trace("Processing analog read response message...");
					processAnalogResponse(readDataResponse(b - ANALOG_IO_START), epoch_time, nano_time);
				} else {
					Logger.warn("Unrecognised response: 0x{}", Integer.toHexString(b & 0xff));
				}
			}
		} catch (RuntimeIOException e) {
			running.compareAndSet(true, false);
			Logger.error(e, "I/O error: {}", e);
		} catch (Throwable t) {
			running.compareAndSet(true, false);
			Logger.error(t, "Error: {}", t);
		}

		Logger.debug("Thread: done");
	}

	private void processDigitalResponse(DataResponse response, long epochTime, long nanoTime) {
		int port = response.getPort();
		int value = response.getValue();
		// Update the cached values
		for (int x = 0; x < 8; x++) {
			int gpio = 8 * port + x;
			Pin pin = pins.get(Integer.valueOf(gpio));
			boolean is_set = (value & (1 << x)) != 0;
			if (pin != null) {
				pin.setValue(is_set);
				eventListener.event(FirmataEventListener.EventType.DIGITAL, gpio, is_set ? 1 : 0, epochTime, nanoTime);
			}
		}
	}

	private void processAnalogResponse(DataResponse response, long eventTime, long nanoTime) {
		// Update the cached value
		int adc = response.getPort();
		Integer pin_num = adcToPinNumberMapping.get(Integer.valueOf(adc));
		if (pin_num != null) {
			Pin pin = pins.get(Integer.valueOf(pin_num.intValue()));
			if (pin != null) {
				pin.setValue(response.getValue());
				eventListener.event(FirmataEventListener.EventType.ANALOG, pin_num.intValue(), response.getValue(),
						eventTime, nanoTime);
			}
		}
	}

	private SysExResponse readSysEx(Byte sysExCommand) {
		byte sysex_cmd;
		if (sysExCommand == null) {
			sysex_cmd = transport.readByte();
		} else {
			sysex_cmd = sysExCommand.byteValue();
		}
		// long start = System.currentTimeMillis();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		while (true) {
			byte b = transport.readByte();
			if (b == END_SYSEX) {
				break;
			}
			baos.write(b);
		}
		byte[] response_data = baos.toByteArray();
		ByteBuffer buffer = ByteBuffer.wrap(response_data);
		// long duration = System.currentTimeMillis() - start;
		// Logger.debug("Took " + duration + "ms to read the SysEx message");

		SysExResponse response = null;
		switch (sysex_cmd) {
		case STRING_DATA:
			String string_data = FirmataProtocol.readString(buffer);
			// String string_data = new String(response_data, StandardCharsets.UTF_16LE);
			Logger.debug("Got string data response '{}'", string_data);
			response = new StringDataResponse(string_data);
			if (string_data.equals("Unhandled sysex command") && inShutdown.get()) {
				Logger.info("Shutdown triggered, exiting serial read loop!");
				running.set(false);
			}
			break;
		case REPORT_FIRMWARE:
			byte major = buffer.get();
			byte minor = buffer.get();

			// Each char is actually sent as 2 7-bit bytes (LSB first)
			/*-
			byte[] chars = new byte[buffer.remaining() / 2];
			for (int i = 0; i < chars.length; i++) {
				chars[i] = (byte) ((buffer.get() & 0x7f) | ((buffer.get() & 0x01) << 7));
			}
			String name = new String(chars, StandardCharsets.UTF_8);
			*/

			response = new FirmwareDetails(major, minor, FirmataProtocol.readString(buffer));
			break;
		case CAPABILITY_RESPONSE:
			List<Set<PinCapability>> capabilities = new ArrayList<>();
			Set<PinCapability> pin_capabilities = new HashSet<>();
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

				capabilities.add(Collections.unmodifiableSet(pin_capabilities));
				pin_capabilities = new HashSet<>();
			}
			response = new CapabilityResponse(capabilities);
			break;
		case REPORT_FEATURES_RESPONSE:
			// TODO Implementation
			response = new ReportFeaturesResponse();
			break;
		case SCHEDULER_DATA:
			byte scheduler_reply_type = buffer.get();
			switch (scheduler_reply_type) {
			case QUERY_ALL_TASKS_REPLY:
				byte[] task_ids = new byte[response_data.length - 1];
				for (int i = 0; i < response_data.length - 1; i++) {
					task_ids[i] = (byte) (response_data[i + 1] & 0x7f);
				}
				response = new SchedulerDataQueryAllTasksResponse(task_ids);
				taskIds.clear();
				// Cache the task ids in use
				for (byte task_id : task_ids) {
					System.out.println("Adding task " + task_id);
					taskIds.put(Byte.valueOf(task_id), Boolean.TRUE);
				}
				break;
			case QUERY_TASK_REPLY:
			case ERROR_TASK_REPLY:
				// TODO Note same payload as the QUERY_TASK_REPLY...
				byte task_id = (byte) (buffer.get() & 0x7f);

				int time_ms = -1;
				int length = -1;
				int position = -1;
				byte[] taskdata = null;
				if (buffer.remaining() > 0) {
					byte[] enc = new byte[buffer.remaining()];
					buffer.get(enc);
					byte[] data = FirmataProtocol.from7BitArray(enc);
					int index = 0;

					time_ms = data[index++] & 0xff;
					time_ms |= ((data[index++] & 0xff) << 8);
					time_ms |= ((data[index++] & 0xff) << 16);
					time_ms |= ((data[index++] & 0xff) << 24);

					length = data[index++] & 0xff;
					length |= ((data[index++] & 0xff) << 8);

					position = data[index++] & 0xff;
					position |= ((data[index++] & 0xff) << 8);

					taskdata = new byte[data.length - index];
					System.arraycopy(data, index, taskdata, 0, data.length - index);
				}

				response = new SchedulerDataQueryTaskResponse(task_id, time_ms, length, position, taskdata,
						sysex_cmd == ERROR_TASK_REPLY);
				break;
			default:
				Logger.warn("Unhandled scheduler reply type: {}", Byte.valueOf(scheduler_reply_type));
			}
			break;
		case PIN_STATE_RESPONSE:
			byte pin = buffer.get();
			PinMode pin_mode = PinMode.valueOf(buffer.get());
			byte[] pin_state = new byte[buffer.remaining()];
			buffer.get(pin_state);
			response = new PinStateResponse(pin, pin_mode, FirmataProtocol.decodeValue(pin_state));
			break;
		case ANALOG_MAPPING_RESPONSE:
			byte[] channels = new byte[buffer.remaining()];
			buffer.get(channels);
			response = new AnalogMappingResponse(channels);
			break;
		case I2C_REPLY:
			int slave_address = FirmataProtocol.decodeValue(buffer.get(), buffer.get());
			int register = FirmataProtocol.decodeValue(buffer.get(), buffer.get());
			byte[] rx_data = new byte[buffer.remaining() / 2];
			if (rx_data.length > 0) {
				for (int i = 0; i < rx_data.length; i++) {
					rx_data[i] = (byte) FirmataProtocol.decodeValue(buffer.get(), buffer.get());
				}
			}
			response = new I2CResponse(slave_address, register, rx_data);
			break;
		default:
			Logger.warn("Unhandled sysex command: 0x{}", Integer.toHexString(sysex_cmd));
		}

		return response;
	}

	private ProtocolVersionResponse readVersionResponse() {
		return new ProtocolVersionResponse(transport.readByte(), transport.readByte());
	}

	private DataResponse readDataResponse(int port) {
		return new DataResponse(port, readShort());
	}

	private int readShort() {
		// LSB (bits 0-6), MSB(bits 7-13)
		return FirmataProtocol.decodeValue(transport.readByte(), transport.readByte());
		// return ((msb & 0x7f) << 7) | (lsb & 0x7f);
	}

	/*
	 * slave address (MSB) + read/write and address mode bits {bit 7: always 0} {bit
	 * 6: auto restart transmission, 0 = stop (default), 1 = restart} {bit 5:
	 * address mode, 1 = 10-bit mode} {bits 4-3: read/write, 00 = write, 01 = read
	 * once, 10 = read continuously, 11 = stop reading} {bits 2-0: slave address MSB
	 * in 10-bit mode, not used in 7-bit mode}
	 */
	static byte[] convertToI2CSlaveAddressLsbMsb(int slaveAddress, boolean autoRestart, boolean addressSize10Bit,
			I2CMode mode) {
		byte[] lsb_msb = FirmataProtocol.convertToLsbMsb(slaveAddress);

		lsb_msb[1] &= 0x7;
		// Bit 6: auto restart transmission, 0 = stop (default), 1 = restart
		if (autoRestart) {
			lsb_msb[1] |= 0x40;
		}
		// Bit 5: address mode, 1 = 10-bit mode
		if (addressSize10Bit) {
			lsb_msb[1] |= 0x20;
		}
		// bits 4-3: read/write, 00 = write, 01 = read once, 10 = read continuously,
		// 11 = stop reading
		lsb_msb[1] |= (mode.ordinal() << 3);

		return lsb_msb;
	}

	static class ResponseMessage {
		static ResponseMessage POISON_MESSAGE = new ResponseMessage(true);

		private boolean poison;

		ResponseMessage() {
			this(false);
		}

		private ResponseMessage(boolean poison) {
			this.poison = poison;
		}

		public boolean isPoison() {
			return poison;
		}
	}

	static class SysExResponse extends ResponseMessage {
	}

	public static class StringDataResponse extends SysExResponse {
		private String value;

		public StringDataResponse(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		@Override
		public String toString() {
			return "StringDataResponse [value=" + value + "]";
		}
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

		public String getVersionString() {
			return "v" + major + "." + minor;
		}

		@Override
		public String toString() {
			return "FirmwareDetails [major=" + major + ", minor=" + minor + ", name=" + name + "]";
		}
	}

	/*-
	0  START_SYSEX                (0xF0)
	1  REPORT_FEATURES            (0x65)
	2  REPORT_FEATURES_RESPONSE   (0x01)
	3  1st FEATURE_ID             (1-127, eg: Serial = 0x60, Stepper = 0x62)
	4  1st FEATURE_MAJOR_VERSION  (0-127)
	5  1st FEATURE_MINOR_VERSION  (0-127)
	6  2nd FEATURE_ID             (1-127, eg: Serial = 0x60, Stepper = 0x62)
	7  2nd FEATURE_MAJOR_VERSION  (0-127)
	8  2nd FEATURE_MINOR_VERSION  (0-127)
	9  3rd FEATURE_ID             (0x00, Extended ID)
	10 3rd FEATURE_ID             (lsb)
	11 3rd FEATURE_ID             (msb)
	12 3rd FEATURE_MAJOR_VERSION  (0-127)
	13 3rd FEATURE_MINOR_VERSION  (0-127)
	...for all supported features
	n  END_SYSEX                  (0xF7)
	 */
	static class ReportFeaturesResponse extends SysExResponse {
		ReportFeaturesResponse() {
		}

		@Override
		public String toString() {
			return "ReportFeaturesResponse";
		}
	}

	static class CapabilityResponse extends SysExResponse {
		private List<Set<PinCapability>> capabilities;

		CapabilityResponse(List<Set<PinCapability>> capabilities) {
			this.capabilities = capabilities;
		}

		public List<Set<PinCapability>> getCapabilities() {
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
	static class PinStateResponse extends SysExResponse {
		private byte pin;
		private PinMode mode;
		private int state;

		public PinStateResponse(byte pin, PinMode mode, int state) {
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

	static class ProtocolVersionResponse extends ResponseMessage {
		private byte major;
		private byte minor;

		ProtocolVersionResponse(byte major, byte minor) {
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

		@Override
		public String toString() {
			return "I2CResponse[slaveAddress=" + slaveAddress + ", register=" + register + ", data.length="
					+ data.length + "]";
		}
	}

	static abstract class SchedulerDataResponse extends SysExResponse {
	}

	static class SchedulerDataErrorResponse extends SchedulerDataResponse {
	}

	static class SchedulerDataQueryAllTasksResponse extends SchedulerDataResponse {
		private byte[] taskIds;

		SchedulerDataQueryAllTasksResponse(byte[] taskIds) {
			this.taskIds = taskIds;
		}

		public byte[] getTaskIds() {
			return taskIds;
		}
	}

	public static class SchedulerDataQueryTaskResponse extends SchedulerDataResponse {
		private byte taskId;
		private int timeMs;
		private int length;
		private int pos;
		private byte[] taskdata;
		private boolean error;

		SchedulerDataQueryTaskResponse(byte taskId, int timeMs, int length, int pos, byte[] taskdata, boolean error) {
			this.taskId = taskId;
			this.timeMs = timeMs;
			this.length = length;
			this.pos = pos;
			this.taskdata = taskdata;
			this.error = error;
		}

		public byte getTaskId() {
			return taskId;
		}

		public int getTimeMs() {
			return timeMs;
		}

		public int getLength() {
			return length;
		}

		public int getPos() {
			return pos;
		}

		public byte[] getTaskdata() {
			return taskdata;
		}

		public boolean isError() {
			return error;
		}

		@Override
		public String toString() {
			return "SchedulerDataQueryTaskResponse [taskId=" + taskId + ", timeMs=" + timeMs + ", length=" + length
					+ ", pos=" + pos + ", taskdata=" + Arrays.toString(taskdata) + ", error=" + error + "]";
		}
	}

	static class Pin {
		private int gpio;
		// Set of pin mode and resolution pairs
		private Set<PinCapability> capabilities;
		private PinMode mode = PinMode.UNKNOWN;
		private int value;
		private Set<PinMode> modes;

		public Pin(int gpio, Set<PinCapability> capabilities) {
			this.gpio = gpio;
			this.capabilities = capabilities;
			modes = capabilities.stream().map(PinCapability::getMode).collect(Collectors.toSet());
		}

		public int getGpio() {
			return gpio;
		}

		public Set<PinCapability> getCapabilities() {
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

	enum I2CMode {
		WRITE, READ_ONCE, READ_CONTINUOUSLY, STOP_READING;
	}

	public static class FirmataErrorMessage extends RuntimeIOException {
		private static final long serialVersionUID = 2994268944182249234L;

		public FirmataErrorMessage(String message) {
			super(message);
		}
	}

	public static class SchedulerDataError extends RuntimeIOException {
		private static final long serialVersionUID = 2156402847568399280L;

		private byte taskId;
		private int timeMs;
		private int length;
		private int position;
		private byte[] taskdata;

		public SchedulerDataError(byte taskId, int timeMs, int length, int position, byte[] taskdata) {
			super("Scheduler data error for task " + taskId);

			this.taskId = taskId;
			this.timeMs = timeMs;
			this.length = length;
			this.position = position;
			this.taskdata = taskdata;
		}

		public byte getTaskId() {
			return taskId;
		}

		public int getTimeMs() {
			return timeMs;
		}

		public int getLength() {
			return length;
		}

		public int getPosition() {
			return position;
		}

		public byte[] getTaskdata() {
			return taskdata;
		}
	}
}
