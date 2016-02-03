package com.diozero.sampleapps;

import java.io.IOException;
import java.util.function.Consumer;

import com.diozero.Button;
import com.diozero.api.DigitalPinEvent;
import com.diozero.api.GpioPullUpDown;
import com.diozero.util.SleepUtil;

public class ButtonTest implements Consumer<DigitalPinEvent> {
	public static void main(String[] args) {
		new ButtonTest().test();
	}
	
	public void test() {
		try (Button button = new Button(12, GpioPullUpDown.PULL_UP)) {
			button.setConsumer(this);
			System.out.println("Sleeping for 10s, thread name=" + Thread.currentThread().getName());
			SleepUtil.sleepSeconds(10);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	@Override
	public void accept(DigitalPinEvent event) {
		System.out.println("accept(" + event + "), thread name=" + Thread.currentThread().getName());
	}
}
