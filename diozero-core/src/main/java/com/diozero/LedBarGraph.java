package com.diozero;

/*
 * #%L
 * Device I/O Zero - Core
 * %%
 * Copyright (C) 2016 - 2017 mattjlewis
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


import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.diozero.api.Action;
import com.diozero.api.OutputDeviceInterface;
import com.diozero.internal.DeviceFactoryHelper;
import com.diozero.internal.spi.GpioDeviceFactoryInterface;
import com.diozero.util.RangeUtil;
import com.diozero.util.RuntimeIOException;

public class LedBarGraph implements OutputDeviceInterface, Closeable {
	private List<LED> leds;
	private float value;
	
	public LedBarGraph(int... gpios) {
		this(DeviceFactoryHelper.getNativeDeviceFactory(), true, gpios);
	}
	
	public LedBarGraph(GpioDeviceFactoryInterface deviceFactory, int... gpios) {
		this(deviceFactory, true, gpios);
	}
	
	public LedBarGraph(GpioDeviceFactoryInterface deviceFactory, boolean activeHigh, int... gpios) {
		leds = new ArrayList<>();
		for (int gpio : gpios) {
			leds.add(new LED(deviceFactory, gpio, activeHigh, false));
		}
	}
	
	public LedBarGraph(LED... leds) {
		this.leds = Arrays.asList(leds);
	}
	
	public LedBarGraph(List<LED> leds) {
		this.leds = leds;
	}
	
	public void on() {
		for (LED led : leds) {
			led.on();
		}
	}
	
	public void off() {
		for (LED led : leds) {
			led.off();
		}
	}
	
	public void toggle() {
		for (LED led : leds) {
			led.toggle();
		}
	}
	
	public void blink() {
		
	}
	
	public void blink(float onTime, float offTime, int iterations, Action stopAction) {
		for (LED led : leds) {
			led.blink(onTime, offTime, iterations, true, stopAction);
		}
	}
	
	/**
	 * Get the proportion of LEDs currently lit.
	 * @return Proportion of LEDs lit. 0..1 if lit from left to right, 0..-1 if lit from right to left.
	 */
	public float getValue() {
		return value;
	}
	
	/**
	 * Light a proportion of the LEDs using value as a percentage.
	 * @param value Proportion of LEDs to light. 0..1 lights from left to right, 0..-1 lights from right to left.
	 */
	@Override
	public void setValue(float newValue) {
		value = RangeUtil.constrain(newValue, -1, 1);
		int light_up_to = Math.round(value * leds.size());
		for (int i=0; i<leds.size(); i++) {
			leds.get(i).setOn(light_up_to >= 0 ? i <= light_up_to : i >= leds.size()+light_up_to);
		}
	}

	@Override
	public void close() throws RuntimeIOException {
		for (LED led : leds) {
			led.close();
		}
	}
}
