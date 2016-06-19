package com.diozero.sandpit;

import org.pmw.tinylog.Logger;

import com.diozero.util.PollNative;

public class PollTest {
	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Usage: " + PollTest.class.getName() + " <pin-number>");
			return;
		}
		
		int pin = Integer.parseInt(args[0]);
		
		test(pin);
	}
	
	private static void test(int pinNumber) {
		PollNative pn = new PollNative();
		Logger.info("Calling poll()");
		pn.poll("/sys/class/gpio/gpio" + pinNumber + "/value", -1, pinNumber,
				(ref, epochTime)->Logger.info("notify(" + ref + ", " + epochTime + ")"));
	}
}
