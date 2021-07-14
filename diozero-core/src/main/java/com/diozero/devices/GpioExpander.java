package com.diozero.devices;

/*
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     GpioExpander.java
 * 
 * This file is part of the diozero project. More information about this project
 * can be found at https://www.diozero.com/.
 * %%
 * Copyright (C) 2016 - 2021 diozero
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

import com.diozero.api.DeviceInterface;
import com.diozero.api.RuntimeIOException;

/**
 * Interface for GPIO expansion boards such as the MCP23xxx family of devices
 */
public interface GpioExpander extends DeviceInterface {
	int GPIOS_PER_PORT = 8;

	/**
	 * Set the directions for all pins on this port using the specified directions
	 * bit mask. 0 = output, 1 = input
	 *
	 * @param port       the bank of up to 8 GPIOs
	 * @param directions bit mask specifying the directions for up to 8 GPIOs. 0 =
	 *                   output, 1 = input
	 */
	void setDirections(int port, byte directions);

	/**
	 * Set the output value for a pins on this port.
	 *
	 * @param port   the bank of up to 8 GPIOs
	 * @param values bit mask specifying on/off values. 1 = on, 0 = off
	 */
	void setValues(int port, byte values);

	@Override
	void close() throws RuntimeIOException;
}
