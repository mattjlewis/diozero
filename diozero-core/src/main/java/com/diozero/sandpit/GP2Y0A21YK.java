package com.diozero.sandpit;

import java.io.IOException;

import com.diozero.api.AnalogueInputDevice;
import com.diozero.api.DeviceFactoryHelper;
import com.diozero.internal.spi.AnalogueInputDeviceFactoryInterface;
import com.diozero.util.SleepUtil;

/**
 * Sharp GP2Y0A21YK distance sensor. http://www.sharpsma.com/webfm_send/1208
 * Range: 10 to 80 cm
 * Typical response time: 39 ms
 * Typical start up delay: 44 ms
 * Average Current Consumption: 30 mA
 * Detection Area Diameter @ 80 cm: 12 cm
 */
public class GP2Y0A21YK extends AnalogueInputDevice {
	
	public GP2Y0A21YK(int pinNumber) throws IOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), pinNumber);
	}
	
	public GP2Y0A21YK(AnalogueInputDeviceFactoryInterface deviceFactory, int pinNumber) throws IOException {
		super(deviceFactory, pinNumber);
		SleepUtil.sleepMillis(44);
	}
	
	public double getDistance() throws IOException {
		float v = getValue();
		return 16.2537 * Math.pow(v, 4) - 129.893 * Math.pow(v, 3) + 382.268 * Math.pow(v, 2) - 512.611 * v + 306.439;
	}
}
