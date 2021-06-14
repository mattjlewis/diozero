package com.diozero.sampleapps.sandpit;

import java.util.concurrent.Future;

import org.tinylog.Logger;

import com.diozero.util.Diozero;
import com.diozero.util.DiozeroScheduler;
import com.diozero.util.SleepUtil;

public class ShutdownTest implements AutoCloseable {
	public static void main(String[] args) {
		// Required to initialise tinylog
		Logger.debug("ShutdownTest");

		// Not normally required as this is invoked from DeviceFactoryHelper
		Diozero.initialiseShutdownHook();

		Object lock = new Object();

		try (ShutdownTest test = new ShutdownTest()) {
			Diozero.registerForShutdown(test);

			/*-
			Future<?> future = DiozeroScheduler.getDefaultInstance().scheduleAtFixedRate(
					() -> System.out.println("Run " + System.currentTimeMillis()), 1_000, 1_000, TimeUnit.MILLISECONDS);
			*/
			Future<?> future = DiozeroScheduler.getDefaultInstance().submit(() -> {
				try {
					while (!Thread.interrupted()) {
						long now_ms = System.currentTimeMillis();
						System.out.println("Run " + now_ms);
						Thread.sleep(0, 1);
						SleepUtil.busySleep(100_000_000);
					}
				} catch (InterruptedException e) {
					System.out.println("Interrupted in loop: " + e);
					e.printStackTrace(System.out);
					Thread.currentThread().interrupt();
				}
				System.out.println("Inner loop DONE");
			});

			System.out.println("Waiting...");

			// Diozero.waitForShutdown();
			// System.out.println("!!!! After waitfForShutdown...");

			future.get();
			System.out.println("!!!! After future.get()");

			/*-
			synchronized (lock) {
				lock.wait();
			}
			System.out.println("!!!! After lock.wait()");
			*/
		} catch (Throwable t) {
			System.out.println("Error in main: " + t);
			t.printStackTrace(System.out);
		} finally {
			System.out.println("Calling Diozero.shutdown()...");
			Diozero.shutdown();
		}
		System.out.println("Done.");
	}

	@Override
	public void close() {
		System.out.println("close() - START");
		try {
			System.out.println("close - sleeping");
			Thread.sleep(100);
		} catch (InterruptedException e) {
			System.out.println("Error: " + e);
		}
		System.out.println("close() - DONE");
	}
}
