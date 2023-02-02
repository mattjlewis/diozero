package com.diozero.internal.spi;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     DeviceFactoryInterface.java
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

import java.util.function.Function;
import java.util.function.Supplier;

import com.diozero.api.DeviceAlreadyOpenedException;
import com.diozero.api.NoSuchDeviceException;
import com.diozero.api.PinInfo;
import com.diozero.api.RuntimeIOException;
import com.diozero.sbc.BoardPinInfo;

public interface DeviceFactoryInterface extends AutoCloseable {
	/**
	 * Get the name of this device factory
	 *
	 * @return the name of this device factory
	 */
	String getName();

	/**
	 * Lifecycle method to start this device factory
	 */
	default void start() {
		// Do nothing
	}

	/**
	 * Close this device factory including all devices that have been provisioned by
	 * it.
	 */
	@Override
	void close() throws RuntimeIOException;

	/**
	 * Check if this device factory is closed.
	 *
	 * @return true if this device factory is closed
	 */
	boolean isClosed();

	/**
	 * Reopen this device factory.
	 */
	void reopen();

	/**
	 * Check if the device with the given unique key is opened
	 *
	 * @param key the unique key of the device
	 * @return true if the device is opened
	 */
	boolean isDeviceOpened(String key);

	/**
	 * diozero internal method to notify the {@link AbstractDeviceFactory} that a
	 * device has been opened. Enables diozero to perform cleanup operations, for
	 * example closing a device factory closes all devices provisionined by that
	 * device factory.
	 *
	 * @param device the internal device
	 */
	void deviceOpened(InternalDeviceInterface device);

	/**
	 * diozero internal method to notify the {@link AbstractDeviceFactory} that a
	 * device has been closed.
	 *
	 * @param device the internal device
	 */
	void deviceClosed(InternalDeviceInterface device);

	/**
	 * Get information about pins that can be provisioned by this device factory.
	 *
	 * @return board pin info instance for this device factory
	 */
	BoardPinInfo getBoardPinInfo();

	/**
	 * diozero internal method to generate a unique key for the specified pin. Used
	 * for maintaining the state of devices provisioned by this device factory.
	 *
	 * @param pinInfo the pin to create the key for
	 * @return a key that is unique to this pin
	 */
	String createPinKey(PinInfo pinInfo);

	/**
	 * diozero internal method to generate a unique key for the specified pin. Used
	 * for maintaining the state of devices provisioned by this device factory.
	 *
	 * @param pinInfo the pin to create the key for
	 * @return a key that is unique to this pin
	 */
	String createPwmPinKey(PinInfo pinInfo);

	/**
	 * diozero internal method to generate a unique key for the specified pin. Used
	 * for maintaining the state of devices provisioned by this device factory.
	 *
	 * @param pinInfo the pin to create the key for
	 * @return a key that is unique to this pin
	 */
	String createServoPinKey(PinInfo pinInfo);

	/**
	 * diozero internal method to generate a unique key for the I2C device at the
	 * specified address attached to the specified I2C bus controller.
	 *
	 * @param controller the I2C bus controller number
	 * @param address    the I2C device address
	 * @return a unique I2C key
	 */
	String createI2CKey(int controller, int address);

	/**
	 * diozero internal method to generate a unique key for the SPI device attached
	 * to the specified SPI controller and chip select.
	 *
	 * @param controller the SPI controller number
	 * @param chipSelect the SPI chip select number
	 * @return a unique SPI key
	 */
	String createSpiKey(int controller, int chipSelect);

	/**
	 * diozero internal method to generate a unique key for the specified serial
	 * device.
	 *
	 * @param deviceFilename the serial device filename
	 * @return a unique serial key
	 */
	String createSerialKey(String deviceFilename);

	/**
	 * Get the already provisioned device for the specified key
	 *
	 * @param key the unique device key
	 * @param <T> derived device type to return
	 * @return the device otherwise null if not found
	 */
	<T extends InternalDeviceInterface> T getDevice(String key);

	/**
	 * Check to see whether the requested device is already opened or not. If so, throws the appropriate exception.
	 *
	 * @param keySupplier creates the device key
	 * @param creator     whatever actually makes the "device"
	 * @param <T>         the type of internal device
	 * @return the "device"
	 */
	default <T extends InternalDeviceInterface> T registerDevice(Supplier<String> keySupplier,
																 Function<String, T> creator) {
		String key = keySupplier.get();

		if (isDeviceOpened(key)) throw new DeviceAlreadyOpenedException("Device '" + key + " is already opened");
		T device = creator.apply(key);
		deviceOpened(device);
		return device;
	}

	/**
	 * Convenience registration for pin-based devices.
	 * @param pinInfo the info
	 * @param creator     whatever actually makes the "device"
	 * @param <T>         the type of internal device
	 * @return the "device"
	 */
	default <T extends InternalDeviceInterface> T registerPinDevice(PinInfo pinInfo, Function<String, T> creator) {
		if (pinInfo == null) {
			throw new NoSuchDeviceException("No such device - pinInfo was null");
		}
		return registerDevice(() -> createPinKey(pinInfo), creator);
	}
}
