package com.diozero.api;

import org.pmw.tinylog.Logger;

import com.diozero.internal.DeviceFactoryHelper;
import com.diozero.internal.spi.GpioDeviceFactoryInterface;
import com.diozero.internal.spi.GpioDeviceInterface;
import com.diozero.internal.spi.GpioDigitalInputOutputDeviceInterface;
import com.diozero.util.RuntimeIOException;

public class DigitalInputOutputDevice extends GpioDevice
implements DigitalInputDeviceInterface {
	protected GpioDigitalInputOutputDeviceInterface device;
	private GpioDeviceInterface.Mode mode;

	/**
	 * @param pinNumber
	 *            Pin number to which the device is connected.
	 * @param mode
	 *            Input or output {@link com.diozero.internal.spi.GpioDeviceInterface.Mode Mode}
	 * @throws RuntimeIOException
	 *             If an I/O error occurs.
	 */
	public DigitalInputOutputDevice(int pinNumber, GpioDeviceInterface.Mode mode) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), pinNumber, mode);
	}

	/**
	 * @param deviceFactory
	 *            Device factory to use to provision this digital input device.
	 * @param pinNumber
	 *            Pin number to which the device is connected.
	 * @param mode
	 *            Input or output {@link com.diozero.internal.spi.GpioDeviceInterface.Mode Mode}
	 * @throws RuntimeIOException
	 *             If an I/O error occurs.
	 */
	public DigitalInputOutputDevice(GpioDeviceFactoryInterface deviceFactory, int pinNumber,
			GpioDeviceInterface.Mode mode) throws RuntimeIOException {
		super(pinNumber);
		
		checkMode(mode);
		
		this.device = deviceFactory.provisionDigitalInputOutputPin(pinNumber, mode);
		this.mode = mode;
	}
	
	private static void checkMode(GpioDeviceInterface.Mode mode) {
		if (mode != GpioDeviceInterface.Mode.DIGITAL_INPUT && mode != GpioDeviceInterface.Mode.DIGITAL_OUTPUT) {
			throw new IllegalArgumentException("Invalid mode value, must be DIGITAL_INPUT or DIGITAL_OUTPUT");
		}
	}

	@Override
	public void close() throws RuntimeIOException {
		Logger.debug("close()");
		device.close();
	}
	
	/**
	 * Get the input / output mode
	 * @return current mode
	 */
	public GpioDeviceInterface.Mode getMode() {
		return mode;
	}
	
	/**
	 * Set the input / output mode
	 * @param mode new mode, valid values are {@link com.diozero.internal.spi.GpioDeviceInterface.Mode DIGITAL_INPUT} and {@link com.diozero.internal.spi.GpioDeviceInterface.Mode DIGITAL_OUTPUT}
	 */
	public void setMode(GpioDeviceInterface.Mode mode) {
		if (mode == this.mode) {
			return;
		}
		checkMode(mode);
		
		device.setMode(mode);
		this.mode = mode;
	}

	/**
	 * Read the current underlying state of the input pin. Does not factor in
	 * active high logic.
	 * 
	 * @return Device state.
	 * @throws RuntimeIOException
	 *             If an I/O error occurred.
	 */
	@Override
	public boolean getValue() throws RuntimeIOException {
		return device.getValue();
	}

	/**
	 * Set the output value (if mode.
	 * 
	 * @param value
	 *            The new value
	 * @throws RuntimeIOException
	 *             If an I/O error occurs
	 */
	public void setValue(boolean value) throws RuntimeIOException {
		if (mode != GpioDeviceInterface.Mode.DIGITAL_OUTPUT) {
			throw new IllegalStateException("Can only set output value for digital output pins");
		}
		device.setValue(value);
	}
}
