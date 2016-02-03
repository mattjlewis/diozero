package com.diozero.api;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.internal.spi.DeviceInterface;

public class DeviceStates {
	private static final Logger logger = LogManager.getLogger(DeviceStates.class);
	
	private Map<String, DeviceInterface> devices;
	
	public DeviceStates() {
		devices = new ConcurrentHashMap<>();
	}

	public boolean isOpened(String key) {
		return devices.containsKey(key);
	}
	
	public void opened(DeviceInterface device) {
		devices.put(device.getKey(), device);
	}
	
	public void closed(DeviceInterface device) {
		logger.debug("closed(" + device.getKey() + ")");
		devices.remove(device.getKey());
	}
	
	public void closeAll() {
		logger.debug("closeAll()");
		for (DeviceInterface device : devices.values()) {
			// No need to remove from the Map as close() should always call closed()
			try { device.close(); } catch (IOException e) { }
		}
	}

	public DeviceInterface getDevice(String key) {
		return devices.get(key);
	}
}
