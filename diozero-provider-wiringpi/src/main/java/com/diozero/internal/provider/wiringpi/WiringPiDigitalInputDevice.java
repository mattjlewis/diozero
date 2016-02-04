package com.diozero.internal.provider.wiringpi;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.api.DigitalPinEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.internal.spi.*;
import com.pi4j.wiringpi.Gpio;
import com.pi4j.wiringpi.GpioInterruptCallback;
import com.pi4j.wiringpi.GpioUtil;

public class WiringPiDigitalInputDevice extends AbstractDevice implements GpioDigitalInputDeviceInterface {
	private static final Logger logger = LogManager.getLogger(WiringPiDigitalInputDevice.class);

	private int pinNumber;
	private GpioEventTrigger trigger;
	
	public WiringPiDigitalInputDevice(String key, DeviceFactoryInterface deviceFactory, int pinNumber,
			GpioPullUpDown pud, GpioEventTrigger trigger) throws IOException {
		super(key, deviceFactory);
		
		this.pinNumber = pinNumber;
		this.trigger = trigger;
		int edge;
		switch (trigger) {
		case RISING:
			edge = Gpio.INT_EDGE_RISING;
			break;
		case FALLING:
			edge = Gpio.INT_EDGE_FALLING;
			break;
		case BOTH:
		default:
			edge = Gpio.INT_EDGE_BOTH;
			break;
		}
		
		try {
			// Note calling this method will automatically export the pin and set the pin direction to INPUT
			if (!GpioUtil.setEdgeDetection(pinNumber, edge)) {
				throw new IOException("Error setting edge detection (" + edge + ") for pin " + pinNumber);
			}
		} catch (RuntimeException re) {
			throw new IOException(re);
		}
	
		int wpi_pud;
		switch (pud) {
		case PULL_DOWN:
			wpi_pud = Gpio.PUD_DOWN;
			break;
		case PULL_UP:
			wpi_pud = Gpio.PUD_UP;
			break;
		case NONE:
		default:
			wpi_pud = Gpio.PUD_OFF;
			break;
		}
		Gpio.pullUpDnControl(pinNumber, wpi_pud);
	}

	@Override
	public void closeDevice() {
		logger.debug("closeDevice()");
		removeListener();
		GpioUtil.unexport(pinNumber);
	}

	@Override
	public boolean getValue() throws IOException {
		return Gpio.digitalRead(pinNumber) == 1;
	}

	@Override
	public int getPin() {
		return pinNumber;
	}

	@Override
	public void setDebounceTimeMillis(int debounceTime) {
		// TODO Support software debounce...
	}

	@Override
	public void setListener(InternalPinListener listener) {
		// TODO Validate that wiringPi actually works this way, i.e. supports separate callbacks for rising and falling
		// Note it looks easier to use GpioInterrupt but this will be less efficient
		// due to its use of Vectors to support multiple listeners and cloning on every event
		if (trigger == GpioEventTrigger.BOTH || trigger == GpioEventTrigger.RISING) {
			Gpio.wiringPiISR(pinNumber, Gpio.INT_EDGE_RISING, new InterruptCallback(true, listener));
		}
		if (trigger == GpioEventTrigger.BOTH || trigger == GpioEventTrigger.FALLING) {
			Gpio.wiringPiISR(pinNumber, Gpio.INT_EDGE_FALLING, new InterruptCallback(false, listener));
		}
	}

	@Override
	public void removeListener() {
		// TODO What to do?
	}
}

class InterruptCallback implements GpioInterruptCallback {
	private boolean value;
	private InternalPinListener listener;
	
	public InterruptCallback(boolean value, InternalPinListener listener) {
		this.value = value;
		this.listener = listener;
	}

	@Override
	public void callback(int pin) {
		long nano_time = System.nanoTime();
		listener.valueChanged(new DigitalPinEvent(pin, System.currentTimeMillis(), nano_time, value));
	}
}
