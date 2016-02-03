package com.diozero.internal.provider.pigpioj;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.diozero.api.DigitalPinEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.internal.spi.*;
import com.diozero.pigpioj.PigpioCallback;
import com.diozero.pigpioj.PigpioGpio;

public class PigpioJDigitalInputDevice extends AbstractDevice implements GpioDigitalInputDeviceInterface, PigpioCallback {
	private static final Logger logger = Logger.getLogger(PigpioJDigitalInputDevice.class.getName());
	
	private int pinNumber;
	private int edge;
	private InternalPinListener listener;

	public PigpioJDigitalInputDevice(String key, DeviceFactoryInterface deviceFactory, int pinNumber,
			GpioPullUpDown pud, GpioEventTrigger trigger) throws IOException {
		super(key, deviceFactory);
		
		switch (trigger) {
		case RISING:
			edge = PigpioGpio.RISING_EDGE;
			break;
		case FALLING:
			edge = PigpioGpio.FALLING_EDGE;
			break;
		case BOTH:
			edge = PigpioGpio.EITHER_EDGE;
			break;
		case NONE:
		default:
			edge = PigpioGpio.NO_EDGE;
		}
		
		int pigpio_pud;
		switch (pud) {
		case PULL_DOWN:
			pigpio_pud = PigpioGpio.PI_PUD_DOWN;
			break;
		case PULL_UP:
			pigpio_pud = PigpioGpio.PI_PUD_UP;
			break;
		case NONE:
		default:
			pigpio_pud = PigpioGpio.PI_PUD_OFF;
			break;
		}
		
		PigpioGpio.setMode(pinNumber, PigpioGpio.MODE_PI_INPUT);
		PigpioGpio.setPullUpDown(pinNumber, pigpio_pud);
		
		this.pinNumber = pinNumber;
	}

	@Override
	public int getPin() {
		return pinNumber;
	}

	@Override
	public void closeDevice() throws IOException {
		// No GPIO close method in pigpio
		removeListener();
	}

	@Override
	public boolean getValue() throws IOException {
		return PigpioGpio.read(pinNumber);
	}

	@Override
	public void setDebounceTimeMillis(int debounceTime) {
		throw new UnsupportedOperationException("Debounce not supported in pigpioj");
	}

	@Override
	public void setListener(InternalPinListener listener) {
		if (edge == PigpioGpio.NO_EDGE) {
			logger.warning("Edge was configured to be None, no point adding a listener");
			return;
		}
		
		if (this.listener != null) {
			removeListener();
		}
		
		this.listener = listener;
		try {
			PigpioGpio.setISRFunc(pinNumber, edge, -1, this);
		} catch (IOException e) {
			logger.log(Level.SEVERE, "Error setting listener: " + e, e);
		}
	}

	@Override
	public void removeListener() {
		try {
			PigpioGpio.setISRFunc(pinNumber, PigpioGpio.EITHER_EDGE, -1, null);
		} catch (IOException e) {
			logger.log(Level.WARNING, "Error removing listener: " + e, e);
		}
		listener = null;
	}

	@Override
	public void callback(int pin, boolean value, long time) {
		long nanos = System.nanoTime();
		logger.info("callback(" + pin + ", " + value + ", " + time + ")");
		
		if (pin != pinNumber) {
			logger.severe("Error, got a callback for the wrong pin (" + pin + "), was expecting " + pinNumber);
		}
		
		if (listener != null) {
			listener.valueChanged(new DigitalPinEvent(pin, time, nanos, value));
		}
	}
}
