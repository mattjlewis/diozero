package com.diozero.api;

import java.util.EnumSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.diozero.sbc.BoardInfo;
import com.diozero.sbc.DeviceFactoryHelper;

@SuppressWarnings("static-method")
public class PwmOutputDeviceTest {
	private static final int DIGITAL_GPIO = 10;
	private static final int PWM_GPIO = 12;

	@BeforeAll
	public static void beforeAll() {
		BoardInfo board_info = DeviceFactoryHelper.getNativeDeviceFactory().getBoardInfo();

		board_info.addGpioPinInfo(DIGITAL_GPIO, DIGITAL_GPIO, EnumSet.of(DeviceMode.DIGITAL_OUTPUT));
		board_info.addGpioPinInfo(PWM_GPIO, PWM_GPIO, EnumSet.of(DeviceMode.PWM_OUTPUT));
	}

	@Test
	public void testPwm() {
		try (PwmOutputDevice device = PwmOutputDevice.Builder.builder(PWM_GPIO).build()) {
			device.setValue(0);
			Assertions.assertEquals(0, device.getValue());

			device.setValue(1);
			Assertions.assertEquals(1, device.getValue());
		}
	}

	@Test
	public void testSoftwarePwm() {
		try (PwmOutputDevice device = PwmOutputDevice.Builder.builder(DIGITAL_GPIO).build()) {
			device.setValue(0);
			Assertions.assertEquals(0, device.getValue());

			device.setValue(1);
			Assertions.assertEquals(1, device.getValue());
		}
	}
}
