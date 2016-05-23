package com.diozero.sampleapps;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

import org.pmw.tinylog.Logger;

import com.diozero.api.DigitalInputDevice;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.sandpit.MotionSensor;
import com.diozero.util.SleepUtil;

public class MotionTest implements Closeable {
	public static void main(String[] args) {
		try (MotionTest test = new MotionTest(13, 19, 26)) {
			Logger.info("Sleeping for 60s");
			SleepUtil.sleepSeconds(60);
		}
	}
	
	private List<DigitalInputDevice> sensors;
	
	@SuppressWarnings("resource")
	private MotionTest(int... pins) {
		sensors = new ArrayList<>();
		
		for (int pin : pins) {
			//DigitalInputDevice sensor = new MotionSensor(pin);
			DigitalInputDevice sensor;
			if (pin == 26) {
				// Fudge for this strange type of PIR:
				// http://skpang.co.uk/catalog/pir-motion-sensor-p-796.html
				// Red (5V) / White (Ground) / Black (open collector Alarm)
				// The alarm pin is an open collector meaning you will need a pull up resistor on the alarm pin
				// Signal motion if there are 5 or more alarms in a 200ms period, check every 50ms
				sensor = new MotionSensor(pin, GpioPullUpDown.PULL_UP, 5, 200, 50);
			} else {
				sensor = new DigitalInputDevice(pin, GpioPullUpDown.PULL_DOWN, GpioEventTrigger.BOTH);
			}
			Logger.info("Created sensor on pin " + pin + " pud=" + sensor.getPullUpDown() + ", trigger=" + sensor.getTrigger());
			sensor.whenActivated(() ->System.out.println("Pin " + pin + " activated"));
			sensor.whenDeactivated(() ->System.out.println("Pin " + pin + " deactivated"));
			sensors.add(sensor);
		}
	}

	@Override
	public void close() {
		for (DigitalInputDevice sensor : sensors) {
			sensor.close();
		}
	}
}
