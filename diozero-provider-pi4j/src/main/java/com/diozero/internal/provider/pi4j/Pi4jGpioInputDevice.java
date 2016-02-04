package com.diozero.internal.provider.pi4j;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.api.DigitalPinEvent;
import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;
import com.diozero.internal.spi.*;
import com.pi4j.io.gpio.*;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
import com.pi4j.wiringpi.GpioUtil;

public class Pi4jGpioInputDevice extends AbstractDevice implements GpioDigitalInputDeviceInterface, GpioPinListenerDigital {
	private static final Logger logger = LogManager.getLogger(Pi4jGpioInputDevice.class);

	private GpioPinDigitalInput digitalInputPin;
	private int pinNumber;
	private InternalPinListener listener;
	
	Pi4jGpioInputDevice(String key, DeviceFactoryInterface deviceFactory, GpioController gpioController,
			int pinNumber, GpioPullUpDown pud, GpioEventTrigger trigger) {
		super(key, deviceFactory);
		
		Pin pin = RaspiGpioBcm.getPin(pinNumber);
		if (pin == null) {
			throw new IllegalArgumentException("Illegal pin number: " + pinNumber);
		}
		
		this.pinNumber = pinNumber;
		
		PinPullResistance ppr;
		switch (pud) {
		case PULL_DOWN:
			ppr = PinPullResistance.PULL_DOWN;
			break;
		case PULL_UP:
			ppr = PinPullResistance.PULL_UP;
			break;
		case NONE:
		default:
			ppr = PinPullResistance.OFF;
		}
		
		PinEdge edge;
		switch (trigger) {
		case FALLING:
			edge = PinEdge.FALLING;
			break;
		case RISING:
			edge = PinEdge.RISING;
			break;
		case NONE:
			edge = PinEdge.NONE;
			break;
		case BOTH:
		default:
			edge = PinEdge.BOTH;
			break;
		}
		
		// Note configuring GPIO event trigger values (rising / falling / both) via the provision APIs isn't possible in Pi4j
		digitalInputPin = gpioController.provisionDigitalInputPin(pin, "Digital Input for BCM GPIO " + pinNumber, ppr);
		
		// RaspiGpioProvider.export() calls this for all input pins:
		GpioUtil.setEdgeDetection(pin.getAddress(), PinEdge.BOTH.getValue());
		//GpioUtil.setEdgeDetection(pin.getAddress(), edge.getValue());
	}

	@Override
	public void closeDevice() {
		logger.debug("closeDevice()");
		removeListener();
		digitalInputPin.removeAllTriggers();
		digitalInputPin.unexport();
	}

	@Override
	public boolean getValue() {
		return digitalInputPin.getState().isHigh();
	}

	@Override
	public int getPin() {
		return pinNumber;
	}
	
	@Override
	public void setDebounceTimeMillis(int debounceTime) {
		digitalInputPin.setDebounce(debounceTime);
	}

	@Override
	public void handleGpioPinDigitalStateChangeEvent(GpioPinDigitalStateChangeEvent event) {
		long nano_time = System.nanoTime();
		if (listener != null) {
			listener.valueChanged(new DigitalPinEvent(
					pinNumber, System.currentTimeMillis(), nano_time, event.getState().isHigh()));
		}
	}

	@Override
	public void setListener(InternalPinListener listener) {
		digitalInputPin.removeAllListeners();
		this.listener = listener;
		digitalInputPin.addListener(this);
	}
	
	@Override
	public void removeListener() {
		digitalInputPin.removeAllListeners();
		this.listener = null;
	}
}
