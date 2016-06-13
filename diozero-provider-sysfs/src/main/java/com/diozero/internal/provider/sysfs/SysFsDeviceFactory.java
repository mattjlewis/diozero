package com.diozero.internal.provider.sysfs;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.*;

import org.pmw.tinylog.Logger;

import com.diozero.api.*;
import com.diozero.internal.spi.*;
import com.diozero.internal.spi.GpioDeviceInterface.Direction;
import com.diozero.util.DioZeroScheduler;
import com.diozero.util.RuntimeIOException;

public class SysFsDeviceFactory extends BaseNativeDeviceFactory
implements Runnable {
	private static final String GPIO_ROOT_DIR = "/sys/class/gpio";
	private static final String EXPORT_FILE = "export";
	private static final String UNEXPORT_FILE = "unexport";
	private static final String GPIO_DIR_PREFIX = "gpio";
	private static final String DIRECTION_FILE = "direction";
	
	private Path rootPath;
	private WatchService exportedGpiosWatchService;
	private Thread watchServiceThread;
	private boolean watchServiceRunning;
	
	public SysFsDeviceFactory() {
		rootPath = FileSystems.getDefault().getPath(GPIO_ROOT_DIR);
		
		try {
			exportedGpiosWatchService = FileSystems.getDefault().newWatchService();
			rootPath.register(exportedGpiosWatchService,
					StandardWatchEventKinds.ENTRY_CREATE,
					StandardWatchEventKinds.ENTRY_DELETE);
			
			// Monitor for changes to exported pins
			DioZeroScheduler.getDaemonInstance().execute(this);
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}
	
	@Override
	public void shutdown() {
		watchServiceRunning = false;
		if (watchServiceThread != null) {
			watchServiceThread.interrupt();
		}
		super.shutdown();
	}

	/**
	 * Check if this pin is exported by checking the existance of /sys/class/gpio/gpioxxx/
	 * @param pinNumber GPIO pin
	 * @return Returns true if this pin is currently exported
	 */
	public boolean isExported(int pinNumber) {
		return Files.isDirectory(getGpioDir(pinNumber));
	}

	@Override
	public String getName() {
		return getClass().getSimpleName();
	}

	@Override
	public int getPwmFrequency(int pinNumber) {
		throw new UnsupportedOperationException("PWM not supported");
	}

	@Override
	public void setPwmFrequency(int pinNumber, int pwmFrequency) {
		throw new UnsupportedOperationException("PWM not supported");
	}

	@Override
	protected GpioDigitalInputDeviceInterface createDigitalInputPin(String key, int pinNumber, GpioPullUpDown pud,
			GpioEventTrigger trigger) throws RuntimeIOException {
		export(pinNumber, Direction.INPUT);
		
		return new SysFsGpioInputDevice(this, getGpioDir(pinNumber), key, pinNumber, pud, trigger);
	}

	@Override
	protected GpioDigitalOutputDeviceInterface createDigitalOutputPin(String key, int pinNumber, boolean initialValue)
			throws RuntimeIOException {
		export(pinNumber, Direction.OUTPUT);
		
		return new SysFsGpioOutputDevice(this, getGpioDir(pinNumber), key, pinNumber, initialValue);
	}

	@Override
	protected GpioAnalogInputDeviceInterface createAnalogInputPin(String key, int pinNumber) throws RuntimeIOException {
		throw new UnsupportedOperationException("Analogue input not supported");
	}

	@Override
	protected PwmOutputDeviceInterface createPwmOutputPin(String key, int pinNumber, float initialValue,
			PwmType pwmType) throws RuntimeIOException {
		throw new UnsupportedOperationException("PWM not supported");
	}

	@Override
	protected SpiDeviceInterface createSpiDevice(String key, int controller, int chipSelect, int frequency,
			SpiClockMode spiClockMode) throws RuntimeIOException {
		throw new UnsupportedOperationException("SPI not supported");
	}

	@Override
	protected I2CDeviceInterface createI2CDevice(String key, int controller, int address, int addressSize,
			int clockFrequency) throws RuntimeIOException {
		throw new UnsupportedOperationException("I2C not supported");
	}
	
	private void export(int pinNumber, Direction direction) {
		if (! isExported(pinNumber)) {
			try (Writer export_writer = new FileWriter(rootPath.resolve(EXPORT_FILE).toFile())) {
				export_writer.write(String.valueOf(pinNumber));
			} catch (IOException e) {
				throw new RuntimeIOException(e);
			}
		}
		
		Path direction_file = getGpioDir(pinNumber).resolve(DIRECTION_FILE);
		// Wait up to 500ms for the gpioxxx/direction file to exist
		int delay = 500;
		long start = System.currentTimeMillis();
		while (true) {
			if (Files.exists(direction_file)) {
				break;
			}
			if (System.currentTimeMillis() - start > delay) {
				unexport(pinNumber);
				throw new RuntimeIOException("Waited for over " + delay + " ms for the GPIO pin to be created, aborting");
			}
			try {
				Thread.sleep(10);
			} catch (InterruptedException ie) {
				// Ignore
			}
		}

		try (FileWriter writer = new FileWriter(direction_file.toFile())) {
			writer.write(direction == Direction.OUTPUT ? "out" : "in");
		} catch (IOException e) {
			unexport(pinNumber);
			throw new RuntimeIOException("Error setting direction for GPIO " + pinNumber, e);
		}
	}
	
	private Path getGpioDir(int pinNumber) {
		return rootPath.resolve(GPIO_DIR_PREFIX + pinNumber);
	}

	void unexport(int pinNumber) {
		if (isExported(pinNumber)) {
			try (Writer unexport_writer = new FileWriter(rootPath.resolve(UNEXPORT_FILE).toFile())) {
				unexport_writer.write(String.valueOf(pinNumber));
			} catch (IOException e) {
				throw new RuntimeIOException(e);
			}
		}
	}

	@Override
	public void run() {
		watchServiceThread = Thread.currentThread();
		watchServiceRunning = true;
		try {
			while (watchServiceRunning) {
				// Wait for a change
				WatchKey key = exportedGpiosWatchService.take();
				for (WatchEvent<?> event: key.pollEvents()) {
					WatchEvent.Kind<?> kind = event.kind();
					if (kind == StandardWatchEventKinds.OVERFLOW) {
						continue;
					}
					
					@SuppressWarnings("unchecked")
					WatchEvent<Path> ev = (WatchEvent<Path>) event;
					Path path = ev.context();
					if (path.getFileName().toString().startsWith(GPIO_DIR_PREFIX)) {
						// TODO Do something...
						Logger.info("GPIO dir changed, path='" + path + "', event kind=" + kind);
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
}
