package com.diozero.remote.message;

/*-
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Remote Server
 * Filename:     ProvisionDigitalInputDevice.java  
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at http://www.diozero.com/
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

import com.diozero.api.GpioEventTrigger;
import com.diozero.api.GpioPullUpDown;

public class ProvisionDigitalInputDevice extends GpioBase {
	private static final long serialVersionUID = 4983915792050568739L;

	private GpioPullUpDown pud;
	private GpioEventTrigger trigger;

	public ProvisionDigitalInputDevice(int gpio, GpioPullUpDown pud, GpioEventTrigger trigger, String correlationId) {
		super(gpio, correlationId);
		
		this.pud = pud;
		this.trigger = trigger;
	}

	public GpioPullUpDown getPud() {
		return pud;
	}

	public GpioEventTrigger getTrigger() {
		return trigger;
	}

	@Override
	public String toString() {
		return "ProvisionDigitalInputDevice [pud=" + pud + ", trigger=" + trigger + ", gpio=" + getGpio()
				+ "]";
	}
}
