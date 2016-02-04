package com.diozero.internal.provider.wiringpi;

import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.GpioUtil;

public class WiringPiRawPerfTest {
	private static final int DEFAULT_ITERATIONS = 5_000_000;
	
	public static void main(String[] args) {
		if (args.length < 1) {
			System.out.println("Usage: " + WiringPiRawPerfTest.class.getName() + " <pin-number> [<iterations>]");
			System.exit(1);
		}
		
		final int pin = Integer.parseInt(args[0]);
		final int iterations = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_ITERATIONS;
		
		Gpio.wiringPiSetupGpio();
		if (GpioUtil.isExported(pin)) {
			GpioUtil.setDirection(pin, GpioUtil.DIRECTION_OUT);
		} else {
			GpioUtil.export(pin, GpioUtil.DIRECTION_OUT);
		}
		Gpio.pinMode(pin, Gpio.OUTPUT);

		for (int j=0; j<5; j++) {
			long start_nano = System.nanoTime();
			for (int i=0; i<iterations; i++) {
				Gpio.digitalWrite(pin, true);
				Gpio.digitalWrite(pin, false);
			}
			long duration_ns = (System.nanoTime() - start_nano);
			System.out.format("Duration for %d iterations: %.4fs%n",
					Integer.valueOf(iterations), Float.valueOf(((float)duration_ns) / 1000 / 1000 / 1000));
		}
		
		GpioUtil.unexport(pin);
	}
}
