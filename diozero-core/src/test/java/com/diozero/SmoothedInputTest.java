package com.diozero;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.junit.Test;
import org.pmw.tinylog.Logger;

import com.diozero.api.DigitalPinEvent;
import com.diozero.api.GpioPullUpDown;
import com.diozero.sandpit.SmoothedInputDevice;
import com.diozero.util.SleepUtil;

public class SmoothedInputTest implements Consumer<DigitalPinEvent> {
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
			Logger.debug("Sleeping for {}s", Float.valueOf(delay));
			SleepUtil.sleepSeconds(delay);
			
			Logger.debug("Stopping event generation and sleeping for {}s", Float.valueOf(delay));
			scheduler.shutdown();
			SleepUtil.sleepSeconds(delay);
			
			// Generate 1 event every 50ms -> 20 events per second
			scheduler = Executors.newScheduledThreadPool(1);
			scheduler.scheduleAtFixedRate(event_generator, 50, 50, TimeUnit.MILLISECONDS);
			Logger.debug("Restarting event generation and sleeping for {}s", Float.valueOf(delay));
			SleepUtil.sleepSeconds(delay);
			scheduler.shutdown();
		} catch (IOException e) {
			Logger.error(e, "Error: {}", e);
		}
	}

	@Override
	public void accept(DigitalPinEvent event) {
		Logger.debug("accept({})", event);
	}
}
