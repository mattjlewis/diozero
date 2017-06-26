package com.diozero.sampleapps;

import com.diozero.devices.MCP23008;
import com.diozero.devices.PwmLed;
import com.diozero.internal.provider.PwmOutputDeviceFactoryInterface;
import com.diozero.util.DeviceFactoryHelper;
import com.diozero.util.SleepUtil;

public class SoftwarePwmOutputTest {
	public static void main(String[] args) {
		try (PwmOutputDeviceFactoryInterface device_factory = new MCP23008();
				PwmLed led0 = new PwmLed(device_factory, 0);
				PwmLed led1 = new PwmLed(device_factory, 1)) {
			led0.on();
			led1.on();
			SleepUtil.sleepSeconds(2);
			
			led0.off();
			led1.off();
			SleepUtil.sleepSeconds(2);
			
			led0.setValue(0.5f);
			led1.setValue(0.5f);
			SleepUtil.sleepSeconds(5);
			
			led0.pulse();
			led1.pulse();
			SleepUtil.sleepSeconds(10);
		} finally {
			DeviceFactoryHelper.getNativeDeviceFactory().close();
		}
	}
}
