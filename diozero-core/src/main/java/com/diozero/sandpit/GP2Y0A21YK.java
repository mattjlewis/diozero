package com.diozero.sandpit;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 diozero
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

import com.diozero.api.AnalogInputDevice;
import com.diozero.api.DistanceSensorInterface;
import com.diozero.internal.DeviceFactoryHelper;
import com.diozero.internal.spi.AnalogInputDeviceFactoryInterface;
import com.diozero.util.RuntimeIOException;
import com.diozero.util.SleepUtil;

/**
 * Sharp GP2Y0A21YK distance sensor. http://www.sharpsma.com/webfm_send/1208
 * Range: 10 to 80 cm
 * Typical response time: 39 ms
 * Typical start up delay: 44 ms
 * Average Current Consumption: 30 mA
 * Detection Area Diameter @ 80 cm: 12 cm
 */
public class GP2Y0A21YK extends AnalogInputDevice implements DistanceSensorInterface {
	public GP2Y0A21YK(int pinNumber) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), pinNumber);
	}
	
	public GP2Y0A21YK(AnalogInputDeviceFactoryInterface deviceFactory, int pinNumber) throws RuntimeIOException {
		super(deviceFactory, pinNumber, 1);
		SleepUtil.sleepMillis(44);
	}
	
	@Override
	public float getScaledValue() throws RuntimeIOException {
		return getDistanceCm();
	}
	
	@Override
	public float getDistanceCm() throws RuntimeIOException {
		float v = super.getScaledValue();
		return (float)(16.2537 * Math.pow(v, 4) - 129.893 * Math.pow(v, 3) + 382.268 * Math.pow(v, 2) - 512.611 * v + 306.439);
	}
}
