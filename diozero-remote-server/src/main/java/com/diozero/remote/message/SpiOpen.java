package com.diozero.remote.message;

/*-
 * #%L
 * Organisation: mattjlewis
 * Project:      Device I/O Zero - Remote Server
 * Filename:     SpiOpen.java  
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

import com.diozero.api.SpiClockMode;

public class SpiOpen extends SpiBase {
	private static final long serialVersionUID = -8304223356554333329L;

	private int frequency;
	private SpiClockMode clockMode;
	private boolean lsbFirst;

	public SpiOpen(int controller, int chipSelect, int frequency, SpiClockMode clockMode, boolean lsbFirst, String correlationId) {
		super(controller, chipSelect, correlationId);
		
		this.frequency = frequency;
		this.clockMode = clockMode;
		this.lsbFirst = lsbFirst;
	}

	public int getFrequency() {
		return frequency;
	}

	public SpiClockMode getClockMode() {
		return clockMode;
	}

	public boolean getLsbFirst() {
		return lsbFirst;
	}

	@Override
	public String toString() {
		return "ProvisionSpiDevice [clockMode=" + clockMode + ", frequency=" + frequency + ", lsbFirst=" + lsbFirst
				+ ", controller=" + getController() + ", chipSelect=" + getChipSelect() + "]";
	}
}
