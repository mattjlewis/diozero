package com.diozero.devices;

/*
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Core
 * Filename:     GP2Y0A21YK.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
 * %%
 * Copyright (C) 2016 - 2020 mattjlewis
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
import com.diozero.internal.provider.AnalogInputDeviceFactoryInterface;
import com.diozero.util.DeviceFactoryHelper;
import com.diozero.util.RuntimeIOException;
import com.diozero.util.SleepUtil;

/**
 * <p>Sharp GP2Y0A21YK distance sensor. <a href="http://www.sharpsma.com/webfm_send/1208">Datasheet</a>, 
 * <a href="http://oomlout.com/parts/IC-PROX-01-guide.pdf">Guide</a>.
 * Important to supply 5V to get correct readings (max output voltage ~3.2V).</p>
 * <pre>
 * Range: 10 to 80 cm
 * Typical response time: 39 ms
 * Typical start up delay: 44 ms
 * Average Current Consumption: 30 mA
 * Detection Area Diameter @ 80 cm: 12 cm
 * Supply voltage : 4.5 to 5.5 V
 * </pre>
 */
public class GP2Y0A21YK extends AnalogInputDevice implements DistanceSensorInterface {
	public GP2Y0A21YK(int gpio) throws RuntimeIOException {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), gpio);
	}
	
	public GP2Y0A21YK(AnalogInputDeviceFactoryInterface deviceFactory, int gpio) throws RuntimeIOException {
		super(deviceFactory, gpio);
		SleepUtil.sleepMillis(44);
	}
	
	/**
	 * Read distance in centimetres, range 10 to 80cm.
	 * @return Distance, range 10 to 80cm.
	 */
	@Override
	public float getDistanceCm() throws RuntimeIOException {
		float v = getScaledValue();
		return (float) (27.86 * Math.pow(v, -1.15));
	}
}
