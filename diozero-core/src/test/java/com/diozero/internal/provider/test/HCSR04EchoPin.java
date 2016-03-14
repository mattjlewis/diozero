package com.diozero.internal.provider.test;

import java.io.IOException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.pmw.tinylog.Logger;

import com.diozero.api.DigitalInputEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.internal.spi.AbstractInputDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.GpioDigitalInputDeviceInterface;
import com.diozero.util.DioZeroScheduler;
import com.diozero.util.RuntimeIOException;

public class HCSR04EchoPin extends AbstractInputDevice<DigitalInputEvent>
implements GpioDigitalInputDeviceInterface, Runnable {
	private static final Random random = new Random(System.nanoTime());
	private static HCSR04EchoPin instance;
	
	static HCSR04EchoPin getInstance() {
		return instance;
	}
	
	private int pinNumber;
	private Lock lock;
	private Condition cond;
	private AtomicBoolean value;
	private long echoStart;
	
	public HCSR04EchoPin(String key, DeviceFactoryInterface deviceFactory,
			int pinNumber, GpioPullUpDown pud, GpioEventTrigger trigger) {
		super(key, deviceFactory);
		
		this.pinNumber = pinNumber;
		
		instance = this;
		
		lock = new ReentrantLock();
		cond = lock.newCondition();
		value = new AtomicBoolean();
		
		// Start the background process to send the echo low signal
		DioZeroScheduler.getDaemonInstance().execute(this);
	}
	
	@Override
	public int getPin() {
		return pinNumber;
	}

	@Override
	public boolean getValue() throws RuntimeIOException {
		return value.get();
	}

	@Override
	public void setDebounceTimeMillis(int debounceTime) {
	}

	@Override
	protected void closeDevice() throws IOException {
	}
	
	void doEcho(long start) {
		try {
			Logger.debug("doEcho()");
			
			if (System.currentTimeMillis() - start < 50) {
				Thread.sleep(random.nextInt(50));
			}
			echoStart = System.currentTimeMillis();
			value.set(true);
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
				if ((System.currentTimeMillis() - echoStart) < 2) {
					Thread.sleep(0, random.nextInt(999_999));
				}
				value.set(false);
				valueChanged(new DigitalInputEvent(pinNumber, System.currentTimeMillis(), System.nanoTime(), false));
				Logger.debug("Time to send echo high then low=" + (System.currentTimeMillis() - echoStart) + "ms");
			}
		} catch (InterruptedException e) {
			Logger.warn(e, "Interrupted: {}", e);
		}
	}
}
