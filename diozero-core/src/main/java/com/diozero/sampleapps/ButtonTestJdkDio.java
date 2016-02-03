package com.diozero.sampleapps;

import java.io.IOException;

import com.diozero.util.SleepUtil;

import jdk.dio.DeviceConfig;
import jdk.dio.DeviceManager;
import jdk.dio.gpio.*;

public class ButtonTestJdkDio implements PinListener {
	public static void main(String[] args) {
		new ButtonTestJdkDio().test(12);
	}
	
	public void test(int pinNumber) {
		GPIOPinConfig pin_config = new GPIOPinConfig.Builder().setControllerNumber(DeviceConfig.UNASSIGNED).setPinNumber(pinNumber).
			setDirection(GPIOPinConfig.DIR_INPUT_ONLY).setDriveMode(GPIOPinConfig.MODE_INPUT_PULL_UP).setTrigger(GPIOPinConfig.TRIGGER_BOTH_EDGES).build();
		try (GPIOPin pin = DeviceManager.open(GPIOPin.class, pin_config)) {
			pin.setInputListener(this);
			System.out.println("Waiting 20s for events..., thread name=" + Thread.currentThread().getName());
			SleepUtil.sleepSeconds(20);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void valueChanged(PinEvent event) {
		System.out.println("valueChanged(" + event.getValue() + "), thread name=" + Thread.currentThread().getName());
	}
}
