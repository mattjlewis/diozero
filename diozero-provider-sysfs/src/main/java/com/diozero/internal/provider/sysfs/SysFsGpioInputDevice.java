package com.diozero.internal.provider.sysfs;

import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.*;

import org.pmw.tinylog.Logger;

import com.diozero.api.DigitalInputEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.internal.spi.AbstractInputDevice;
import com.diozero.internal.spi.GpioDigitalInputDeviceInterface;
import com.diozero.util.DioZeroScheduler;
import com.diozero.util.RuntimeIOException;

/**
 * Pure Java implementation using <a href="https://www.kernel.org/doc/Documentation/gpio/sysfs.txt">/sys/class/gpio</a> kernel module
 *
 */
public class SysFsGpioInputDevice extends AbstractInputDevice<DigitalInputEvent>
implements GpioDigitalInputDeviceInterface, Runnable {
	private static final String EDGE_FILE = "edge";
	private static final String VALUE_FILE = "value";
	//private static final byte LOW_VALUE = '0';
	private static final byte HIGH_VALUE = '1';
	
	private SysFsDeviceFactory deviceFactory;
	private int pinNumber;
	private RandomAccessFile valueFile;
	private boolean watchServiceRunning;
	private WatchService valueWatchService;
	private Thread watchServiceThread;

	public SysFsGpioInputDevice(SysFsDeviceFactory deviceFactory, Path gpioDir,
			String key, int pinNumber, GpioPullUpDown pud, GpioEventTrigger trigger) {
		super(key, deviceFactory);
		
		this.deviceFactory = deviceFactory;
		this.pinNumber = pinNumber;

		try (FileWriter writer = new FileWriter(gpioDir.resolve(EDGE_FILE).toFile())) {
			writer.write(trigger.name().toLowerCase());
		} catch (IOException e) {
			throw new RuntimeIOException("Error setting edge for GPIO " + pinNumber, e);
		}
		
		// Note: Not possible to set pull-up/down resistor configuration via /sys/class/gpio
		// TODO Set active_low based on the pud value?
		
		Path value_path = gpioDir.resolve(VALUE_FILE);
		try {
			valueFile = new RandomAccessFile(value_path.toFile(), "r");
		} catch (IOException e) {
			throw new RuntimeIOException("Error opening value file for GPIO " + pinNumber, e);
		}

		// FIXME WatchService doesn't work with /sys, /proc and network file-systems
		// http://stackoverflow.com/questions/30190730/nio-watchservice-for-unix-sys-classes-gpio-files
		try {
			valueWatchService = FileSystems.getDefault().newWatchService();
			gpioDir.register(valueWatchService, StandardWatchEventKinds.ENTRY_MODIFY);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}
	
	@Override
	public void run() {
		watchServiceThread = Thread.currentThread();
		watchServiceRunning = true;
		try {
			while (watchServiceRunning) {
				// Wait for a change
				WatchKey key = valueWatchService.take();
				Logger.debug("Got WatchKey '" + key + "'");
				long epoch_time = System.currentTimeMillis();
				long nano_time = System.nanoTime();
				for (WatchEvent<?> event: key.pollEvents()) {
					WatchEvent.Kind<?> kind = event.kind();
					Logger.debug("Got WatchEvent '" + event + "'");
					if (kind != StandardWatchEventKinds.ENTRY_MODIFY) {
						continue;
					}
					
					Path path = (Path) event.context();
					Logger.debug("Got WatchEvent Path '" + path + "'");
					if (path.endsWith(VALUE_FILE)) {
						valueChanged(new DigitalInputEvent(pinNumber, epoch_time, nano_time, getValue()));
					}
				}
				if (! key.reset()) {
					Logger.error("Error, key.reset() return false");
					break;
				}
			}
		} catch (InterruptedException e) {
			// Ignore
		}
		watchServiceRunning = false;
	}

	@Override
	public int getPin() {
		return pinNumber;
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		try {
			valueFile.seek(0);
			return valueFile.readByte() == HIGH_VALUE;
		} catch (IOException e) {
			throw new RuntimeIOException("Error reading value", e);
		}
	}

	@Override
	public void setDebounceTimeMillis(int debounceTime) {
		throw new UnsupportedOperationException("Debounce not supported");
	}

	@Override
	public void enableListener() {
		disableListener();
		DioZeroScheduler.getNonDaemonInstance().execute(this);
	}

	@Override
	public void disableListener() {
		watchServiceRunning = false;
		if (watchServiceThread != null) {
			watchServiceThread.interrupt();
		}
	}

	@Override
	public void closeDevice() throws RuntimeIOException {
		Logger.debug("closeDevice()");
		disableListener();
		try {
			valueWatchService.close();
			valueFile.close();
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
		deviceFactory.unexport(pinNumber);
	}
}
