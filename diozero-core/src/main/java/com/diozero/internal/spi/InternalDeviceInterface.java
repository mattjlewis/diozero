package com.diozero.internal.spi;

import com.diozero.api.DeviceInterface;

/**
 * Fundamental interface for all low-level device types.
 */
public interface InternalDeviceInterface extends DeviceInterface {
	/**
	 * Get the unique device identifier for this device
	 * 
	 * @return unique device identifier
	 */
	String getKey();

	/**
	 * Check if this device is open or closed
	 * 
	 * @return true if this device is open
	 */
	boolean isOpen();
}
