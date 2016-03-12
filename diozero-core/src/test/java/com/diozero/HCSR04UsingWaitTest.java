package com.diozero;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.Before;
import org.junit.Test;
import org.pmw.tinylog.Logger;

import com.diozero.api.DigitalInputEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.internal.provider.test.TestDeviceFactory;
import com.diozero.internal.spi.*;
import com.diozero.sandpit.HCSR04UsingWait;
import com.diozero.util.DioZeroScheduler;
import com.diozero.util.RuntimeIOException;
import com.diozero.util.SleepUtil;

@SuppressWarnings("static-method")
public class HCSR04UsingWaitTest {
	private static HCSR04EchoPin echoPin;
	private static final Random random = new Random(System.nanoTime());
	private static long start;
	private static long echoStart;
	
	@Before
	public void setup() {
		TestDeviceFactory.setDigitalInputDeviceClass(HCSR04EchoPin.class);
		TestDeviceFactory.setDigitalOutputDeviceClass(HCSR04TriggerPin.class);
	}
	
	@Test
	public void test() {
		// Purely to prime the scheduler
		DioZeroScheduler.getDaemonInstance().execute(() -> Logger.info(""));
		try (HCSR04UsingWait hcsr04 = new HCSR04UsingWait(26, 4)) {
			for (int i=0; i<10; i++) {
				float distance = hcsr04.getDistanceCm();
				Logger.info("Distance={}", Float.valueOf(distance));
				SleepUtil.sleepSeconds(1);
			}
		}
	}
	
	public static class HCSR04EchoPin extends AbstractInputDevice<DigitalInputEvent> implements GpioDigitalInputDeviceInterface, Runnable {
		private int pinNumber;
		private Lock lock;
		private Condition cond;
		
		public HCSR04EchoPin(String key, DeviceFactoryInterface deviceFactory,
				int pinNumber, GpioPullUpDown pud, GpioEventTrigger trigger) {
			super(key, deviceFactory);
			
			this.pinNumber = pinNumber;
			
			echoPin = this;
			
			lock = new ReentrantLock();
			cond = lock.newCondition();
			
			// Start the background process to send the echo low signal
			DioZeroScheduler.getDaemonInstance().execute(this);
		}
		
		@Override
		public int getPin() {
			return pinNumber;
		}

		@Override
		public boolean getValue() throws RuntimeIOException {
			return false;
		}

		@Override
		public void setDebounceTimeMillis(int debounceTime) {
		}

		@Override
		protected void closeDevice() throws IOException {
		}
		
		private void doEcho() {
			try {
				Logger.debug("doEcho()");
				
				if (System.currentTimeMillis() - start < 50) {
					Thread.sleep(random.nextInt(50));
				}
				echoStart = System.currentTimeMillis();
				valueChanged(new DigitalInputEvent(pinNumber, echoStart, System.nanoTime(), true));
				// Need to send echo low in a separate thread
				lock.lock();
				try {
					cond.signalAll();
				} finally {
					lock.unlock();
				}
			} catch (InterruptedException e) {
				Logger.warn(e, "Interrupted: {}", e);
			}
		}

		@Override
		public void run() {
			try {
				while (true) {
					lock.lock();
					try {
						cond.await();
					} finally {
						lock.unlock();
					}
//					if ((System.currentTimeMillis() - echoStart) < 2) {
//						Thread.sleep(random.nextInt(2), random.nextInt(1000));
//					}
					valueChanged(new DigitalInputEvent(pinNumber, System.currentTimeMillis(), System.nanoTime(), false));
					Logger.debug("Time to send echo high then low=" + (System.currentTimeMillis() - echoStart) + "ms");
				}
			} catch (InterruptedException e) {
				Logger.warn(e, "Interrupted: {}", e);
			}
		}
	}
	
	public static class HCSR04TriggerPin extends AbstractDevice implements GpioDigitalOutputDeviceInterface, Runnable {
		private int pinNumber;
		private boolean value;
		
		public HCSR04TriggerPin(String key, DeviceFactoryInterface deviceFactory,
				int pinNumber, boolean initialValue) {
			super(key, deviceFactory);
			
			this.pinNumber = pinNumber;
			this.value = initialValue;
		}
		
		@Override
		public int getPin() {
			return pinNumber;
		}

		@Override
		public boolean getValue() throws RuntimeIOException {
			return value;
		}
		
		@Override
		public void setValue(boolean value) throws RuntimeIOException {
			this.value = value;

			if (! value) {
				start = System.currentTimeMillis();
				DioZeroScheduler.getDaemonInstance().execute(this);
			}
		}
		
		@Override
		public void run() {
			Logger.debug("run()");
			SleepUtil.sleepMillis(50);
			echoPin.doEcho();
		}

		@Override
		protected void closeDevice() throws IOException {
		}
	}
}
