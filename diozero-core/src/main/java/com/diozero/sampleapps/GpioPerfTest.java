package com.diozero.sampleapps;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.api.DigitalOutputDevice;

public class GpioPerfTest {
	private static final Logger logger = LogManager.getLogger(GpioPerfTest.class);
	
	private static final int ITERATIONS = 100_000;

	public static void main(String[] args) {
		if (args.length < 1) {
			logger.error("Usage: " + GpioPerfTest.class.getName() + " <pin-number> [<iterations>]");
			System.exit(1);
		}
		
		int pin = Integer.parseInt(args[0]);
		
		int iterations = ITERATIONS;
		if (args.length > 1) {
			iterations = Integer.parseInt(args[1]);
		}
		
		try (DigitalOutputDevice gpio = new DigitalOutputDevice(pin)) {
			for (int j=0; j<5; j++) {
				long start_nano = System.nanoTime();
				for (int i=0; i<iterations; i++) {
					gpio.setValue(true);
					gpio.setValue(false);
				}
				long duration_ns = System.nanoTime() - start_nano;
				
				logger.info(String.format("Duration for %d iterations: %.4fs",
						Integer.valueOf(iterations), Float.valueOf(((float)duration_ns) / 1000 / 1000 / 1000)));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
