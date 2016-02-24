package com.diozero.sandpit;

import java.util.List;

import org.pmw.tinylog.Logger;

import com.diozero.sandpit.W1ThermSensor.Type;

public class W1ThermSensorTest {
	
	public static void main(String[] args) {
		System.out.println(Type.valueOf(Type.DS1822.getId()));
		System.out.println(Type.valueOf(Type.DS1822.name()));
		
		List<W1ThermSensor> sensors = W1ThermSensor.getAvailableSensors("src/test/resources/devices");
		System.out.println(sensors);
		for (W1ThermSensor sensor : sensors) {
			Logger.debug("Type=" + sensor.getType());
			Logger.debug("Serial number=" + sensor.getSerialNumber());
			if (sensor.getSerialNumber().startsWith("789")) {
				Logger.debug("Temperature={}", Float.valueOf(sensor.getTemperature()));
			}
		}
	}
}
