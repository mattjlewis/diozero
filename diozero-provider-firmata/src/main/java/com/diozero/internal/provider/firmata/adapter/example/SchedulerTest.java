package com.diozero.internal.provider.firmata.adapter.example;

import com.diozero.api.SerialConstants;
import com.diozero.api.SerialDevice;
import com.diozero.internal.provider.firmata.adapter.FirmataAdapter;
import com.diozero.internal.provider.firmata.adapter.FirmataAdapter.SchedulerDataQueryTaskResponse;
import com.diozero.internal.provider.firmata.adapter.FirmataEventListener.EventType;
import com.diozero.internal.provider.firmata.adapter.FirmataProtocol;
import com.diozero.internal.provider.firmata.adapter.SerialFirmataTransport;
import com.diozero.util.Hex;
import com.diozero.util.PropertyUtil;
import com.diozero.util.SleepUtil;

public class SchedulerTest implements FirmataProtocol {
	static final String SERIAL_PORT_PROP = "diozero.firmata.serialPort";
	static final String SERIAL_BAUD_PROP = "diozero.firmata.serialBaud";
	static final String SERIAL_DATA_BITS_PROP = "diozero.firmata.serialDataBits";
	static final String SERIAL_STOP_BITS_PROP = "diozero.firmata.serialStopBits";
	static final String SERIAL_PARITY_PROP = "diozero.firmata.serialParity";
	static final String TCP_HOST_PROP = "diozero.firmata.tcpHostname";
	static final String TCP_PORT_PROP = "diozero.firmata.tcpPort";
	static final int DEFAULT_TCP_PORT = 3030;

	private static int num7BitOutbytes(int a) {
		// #define num7BitOutbytes(a)(((a)*7)>>3)
		return (a * 7) >> 3;
	}

