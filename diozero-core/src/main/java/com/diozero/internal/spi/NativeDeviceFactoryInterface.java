package com.diozero.internal.spi;

/*-
 * #%L
 * Organisation: diozero
 * Project:      diozero - Core
 * Filename:     NativeDeviceFactoryInterface.java
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

import java.util.stream.Stream;

import com.diozero.api.DeviceMode;
import com.diozero.sbc.BoardInfo;
import com.diozero.util.ServiceLoaderUtil;

import aQute.bnd.annotation.Resolution;
import aQute.bnd.annotation.spi.ServiceConsumer;

@ServiceConsumer(value = NativeDeviceFactoryInterface.class,resolution = Resolution.OPTIONAL)
public interface NativeDeviceFactoryInterface extends GpioDeviceFactoryInterface, SpiDeviceFactoryInterface,
		I2CDeviceFactoryInterface, PwmOutputDeviceFactoryInterface, ServoDeviceFactoryInterface,
		AnalogInputDeviceFactoryInterface, AnalogOutputDeviceFactoryInterface, SerialDeviceFactoryInterface {
	void registerDeviceFactory(DeviceFactoryInterface deviceFactory);

	BoardInfo getBoardInfo();

	int getGpioValue(int gpio);

	DeviceMode getGpioMode(int gpio);

	float getCpuTemperature();

	static Stream<NativeDeviceFactoryInterface> loadInstances() {
		/*-
		return StreamSupport.stream(ServiceLoader.load(NativeDeviceFactoryInterface.class).spliterator(), false);
		*/
		return ServiceLoaderUtil.stream(NativeDeviceFactoryInterface.class);
	}
}
