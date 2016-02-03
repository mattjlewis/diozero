package com.diozero.api;

import java.io.IOException;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.diozero.internal.spi.GpioDeviceFactoryInterface;
import com.diozero.internal.spi.GpioDigitalInputDeviceInterface;
import com.diozero.internal.spi.InternalPinListener;

/**
 * Represents a generic input device.
 * 
 */
public class DigitalInputDevice extends GpioDevice implements InternalPinListener {
	private static final Logger logger = LogManager.getLogger(DigitalInputDevice.class);
	
	private Consumer<DigitalPinEvent> consumer;
	protected boolean activeHigh;
	protected GpioDigitalInputDeviceInterface device;

	public DigitalInputDevice(int pinNumber, GpioPullUpDown pud, GpioEventTrigger trigger) throws IOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), pinNumber, pud, trigger);
	}

	public DigitalInputDevice(GpioDeviceFactoryInterface deviceFactory, int pinNumber, GpioPullUpDown pud, GpioEventTrigger trigger) throws IOException {
		this(deviceFactory.provisionDigitalInputPin(pinNumber, pud, trigger), pud != GpioPullUpDown.PULL_DOWN);
	}

	public DigitalInputDevice(GpioDigitalInputDeviceInterface device, boolean activeHigh) {
		super(device.getPin());
		
		this.device = device;
		this.activeHigh = activeHigh;
	}

	@Override
	public void close() {
		logger.debug("close()");
		device.close();
	}

	public boolean getValue() throws IOException {
		return device.getValue();
	}
	
	public boolean isActive() throws IOException {
		return device.getValue() == activeHigh;
	}

	public void setConsumer(Consumer<DigitalPinEvent> consumer) {
		device.setListener(this);
		this.consumer = consumer;
	}

	@Override
	public void valueChanged(DigitalPinEvent event) {
		if (consumer != null) {
			consumer.accept(event);
		}
	}
}
