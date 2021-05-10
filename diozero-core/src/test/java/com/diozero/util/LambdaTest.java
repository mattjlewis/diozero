package com.diozero.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.tinylog.Logger;

public class LambdaTest {
	public static void main(String[] args) {
		Consumer<Void> consumer = (Void v) -> System.out.println("Hello consumer");

		consumer.accept(null);

		Command c = () -> System.out.println("Hello lambda");
		c.action();

		carryOutWork(() -> System.out.println("In here 1"));

		LambdaTest t = new LambdaTest();

		Logger.info("Using local invoke utility");
		for (int i = 0; i < 10; i++) {
			invoke(t::getValue, t::setValue);
			SleepUtil.sleepSeconds(1);
		}

		Logger.info("Using DioZeroScheduler.invokeAtFixedRate()");
		ScheduledFuture<?> future1 = DiozeroScheduler.getNonDaemonInstance().invokeAtFixedRate(t::getValue, t::setValue,
				100, 1000, TimeUnit.MILLISECONDS);
		for (int i = 0; i < 10; i++) {
			SleepUtil.sleepSeconds(1);
		}
		future1.cancel(true);

		Logger.info("Using ScheduledExecutorService.scheduleAtFixedRate()");
		ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
		ScheduledFuture<?> future2 = executor.scheduleAtFixedRate(t::getValue, 100, 1000, TimeUnit.MILLISECONDS);
		for (int i = 0; i < 10; i++) {
			SleepUtil.sleepSeconds(1);
		}
		future2.cancel(true);
		executor.shutdownNow();
	}

	static void invoke(Supplier<Float> source, Consumer<Float> sink) {
		sink.accept(source.get());
	}

	public float getValue() {
		System.out.println("getValue() called");
		return ThreadLocalRandom.current().nextFloat();
	}

	public void setValue(float f) {
		System.out.println("setValue(" + f + ")");
	}

	interface Command extends Consumer<Void> {
		@Override
		default void accept(Void v) {
			action();
		}

		void action();
	}

	public static void carryOutWork(SimpleFuncInterface sfi) {
		sfi.abc();
	}
}

@FunctionalInterface
interface SimpleFuncInterface {
	void abc();
}
