/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     AnalogOutputDevice.java
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

package com.diozero.api;

import com.diozero.internal.spi.AnalogOutputDeviceFactoryInterface;
import com.diozero.internal.spi.AnalogOutputDeviceInterface;
import com.diozero.sbc.DeviceFactoryHelper;

/**
 * Analog output device. The output value is scaled in the range 0..1.
 */
public class AnalogOutputDevice extends GpioDevice {
	public static final class Builder {
		public static Builder builder(int dacNum) {
			return new Builder(dacNum);
		}

		public static Builder builder(PinInfo pinInfo) {
			return new Builder(pinInfo);
		}

		private Integer dacNum;
		private PinInfo pinInfo;
		private float initialValue = 0;
		private AnalogOutputDeviceFactoryInterface deviceFactory;

		public Builder(int dacNum) {
			this.dacNum = Integer.valueOf(dacNum);
		}

		public Builder(PinInfo pinInfo) {
			this.pinInfo = pinInfo;
		}

		/**
		 * Set the analog output value to be set when provisioning the device
		 *
		 * @param initialValue initial analog output value, must be 0..1
		 * @return the build instance
		 * @throws IllegalArgumentException if the initialValue is out of bounds
		 */
		public Builder setInitialValue(float initialValue) throws IllegalArgumentException {
			if (initialValue < 0 || initialValue > 1) {
				throw new IllegalArgumentException("Analog output value must be 0..1");
			}

			this.initialValue = initialValue;
			return this;
		}

		public Builder setDeviceFactory(AnalogOutputDeviceFactoryInterface deviceFactory) {
			this.deviceFactory = deviceFactory;
			return this;
		}

		public AnalogOutputDevice build() {
			// Default to the native device factory if not set
			if (deviceFactory == null) {
				deviceFactory = DeviceFactoryHelper.getNativeDeviceFactory();
			}

			if (pinInfo == null) {
				pinInfo = deviceFactory.getBoardPinInfo().getByDacNumberOrThrow(dacNum.intValue());
			}

			return new AnalogOutputDevice(deviceFactory, pinInfo, initialValue);
		}
	}

	private AnalogOutputDeviceInterface delegate;

	public AnalogOutputDevice(AnalogOutputDeviceFactoryInterface deviceFactory, PinInfo pinInfo, float initialValue) {
		super(pinInfo);

		delegate = deviceFactory.provisionAnalogOutputDevice(pinInfo, initialValue);
	}

	public float getValue() {
		return delegate.getValue();
	}

	/**
	 * Set the analog output value
	 *
	 * @param value new analog output value in the range -1..1
	 * @throws IllegalArgumentException if value is out of bounds
	 */
	public void setValue(float value) throws IllegalArgumentException {
		if (value < -1 || value > 1) {
			throw new IllegalArgumentException("Analog output value must be -1..1");
		}
		delegate.setValue(value);
	}

	@Override
	public void close() {
		delegate.close();
	}
}
