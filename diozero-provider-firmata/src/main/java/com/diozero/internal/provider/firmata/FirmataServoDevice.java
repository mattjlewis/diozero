package com.diozero.internal.provider.firmata;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Firmata
 * Filename:     FirmataServoDevice.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2022 diozero
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import org.tinylog.Logger;

import com.diozero.api.RuntimeIOException;
import com.diozero.internal.provider.firmata.adapter.FirmataAdapter;
import com.diozero.internal.provider.firmata.adapter.FirmataProtocol.PinMode;
import com.diozero.internal.spi.AbstractDevice;
import com.diozero.internal.spi.InternalServoDeviceInterface;

public class FirmataServoDevice extends AbstractDevice implements InternalServoDeviceInterface {
	// Values < 544 will be treated as angles in degrees, values >= 544 are treated
	// as pulse width in microseconds
	// Ref:
	// https://github.com/firmata/firmata.js/blob/54dda2d2112e9fc3f997324df22d59f6e3d05298/packages/firmata-io/lib/firmata.js#L923
	private static final int MIN_PULSE_WIDTH_US = 544;
	private static final int ARDUINO_SERVO_FREQUENCY = 50;

	private FirmataAdapter adapter;
	private int gpio;
	private int max;

	public FirmataServoDevice(FirmataDeviceFactory deviceFactory, String key, int gpio, int minPulseWidthUs,
			int maxPulseWidthUs, int initialPulseWidthUs) {
		super(key, deviceFactory);

		this.gpio = gpio;

		adapter = deviceFactory.getFirmataAdapter();
		max = adapter.getMax(gpio, PinMode.SERVO);

		// minPulse and maxPulse are both 14-bit unsigned integers
		/*-
		// This will map 0-180 to 1000-1500
		board.servoConfig(9, 1000, 1500);
		 */
		adapter.servoConfig(gpio, Math.max(MIN_PULSE_WIDTH_US, minPulseWidthUs), Math.min(max, maxPulseWidthUs));
		adapter.setPinMode(gpio, PinMode.SERVO);

		setPulseWidthUs(initialPulseWidthUs);
	}

	@Override
	public int getGpio() {
		return gpio;
	}

	@Override
	public int getServoNum() {
		return gpio;
	}

	@Override
	public int getPulseWidthUs() throws RuntimeIOException {
		return adapter.getValue(gpio);
	}

	@Override
	public void setPulseWidthUs(int pulseWidthUs) throws RuntimeIOException {
		// Values < 544 will be treated as angles in degrees, values >= 544 are treated
		// as pulse width in microseconds
		// Ref:
		// https://github.com/firmata/firmata.js/blob/54dda2d2112e9fc3f997324df22d59f6e3d05298/packages/firmata-io/lib/firmata.js#L923
		// Should really be in the range 544..2,500 microseconds
		adapter.setValue(gpio, Math.max(MIN_PULSE_WIDTH_US, Math.min(max, pulseWidthUs)));
	}

	@Override
	public int getServoFrequency() {
		return ARDUINO_SERVO_FREQUENCY;
	}

	@Override
	public void setServoFrequency(int frequencyHz) throws RuntimeIOException {
		throw new UnsupportedOperationException("Cannot change servo frequency via Firmata");
	}

	@Override
	protected void closeDevice() throws RuntimeIOException {
		Logger.trace("closeDevice() {}", getKey());
		// Cannot do setPulseWidth(0) as that is interpreted as 0 degrees
		// So... revert to the default (digital output, value off)
		adapter.setPinMode(gpio, PinMode.DIGITAL_OUTPUT);
		adapter.setDigitalValue(gpio, false);
	}
}
