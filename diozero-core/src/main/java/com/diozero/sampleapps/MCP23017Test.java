package com.diozero.sampleapps;

import java.io.IOException;
import java.util.function.Consumer;

import com.diozero.LED;
import com.diozero.MCP23017;
import com.diozero.api.*;
import com.diozero.util.SleepUtil;

public class MCP23017Test implements Consumer<DigitalPinEvent> {
	public static void main(String[] args) {
		new MCP23017Test().test();
	}
	
	private LED led;
	
	public MCP23017Test() {
	}
	
	public void test() {
		try (MCP23017 mcp23017 = new MCP23017(21, 20)) {
			try (DigitalInputDevice button = mcp23017.provisionDigitalInputDevice(0, GpioPullUpDown.PULL_UP, GpioEventTrigger.BOTH)) {
				led = new LED(mcp23017.provisionDigitalOutputPin(1, false), true);
				button.setConsumer(this);
				System.out.println("Sleeping for 20s");
				SleepUtil.sleepSeconds(10);
				
				SleepUtil.sleepSeconds(1);
				
				System.out.println("On");
				led.on();
				SleepUtil.sleepSeconds(1);
				
				System.out.println("Off");
				led.off();
				SleepUtil.sleepSeconds(1);
				
				System.out.println("Blink");
				led.blink(0.5f, 0.5f, 10, false);
				
				System.out.println("Done");
			} finally {
				if (led != null) { led.close(); }
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void accept(DigitalPinEvent event) {
		System.out.println("accept(" + event + ")");
		if (led != null) {
			try { led.setValue(!event.getValue()); } catch (IOException e) { System.out.println("Error: " + e); }
		}
	}
}
