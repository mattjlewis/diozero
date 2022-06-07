package com.diozero.internal.spi;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     MmapGpioInterface.java
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

import com.diozero.api.DeviceMode;
import com.diozero.api.GpioPullUpDown;

public interface MmapGpioInterface extends AutoCloseable {
	void initialise();

	@Override
	void close();

	DeviceMode getMode(int gpio);

	/**
	 * Set the new mode for this GPIO. Note typically only supports
	 * {@link com.diozero.api.DeviceMode#DIGITAL_INPUT DIGITAL_INPUT},
	 * {@link com.diozero.api.DeviceMode.DeviceMode#DIGITAL_OUTPUT DIGITAL_OUTPUT}
	 * and possibly {@link com.diozero.api.DeviceMode.DeviceMode#PWM_OUTPUT
	 * PWM_OUTPUT}.
	 *
	 * @param gpio The GPIO to configure
	 * @param mode The new mode
	 */
	void setMode(int gpio, DeviceMode mode);

	/**
	 * Set the new mode for this GPIO without any checks on either the GPIO number
	 * or new mode. <strong>Health warning</strong>: make sure you know what you are
	 * doing when invoking this method.
	 *
	 * @param gpio The GPIO to configure
	 * @param mode The new mode
	 */
	void setModeUnchecked(int gpio, int mode);

	void setPullUpDown(int gpio, GpioPullUpDown pud);

	boolean gpioRead(int gpio);

	void gpioWrite(int gpio, boolean value);
}
