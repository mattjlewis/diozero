package com.diozero;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import com.diozero.api.DigitalPinEvent;
import com.diozero.api.GpioPullUpDown;
import com.diozero.sandpit.SmoothedInputDevice;
import com.diozero.util.SleepUtil;

public class SmoothedInputTest implements Consumer<DigitalPinEvent> {
	private static final Logger logger = LogManager.getLogger(SmoothedInputTest.class);
	
	@Test
	public void test() {
		int pin = 1;
		float delay = 2;
		// 10 events in 1 second
		try (SmoothedInputDevice device = new SmoothedInputDevice(pin, GpioPullUpDown.NONE, 10, 1000)) {
			device.setConsumer(this);
			Runnable event_generator = new Runnable() {
				@Override
				public void run() {
					long nano_time = System.nanoTime();
					long now = System.currentTimeMillis();
					device.valueChanged(new DigitalPinEvent(pin, now, nano_time, true));
				}
			};
			
			// Generate 1 event every 50ms -> 20 events per second
			ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
			scheduler.scheduleAtFixedRate(event_generator, 50, 50, TimeUnit.MILLISECONDS);
			logger.debug("Sleeping for " + delay + "s");
			SleepUtil.sleepSeconds(delay);
			
			logger.debug("Stopping event generation and sleeping for " + delay + "s");
			scheduler.shutdown();
			SleepUtil.sleepSeconds(delay);
			
			// Generate 1 event every 50ms -> 20 events per second
			scheduler = Executors.newScheduledThreadPool(1);
			scheduler.scheduleAtFixedRate(event_generator, 50, 50, TimeUnit.MILLISECONDS);
			logger.debug("Restarting event generation and sleeping for " + delay + "s");
			SleepUtil.sleepSeconds(delay);
			scheduler.shutdown();
		} catch (IOException e) {
			logger.error("Error: " + e, e);
		}
	}

	@Override
	public void accept(DigitalPinEvent event) {
		logger.debug("accept(" + event + ")");
	}
}
