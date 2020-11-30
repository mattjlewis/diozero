package com.diozero.api;

/*
 * #%L
 * Organisation: diozero
 * Project:      Device I/O Zero - Core
 * Filename:     AnalogInputDevice.java  
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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.tinylog.Logger;

import com.diozero.internal.spi.AnalogInputDeviceFactoryInterface;
import com.diozero.internal.spi.AnalogInputDeviceInterface;
import com.diozero.sbc.DeviceFactoryHelper;
import com.diozero.util.DiozeroScheduler;

/**
 * <p>
 * The AnalogInputDevice base class encapsulates logic for interfacing with
 * analog devices. This class provides access to unscaled (-1..1) and scaled
 * (e.g. voltage, temperature, distance) readings. For scaled readings is
 * important that the device factory is configured correctly - all raw analog
 * readings are normalised (i.e. -1..1).
 * </p>
 * <p>
 * Note: The Raspberry Pi does not natively support analog input devices, see
 * {@link com.diozero.devices.McpAdc McpAdc} for connecting to analog-to-digital
 * converters.
 * </p>
 * <p>
 * Example: Temperature readings using an MCP3008 and TMP36:
 * </p>
 * <img src="doc-files/MCP3008_TMP36.png" alt="MCP3008 TMP36">
 * <p>
 * Code taken from <a href=
 * "https://github.com/mattjlewis/diozero/blob/master/diozero-sampleapps/src/main/java/com/diozero/sampleapps/TMP36Test.java">TMP36Test</a>:
 * </p>
 * 
 * <pre>
 * {@code
 *try (McpAdc adc = new McpAdc(type, chipSelect);
 *	TMP36 tmp36 = new TMP36(adc, pin, vRef, tempOffset)) {
 *	for (int i=0; i<ITERATIONS; i++) {
 *		double tmp = tmp36.getTemperature();
 *		Logger.info("Temperature: {}", String.format("%.2f", Double.valueOf(tmp)));
 *		SleepUtil.sleepSeconds(.5);
 *	}
 *}
 *}
 * </pre>
 */
public class AnalogInputDevice extends GpioInputDevice<AnalogInputEvent> implements Runnable {
	private static final int DEFAULT_POLL_INTERVAL = 50;
	private AnalogInputDeviceInterface device;
	private Float lastValue;
	private int pollInterval = DEFAULT_POLL_INTERVAL;
	private float percentChange;
	private AtomicBoolean stopScheduler;
	private float range;

	/**
	 * @param gpio GPIO to which the device is connected.
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public AnalogInputDevice(int gpio) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), gpio);
	}

	/**
	 * @param gpio  GPIO to which the device is connected.
	 * @param range To be used for taking scaled readings for this device.
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public AnalogInputDevice(int gpio, float range) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), gpio, range);
	}

	/**
	 * @param deviceFactory The device factory to use to provision this device.
	 * @param gpio          GPIO to which the device is connected.
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public AnalogInputDevice(AnalogInputDeviceFactoryInterface deviceFactory, int gpio) throws RuntimeIOException {
		this(deviceFactory, gpio, deviceFactory.getVRef());
	}

	/**
	 * @param deviceFactory The device factory to use to provision this device.
	 * @param gpio          GPIO to which the device is connected.
	 * @param range         To be used for taking scaled readings for this device.
	 * @throws RuntimeIOException If an I/O error occurred.
	 */
	public AnalogInputDevice(AnalogInputDeviceFactoryInterface deviceFactory, int gpio, float range)
			throws RuntimeIOException {
		super(gpio);

		this.range = range;
		device = deviceFactory.provisionAnalogInputDevice(gpio);
		stopScheduler = new AtomicBoolean(true);
	}

	@Override
	public void close() throws RuntimeIOException {
		Logger.trace("close()");
		stopScheduler.set(true);
		device.close();
	}

	@Override
	protected void enableDeviceListener() {
		if (device.generatesEvents()) {
			device.setListener(this);
		} else {
			stopScheduler.set(false);
			DiozeroScheduler.getDaemonInstance().scheduleAtFixedRate(this, pollInterval, pollInterval,
					TimeUnit.MILLISECONDS);
		}
	}

	@Override
	protected void disableDeviceListener() {
		stopScheduler.set(true);
		device.removeListener();
	}

	@Override
	public void run() {
		if (stopScheduler.get()) {
			throw new RuntimeException("Stopping scheduler due to close request, device key=" + device.getKey());
		}

		float unscaled = getUnscaledValue();
		if (changeDetected(unscaled)) {
			valueChanged(new AnalogInputEvent(gpio, System.currentTimeMillis(), System.nanoTime(), unscaled));
			lastValue = Float.valueOf(unscaled);
		}
	}

	@Override
	public void valueChanged(AnalogInputEvent event) {
		event.setRange(range);
		super.valueChanged(event);
	}

	private boolean changeDetected(float value) {
		if (lastValue == null) {
			return true;
		}

		float last_value = lastValue.floatValue();
		if (percentChange != 0) {
			return value < (1 - percentChange) * last_value || value > (1 + percentChange) * last_value;
		}

		return value != last_value;
	}

	/**
	 * Get the analog range for this input device as used by
	 * {@link AnalogInputDevice#getScaledValue} and
	 * {@link AnalogInputDevice#convertToScaledValue}
	 * 
	 * @return the analog range for this input device
	 */
	public float getRange() {
		return range;
	}

	/**
	 * Get the unscaled normalised value in the range 0..1 (if unsigned) or -1..1
	 * (if signed)
	 * 
	 * @return the unscaled value
	 * @throws RuntimeIOException if there was an I/O error
	 */
	public float getUnscaledValue() throws RuntimeIOException {
		return device.getValue();
	}

	/**
	 * Get the scaled value in the range 0..range (if unsigned) or -range..range (if
	 * signed)
	 * 
	 * @return the scaled value (-range..range)
	 * @throws RuntimeIOException if there was an I/O error
	 */
	public float getScaledValue() throws RuntimeIOException {
		// The raw device must return unscaled values (-1..1)
		return convertToScaledValue(device.getValue());
	}

	/**
	 * Convert the specified unscaled value (-1..1) to a scaled one (-range..range).
	 * 
	 * @see AnalogInputDevice#getRange
	 * 
	 * @param unscaledValue the unscaled value in the range -1..1
	 * @return the scaled value in -range..range
	 */
	public float convertToScaledValue(float unscaledValue) {
		return unscaledValue * range;
	}

	/**
	 * Register a listener for value changes, will check for changes every 50ms.
	 * 
	 * @param listener      The listener callback.
	 * @param percentChange Degree of change required to trigger an event.
	 */
	public void addListener(InputEventListener<AnalogInputEvent> listener, float percentChange) {
		addListener(listener, percentChange, DEFAULT_POLL_INTERVAL);
	}

	/**
	 * Register a listener for value changes, will check for changes every 50ms.
	 * 
	 * @param listener      The listener callback.
	 * @param percentChange Degree of change required to trigger an event.
	 * @param pollInterval  Time in milliseconds at which reading should be taken.
	 */
	public void addListener(InputEventListener<AnalogInputEvent> listener, float percentChange, int pollInterval) {
		this.percentChange = percentChange;
		this.pollInterval = pollInterval;
		addListener(listener);
	}
}
