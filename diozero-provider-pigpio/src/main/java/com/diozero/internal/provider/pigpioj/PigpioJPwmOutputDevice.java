package com.diozero.internal.provider.pigpioj;

import com.diozero.api.RuntimeIOException;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.DeviceFactoryInterface;
import com.diozero.internal.spi.PwmOutputDeviceInterface;

import uk.pigpioj.PigpioInterface;

public class PigpioJPwmOutputDevice extends AbstractDevice implements PwmOutputDeviceInterface {
	private PigpioInterface pigpioImpl;
	private int gpio;
	private int range;

	public PigpioJPwmOutputDevice(String key, DeviceFactoryInterface deviceFactory, PigpioInterface pigpioImpl,
			int gpio, float initialValue, int range) {
		super(key, deviceFactory);

		this.pigpioImpl = pigpioImpl;
		this.gpio = gpio;
		this.range = range;
		
		setValue(initialValue);
	}

	@Override
	protected void closeDevice() {
		// TODO Nothing to do?
	}

	@Override
	public int getGpio() {
		return gpio;
	}
	
	@Override
	public int getPwmNum() {
		return gpio;
	}

	@Override
	public float getValue() throws RuntimeIOException {
		int dc = pigpioImpl.getPWMDutyCycle(gpio);
		if (dc < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.getPWMDutyCycle(), response: " + dc);
		}
		
		return dc / (float) range;
	}

	@Override
	public void setValue(float value) throws RuntimeIOException {
		int rc = pigpioImpl.setPWMDutyCycle(gpio, Math.round(range * value));
		if (rc < 0) {
			throw new RuntimeIOException("Error calling pigpioImpl.setPWMDutyCycle(), response: " + rc);
		}
	}
}