	public static void main(String[] args) {
		byte[] data = { (byte) 0xf5, 0x0d, 0x01 };
		Hex.dumpByteArray(data);
		byte[] enc = FirmataProtocol.to7BitArray(data);
		Hex.dumpByteArray(enc);
		data = FirmataProtocol.from7BitArray(enc);
		Hex.dumpByteArray(data);

		int argc = 5;
		int len = num7BitOutbytes(argc - 2);
		System.out.println("len: " + len);

		String serial_port_name = PropertyUtil.getProperty(SERIAL_PORT_PROP, null);
		if (serial_port_name == null) {
			return;
		}

		SerialDevice.DataBits data_bits = SerialConstants.DEFAULT_DATA_BITS;
		String val = PropertyUtil.getProperty(SERIAL_DATA_BITS_PROP, null);
		if (val != null) {
			data_bits = SerialDevice.DataBits.valueOf(val.trim());
		}

		SerialDevice.StopBits stop_bits = SerialConstants.DEFAULT_STOP_BITS;
		val = PropertyUtil.getProperty(SERIAL_STOP_BITS_PROP, null);
		if (val != null) {
			stop_bits = SerialDevice.StopBits.valueOf(val.trim());
		}

		SerialDevice.Parity parity = SerialConstants.DEFAULT_PARITY;
		val = PropertyUtil.getProperty(SERIAL_PARITY_PROP, null);
		if (val != null) {
			parity = SerialDevice.Parity.valueOf(val.trim());
		}

		try (FirmataAdapter adapter = new FirmataAdapter(new SerialFirmataTransport(serial_port_name,
				PropertyUtil.getIntProperty(SERIAL_BAUD_PROP, SerialConstants.BAUD_57600), data_bits, stop_bits, parity,
				SerialConstants.DEFAULT_READ_BLOCKING, SerialConstants.DEFAULT_MIN_READ_CHARS,
				SerialConstants.DEFAULT_READ_TIMEOUT_MILLIS), SchedulerTest::event)) {
			adapter.start();
			adapter.systemReset();

			int gpio = 13;
			adapter.setPinMode(gpio, PinMode.DIGITAL_OUTPUT);
			for (int i = 0; i < 5; i++) {
				adapter.setDigitalValue(gpio, true);
				SleepUtil.sleepMillis(200);
				adapter.setDigitalValue(gpio, false);
				SleepUtil.sleepMillis(200);
			}

			System.out.format("name: %s, version: %s%n", adapter.getFirmware().getName(),
					adapter.getFirmware().getVersionString());

			listTasks(adapter);

			int task_id = 1;
			int length = 11;
			adapter.createTask(task_id, length);
			listTasks(adapter);
			SchedulerDataQueryTaskResponse resp = adapter.queryTask(task_id);
			System.out.format("Task %d: %s, taskdata.length: %d%n", Integer.valueOf(task_id), resp,
					Integer.valueOf(resp.getTaskdata() == null ? -1 : resp.getTaskdata().length));

			task_id = 99;
			length++;
			adapter.createTask(task_id, length);
			listTasks(adapter);
			resp = adapter.queryTask(task_id);
			System.out.format("Task %d: %s, taskdata.length: %d%n", Integer.valueOf(task_id), resp,
					Integer.valueOf(resp.getTaskdata() == null ? -1 : resp.getTaskdata().length));

			task_id = 5;
			length++;
			adapter.createTask(task_id, length);
			listTasks(adapter);
			resp = adapter.queryTask(task_id);
			System.out.format("Task %d: %s, taskdata.length: %d%n", Integer.valueOf(task_id), resp,
					Integer.valueOf(resp.getTaskdata() == null ? -1 : resp.getTaskdata().length));

			task_id = 15;
			length = 0;
			adapter.createTask(task_id, length);
			listTasks(adapter);
			resp = adapter.queryTask(task_id);
			System.out.format("Task %d: %s, taskdata.length: %d%n", Integer.valueOf(task_id), resp,
					Integer.valueOf(resp.getTaskdata() == null ? -1 : resp.getTaskdata().length));

			adapter.deleteTask(5);
			listTasks(adapter);

			adapter.deleteTask(1);
			listTasks(adapter);

			adapter.deleteTask(99);
			listTasks(adapter);

			task_id = 0x0c;
			int delay_ms = 100;

			byte[] set_gpio_on_message = FirmataProtocol.createSetDigitalValueMessage(gpio, true);
			byte[] set_gpio_off_message = FirmataProtocol.createSetDigitalValueMessage(gpio, false);
			byte[] scheduler_delay_msg = FirmataProtocol.createSchedulerDelayMessage(delay_ms);

			length = set_gpio_on_message.length + scheduler_delay_msg.length + set_gpio_off_message.length
					+ scheduler_delay_msg.length;
			System.out.println("Creating task " + task_id + " of length " + length);
			adapter.createTask(task_id, length);
			listTasks(adapter);
			resp = adapter.queryTask(task_id);
			System.out.format("Task %d: %s, taskdata.length: %d%n", Integer.valueOf(task_id), resp,
					Integer.valueOf(resp.getTaskdata() == null ? -1 : resp.getTaskdata().length));
			Hex.dumpByteArray(resp.getTaskdata());
			System.out.println();

			System.out.println("Adding first task");
			adapter.addToTask(task_id, set_gpio_on_message);

			byte[] task_ids = adapter.queryAllTasks();
			if (task_ids.length < 1) {
				System.out.println("No tasks, exiting");
				return;
			}
			System.out.println(task_ids.length + " task(s)");
			System.out.println();

			resp = adapter.queryTask(task_id);
			System.out.format("Task %d: %s, taskdata.length: %d%n", Integer.valueOf(task_id), resp,
					Integer.valueOf(resp.getTaskdata() == null ? -1 : resp.getTaskdata().length));
			Hex.dumpByteArray(resp.getTaskdata());
			System.out.println();

			adapter.addToTask(task_id, scheduler_delay_msg);
			adapter.addToTask(task_id, set_gpio_off_message);
			adapter.addToTask(task_id, scheduler_delay_msg);

			listTasks(adapter);
			resp = adapter.queryTask(task_id);
			System.out.format("Task %d: %s, taskdata.length: %d%n", Integer.valueOf(task_id), resp,
					Integer.valueOf(resp.getTaskdata() == null ? -1 : resp.getTaskdata().length));
			Hex.dumpByteArray(resp.getTaskdata());
			System.out.println();

			System.out.println("scheduling task " + task_id);
			adapter.scheduleTask(task_id, 100);

			SleepUtil.sleepSeconds(5);

			listTasks(adapter);
			resp = adapter.queryTask(task_id);
			System.out.format("Task %d: %s, taskdata.length: %d%n", Integer.valueOf(task_id), resp,
					Integer.valueOf(resp.getTaskdata() == null ? -1 : resp.getTaskdata().length));
			if (resp.getTaskdata() != null) {
				Hex.dumpByteArray(resp.getTaskdata());
			}
			System.out.println();

			System.out.println("Removing all tasks");
			adapter.schedulerReset();
			SleepUtil.sleepSeconds(1);

			System.out.println("Creating new task using convenience method");
			task_id = adapter.createTask(set_gpio_on_message, scheduler_delay_msg, set_gpio_off_message,
					scheduler_delay_msg);
			adapter.scheduleTask(task_id, 5);
			SleepUtil.sleepSeconds(5);
			adapter.deleteTask(task_id);

			System.out.println("Blinking");
			for (int i = 0; i < 5; i++) {
				adapter.setDigitalValue(gpio, true);
				SleepUtil.sleepMillis(200);
				adapter.setDigitalValue(gpio, false);
				SleepUtil.sleepMillis(200);
			}
		}
	}

	private static void listTasks(FirmataAdapter adapter) {
		byte[] task_ids = adapter.queryAllTasks();
		if (task_ids.length == 0) {
			System.out.println("No tasks");
		} else {
			System.out.println(task_ids.length + " task(s):");
			for (int i = 0; i < task_ids.length; i++) {
				System.out.println(task_ids[i]);
			}
		}
	}

	public static void event(EventType eventType, int gpio, int value, long epochTime, long nanoTime) {
		System.out
				.println("event(" + eventType + ", " + gpio + ", " + value + ", " + epochTime + ", " + nanoTime + ")");
	}
}
